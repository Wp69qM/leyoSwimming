$code = Get-Content -Raw 'scripts\deploy-first-stage.ps1'
$err = $null
[System.Management.Automation.PSParser]::Tokenize($code, [ref]$err) | Out-Null
if ($err.Count -eq 0) {
    Write-Host 'SYNTAX OK' -ForegroundColor Green
} else {
    Write-Host 'SYNTAX ERROR:' -ForegroundColor Red
    $err | ForEach-Object { Write-Host $_.Message }
}
