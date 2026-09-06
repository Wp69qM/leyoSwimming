#!/usr/bin/env pwsh
#requires -Version 5.1

<#
.SYNOPSIS
    leyoSwimming AI Service RAG + Tavily 兜底功能升级脚本。
.DESCRIPTION
    本次升级仅针对 ai-service：
    1. 本地打包 ai-service 源码
    2. 上传至服务器
    3. 备份 Chroma 向量库与 .env
    4. 更新 Python 依赖
    5. 重启 ai-service
    6. 执行健康检查

    适用于 ai-service 已以宿主机方式运行的生产环境。
.EXAMPLE
    .\scripts\upgrade-ai-service-rag.ps1 -ServerIP '123.45.67.89'
.EXAMPLE
    .\scripts\upgrade-ai-service-rag.ps1 -ServerIP '123.45.67.89' -ReinstallAIDeps -AcceptHostKey
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory, HelpMessage = "目标服务器 IP")]
    [string]$ServerIP,

    [string]$ServerUser = "root",

    [string]$RemotePath = "/opt/leyoSwimming",

    [string]$ProjectRoot = "$PSScriptRoot\..",

    [string]$HostKeyFingerprint = "",

    [switch]$AcceptHostKey,

    [string]$PyPIIndexUrl = "https://pypi.org/simple",

    [switch]$ReinstallAIDeps,

    [switch]$SkipUpload,

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

function Install-PoshSSH {
    if (-not (Get-Module -ListAvailable -Name Posh-SSH)) {
        Write-Host "正在安装 Posh-SSH 模块（当前用户范围）..." -ForegroundColor Cyan
        Install-Module -Name Posh-SSH -Scope CurrentUser -Force -AllowClobber
    }
    Import-Module Posh-SSH -Force
}

function Get-SecureCredential {
    Write-Host "`n请输入服务器 SSH 密码（输入时不会显示）" -ForegroundColor Cyan
    $securePassword = Read-Host -AsSecureString "SSH Password"
    return New-Object System.Management.Automation.PSCredential($ServerUser, $securePassword)
}

function Invoke-RemoteCommand {
    param(
        [Parameter(Mandatory)]
        [string]$Command,
        [switch]$IgnoreExitCode,
        [int]$TimeOut = 60
    )
    $session = Get-SSHSession | Select-Object -First 1
    if (-not $session) {
        throw "没有可用的 SSH 会话。"
    }
    $result = Invoke-SSHCommand -SSHSession $session -Command $Command -TimeOut $TimeOut
    if (-not $IgnoreExitCode -and $result.ExitStatus -ne 0) {
        $output = ($result.Output | Out-String).Trim()
        if ($output.Length -gt 2000) {
            $output = $output.Substring(0, 2000) + "`n...（输出已截断）"
        }
        throw "远程命令执行失败 (ExitStatus=$($result.ExitStatus))：`n$Command`n输出：`n$output"
    }
    return $result
}

function ConvertTo-ShellLiteral {
    param([string]$Value)
    return "'" + ($Value -replace "'", "'\''") + "'"
}

