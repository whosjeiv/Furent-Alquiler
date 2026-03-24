#!/bin/bash

# Script de configuración para Google OAuth2
# Este script te ayuda a configurar las variables de entorno necesarias

echo "=========================================="
echo "  Configuración de Google OAuth2 - Furent"
echo "=========================================="
echo ""

# Verificar si ya existen las variables
if [ ! -z "$GOOGLE_CLIENT_ID" ] && [ ! -z "$GOOGLE_CLIENT_SECRET" ]; then
    echo "✅ Las variables de entorno ya están configuradas:"
    echo "   GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID:0:20}..."
    echo "   GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET:0:10}..."
    echo ""
    read -p "¿Deseas reconfigurarlas? (s/n): " reconfig
    if [ "$reconfig" != "s" ] && [ "$reconfig" != "S" ]; then
        echo "Configuración cancelada."
        exit 0
    fi
fi

echo ""
echo "📋 Pasos previos:"
echo "1. Ve a https://console.cloud.google.com/"
echo "2. Crea un proyecto o selecciona uno existente"
echo "3. Habilita la API de Google+"
echo "4. Crea credenciales OAuth 2.0 (Aplicación web)"
echo "5. Agrega el URI de redireccionamiento:"
echo "   http://localhost:8080/login/oauth2/code/google"
echo ""

read -p "¿Ya completaste estos pasos? (s/n): " ready
if [ "$ready" != "s" ] && [ "$ready" != "S" ]; then
    echo "Por favor, completa los pasos previos y vuelve a ejecutar este script."
    exit 0
fi

echo ""
echo "Ingresa tus credenciales de Google OAuth2:"
echo ""

read -p "Google Client ID: " client_id
read -p "Google Client Secret: " client_secret

if [ -z "$client_id" ] || [ -z "$client_secret" ]; then
    echo "❌ Error: Debes proporcionar ambas credenciales."
    exit 1
fi

# Exportar variables de entorno
export GOOGLE_CLIENT_ID="$client_id"
export GOOGLE_CLIENT_SECRET="$client_secret"

echo ""
echo "✅ Variables de entorno configuradas exitosamente!"
echo ""
echo "Para que persistan en tu sesión actual, ejecuta:"
echo ""
echo "export GOOGLE_CLIENT_ID=\"$client_id\""
echo "export GOOGLE_CLIENT_SECRET=\"$client_secret\""
echo ""
echo "Para hacerlas permanentes, agrégalas a tu ~/.bashrc o ~/.zshrc:"
echo ""
echo "echo 'export GOOGLE_CLIENT_ID=\"$client_id\"' >> ~/.bashrc"
echo "echo 'export GOOGLE_CLIENT_SECRET=\"$client_secret\"' >> ~/.bashrc"
echo ""
echo "Ahora puedes ejecutar la aplicación con:"
echo "./mvnw spring-boot:run"
echo ""
