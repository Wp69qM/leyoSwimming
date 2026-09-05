# 一键启动所有本地开发服务
$root = "d:\AI Agent\leyoSwimming"

# 先清理旧日志，方便查看
Remove-Item -Path "$root\ai-service\dev.log" -ErrorAction SilentlyContinue
Remove-Item -Path "$root\backend\dev.log" -ErrorAction SilentlyContinue
Remove-Item -Path "$root\web-admin\dev.log" -ErrorAction SilentlyContinue
Remove-Item -Path "$root\miniapp-user\dev.log" -ErrorAction SilentlyContinue

# backend 用 ps1（内部调用 bat，绕过 PowerShell $env 被过滤的问题）
$log = "$root\backend\dev.log"
$backend = Start-Process -FilePath "powershell" -ArgumentList "-ExecutionPolicy", "Bypass", "-NoExit", "-Command", "cd `"$root\backend`"; .\start-dev.ps1 *> `"$log`"" -PassThru -WindowStyle Hidden
Write-Host "[backend] PID: $($backend.Id)"

# ai-service
$log = "$root\ai-service\dev.log"
$ai = Start-Process -FilePath "powershell" -ArgumentList "-ExecutionPolicy", "Bypass", "-NoExit", "-Command", "cd `"$root\ai-service`"; .\start-dev.ps1 *> `"$log`"" -PassThru -WindowStyle Hidden
Write-Host "[ai-service] PID: $($ai.Id)"

# web-admin
$log = "$root\web-admin\dev.log"
$web = Start-Process -FilePath "powershell" -ArgumentList "-ExecutionPolicy", "Bypass", "-NoExit", "-Command", "cd `"$root\web-admin`"; .\start-dev.ps1 *> `"$log`"" -PassThru -WindowStyle Hidden
Write-Host "[web-admin] PID: $($web.Id)"

# miniapp-user
$log = "$root\miniapp-user\dev.log"
$mini = Start-Process -FilePath "powershell" -ArgumentList "-ExecutionPolicy", "Bypass", "-NoExit", "-Command", "cd `"$root\miniapp-user`"; .\start-dev.ps1 *> `"$log`"" -PassThru -WindowStyle Hidden
Write-Host "[miniapp-user] PID: $($mini.Id)"

Write-Host "所有服务已启动，请等待 30~60 秒完成首次编译..."
