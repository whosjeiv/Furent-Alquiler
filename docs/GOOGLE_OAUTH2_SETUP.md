# Configuración de Inicio de Sesión con Google OAuth2

Este documento explica cómo configurar el inicio de sesión con Google en Furent.

## Pasos para Configurar Google OAuth2

### 1. Crear un Proyecto en Google Cloud Console

1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Crea un nuevo proyecto o selecciona uno existente
3. Habilita la API de Google+ (Google+ API)

### 2. Configurar la Pantalla de Consentimiento OAuth

1. En el menú lateral, ve a **APIs y servicios** > **Pantalla de consentimiento de OAuth**
2. Selecciona el tipo de usuario (Externo para usuarios públicos)
3. Completa la información requerida:
   - Nombre de la aplicación: **Furent**
   - Correo electrónico de asistencia
   - Logo de la aplicación (opcional)
   - Dominios autorizados
4. Agrega los scopes necesarios:
   - `userinfo.email`
   - `userinfo.profile`
5. Guarda los cambios

### 3. Crear Credenciales OAuth 2.0

1. Ve a **APIs y servicios** > **Credenciales**
2. Haz clic en **Crear credenciales** > **ID de cliente de OAuth 2.0**
3. Selecciona **Aplicación web** como tipo de aplicación
4. Configura los URIs autorizados:

   **Orígenes de JavaScript autorizados:**
   ```
   http://localhost:8080
   https://tu-dominio.com
   ```

   **URIs de redireccionamiento autorizados:**
   ```
   http://localhost:8080/login/oauth2/code/google
   https://tu-dominio.com/login/oauth2/code/google
   ```

5. Haz clic en **Crear**
6. Copia el **ID de cliente** y el **Secreto del cliente**

### 4. Configurar las Variables de Entorno

Agrega las siguientes variables de entorno a tu sistema o archivo `.env`:

```bash
GOOGLE_CLIENT_ID=tu-client-id-aqui.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=tu-client-secret-aqui
```

**En Windows (PowerShell):**
```powershell
$env:GOOGLE_CLIENT_ID="tu-client-id-aqui.apps.googleusercontent.com"
$env:GOOGLE_CLIENT_SECRET="tu-client-secret-aqui"
```

**En Linux/Mac:**
```bash
export GOOGLE_CLIENT_ID="tu-client-id-aqui.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="tu-client-secret-aqui"
```

### 5. Configuración en Producción

Para producción, asegúrate de:

1. Actualizar los URIs de redireccionamiento con tu dominio real
2. Configurar las variables de entorno en tu servidor/plataforma de hosting
3. Verificar que el dominio esté en la lista de dominios autorizados

**Ejemplo para Heroku:**
```bash
heroku config:set GOOGLE_CLIENT_ID=tu-client-id
heroku config:set GOOGLE_CLIENT_SECRET=tu-client-secret
```

**Ejemplo para Docker:**
```yaml
environment:
  - GOOGLE_CLIENT_ID=tu-client-id
  - GOOGLE_CLIENT_SECRET=tu-client-secret
```

## Arquitectura de la Implementación

### Componentes Creados

1. **OAuth2UserService** (`src/main/java/com/alquiler/furent/service/OAuth2UserService.java`)
   - Maneja la carga de usuarios desde Google
   - Crea o actualiza usuarios automáticamente
   - Sincroniza información del perfil

2. **OAuth2LoginSuccessHandler** (`src/main/java/com/alquiler/furent/config/OAuth2LoginSuccessHandler.java`)
   - Maneja el éxito del login OAuth2
   - Redirige según el rol del usuario (admin/user)
   - Actualiza la última sesión

3. **Modelo User actualizado**
   - Nuevos campos: `provider`, `providerId`, `profileImageUrl`
   - Soporta múltiples proveedores de autenticación

4. **SecurityConfig actualizado**
   - Configuración de OAuth2 integrada
   - Endpoints de callback configurados

## Flujo de Autenticación

1. Usuario hace clic en "Iniciar sesión con Google"
2. Redirige a `/oauth2/authorization/google`
3. Google muestra la pantalla de consentimiento
4. Usuario autoriza la aplicación
5. Google redirige a `/login/oauth2/code/google` con un código
6. Spring Security intercambia el código por tokens
7. `OAuth2UserService` carga/crea el usuario
8. `OAuth2LoginSuccessHandler` redirige al usuario

## Campos del Usuario OAuth2

Cuando un usuario inicia sesión con Google, se almacenan los siguientes datos:

- `email`: Email del usuario de Google
- `nombre`: Nombre (given_name)
- `apellido`: Apellido (family_name)
- `provider`: "google"
- `providerId`: ID único del usuario en Google (sub)
- `profileImageUrl`: URL de la foto de perfil
- `password`: Vacío (no se requiere para OAuth2)

## Seguridad

- Los usuarios OAuth2 no tienen contraseña local
- No pueden usar el login tradicional con email/password
- La autenticación se delega completamente a Google
- Los tokens de acceso no se almacenan en la base de datos

## Pruebas

Para probar el inicio de sesión con Google:

1. Inicia la aplicación: `./mvnw spring-boot:run`
2. Ve a `http://localhost:8080/login`
3. Haz clic en el botón "Google"
4. Inicia sesión con tu cuenta de Google
5. Verifica que seas redirigido correctamente

## Solución de Problemas

### Error: redirect_uri_mismatch

**Causa:** El URI de redireccionamiento no está autorizado en Google Cloud Console.

**Solución:** Verifica que `http://localhost:8080/login/oauth2/code/google` esté en la lista de URIs autorizados.

### Error: invalid_client

**Causa:** El Client ID o Client Secret son incorrectos.

**Solución:** Verifica que las variables de entorno estén configuradas correctamente.

### Usuario no se crea automáticamente

**Causa:** Error en `OAuth2UserService`.

**Solución:** Revisa los logs de la aplicación para ver el error específico.

## Extensión Futura

Para agregar más proveedores OAuth2 (Facebook, GitHub, etc.):

1. Agrega la configuración en `application.properties`
2. Actualiza `OAuth2UserService` para manejar diferentes proveedores
3. Agrega el botón correspondiente en `login.html`

## Referencias

- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Google OAuth2 Documentation](https://developers.google.com/identity/protocols/oauth2)
- [Google Cloud Console](https://console.cloud.google.com/)
