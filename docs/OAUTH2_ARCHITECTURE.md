# Arquitectura de OAuth2 en Furent

## Diagrama de Flujo

```
┌─────────────┐
│   Usuario   │
└──────┬──────┘
       │ 1. Click "Iniciar con Google"
       ▼
┌─────────────────────────────────────┐
│  /oauth2/authorization/google       │
│  (Spring Security OAuth2)           │
└──────┬──────────────────────────────┘
       │ 2. Redirect a Google
       ▼
┌─────────────────────────────────────┐
│  Google OAuth2 Authorization        │
│  (accounts.google.com)              │
└──────┬──────────────────────────────┘
       │ 3. Usuario autoriza
       ▼
┌─────────────────────────────────────┐
│  /login/oauth2/code/google          │
│  (Callback URL)                     │
└──────┬──────────────────────────────┘
       │ 4. Spring intercambia código por token
       ▼
┌─────────────────────────────────────┐
│  OAuth2UserService                  │
│  - Obtiene info del usuario         │
│  - Busca/crea usuario en MongoDB    │
│  - Sincroniza perfil                │
└──────┬──────────────────────────────┘
       │ 5. Usuario autenticado
       ▼
┌─────────────────────────────────────┐
│  OAuth2LoginSuccessHandler          │
│  - Actualiza última sesión          │
│  - Redirige según rol               │
└──────┬──────────────────────────────┘
       │ 6. Redirect
       ▼
┌─────────────────────────────────────┐
│  / (Usuario) o /admin (Admin)       │
└─────────────────────────────────────┘
```

## Componentes

### 1. SecurityConfig
**Ubicación:** `src/main/java/com/alquiler/furent/config/SecurityConfig.java`

**Responsabilidad:**
- Configura OAuth2 Login
- Define endpoints públicos y protegidos
- Integra OAuth2UserService y OAuth2LoginSuccessHandler

**Configuración clave:**
```java
.oauth2Login(oauth2 -> oauth2
    .loginPage("/login")
    .userInfoEndpoint(userInfo -> userInfo
        .userService(oauth2UserService))
    .successHandler(oauth2LoginSuccessHandler)
    .permitAll())
```

### 2. OAuth2UserService
**Ubicación:** `src/main/java/com/alquiler/furent/service/OAuth2UserService.java`

**Responsabilidad:**
- Extiende `DefaultOAuth2UserService`
- Carga información del usuario desde Google
- Crea o actualiza usuario en MongoDB
- Sincroniza datos del perfil

**Datos obtenidos de Google:**
- `sub` → providerId (ID único de Google)
- `email` → email
- `given_name` → nombre
- `family_name` → apellido
- `picture` → profileImageUrl

### 3. OAuth2LoginSuccessHandler
**Ubicación:** `src/main/java/com/alquiler/furent/config/OAuth2LoginSuccessHandler.java`

**Responsabilidad:**
- Maneja el éxito del login OAuth2
- Actualiza última sesión del usuario
- Redirige según el rol (ADMIN → /admin, USER → /)

### 4. User Model
**Ubicación:** `src/main/java/com/alquiler/furent/model/User.java`

**Campos OAuth2 agregados:**
```java
private String provider;          // "google", "facebook", etc.
private String providerId;        // ID del usuario en el proveedor
private String profileImageUrl;   // URL de la foto de perfil
```

### 5. UserService
**Ubicación:** `src/main/java/com/alquiler/furent/service/UserService.java`

**Modificación:**
- Soporte para usuarios sin contraseña (OAuth2)
- Placeholder `{noop}` para usuarios OAuth2

## Flujo de Datos

### Primer Login (Usuario Nuevo)

```
Google → OAuth2UserService → MongoDB
                ↓
        Crear nuevo User:
        - email: user@gmail.com
        - nombre: John
        - apellido: Doe
        - provider: "google"
        - providerId: "123456789"
        - profileImageUrl: "https://..."
        - password: "" (vacío)
        - role: "USER"
        - activo: true
```

### Login Subsecuente (Usuario Existente)

```
Google → OAuth2UserService → MongoDB
                ↓
        Actualizar User:
        - provider: "google"
        - providerId: "123456789"
        - profileImageUrl: "https://..." (actualizado)
        - ultimaSesion: now()
```

## Seguridad

### Tokens
- Los tokens de acceso de Google NO se almacenan
- Spring Security maneja el intercambio de tokens automáticamente
- La sesión se mantiene con cookies HTTP-only

### Contraseñas
- Usuarios OAuth2 no tienen contraseña local
- No pueden usar login tradicional (email/password)
- Solo pueden autenticarse vía Google

### Validación
- Email verificado por Google
- No se requiere verificación adicional
- Cuenta activa por defecto

## Configuración

### application.properties
```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=profile,email
spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
```

### Variables de Entorno
```bash
GOOGLE_CLIENT_ID=xxx.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=xxx
```

## Endpoints

| Endpoint | Método | Descripción |
|----------|--------|-------------|
| `/oauth2/authorization/google` | GET | Inicia el flujo OAuth2 |
| `/login/oauth2/code/google` | GET | Callback de Google |
| `/login` | GET | Página de login con botón de Google |

## Extensibilidad

Para agregar más proveedores (Facebook, GitHub, etc.):

1. **Agregar configuración en application.properties:**
```properties
spring.security.oauth2.client.registration.facebook.client-id=${FACEBOOK_CLIENT_ID}
spring.security.oauth2.client.registration.facebook.client-secret=${FACEBOOK_CLIENT_SECRET}
```

2. **Actualizar OAuth2UserService:**
```java
String provider = userRequest.getClientRegistration().getRegistrationId();
if ("facebook".equals(provider)) {
    // Lógica específica de Facebook
}
```

3. **Agregar botón en login.html:**
```html
<a th:href="@{/oauth2/authorization/facebook}">
    Iniciar con Facebook
</a>
```

## Monitoreo y Logs

### Logs importantes:
```
INFO  OAuth2 login attempt - Provider: google, Email: user@gmail.com
INFO  Nuevo usuario creado desde OAuth2: user@gmail.com
INFO  Usuario existente actualizado con OAuth2: user@gmail.com
INFO  OAuth2 login success for: user@gmail.com
```

### Métricas:
- Usuarios registrados vía OAuth2
- Logins exitosos por proveedor
- Errores de autenticación

## Troubleshooting

### Error: redirect_uri_mismatch
**Causa:** URI de callback no autorizado en Google Console

**Solución:** Agregar `http://localhost:8080/login/oauth2/code/google` a URIs autorizados

### Error: invalid_client
**Causa:** Client ID o Secret incorrectos

**Solución:** Verificar variables de entorno

### Usuario no se crea
**Causa:** Error en OAuth2UserService

**Solución:** Revisar logs para ver el error específico

## Referencias

- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Google OAuth2 Documentation](https://developers.google.com/identity/protocols/oauth2)
- [OAuth 2.0 RFC](https://datatracker.ietf.org/doc/html/rfc6749)
