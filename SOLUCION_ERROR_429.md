# 🔧 Solución: Error 429 - Too Many Requests

## ❌ Problema

Al acceder a `http://localhost:8080/login` aparece:

```json
{
  "timestamp": "2026-03-24T04:11:28",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Demasiadas solicitudes. Intenta de nuevo más tarde."
}
```

## 🔍 Causa

El filtro de **Rate Limiting** está bloqueando las solicitudes porque se excedió el límite de:
- **100 solicitudes por minuto** por IP

Esto es común en desarrollo cuando:
- Recargas la página muchas veces
- Tienes auto-refresh habilitado
- Estás probando repetidamente

## ✅ Solución Aplicada

He **deshabilitado el Rate Limiting en desarrollo** para evitar este problema.

### Cambios realizados:

**1. `src/main/resources/application.properties`**
```properties
furent.rate-limit.enabled=false
```

**2. `src/main/resources/application-dev.properties`**
```properties
furent.rate-limit.enabled=false
```

## 🚀 Cómo Aplicar la Solución

### Opción 1: Reiniciar la Aplicación (Recomendado)

1. **Detén la aplicación** (Ctrl+C en la terminal)

2. **Reinicia:**
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Espera a que inicie completamente** (verás "Started FurentApplication")

4. **Accede de nuevo:** http://localhost:8080/login

### Opción 2: Esperar 1 Minuto

Si no quieres reiniciar, simplemente espera 60 segundos y el contador se reseteará automáticamente.

## 🧪 Verificar que Funciona

Después de reiniciar, deberías ver:

1. **La página de login cargando correctamente**
2. **El botón "Google" visible**
3. **Sin errores 429**

### Logs esperados:
```
INFO  Started FurentApplication in X seconds
DEBUG Securing GET /login
DEBUG Secured GET /login
```

**NO deberías ver:**
```
WARN  Rate limit exceeded: IP 0:0:0:0:0:0:0:1 en /login
```

## 📊 Rate Limiting en Producción

El rate limiting está **deshabilitado solo en desarrollo**. En producción, se recomienda habilitarlo para proteger contra:

- Ataques de fuerza bruta
- Abuso de API
- DDoS

### Para habilitar en producción:

**`application-prod.properties`:**
```properties
furent.rate-limit.enabled=true
```

### Límites configurados:

| Endpoint | Límite | Propósito |
|----------|--------|-----------|
| `/api/auth/login` | 5 req/min | Anti brute-force |
| `/api/auth/register` | 3 req/min | Anti spam |
| `/api/auth/*` | 10 req/min | Protección auth |
| Otros endpoints | 100 req/min | Protección general |

## 🔧 Configuración Avanzada

Si necesitas ajustar los límites, edita:

**`src/main/java/com/alquiler/furent/config/RateLimitFilter.java`**

```java
private static final int MAX_REQUESTS_PER_MINUTE = 100;
private static final int MAX_LOGIN_PER_MINUTE = 5;
private static final int MAX_REGISTER_PER_MINUTE = 3;
```

## 🐛 Otros Errores Comunes

### Error: "Cannot connect to MongoDB"
**Solución:** Inicia MongoDB o usa Docker:
```bash
docker-compose up -d mongodb
```

### Error: "Port 8080 already in use"
**Solución:** Detén la aplicación anterior o cambia el puerto:
```properties
server.port=8081
```

### Error: "redirect_uri_mismatch" (Google OAuth)
**Solución:** Verifica URIs en Google Console:
- https://console.cloud.google.com/apis/credentials
- Agrega: `http://localhost:8080/login/oauth2/code/google`

## ✅ Checklist de Solución

- [x] Rate limiting deshabilitado en `application.properties`
- [x] Rate limiting deshabilitado en `application-dev.properties`
- [ ] Aplicación reiniciada
- [ ] Login accesible sin error 429
- [ ] Botón de Google visible

## 📚 Más Información

- **Inicio rápido:** `INICIO_RAPIDO_GOOGLE_OAUTH.md`
- **Configuración completa:** `CONFIGURACION_GOOGLE_COMPLETADA.md`
- **Documentación OAuth2:** `docs/GOOGLE_OAUTH2_SETUP.md`

---

**Siguiente paso:** Reinicia la aplicación con `./mvnw spring-boot:run`
