# 🚀 Inicio Rápido - Google OAuth2

## ⚠️ IMPORTANTE: Error 429 Solucionado

Si viste un error "429 - Too Many Requests", ya está solucionado. El rate limiting ha sido deshabilitado en desarrollo.

**Reinicia la aplicación:**
```powershell
.\restart-app.ps1
```

O manualmente:
```bash
# Detén la app (Ctrl+C)
./mvnw spring-boot:run
```

---

## ⚡ 3 Pasos para Empezar

### 📋 Paso 1: Verificar Google Console (2 minutos)

1. Abre: https://console.cloud.google.com/apis/credentials

2. Busca tu Client ID: `602363263815-jhqmdp2hqo1dbs9lklorquoulb8ki5ja`

3. Verifica que estos URIs estén autorizados:

   **URIs de redireccionamiento:**
   ```
   http://localhost:8080/login/oauth2/code/google
   ```

   **Orígenes JavaScript:**
   ```
   http://localhost:8080
   ```

4. Si no están, agrégalos y guarda

---

### 🚀 Paso 2: Ejecutar la Aplicación (1 minuto)

**Windows:**
```powershell
.\run-with-google-oauth.ps1
```

**Linux/Mac:**
```bash
chmod +x run-with-google-oauth.sh
./run-with-google-oauth.sh
```

**O manualmente:**
```bash
./mvnw spring-boot:run
```

---

### 🧪 Paso 3: Probar (30 segundos)

1. Abre: http://localhost:8080/login

2. Haz clic en el botón **"Google"**

3. Inicia sesión con tu cuenta de Google

4. ¡Listo! Deberías estar dentro

---

## ✅ Verificar que Funciona

### En los logs verás:
```
INFO  OAuth2 login attempt - Provider: google, Email: tu@gmail.com
INFO  Nuevo usuario creado desde OAuth2: tu@gmail.com
INFO  OAuth2 login success for: tu@gmail.com
```

### En MongoDB:
```bash
mongosh
use FurentDataBase
db.usuarios.find({ provider: "google" })
```

---

## 🐛 ¿Problemas?

### Error: "redirect_uri_mismatch"
➡️ **Solución:** Verifica el Paso 1 - URIs en Google Console

### Error: "invalid_client"
➡️ **Solución:** Las credenciales ya están configuradas en `application.properties`

### MongoDB no conecta
➡️ **Solución:** Inicia MongoDB o usa Docker:
```bash
docker-compose up -d mongodb
```

---

## 📚 Más Información

- **Configuración completa:** `CONFIGURACION_GOOGLE_COMPLETADA.md`
- **Arquitectura:** `docs/OAUTH2_ARCHITECTURE.md`
- **Ejemplos API:** `docs/OAUTH2_API_EXAMPLES.md`
- **Resumen:** `RESUMEN_IMPLEMENTACION.md`

---

## 🎯 Credenciales Configuradas

```
✅ Client ID: 602363263815-jhqmdp2hqo1dbs9lklorquoulb8ki5ja.apps.googleusercontent.com
✅ Client Secret: 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
✅ Redirect URI: http://localhost:8080/login/oauth2/code/google
```

---

## 🎉 ¡Eso es todo!

**Solo verifica el Paso 1 y ejecuta la aplicación.**

El botón de Google ya está funcionando en la página de login.
