#!/usr/bin/env pwsh
#requires -Version 5.1

<#
.SYNOPSIS
    leyoSwimming 第一阶段生产环境部署脚本
.DESCRIPTION
    部署后端、AI 服务镜像、管理后台、用户端 H5、教练端 H5 到 Ubuntu 服务器。
    本阶段使用 HTTP + 服务器 IP 访问，不启用 HTTPS/小程序/OSS/短信。
    LLM API Key 可留空，AI 服务后续单独启动。
#>

[CmdletBinding()]
param(
    [string]$ServerIP = "",
    [string]$ServerUser = "root",
    [string]$RemotePath = "/opt/leyo-swimming",
    [string]$ProjectRoot = "$PSScriptRoot\..",
    [switch]$SkipBuild,
    [switch]$SkipUpload,
    [switch]$SkipDeploy,
    [switch]$StopOnly,
    [switch]$Redeploy
)

$ErrorActionPreference = "Stop"

# 统一的脚本级状态
$script:sshCred = $null
# 本阶段默认在宿主机直接运行 AI 服务，避免 Docker Hub 拉取/构建超时，
# 同时保证 .env 中的 AI_SERVICE_BASE_URL 指向宿主机 IP 而非容器名。
$script:aiServiceMode = "direct"  # docker | direct
$script:llmApiKey = ""

# -------------------------------------------------
# 工具函数
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

function New-RandomSecret {
    param([int]$Length = 48)
    # 使用加密安全随机数生成器，兼容 PowerShell 5.1 (.NET Framework) 与 PowerShell 7+ (.NET Core)
    $bytes = New-Object byte[] $Length
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($bytes)
    }
    finally {
        $rng.Dispose()
    }
    $chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*-_+='
    $secret = -join ($bytes | ForEach-Object { $chars[$_ % $chars.Length] })
    return $secret
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
        throw "远程命令执行失败 (ExitStatus=$($result.ExitStatus))：`n$Command`n输出：`n$($result.Output)"
    }
    return $result
}

function Write-Section {
    param([string]$Message)
    Write-Host "`n========================================" -ForegroundColor Green
    Write-Host $Message -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
}

function Stop-AllServices {
    <#
    .SYNOPSIS
        停止所有 leyoSwimming 相关服务（Docker 容器 + 宿主机 AI 服务）。
    #>
    Write-Section "停止所有 leyoSwimming 服务"

    # 1. 停止 systemd 托管的 AI 服务（如果存在）
    $serviceExists = Invoke-RemoteCommand -Command "systemctl list-unit-files leyo-ai.service 2>/dev/null | grep -q leyo-ai && echo YES || echo NO" -IgnoreExitCode
    if ($serviceExists.Output.Trim() -eq "YES") {
        Write-Host "停止 leyo-ai systemd 服务 ..." -ForegroundColor Cyan
        Invoke-RemoteCommand -Command "systemctl stop leyo-ai 2>&1 || true" -IgnoreExitCode -TimeOut 60 | Out-Null
        Write-Host "  leyo-ai 已停止" -ForegroundColor Green
    }
    else {
        Write-Host "未检测到 leyo-ai systemd 服务，跳过。" -ForegroundColor Gray
    }

    # 2. 停止 Docker Compose 服务
    $composeExists = Invoke-RemoteCommand -Command "test -f $RemotePath/deploy/docker-compose.prod.yml && echo YES || echo NO" -IgnoreExitCode
    if ($composeExists.Output.Trim() -eq "YES") {
        Write-Host "停止 Docker Compose 服务 ..." -ForegroundColor Cyan
        $stopComposeCmd = @(
            "cd $RemotePath/deploy",
            "if test -f docker-compose.ai-direct.yml; then",
            "  docker compose -f docker-compose.prod.yml -f docker-compose.ai-direct.yml down --remove-orphans 2>&1 || true",
            "else",
            "  docker compose -f docker-compose.prod.yml down --remove-orphans 2>&1 || true",
            "fi"
        )
        Invoke-RemoteCommand -Command ($stopComposeCmd -join "`n") -IgnoreExitCode -TimeOut 180 | Out-Null
        Write-Host "  Docker 服务已停止" -ForegroundColor Green
    }
    else {
        Write-Host "未检测到 docker-compose.prod.yml，跳过。" -ForegroundColor Gray
    }

    Write-Host "所有服务已停止，端口已释放。" -ForegroundColor Green
}

function Invoke-BuildIfNeeded {
    <#
    .SYNOPSIS
        如果产物已存在则询问是否重新构建，否则直接构建。
    #>
    param(
        [Parameter(Mandatory)]
        [string]$Name,
        [Parameter(Mandatory)]
        [string]$ArtifactPath,
        [Parameter(Mandatory)]
        [scriptblock]$BuildScript
    )
    $fullArtifactPath = Join-Path $ProjectRoot $ArtifactPath
    if (Test-Path $fullArtifactPath) {
        Write-Host "检测到 $Name 产物已存在：$fullArtifactPath" -ForegroundColor Yellow
        $rebuild = Read-Host "是否重新构建 $Name？(y/N，默认 N)"
        if ($rebuild -match '^(y|Y|yes|YES)$') {
            Write-Host "重新构建 $Name ..." -ForegroundColor Cyan
            & $BuildScript
        }
        else {
            Write-Host "跳过构建，使用已有产物：$fullArtifactPath" -ForegroundColor Green
        }
    }
    else {
        Write-Host "$Name 产物不存在，开始构建 ..." -ForegroundColor Cyan
        & $BuildScript
    }
}

# -------------------------------------------------
# 前置检查
# -------------------------------------------------
Write-Section "Step 0: 前置环境检查"

if ([string]::IsNullOrWhiteSpace($ServerIP)) {
    throw "必须指定服务器 IP，请使用 -ServerIP 参数传入，例如：.\scripts\deploy-first-stage.ps1 -ServerIP '123.45.67.89'"
}

if (-not (Test-CommandExists "ssh")) {
    throw "未找到 OpenSSH 客户端 (ssh.exe)，请先安装或启用 OpenSSH 客户端。"
}
if (-not (Test-CommandExists "scp")) {
    throw "未找到 OpenSSH scp (scp.exe)，请先安装或启用 OpenSSH 客户端。"
}
if (-not (Test-CommandExists "java")) {
    throw "未找到 Java，请确认 JDK 17+ 已安装。"
}
if (-not (Test-CommandExists "yarn")) {
    throw "未找到 yarn，请确认 Node.js 和 yarn 已安装。"
}

