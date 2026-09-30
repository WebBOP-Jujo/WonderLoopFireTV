# ==============================================================================
# WonderLoop — Despliegue Automatizado en Google Cloud Run
# RevenueCat Shipaton 2026
# ==============================================================================

param(
    [string]$ProjectId = "",
    [string]$Region = "europe-southwest1",
    [string]$ServiceName = "wonderloop-backend",
    [string]$GeminiApiKey = ""
)

Write-Host "☁️ ===========================================================" -ForegroundColor Cyan
Write-Host "   WonderLoop Backend — Despliegue en Google Cloud Run" -ForegroundColor Cyan
Write-Host "===========================================================" -ForegroundColor Cyan

# 1. Verificar si gcloud está disponible
$gcloudInstalled = Get-Command "gcloud" -ErrorAction SilentlyContinue

if (-not $gcloudInstalled) {
    Write-Host "❌ El comando 'gcloud' no está instalado en tu PATH." -ForegroundColor Red
    Write-Host "`nPara instalarlo en Windows ejecuta:" -ForegroundColor Yellow
    Write-Host "   winget install Google.CloudSDK" -ForegroundColor White
    Write-Host "Luego reinicia tu terminal PowerShell y ejecuta:" -ForegroundColor Yellow
    Write-Host "   gcloud auth login" -ForegroundColor White
    exit 1
}

# 2. Verificar ProjectId
if ([string]::IsNullOrWhiteSpace($ProjectId)) {
    # Intentar obtener el proyecto activo configurado en gcloud
    $ProjectId = (gcloud config get-value project 2>$null).Trim()
    if ([string]::IsNullOrWhiteSpace($ProjectId)) {
        Write-Host "⚠️ Debes especificar el ID de tu proyecto de Google Cloud." -ForegroundColor Yellow
        Write-Host "Ejemplo de uso: .\deploy-cloudrun.ps1 -ProjectId 'mi-proyecto-id'" -ForegroundColor White
        exit 1
    }
}

# 3. Extraer GEMINI_API_KEY de .env si no fue provista
if ([string]::IsNullOrWhiteSpace($GeminiApiKey)) {
    if (Test-Path ".env") {
        $envLines = Get-Content ".env"
        foreach ($line in $envLines) {
            if ($line -match "^GEMINI_API_KEY=(.+)$") {
                $GeminiApiKey = $Matches[1].Trim()
                break
            }
        }
    }
}

if ([string]::IsNullOrWhiteSpace($GeminiApiKey)) {
    Write-Host "⚠️ No se detectó GEMINI_API_KEY en .env ni como parámetro." -ForegroundColor Yellow
    Write-Host "Asegúrate de configurar GEMINI_API_KEY en las variables de entorno de Cloud Run." -ForegroundColor Gray
}

Write-Host "✅ Google Cloud SDK detectado." -ForegroundColor Green
Write-Host "📌 Proyecto: $ProjectId" -ForegroundColor Cyan
Write-Host "📌 Región: $Region (Madrid)" -ForegroundColor Cyan
Write-Host "📌 Servicio: $ServiceName" -ForegroundColor Cyan
Write-Host "📦 Compilando y desplegando con Cloud Build (usando Dockerfile multi-stage)..." -ForegroundColor Yellow

$envVars = "NODE_ENV=production,APP_SECRET_KEY=change-me-to-a-long-random-secret"
if (-not [string]::IsNullOrWhiteSpace($GeminiApiKey)) {
    $envVars += ",GEMINI_API_KEY=$GeminiApiKey"
}

# Ejecutar despliegue
gcloud run deploy $ServiceName `
    --source . `
    --project $ProjectId `
    --region $Region `
    --allow-unauthenticated `
    --timeout 300s `
    --max-instances 2 `
    --memory 512Mi `
    --set-env-vars "$envVars"

if ($LASTEXITCODE -eq 0) {
    $ServiceUrl = gcloud run services describe $ServiceName --project $ProjectId --region $Region --format "value(status.url)"
    Write-Host "`n🎉 [ÉXITO TOTAL] Backend disponible 24/7 en Cloud Run:" -ForegroundColor Green
    Write-Host "🔗 URL pública: $ServiceUrl" -ForegroundColor Cyan
    Write-Host "🩺 Comprobar salud: $ServiceUrl/api/story/health" -ForegroundColor Yellow
    Write-Host "`n👉 Ahora pega esta URL en WonderLoopApi.kt para conectar la App Android." -ForegroundColor White
} else {
    Write-Host "❌ Error durante el despliegue. Revisa los mensajes de error arriba." -ForegroundColor Red
}
