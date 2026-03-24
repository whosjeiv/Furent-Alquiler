# Script para ejecutar Furent con Google OAuth2 configurado
# Windows PowerShell

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Furent - Inicio con Google OAuth2" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar que MongoDB esté corriendo
Write-Host "🔍 Verificando MongoDB..." -ForegroundColor Yellow
$mongoRunning = Get-Process mongod -ErrorAction SilentlyContinue
if (-not $mongoRunning) {
    Write-Host "⚠️  MongoDB no está corriendo." -ForegroundColor Red
    Write-Host "   Opciones:" -ForegroundColor Yellow
    Write-Host "   1. Iniciar MongoDB manualmente" -ForegroundColor White
    Write-Host "   2. Usar Docker: docker-compose up -d mongodb" -ForegroundColor White
    Write-Host ""
    $continue = Read-Host "¿Deseas continuar de todas formas? (s/n)"
    if ($continue -ne "s" -and $continue -ne "S") {
        exit
    }
}
else {
    Write-Host "✅ MongoDB está corriendo" -ForegroundColor Green
}

Write-Host ""
Write-Host "📋 Configuración OAuth2:" -ForegroundColor Yellow
Write-Host "   Client ID: 602363263815-jhqmdp2hqo1dbs9lklorquoulb8ki5ja.apps.googleusercontent.com" -ForegroundColor White
Write-Host "   Redirect URI: http://localhost:8080/login/oauth2/code/google" -ForegroundColor White
Write-Host ""

Write-Host "⚠️  IMPORTANTE: Verifica que el URI de redireccionamiento esté autorizado en:" -ForegroundColor Yellow
Write-Host "   https://console.cloud.google.com/apis/credentials" -ForegroundColor Cyan
Write-Host ""

$ready = Read-Host "¿Está todo configurado en Google Console? (s/n)"
if ($ready -ne "s" -and $ready -ne "S") {
    Write-Host ""
    Write-Host "Por favor, configura los URIs en Google Console y vuelve a ejecutar este script." -ForegroundColor Yellow
    Write-Host "Consulta: CONFIGURACION_GOOGLE_COMPLETADA.md" -ForegroundColor Cyan
    exit
}

Write-Host ""
Write-Host "🚀 Iniciando aplicación..." -ForegroundColor Green
Write-Host ""
Write-Host "Una vez iniciada, abre tu navegador en:" -ForegroundColor Yellow
Write-Host "   http://localhost:8080/login" -ForegroundColor Cyan
Write-Host ""
Write-Host "Presiona Ctrl+C para detener la aplicación" -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Ejecutar la aplicación
.\mvnw.cmd spring-boot:run
