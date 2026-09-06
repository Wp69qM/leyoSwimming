#!/usr/bin/env pwsh
#requires -Version 5.1

<#
.SYNOPSIS
    将本地知识库文档同步到生产服务器。
.DESCRIPTION
    同步内容：
    1. MySQL 表 ai_knowledge_document（文档元数据与正文）
    2. ai-service/data/chroma 向量库数据

    同步前会自动备份生产端的 MySQL 表和 Chroma 数据，同步完成后重启 ai-service。
    采用事务性 MySQL 导入和原子性 Chroma 目录替换，失败时尽量保证数据一致性。

    要求：
    - 本地已安装 mysqldump（在 PATH 中）
    - 服务器已安装 mysql 客户端
    - 服务器上 ai-service 以 systemd 服务运行

    安全说明：
    - MySQL 密码通过临时配置文件 (--defaults-extra-file) 传递，避免出现在命令行
    - 建议首次运行前先执行 -DryRun 查看计划
.EXAMPLE
    .\scripts\sync-knowledge-to-prod.ps1 -ServerIP '14.103.149.202' -AcceptHostKey `
        -LocalMySqlPassword (Read-Host -AsSecureString "本地 MySQL 密码") `
        -ProdMySqlPassword (Read-Host -AsSecureString "生产 MySQL 密码")
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory, HelpMessage = "目标服务器 IP")]
    [string]$ServerIP,

    [string]$ServerUser = "root",

    [string]$ProjectRoot = "$PSScriptRoot\..",

    # 本地 MySQL 连接
    [string]$LocalMySqlHost = "localhost",
    [int]$LocalMySqlPort = 3306,
    [string]$LocalMySqlUser = "root",
    [Parameter(Mandatory, HelpMessage = "本地 MySQL 密码")]
    [Security.SecureString]$LocalMySqlPassword,
    [string]$LocalMySqlDatabase = "leyo",

    # 生产 MySQL 连接
    [string]$ProdMySqlHost = "localhost",
    [int]$ProdMySqlPort = 3306,
    [string]$ProdMySqlUser = "root",
    [Parameter(Mandatory, HelpMessage = "生产 MySQL 密码")]
    [Security.SecureString]$ProdMySqlPassword,
    [string]$ProdMySqlDatabase = "leyo_prod",

    # Chroma 数据路径
    [string]$LocalChromaPath = "$PSScriptRoot\..\ai-service\data\chroma",

    [ValidatePattern('^/opt/leyo-swimming(/[\w\-]+)+$')]
    [string]$RemoteChromaPath = "/opt/leyo-swimming/ai-service/data/chroma",

    [string]$RemoteAiServiceDir = "/opt/leyo-swimming/ai-service",

    [string]$AiServiceSystemd = "leyo-ai-service",

    [string]$BackupDir = "/opt/backups",

    [string]$HostKeyFingerprint = "",

    [switch]$AcceptHostKey,

    [switch]$SkipBackup,

    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

