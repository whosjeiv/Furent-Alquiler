# ✅ Configuración de Google OAuth2 - COMPLETADA

## Credenciales Configuradas

```
Client ID: 602363263815-jhqmdp2hqo1dbs9lklorquoulb8ki5ja.apps.googleusercontent.com
Client Secret: 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
```

✅ Las credenciales ya están configuradas en `src/main/resources/application.properties`

## ⚠️ IMPORTANTE: Verificar URIs de Redireccionamiento en Google Console

Para que el inicio de sesión funcione, debes verificar que los siguientes URIs estén autorizados en Google Cloud Console:

### 1. Ve a Google Cloud Console
https://console.cloud.google.com/apis/credentials

### 2. Selecciona tu Client ID OAuth 2.0
Busca: `602363263815-jhqmdp2hqo1dbs9lklorquoulb8ki5ja.apps.googleusercontent.com`

### 3. Verifica que estos URIs estén en "URIs de redireccionamiento autorizados"

**Para desarrollo local:**
```
http://localhost:8080/login/oauth2/code/google
```

**Para producción (cuando despliegues):**
```
https://tu-dominio.com/login/oauth2/code/google
```

### 4. Verifica "Orígenes de JavaScript autorizados"

**Para desarrollo local:**
```
http://localhost:8080
```

**Para producción:**
```
https://tu-dominio.com
```

## 🚀 Ejecutar la Aplicación

Ahora puedes ejecutar la aplicación directamente:

```bash
./mvnw spring-boot:run
```

O en Windows:
```bash
.\mvnw.cmd spring-boot:run
```

## 🧪 Probar el Login con Google

1. Abre tu navegador en: http://localhost:8080/login
2. Haz clic en el botón "Google"
3. Inicia sesión con tu cuenta de Google
4. Deberías ser redirigido automáticamente a la página principal

## 🔍 Verificar que Funciona

### Logs esperados:
```
INFO  OAuth2 login attempt - Provider: google, Email: tu-email@gmail.com
INFO  Nuevo usuario creado desde OAuth2: tu-email@gmail.com
INFO  OAuth2 login success for: tu-email@gmail.com
```

### Verificar en MongoDB:
```javascript
// Conecta a MongoDB
mongosh

// Usa la base de datos
use FurentDataBase

// Busca tu usuario
db.usuarios.find({ provider: "google" }).pretty()
```

Deberías ver algo como:
```json
{
  "_id": ObjectId("..."),
  "email": "tu-email@gmail.com",
  "nombre": "Tu Nombre",
  "apellido": "Tu Apellido",
  "provider": "google",
  "providerId": "123456789...",
  "profileImageUrl": "https://lh3.googleusercontent.com/...",
  "role": "USER",
  "activo": true,
  "fechaCreacion": ISODate("2026-03-24T...")
}
```

## ⚠️ Solución de Problemas

### Error: "redirect_uri_mismatch"

**Causa:** El URI de redireccionamiento no está autorizado en Google Console.

**Solución:**
1. Ve a https://console.cloud.google.com/apis/credentials
2. Edita tu Client ID OAuth 2.0
3. Agrega: `http://localhost:8080/login/oauth2/code/google`
4. Guarda los cambios
5. Espera 1-2 minutos para que se propague
6. Intenta de nuevo

### Error: "invalid_client"

**Causa:** Las credenciales son incorrectas.

**Solución:** Verifica que las credenciales en `application.properties` coincidan exactamente con las de Google Console.

### Error: "Access blocked: This app's request is invalid"

**Causa:** La pantalla de consentimiento OAuth no está configurada.

**Solución:**
1. Ve a https://console.cloud.google.com/apis/credentials/consent
2. Completa la información requerida
3. Agrega los scopes: `userinfo.email` y `userinfo.profile`
4. Guarda los cambios

### El usuario no se crea en MongoDB

**Verificar:**
1. MongoDB está corriendo: `mongosh` o verifica Docker
2. Revisa los logs de la aplicación
3. Verifica la conexión en `application.properties`

## 🎯 Próximos Pasos

### 1. Probar con diferentes cuentas de Google
- Cuenta personal (@gmail.com)
- Cuenta de Google Workspace (si tienes)

### 2. Verificar roles
- Los usuarios nuevos se crean con rol "USER"
- Para hacer admin a un usuario:
  ```javascript
  db.usuarios.updateOne(
    { email: "tu-email@gmail.com" },
    { $set: { role: "ADMIN" } }
  )
  ```

### 3. Personalizar la experiencia
- Mostrar foto de perfil en el navbar
- Agregar opción "Vincular cuenta de Google" para usuarios existentes
- Implementar más proveedores (Facebook, GitHub)

## 📊 Monitoreo

### Ver todos los usuarios OAuth2:
```javascript
db.usuarios.find({ provider: { $exists: true } }).pretty()
```

### Contar usuarios por proveedor:
```javascript
db.usuarios.aggregate([
  { $group: { _id: "$provider", count: { $sum: 1 } } }
])
```

## 🔐 Seguridad en Producción

Cuando despliegues a producción:

1. **Usa variables de entorno:**
   ```properties
   spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
   spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
   ```

2. **Actualiza los URIs en Google Console** con tu dominio real

3. **Habilita HTTPS** (obligatorio para OAuth2 en producción)

4. **Configura la pantalla de consentimiento** para usuarios externos

## ✅ Checklist Final

- [x] Credenciales configuradas en application.properties
- [ ] URIs de redireccionamiento verificados en Google Console
- [ ] Aplicación ejecutándose
- [ ] Login con Google probado
- [ ] Usuario creado en MongoDB
- [ ] Logs verificados

## 📚 Documentación Adicional

- [GOOGLE_OAUTH_QUICKSTART.md](GOOGLE_OAUTH_QUICKSTART.md) - Guía rápida
- [docs/GOOGLE_OAUTH2_SETUP.md](docs/GOOGLE_OAUTH2_SETUP.md) - Guía completa
- [docs/OAUTH2_ARCHITECTURE.md](docs/OAUTH2_ARCHITECTURE.md) - Arquitectura
- [docs/OAUTH2_API_EXAMPLES.md](docs/OAUTH2_API_EXAMPLES.md) - Ejemplos de API

---

**¡Todo listo! 🎉** Ahora puedes ejecutar `./mvnw spring-boot:run` y probar el login con Google.
