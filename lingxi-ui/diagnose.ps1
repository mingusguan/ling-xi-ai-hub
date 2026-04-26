# 诊断脚本 - 检查编译状态

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Build Process Diagnostic Tool" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查 Node 进程
$nodeProcesses = Get-Process -Name node -ErrorAction SilentlyContinue
if ($nodeProcesses) {
    Write-Host "[OK] Node processes running:" -ForegroundColor Green
    foreach ($proc in $nodeProcesses) {
        $memoryMB = [math]::Round($proc.WorkingSet64 / 1MB, 2)
        Write-Host "     PID: $($proc.Id) | Memory: ${memoryMB} MB | CPU: $($proc.CPU)s" -ForegroundColor White
    }
} else {
    Write-Host "[WARN] No Node processes found!" -ForegroundColor Red
    Write-Host "       The build may have crashed." -ForegroundColor Red
}
Write-Host ""

# 检查内存
$os = Get-WmiObject Win32_OperatingSystem
$totalRAM = [math]::Round($os.TotalVisibleMemorySize / 1MB, 2)
$freeRAM = [math]::Round($os.FreePhysicalMemory / 1MB, 2)
$usedRAM = [math]::Round(($os.TotalVisibleMemorySize - $os.FreePhysicalMemory) / 1MB, 2)

Write-Host "System Memory:" -ForegroundColor Yellow
Write-Host "  Total: ${totalRAM} GB" -ForegroundColor White
Write-Host "  Used:  ${usedRAM} GB" -ForegroundColor White
Write-Host "  Free:  ${freeRAM} GB" -ForegroundColor White
Write-Host ""

# 检查端口占用
Write-Host "Port 80 status:" -ForegroundColor Yellow
$port80 = Get-NetTCPConnection -LocalPort 80 -ErrorAction SilentlyContinue
if ($port80) {
    Write-Host "  Port 80 is IN USE" -ForegroundColor Red
    $port80 | ForEach-Object {
        $proc = Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Host "    By: $($proc.Name) (PID: $($proc.Id))" -ForegroundColor White
        }
    }
} else {
    Write-Host "  Port 80 is FREE" -ForegroundColor Green
}
Write-Host ""

# 检查 node_modules 大小
if (Test-Path "node_modules") {
    $folder = Get-Item "node_modules"
    Write-Host "node_modules folder exists" -ForegroundColor Green
    Write-Host "  Created: $($folder.CreationTime)" -ForegroundColor White
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Recommendation:" -ForegroundColor Yellow
Write-Host ""

if ($freeRAM -lt 2) {
    Write-Host "  [CRITICAL] Low memory! Less than 2GB free." -ForegroundColor Red
    Write-Host "  - Close other applications (Chrome, VS Code, etc.)" -ForegroundColor White
    Write-Host "  - Try restarting your computer" -ForegroundColor White
} elseif ($freeRAM -lt 4) {
    Write-Host "  [WARNING] Memory is tight. ${freeRAM}GB free." -ForegroundColor Yellow
    Write-Host "  - Consider closing some applications" -ForegroundColor White
} else {
    Write-Host "  [OK] Memory looks good. ${freeRAM}GB free." -ForegroundColor Green
    Write-Host ""
    Write-Host "  If build is stuck:" -ForegroundColor Yellow
    Write-Host "  1. Wait 5 more minutes (compiling 2000+ modules takes time)" -ForegroundColor White
    Write-Host "  2. Check if progress numbers are changing" -ForegroundColor White
    Write-Host "  3. If truly stuck, press Ctrl+C and restart" -ForegroundColor White
}

Write-Host ""
Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
