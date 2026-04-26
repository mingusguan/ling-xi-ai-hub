# 测试是否是环境问题

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Environment Diagnostic" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. 检查 Node 版本
Write-Host "[1/6] Node.js version:" -ForegroundColor Yellow
$nodeVersion = node --version
Write-Host "      $nodeVersion" -ForegroundColor White
Write-Host ""

# 2. 检查 npm 版本
Write-Host "[2/6] npm version:" -ForegroundColor Yellow
$npmVersion = npm --version
Write-Host "      $npmVersion" -ForegroundColor White
Write-Host ""

# 3. 检查磁盘空间
Write-Host "[3/6] Disk space:" -ForegroundColor Yellow
$disk = Get-PSDrive C
$freeGB = [math]::Round($disk.Free / 1GB, 2)
Write-Host "      C: Drive - Free: ${freeGB} GB" -ForegroundColor White
if ($freeGB -lt 5) {
    Write-Host "      [WARNING] Low disk space!" -ForegroundColor Red
}
Write-Host ""

# 4. 检查防病毒软件
Write-Host "[4/6] Windows Defender status:" -ForegroundColor Yellow
$defender = Get-MpPreference -ErrorAction SilentlyContinue
if ($defender) {
    $realTimeProtection = $defender.DisableRealtimeMonitoring
    if ($realTimeProtection -eq $false) {
        Write-Host "      Real-time protection: ON (may slow down compilation)" -ForegroundColor Yellow
        Write-Host "      Consider adding node_modules to exclusion list" -ForegroundColor Gray
    } else {
        Write-Host "      Real-time protection: OFF" -ForegroundColor Green
    }
}
Write-Host ""

# 5. 检查是否有其他 node 进程
Write-Host "[5/6] Node processes:" -ForegroundColor Yellow
$nodeProcs = Get-Process -Name node -ErrorAction SilentlyContinue
if ($nodeProcs) {
    Write-Host "      Found $($nodeProcs.Count) node process(es)" -ForegroundColor Red
    Write-Host "      These may be causing conflicts!" -ForegroundColor Red
} else {
    Write-Host "      No node processes running" -ForegroundColor Green
}
Write-Host ""

# 6. 测试简单的 npm 命令
Write-Host "[6/6] Testing npm..." -ForegroundColor Yellow
npm --version > $null 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "      npm is working" -ForegroundColor Green
} else {
    Write-Host "      npm has issues!" -ForegroundColor Red
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Recommendations:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Node version should be 14.x or 16.x (not 17+)" -ForegroundColor White
Write-Host "2. Free disk space should be > 10GB" -ForegroundColor White
Write-Host "3. Add node_modules to antivirus exclusion" -ForegroundColor White
Write-Host "4. Kill all node processes before starting" -ForegroundColor White
Write-Host ""
Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
