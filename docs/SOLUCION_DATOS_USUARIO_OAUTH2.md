# 🔧 Solución: Datos de Usuario OAuth2 no se Muestran

## ❌ Problema

Después de iniciar sesión con Google, la información del usuario (nombre, apellido, email) no aparece en la página de configuración.

## 🔍 Causa

Cuando un usuario inicia sesión con OAuth2 (Google), Spring Security usa un objeto `OAuth2AuthenticationToken` en lugar de un `UserDetails` estándar.

El problema era que el código estaba usando `auth.getName()` que devuelve:
- **Login tradicional:** El email del usuario
- **Login OAuth2:** El ID de Google (ej: `101494796113160910759`)

Por lo tanto, no podía encontrar al usuario en MongoDB por email.

## ✅ Solución Aplicada

He actualizado el código para manejar correctamente ambos tipos de autenticación:

### 1. GlobalModelAdvice.java
Ahora detecta si es OAuth2 y extrae el email correctamente:

```java
if (auth instanceof OAuth2AuthenticationToken) {
    OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) auth;
    email = oauth2Token.getPrincipal().getAttribute("email");
} else {
    email = auth.getName();
}
```

### 2. PageController.java
Actualizado el método `/configuracion` con la misma lógica.

## 🚀 Aplicar la Solución

**IMPORTANTE:** Debes reiniciar la aplicación para que los cambios surtan efecto.

### Opción 1: Script Automático
```powershell
.\restart-app.ps1
```

### Opción 2: Manual
1. Detén la aplicación (Ctrl+C)
2. Reinicia:
   ```bash
   ./mvnw spring-boot:run
   ```
3. Espera a que inicie completamente
4. Accede a: http://localhost:8080/configuracion

## ✨ Resultado Esperado

Después de reiniciar, deberías ver en `/configuracion`:

✅ **Nombre:** Jose Daniel
✅ **Apellido:** Valdes Lastres  
✅ **Email:** valdeslastresjosedaniel@gmail.com
✅ **Foto de perfil:** Tu foto de Google

## 🔍 Verificar en MongoDB

El usuario ya está guardado en MongoDB. Puedes verificarlo:

```bash
mongosh
use FurentDataBase
db.usuarios.find({ email: "valdeslastresjosedaniel@gmail.com" }).pretty()
```

Deberías ver:
```json
{
  "_id": ObjectId("..."),
  "email": "valdeslastresjosedaniel@gmail.com",
  "nombre": "Jose Daniel",
  "apellido": "Valdes Lastres",
  "provider": "google",
  "providerId": "101494796113160910759",
  "profileImageUrl": "https://lh3.googleusercontent.com/...",
  "role": "USER",
  "activo": true
}
```

## 📊 Archivos Modificados

- `src/main/java/com/alquiler/furent/config/GlobalModelAdvice.java` ✅
- `src/main/java/com/alquiler/furent/controller/PageController.java` ✅

## 🎯 Qué se Arregló

1. ✅ Detección correcta de usuarios OAuth2
2. ✅ Extracción del email desde el token OAuth2
3. ✅ Búsqueda correcta del usuario en MongoDB
4. ✅ Datos del usuario mostrados en todas las páginas
5. ✅ Foto de perfil de Google disponible

## 🔧 Mejoras Adicionales

Si quieres mostrar la foto de perfil de Google en lugar del avatar con iniciales, puedes actualizar la plantilla:

**settings.html:**
```html
<div class="w-16 h-16 rounded-2xl overflow-hidden">
    <img th:if="${currentUser.profileImageUrl}" 
         th:src="${currentUser.profileImageUrl}" 
         alt="Perfil" 
         class="w-full h-full object-cover">
    <div th:unless="${currentUser.profileImageUrl}" 
         class="w-full h-full bg-gradient-to-br from-furent-400 to-furent-600 flex items-center justify-center">
        <span class="text-white font-bold text-xl" 
              th:text="${#strings.substring(currentUser.nombre, 0, 1) + #strings.substring(currentUser.apellido, 0, 1)}">
        </span>
    </div>
</div>
```

## ⚠️ Nota Importante

Los cambios en el código Java requieren reiniciar la aplicación. Los cambios en plantillas HTML (Thymeleaf) se recargan automáticamente en modo desarrollo.

---

**Siguiente paso:** Reinicia la aplicación con `.\restart-app.ps1`
