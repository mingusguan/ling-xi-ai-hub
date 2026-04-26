# 强制停止所有 node 进程
Write-Host "Stopping all Node processes..." -ForegroundColor Red
Get-Process -Name node -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 2

# 清理所有缓存
Write-Host "Cleaning cache..." -ForegroundColor Cyan
Remove-Item -Recurse -Force node_modules\.cache -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force dist -ErrorAction SilentlyContinue
Write-Host "Cache cleaned successfully" -ForegroundColor Green

# 清理 npm 缓存
Write-Host "Cleaning npm cache..." -ForegroundColor Cyan
npm cache clean --force

Write-Host "-----------------------------------" -ForegroundColor Gray
Write-Host "Starting dev server..." -ForegroundColor Cyan
Write-Host "-----------------------------------" -ForegroundColor Gray

# 启动开发服务器
npm run dev
