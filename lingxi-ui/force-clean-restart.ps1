# 强制清理并重启 - 终极版

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  FORCE CLEAN & RESTART" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. 强制杀死所有 node 进程
Write-Host "[1/4] Killing ALL Node processes..." -ForegroundColor Red
taskkill /F /IM node.exe 2>&1 | Out-Null
taskkill /F /IM npm.exe 2>&1 | Out-Null
Start-Sleep -Seconds 3
Write-Host "      Done!" -ForegroundColor Green
Write-Host ""

# 2. 强制删除 node_modules (使用 robocopy 方法，更快)
Write-Host "[2/4] Deleting node_modules (this takes 30-60 seconds)..." -ForegroundColor Yellow
if (Test-Path "node_modules") {
    # 创建空目录用于 robocopy
    $emptyDir = Join-Path $env:TEMP "empty_dir_$(Get-Random)"
    New-Item -ItemType Directory -Force -Path $emptyDir | Out-Null
    
    # 使用 robocopy 快速删除
    robocopy $emptyDir node_modules /MIR /NFL /NDL /NJH | Out-Null
    Remove-Item -Force $emptyDir
    Remove-Item -Force "node_modules" -ErrorAction SilentlyContinue
    
    Write-Host "      node_modules deleted!" -ForegroundColor Green
}
Write-Host ""

# 3. 清理所有缓存
Write-Host "[3/4] Cleaning all caches..." -ForegroundColor Yellow
Remove-Item -Force package-lock.json -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force .cache -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force dist -ErrorAction SilentlyContinue
npm cache clean --force 2>&1 | Out-Null
Write-Host "      All caches cleaned!" -ForegroundColor Green
Write-Host ""

# 4. 重新安装并启动
Write-Host "[4/4] Installing dependencies..." -ForegroundColor Yellow
Write-Host "      Using Taobao mirror (faster in China)..." -ForegroundColor Gray
Write-Host ""

npm install --registry=https://registry.npmmirror.com --no-optional

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  Installation SUCCESSFUL!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Starting dev server in 3 seconds..." -ForegroundColor Cyan
    Start-Sleep -Seconds 3
    Write-Host ""
    
    npm run dev
} else {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "  Installation FAILED!" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Trying with default registry..." -ForegroundColor Yellow
    npm install
}
