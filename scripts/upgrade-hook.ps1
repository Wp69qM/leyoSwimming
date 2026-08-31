#!/usr/bin/env pwsh
#requires -Version 5.1

<#
.SYNOPSIS
    leyoSwimming 升级触发器（Hook）：检测代码变更并自动调用升级脚本。
.DESCRIPTION
    通过 git diff 识别自上次部署以来发生变更的业务模块，输出提醒，
    并在用户确认（或 -AutoDeploy）后调用 upgrade-server.ps1 完成构建、上传与重启。
    支持手动运行，也支持注册为 git post-commit hook。
.EXAMPLE
    .\scripts\upgrade-hook.ps1 -ServerIP '123.45.67.89'
    检测变更并询问是否升级。
.EXAMPLE
    .\scripts\upgrade-hook.ps1 -AutoDeploy
    若设置了环境变量 LEYO_UPGRADE_SERVER_IP，则自动部署变更模块。
#>

[CmdletBinding()]
param(
    [string]$ServerIP = "",

    [string]$SinceCommit = "",

    [string]$ProjectRoot = "$PSScriptRoot\..",

    [switch]$AutoDeploy,

    [switch]$ForceUpload,

    [switch]$ReinstallAIDeps,

    [string]$HostKeyFingerprint = "",

    [switch]$AcceptHostKey
)

$ErrorActionPreference = "Stop"

$script:LastDeployedCommitFile = Join-Path $ProjectRoot ".last-deployed-commit"

# -------------------------------------------------
# 工具函数
# -------------------------------------------------
function Write-Section {
    param([string]$Message)
    Write-Host "`n========================================" -ForegroundColor Green
    Write-Host $Message -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
}

function Get-CurrentCommit {
    try {
        $hash = git rev-parse HEAD 2>$null
        if ($LASTEXITCODE -eq 0 -and $hash) { return $hash.Trim() }
    }
    catch {
        # ignore
    }
    return $null
}

function Get-LastDeployedCommit {
    if (Test-Path $script:LastDeployedCommitFile) {
        return (Get-Content $script:LastDeployedCommitFile -Raw).Trim()
    }
    return $null
}

function Test-GitCommit {
    param([string]$Commit)
    if (-not $Commit) { return $false }
    $verify = git rev-parse --verify -- "$Commit" 2>$null
    return ($LASTEXITCODE -eq 0 -and $verify)
}

function Get-GitChangedFiles {
    param([string]$SinceCommit)

    try {
        $quiet = git diff --quiet "$SinceCommit" HEAD 2>$null
        if ($LASTEXITCODE -eq 0) { return @() }
        if ($LASTEXITCODE -ne 1) {
            throw "git diff --quiet 失败 (exit $LASTEXITCODE)"
        }
        $output = git diff --name-only "$SinceCommit" HEAD 2>$null
        if ($LASTEXITCODE -ne 0) {
            throw "git diff --name-only 失败 (exit $LASTEXITCODE)"
        }
        return $output -split "`r?`n" | Where-Object { $_.Trim() -ne "" }
    }
    catch {
        Write-Warning "无法执行 git diff：$_"
        return @()
    }
}

function Get-ChangedModules {
    param([string[]]$ChangedFiles)

    # 模块路径映射：新增模块时只需修改此处
    $modulePatterns = [ordered]@{
        "backend"       = @("backend/*")
        "miniapp-user"  = @("miniapp-user/src/*", "miniapp-user/config/*", "miniapp-user/package.json")
        "miniapp-coach" = @("miniapp-coach/src/*", "miniapp-coach/config/*", "miniapp-coach/package.json")
        "web-admin"     = @("web-admin/src/*", "web-admin/config/*", "web-admin/*.config.*", "web-admin/package.json")
        "ai-service"    = @("ai-service/app/*", "ai-service/requirements.txt", "ai-service/Dockerfile")
    }
    $frontendModules = @("web-admin", "miniapp-user", "miniapp-coach")

    $modules = @()
    $configChanged = $false
    $sharedChanged = $false

    foreach ($file in $ChangedFiles) {
        $normalized = $file -replace "\\", "/"

        $matched = $false
        foreach ($entry in $modulePatterns.GetEnumerator()) {
            foreach ($pattern in $entry.Value) {
                if ($normalized -like $pattern) {
                    $modules += $entry.Key
                    $matched = $true
                    break
                }
            }
            if ($matched) { break }
        }

        if (-not $matched) {
            if ($normalized -like "shared/*") {
                $sharedChanged = $true
            }
            elseif ($normalized -like "deploy/*") {
                $configChanged = $true
            }
            elseif ($normalized -like "scripts/upgrade*") {
                # 升级脚本本身变更不影响业务产物
            }
        }
    }

    # shared 变更会影响所有前端产物（shared 为 TS 公共包）
    if ($sharedChanged) {
        $modules += $frontendModules
        Write-Host "检测到 shared/ 公共包变更，将联动升级所有前端模块。" -ForegroundColor Yellow
    }

    $uniqueModules = $modules | Select-Object -Unique | Sort-Object

    return [PSCustomObject]@{
        Modules = $uniqueModules
        ConfigChanged = $configChanged
    }
}

