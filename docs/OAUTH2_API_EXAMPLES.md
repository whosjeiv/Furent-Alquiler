# Ejemplos de API con OAuth2

## Endpoints OAuth2

### 1. Iniciar Sesión con Google (Web)

**URL:** `GET /oauth2/authorization/google`

**Descripción:** Inicia el flujo de autenticación OAuth2 con Google. Redirige al usuario a la página de login de Google.

**Ejemplo HTML:**
```html
<a href="/oauth2/authorization/google" class="btn-google">
    Iniciar sesión con Google
</a>
```

**Flujo:**
1. Usuario hace clic en el enlace
2. Redirige a Google para autenticación
3. Usuario autoriza la aplicación
4. Google redirige a `/login/oauth2/code/google`
5. Usuario autenticado y redirigido a `/` o `/admin`

---

### 2. Callback de Google (Automático)

**URL:** `GET /login/oauth2/code/google?code={authorization_code}`

**Descripción:** Endpoint de callback manejado automáticamente por Spring Security. No debe ser llamado manualmente.

**Parámetros:**
- `code`: Código de autorización de Google (automático)
- `state`: Token CSRF (automático)

---

## Integración con API REST

### Obtener Usuario Actual (JWT)

**URL:** `GET /api/auth/me`

**Headers:**
```
Authorization: Bearer {jwt_token}
```

**Respuesta exitosa (200):**
```json
{
  "id": "507f1f77bcf86cd799439011",
  "email": "usuario@gmail.com",
  "nombre": "Juan",
  "apellido": "Pérez",
  "telefono": "",
  "role": "USER",
  "activo": true,
  "provider": "google",
  "profileImageUrl": "https://lh3.googleusercontent.com/a/...",
  "fechaCreacion": "2026-03-24T10:30:00"
}
```

**Ejemplo cURL:**
```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## Diferencias entre Usuarios OAuth2 y Locales

### Usuario Local (Email/Password)

```json
{
  "id": "507f1f77bcf86cd799439011",
  "email": "usuario@example.com",
  "nombre": "María",
  "apellido": "García",
  "provider": null,
  "providerId": null,
  "profileImageUrl": null,
  "password": "$2a$10$..." // Hash BCrypt
}
```

### Usuario OAuth2 (Google)

```json
{
  "id": "507f1f77bcf86cd799439012",
  "email": "usuario@gmail.com",
  "nombre": "Juan",
  "apellido": "Pérez",
  "provider": "google",
  "providerId": "123456789012345678901",
  "profileImageUrl": "https://lh3.googleusercontent.com/a/...",
  "password": "" // Vacío - no usa password local
}
```

---

## Casos de Uso

### Caso 1: Usuario se registra con Google

**Flujo:**
1. Usuario hace clic en "Iniciar con Google"
2. Autoriza la aplicación en Google
3. Sistema crea usuario automáticamente:
   ```json
   {
     "email": "nuevo@gmail.com",
     "nombre": "Nuevo",
     "apellido": "Usuario",
     "provider": "google",
     "providerId": "123...",
     "role": "USER",
     "activo": true,
     "password": ""
   }
   ```
4. Usuario redirigido a `/`

### Caso 2: Usuario existente inicia sesión con Google

**Escenario:** Usuario se registró con email/password, luego usa Google

**Flujo:**
1. Usuario hace clic en "Iniciar con Google"
2. Sistema encuentra usuario por email
3. Actualiza campos OAuth2:
   ```json
   {
     "email": "existente@gmail.com",
     "provider": "google",
     "providerId": "123...",
     "profileImageUrl": "https://..."
   }
   ```
4. Usuario puede usar ambos métodos de login

### Caso 3: Admin inicia sesión con Google

**Flujo:**
1. Admin hace clic en "Iniciar con Google"
2. Sistema verifica rol: `ADMIN`
3. Redirige a `/admin` en lugar de `/`

---

## Integración Frontend

### React/Vue/Angular

```javascript
// Botón de login con Google
function GoogleLoginButton() {
  const handleGoogleLogin = () => {
    window.location.href = '/oauth2/authorization/google';
  };

  return (
    <button onClick={handleGoogleLogin}>
      <img src="/google-icon.svg" alt="Google" />
      Iniciar con Google
    </button>
  );
}
```

### Vanilla JavaScript

```javascript
document.getElementById('google-login-btn').addEventListener('click', () => {
  window.location.href = '/oauth2/authorization/google';
});
```

---

## Manejo de Sesiones

### Verificar si el usuario está autenticado

**Thymeleaf:**
```html
<div sec:authorize="isAuthenticated()">
  <p>Bienvenido, <span sec:authentication="name"></span></p>
  <img th:src="${user.profileImageUrl}" alt="Perfil" />