# ============================================================================
# 工具函数
# ============================================================================
function Test-CommandExists {
    param([string]$Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function ConvertFrom-SecureStringPlain {
    param([Security.SecureString]$SecureString)
    return [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureString)
    )
}

function New-MySqlDefaultsFile {
    param(
        [string]$Host,
        [int]$Port,
        [string]$User,
        [Security.SecureString]$Password,
        [string]$Group = "client"
    )
    $plainPassword = ConvertFrom-SecureStringPlain -SecureString $Password
    $tmp = [System.IO.Path]::GetTempFileName()
    @"
[$Group]
host=$Host
port=$Port
user=$User
password=$plainPassword
"@ | Set-Content -Path $tmp -Encoding UTF8 -NoNewline
    return $tmp
}

function Install-PoshSSH {
    if (-not (Get-Module -ListAvailable -Name Posh-SSH)) {
        Write-Host "正在安装 Posh-SSH 模块..."
        Install-Module -Name Posh-SSH -Scope CurrentUser -Force -AllowClobber
    }
    Import-Module Posh-SSH -Force
}

function New-SSHSessionSafe {
    param([string]$IPAddress, [string]$CredentialUser)
    $securePassword = Read-Host -Prompt "请输入服务器 $IPAddress 的 $CredentialUser 密码" -AsSecureString
    $cred = New-Object System.Management.Automation.PSCredential($CredentialUser, $securePassword)

    $sessionParams = @{
        ComputerName = $IPAddress
        Credential   = $cred
        AcceptKey    = $AcceptHostKey.IsPresent
    }
    if ($HostKeyFingerprint) {
        $sessionParams['KeyString'] = $HostKeyFingerprint
    }
    return New-SSHSession @sessionParams
}

function Invoke-RemoteCommand {
    param(
        [object]$Session,
        [string]$Command,
        [string]$Description = "远程命令",
        [int]$Timeout = 120
    )
    Write-Host "[$Description]"
    if ($DryRun.IsPresent) {
        Write-Host "  [DryRun] 跳过执行"
        return @{ ExitStatus = 0; Output = "" }
    }
    $result = Invoke-SSHCommand -SessionId $Session.SessionId -Command $Command -TimeOut $Timeout
    if ($result.ExitStatus -ne 0) {
        throw "远程命令失败 (exit=$($result.ExitStatus)): $Command`nSTDOUT:`n$($result.Output)`nSTDERR:`n$($result.Error)"
    }
    if ($result.Output) {
        Write-Host $result.Output
    }
    return $result
}

function Send-RemoteFile {
    param(
        [object]$Session,
        [string]$LocalFile,
        [string]$RemoteDestination,
        [string]$Description = "上传文件"
    )
    Write-Host "[$Description] $LocalFile -> ${ServerIP}:$RemoteDestination"
    if ($DryRun.IsPresent) {
        Write-Host "  [DryRun] 跳过上传"
        return
    }
    Set-SCPFile -ComputerName $ServerIP -Credential $Session.Credential `
        -LocalFile $LocalFile -RemotePath $RemoteDestination -AcceptKey:$AcceptHostKey.IsPresent -OperationTimeout 300
}

function Remove-TempFileSafely {
    param([string]$Path)
    if ($Path -and (Test-Path $Path)) {
        Remove-Item $Path -ErrorAction SilentlyContinue
    }
}

# ============================================================================
# 前置检查
# ============================================================================
Write-Host "========================================"
Write-Host "leyoSwimming 知识库同步到生产"
Write-Host "========================================"

if (-not (Test-CommandExists "mysqldump")) {
    throw "本地未找到 mysqldump，请先安装 MySQL 客户端并加入 PATH。"
}

if (-not (Test-Path $LocalChromaPath)) {
    throw "本地 Chroma 数据目录不存在: $LocalChromaPath"
}

Install-PoshSSH

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$localDumpFile = Join-Path $env:TEMP "ai_knowledge_document_sync_$timestamp.sql"
$localChromaTar = Join-Path $env:TEMP "chroma_sync_$timestamp.tar.gz"
$localDefaultsFile = $null

$session = $null

# ============================================================================
# 主流程
# ============================================================================
try {
    # ========================================================================
    # 1. 导出本地 MySQL 知识库表
    # ========================================================================
    Write-Host ""
    Write-Host "[1/6] 导出本地 MySQL 表 ai_knowledge_document..."
    $localDefaultsFile = New-MySqlDefaultsFile -Host $LocalMySqlHost -Port $LocalMySqlPort -User $LocalMySqlUser -Password $LocalMySqlPassword -Group "mysqldump"

    if (-not $DryRun.IsPresent) {
        & mysqldump --defaults-extra-file=$localDefaultsFile --single-transaction --quick --lock-tables=false $LocalMySqlDatabase ai_knowledge_document > $localDumpFile
        if ($LASTEXITCODE -ne 0) {
            throw "mysqldump 导出失败，退出码 $LASTEXITCODE"
        }
        if (-not (Test-Path $localDumpFile) -or (Get-Item $localDumpFile).Length -eq 0) {
            throw "mysqldump 导出文件为空"
        }
        Write-Host "导出完成: $localDumpFile ($((Get-Item $localDumpFile).Length) bytes)"
    }

    # ========================================================================
    # 2. 打包本地 Chroma 向量库
    # ========================================================================
    Write-Host ""
    Write-Host "[2/6] 打包本地 Chroma 向量库..."
    $chromaParent = Split-Path $LocalChromaPath -Parent
    $chromaDirName = Split-Path $LocalChromaPath -Leaf

    if (-not $DryRun.IsPresent) {
        & tar -czf $localChromaTar -C $chromaParent $chromaDirName
        if ($LASTEXITCODE -ne 0) {
            throw "Chroma 打包失败，退出码 $LASTEXITCODE"
        }
        if (-not (Test-Path $localChromaTar) -or (Get-Item $localChromaTar).Length -eq 0) {
            throw "Chroma 打包文件为空"
        }
        Write-Host "打包完成: $localChromaTar ($((Get-Item $localChromaTar).Length) bytes)"
    }

    # ========================================================================
    # 3. 建立 SSH 连接
    # ========================================================================
    Write-Host ""
    Write-Host "[3/6] 连接服务器 $ServerIP..."
    $session = New-SSHSessionSafe -IPAddress $ServerIP -CredentialUser $ServerUser
    Write-Host "SSH 连接成功"

    if ($AcceptHostKey.IsPresent -and -not $HostKeyFingerprint) {
        Write-Warning "使用 -AcceptHostKey 但未提供 -HostKeyFingerprint，会信任任何服务器密钥，存在中间人风险。"
    }

    # ========================================================================
    # 4. 检查远程依赖
    # ========================================================================
    Write-Host ""
    Write-Host "[4/6] 检查远程依赖..."
    Invoke-RemoteCommand -Session $session -Command "command -v mysql && command -v mysqldump && command -v tar && command -v systemctl && command -v curl" -Description "检查远程依赖"

    # ========================================================================
    # 5. 上传文件到服务器 /tmp
    # ========================================================================
    Write-Host ""
    Write-Host "[5/6] 上传同步文件..."
    Send-RemoteFile -Session $session -LocalFile $localDumpFile -RemoteDestination "/tmp" -Description "上传 MySQL 导出文件"
    Send-RemoteFile -Session $session -LocalFile $localChromaTar -RemoteDestination "/tmp" -Description "上传 Chroma 向量库"

    # ========================================================================
    # 6. 在服务器上执行同步
    # ========================================================================
    Write-Host ""
    Write-Host "[6/6] 在生产环境执行同步..."

    $remoteDumpFile = "/tmp/$(Split-Path $localDumpFile -Leaf)"
    $remoteChromaTar = "/tmp/$(Split-Path $localChromaTar -Leaf)"
    $prodPasswordPlain = ConvertFrom-SecureStringPlain -SecureString $ProdMySqlPassword
    $skipBackupFlag = if ($SkipBackup.IsPresent) { '1' } else { '0' }

    $remoteScript = @"
#!/bin/bash
set -euo pipefail

BACKUP_DIR="$BackupDir"
REMOTE_DUMP_FILE="$remoteDumpFile"
REMOTE_CHROMA_TAR="$remoteChromaTar"
REMOTE_CHROMA_PATH="$RemoteChromaPath"
AI_SERVICE_SYSTEMD="$AiServiceSystemd"
PROD_MYSQL_HOST="$ProdMySqlHost"
PROD_MYSQL_PORT="$ProdMySqlPort"
PROD_MYSQL_USER="$ProdMySqlUser"
PROD_MYSQL_PASSWORD="$prodPasswordPlain"
PROD_MYSQL_DATABASE="$ProdMySqlDatabase"
TIMESTAMP="$timestamp"

export MYSQL_PWD="\$PROD_MYSQL_PASSWORD"

# 创建备份目录并收紧权限
mkdir -p "\$BACKUP_DIR"
chmod 700 "\$BACKUP_DIR"

# 若启用了备份，备份生产 MySQL 表和 Chroma 数据
if [ "$skipBackupFlag" = "0" ]; then
    mysqldump -h\$PROD_MYSQL_HOST -P\$PROD_MYSQL_PORT -u\$PROD_MYSQL_USER \$PROD_MYSQL_DATABASE ai_knowledge_document > "\$BACKUP_DIR/ai_knowledge_document_before_sync_\$TIMESTAMP.sql"
    chmod 600 "\$BACKUP_DIR/ai_knowledge_document_before_sync_\$TIMESTAMP.sql"

    if [ -d "\$REMOTE_CHROMA_PATH" ]; then
        tar -czf "\$BACKUP_DIR/chroma_before_sync_\$TIMESTAMP.tar.gz" -C "\$(dirname \$REMOTE_CHROMA_PATH)" "\$(basename \$REMOTE_CHROMA_PATH)"
        chmod 600 "\$BACKUP_DIR/chroma_before_sync_\$TIMESTAMP.tar.gz"
    fi
fi

# 停止 ai-service，并通过 trap 保证后续一定能重新启动
systemctl stop \$AI_SERVICE_SYSTEMD || true

cleanup() {
    echo "确保 ai-service 重新启动..."
    systemctl start \$AI_SERVICE_SYSTEMD || true
}
trap cleanup EXIT

# 事务性导入 MySQL 表
mysql -h\$PROD_MYSQL_HOST -P\$PROD_MYSQL_PORT -u\$PROD_MYSQL_USER \$PROD_MYSQL_DATABASE <<EOSQL
START TRANSACTION;
DELETE FROM ai_knowledge_document;
SOURCE \$REMOTE_DUMP_FILE;
COMMIT;
EOSQL

# 原子替换 Chroma 向量库
CHROMA_TMP="\${REMOTE_CHROMA_PATH}.tmp.\$$"
rm -rf "\$CHROMA_TMP"
mkdir -p "\$CHROMA_TMP"

tar -xzf "\$REMOTE_CHROMA_TAR" -C "\$CHROMA_TMP" --strip-components=1

# 校验 Chroma 数据基本结构
if [ ! -f "\$CHROMA_TMP/chroma.sqlite3" ] && [ ! -d "\$CHROMA_TMP/index" ]; then
    echo "ERROR: Chroma 数据校验失败，未找到 chroma.sqlite3 或 index 目录" >&2
    exit 1
fi

# 原子替换
rm -rf "\${REMOTE_CHROMA_PATH}.old"
if [ -d "\$REMOTE_CHROMA_PATH" ]; then
    mv "\$REMOTE_CHROMA_PATH" "\${REMOTE_CHROMA_PATH}.old"
fi
mv "\$CHROMA_TMP" "\$REMOTE_CHROMA_PATH"
rm -rf "\${REMOTE_CHROMA_PATH}.old"

# 清理远程临时文件
rm -f "\$REMOTE_DUMP_FILE" "\$REMOTE_CHROMA_TAR"

echo "同步脚本主体执行完成"
"@

    if ($DryRun.IsPresent) {
        Write-Host "[DryRun] 将要执行的远程脚本:"
        Write-Host $remoteScript
        Write-Host "[DryRun] 跳过实际执行"
    }
    else {
        Invoke-RemoteCommand -Session $session -Command $remoteScript -Description "执行远程同步脚本" -Timeout 300
    }

    # ========================================================================
    # 7. 验证
    # ========================================================================
    Write-Host ""
    Write-Host "[7/6] 验证同步结果..."

    if (-not $DryRun.IsPresent) {
        Start-Sleep -Seconds 5
        $verifyCmd = "curl -s -o /dev/null -w '%{http_code}' http://localhost:8000/health"
        $healthResult = Invoke-SSHCommand -SessionId $session.SessionId -Command $verifyCmd -TimeOut 30
        $healthCode = $healthResult.Output.Trim()
        Write-Host "AI Service 健康检查 HTTP $healthCode"
        if ($healthCode -ne '200') {
            throw "AI Service 健康检查未通过: HTTP $healthCode"
        }

        $countCmd = "export MYSQL_PWD='$prodPasswordPlain'; mysql -h$ProdMySqlHost -P$ProdMySqlPort -u$ProdMySqlUser $ProdMySqlDatabase -N -e 'SELECT COUNT(*) FROM ai_knowledge_document WHERE status=0;'"
        $countResult = Invoke-SSHCommand -SessionId $session.SessionId -Command $countCmd -TimeOut 30
        Write-Host "生产环境启用状态的知识库文档数: $($countResult.Output.Trim())"
    }
}
finally {
    # 关闭 SSH 会话
    if ($session) {
        Remove-SSHSession -SessionId $session.SessionId -ErrorAction SilentlyContinue | Out-Null
    }

    # 清理本地临时文件
    Remove-TempFileSafely -Path $localDumpFile
    Remove-TempFileSafely -Path $localChromaTar
    Remove-TempFileSafely -Path $localDefaultsFile
}

Write-Host ""
Write-Host "========================================"
if ($DryRun.IsPresent) {
    Write-Host "[DryRun] 知识库同步计划预览完成"
}
else {
    Write-Host "知识库同步完成"
}
Write-Host "========================================"