function Update-LastDeployedCommit {
    param([string]$CommitHash)
    $CommitHash | Out-File -FilePath $script:LastDeployedCommitFile -Encoding UTF8 -NoNewline
    Write-Host "已记录本次部署 commit：$CommitHash" -ForegroundColor Green
}

function Test-InteractiveShell {
    # Git hook 等场景通常重定向了 stdin/stdout，Read-Host 会立即返回空值
    return [Environment]::UserInteractive `
        -and -not [Console]::IsInputRedirected `
        -and -not [Console]::IsOutputRedirected
}

# -------------------------------------------------
# 主流程
# -------------------------------------------------
Write-Section "leyoSwimming 升级触发器"

$currentCommit = Get-CurrentCommit
if (-not $currentCommit) {
    throw "无法获取当前 commit，请确认当前目录是 git 仓库且已安装 git。"
}
Write-Host "当前 commit：$currentCommit" -ForegroundColor Gray

$sinceCommit = $null

if ($SinceCommit) {
    if (-not (Test-GitCommit -Commit $SinceCommit)) {
        throw "SinceCommit 不是有效的 git commit-ish：$SinceCommit"
    }
    $sinceCommit = $SinceCommit
}
else {
    $lastDeployed = Get-LastDeployedCommit
    if ($lastDeployed -and (Test-GitCommit -Commit $lastDeployed)) {
        $sinceCommit = $lastDeployed
    }
    else {
        if ($lastDeployed) {
            Write-Warning ".last-deployed-commit 内容无效（$lastDeployed），将使用 HEAD~1 作为对比基准。"
        }
        else {
            Write-Host "未找到 .last-deployed-commit，默认对比上一次 commit (HEAD~1)。" -ForegroundColor Yellow
        }
        $sinceCommit = "HEAD~1"
    }
}

Write-Host "对比范围：$sinceCommit..HEAD" -ForegroundColor Gray

$changedFiles = Get-GitChangedFiles -SinceCommit $sinceCommit
if ($changedFiles.Count -eq 0) {
    Write-Host "`n未检测到代码变更，无需升级。" -ForegroundColor Green
    exit 0
}

Write-Host "`n检测到变更文件（共 $($changedFiles.Count) 个）：" -ForegroundColor Cyan
$changedFiles | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }

$analysis = Get-ChangedModules -ChangedFiles $changedFiles
$changedModules = $analysis.Modules

if ($analysis.ConfigChanged) {
    Write-Host "`n注意：deploy/ 目录下的配置发生变更，upgrade-server.ps1 默认不上传部署配置。" -ForegroundColor Red
    Write-Host "如需要更新 docker-compose / nginx 配置，请使用 deploy-first-stage.ps1 或手动同步。" -ForegroundColor Red
}

if ($changedModules.Count -eq 0) {
    Write-Host "`n检测到的变更不需要重新编译业务产物（可能是文档/脚本），无需升级。" -ForegroundColor Green
    exit 0
}

Write-Host "`n需要升级的模块：" -ForegroundColor Cyan
$changedModules | ForEach-Object { Write-Host "  - $_" -ForegroundColor White }

# 确定服务器 IP
$targetServerIP = $ServerIP
if (-not $targetServerIP) {
    $targetServerIP = [System.Environment]::GetEnvironmentVariable("LEYO_UPGRADE_SERVER_IP")
}
if (-not $targetServerIP) {
    Write-Host "`n未通过参数或环境变量 LEYO_UPGRADE_SERVER_IP 指定服务器 IP。" -ForegroundColor Yellow
    if (Test-InteractiveShell) {
        $targetServerIP = Read-Host "请输入要升级的服务器 IP（直接回车则仅提醒，不执行部署）"
    }
    else {
        Write-Host "当前处于非交互式环境（如 Git hook），无法提示输入，跳过部署。" -ForegroundColor Yellow
    }
}

