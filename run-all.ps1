# Brings up the whole stack in the right order: LocalStack, sandbox seed, backend, ML service, frontend.
# From the repo root, run: .\run-all.ps1
# Uses local Docker + local Java/Maven/Node/Python. No script talks to a real AWS account.
param(
  [switch]$SkipSeed
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

function Assert-Command($Name) {
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Missing required command: $Name"
  }
}

Assert-Command docker; Assert-Command curl; Assert-Command java; Assert-Command mvn;
Assert-Command node; Assert-Command npm; Assert-Command python

Write-Host "==> Starting LocalStack"
docker compose up -d localstack | Out-Host
$ready = $false
for ($i = 0; $i -lt 30; $i++) {
  try { Invoke-RestMethod "http://localhost:4566/_localstack/health" | Out-Null; $ready = $true; break } catch { Start-Sleep 2 }
}
if (-not $ready) { throw "LocalStack did not become healthy." }

if (-not $SkipSeed) {
  Write-Host "==> Seeding sandbox"
  docker compose --profile seed run --rm seed | Out-Host
}

Write-Host "==> Building backend"
# SQLite does not create the parent directory for ./data/optibrain-dev.db.
New-Item -ItemType Directory -Force -Path (Join-Path $Root 'data') | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $Root 'backend\data') | Out-Null
Push-Location (Join-Path $Root 'backend')
try {
  & mvn -q package -DskipTests
  if ($LASTEXITCODE -ne 0) { throw "backend build failed" }
} finally { Pop-Location }

Write-Host "==> Starting backend"
$backend = Start-Process -FilePath java -ArgumentList '-jar','backend\target\backend-0.0.1-SNAPSHOT.jar' -WorkingDirectory $Root -PassThru -WindowStyle Hidden

Write-Host "==> Starting ML service"
$ml = Start-Process -FilePath python -ArgumentList '-m','uvicorn','main:app','--port','8000','--host','127.0.0.1' -WorkingDirectory (Join-Path $Root 'ml-service') -PassThru -WindowStyle Hidden

Write-Host "==> Starting frontend"
$frontend = Start-Process -FilePath npm.cmd -ArgumentList 'run','dev','--','--port','3000' -WorkingDirectory (Join-Path $Root 'frontend') -PassThru -WindowStyle Hidden

$ready = $false
for ($i = 0; $i -lt 60; $i++) {
  try {
    Invoke-RestMethod 'http://localhost:8080/actuator/health' | Out-Null
    Invoke-RestMethod 'http://localhost:8000/health' | Out-Null
    Invoke-WebRequest 'http://localhost:3000' -UseBasicParsing | Out-Null
    $ready = $true; break
  } catch { Start-Sleep 2 }
}
if (-not $ready) {
  Write-Warning "Services did not all become ready on the first wait."
} else { Write-Host "==> Ready" }

Write-Host "Frontend: http://localhost:3000"
Write-Host "Backend:  http://localhost:8080/actuator/health"
Write-Host "ML:       http://localhost:8000/health"
Write-Host "LocalStack: http://localhost:4566"
Write-Host "Stop with Ctrl+C, then run: docker compose stop localstack"

try {
  while ($true) { Start-Sleep -Seconds 1 }
} finally {
  Stop-Process -Id $backend.Id, $ml.Id, $frontend.Id -Force -ErrorAction SilentlyContinue
}
