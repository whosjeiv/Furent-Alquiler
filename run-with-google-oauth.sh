#!/bin/bash

# Script para ejecutar Furent con Google OAuth2 configurado
# Linux/Mac

echo "========================================"
echo "  Furent - Inicio con Google OAuth2"
echo "========================================"
echo ""

# Verificar que MongoDB esté corriendo
echo "🔍 Verificando MongoDB..."
if pgrep -x "mongod" > /dev/null; then
    echo "✅ MongoDB está corriendo"
elif docker ps | grep -q mongodb; then
    echo "✅ MongoDB está corriendo en Docker"
else
    echo "⚠️  MongoDB no está corriendo."
    echo "   Opciones:"
    echo "   1. Iniciar MongoDB manualmente"
    echo "   2. Usar Docker: docker-compose up -d mongodb"
    echo ""
    read -p "¿Deseas continuar de todas formas? (s/n): " continue
    if [ "$continue" != "s" ] && [ "$continue" != "S" ]; then
        exit 0
    fi
fi

echo ""
echo "📋 Configuración OAuth2:"
echo "   Client ID: 602363263815-jhqmdp2hqo1dbs9lklorquoulb8ki5ja.apps.googleusercontent.com"
echo "   Redirect URI: http://localhost:8080/login/oauth2/code/google"
echo ""

echo "⚠️  IMPORTANTE: Verifica que el URI de redireccionamiento esté autorizado en:"
echo "   https://console.cloud.google.com/apis/credentials"
echo ""

read -p "¿Está todo configurado en Google Console? (s/n): " ready
if [ "$ready" != "s" ] && [ "$ready" != "S" ]; then
    echo ""
    echo "Por favor, configura los URIs en Google Console y vuelve a ejecutar este script."
    echo "Consulta: CONFIGURACION_GOOGLE_COMPLETADA.md"
    exit 0
fi

echo ""
echo "🚀 Iniciando aplicación..."
echo ""
echo "Una vez iniciada, abre tu navegador en:"
echo "   http://localhost:8080/login"
echo ""
echo "Presiona Ctrl+C para detener la aplicación"
echo ""
echo "========================================"
echo ""

# Ejecutar la aplicación
./mvnw spring-boot:run
