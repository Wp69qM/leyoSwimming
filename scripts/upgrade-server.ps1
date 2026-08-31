#!/usr/bin/env pwsh
#requires -Version 5.1

<#
.SYNOPSIS
    leyoSwimming 服务器升级脚本（适用于已完成首次部署后的增量升级）。
.DESCRIPTION
    根据变更模块重新构建本地产物，上传至已部署的 Ubuntu 服务器，并重启对应服务。
    支持按需升级：backend / web-admin / miniapp-user / miniapp-coach / ai-service。
.EXAMPLE
    .\scripts\upgrade-server.ps1 -ServerIP '123.45.67.89'
    检测所有模块并按需升级（默认行为）。
.EXAMPLE
    .\scripts\upgrade-server.ps1 -ServerIP '123.45.67.89' -Modules backend,web-admin -ForceUpload
    仅升级后端与管理后台，且自动覆盖服务器上的旧产物。
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory, HelpMessage = "目标服务器 IP")]
    [string]$ServerIP,

    [string]$ServerUser = "root",

    [string]$RemotePath = "/opt/leyo-swimming",

    [string]$ProjectRoot = "$PSScriptRoot\..",

    [string]$ApiProtocol = "http",

    [string]$Modules = "all",

    [switch]$SkipBuild,

    [switch]$SkipUpload,

    [switch]$ForceUpload,

    [switch]$ReinstallAIDeps,

    [string]$HostKeyFingerprint = "",

    [switch]$AcceptHostKey,

    [string]$PyPIIndexUrl = "https://pypi.org/simple",

    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

if ($ApiProtocol -notin @("http", "https")) {
    throw "ApiProtocol 必须是 http 或 https"
}

if ($PyPIIndexUrl -notmatch '^https?://') {
    throw "PyPIIndexUrl 必须以 http:// 或 https:// 开头"
}
$unsafeUrlChars = @(';', '|', '&', '$', '"', "`n", "`r", '<', '>', '`', '(', ')', "'")
if ($PyPIIndexUrl.IndexOfAny($unsafeUrlChars) -ge 0) {
    throw "PyPIIndexUrl 包含不安全的字符：$PyPIIndexUrl"
}

$script:sshCred = $null
$KeepBackups = 3

# -------------------------------------------------
# 通用工具函数
# -------------------------------------------------
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
    $script:sshCred = New-Object System.Management.Automation.PSCredential($ServerUser, $securePassword)
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
        throw "没有可用的 SSH 会话，请先建立连接。"
    }
    $result = Invoke-SSHCommand -SSHSession $session -Command $Command -TimeOut $TimeOut
    if (-not $IgnoreExitCode -and $result.ExitStatus -ne 0) {
        $trimmedOutput = ($result.Output | Out-String).Trim()
        if ($trimmedOutput.Length -gt 2000) {
            $trimmedOutput = $trimmedOutput.Substring(0, 2000) + "`n...（输出已截断）"
        }
        throw "远程命令执行失败 (ExitStatus=$($result.ExitStatus))：`n$Command`n输出：`n$trimmedOutput"
    }
    return $result
}

function Write-Section {
    param([string]$Message)
    Write-Host "`n========================================" -ForegroundColor Green
    Write-Host $Message -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
}

function Get-PackageManager {
    param([string]$ModuleRoot)
    if (Test-Path (Join-Path $ModuleRoot "yarn.lock")) { return "yarn" }
    if (Test-Path (Join-Path $ModuleRoot "package-lock.json")) { return "npm" }
    if (Test-Path (Join-Path $ModuleRoot "pnpm-lock.yaml")) { return "pnpm" }
    return "yarn"
}

function Invoke-FrontendInstall {
    param(
        [string]$ModuleRoot,
        [string]$Manager
    )
    Push-Location $ModuleRoot
    try {
        switch ($Manager) {
            "npm" { & npm install }
            "pnpm" { & pnpm install }
            default { & yarn install }
        }
        if ($LASTEXITCODE -ne 0) { throw "$Manager install 失败" }
    }
    finally {
        Pop-Location
    }
}

