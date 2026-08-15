#Requires -Version 5.1
<#
.SYNOPSIS
    收集批次复盘原始数据，为更新经验手册（docs/figma/batch{N}-lessons-learned.md）做准备。

.DESCRIPTION
    该脚本不直接调用 AI，而是生成一个 Markdown 输入文件（tmp/batch-retrospective-input-{batch}.md），
    供 batch-retrospective Agent 读取并生成经验手册更新。

.PARAMETER Batch
    批次名称，例如 batch1、batch2。

.PARAMETER StartDate
    批次开始日期，格式 yyyy-MM-dd。

.PARAMETER EndDate
    批次结束日期，格式 yyyy-MM-dd（可选，默认为今天）。

.PARAMETER LogDays
    向前追溯多少天的 git 日志，当未指定 StartDate 时使用。

.EXAMPLE
    .\scripts\update-batch-lessons.ps1 -Batch batch2 -StartDate 2026-08-15 -EndDate 2026-08-28
    .\scripts\update-batch-lessons.ps1 -Batch batch2 -LogDays 14
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$Batch,

    [Parameter(Mandatory = $false)]
    [string]$StartDate,

    [Parameter(Mandatory = $false)]
    [string]$EndDate = (Get-Date -Format 'yyyy-MM-dd'),

    [Parameter(Mandatory = $false)]
    [int]$LogDays = 14
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
$TmpDir = Join-Path $RepoRoot 'tmp'
$OutputFile = Join-Path $TmpDir "batch-retrospective-input-${Batch}.md"

if (-not (Test-Path $TmpDir)) {
    New-Item -ItemType Directory -Force -Path $TmpDir | Out-Null
}

# 1. 确定时间范围
if ([string]::IsNullOrWhiteSpace($StartDate)) {
    $since = (Get-Date).AddDays(-$LogDays).ToString('yyyy-MM-dd')
} else {
    $since = $StartDate
}
$until = $EndDate

# 2. 收集 git 提交信息
$gitFormat = '%H|%ad|%an|%s'
$gitSince = "${since}T00:00:00+08:00"
$gitUntil = "${until}T23:59:59+08:00"

$commits = & git -C $RepoRoot log --all --format=$gitFormat --date=format:'%Y-%m-%d %H:%M:%S' --since=$gitSince --until=$gitUntil |
    ForEach-Object {
        $parts = $_ -split '\|', 4
        [PSCustomObject]@{
            Hash    = $parts[0].Substring(0, 7)
            Date    = $parts[1]
            Author  = $parts[2]
            Message = $parts[3]
        }
    }

# 3. 收集变更文件（按前端源码目录分组）
$changedFiles = & git -C $RepoRoot diff --name-only --since=$gitSince --until=$gitUntil |
    Where-Object { $_ -match '^(miniapp-user|miniapp-coach|web-admin)/' -or $_ -match '^docs/figma/page-spec/' }

$frontendFiles = $changedFiles | Where-Object { $_ -match '^(miniapp-user|miniapp-coach|web-admin)/' }
$specFiles = $changedFiles | Where-Object { $_ -match '^docs/figma/page-spec/' }

# 4. 查找 visual-review 报告（约定存放在 tmp/visual-reviews/）
$visualReviewDir = Join-Path $TmpDir 'visual-reviews'
$visualReviewFiles = @()
if (Test-Path $visualReviewDir) {
    $visualReviewFiles = Get-ChildItem -Path $visualReviewDir -Filter "*.md" -File |
        Where-Object { $_.LastWriteTime -ge [datetime]::Parse($since) -and $_.LastWriteTime -le [datetime]::Parse($until).AddDays(1) } |
        Select-Object -ExpandProperty FullName
}

# 5. 读取现有经验手册（如果存在）
$lessonsFile = Join-Path $RepoRoot "docs\figma\${Batch}-lessons-learned.md"
$existingLessons = ''
if (Test-Path $lessonsFile) {
    $existingLessons = Get-Content -Raw -Path $lessonsFile
}

