#Requires -Version 5.1
<#
.SYNOPSIS
    基于最近一次 git commit 与当天的会话记忆，自动更新 docs/project-daily-log.md。

.DESCRIPTION
    该脚本通常在 .git/hooks/post-commit 中调用。它会：
    1. 读取最近一次 commit 的信息。
    2. 读取当天（commit 日期）的 memory topics。
    3. 在 docs/project-daily-log.md 中查找或创建对应日期的条目。
    4. 将新的 commit 与 topics 追加到「主要工作」列表中（去重）。

    该脚本不直接调用 AI，只执行确定性的数据合并；冲突或需要人工润色时会在输出中提示。

.PARAMETER Date
    指定日期，格式 yyyy-MM-dd。默认为最近一次 commit 的提交日期（本地时区）。

.PARAMETER LogFile
    日志文件路径。默认为 docs/project-daily-log.md。

.PARAMETER MemoryRoot
    记忆根目录。默认为 c:\Users\EDY\.trae-cn\memory\projects\-d-AI-Agent-leyoSwimming。

.EXAMPLE
    .\scripts\update-daily-log.ps1
    .\scripts\update-daily-log.ps1 -Date 2026-08-14
#>

param(
    [Parameter(Mandatory = $false)]
    [string]$Date,

    [Parameter(Mandatory = $false)]
    [string]$LogFile = (Join-Path (Split-Path -Parent $PSScriptRoot) 'docs\project-daily-log.md'),

    [Parameter(Mandatory = $false)]
    [string]$MemoryRoot = 'c:\Users\EDY\.trae-cn\memory\projects\-d-AI-Agent-leyoSwimming'
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot

# 1. 读取最近一次 commit 信息
$lastCommit = & git -C $RepoRoot log -1 --format='%H|%ad|%an|%s' --date=format:'%Y-%m-%d %H:%M:%S'
if ([string]::IsNullOrWhiteSpace($lastCommit)) {
    Write-Host "未找到任何 commit，退出。"
    exit 0
}

$parts = $lastCommit -split '\|', 4
$commitHash = $parts[0]
$commitDateTime = $parts[1]
$commitAuthor = $parts[2]
$commitMessage = $parts[3]

# 2. 确定目标日期
if ([string]::IsNullOrWhiteSpace($Date)) {
    $targetDate = $commitDateTime.Substring(0, 10)
} else {
    $targetDate = $Date
}

# 3. 获取当天的所有 commit（按提交者日期）
$dayStart = "${targetDate}T00:00:00+08:00"
$dayEnd = "${targetDate}T23:59:59+08:00"
$dayCommits = & git -C $RepoRoot log --all --format='%H|%ad|%an|%s' --date=format:'%Y-%m-%d %H:%M:%S' --since=$dayStart --until=$dayEnd |
    ForEach-Object {
        $p = $_ -split '\|', 4
        [PSCustomObject]@{
            Hash    = $p[0].Substring(0, 7)
            Date    = $p[1]
            Author  = $p[2]
            Message = $p[3]
        }
    }

# 4. 读取当天的 memory topics（如果存在）
$memoryDateFolder = Join-Path $MemoryRoot ($targetDate -replace '-', '')
$topicsFile = Join-Path $memoryDateFolder 'topics.md'
$memoryTopics = @()

if (Test-Path $topicsFile) {
    $topicsContent = Get-Content -Raw -Path $topicsFile -Encoding UTF8
    # 简单解析：每个 topic 以 [session_id: ...] 开头
    $matches = [regex]::Matches($topicsContent, '\[session_id:\s*([^\s|]+)\s*\|\s*topic_summary_time:\s*([\d\-:\s]+)\](.*?)(?=\[session_id:|\Z)', [System.Text.RegularExpressions.RegexOptions]::Singleline)
    foreach ($m in $matches) {
        $memoryTopics += [PSCustomObject]@{
            SessionId = $m.Groups[1].Value.Trim()
            Time      = $m.Groups[2].Value.Trim()
            Content   = ($m.Groups[3].Value.Trim() -replace '\s+', ' ').Substring(0, [Math]::Min(300, $m.Groups[3].Value.Trim().Length))
        }
    }
}

# 5. 读取现有日志文件
$logContent = ''
if (Test-Path $LogFile) {
    $logContent = Get-Content -Raw -Path $LogFile -Encoding UTF8
}

# 6. 生成当天条目内容
$dateHeader = "## ${targetDate}"
$newWorkItems = [System.Collections.Generic.List[string]]::new()

foreach ($c in $dayCommits) {
    $item = "- 提交 [$($c.Hash)] $($c.Message)"
    if (-not $newWorkItems.Contains($item)) {
        $newWorkItems.Add($item)
    }
}

foreach ($t in $memoryTopics) {
    $item = "- 会话 [$($t.SessionId)] $($t.Content)"
    if (-not $newWorkItems.Contains($item)) {
        $newWorkItems.Add($item)
    }
}

if ($newWorkItems.Count -eq 0) {
    Write-Host "当天无新 commit 或 topics，无需更新。"
    exit 0
}

$workItemsText = $newWorkItems -join "`n"

# 7. 合并到日志文件
function Get-NextHeaderOffset {
    param([string]$text, [int]$startIndex)
    $next = $text.IndexOf('`n## ', $startIndex)
    if ($next -lt 0) { return $text.Length }
    return $next + 1
}

$headerIndex = $logContent.IndexOf("`n${dateHeader}")
if ($headerIndex -lt 0) {
    $headerIndex = $logContent.IndexOf("${dateHeader}`n")
}

if ($headerIndex -ge 0) {
    # 找到现有日期条目，在其后追加「自动补充记录」小节，不破坏原有人工总结
    $sectionStart = $headerIndex
    $sectionEnd = Get-NextHeaderOffset -text $logContent -startIndex ($sectionStart + 1)

    $autoSection = @"

### [auto] ${targetDate} 补充记录

> 本小节由 `scripts/update-daily-log.ps1` 在 commit 后自动生成，用于保留原始 commit 与会话记录。
> 建议每日结束时由 Agent/人工基于本节内容更新上方的精炼总结。

${workItemsText}
"@

    # 找到日期条目内的分隔线 "\n---\n"，在其之前插入自动补充小节
    $sectionEnd = Get-NextHeaderOffset -text $logContent -startIndex ($sectionStart + 1)
    $section = $logContent.Substring($sectionStart, $sectionEnd - $sectionStart)
    $dividerIndex = $section.LastIndexOf("`n---`n")
    if ($dividerIndex -ge 0) {
        $insertPos = $sectionStart + $dividerIndex
    } else {
        $insertPos = $sectionEnd
    }
    $logContent = $logContent.Insert($insertPos, $autoSection)
} else {
    # 在文件末尾新增日期条目
    $newSection = @"

${dateHeader}

**主要工作**：

${workItemsText}

**关键决策**：

- （待补充）

**项目阶段**：

- （待补充）

---
"@
    $logContent = $logContent.TrimEnd() + "`n${newSection}"
}

# 8. 写回文件
Set-Content -Path $LogFile -Value $logContent.TrimStart() -Encoding UTF8

Write-Host "已更新每日工作日志：$LogFile"
Write-Host "更新日期：$targetDate"
Write-Host "新增条目数：$($newWorkItems.Count)"



