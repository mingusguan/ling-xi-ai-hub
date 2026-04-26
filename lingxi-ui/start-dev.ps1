# 清理缓存并启动开发服务器
Write-Host "🧹 清理编译缓存..." -ForegroundColor Cyan

# 删除 babel 缓存
if (Test-Path "node_modules\.cache") {
    Remove-Item -Recurse -Force "node_modules\.cache" -ErrorAction SilentlyContinue
    Write-Host "✅ Babel 缓存已清理" -ForegroundColor Green
}

# 删除临时文件
if (Test-Path "dist") {
    Remove-Item -Recurse -Force "dist" -ErrorAction SilentlyContinue
    Write-Host "✅ dist 目录已清理" -ForegroundColor Green
}

Write-Host "🚀 启动开发服务器..." -ForegroundColor Cyan
Write-Host "-----------------------------------" -ForegroundColor Gray

# 启动开发服务器
npm run dev
