# 后台启动，不显示编译进度

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Starting Dev Server (Background)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Starting webpack compilation..." -ForegroundColor Yellow
Write-Host "This will take 3-8 minutes on first run." -ForegroundColor Gray
Write-Host "The browser will open automatically when ready." -ForegroundColor Green
Write-Host ""
Write-Host "DO NOT close this window!" -ForegroundColor Red
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 后台启动，不阻塞
$process = Start-Process -FilePath "npm" -ArgumentList "run", "dev" -PassThru -WindowStyle Normal

Write-Host ""
Write-Host "Dev server is starting..." -ForegroundColor Green
Write-Host "Process ID: $($process.Id)" -ForegroundColor Gray
Write-Host ""
Write-Host "Waiting for compilation to complete..." -ForegroundColor Yellow
Write-Host "Please wait 5-8 minutes for first run." -ForegroundColor Cyan
Write-Host ""
Write-Host "You can minimize this window and do other work." -ForegroundColor Green
Write-Host "The browser will open automatically when ready!" -ForegroundColor Green
Write-Host ""
Write-Host "Press any key to exit this monitor window..." -ForegroundColor Gray
Write-Host "(The dev server will continue running in background)" -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
