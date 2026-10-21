# ==============================================================================
# WonderLoop TV - deploy the backend to an Amazon EC2 instance (run from your PC)
#
#   cd backend
#   .\deploy\deploy-ec2.ps1 -Ip 3.85.10.20 -KeyPath C:\Users\me\.ssh\wonderloop-key.pem
#   (optional) -Domain api.mydomain.com   -> otherwise <ip-with-dashes>.sslip.io is used
#
# What it does:
#   1. Builds a production .env from backend/.env WITHOUT the local AWS access keys
#      (on EC2 the instance role provides AWS credentials).
#   2. Uploads the backend source (no node_modules / dist / .env) over SSH.
#   3. Runs deploy/bootstrap.sh on the instance: Docker + Compose + Caddy (HTTPS) + build + start.
#   4. Waits until https://<domain>/api/story/health answers.
# Re-run it any time to deploy a new version.
# ==============================================================================

param(
    [Parameter(Mandatory = $true)][string]$Ip,
    [Parameter(Mandatory = $true)][string]$KeyPath,
    [string]$Domain = "",
    [string]$User = "ec2-user"
)

# "Continue": Windows PowerShell 5.1 turns harmless native stderr lines (ssh/scp warnings) into terminating
# errors under "Stop" when output is redirected. Native failures are detected through $LASTEXITCODE instead.
$ErrorActionPreference = "Continue"
$BackendDir = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($Domain)) {
    $Domain = ($Ip -replace '\.', '-') + ".sslip.io"
}
$Target = "$User@$Ip"
$SshOpts = @("-i", $KeyPath, "-o", "StrictHostKeyChecking=accept-new", "-o", "ConnectTimeout=15")

Write-Host "== WonderLoop TV backend -> EC2 $Ip (https://$Domain)" -ForegroundColor Cyan

# --- 0. SSH key permissions (OpenSSH on Windows rejects keys readable by other users) ---
if (-not (Test-Path $KeyPath)) { throw "Key file not found: $KeyPath" }
icacls $KeyPath /inheritance:r /grant:r "$($env:USERNAME):(R)" | Out-Null

# --- 1. Production environment file ---
$LocalEnv = Join-Path $BackendDir ".env"
if (-not (Test-Path $LocalEnv)) { throw "backend/.env not found" }

$skipKeys = @("AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY", "AWS_SESSION_TOKEN", "PORT", "NODE_ENV", "TRUST_PROXY", "PUBLIC_BASE_URL", "LAN_IP")
$values = @{}
$prodLines = New-Object System.Collections.Generic.List[string]
foreach ($line in Get-Content $LocalEnv) {
    if ($line -match '^\s*([A-Z0-9_]+)\s*=(.*)$') {
        $key = $Matches[1]
        $values[$key] = $Matches[2].Trim()
        if ($skipKeys -notcontains $key) { $prodLines.Add("$key=$($values[$key])") }
    }
}
if (-not $values["APP_SECRET_KEY"] -or $values["APP_SECRET_KEY"].Length -lt 32) { throw "APP_SECRET_KEY (>= 32 chars) missing in backend/.env" }
if (-not $values["GEMINI_API_KEY"]) { throw "GEMINI_API_KEY missing in backend/.env" }
if (-not $values["AWS_REGION"]) { $prodLines.Add("AWS_REGION=us-east-1") }
$prodLines.Add("NODE_ENV=production")
$prodLines.Add("PUBLIC_BASE_URL=https://$Domain")

$TmpDir = Join-Path $env:TEMP ("wonderloop-deploy-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $TmpDir | Out-Null
$ProdEnv = Join-Path $TmpDir "wonderloop.env"
# LF line endings, no BOM: the file is read by Docker on Linux
[System.IO.File]::WriteAllText($ProdEnv, (($prodLines -join "`n") + "`n"), (New-Object System.Text.UTF8Encoding($false)))

try {
    # --- 2. Source bundle ---
    $Bundle = Join-Path $TmpDir "backend.tgz"
    Write-Host ">> Packing backend source" -ForegroundColor Yellow
    & "$env:SystemRoot\System32\tar.exe" -czf $Bundle --exclude=node_modules --exclude=dist --exclude=.env --exclude=*.log -C $BackendDir .
    if ($LASTEXITCODE -ne 0) { throw "tar failed" }

    Write-Host ">> Uploading to $Target" -ForegroundColor Yellow
    & scp @SshOpts $Bundle $ProdEnv "${Target}:/tmp/"
    if ($LASTEXITCODE -ne 0) { throw "scp failed (check the IP, the key and that port 22 is open for your IP)" }

    # --- 3. Remote install + start ---
    Write-Host ">> Building and starting containers on the instance (first run takes a few minutes)" -ForegroundColor Yellow
    $remote = "set -e; sudo mkdir -p /opt/wonderloop/backend; sudo rm -rf /opt/wonderloop/backend/src; " +
              "sudo tar -xzf /tmp/backend.tgz -C /opt/wonderloop/backend; rm -f /tmp/backend.tgz; " +
              "sudo sed -i 's/\r$//' /opt/wonderloop/backend/deploy/bootstrap.sh /opt/wonderloop/backend/deploy/Caddyfile /opt/wonderloop/backend/deploy/docker-compose.yml; " +
              "sudo bash /opt/wonderloop/backend/deploy/bootstrap.sh $Domain"
    & ssh @SshOpts $Target $remote
    if ($LASTEXITCODE -ne 0) { throw "remote bootstrap failed" }
}
finally {
    Remove-Item -Recurse -Force $TmpDir -ErrorAction SilentlyContinue
}

# --- 4. Health check over HTTPS ---
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$HealthUrl = "https://$Domain/api/story/health"
Write-Host ">> Waiting for $HealthUrl (certificate issuance can take up to a minute)" -ForegroundColor Yellow
$ok = $false
for ($i = 0; $i -lt 24 -and -not $ok; $i++) {
    try {
        $resp = Invoke-WebRequest -Uri $HealthUrl -UseBasicParsing -TimeoutSec 10
        if ($resp.StatusCode -eq 200) { $ok = $true }
    } catch {
        Start-Sleep -Seconds 5
    }
}

if ($ok) {
    Write-Host "`nDeployed: https://$Domain" -ForegroundColor Green
    Write-Host "Providers: https://$Domain/" -ForegroundColor Green
} else {
    Write-Host "`nThe health check did not answer yet. Inspect the logs with:" -ForegroundColor Red
    Write-Host "  ssh -i $KeyPath $Target 'cd /opt/wonderloop/backend/deploy && sudo docker compose logs --tail 80'" -ForegroundColor White
    exit 1
}