function Get-MavenWrapper {
    if ($IsWindows -or -not (Get-Variable IsWindows -ErrorAction SilentlyContinue)) {
        return ".\mvnw.cmd"
    }
    return "./mvnw"
}

function ConvertTo-ShellLiteral {
    <#
    .SYNOPSIS
        将字符串转义为安全的单引号 shell 字面量（bash 风格：'\''）。
    #>
    param([string]$Value)
    return "'" + ($Value -replace "'", "'\''") + "'"
}

function Test-ShellSafePath {
    <#
    .SYNOPSIS
        检查路径是否只包含安全的 shell 路径字符，并阻止相对路径/路径遍历。
    #>
    param([string]$Path)
    $unsafeChars = @(';', '|', '&', '$', '"', "`n", "`r", '<', '>', '`', '(', ')', '\', "'")
    if ($Path.IndexOfAny($unsafeChars) -ge 0) {
        throw "路径包含不安全的 shell 字符：$Path"
    }
    if ($Path -notmatch "^/") {
        throw "RemotePath 必须是服务器上的绝对路径（以 / 开头）：$Path"
    }
    if ($Path -match "\.\.") {
        throw "RemotePath 不能包含路径遍历（..）：$Path"
    }
}

function Get-RemoteLiteral {
    <#
    .SYNOPSIS
        返回远程服务器上相对于 RemotePath 的 shell 安全字面量路径。
    #>
    param([string]$RelativePath = "")
    $fullPath = if ($RelativePath) { "$RemotePath/$RelativePath" } else { $RemotePath }
    return ConvertTo-ShellLiteral $fullPath
}

# -------------------------------------------------
# 模块定义：产物路径、构建命令、上传目标
# -------------------------------------------------
$moduleDefinitions = @{
    "backend" = @{
        Name = "后端 Spring Boot"
        ArtifactPath = "backend\target\leyo-swimming-backend-*.jar"
        RemoteName = "leyo-swimming-backend.jar"
        BuildScript = {
            Push-Location "$ProjectRoot\backend"
            try {
                $mvnw = Get-MavenWrapper
                & $mvnw clean package -DskipTests -B
                if ($LASTEXITCODE -ne 0) { throw "后端 Maven 构建失败" }
                $jarFiles = Get-ChildItem "target\leyo-swimming-backend-*.jar" -ErrorAction SilentlyContinue
                if (-not $jarFiles) {
                    throw "后端 jar 产物不存在"
                }
                Write-Host "  检测到后端产物：$($jarFiles[0].Name)" -ForegroundColor Gray
            }
            finally {
                Pop-Location
            }
        }
    }
    "web-admin" = @{
        Name = "管理后台 Web Admin"
        ArtifactPath = "web-admin\dist"
        RemoteName = "web-admin-dist"
        BuildScript = {
            Push-Location "$ProjectRoot\web-admin"
            $previousEnv = $env:VITE_API_BASE_URL
            try {
                $mgr = Get-PackageManager -ModuleRoot "$ProjectRoot\web-admin"
                Invoke-FrontendInstall -ModuleRoot "$ProjectRoot\web-admin" -Manager $mgr
                $env:VITE_API_BASE_URL = "$($ApiProtocol)://$($ServerIP)/api"
                switch ($mgr) {
                    "npm" { & npm run build }
                    "pnpm" { & pnpm run build }
                    default { & yarn build }
                }
                if ($LASTEXITCODE -ne 0) { throw "管理后台构建失败" }
            }
            finally {
                $env:VITE_API_BASE_URL = $previousEnv
                Pop-Location
            }
        }
    }
    "miniapp-user" = @{
        Name = "用户端 H5"
        ArtifactPath = "miniapp-user\h5-user-dist"
        RemoteName = "h5-user-dist"
        BuildScript = {
            Push-Location "$ProjectRoot\miniapp-user"
            $previousEnv = $env:TARO_APP_API_BASE_URL
            try {
                $mgr = Get-PackageManager -ModuleRoot "$ProjectRoot\miniapp-user"
                Invoke-FrontendInstall -ModuleRoot "$ProjectRoot\miniapp-user" -Manager $mgr
                $env:TARO_APP_API_BASE_URL = "$($ApiProtocol)://$($ServerIP)/api"
                switch ($mgr) {
                    "npm" { & npm run build:h5 }
                    "pnpm" { & pnpm run build:h5 }
                    default { & yarn build:h5 }
                }
                if ($LASTEXITCODE -ne 0) { throw "用户端 H5 构建失败" }
                if (Test-Path "h5-user-dist") { Remove-Item -Recurse -Force "h5-user-dist" }
                Rename-Item -Path "dist" -NewName "h5-user-dist"
            }
            finally {
                $env:TARO_APP_API_BASE_URL = $previousEnv
                Pop-Location
            }
        }
    }
    "miniapp-coach" = @{
        Name = "教练端 H5"
        ArtifactPath = "miniapp-coach\h5-coach-dist"
        RemoteName = "h5-coach-dist"
        BuildScript = {
            Push-Location "$ProjectRoot\miniapp-coach"
            $previousEnv = $env:TARO_APP_API_BASE_URL
            try {
                $mgr = Get-PackageManager -ModuleRoot "$ProjectRoot\miniapp-coach"
                Invoke-FrontendInstall -ModuleRoot "$ProjectRoot\miniapp-coach" -Manager $mgr
                $env:TARO_APP_API_BASE_URL = "$($ApiProtocol)://$($ServerIP)/api"
                switch ($mgr) {
                    "npm" { & npm run build:h5 }
                    "pnpm" { & pnpm run build:h5 }
                    default { & yarn build:h5 }
                }
                if ($LASTEXITCODE -ne 0) { throw "教练端 H5 构建失败" }
                if (Test-Path "h5-coach-dist") { Remove-Item -Recurse -Force "h5-coach-dist" }
                Rename-Item -Path "dist" -NewName "h5-coach-dist"
            }
            finally {
                $env:TARO_APP_API_BASE_URL = $previousEnv
                Pop-Location
            }
        }
    }
    "ai-service" = @{
        Name = "AI 服务"
        ArtifactPath = "ai-service"
        RemoteName = "ai-service"
        BuildScript = {
            Write-Host "AI 服务无需本地编译，将直接上传源码目录。" -ForegroundColor Gray
        }
    }
}

# -------------------------------------------------
# 前置检查
# -------------------------------------------------

Test-ShellSafePath -Path $RemotePath
$remotePathLit = ConvertTo-ShellLiteral $RemotePath

try {
Write-Section "Step 0: 前置检查"

if ([string]::IsNullOrWhiteSpace($ServerIP)) {
    throw "必须指定服务器 IP：-ServerIP '123.45.67.89'"
}
$ipv4Pattern = '^(?:(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\.){3}(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)$'
$ipv6Pattern = '^\[(?:[0-9a-fA-F:]+)\]$'
$fqdnPattern = '^[a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?)*$'
if ($ServerIP -notmatch $ipv4Pattern -and $ServerIP -notmatch $ipv6Pattern -and $ServerIP -notmatch $fqdnPattern) {
    throw "ServerIP 格式不合法（应为 IPv4、IPv6 或域名）：$ServerIP"
}

if (-not (Test-CommandExists "ssh")) { throw "未找到 ssh.exe，请安装 OpenSSH 客户端。" }
if (-not (Test-CommandExists "scp")) { throw "未找到 scp.exe，请安装 OpenSSH 客户端。" }
if (-not (Test-CommandExists "tar")) { throw "未找到 tar，升级脚本依赖 tar 打包目录。" }

Install-PoshSSH
Get-SecureCredential

Write-Host "测试 SSH 连接到 ${ServerUser}@${ServerIP} ..." -ForegroundColor Cyan
if ($HostKeyFingerprint) {
    Write-Host "使用指定的主机密钥指纹进行校验 ..." -ForegroundColor Cyan
    $session = New-SSHSession -ComputerName $ServerIP -Credential $script:sshCred -HostKey $HostKeyFingerprint
}
elseif ($AcceptHostKey) {
    Write-Host "警告：启用 -AcceptHostKey，将自动接受服务器 SSH 主机密钥（存在中间人攻击风险）。" -ForegroundColor Yellow
    $session = New-SSHSession -ComputerName $ServerIP -Credential $script:sshCred -AcceptKey
}
else {
    Write-Host "警告：未指定 -HostKeyFingerprint，将自动接受服务器 SSH 主机密钥（存在中间人攻击风险）。" -ForegroundColor Yellow
    $session = New-SSHSession -ComputerName $ServerIP -Credential $script:sshCred -AcceptKey
}
if (-not $session) { throw "SSH 连接失败" }
Write-Host "SSH 连接成功" -ForegroundColor Green

# 确认服务器已部署
$deployExists = Invoke-RemoteCommand -Command "test -f $(Get-RemoteLiteral 'deploy/docker-compose.prod.yml') && echo YES || echo NO" -IgnoreExitCode
if ($deployExists.Output.Trim() -ne "YES") {
    throw "服务器上未检测到 $(Get-RemoteLiteral 'deploy/docker-compose.prod.yml')，请先运行 deploy-first-stage.ps1 完成首次部署。"
}

# 确定 docker compose 文件组合
$script:composeFiles = "-f docker-compose.prod.yml"
$script:aiDirectMode = $false
$aiDirectExists = Invoke-RemoteCommand -Command "test -f $(Get-RemoteLiteral 'deploy/docker-compose.ai-direct.yml') && echo YES || echo NO" -IgnoreExitCode
if ($aiDirectExists.Output.Trim() -eq "YES") {
    $script:composeFiles = "-f docker-compose.prod.yml -f docker-compose.ai-direct.yml"
    $script:aiDirectMode = $true
}
Write-Host "检测到部署模式：$(if ($script:aiDirectMode) { 'AI 服务宿主机直接运行' } else { 'AI 服务 Docker 运行' })" -ForegroundColor Gray

# 解析模块列表（支持逗号分隔，如 backend,web-admin）
$validModuleNames = @("all") + $moduleDefinitions.Keys
$moduleList = $Modules -split ',' | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne "" }
$invalidModules = $moduleList | Where-Object { $_ -notin $validModuleNames }
if ($invalidModules.Count -gt 0) {
    throw "无效的模块：$($invalidModules -join ', ')。有效值为：$($validModuleNames -join ', ')"
}
$selectedModules = if ($moduleList -contains "all") { $moduleDefinitions.Keys } else { $moduleList }
$selectedModules = $selectedModules | Select-Object -Unique | Sort-Object

Write-Host "`n本次计划升级的模块：" -ForegroundColor Cyan
foreach ($m in $selectedModules) {
    Write-Host "  - $($moduleDefinitions[$m].Name)" -ForegroundColor White
}

if ($DryRun) {
    Write-Host "`nDryRun 模式：仅显示计划，不执行实际操作。" -ForegroundColor Yellow
    Get-SSHSession | Remove-SSHSession | Out-Null
    exit 0
}

# -------------------------------------------------
# 本地构建
# -------------------------------------------------
if (-not $SkipBuild) {
    Write-Section "Step 1: 本地构建"

    foreach ($m in $selectedModules) {
        $def = $moduleDefinitions[$m]
        Write-Host "构建 $($def.Name) ..." -ForegroundColor Cyan
        & $def.BuildScript
        Write-Host "  $($def.Name) 构建完成" -ForegroundColor Green
    }
}
else {
    Write-Host "跳过本地构建（-SkipBuild）" -ForegroundColor Yellow
}

# -------------------------------------------------
# 上传到服务器
# -------------------------------------------------
if (-not $SkipUpload) {
    Write-Section "Step 2: 上传到服务器"

    $uploadItems = foreach ($m in $selectedModules) {
        $def = $moduleDefinitions[$m]
        $artifactPath = $def.ArtifactPath
        if ($artifactPath -match '\*|\?') {
            $resolved = Resolve-Path (Join-Path $ProjectRoot $artifactPath) -ErrorAction SilentlyContinue | Select-Object -First 1
            if (-not $resolved) {
                throw "上传失败，未找到匹配的本地产物：$artifactPath（模块：$m）"
            }
            $localPath = $resolved.Path
        }
        else {
            $localPath = Join-Path $ProjectRoot $artifactPath
        }
        if (-not (Test-Path $localPath)) {
            throw "上传失败，本地产物不存在：$localPath（模块：$m）"
        }
        [PSCustomObject]@{
            Module = $m
            LocalPath = $localPath
            RemoteParent = "$RemotePath/"
            NewName = $def.RemoteName
        }
    }

    $swapQueue = @()

    foreach ($item in $uploadItems) {
        $localPath = $item.LocalPath
        $remoteParent = $item.RemoteParent
        $newName = $item.NewName
        $remoteTarget = "$remoteParent$newName"
        $remoteTargetLit = ConvertTo-ShellLiteral $remoteTarget
        $remoteParentLit = ConvertTo-ShellLiteral $remoteParent
        $tmpName = "$newName.tmp"
        $tmpTarget = "$remoteParent$tmpName"
        $tmpTargetLit = ConvertTo-ShellLiteral $tmpTarget

        $existsCheck = Invoke-RemoteCommand -Command "test -e $remoteTargetLit && echo YES || echo NO" -IgnoreExitCode
        $exists = $existsCheck.Output.Trim() -eq "YES"

        $shouldUpload = $true
        if ($exists -and -not $ForceUpload) {
            Write-Host "服务器上已存在：$remoteTarget" -ForegroundColor Yellow
            $reupload = Read-Host "  是否覆盖上传 $newName？(y/N，默认 N)"
            if ($reupload -notmatch '^(y|Y|yes|YES)$') {
                $shouldUpload = $false
            }
        }
        elseif ($exists -and $ForceUpload) {
            Write-Host "强制覆盖：$remoteTarget" -ForegroundColor Yellow
        }

        if (-not $shouldUpload) {
            Write-Host "  跳过上传：$remoteTarget" -ForegroundColor Gray
            continue
        }

        Write-Host "上传 $localPath -> ${ServerUser}@${ServerIP}:$remoteTarget (临时路径：$tmpTarget)" -ForegroundColor Cyan

        # 先上传到 .tmp 路径；失败时不会破坏现有产物
        Invoke-RemoteCommand -Command "mkdir -p $remoteParentLit && rm -rf $tmpTargetLit" | Out-Null

        # 复用与 SSH 会话相同的主机密钥校验策略，防止 SCP 链路被中间人劫持
        $scpBaseParams = @{
            ComputerName = $ServerIP
            Credential   = $script:sshCred
        }
        if ($HostKeyFingerprint) {
            $scpBaseParams['HostKey'] = $HostKeyFingerprint
        }
        elseif ($AcceptHostKey) {
            $scpBaseParams['AcceptKey'] = $true
        }

        $isDirectory = (Get-Item $localPath).PSIsContainer
        if ($isDirectory) {
            $sourceLeaf = Split-Path -Leaf $localPath
            $tarName = "$newName.tar.gz"
            $tmpTarName = "$tarName.tmp"
            $tempTar = Join-Path $env:TEMP $tarName
            if (Test-Path $tempTar) { Remove-Item -Force $tempTar }
            $parentDir = Split-Path -Parent $localPath

            & tar -czf $tempTar -C $parentDir $sourceLeaf
            if ($LASTEXITCODE -ne 0) { throw "打包目录失败：$localPath" }

            Set-SCPItem @scpBaseParams `
                -Path $tempTar -Destination $remoteParent -NewName $tmpTarName
            Remove-Item -Force $tempTar

            # 目录产物：先解压到唯一 staging 目录，再用 mv -T 做原子替换；失败时尝试回滚
            $stagingName = "$sourceLeaf.staging.$(Get-Date -Format 'yyyyMMddHHmmss')"
            $stagingNameLit = ConvertTo-ShellLiteral $stagingName
            $stagingTargetLit = ConvertTo-ShellLiteral "$stagingName/$sourceLeaf"

            $extractCmd = @(
                "cd $remoteParentLit",
                "rm -rf $stagingNameLit && mkdir -p $stagingNameLit",
                "tar -xzf $(ConvertTo-ShellLiteral $tmpTarName) -C $stagingNameLit",
                "rm -f $(ConvertTo-ShellLiteral $tmpTarName)"
            ) -join " && "
            $backupName = ConvertTo-ShellLiteral "$remoteTarget.backup.$(Get-Date -Format 'yyyyMMddHHmmss')"
            $swapCmd = "$extractCmd && if [ -e $remoteTargetLit ]; then mv -T $remoteTargetLit $backupName; fi && mv -T $stagingTargetLit $remoteTargetLit || { if [ -e $backupName ]; then mv -T $backupName $remoteTargetLit; fi; exit 1; }"
            $backupCleanup = "cd $remoteParentLit && ls -1td $(ConvertTo-ShellLiteral "$newName.backup.")* 2>/dev/null | tail -n +$($KeepBackups + 1) | xargs -r rm -rf"
            $swapQueue += [PSCustomObject]@{
                NewName = $newName
                RemoteTarget = $remoteTarget
                Command = $swapCmd
                Cleanup = $backupCleanup
            }
        }
        else {
            Set-SCPItem @scpBaseParams `
                -Path $localPath -Destination $remoteParent -NewName $tmpName

            $backupName = ConvertTo-ShellLiteral "$remoteTarget.backup.$(Get-Date -Format 'yyyyMMddHHmmss')"
            $swapCmd = "if [ -e $remoteTargetLit ]; then mv -T $remoteTargetLit $backupName; fi && mv -T $tmpTargetLit $remoteTargetLit || { if [ -e $backupName ]; then mv -T $backupName $remoteTargetLit; fi; exit 1; }"
            $backupCleanup = "cd $remoteParentLit && ls -1td $(ConvertTo-ShellLiteral "$newName.backup.")* 2>/dev/null | tail -n +$($KeepBackups + 1) | xargs -r rm -rf"
            $swapQueue += [PSCustomObject]@{
                NewName = $newName
                RemoteTarget = $remoteTarget
                Command = $swapCmd
                Cleanup = $backupCleanup
            }
        }
    }

    if ($swapQueue.Count -gt 0) {
        Write-Host "`n开始原子切换产物到正式路径 ..." -ForegroundColor Cyan
        foreach ($swap in $swapQueue) {
            Write-Host "  切换 $($swap.NewName) -> $($swap.RemoteTarget)" -ForegroundColor Gray
            Invoke-RemoteCommand -Command $swap.Command -TimeOut 120 | Out-Null
            Write-Host "  清理旧备份（保留最近 $KeepBackups 个）" -ForegroundColor Gray
            Invoke-RemoteCommand -Command $swap.Cleanup -TimeOut 60 -IgnoreExitCode | Out-Null
        }
    }
}
else {
    Write-Host "跳过上传（-SkipUpload）" -ForegroundColor Yellow
}

# -------------------------------------------------
# 重启服务
# -------------------------------------------------
Write-Section "Step 3: 重启服务"

$restartBackend = $selectedModules -contains "backend"
$restartNginx = $selectedModules -contains "web-admin" -or
                $selectedModules -contains "miniapp-user" -or
                $selectedModules -contains "miniapp-coach"
$restartAI = $selectedModules -contains "ai-service"

if ($restartBackend) {
    Write-Host "重启后端容器 ..." -ForegroundColor Cyan
    Invoke-RemoteCommand -Command "cd $remotePathLit/deploy && docker compose $script:composeFiles up -d backend 2>&1" -TimeOut 120 | Out-Null
    Write-Host "  后端容器已重启" -ForegroundColor Green
}

if ($restartNginx) {
    Write-Host "重新加载 Nginx 配置 ..." -ForegroundColor Cyan
    Invoke-RemoteCommand -Command "cd $remotePathLit/deploy && docker compose $script:composeFiles exec -T nginx nginx -s reload 2>&1 || docker compose $script:composeFiles restart nginx 2>&1" -TimeOut 60 | Out-Null
    Write-Host "  Nginx 已重载" -ForegroundColor Green
}

if ($restartAI) {
    Write-Host "重启 AI 服务 ..." -ForegroundColor Cyan
    if ($script:aiDirectMode) {
        $aiRestartCmds = @(
            "cd $remotePathLit/ai-service"
        )
        if ($ReinstallAIDeps) {
            $aiRestartCmds += "source $remotePathLit/ai-service-venv/bin/activate && pip install -r requirements.txt -i $(ConvertTo-ShellLiteral $PyPIIndexUrl) 2>&1"
        }
        $aiRestartCmds += "systemctl restart leyo-ai 2>&1"
        Invoke-RemoteCommand -Command ($aiRestartCmds -join " && ") -TimeOut 120 | Out-Null
    }
    else {
        Invoke-RemoteCommand -Command "cd $remotePathLit/deploy && docker compose $script:composeFiles up -d ai-service 2>&1" -TimeOut 120 | Out-Null
    }
    Write-Host "  AI 服务已重启" -ForegroundColor Green
}

# -------------------------------------------------
# 健康检查
# -------------------------------------------------
Write-Section "Step 4: 健康检查"

if ($restartBackend) {
    Write-Host "等待后端服务就绪 ..." -ForegroundColor Cyan
    $waitBackendCmds = @(
        'i=1',
        'while [ $i -le 30 ]; do',
        '  if wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health 2>/dev/null || curl -fsS http://localhost:8080/actuator/health >/dev/null 2>&1; then',
        '    echo "Backend is healthy"',
        '    exit 0',
        '  fi',
        '  echo "Waiting for backend... $i/30"',
        '  sleep 5',
        '  i=$((i+1))',
        'done',
        'echo "Backend did not become healthy in time"',
        'exit 1'
    )
    Invoke-RemoteCommand -Command ($waitBackendCmds -join "`n") -TimeOut 180
}

if ($restartAI) {
    Write-Host "等待 AI 服务就绪 ..." -ForegroundColor Cyan
    $waitAICmds = @(
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
    )
    Invoke-RemoteCommand -Command ($waitAICmds -join "`n") -TimeOut 120
}

# -------------------------------------------------
# 完成
# -------------------------------------------------
Write-Section "升级完成"

Write-Host "访问地址：" -ForegroundColor Green
Write-Host "  管理后台： http://$ServerIP/admin/" -ForegroundColor White
Write-Host "  用户端 H5： http://$ServerIP/h5/user/" -ForegroundColor White
Write-Host "  教练端 H5： http://$ServerIP/h5/coach/" -ForegroundColor White
Write-Host "  API 接口：  $($ApiProtocol)://$($ServerIP)/api/" -ForegroundColor White
}
finally {
    try {
        Get-SSHSession | Remove-SSHSession | Out-Null
    }
    catch {
        Write-Warning "清理 SSH 会话时出错：$_"
    }
}
