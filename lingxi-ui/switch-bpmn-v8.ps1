# 快速切换到 bpmn-js 8.x 并重启

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Downgrade bpmn-js to v8.9.0" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. 停止所有 node 进程
Write-Host "[1/4] Stopping Node processes..." -ForegroundColor Red
taskkill /F /IM node.exe 2>&1 | Out-Null
Start-Sleep -Seconds 2
Write-Host "      Done!" -ForegroundColor Green
Write-Host ""

# 2. 删除 node_modules 和 lock 文件
Write-Host "[2/4] Removing old dependencies..." -ForegroundColor Yellow
if (Test-Path "node_modules") {
    $emptyDir = Join-Path $env:TEMP "empty_dir_$(Get-Random)"
    New-Item -ItemType Directory -Force -Path $emptyDir | Out-Null
    robocopy $emptyDir node_modules /MIR /NFL /NDL /NJH | Out-Null
    Remove-Item -Force $emptyDir
    Remove-Item -Force "node_modules" -ErrorAction SilentlyContinue
    Write-Host "      node_modules deleted!" -ForegroundColor Green
}
Remove-Item -Force package-lock.json -ErrorAction SilentlyContinue
Write-Host ""

# 3. 安装依赖（使用 bpmn-js 8.9.0）
Write-Host "[3/4] Installing dependencies with bpmn-js 8.9.0..." -ForegroundColor Yellow
Write-Host "      This will be MUCH faster..." -ForegroundColor Gray
Write-Host ""

npm install --registry=https://registry.npmmirror.com

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "      Installation successful!" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "      Installation failed! Trying default registry..." -ForegroundColor Red
    npm install
}
Write-Host ""

# 4. 启动
Write-Host "[4/4] Starting dev server..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

npm run dev
