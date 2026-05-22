# Inicio de Sesión con Google - Guía Rápida

## ✅ Implementación Completada

Se ha implementado exitosamente el inicio de sesión con Google OAuth2 en Furent.

## 🚀 Configuración Rápida

### 1. Obtener Credenciales de Google

1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Crea un proyecto nuevo
3. Habilita la API de Google+
4. Crea credenciales OAuth 2.0:
   - Tipo: Aplicación web
   - URI de redireccionamiento: `http://localhost:8080/login/oauth2/code/google`

### 2. Configurar Variables de Entorno

**Windows (PowerShell):**
```powershell
$env:GOOGLE_CLIENT_ID="tu-client-id.apps.googleusercontent.com"
$env:GOOGLE_CLIENT_SECRET="tu-client-secret"
```

**Linux/Mac:**
```bash
export GOOGLE_CLIENT_ID="tu-client-id.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="tu-client-secret"
```

### 3. Ejecutar la Aplicación

```bash
./mvnw spring-boot:run
```

### 4. Probar

1. Abre `http://localhost:8080/login`
2. Haz clic en el botón "Google"
3. Inicia sesión con tu cuenta de Google

## 📦 Archivos Creados/Modificados

### Nuevos Archivos:
- `src/main/java/com/alquiler/furent/service/OAuth2UserService.java`
- `src/main/java/com/alquiler/furent/config/OAuth2LoginSuccessHandler.java`
- `docs/GOOGLE_OAUTH2_SETUP.md`

### Archivos Modificados:
- `pom.xml` - Agregada dependencia `spring-boot-starter-oauth2-client`
- `src/main/java/com/alquiler/furent/model/User.java` - Campos OAuth2
- `src/main/java/com/alquiler/furent/config/SecurityConfig.java` - Configuración OAuth2
- `src/main/java/com/alquiler/furent/service/UserService.java` - Soporte OAuth2
- `src/main/resources/templates/login.html` - Botón de Google
- `src/main/resources/application.properties` - Configuración OAuth2

## 🔧 Características

✅ Inicio de sesión con Google
✅ Creación automática de usuarios
✅ Sincronización de perfil (nombre, apellido, foto)
✅ Soporte multi-proveedor (preparado para Facebook, GitHub, etc.)
✅ Integración con sistema de roles existente
✅ Manejo de sesiones y redirección según rol

## 📖 Documentación Completa

Para más detalles, consulta: `docs/GOOGLE_OAUTH2_SETUP.md`

## 🔐 Seguridad

- Los usuarios OAuth2 no tienen contraseña local
- La autenticación se delega completamente a Google
- Los tokens no se almacenan en la base de datos
- Soporte para cuentas suspendidas

## 🎯 Próximos Pasos

Para agregar más proveedores (Facebook, GitHub):
1. Agrega la configuración en `application.properties`
2. Actualiza `OAuth2UserService` para el nuevo proveedor
3. Agrega el botón en `login.html`
