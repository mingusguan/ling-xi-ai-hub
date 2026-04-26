# 终极解决方案：完全重装依赖

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Vue Project Complete Reset Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. 停止所有 node 进程
Write-Host "[1/5] Stopping all Node processes..." -ForegroundColor Yellow
Get-Process -Name node -ErrorAction SilentlyContinue | Stop-Process -Force
Get-Process -Name npm -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 2
Write-Host "      Done!" -ForegroundColor Green
Write-Host ""

# 2. 删除 node_modules
Write-Host "[2/5] Removing node_modules folder..." -ForegroundColor Yellow
if (Test-Path "node_modules") {
    Remove-Item -Recurse -Force node_modules
    Write-Host "      node_modules deleted!" -ForegroundColor Green
} else {
    Write-Host "      node_modules not found, skipping..." -ForegroundColor Gray
}
Write-Host ""

# 3. 删除 lock 文件和缓存
Write-Host "[3/5] Cleaning lock files and caches..." -ForegroundColor Yellow
if (Test-Path "package-lock.json") {
    Remove-Item -Force package-lock.json
    Write-Host "      package-lock.json deleted!" -ForegroundColor Green
}
if (Test-Path "node_modules\.cache") {
    Remove-Item -Recurse -Force node_modules\.cache
    Write-Host "      cache deleted!" -ForegroundColor Green
}
if (Test-Path "dist") {
    Remove-Item -Recurse -Force dist
    Write-Host "      dist deleted!" -ForegroundColor Green
}
npm cache clean --force 2>&1 | Out-Null
Write-Host "      All caches cleaned!" -ForegroundColor Green
Write-Host ""

# 4. 重新安装依赖
Write-Host "[4/5] Installing dependencies (this may take 3-5 minutes)..." -ForegroundColor Yellow
Write-Host "      Using npm mirror for faster download..." -ForegroundColor Gray
npm install --registry=https://registry.npmmirror.com
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "      npm install failed! Trying with default registry..." -ForegroundColor Red
    npm install
}
Write-Host ""
Write-Host "      Dependencies installed successfully!" -ForegroundColor Green
Write-Host ""

# 5. 启动开发服务器
Write-Host "[5/5] Starting development server..." -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

npm run dev
