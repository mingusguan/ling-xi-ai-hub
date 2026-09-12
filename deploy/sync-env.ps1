<#
.SYNOPSIS
  灵犀伴行生产环境 .env 同步工具。

.DESCRIPTION
  deploy/.env 保存的是生产环境真实配置，本地与服务器各存一份，该文件被 .gitignore 忽略、绝不入库。
  本脚本负责两份之间的比对与同步。

  命令：
    compare  只比对本地与服务器的键差异，值打码输出（默认动作，安全只读）。
    pull     从服务器拉取 .env 覆盖本地（拉取前自动备份到 .secrets/）。
    push     把本地 .env 上传到服务器（上传前自动备份服务器原文件）。

  重要：推送时默认保留服务器上的 IMAGE_TAG。CI 每次部署都会用当前 commit SHA
  覆写服务器的 IMAGE_TAG，本地那份通常已过期；若一并推送会让 deploy.sh 拉到旧镜像。

.NOTES
  服务器 .env 只应通过本脚本修改，避免手工编辑与同步互相覆盖。
#>
[CmdletBinding()]
param(
  [ValidateSet('compare', 'pull', 'push')]
  [string]$Action = 'compare',

  [string]$SshAlias = 'obsidian-server',
  [string]$RemoteEnvPath = '/data/mingus/lingxi/deploy/.env',

  [switch]$Force
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $PSScriptRoot '.env'
$secretsDir = Join-Path $repoRoot '.secrets'
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

# 这些键由 CI 的 deploy.sh 写入服务器，本地副本不作权威，推送时保留服务器值。
$serverOwnedKeys = @('IMAGE_TAG')

function Invoke-Remote {
  param([string]$Command, [switch]$AllowFailure)
  $out = ssh -o BatchMode=yes $SshAlias $Command 2>&1
  if ($LASTEXITCODE -ne 0 -and -not $AllowFailure) {
    throw "远程命令失败（exit=$LASTEXITCODE）：$Command`n$out"
  }
  return ($out | Out-String)
}

function Read-EnvMap {
  param([string]$Text)
  $map = [ordered]@{}
  foreach ($line in ($Text -split "`r?`n")) {
    if ($line -match '^\s*#' -or $line -match '^\s*$') { continue }
    if ($line -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
      $map[$Matches[1]] = $Matches[2]
    }
  }
  return $map
}

function Get-MaskedEnv {
  param([System.Collections.Specialized.OrderedDictionary]$Map)
  $masked = [ordered]@{}
  foreach ($key in $Map.Keys) {
    $value = $Map[$key]
    if ($value -eq '') { $masked[$key] = '' ; continue }
    if ($value -match '^(change-me|replace-with)') { $masked[$key] = $value; continue }
    $masked[$key] = "***set(len=$($value.Length))***"
  }
  return $masked
}

function Test-LocalEnv {
  if (-not (Test-Path $envFile)) {
    throw "本地缺少 deploy/.env。先执行：pwsh deploy/sync-env.ps1 -Action pull"
  }
  $text = [System.IO.File]::ReadAllText($envFile, [System.Text.Encoding]::UTF8)
  $map = Read-EnvMap $text
  if ($map.Count -eq 0) { throw 'deploy/.env 为空或格式无法解析。' }
  return @{ Text = $text; Map = $map }
}

function Test-RemoteBusy {
  $swp = (Invoke-Remote "ls $RemoteEnvPath.swp 2>/dev/null || true").Trim()
  if ($swp -ne '' -and -not $Force) {
    throw "服务器上存在 $RemoteEnvPath.swp，可能有人正在编辑。确认安全后加 -Force 继续。"
  }
}

function Save-Backup {
  param([string]$Text, [string]$Tag)
  if (-not (Test-Path $secretsDir)) { New-Item -ItemType Directory -Path $secretsDir | Out-Null }
  $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
  $path = Join-Path $secretsDir "env.$Tag-$stamp.bak"
  [System.IO.File]::WriteAllText($path, $Text, $utf8NoBom)
  return $path
}

function Show-Compare {
  param([string]$LocalText, [string]$RemoteText)
  $local = Read-EnvMap $LocalText
  $remote = Read-EnvMap $RemoteText

  Write-Host '=== 仅本地有（推送会新增到服务器）===' -ForegroundColor Cyan
  $onlyLocal = $local.Keys | Where-Object { -not $remote.Contains($_) }
  if ($onlyLocal) { $onlyLocal | ForEach-Object { Write-Host "  + $_" } } else { Write-Host '  （无）' }

  Write-Host '=== 仅服务器有（拉取会新增到本地；推送会删除服务器上的这些键）===' -ForegroundColor Cyan
  $onlyRemote = $remote.Keys | Where-Object { -not $local.Contains($_) }
  if ($onlyRemote) { $onlyRemote | ForEach-Object { Write-Host "  - $_" } } else { Write-Host '  （无）' }

  Write-Host '=== 取值不同的键 ===' -ForegroundColor Cyan
  $diff = 0
  foreach ($key in $local.Keys) {
    if (-not $remote.Contains($key)) { continue }
    if ($local[$key] -eq $remote[$key]) { continue }
    $diff++
    $localShown = if ($local[$key] -eq '') { '<空>' } else { "len=$($local[$key].Length)" }
    $remoteShown = if ($remote[$key] -eq '') { '<空>' } else { "len=$($remote[$key].Length)" }
    $ownerNote = if ($serverOwnedKeys -contains $key) { '  [服务器属主，push 时保留服务器值]' } else { '' }
    Write-Host "  ~ ${key}: 本地=$localShown 服务器=$remoteShown$ownerNote"
  }
  if ($diff -eq 0) { Write-Host '  （无）' }
  Write-Host ""
  Write-Host "本地键数=$($local.Count)  服务器键数=$($remote.Count)" -ForegroundColor Gray
}

Write-Host "灵犀伴行 .env 同步（action=$Action, 服务器=$SshAlias）" -ForegroundColor Green
Write-Host ""

switch ($Action) {
  'compare' {
    $local = Test-LocalEnv
    $remoteText = Invoke-Remote "cat $RemoteEnvPath"
    Show-Compare -LocalText $local.Text -RemoteText $remoteText
  }

  'pull' {
    $remoteText = Invoke-Remote "cat $RemoteEnvPath"
    if ((Read-EnvMap $remoteText).Count -eq 0) { throw '服务器 .env 为空或无法解析，已中止。' }
    if (Test-Path $envFile) {
      $backup = Save-Backup -Text ([System.IO.File]::ReadAllText($envFile, [System.Text.Encoding]::UTF8)) -Tag 'local-before-pull'
      Write-Host "已备份本地旧文件 -> $backup" -ForegroundColor Yellow
    }
    [System.IO.File]::WriteAllText($envFile, $remoteText, $utf8NoBom)
    $newMap = Read-EnvMap $remoteText
    Write-Host "已拉取服务器 .env -> deploy/.env（$($newMap.Count) 个键）" -ForegroundColor Green
    Write-Host "注意：其中 IMAGE_TAG=$($newMap['IMAGE_TAG']) 是服务器当前部署版本，push 时不会被回写。" -ForegroundColor Gray
  }

  'push' {
    $local = Test-LocalEnv
    Test-RemoteBusy

    $remoteText = Invoke-Remote "cat $RemoteEnvPath"
    $remoteMap = Read-EnvMap $remoteText

    # 用本地内容作为基础，逐键保留服务器属主字段。
    $lines = @()
    foreach ($key in $local.Map.Keys) {
      if (($serverOwnedKeys -contains $key) -and $remoteMap.Contains($key)) {
        $lines += "$key=$($remoteMap[$key])"
      } else {
        $lines += "$key=$($local.Map[$key])"
      }
    }
    $merged = ($lines -join "`n") + "`n"
    $mergedB64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($merged))

    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $remoteBackup = "$RemoteEnvPath.bak-$stamp"
    $script = "set -eu; cp $RemoteEnvPath $remoteBackup; " +
              "printf '%s' '$mergedB64' | base64 -d > $RemoteEnvPath.uploading; " +
              "mv $RemoteEnvPath.uploading $RemoteEnvPath; " +
              "grep -c '^[A-Za-z_]' $RemoteEnvPath"
    $result = Invoke-Remote $script
    Write-Host "已上传（服务器备份：$remoteBackup）" -ForegroundColor Green
    Write-Host "服务器 .env 生效键数：$($result.Trim())"
    Write-Host "实际生效需要重启容器：" -ForegroundColor Yellow
    Write-Host "  ssh $SshAlias 'cd /data/mingus/lingxi/deploy && docker compose --env-file .env -f docker-compose.prod.yml up -d lingxi-admin'"
  }
}