Install-PoshSSH

Get-SecureCredential

Write-Host "测试 SSH 连接到 ${ServerUser}@${ServerIP} ..." -ForegroundColor Cyan
$session = New-SSHSession -ComputerName $ServerIP -Credential $script:sshCred -AcceptKey
if (-not $session) {
    throw "SSH 连接失败，请检查 IP、用户名和密码。"
}
Write-Host "SSH 连接成功， Session ID: $($session.SessionId)" -ForegroundColor Green

# 仅停止服务模式：不需要后续的配置、构建、部署流程
if ($StopOnly) {
    Stop-AllServices
    Get-SSHSession | Remove-SSHSession | Out-Null
    Write-Host "`n已停止所有服务并断开 SSH。" -ForegroundColor Green
    exit 0
}

# 重新部署模式：先停止所有服务，再进入正常部署流程
if ($Redeploy) {
    Stop-AllServices
    Write-Host "`n进入重新部署流程 ..." -ForegroundColor Cyan
}

# 输入 AI 服务完整配置
function Get-AIServiceConfig {
    Write-Host "`n===== AI 服务配置 =====" -ForegroundColor Cyan
    Write-Host "以下配置将生成 AI 服务的生产环境变量。`n" -ForegroundColor Gray

    $cfg = @{}

    $cfg['LLM_MODEL'] = Read-HostWithDefault "LLM 模型名称" "deepseek-chat"
    $cfg['LLM_BASE_URL'] = Read-HostWithDefault "LLM Base URL" "https://api.deepseek.com/v1"
    Write-Host "LLM API Key（没有可先按回车，AI 服务后续单独启动；输入时不显示）" -ForegroundColor Cyan
    $cfg['LLM_API_KEY'] = Read-HostSecure "LLM API Key"

    $cfg['LLM_TEMPERATURE'] = Read-HostWithDefault "LLM Temperature" "0.3"
    $cfg['LLM_MAX_TOKENS'] = Read-HostWithDefault "LLM Max Tokens" "2048"
    $cfg['LLM_TIMEOUT_SECONDS'] = Read-HostWithDefault "LLM Timeout Seconds" "30"

    $cfg['LANGCHAIN_TRACING_V2'] = Read-HostWithDefault "启用 LangSmith 追踪 (true/false)" "false"
    if ($cfg['LANGCHAIN_TRACING_V2'] -eq 'true') {
        Write-Host "LangSmith API Key（输入时不显示）" -ForegroundColor Cyan
        $cfg['LANGCHAIN_API_KEY'] = Read-HostSecure "LangSmith API Key"
        $cfg['LANGCHAIN_PROJECT'] = Read-HostWithDefault "LangSmith Project" "leyo_prod"
        $cfg['LANGCHAIN_ENDPOINT'] = Read-HostWithDefault "LangSmith Endpoint" "https://api.smith.langchain.com"
        if ([string]::IsNullOrWhiteSpace($cfg['LANGCHAIN_API_KEY'])) {
            Write-Host "未提供 LangSmith API Key，自动关闭 LangSmith 追踪。" -ForegroundColor Yellow
            $cfg['LANGCHAIN_TRACING_V2'] = 'false'
        }
    } else {
        $cfg['LANGCHAIN_API_KEY'] = ''
        $cfg['LANGCHAIN_PROJECT'] = 'leyo_prod'
        $cfg['LANGCHAIN_ENDPOINT'] = 'https://api.smith.langchain.com'
    }

    $cfg['LOG_LEVEL'] = Read-HostWithDefault "日志级别 (DEBUG/INFO/WARNING/ERROR)" "INFO"
    $cfg['SESSION_MESSAGE_TTL_SECONDS'] = Read-HostWithDefault "会话消息 TTL (秒)" "604800"
    $cfg['SESSION_MAX_MESSAGES'] = Read-HostWithDefault "会话最大消息数" "20"
    $cfg['RATE_LIMIT_USER_PER_MINUTE'] = Read-HostWithDefault "每用户每分钟限流" "30"
    $cfg['RATE_LIMIT_IP_PER_MINUTE'] = Read-HostWithDefault "每 IP 每分钟限流" "60"
    # 生产环境有 Nginx 在前面，信任 1 层代理
    $cfg['TRUSTED_PROXY_COUNT'] = Read-HostWithDefault "可信代理层数 (Nginx 前置填 1)" "1"

    return $cfg
}

function Read-HostWithDefault {
    param([string]$Prompt, [string]$Default)
    $val = Read-Host "$Prompt [默认: $Default]"
    if ([string]::IsNullOrWhiteSpace($val)) { return $Default }
    return $val
}

