#Requires -Version 5.1
<#
.SYNOPSIS
    安装项目自定义 Git hooks（post-commit 等）到 .git/hooks/。

.DESCRIPTION
    Git hooks 不会随仓库提交，因此新 clone 或 hooks 被覆盖后需要重新安装。
    本脚本将 scripts/git-hooks/ 下的 hook 文件复制到 .git/hooks/，并保留备份。

.EXAMPLE
    .\scripts\install-git-hooks.ps1
#>

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
$SourceDir = Join-Path $PSScriptRoot 'git-hooks'
$TargetDir = Join-Path $RepoRoot '.git\hooks'

if (-not (Test-Path $TargetDir)) {
    throw "未找到 .git/hooks 目录。请在 Git 仓库根目录运行本脚本。"
}

$hooks = Get-ChildItem -Path $SourceDir -File
foreach ($hook in $hooks) {
    $targetPath = Join-Path $TargetDir $hook.Name
    $backupPath = "${targetPath}.backup"

    if (Test-Path $targetPath) {
        Copy-Item -Path $targetPath -Destination $backupPath -Force
        Write-Host "已备份现有 hook：$($hook.Name) -> $($hook.Name).backup"
    }

    Copy-Item -Path $hook.FullName -Destination $targetPath -Force
    Write-Host "已安装 hook：$($hook.Name)"
}

Write-Host "`nGit hooks 安装完成。"
Write-Host "你可以通过提交一次测试来验证：git commit --allow-empty -m \"test hook\""

