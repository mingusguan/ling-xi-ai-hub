# 最终解决方案 - 跳过编译问题库

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ULTIMATE FIX - Skip Problem Libraries" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. 停止所有进程
Write-Host "[1/5] Killing all Node processes..." -ForegroundColor Red
taskkill /F /IM node.exe 2>&1 | Out-Null
Start-Sleep -Seconds 2
Write-Host "      Done!" -ForegroundColor Green
Write-Host ""

# 2. 备份旧配置
Write-Host "[2/5] Backing up old config..." -ForegroundColor Yellow
if (Test-Path "vue.config.js") {
    Copy-Item vue.config.js vue.config.js.old -Force
    Write-Host "      Backup created!" -ForegroundColor Green
}
Write-Host ""

# 3. 使用优化配置
Write-Host "[3/5] Applying optimized configuration..." -ForegroundColor Yellow
Copy-Item vue.config.optimized.js vue.config.js -Force
Write-Host "      Optimized config applied!" -ForegroundColor Green
Write-Host ""

# 4. 清理缓存
Write-Host "[4/5] Cleaning caches..." -ForegroundColor Yellow
Remove-Item -Recurse -Force node_modules\.cache -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force dist -ErrorAction SilentlyContinue
npm cache clean --force 2>&1 | Out-Null
Write-Host "      Caches cleaned!" -ForegroundColor Green
Write-Host ""

# 5. 启动
Write-Host "[5/5] Starting with optimized config..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "This config will:" -ForegroundColor Yellow
Write-Host "  - Skip babel compilation for large libraries" -ForegroundColor White
Write-Host "  - Split echarts and bpmn-js into separate chunks" -ForegroundColor White  
Write-Host "  - Use aggressive caching" -ForegroundColor White
Write-Host ""
Write-Host "Compilation should complete in 2-4 minutes!" -ForegroundColor Green
Write-Host ""
Write-Host "Starting now..." -ForegroundColor Cyan
Write-Host ""

npm run dev
