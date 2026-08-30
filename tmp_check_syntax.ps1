$content = Get-Content -LiteralPath 'D:\AI Agent\leyoSwimming\scripts\deploy-first-stage.ps1' -Raw
$errors = @()
[System.Management.Automation.PSParser]::Tokenize($content, [ref]$errors)
if ($errors.Count -gt 0) {
    $errors | ForEach-Object {
        Write-Host ("Line " + $_.Token.StartLine + ", Col " + $_.Token.StartColumn + ": " + $_.Message) -ForegroundColor Red
    }
    exit 1
} else {
    Write-Host 'No syntax errors found.' -ForegroundColor Green
}