if ([string]::IsNullOrWhiteSpace($targetServerIP)) {
    Write-Host "`n未提供服务器 IP，跳过部署。" -ForegroundColor Yellow
    Write-Host "后续可手动执行：" -ForegroundColor Yellow
    Write-Host "  .\scripts\upgrade-server.ps1 -ServerIP '<IP>' -Modules $($changedModules -join ',')" -ForegroundColor White
    exit 0
}

# 确认是否部署
if ($AutoDeploy) {
    $autoDeployEnabled = [System.Environment]::GetEnvironmentVariable("LEYO_UPGRADE_AUTO_DEPLOY")
    if ($autoDeployEnabled -ne "1") {
        Write-Warning "启用 -AutoDeploy 但未设置环境变量 LEYO_UPGRADE_AUTO_DEPLOY=1，将转为交互式确认。"
        $AutoDeploy = $false
    }
}

if (-not $AutoDeploy) {
    Write-Host "`n准备使用升级脚本部署到服务器：$targetServerIP" -ForegroundColor Cyan
    if (Test-InteractiveShell) {
        $confirm = Read-Host "是否立即执行升级？(y/N，默认 N)"
        if ($confirm -notmatch '^(y|Y|yes|YES)$') {
            Write-Host "已取消部署。" -ForegroundColor Yellow
            exit 0
        }
    }
    else {
        Write-Host "当前处于非交互式环境（如 Git hook），未启用 -AutoDeploy，跳过部署。" -ForegroundColor Yellow
        Write-Host "如需自动部署，请设置环境变量 LEYO_UPGRADE_AUTO_DEPLOY=1 或手动执行：" -ForegroundColor Yellow
        Write-Host "  .\scripts\upgrade-server.ps1 -ServerIP '$targetServerIP' -Modules $($changedModules -join ',')" -ForegroundColor White
        exit 0
    }
}

# 执行升级
$upgradeScript = Join-Path $ProjectRoot "scripts\upgrade-server.ps1"
if (-not (Test-Path $upgradeScript)) {
    throw "升级脚本不存在：$upgradeScript"
}

# 优先使用当前 PowerShell 进程路径，兼容 Windows PowerShell 5.1 与 PowerShell 7
$powerShellExe = (Get-Process -Id $PID).Path
if (-not $powerShellExe -or -not (Test-Path $powerShellExe)) {
    $powerShellExe = if (Get-Command pwsh -ErrorAction SilentlyContinue) { "pwsh" } else { "powershell" }
}

# 主机密钥校验参数：优先参数，其次环境变量 LEYO_UPGRADE_HOST_KEY
$effectiveHostKey = $HostKeyFingerprint
if (-not $effectiveHostKey) {
    $effectiveHostKey = [System.Environment]::GetEnvironmentVariable("LEYO_UPGRADE_HOST_KEY")
}

$upgradeArgs = @{
    FilePath = $powerShellExe
    ArgumentList = @(
        "-ExecutionPolicy", "Bypass",
        "-NoProfile",
        "-File", $upgradeScript,
        "-ServerIP", $targetServerIP
    )
    Wait = $true
    NoNewWindow = $true
}

# upgrade-server.ps1 的 -Modules 现在按逗号分隔的字符串解析，
# 避免 PowerShell 5.1 中 Start-Process 多次传入同名参数导致 ParameterAlreadyBound。
$upgradeArgs.ArgumentList += @("-Modules", ($changedModules -join ','))
if ($ForceUpload) { $upgradeArgs.ArgumentList += "-ForceUpload" }
if ($ReinstallAIDeps) { $upgradeArgs.ArgumentList += "-ReinstallAIDeps" }
if ($effectiveHostKey) {
    $upgradeArgs.ArgumentList += @("-HostKeyFingerprint", $effectiveHostKey)
}
elseif ($AcceptHostKey) {
    $upgradeArgs.ArgumentList += "-AcceptHostKey"
}

Write-Host "`n正在调用升级脚本 ..." -ForegroundColor Cyan
$process = Start-Process @upgradeArgs -PassThru
if ($process.ExitCode -ne 0) {
    throw "升级脚本执行失败，ExitCode=$($process.ExitCode)"
}

# 记录部署 commit
Update-LastDeployedCommit -CommitHash $currentCommit

Write-Host "`n升级触发器执行完毕。" -ForegroundColor Green