function Test-ShellSafePath {
    param([string]$Path)
    $unsafeChars = @(';', '|', '&', '$', '"', "`n", "`r", '<', '>', '`', '(', ')', '\', "'")
    if ($Path.IndexOfAny($unsafeChars) -ge 0) {
        throw "路径包含不安全的 shell 字符：$Path"
    }
    if ($Path -notmatch "^/") {
        throw "RemotePath 必须是绝对路径（以 / 开头）：$Path"
    }
    if ($Path -match "\.\.") {
        throw "RemotePath 不能包含路径遍历（..）：$Path"
    }
}

function Write-Section {
    param([string]$Message)
    Write-Host "`n========================================" -ForegroundColor Green
    Write-Host $Message -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
}

function Test-RemoteEnvVar {
    param(
        [string]$VarName,
        [string]$EnvFile = "ai-service/.env"
    )
    $lit = ConvertTo-ShellLiteral "$RemotePath/$EnvFile"
    $varLit = ConvertTo-ShellLiteral $VarName
    $result = Invoke-RemoteCommand -Command "grep -E '^$varLit=' $lit 2>/dev/null || echo NOT_FOUND" -IgnoreExitCode
    return ($result.Output.Trim() -ne "NOT_FOUND")
}

function Get-RemoteEnvVarValue {
    param(
        [string]$VarName,
        [string]$EnvFile = "ai-service/.env"
    )
    $lit = ConvertTo-ShellLiteral "$RemotePath/$EnvFile"
    $varLit = ConvertTo-ShellLiteral $VarName
    $result = Invoke-RemoteCommand -Command "grep -E '^$varLit=' $lit 2>/dev/null | sed 's/^[^=]*=//' | head -n 1" -IgnoreExitCode
    return $result.Output.Trim()
}

# ============================================================================
# 主流程
# ============================================================================
try {
    Write-Section "Step 0: 前置检查"

    if ($PyPIIndexUrl -notmatch '^https?://') {
        throw "PyPIIndexUrl 必须以 http:// 或 https:// 开头"
    }
    $unsafeUrlChars = @(';', '|', '&', '$', '"', "`n", "`r", '<', '>', '`', '(', ')', "'")
    if ($PyPIIndexUrl.IndexOfAny($unsafeUrlChars) -ge 0) {
        throw "PyPIIndexUrl 包含不安全的字符：$PyPIIndexUrl"
    }

    Test-ShellSafePath -Path $RemotePath
    $remotePathLit = ConvertTo-ShellLiteral $RemotePath

    $aiServiceRoot = Join-Path $ProjectRoot "ai-service"
    if (-not (Test-Path $aiServiceRoot)) {
        throw "本地 ai-service 目录不存在：$aiServiceRoot"
    }

    foreach ($cmd in @("ssh", "scp", "tar")) {
        if (-not (Test-CommandExists $cmd)) {
            throw "未找到 $cmd，请安装 OpenSSH 客户端和 tar 工具。"
        }
    }

    Install-PoshSSH
    $sshCred = Get-SecureCredential

    Write-Host "测试 SSH 连接到 ${ServerUser}@${ServerIP} ..." -ForegroundColor Cyan
    $newSessionParams = @{
        ComputerName = $ServerIP
        Credential   = $sshCred
    }
    if ($HostKeyFingerprint) {
        $newSessionParams['HostKey'] = $HostKeyFingerprint
    }
    else {
        if (-not $AcceptHostKey) {
            Write-Warning "未指定 -HostKeyFingerprint，将自动接受服务器 SSH 主机密钥（存在中间人攻击风险）。"
        }
        $newSessionParams['AcceptKey'] = $true
    }
    $session = New-SSHSession @newSessionParams
    if (-not $session) { throw "SSH 连接失败" }
    Write-Host "SSH 连接成功" -ForegroundColor Green

    # 确认服务器已部署 ai-service
    $aiServiceExists = Invoke-RemoteCommand -Command "test -d $(ConvertTo-ShellLiteral "$RemotePath/ai-service") && echo YES || echo NO" -IgnoreExitCode
    if ($aiServiceExists.Output.Trim() -ne "YES") {
        throw "服务器上未检测到 $RemotePath/ai-service，请先完成首次部署。"
    }

    # 检查 systemd 服务名
    $aiServiceSystemd = "leyo-ai-service"
    $systemdExists = Invoke-RemoteCommand -Command "systemctl list-unit-files | grep -q '^$aiServiceSystemd' && echo YES || echo NO" -IgnoreExitCode
    if ($systemdExists.Output.Trim() -ne "YES") {
        Write-Warning "未检测到 systemd 服务 $aiServiceSystemd，将尝试使用 pkill/手动启动"
        $aiServiceSystemd = $null
    }

    # 检查远程 .env 是否包含新变量
    Write-Host "`n检查服务器 ai-service/.env 中的新环境变量..." -ForegroundColor Cyan
    $requiredVars = @(
        @{ Name = "TAVILY_API_KEY"; Description = "Tavily 联网搜索 API Key（可选但强烈建议）" },
        @{ Name = "TAVILY_MAX_RESULTS"; Description = "Tavily 返回结果数量" },
        @{ Name = "CHROMA_PERSIST_DIRECTORY"; Description = "Chroma 数据持久化目录" },
        @{ Name = "KNOWLEDGE_SIMILARITY_THRESHOLD"; Description = "知识库相似度阈值" }
    )
    $missingVars = @()
    foreach ($var in $requiredVars) {
        if (Test-RemoteEnvVar -VarName $var.Name) {
            $value = Get-RemoteEnvVarValue -VarName $var.Name
            Write-Host "  [OK] $($var.Name) = $value" -ForegroundColor Gray
        }
        else {
            Write-Host "  [MISSING] $($var.Name) - $($var.Description)" -ForegroundColor Yellow
            $missingVars += $var
        }
    }
    if ($missingVars.Count -gt 0) {
        Write-Warning "服务器 ai-service/.env 缺少以下新变量，请先按升级文档补充："
        foreach ($var in $missingVars) {
            Write-Host "  - $($var.Name): $($var.Description)" -ForegroundColor Yellow
        }
        $continue = Read-Host "是否继续升级？(y/N，默认 N)"
        if ($continue -notmatch '^(y|Y|yes|YES)$') {
            Write-Host "已取消升级。" -ForegroundColor Red
            exit 0
        }
    }

    if ($DryRun) {
        Write-Host "`nDryRun 模式：仅显示计划，不执行实际操作。" -ForegroundColor Yellow
        exit 0
    }

    # ============================================================================
    # Step 1: 本地打包
    # ============================================================================
    Write-Section "Step 1: 本地打包 ai-service"

    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $tarName = "ai-service-release-${timestamp}.tar.gz"
    $tempTar = Join-Path $env:TEMP $tarName
    if (Test-Path $tempTar) { Remove-Item -Force $tempTar }

    Push-Location $aiServiceRoot
    try {
        $excludeArgs = @(
            "--exclude=.venv",
            "--exclude=logs",
            "--exclude=data",
            "--exclude=__pycache__",
            "--exclude=*.pyc",
            "--exclude=.git",
            "--exclude=.pytest_cache",
            "--exclude=.mypy_cache",
            "--exclude=*.tar.gz"
        )
        & tar -czf $tempTar @excludeArgs .
        if ($LASTEXITCODE -ne 0) { throw "打包 ai-service 失败" }
    }
    finally {
        Pop-Location
    }
    Write-Host "打包完成：$tempTar" -ForegroundColor Green

    # ============================================================================
    # Step 2: 备份服务器数据
    # ============================================================================
    if (-not $SkipBackup) {
        Write-Section "Step 2: 备份服务器数据"

        $backupCmds = @(
            "mkdir -p $remotePathLit/backups",
            "cp $(ConvertTo-ShellLiteral "$RemotePath/ai-service/.env") $(ConvertTo-ShellLiteral "$RemotePath/backups/ai-service.env.${timestamp}.bak") 2>/dev/null || true",
            "if [ -d $(ConvertTo-ShellLiteral "$RemotePath/ai-service/data/chroma") ]; then tar czf $(ConvertTo-ShellLiteral "$RemotePath/backups/chroma-${timestamp}.tar.gz") -C $(ConvertTo-ShellLiteral "$RemotePath/ai-service") data/chroma; fi"
        )
        Invoke-RemoteCommand -Command ($backupCmds -join " && ") -TimeOut 120 | Out-Null
        Write-Host "备份完成：$RemotePath/backups/" -ForegroundColor Green
    }
    else {
        Write-Host "跳过备份（-SkipBackup）" -ForegroundColor Yellow
    }

    # ============================================================================
    # Step 3: 上传代码包
    # ============================================================================
    if (-not $SkipUpload) {
        Write-Section "Step 3: 上传代码包到服务器"

        $scpParams = @{
            ComputerName = $ServerIP
            Credential   = $sshCred
            Path         = $tempTar
            Destination  = "$RemotePath/"
            NewName      = $tarName
        }
        if ($HostKeyFingerprint) {
            $scpParams['HostKey'] = $HostKeyFingerprint
        }
        elseif ($AcceptHostKey) {
            $scpParams['AcceptKey'] = $true
        }
        Set-SCPItem @scpParams
        Write-Host "上传完成：$RemotePath/$tarName" -ForegroundColor Green
    }
    else {
        Write-Host "跳过上传（-SkipUpload）" -ForegroundColor Yellow
    }

    # ============================================================================
    # Step 4: 服务器端升级
    # ============================================================================
    Write-Section "Step 4: 服务器端升级 ai-service"

    $aiServiceDirLit = ConvertTo-ShellLiteral "$RemotePath/ai-service"
    $tarPathLit = ConvertTo-ShellLiteral "$RemotePath/$tarName"
    $venvPathLit = ConvertTo-ShellLiteral "$RemotePath/ai-service/.venv"
    $requirementsLit = ConvertTo-ShellLiteral "$RemotePath/ai-service/requirements.txt"

    # 4.1 停止服务
    if ($aiServiceSystemd) {
        Write-Host "停止 systemd 服务 $aiServiceSystemd ..." -ForegroundColor Cyan
        Invoke-RemoteCommand -Command "systemctl stop $aiServiceSystemd 2>&1 || true" -TimeOut 60 -IgnoreExitCode | Out-Null
    }
    else {
        Write-Host "未使用 systemd，尝试停止 uvicorn 进程 ..." -ForegroundColor Cyan
        Invoke-RemoteCommand -Command "pkill -f 'uvicorn app.main:app' 2>&1 || true" -TimeOut 30 -IgnoreExitCode | Out-Null
    }
    Start-Sleep -Seconds 2

    # 4.2 解压覆盖
    Write-Host "解压覆盖 ai-service 代码 ..." -ForegroundColor Cyan
    $extractCmd = @(
        "cd $remotePathLit",
        "mkdir -p $aiServiceDirLit",
        "tar xzvf $tarPathLit -C $aiServiceDirLit",
        "rm -f $tarPathLit"
    ) -join " && "
    Invoke-RemoteCommand -Command $extractCmd -TimeOut 120 | Out-Null
    Write-Host "代码覆盖完成" -ForegroundColor Green

    # 4.3 创建/更新虚拟环境并安装依赖
    Write-Host "更新 Python 虚拟环境与依赖 ..." -ForegroundColor Cyan
    $depsCmd = @(
        "cd $aiServiceDirLit",
        "python3 -m venv $venvPathLit",
        "source $venvPathLit/bin/activate",
        "pip install --upgrade pip -i $(ConvertTo-ShellLiteral $PyPIIndexUrl)",
        "pip install -r $requirementsLit -i $(ConvertTo-ShellLiteral $PyPIIndexUrl)"
    )
    if ($ReinstallAIDeps) {
        $depsCmd += "pip install --force-reinstall -r $requirementsLit -i $(ConvertTo-ShellLiteral $PyPIIndexUrl)"
    }
    $depsCmd += "python -c 'import chromadb; import tavily; import langchain_chroma; print(\`"dependencies ok\`")'"
    Invoke-RemoteCommand -Command ($depsCmd -join " && ") -TimeOut 300 | Out-Null
    Write-Host "依赖更新完成" -ForegroundColor Green

    # 4.4 启动服务
    Write-Host "启动 ai-service ..." -ForegroundColor Cyan
    if ($aiServiceSystemd) {
        Invoke-RemoteCommand -Command "systemctl start $aiServiceSystemd 2>&1" -TimeOut 60 | Out-Null
    }
    else {
        Write-Warning "未检测到 systemd 服务，请手动启动 ai-service"
    }

    # ============================================================================
    # Step 5: 健康检查
    # ============================================================================
    Write-Section "Step 5: 健康检查"

    $healthCheckCmd = @(
        'i=1',
        'while [ $i -le 30 ]; do',
        '  if wget --no-verbose --tries=1 -O /dev/null http://localhost:8000/health 2>/dev/null || curl -fsS http://localhost:8000/health >/dev/null 2>&1; then',
        '    echo "AI service is healthy"',
        '    exit 0',
        '  fi',
        '  echo "Waiting for AI service... $i/30"',
        '  sleep 2',
        '  i=$((i+1))',
        'done',
        'echo "AI service did not become healthy in time"',
        'exit 1'
    ) -join "`n"
    Invoke-RemoteCommand -Command $healthCheckCmd -TimeOut 120

    # ============================================================================
    # Step 6: 完成
    # ============================================================================
    Write-Section "升级完成"
    Write-Host "ai-service 已成功升级并运行" -ForegroundColor Green
    Write-Host "健康检查地址：http://${ServerIP}:8000/health" -ForegroundColor White
    Write-Host "测试接口：curl -X POST http://${ServerIP}:8000/api/ai-assistant/chat -H 'Content-Type: application/json' -d '{\"session_id\":\"sess_upgrade_test\",\"message\":\"新手游泳装备推荐\"}'" -ForegroundColor White
}
finally {
    if ($tempTar -and (Test-Path $tempTar)) {
        Remove-Item -Force $tempTar -ErrorAction SilentlyContinue
    }
    try {
        Get-SSHSession | Remove-SSHSession | Out-Null
    }
    catch {
        Write-Warning "清理 SSH 会话时出错：$_"
    }
}
