# Script de configuración para Google OAuth2 (PowerShell)
# Este script te ayuda a configurar las variables de entorno necesarias

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  Configuración de Google OAuth2 - Furent" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar si ya existen las variables
if ($env:GOOGLE_CLIENT_ID -and $env:GOOGLE_CLIENT_SECRET) {
    Write-Host "✅ Las variables de entorno ya están configuradas:" -ForegroundColor Green
    Write-Host "   GOOGLE_CLIENT_ID: $($env:GOOGLE_CLIENT_ID.Substring(0, [Math]::Min(20, $env:GOOGLE_CLIENT_ID.Length)))..."
    Write-Host "   GOOGLE_CLIENT_SECRET: $($env:GOOGLE_CLIENT_SECRET.Substring(0, [Math]::Min(10, $env:GOOGLE_CLIENT_SECRET.Length)))..."
    Write-Host ""
    $reconfig = Read-Host "¿Deseas reconfigurarlas? (s/n)"
    if ($reconfig -ne "s" -and $reconfig -ne "S") {
        Write-Host "Configuración cancelada." -ForegroundColor Yellow
        exit
    }
}

Write-Host ""
Write-Host "📋 Pasos previos:" -ForegroundColor Yellow
Write-Host "1. Ve a https://console.cloud.google.com/"
Write-Host "2. Crea un proyecto o selecciona uno existente"
Write-Host "3. Habilita la API de Google+"
Write-Host "4. Crea credenciales OAuth 2.0 (Aplicación web)"
Write-Host "5. Agrega el URI de redireccionamiento:"
Write-Host "   http://localhost:8080/login/oauth2/code/google" -ForegroundColor Cyan
Write-Host ""

$ready = Read-Host "¿Ya completaste estos pasos? (s/n)"
if ($ready -ne "s" -and $ready -ne "S") {
    Write-Host "Por favor, completa los pasos previos y vuelve a ejecutar este script." -ForegroundColor Yellow
    exit
}

Write-Host ""
Write-Host "Ingresa tus credenciales de Google OAuth2:" -ForegroundColor Yellow
Write-Host ""

$client_id = Read-Host "Google Client ID"
$client_secret = Read-Host "Google Client Secret"

if ([string]::IsNullOrWhiteSpace($client_id) -or [string]::IsNullOrWhiteSpace($client_secret)) {
    Write-Host "❌ Error: Debes proporcionar ambas credenciales." -ForegroundColor Red
    exit 1
}

# Configurar variables de entorno para la sesión actual
$env:GOOGLE_CLIENT_ID = $client_id
$env:GOOGLE_CLIENT_SECRET = $client_secret

Write-Host ""
Write-Host "✅ Variables de entorno configuradas exitosamente para esta sesión!" -ForegroundColor Green
Write-Host ""
Write-Host "Para hacerlas permanentes (usuario actual), ejecuta:" -ForegroundColor Yellow
Write-Host ""
Write-Host "[System.Environment]::SetEnvironmentVariable('GOOGLE_CLIENT_ID', '$client_id', 'User')" -ForegroundColor Cyan
Write-Host "[System.Environment]::SetEnvironmentVariable('GOOGLE_CLIENT_SECRET', '$client_secret', 'User')" -ForegroundColor Cyan
Write-Host ""
Write-Host "Ahora puedes ejecutar la aplicación con:" -ForegroundColor Yellow
Write-Host ".\mvnw.cmd spring-boot:run" -ForegroundColor Cyan
Write-Host ""

# Preguntar si desea hacerlas permanentes
$permanent = Read-Host "¿Deseas hacer estas variables permanentes? (s/n)"
if ($permanent -eq "s" -or $permanent -eq "S") {
    [System.Environment]::SetEnvironmentVariable('GOOGLE_CLIENT_ID', $client_id, 'User')
    [System.Environment]::SetEnvironmentVariable('GOOGLE_CLIENT_SECRET', $client_secret, 'User')
    Write-Host "✅ Variables configuradas permanentemente!" -ForegroundColor Green
    Write-Host "Nota: Puede que necesites reiniciar tu terminal para que surtan efecto." -ForegroundColor Yellow
}
