# 快速修复前端启动问题

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Quick Fix for Frontend Startup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. 停止所有node进程
Write-Host "[1/5] Stopping all Node processes..." -ForegroundColor Yellow
Get-Process -Name node -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 2
Write-Host "      Done!" -ForegroundColor Green
Write-Host ""

# 2. 删除缓存
Write-Host "[2/5] Clearing cache directories..." -ForegroundColor Yellow
if (Test-Path "node_modules\.cache") {
    Remove-Item -Recurse -Force "node_modules\.cache" -ErrorAction SilentlyContinue
    Write-Host "      Deleted .cache" -ForegroundColor Green
}
if (Test-Path ".eslintcache") {
    Remove-Item -Force ".eslintcache" -ErrorAction SilentlyContinue
    Write-Host "      Deleted .eslintcache" -ForegroundColor Green
}
Write-Host ""

# 3. 设置更大的内存限制并启动
Write-Host "[3/5] Starting dev server with optimized settings..." -ForegroundColor Yellow
Write-Host ""
Write-Host "Note: First compilation may take 3-5 minutes, please wait..." -ForegroundColor Cyan
Write-Host ""

# 使用更高的内存限制和禁用某些检查来加速
$env:NODE_OPTIONS="--max_old_space_size=12288"
$env:WEBPACK_DEV_SERVER_INLINE=false
$env:WEBPACK_DEV_SERVER_HOT=true

npm run dev
