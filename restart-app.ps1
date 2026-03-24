# Script para reiniciar la aplicación Furent
# Windows PowerShell

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Reiniciando Furent" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Buscar y detener procesos de Maven/Java relacionados con Furent
Write-Host "🛑 Deteniendo procesos existentes..." -ForegroundColor Yellow

$javaProcesses = Get-Process java -ErrorAction SilentlyContinue | Where-Object {
    $_.CommandLine -like "*furent*" -or $_.CommandLine -like "*spring-boot*"
}

if ($javaProcesses) {
    $javaProcesses | ForEach-Object {
        Write-Host "   Deteniendo proceso: $($_.Id)" -ForegroundColor Gray
        Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
    }
    Start-Sleep -Seconds 2
    Write-Host "✅ Procesos detenidos" -ForegroundColor Green
} else {
    Write-Host "   No hay procesos corriendo" -ForegroundColor Gray
}

Write-Host ""
Write-Host "🧹 Limpiando compilación anterior..." -ForegroundColor Yellow
.\mvnw.cmd clean -q

Write-Host ""
Write-Host "🔨 Compilando..." -ForegroundColor Yellow
.\mvnw.cmd compile -DskipTests -q

Write-Host ""
Write-Host "✅ Rate Limiting: DESHABILITADO (desarrollo)" -ForegroundColor Green
Write-Host "✅ OAuth2 Google: CONFIGURADO" -ForegroundColor Green
Write-Host ""

Write-Host "🚀 Iniciando aplicación..." -ForegroundColor Green
Write-Host ""
Write-Host "Una vez iniciada, abre:" -ForegroundColor Yellow
Write-Host "   http://localhost:8080/login" -ForegroundColor Cyan
Write-Host ""
Write-Host "Presiona Ctrl+C para detener" -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Iniciar la aplicación
.\mvnw.cmd spring-boot:run