function Read-HostSecure {
    <#
    .SYNOPSIS
        安全读取敏感输入（不回显），返回普通字符串。
    #>
    param([string]$Prompt)
    $secure = Read-Host -AsSecureString $Prompt
    $bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
    }
    finally {
        [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

# -------------------------------------------------
# 服务器环境检查
# -------------------------------------------------
function Test-ServerEnvironment {
    Write-Section "Step 0.5: 服务器环境检查"

    Write-Host "系统信息：" -ForegroundColor Cyan
    $osInfo = Invoke-RemoteCommand -Command "lsb_release -ds 2>/dev/null || cat /etc/os-release | grep PRETTY_NAME | cut -d'=' -f2 | tr -d '\`"'"
    Write-Host "  OS: $($osInfo.Output.Trim())" -ForegroundColor Gray

    $kernel = Invoke-RemoteCommand -Command "uname -r"
    Write-Host "  Kernel: $($kernel.Output.Trim())" -ForegroundColor Gray

    Write-Host "`nDocker 检查：" -ForegroundColor Cyan
    $dockerVersion = Invoke-RemoteCommand -Command "docker --version 2>/dev/null || echo 'NOT_INSTALLED'" -IgnoreExitCode
    $composeVersion = Invoke-RemoteCommand -Command "docker compose version 2>/dev/null || echo 'NOT_INSTALLED'" -IgnoreExitCode

    if ($dockerVersion.Output -match "NOT_INSTALLED") {
        Write-Host "  Docker: 未安装" -ForegroundColor Red
        return $false
    }
    Write-Host "  Docker: $($dockerVersion.Output.Trim())" -ForegroundColor Green

    if ($composeVersion.Output -match "NOT_INSTALLED") {
        Write-Host "  Docker Compose: 未安装" -ForegroundColor Red
        return $false
    }
    Write-Host "  Docker Compose: $($composeVersion.Output.Trim())" -ForegroundColor Green

    $dockerStatus = Invoke-RemoteCommand -Command "systemctl is-active docker 2>/dev/null || echo 'inactive'" -IgnoreExitCode
    if ($dockerStatus.Output.Trim() -ne "active") {
        Write-Host "  Docker 服务状态: 未运行，尝试启动 ..." -ForegroundColor Yellow
        Invoke-RemoteCommand -Command "systemctl start docker" -IgnoreExitCode | Out-Null
        $dockerStatus = Invoke-RemoteCommand -Command "systemctl is-active docker 2>/dev/null || echo 'inactive'" -IgnoreExitCode
        if ($dockerStatus.Output.Trim() -ne "active") {
            throw "Docker 服务无法启动，请手动检查。"
        }
    }
    Write-Host "  Docker 服务状态: 运行中" -ForegroundColor Green

    Write-Host "`n资源检查：" -ForegroundColor Cyan
    $disk = Invoke-RemoteCommand -Command "df -h / | awk 'NR==2 {print $4}'"
    Write-Host "  / 可用磁盘: $($disk.Output.Trim())" -ForegroundColor Gray
    $mem = Invoke-RemoteCommand -Command "free -h | awk '/^Mem:/ {print $7}'"
    Write-Host "  可用内存: $($mem.Output.Trim())" -ForegroundColor Gray

    Write-Host "`n端口检查（以下端口应为空闲）：" -ForegroundColor Cyan
    $portsToCheck = @(80, 8080, 3306, 6379, 8000)
    $occupiedPorts = @()
    foreach ($port in $portsToCheck) {
        $portCheck = Invoke-RemoteCommand -Command "ss -tlnp 2>/dev/null | grep -q ':$port ' && echo OCCUPIED || echo FREE" -IgnoreExitCode
        if ($portCheck.Output.Trim() -eq "OCCUPIED") {
            Write-Host "  端口 $port`: 被占用" -ForegroundColor Red
            $occupiedPorts += $port
        } else {
            Write-Host "  端口 $port`: 空闲" -ForegroundColor Green
        }
    }
    if ($occupiedPorts.Count -gt 0) {
        throw "端口 $($occupiedPorts -join ', ') 已被占用，请先释放或调整 docker-compose.prod.yml 的端口映射。"
    }

    Write-Host "`n服务器环境检查通过。" -ForegroundColor Green
    return $true
}

function Install-ServerDocker {
    Write-Section "Step 0.6: 安装 Docker 与 Docker Compose"
    Write-Host "检测到服务器未安装 Docker，开始安装 ..." -ForegroundColor Yellow

    $installDockerCmds = @(
        "set -e",
        "export DEBIAN_FRONTEND=noninteractive",
        "apt-get update",
        "apt-get install -y ca-certificates curl gnupg lsb-release",
        "install -m 0755 -d /etc/apt/keyrings",
        "curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg",
        "chmod a+r /etc/apt/keyrings/docker.gpg",
        "echo `"deb [arch=`$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu `$(lsb_release -cs) stable`" > /etc/apt/sources.list.d/docker.list",
        "apt-get update",
        "apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin",
        "systemctl enable docker",
        "systemctl start docker"
    )
    Invoke-RemoteCommand -Command ($installDockerCmds -join "`n") -TimeOut 300

    Write-Host "Docker 安装完成。" -ForegroundColor Green
}

function Set-DockerMirror {
    # 按优先级排列的国内镜像源，服务器在腾讯云时优先尝试腾讯云镜像
    $mirrors = @(
        "https://mirror.ccs.tencentyun.com",
        "https://docker.m.daocloud.io",
        "https://hub-mirror.c.163.com",
        "https://docker.mirrors.ustc.edu.cn"
    )

    Write-Host "检查本地是否已存在 python:3.11-slim 镜像 ..." -ForegroundColor Cyan
    $localImageCheck = Invoke-RemoteCommand -Command "docker images --format '{{.Repository}}:{{.Tag}}' | grep -q '^python:3.11-slim$' && echo YES || echo NO" -IgnoreExitCode
    if ($localImageCheck.Output.Trim() -eq "YES") {
        Write-Host "  本地已存在 python:3.11-slim 镜像，跳过镜像源测试。" -ForegroundColor Green
        return
    }

    Write-Host "检查 Docker Hub 连通性（超时 5 分钟） ..." -ForegroundColor Cyan
    $pullTest = Invoke-RemoteCommand -Command "timeout 300 docker pull python:3.11-slim 2>&1 | tail -n 5" -IgnoreExitCode -TimeOut 320
    if ($pullTest.ExitStatus -eq 0) {
        Write-Host "  Docker Hub 可直接访问，无需配置镜像。" -ForegroundColor Green
        return
    }

    Write-Host "  Docker Hub 访问超时，尝试配置国内镜像 ..." -ForegroundColor Yellow

    foreach ($mirror in $mirrors) {
        Write-Host "  尝试镜像源：$mirror" -ForegroundColor Yellow

        $mirrorConfig = "{`"registry-mirrors`": [`"$mirror`"]}"
        $setupMirrorCmds = @(
            "mkdir -p /etc/docker",
            "echo '$mirrorConfig' > /etc/docker/daemon.json",
            "systemctl daemon-reload",
            "systemctl restart docker",
            "sleep 5",
            "docker info 2>/dev/null | grep -A2 'Registry Mirrors' || true"
        )
        Invoke-RemoteCommand -Command ($setupMirrorCmds -join "`n") -TimeOut 120 | Out-Null

        $pullTest = Invoke-RemoteCommand -Command "timeout 300 docker pull python:3.11-slim 2>&1 | tail -n 10" -IgnoreExitCode -TimeOut 320
        if ($pullTest.ExitStatus -eq 0) {
            Write-Host "  镜像 $mirror 可用，已配置。" -ForegroundColor Green
            return
        }
        Write-Host "  镜像 $mirror 不可用，继续尝试下一个 ..." -ForegroundColor Gray
    }

    throw "所有国内 Docker 镜像源均无法拉取 python:3.11-slim。请检查服务器网络，或手动配置可用的镜像加速器后再重试。"
}

function Pull-DockerImageIfMissing {
    param(
        [Parameter(Mandatory)]
        [string]$ImageName
    )

    Write-Host "检查本地是否已存在镜像 $ImageName ..." -ForegroundColor Cyan
    $localCheck = Invoke-RemoteCommand -Command "docker images --format '{{.Repository}}:{{.Tag}}' | grep -q '^$([regex]::Escape($ImageName))$' && echo YES || echo NO" -IgnoreExitCode
    if ($localCheck.Output.Trim() -eq "YES") {
        Write-Host "  本地已存在 $ImageName，跳过拉取。" -ForegroundColor Green
        return
    }

    Write-Host "  本地不存在 $ImageName，开始拉取（超时 5 分钟） ..." -ForegroundColor Yellow
    # 使用 PIPESTATUS 获取 docker pull 的真实退出状态，避免被 tail 掩盖
    $pullResult = Invoke-RemoteCommand -Command "set -o pipefail; timeout 300 docker pull $ImageName 2>&1 | tail -n 20; exit `${PIPESTATUS[0]}" -IgnoreExitCode -TimeOut 320
    if ($pullResult.ExitStatus -ne 0) {
        throw "拉取镜像 $ImageName 失败：`n$($pullResult.Output)"
    }

    # 二次确认镜像确实已存在
    $verifyCheck = Invoke-RemoteCommand -Command "docker images --format '{{.Repository}}:{{.Tag}}' | grep -q '^$([regex]::Escape($ImageName))$' && echo YES || echo NO" -IgnoreExitCode
    if ($verifyCheck.Output.Trim() -ne "YES") {
        throw "镜像 $ImageName 拉取后仍未找到，可能拉取过程中被中断。"
    }
    Write-Host "  $ImageName 拉取完成。" -ForegroundColor Green
}

function Install-AIServiceDirectly {
    Write-Section "Step 3.2-direct: 宿主机直接运行 AI 服务"

    $pythonCheck = Invoke-RemoteCommand -Command "python3.11 --version 2>&1 || echo NOT_FOUND" -IgnoreExitCode
    if ($pythonCheck.Output -match "Python 3\.11") {
        Write-Host "检测到系统已安装 Python 3.11，跳过安装步骤。" -ForegroundColor Green
    }
    else {
        Write-Host "安装 Python 3.11 ..." -ForegroundColor Cyan
        $installPythonCmds = @(
            "export DEBIAN_FRONTEND=noninteractive",
            "apt-get update",
            "apt-get install -y software-properties-common",
            "add-apt-repository -y ppa:deadsnakes/ppa",
            "apt-get update",
            "apt-get install -y python3.11 python3.11-venv python3.11-dev python3-pip"
        )
        Invoke-RemoteCommand -Command ($installPythonCmds -join "`n") -TimeOut 240 | Out-Null
    }

    $venvPath = "$RemotePath/ai-service-venv"
    $venvMarker = "$venvPath/.deps-installed"
    $venvExists = Invoke-RemoteCommand -Command "test -d $venvPath && test -f $venvPath/bin/uvicorn && echo YES || echo NO" -IgnoreExitCode
    if ($venvExists.Output.Trim() -eq "YES") {
        Write-Host "检测到虚拟环境已存在，跳过创建。" -ForegroundColor Green
    }
    else {
        Write-Host "创建 Python 虚拟环境 $venvPath ..." -ForegroundColor Cyan
        Invoke-RemoteCommand -Command "rm -rf $venvPath && python3.11 -m venv $venvPath" -TimeOut 60 | Out-Null
    }

    $depsInstalled = Invoke-RemoteCommand -Command "test -f $venvMarker && echo YES || echo NO" -IgnoreExitCode
    if ($depsInstalled.Output.Trim() -eq "YES") {
        Write-Host "检测到 AI 服务依赖已安装，跳过安装。" -ForegroundColor Green
    }
    else {
        Write-Host "安装 AI 服务依赖 ..." -ForegroundColor Cyan
        $installDepsCmds = @(
            "cd $RemotePath/ai-service",
            "source $venvPath/bin/activate",
            "pip install --upgrade pip -i https://pypi.tuna.tsinghua.edu.cn/simple",
            "pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple",
            "touch $venvMarker"
        )
        Invoke-RemoteCommand -Command ($installDepsCmds -join "`n") -TimeOut 300
    }

    Write-Host "创建 systemd 服务 ..." -ForegroundColor Cyan
    $serviceContent = @"
[Unit]
Description=leyo AI Service
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=$RemotePath/ai-service
EnvironmentFile=$RemotePath/deploy/.env
ExecStart=$venvPath/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
"@
    $serviceCmds = @(
        "cat > /etc/systemd/system/leyo-ai.service <<'EOF'",
        $serviceContent,
        "EOF",
        "systemctl daemon-reload",
        "systemctl enable leyo-ai"
    )
    Invoke-RemoteCommand -Command ($serviceCmds -join "`n") -TimeOut 60 | Out-Null

    Write-Host "AI 服务 systemd 配置完成。" -ForegroundColor Green
}

# 执行服务器环境检查
$serverReady = Test-ServerEnvironment
if (-not $serverReady) {
    Install-ServerDocker
    # 安装后再次检查
    $null = Test-ServerEnvironment
}

# -------------------------------------------------
# 本地构建
# -------------------------------------------------
if (-not $SkipBuild) {
    Write-Section "Step 1: 本地构建"

    # 1.1 后端 jar
    Invoke-BuildIfNeeded -Name "后端 Spring Boot jar" -ArtifactPath "backend\target\leyo-swimming-backend-0.1.0-SNAPSHOT.jar" -BuildScript {
        Push-Location "$ProjectRoot\backend"
        try {
            .\mvnw.cmd clean package -DskipTests -B
            if (-not (Test-Path "target\leyo-swimming-backend-0.1.0-SNAPSHOT.jar")) {
                throw "后端 jar 构建失败，未找到产物。"
            }
        } finally {
            Pop-Location
        }
    }

    # 1.2 AI 服务源码（镜像改在服务器端构建，本地无需 Docker）
    Write-Host "检查 AI 服务源码 ..." -ForegroundColor Cyan
    if (-not (Test-Path "$ProjectRoot\ai-service\Dockerfile")) {
        throw "AI 服务源码不完整，未找到 Dockerfile。"
    }
    if (-not (Test-Path "$ProjectRoot\ai-service\requirements.txt")) {
        throw "AI 服务源码不完整，未找到 requirements.txt。"
    }

    # 1.3 管理后台
    Invoke-BuildIfNeeded -Name "管理后台 web-admin" -ArtifactPath "web-admin\dist" -BuildScript {
        Push-Location "$ProjectRoot\web-admin"
        try {
            yarn install
            $env:VITE_API_BASE_URL = "http://$ServerIP/api"
            yarn build
        } finally {
            Pop-Location
        }
    }

    # 1.4 用户端 H5
    Invoke-BuildIfNeeded -Name "用户端 H5" -ArtifactPath "miniapp-user\h5-user-dist" -BuildScript {
        Push-Location "$ProjectRoot\miniapp-user"
        try {
            yarn install
            $env:TARO_APP_API_BASE_URL = "http://$ServerIP/api"
            yarn build:h5
            if (Test-Path "h5-user-dist") { Remove-Item -Recurse -Force "h5-user-dist" }
            Rename-Item -Path "dist" -NewName "h5-user-dist"
        } finally {
            Pop-Location
        }
    }

    # 1.5 教练端 H5
    Invoke-BuildIfNeeded -Name "教练端 H5" -ArtifactPath "miniapp-coach\h5-coach-dist" -BuildScript {
        Push-Location "$ProjectRoot\miniapp-coach"
        try {
            yarn install
            $env:TARO_APP_API_BASE_URL = "http://$ServerIP/api"
            yarn build:h5
            if (Test-Path "h5-coach-dist") { Remove-Item -Recurse -Force "h5-coach-dist" }
            Rename-Item -Path "dist" -NewName "h5-coach-dist"
        } finally {
            Pop-Location
        }
    }
}
else {
    Write-Host "跳过本地构建（--SkipBuild）" -ForegroundColor Yellow
}

# -------------------------------------------------
# 上传到服务器
# -------------------------------------------------
if (-not $SkipUpload) {
    Write-Section "Step 2: 上传构建产物到服务器"

    Invoke-RemoteCommand -Command "mkdir -p $RemotePath/uploads $RemotePath/deploy/init-db" | Out-Null

    $uploadItems = @(
        @{ Local = "backend\target\leyo-swimming-backend-0.1.0-SNAPSHOT.jar"; RemoteParent = "$RemotePath/"; NewName = "leyo-swimming-backend-0.1.0-SNAPSHOT.jar"; Force = $false },
        @{ Local = "ai-service"; RemoteParent = "$RemotePath/"; NewName = "ai-service"; Force = $false },
        @{ Local = "web-admin\dist"; RemoteParent = "$RemotePath/"; NewName = "web-admin-dist"; Force = $false },
        @{ Local = "miniapp-user\h5-user-dist"; RemoteParent = "$RemotePath/"; NewName = "h5-user-dist"; Force = $false },
        @{ Local = "miniapp-coach\h5-coach-dist"; RemoteParent = "$RemotePath/"; NewName = "h5-coach-dist"; Force = $false },
        # 核心部署配置文件必须始终与本地一致，强制覆盖
        @{ Local = "deploy\.env.example"; RemoteParent = "$RemotePath/deploy/"; NewName = ".env.example"; Force = $true },
        @{ Local = "deploy\nginx.conf"; RemoteParent = "$RemotePath/deploy/"; NewName = "nginx.conf"; Force = $true },
        @{ Local = "deploy\docker-compose.prod.yml"; RemoteParent = "$RemotePath/deploy/"; NewName = "docker-compose.prod.yml"; Force = $true }
    )

    $extractionQueue = @()

    # ---------- 上传阶段 ----------
    foreach ($item in $uploadItems) {
        $localPath = Join-Path $ProjectRoot $item.Local
        $remoteParent = $item.RemoteParent
        $newName = $item.NewName
        $remoteTarget = "$remoteParent$newName"
        if (-not (Test-Path $localPath)) {
            throw "上传失败，本地路径不存在：$localPath"
        }

        # 检查服务器上是否已存在
        $existsCheck = Invoke-RemoteCommand -Command "test -e '$remoteTarget' && echo YES || echo NO" -IgnoreExitCode
        $exists = $existsCheck.Output.Trim() -eq "YES"

        $shouldUpload = $true
        if ($exists) {
            if ($item.Force) {
                Write-Host "检测到服务器上已存在核心配置文件：$remoteTarget，将强制覆盖。" -ForegroundColor Yellow
            }
            else {
                Write-Host "检测到服务器上已存在：$remoteTarget" -ForegroundColor Yellow
                $reupload = Read-Host "  是否重新上传 $newName？(y/N，默认 N)"
                if ($reupload -notmatch '^(y|Y|yes|YES)$') {
                    $shouldUpload = $false
                }
            }
        }

        if (-not $shouldUpload) {
            Write-Host "  跳过上传：$remoteTarget" -ForegroundColor Gray
            continue
        }

        Write-Host "上传 $localPath -> ${ServerUser}@${ServerIP}:$remoteTarget" -ForegroundColor Cyan
        # 确保远端父目录存在，并清理已有目标
        Invoke-RemoteCommand -Command "mkdir -p $remoteParent && rm -rf $remoteTarget" -IgnoreExitCode | Out-Null

        $isDirectory = (Get-Item $localPath).PSIsContainer
        if ($isDirectory) {
            # Posh-SSH 3.x 的 Set-SCPItem 对目录支持不稳定，
            # 改为本地打 tar.gz 上传，待全部上传完成后再统一解压
            $sourceLeaf = Split-Path -Leaf $localPath
            $tarName = "$newName.tar.gz"
            $tempTar = Join-Path $env:TEMP $tarName
            if (Test-Path $tempTar) { Remove-Item -Force $tempTar }
            $parentDir = Split-Path -Parent $localPath
            Write-Host "  打包目录 $localPath -> $tempTar" -ForegroundColor Gray
            & tar -czf $tempTar -C $parentDir $sourceLeaf
            if ($LASTEXITCODE -ne 0) {
                throw "打包目录失败：$localPath"
            }

            Set-SCPItem -ComputerName $ServerIP -Credential $script:sshCred `
                -Path $tempTar -Destination $remoteParent -NewName $tarName
            Remove-Item -Force $tempTar

            $extractionQueue += @{
                RemoteParent = $remoteParent
                TarName = $tarName
                SourceLeaf = $sourceLeaf
                NewName = $newName
            }
        }
        else {
            Set-SCPItem -ComputerName $ServerIP -Credential $script:sshCred `
                -Path $localPath -Destination $remoteParent -NewName $newName
        }
    }

    # ---------- 解压阶段（所有上传完成后统一执行） ----------
    if ($extractionQueue.Count -gt 0) {
        Write-Host "`n开始解压上传的目录 ..." -ForegroundColor Cyan
        foreach ($extract in $extractionQueue) {
            $remoteParent = $extract.RemoteParent
            $tarName = $extract.TarName
            $sourceLeaf = $extract.SourceLeaf
            $newName = $extract.NewName
            $extractCmd = "cd '$remoteParent' && tar -xzf '$tarName' 2>&1"
            if ($sourceLeaf -ne $newName) {
                $extractCmd += " && mv '$sourceLeaf' '$newName' 2>&1"
            }
            $extractCmd += " && rm -f '$tarName'"
            Write-Host "  解压 $remoteParent$tarName -> $newName" -ForegroundColor Gray
            Invoke-RemoteCommand -Command $extractCmd -TimeOut 120 | Out-Null
        }
    }
}
else {
    Write-Host "跳过上传（--SkipUpload）" -ForegroundColor Yellow
}

# -------------------------------------------------
# 服务器端部署
# -------------------------------------------------
if (-not $SkipDeploy) {
    Write-Section "Step 3: 服务器端部署"

    # 3.1 确认 Docker 可用（Step 0.5/0.6 已安装）
    Write-Host "确认 Docker 环境 ..." -ForegroundColor Cyan
    $dockerCheck = Invoke-RemoteCommand -Command "command -v docker && docker compose version" -IgnoreExitCode
    if ($dockerCheck.ExitStatus -ne 0) {
        throw "Docker 未正确安装，请检查 Step 0.6 的输出日志。"
    }
    Write-Host "Docker 已就绪" -ForegroundColor Green

    # 3.1.5 配置 Docker 国内镜像（针对中国大陆服务器）
    Set-DockerMirror

    # 3.2 准备 .env
    $remoteEnvExists = Invoke-RemoteCommand -Command "test -f $RemotePath/deploy/.env && echo YES || echo NO" -IgnoreExitCode
    $regenerateEnv = $true
    if ($Redeploy -and $remoteEnvExists.Output.Trim() -eq "YES") {
        Write-Host "检测到服务器上已存在 .env。" -ForegroundColor Yellow
        Write-Host "警告：重新生成 .env 会轮换 JWT_SECRET、PHONE_ENCRYPTION_KEY、ID_CARD_ENCRYPTION_KEY、INTERNAL_API_TOKEN，" -ForegroundColor Red
        Write-Host "      可能导致已登录用户失效、已加密数据无法解密、AI 服务内部认证失败。" -ForegroundColor Red
        $regenerateEnv = (Read-Host "是否重新生成 .env？(y/N，默认 N)") -match '^(y|Y|yes|YES)$'
    }

    if ($regenerateEnv) {
        Write-Host "生成生产环境 .env ..." -ForegroundColor Cyan
        $script:aiConfig = Get-AIServiceConfig
        $script:llmApiKey = $script:aiConfig['LLM_API_KEY']

        $jwtSecret = New-RandomSecret -Length 64
        $phoneKey = New-RandomSecret -Length 32
        $idCardKey = New-RandomSecret -Length 32
        $internalToken = New-RandomSecret -Length 48

        $aiServiceBaseUrl = if ($script:aiServiceMode -eq "direct") { "http://$ServerIP:8000" } else { "http://leyo-ai-service:8000" }

        $envLines = @(
            "# MySQL（本阶段与开发环境一致）",
            "MYSQL_ROOT_PASSWORD=leyo_root",
            "MYSQL_DATABASE=leyo_dev",
            "MYSQL_USER=leyo",
            "MYSQL_PASSWORD=leyo_dev",
            "",
            "# Redis",
            "REDIS_PASSWORD=leyo1234",
            "",
            "# JWT",
            "JWT_SECRET=$jwtSecret",
            "",
            "# 加密密钥",
            "PHONE_ENCRYPTION_KEY=$phoneKey",
            "ID_CARD_ENCRYPTION_KEY=$idCardKey",
            "",
            "# AI 服务",
            "AI_SERVICE_BASE_URL=$aiServiceBaseUrl",
            "INTERNAL_API_TOKEN=$internalToken",
            "",
            "# LLM 配置",
            "LLM_MODEL=$($script:aiConfig['LLM_MODEL'])",
            "LLM_BASE_URL=$($script:aiConfig['LLM_BASE_URL'])",
            "LLM_API_KEY=$script:llmApiKey",
            "LLM_TEMPERATURE=$($script:aiConfig['LLM_TEMPERATURE'])",
            "LLM_MAX_TOKENS=$($script:aiConfig['LLM_MAX_TOKENS'])",
            "LLM_TIMEOUT_SECONDS=$($script:aiConfig['LLM_TIMEOUT_SECONDS'])",
            "",
            "# LangSmith 配置",
            "LANGCHAIN_TRACING_V2=$($script:aiConfig['LANGCHAIN_TRACING_V2'])",
            "LANGCHAIN_API_KEY=$($script:aiConfig['LANGCHAIN_API_KEY'])",
            "LANGCHAIN_PROJECT=$($script:aiConfig['LANGCHAIN_PROJECT'])",
            "LANGCHAIN_ENDPOINT=$($script:aiConfig['LANGCHAIN_ENDPOINT'])",
            "",
            "# AI 服务运行配置",
            "APP_NAME=leyo-ai-service",
            "APP_ENV=production",
            "HOST=0.0.0.0",
            "PORT=8000",
            "LOG_LEVEL=$($script:aiConfig['LOG_LEVEL'])",
            "",
            "# 会话与限流",
            "SESSION_MESSAGE_TTL_SECONDS=$($script:aiConfig['SESSION_MESSAGE_TTL_SECONDS'])",
            "SESSION_MAX_MESSAGES=$($script:aiConfig['SESSION_MAX_MESSAGES'])",
            "RATE_LIMIT_USER_PER_MINUTE=$($script:aiConfig['RATE_LIMIT_USER_PER_MINUTE'])",
            "RATE_LIMIT_IP_PER_MINUTE=$($script:aiConfig['RATE_LIMIT_IP_PER_MINUTE'])",
            "",
            "# 可信代理层数（Nginx 前置填 1）",
            "TRUSTED_PROXY_COUNT=$($script:aiConfig['TRUSTED_PROXY_COUNT'])",
            "",
            "# AI 服务 Mock 模式",
            "ENABLE_MOCK_DATA=false",
            "",
            "# 生产访问地址",
            "DOMAIN=$ServerIP"
        )
        $envContent = $envLines -join "`n"
        $localEnvPath = Join-Path $env:TEMP "leyo-deploy-env"
        $envContent | Out-File -FilePath $localEnvPath -Encoding UTF8 -NoNewline

        Write-Host "上传生产环境 .env ..." -ForegroundColor Cyan
        Set-SCPItem -ComputerName $ServerIP -Credential $script:sshCred `
            -Path $localEnvPath -Destination "$RemotePath/deploy/" -NewName ".env"
        Remove-Item -Force $localEnvPath
    }
    else {
        Write-Host "保留服务器上已有的 .env，跳过重新生成。" -ForegroundColor Green
        # 读取现有 .env 中的 LLM_API_KEY，用于后续判断是否启动 AI 服务
        $script:llmApiKey = Invoke-RemoteCommand -Command "grep '^LLM_API_KEY=' $RemotePath/deploy/.env | cut -d'=' -f2-" -IgnoreExitCode | Select-Object -ExpandProperty Output
        $script:llmApiKey = if ($script:llmApiKey) { $script:llmApiKey.Trim() } else { "" }
    }

    # 3.2.5 自动修正 .env 中的服务器 IP 相关配置（不轮换密钥）
    Write-Host "检查并修正 .env 中的服务器地址配置 ..." -ForegroundColor Cyan
    $expectedAiUrl = "http://$ServerIP`:8000"
    $currentAiUrl = Invoke-RemoteCommand -Command "grep '^AI_SERVICE_BASE_URL=' $RemotePath/deploy/.env | cut -d'=' -f2-" -IgnoreExitCode | Select-Object -ExpandProperty Output
    $currentAiUrl = if ($currentAiUrl) { $currentAiUrl.Trim() } else { "" }
    if ([string]::IsNullOrWhiteSpace($currentAiUrl) -or $currentAiUrl -eq "http://" -or $currentAiUrl -ne $expectedAiUrl) {
        Write-Host "  AI_SERVICE_BASE_URL 当前为 [$currentAiUrl]，自动修正为 [$expectedAiUrl]" -ForegroundColor Yellow
        Invoke-RemoteCommand -Command "sed -i 's|^AI_SERVICE_BASE_URL=.*|AI_SERVICE_BASE_URL=$expectedAiUrl|' $RemotePath/deploy/.env" -IgnoreExitCode | Out-Null
    }
    else {
        Write-Host "  AI_SERVICE_BASE_URL 已正确：$currentAiUrl" -ForegroundColor Green
    }

    $currentDomain = Invoke-RemoteCommand -Command "grep '^DOMAIN=' $RemotePath/deploy/.env | cut -d'=' -f2-" -IgnoreExitCode | Select-Object -ExpandProperty Output
    $currentDomain = if ($currentDomain) { $currentDomain.Trim() } else { "" }
    if ([string]::IsNullOrWhiteSpace($currentDomain) -or $currentDomain -eq "123.45.67.89" -or $currentDomain -ne $ServerIP) {
        Write-Host "  DOMAIN 当前为 [$currentDomain]，自动修正为 [$ServerIP]" -ForegroundColor Yellow
        Invoke-RemoteCommand -Command "sed -i 's|^DOMAIN=.*|DOMAIN=$ServerIP|' $RemotePath/deploy/.env" -IgnoreExitCode | Out-Null
    }
    else {
        Write-Host "  DOMAIN 已正确：$currentDomain" -ForegroundColor Green
    }

    # 3.3 安装 AI 服务
    # 本阶段默认使用宿主机直接运行，避免 Docker Hub 镜像拉取/构建超时
    $aiFilesCheck = Invoke-RemoteCommand -Command "test -f $RemotePath/ai-service/Dockerfile && test -f $RemotePath/ai-service/requirements.txt && echo OK || echo MISSING" -IgnoreExitCode
    if ($aiFilesCheck.Output.Trim() -ne "OK") {
        throw "AI 服务源码不完整，请确认 $RemotePath/ai-service 下存在 Dockerfile 和 requirements.txt"
    }

    $script:aiServiceMode = "direct"
    Install-AIServiceDirectly

    # 3.4 启动核心服务（不含 AI 服务）
    Write-Host "启动 MySQL、Redis、后端、Nginx ..." -ForegroundColor Cyan

    # 3.4.1 按需拉取核心服务镜像（本地已存在则跳过，避免重复下载）
    $coreImages = @("mysql:8.0", "redis:7-alpine", "eclipse-temurin:21-jre-alpine", "nginx:alpine")
    foreach ($image in $coreImages) {
        Pull-DockerImageIfMissing -ImageName $image
    }

    if ($script:aiServiceMode -eq "direct") {
        # 宿主机运行 AI 服务时：
        # 1. 后端容器把 leyo-ai-service 解析到宿主机
        # 2. nginx 把 leyo-ai-service 解析到宿主机（MCP 端点 /mcp-server/ 反代到宿主机 AI 服务）
        # 3. nginx 不再依赖 ai-service 容器（因为 ai-service 不在 Docker 里运行）
        $composeOverride = @"
services:
  backend:
    extra_hosts:
      - "leyo-ai-service:host-gateway"
  nginx:
    extra_hosts:
      - "leyo-ai-service:host-gateway"
    depends_on:
      - backend
"@
        $overrideCmds = @(
            "cat > $RemotePath/deploy/docker-compose.ai-direct.yml <<'EOF'",
            $composeOverride,
            "EOF"
        )
        Invoke-RemoteCommand -Command ($overrideCmds -join "`n") -TimeOut 30 | Out-Null

        $startCoreCmds = @(
            "cd $RemotePath/deploy",
            "docker compose -f docker-compose.prod.yml -f docker-compose.ai-direct.yml down --remove-orphans 2>&1 || true",
            "docker compose -f docker-compose.prod.yml -f docker-compose.ai-direct.yml up -d mysql redis backend nginx 2>&1"
        )
    }
    else {
        $startCoreCmds = @(
            "cd $RemotePath/deploy",
            "docker compose -f docker-compose.prod.yml down --remove-orphans 2>&1 || true",
            "docker compose -f docker-compose.prod.yml up -d mysql redis backend nginx 2>&1"
        )
    }
    try {
        Invoke-RemoteCommand -Command ($startCoreCmds -join "`n") -TimeOut 300 | Out-Null
    }
    catch {
        Write-Host "`ndocker compose 启动失败，正在排查 ..." -ForegroundColor Red

        # 1. 输出 compose 配置校验结果，检查 .env 变量是否缺失
        if ($script:aiServiceMode -eq "direct") {
            $configCmd = "cd $RemotePath/deploy && docker compose -f docker-compose.prod.yml -f docker-compose.ai-direct.yml config 2>&1"
        }
        else {
            $configCmd = "cd $RemotePath/deploy && docker compose -f docker-compose.prod.yml config 2>&1"
        }
        Write-Host "`n[docker compose config 输出]" -ForegroundColor Yellow
        $config = Invoke-RemoteCommand -Command $configCmd -IgnoreExitCode -TimeOut 60
        Write-Host $config.Output -ForegroundColor Gray

        # 2. 输出容器日志（如果有的话）
        if ($script:aiServiceMode -eq "direct") {
            $logCmd = "cd $RemotePath/deploy && docker compose -f docker-compose.prod.yml -f docker-compose.ai-direct.yml logs --tail=80 2>&1"
        }
        else {
            $logCmd = "cd $RemotePath/deploy && docker compose -f docker-compose.prod.yml logs --tail=80 2>&1"
        }
        Write-Host "`n[docker compose logs 输出]" -ForegroundColor Yellow
        $logs = Invoke-RemoteCommand -Command $logCmd -IgnoreExitCode -TimeOut 60
        Write-Host $logs.Output -ForegroundColor Gray

        throw
    }

    # 3.5 等待后端健康
    Write-Host "等待后端服务就绪 ..." -ForegroundColor Cyan
    $waitBackendCmds = @(
        "for i in {1..30}; do",
        '  if wget --no-verbose --tries=1 --spider http://localhost:8080/health 2>/dev/null; then',
        '    echo "Backend is healthy"',
        "    exit 0",
        "  fi",
        '  echo "Waiting for backend... $i/30"',
        "  sleep 5",
        "done",
        'echo "Backend did not become healthy in time"',
        "exit 1"
    )
    Invoke-RemoteCommand -Command ($waitBackendCmds -join "`n")

    # 3.6 启动 AI 服务（仅在提供了 LLM Key 时）
    if ([string]::IsNullOrWhiteSpace($script:llmApiKey)) {
        Write-Host "`n未提供 LLM API Key，已跳过 AI 服务启动。" -ForegroundColor Yellow
        if ($script:aiServiceMode -eq "direct") {
            Write-Host "后续提供 Key 后，请登录服务器执行：" -ForegroundColor Yellow
            Write-Host "  nano $RemotePath/deploy/.env  # 修改 LLM_API_KEY" -ForegroundColor Yellow
            Write-Host "  systemctl restart leyo-ai" -ForegroundColor Yellow
        }
        else {
            Write-Host "后续提供 Key 后，请登录服务器执行：" -ForegroundColor Yellow
            Write-Host "  cd $RemotePath/deploy" -ForegroundColor Yellow
            Write-Host "  nano .env  # 修改 LLM_API_KEY" -ForegroundColor Yellow
            Write-Host "  docker compose -f docker-compose.prod.yml up -d ai-service" -ForegroundColor Yellow
        }
    }
    else {
        Write-Host "启动 AI 服务 ..." -ForegroundColor Cyan
        if ($script:aiServiceMode -eq "direct") {
            Invoke-RemoteCommand -Command "systemctl restart leyo-ai" | Out-Null
        }
        else {
            Invoke-RemoteCommand -Command "cd $RemotePath/deploy && docker compose -f docker-compose.prod.yml up -d ai-service" | Out-Null
        }
    }

    # 3.7 创建初始管理员（手动）
    Write-Host "`n请手动创建初始管理员账号。登录服务器后执行：" -ForegroundColor Yellow
    Write-Host "  ssh root@$ServerIP" -ForegroundColor White
    Write-Host '  curl -X POST http://localhost:8080/api/admin/auth/register -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin123\",\"nickname\":\"管理员\"}"' -ForegroundColor White
    Write-Host "具体接口路径请参考后端实际提供的管理员注册/初始化接口。" -ForegroundColor Gray
}
else {
    Write-Host "跳过服务器部署（--SkipDeploy）" -ForegroundColor Yellow
}

# -------------------------------------------------
# 部署完成
# -------------------------------------------------
Write-Section "部署完成"

Write-Host "访问地址：" -ForegroundColor Green
Write-Host "  管理后台： http://$ServerIP/admin/" -ForegroundColor White
Write-Host "  用户端 H5： http://$ServerIP/h5/user/" -ForegroundColor White
Write-Host "  教练端 H5： http://$ServerIP/h5/coach/" -ForegroundColor White
Write-Host "  API 接口：  http://$ServerIP/api/" -ForegroundColor White

Write-Host "`n后续维护命令（登录服务器后执行）：" -ForegroundColor Green
Write-Host "  cd $RemotePath/deploy" -ForegroundColor White
Write-Host "  docker compose -f docker-compose.prod.yml ps" -ForegroundColor White
Write-Host "  docker compose -f docker-compose.prod.yml logs -f backend" -ForegroundColor White
if ($script:aiServiceMode -eq "direct") {
    Write-Host "  systemctl status leyo-ai" -ForegroundColor White
    Write-Host "  journalctl -u leyo-ai -f" -ForegroundColor White
}

if ([string]::IsNullOrWhiteSpace($script:llmApiKey)) {
    Write-Host "`n注意：AI 服务尚未启动，请在获得 LLM API Key 后更新 $RemotePath/deploy/.env 中的 LLM_API_KEY，然后执行：" -ForegroundColor Yellow
    if ($script:aiServiceMode -eq "direct") {
        Write-Host "  systemctl restart leyo-ai" -ForegroundColor White
    }
    else {
        Write-Host "  docker compose -f docker-compose.prod.yml up -d ai-service" -ForegroundColor White
    }
}

# 关闭 SSH 会话
Get-SSHSession | Remove-SSHSession | Out-Null