</div>
```

**JavaScript (verificar sesión):**
```javascript
fetch('/api/auth/me', {
  headers: {
    'Authorization': 'Bearer ' + localStorage.getItem('jwt_token')
  }
})
.then(response => {
  if (response.ok) {
    return response.json();
  }
  throw new Error('No autenticado');
})
.then(user => {
  console.log('Usuario:', user);
  if (user.provider === 'google') {
    console.log('Autenticado con Google');
  }
})
.catch(error => {
  console.error('Error:', error);
  // Redirigir a login
  window.location.href = '/login';
});
```

---

## Logout

### Cerrar sesión (Web)

**URL:** `POST /logout`

**Ejemplo HTML:**
```html
<form th:action="@{/logout}" method="post">
  <button type="submit">Cerrar sesión</button>
</form>
```

### Cerrar sesión (API REST)

**URL:** `POST /api/auth/logout`

**Headers:**
```
Authorization: Bearer {jwt_token}
```

**Ejemplo cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Respuesta (200):**
```json
{
  "message": "Sesión cerrada exitosamente"
}
```

---

## Seguridad

### CSRF Protection

Para formularios web, incluye el token CSRF:

```html
<form th:action="@{/logout}" method="post">
  <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />
  <button type="submit">Cerrar sesión</button>
</form>
```

### Cookies HTTP-Only

Las cookies de sesión son HTTP-only y no accesibles desde JavaScript:

```
Set-Cookie: JSESSIONID=...; Path=/; HttpOnly; SameSite=Lax
```

---

## Monitoreo

### Logs de autenticación OAuth2

```
2026-03-24 10:30:15 INFO  OAuth2 login attempt - Provider: google, Email: user@gmail.com
2026-03-24 10:30:16 INFO  Nuevo usuario creado desde OAuth2: user@gmail.com
2026-03-24 10:30:16 INFO  OAuth2 login success for: user@gmail.com
```

### Verificar usuarios OAuth2 en MongoDB

```javascript
// MongoDB Shell
db.usuarios.find({ provider: "google" })

// Contar usuarios por proveedor
db.usuarios.aggregate([
  { $group: { _id: "$provider", count: { $sum: 1 } } }
])
```

---

## Testing

### Postman Collection

```json
{
  "info": {
    "name": "Furent OAuth2 API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Get Current User",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{jwt_token}}"
          }
        ],
        "url": {
          "raw": "http://localhost:8080/api/auth/me",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "auth", "me"]
        }
      }
    }
  ]
}
```

---

## Troubleshooting

### Error: Usuario no se crea

**Verificar logs:**
```bash
tail -f logs/furent-dev.log | grep OAuth2
```

**Verificar MongoDB:**
```javascript
db.usuarios.find({ email: "usuario@gmail.com" })
```

### Error: Redirect URI mismatch

**Verificar configuración:**
```bash
echo $GOOGLE_CLIENT_ID
echo $GOOGLE_CLIENT_SECRET
```

**Verificar Google Console:**
- URIs autorizados deben incluir: `http://localhost:8080/login/oauth2/code/google`

---

## Referencias

- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Google OAuth2 Playground](https://developers.google.com/oauthplayground/)
- [JWT.io Debugger](https://jwt.io/)
