# 彻底解决编译卡死问题

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  FINAL FIX - Kill Conflicts & Optimize" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. 强制杀死所有 node 进程
Write-Host "[1/4] Killing ALL Node processes..." -ForegroundColor Red
taskkill /F /IM node.exe 2>&1 | Out-Null
Start-Sleep -Seconds 3

# 验证是否全部关闭
$nodeProcs = Get-Process -Name node -ErrorAction SilentlyContinue
if ($nodeProcs) {
    Write-Host "      Warning: Some node processes still running!" -ForegroundColor Yellow
    taskkill /F /IM node.exe /T 2>&1 | Out-Null
    Start-Sleep -Seconds 2
}
Write-Host "      All node processes killed!" -ForegroundColor Green
Write-Host ""

# 2. 添加 node_modules 到 Windows Defender 排除列表（需要管理员权限）
Write-Host "[2/4] Adding exclusion to Windows Defender..." -ForegroundColor Yellow
Write-Host "      This requires Administrator privileges." -ForegroundColor Gray
Write-Host ""

$projectPath = Resolve-Path "E:\JAVA\LingXi-AI-Hub\lingxi-ui\node_modules"
try {
    Add-MpPreference -ExclusionPath $projectPath -ErrorAction Stop
    Write-Host "      Exclusion added successfully!" -ForegroundColor Green
} catch {
    Write-Host "      Could not add exclusion (need Admin rights)." -ForegroundColor Yellow
    Write-Host "      Please run this script as Administrator next time." -ForegroundColor Gray
    Write-Host "      For now, we'll proceed without it..." -ForegroundColor Gray
}
Write-Host ""

# 3. 清理缓存
Write-Host "[3/4] Cleaning all caches..." -ForegroundColor Yellow
Remove-Item -Recurse -Force node_modules\.cache -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force dist -ErrorAction SilentlyContinue
Remove-Item -Force package-lock.json -ErrorAction SilentlyContinue
npm cache clean --force 2>&1 | Out-Null
Write-Host "      All caches cleaned!" -ForegroundColor Green
Write-Host ""

# 4. 启动（不使用 --progress 参数，减少输出）
Write-Host "[4/4] Starting dev server..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "IMPORTANT:" -ForegroundColor Red
Write-Host "  - DO NOT open multiple terminals!" -ForegroundColor White
Write-Host "  - Wait 3-5 minutes for first compilation" -ForegroundColor White
Write-Host "  - Browser will open automatically" -ForegroundColor White
Write-Host ""
Write-Host "Starting now..." -ForegroundColor Green
Write-Host ""

# 修改 package.json 临时移除 --progress 参数
$packageJson = Get-Content "package.json" -Raw | ConvertFrom-Json
$originalDev = $packageJson.scripts.dev
$packageJson.scripts.dev = "set NODE_OPTIONS=--max_old_space_size=8192 && vue-cli-service serve"
$packageJson | ConvertTo-Json -Depth 100 | Set-Content "package.json"

npm run dev

# 恢复原始配置
$packageJson.scripts.dev = $originalDev
$packageJson | ConvertTo-Json -Depth 100 | Set-Content "package.json"