# 6. 生成复盘输入文件
$sb = [System.Text.StringBuilder]::new()
[void]$sb.AppendLine("# ${Batch} 批次复盘原始数据")
[void]$sb.AppendLine('')
[void]$sb.AppendLine("> 本文件由 `scripts/update-batch-lessons.ps1` 自动生成，供 `batch-retrospective` Agent 读取。")
[void]$sb.AppendLine("> 生成时间：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
[void]$sb.AppendLine('')

[void]$sb.AppendLine("## 1. 时间范围")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("| 项目 | 值 |")
[void]$sb.AppendLine("|------|-----|")
[void]$sb.AppendLine("| 批次 | ${Batch} |")
[void]$sb.AppendLine("| 开始日期 | ${since} |")
[void]$sb.AppendLine("| 结束日期 | ${until} |")
[void]$sb.AppendLine("| Commit 数量 | $($commits.Count) |")
[void]$sb.AppendLine("")

[void]$sb.AppendLine("## 2. Commit 列表")
[void]$sb.AppendLine("")
if ($commits.Count -eq 0) {
    [void]$sb.AppendLine("该时间范围内无提交。")
} else {
    [void]$sb.AppendLine("| Hash | 日期 | 作者 | 消息 |")
    [void]$sb.AppendLine("|------|------|------|------|")
    foreach ($c in $commits) {
        [void]$sb.AppendLine("| $($c.Hash) | $($c.Date) | $($c.Author) | $($c.Message) |")
    }
}
[void]$sb.AppendLine("")

[void]$sb.AppendLine("## 3. 变更文件统计")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("### 3.1 前端源码文件")
[void]$sb.AppendLine("")
if ($frontendFiles.Count -eq 0) {
    [void]$sb.AppendLine("无前端源码变更。")
} else {
    foreach ($f in $frontendFiles) {
        [void]$sb.AppendLine("- $f")
    }
}
[void]$sb.AppendLine("")

[void]$sb.AppendLine("### 3.2 page-spec 文件")
[void]$sb.AppendLine("")
if ($specFiles.Count -eq 0) {
    [void]$sb.AppendLine("无 page-spec 变更。")
} else {
    foreach ($f in $specFiles) {
        [void]$sb.AppendLine("- $f")
    }
}
[void]$sb.AppendLine("")

[void]$sb.AppendLine("## 4. Visual Review 报告")
[void]$sb.AppendLine("")
if ($visualReviewFiles.Count -eq 0) {
    [void]$sb.AppendLine("未找到 visual-review 报告。请确认是否已运行 visual-reviewer Agent 并输出到 `tmp/visual-reviews/`。")
} else {
    foreach ($f in $visualReviewFiles) {
        $rel = Resolve-Path -Relative -Path $f
        [void]$sb.AppendLine("- $rel")
    }
}
[void]$sb.AppendLine("")

[void]$sb.AppendLine("## 5. 现有经验手册内容")
[void]$sb.AppendLine("")
if ([string]::IsNullOrWhiteSpace($existingLessons)) {
    [void]$sb.AppendLine("未找到 ${Batch} 经验手册（${lessonsFile}）。")
} else {
    [void]$sb.AppendLine("文件路径：${lessonsFile}")
    [void]$sb.AppendLine("")
    [void]$sb.AppendLine('```markdown')
    [void]$sb.AppendLine($existingLessons)
    [void]$sb.AppendLine('```')
}
[void]$sb.AppendLine("")

[void]$sb.AppendLine("## 6. 请 Agent 完成的复盘任务")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("请 `batch-retrospective` Agent 基于以上数据：")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("1. 从 Commit 消息、变更文件、visual-review 报告中识别新的视觉还原问题、规范理解问题、流程执行问题、跨端一致性问题。")
[void]$sb.AppendLine("2. 将新增问题按 §2 的格式追加到 `${Batch}-lessons-learned.md` 的「典型问题」章节。")
[void]$sb.AppendLine("3. 如有新的根因或预防措施，更新 §3 / §4 / §5。")
[void]$sb.AppendLine("4. 在变更日志中新增一行。")
[void]$sb.AppendLine("5. 输出「本次复盘新增/修改的内容摘要」到当前会话。")

Set-Content -Path $OutputFile -Value $sb.ToString() -Encoding UTF8

Write-Host "已生成批次复盘输入文件：$OutputFile"
Write-Host "下一步：调用 batch-retrospective Agent 读取该文件并更新 $lessonsFile"

