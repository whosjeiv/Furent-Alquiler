# Guía de Despliegue - Furent

Esta guía documenta el proceso completo de despliegue de Furent en entornos de producción.

## Tabla de Contenidos

- [Requisitos Previos](#requisitos-previos)
- [Variables de Entorno](#variables-de-entorno)
- [Configuración de MongoDB](#configuración-de-mongodb)
- [Configuración de Redis](#configuración-de-redis)
- [Configuración de SMTP](#configuración-de-smtp)
- [Despliegue con Docker](#despliegue-con-docker)
- [Despliegue Manual](#despliegue-manual)
- [Checklist Pre-Deployment](#checklist-pre-deployment)
- [Checklist Post-Deployment](#checklist-post-deployment)
- [Monitoreo y Observabilidad](#monitoreo-y-observabilidad)
- [Troubleshooting](#troubleshooting)

---

## Requisitos Previos

### Software Requerido

- **Java**: JDK 17 o superior
- **Maven**: 3.8+ (para compilación)
- **MongoDB**: 7.0 o superior
- **Redis**: 7.0 o superior
- **Docker** (opcional): 24.0+ con Docker Compose
- **Nginx** (recomendado): Para proxy reverso y SSL

### Hardware Mínimo (Producción)

- **CPU**: 2 cores
- **RAM**: 4 GB (2 GB para la aplicación)
- **Disco**: 20 GB (10 GB para MongoDB)
- **Red**: Conexión estable con ancho de banda adecuado

### Puertos Requeridos

- `8080`: Aplicación Spring Boot
- `27017`: MongoDB
- `6379`: Redis
- `9090`: Prometheus (opcional)
- `3000`: Grafana (opcional)

---

## Variables de Entorno

> **📖 Documentación Completa**: Para una guía detallada de todas las variables de entorno, ejemplos de configuración y mejores prácticas de seguridad, consulta [ENVIRONMENT_VARIABLES.md](./ENVIRONMENT_VARIABLES.md)

### Variables Obligatorias

Estas variables **DEBEN** configurarse en producción:

```bash
# MongoDB
export MONGODB_URI="mongodb://usuario:password@host:27017/furent?authSource=admin"

# Redis
export REDIS_HOST="redis.example.com"
export REDIS_PORT="6379"
export REDIS_PASSWORD="your-redis-password"

# Admin Password (CRÍTICO)
export FURENT_ADMIN_PASSWORD="your-secure-admin-password"

# JWT Secret (CRÍTICO)
export JWT_SECRET="base64-encoded-secret-key-minimum-256-bits"

# Email SMTP
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="your-app-specific-password"
```

### Variables Opcionales

```bash
# Spring Profile
export SPRING_PROFILES_ACTIVE="prod"

# PayU (Pasarela de pago)
export PAYU_API_KEY="your-payu-api-key"
export PAYU_MERCHANT_ID="your-merchant-id"
export PAYU_ACCOUNT_ID="your-account-id"

# Grafana
export GRAFANA_PASSWORD="your-grafana-password"
```

### Generación de Secrets Seguros

```bash
# Generar JWT Secret (base64, 256 bits)
openssl rand -base64 32

# Generar Admin Password seguro
openssl rand -base64 24
```

---

## Configuración de MongoDB

### 1. Instalación de MongoDB 7

#### Ubuntu/Debian

```bash
# Importar clave pública
wget -qO - https://www.mongodb.org/static/pgp/server-7.0.asc | sudo apt-key add -

# Agregar repositorio
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list

# Instalar
sudo apt-get update
sudo apt-get install -y mongodb-org

# Iniciar servicio
sudo systemctl start mongod
sudo systemctl enable mongod
```

#### Docker

```bash
docker run -d \
  --name furent-mongo \
  -p 27017:27017 \
  -v mongo-data:/data/db \
  -e MONGO_INITDB_ROOT_USERNAME=admin \
  -e MONGO_INITDB_ROOT_PASSWORD=your-password \
  mongo:7
```

### 2. Crear Base de Datos y Usuario

```javascript
// Conectar a MongoDB
mongosh

// Crear usuario para Furent
use admin
db.createUser({
  user: "furent_user",
  pwd: "secure_password_here",
  roles: [
    { role: "readWrite", db: "furent" },
    { role: "dbAdmin", db: "furent" }
  ]
})

// Verificar conexión
use furent
db.auth("furent_user", "secure_password_here")
```

### 3. Crear Índices MongoDB

La aplicación crea índices automáticamente con `spring.data.mongodb.auto-index-creation=true`, pero para producción se recomienda crearlos manualmente:

```javascript
use furent

// Índices para Users
db.users.createIndex({ "tenantId": 1, "email": 1 }, { unique: true })
db.users.createIndex({ "tenantId": 1, "estado": 1 })
db.users.createIndex({ "email": 1 })

// Índices para Products
db.products.createIndex({ "tenantId": 1, "categoriaNombre": 1 })
db.products.createIndex({ "tenantId": 1, "disponible": 1 })
db.products.createIndex({ "nombre": "text", "descripcion": "text" })

// Índices para Reservations
db.reservations.createIndex({ "tenantId": 1, "usuarioId": 1 })
db.reservations.createIndex({ "tenantId": 1, "estado": 1 })
db.reservations.createIndex({ "fechaInicio": 1, "fechaFin": 1 })

// Índices para Payments
db.payments.createIndex({ "tenantId": 1, "usuarioId": 1 })
db.payments.createIndex({ "tenantId": 1, "estado": 1 })
db.payments.createIndex({ "reservaId": 1 })
db.payments.createIndex({ "referencia": 1 }, { unique: true })

// Índices para Password Reset Tokens
db.password_reset_tokens.createIndex({ "tenantId": 1, "userId": 1 })
db.password_reset_tokens.createIndex({ "token": 1 }, { unique: true })
db.password_reset_tokens.createIndex({ "expiresAt": 1 }, { expireAfterSeconds: 0 })

// Índices para Contact Messages
db.contact_messages.createIndex({ "tenantId": 1, "leido": 1 })
db.contact_messages.createIndex({ "fechaCreacion": -1 })

// Índices para Coupons
db.coupons.createIndex({ "tenantId": 1, "codigo": 1 }, { unique: true })
db.coupons.createIndex({ "tenantId": 1, "activo": 1 })

// Índices para Categories
db.categories.createIndex({ "tenantId": 1, "nombre": 1 }, { unique: true })
```

### 4. Configuración de Seguridad MongoDB

```javascript
// Habilitar autenticación en /etc/mongod.conf
security:
  authorization: enabled

// Configurar bind IP
net:
  bindIp: 127.0.0.1,<your-server-ip>
  port: 27017

// Reiniciar MongoDB
sudo systemctl restart mongod
```

### 5. Backup y Restore

```bash
# Backup completo
mongodump --uri="mongodb://furent_user:password@localhost:27017/furent" --out=/backup/furent-$(date +%Y%m%d)

# Restore
mongorestore --uri="mongodb://furent_user:password@localhost:27017/furent" /backup/furent-20260315
```

---

## Configuración de Redis

### 1. Instalación de Redis 7

#### Ubuntu/Debian

```bash
# Instalar Redis
sudo apt-get update
sudo apt-get install -y redis-server

# Configurar Redis
sudo nano /etc/redis/redis.conf
```

#### Docker

```bash
docker run -d \
  --name furent-redis \
  -p 6379:6379 \
  -v redis-data:/data \
  redis:7-alpine redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
```

### 2. Configuración de Redis para Producción

Editar `/etc/redis/redis.conf`:

```conf
# Bind a IP específica (no usar 0.0.0.0 en producción)
bind 127.0.0.1 <your-server-ip>

# Habilitar password
requirepass your-secure-redis-password

# Configurar memoria máxima
maxmemory 256mb
maxmemory-policy allkeys-lru

# Persistencia (opcional, para cache no es crítico)
save 900 1
save 300 10
save 60 10000

# Logs
loglevel notice
logfile /var/log/redis/redis-server.log
```

### 3. Reiniciar Redis

```bash
sudo systemctl restart redis-server
sudo systemctl enable redis-server

# Verificar estado
redis-cli -a your-password ping
# Debe retornar: PONG
```

### 4. Monitoreo de Redis

```bash
# Ver estadísticas
redis-cli -a your-password INFO stats

# Ver memoria usada
redis-cli -a your-password INFO memory

# Ver claves en cache
redis-cli -a your-password KEYS "*"
```

---

## Configuración de SMTP

### Opción 1: Gmail (Desarrollo/Pequeña Escala)

#### 1. Habilitar Autenticación de 2 Factores

1. Ir a [Google Account Security](https://myaccount.google.com/security)
2. Habilitar "Verificación en 2 pasos"

#### 2. Generar Contraseña de Aplicación

1. Ir a [App Passwords](https://myaccount.google.com/apppasswords)
2. Seleccionar "Correo" y "Otro (nombre personalizado)"
3. Ingresar "Furent App"
4. Copiar la contraseña generada (16 caracteres)

#### 3. Configurar Variables de Entorno

```bash
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="xxxx xxxx xxxx xxxx"  # Contraseña de aplicación
```

#### 4. Límites de Gmail

- **Límite diario**: 500 emails/día (cuentas gratuitas)
- **Límite por minuto**: ~20 emails/minuto
- **Recomendación**: Solo para desarrollo o aplicaciones pequeñas

### Opción 2: SendGrid (Producción Recomendada)

#### 1. Crear Cuenta en SendGrid

1. Registrarse en [SendGrid](https://sendgrid.com/)
2. Verificar dominio
3. Crear API Key

#### 2. Configurar en application-prod.properties

```properties
spring.mail.host=smtp.sendgrid.net
spring.mail.port=587
spring.mail.username=apikey
spring.mail.password=${SENDGRID_API_KEY}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

#### 3. Límites de SendGrid

- **Free Tier**: 100 emails/día
- **Essentials**: 40,000 emails/mes ($19.95/mes)
- **Pro**: 100,000 emails/mes ($89.95/mes)

### Opción 3: Amazon SES (Alta Escala)

#### 1. Configurar AWS SES

```bash
# Instalar AWS CLI
aws configure

# Verificar dominio
aws ses verify-domain-identity --domain example.com
```

#### 2. Configurar en application-prod.properties

```properties
spring.mail.host=email-smtp.us-east-1.amazonaws.com
spring.mail.port=587
spring.mail.username=${AWS_SES_USERNAME}
spring.mail.password=${AWS_SES_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

#### 3. Límites de AWS SES

- **Sandbox**: 200 emails/día
- **Producción**: 50,000 emails/día (solicitar aumento)
- **Costo**: $0.10 por 1,000 emails

### Verificar Configuración SMTP

```bash
# Probar envío de email desde la aplicación
curl -X POST http://localhost:8080/api/test-email \
  -H "Content-Type: application/json" \
  -d '{"to": "test@example.com", "subject": "Test", "body": "Test email"}'
```

---

## Despliegue con Docker

### 1. Compilar la Aplicación

```bash
# Clonar repositorio
git clone https://github.com/your-org/furent.git
cd furent

# Compilar con Maven
./mvnw clean package -DskipTests

# Verificar JAR generado
ls -lh target/furent-*.jar
```

### 2. Configurar Variables de Entorno

Crear archivo `.env`:

```bash
# .env
MONGODB_URI=mongodb://furent_user:password@mongo:27017/furent
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password
FURENT_ADMIN_PASSWORD=your-admin-password
JWT_SECRET=your-jwt-secret-base64
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
GRAFANA_PASSWORD=your-grafana-password
```

### 3. Iniciar Stack Completo

```bash
# Iniciar todos los servicios
docker-compose up -d

# Ver logs
docker-compose logs -f app

# Verificar estado
docker-compose ps
```

### 4. Verificar Servicios

```bash
# Aplicación
curl http://localhost:8080/actuator/health

# MongoDB
docker exec -it furent-mongo mongosh --eval "db.adminCommand('ping')"

# Redis
docker exec -it furent-redis redis-cli ping

# Prometheus
curl http://localhost:9090/-/healthy

# Grafana
curl http://localhost:3000/api/health
```

### 5. Detener y Limpiar

```bash
# Detener servicios
docker-compose down

# Detener y eliminar volúmenes (CUIDADO: elimina datos)
docker-compose down -v

# Ver logs de un servicio específico
docker-compose logs app
```

---

## Despliegue Manual

### 1. Preparar el Servidor

```bash
# Actualizar sistema
sudo apt-get update && sudo apt-get upgrade -y

# Instalar Java 17
sudo apt-get install -y openjdk-17-jdk

# Verificar instalación
java -version
```

### 2. Crear Usuario de Sistema

```bash
# Crear usuario dedicado
sudo useradd -r -m -U -d /opt/furent -s /bin/bash furent

# Crear directorios
sudo mkdir -p /opt/furent/{app,logs,uploads}
sudo chown -R furent:furent /opt/furent
```

### 3. Copiar Aplicación

```bash
# Copiar JAR al servidor
scp target/furent-2.0.0.jar user@server:/opt/furent/app/

# Dar permisos
sudo chown furent:furent /opt/furent/app/furent-2.0.0.jar
sudo chmod 500 /opt/furent/app/furent-2.0.0.jar
```

### 4. Crear Archivo de Configuración

```bash
# Crear archivo de variables de entorno
sudo nano /opt/furent/app/furent.env
```

Contenido de `furent.env`:

```bash
SPRING_PROFILES_ACTIVE=prod
MONGODB_URI=mongodb://furent_user:password@localhost:27017/furent
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password
FURENT_ADMIN_PASSWORD=your-admin-password
JWT_SECRET=your-jwt-secret
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

### 5. Crear Servicio Systemd

```bash
sudo nano /etc/systemd/system/furent.service
```

Contenido de `furent.service`:

```ini
[Unit]
Description=Furent SaaS Application
After=network.target mongodb.service redis.service

[Service]
Type=simple
User=furent
Group=furent
WorkingDirectory=/opt/furent/app
EnvironmentFile=/opt/furent/app/furent.env
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar /opt/furent/app/furent-2.0.0.jar
SuccessExitStatus=143
TimeoutStopSec=10
Restart=on-failure
RestartSec=5

# Logging
StandardOutput=journal
StandardError=journal
SyslogIdentifier=furent

# Security
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/opt/furent/logs /opt/furent/uploads

[Install]
WantedBy=multi-user.target
```

### 6. Iniciar Servicio

```bash
# Recargar systemd
sudo systemctl daemon-reload

# Habilitar inicio automático
sudo systemctl enable furent

# Iniciar servicio
sudo systemctl start furent

# Verificar estado
sudo systemctl status furent

# Ver logs
sudo journalctl -u furent -f
```

### 7. Configurar Nginx como Proxy Reverso

```bash
sudo nano /etc/nginx/sites-available/furent
```

Contenido de configuración Nginx:

```nginx
upstream furent_backend {
    server 127.0.0.1:8080;
}

server {
    listen 80;
    server_name furent.example.com;

    # Redirigir HTTP a HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name furent.example.com;

    # SSL Certificates (usar Let's Encrypt)
    ssl_certificate /etc/letsencrypt/live/furent.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/furent.example.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # Security Headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Logs
    access_log /var/log/nginx/furent-access.log;
    error_log /var/log/nginx/furent-error.log;

    # Client body size (para uploads)
    client_max_body_size 10M;

    location / {
        proxy_pass http://furent_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Static files (si se sirven desde Nginx)
    location /uploads/ {
        alias /opt/furent/uploads/;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

Habilitar sitio:

```bash
# Crear symlink
sudo ln -s /etc/nginx/sites-available/furent /etc/nginx/sites-enabled/

# Verificar configuración
sudo nginx -t

# Recargar Nginx
sudo systemctl reload nginx
```

### 8. Configurar SSL con Let's Encrypt

```bash
# Instalar Certbot
sudo apt-get install -y certbot python3-certbot-nginx

# Obtener certificado
sudo certbot --nginx -d furent.example.com

# Renovación automática (ya configurada por defecto)
sudo certbot renew --dry-run
```

---

## Checklist Pre-Deployment

### Configuración

- [ ] Variables de entorno configuradas correctamente
- [ ] `FURENT_ADMIN_PASSWORD` es seguro (mínimo 16 caracteres)
- [ ] `JWT_SECRET` es único y seguro (base64, 256 bits)
- [ ] Credenciales SMTP configuradas y verificadas
- [ ] MongoDB URI apunta al servidor correcto
- [ ] Redis configurado con password
- [ ] Profile de Spring configurado como `prod`

### Base de Datos

- [ ] MongoDB instalado y corriendo
- [ ] Usuario de base de datos creado con permisos correctos
- [ ] Índices MongoDB creados manualmente
- [ ] Backup inicial de MongoDB configurado
- [ ] Autenticación MongoDB habilitada
- [ ] Firewall configurado para MongoDB (solo IPs permitidas)

### Cache

- [ ] Redis instalado y corriendo
- [ ] Redis configurado con password
- [ ] Política de evicción configurada (`allkeys-lru`)
- [ ] Memoria máxima configurada
- [ ] Firewall configurado para Redis

### Seguridad

- [ ] Firewall configurado (UFW/iptables)
- [ ] Solo puertos necesarios abiertos (80, 443, 22)
- [ ] SSH configurado con autenticación por clave
- [ ] Fail2ban instalado y configurado
- [ ] SSL/TLS configurado correctamente
- [ ] Headers de seguridad HTTP configurados
- [ ] CSRF habilitado en la aplicación
- [ ] Contraseñas almacenadas con BCrypt

### Aplicación

- [ ] Aplicación compilada con `./mvnw clean package`
- [ ] Tests ejecutados exitosamente
- [ ] JAR copiado al servidor
- [ ] Servicio systemd creado y habilitado
- [ ] Logs configurados correctamente
- [ ] Directorio de uploads creado con permisos correctos
- [ ] Thymeleaf cache habilitado (`spring.thymeleaf.cache=true`)

### Infraestructura

- [ ] Nginx instalado y configurado
- [ ] Proxy reverso configurado
- [ ] SSL/TLS configurado con Let's Encrypt
- [ ] Logs de Nginx configurados
- [ ] Compresión habilitada en Nginx
- [ ] Rate limiting configurado (opcional)

### Monitoreo

- [ ] Prometheus configurado (opcional)
- [ ] Grafana configurado con dashboards (opcional)
- [ ] Alertas configuradas (opcional)
- [ ] Logs centralizados (opcional)

---

## Checklist Post-Deployment

### Verificación Inmediata

- [ ] Aplicación responde en `https://furent.example.com`
- [ ] Health check responde: `curl https://furent.example.com/actuator/health`
- [ ] Login de admin funciona con credenciales configuradas
- [ ] MongoDB conectado correctamente (verificar logs)
- [ ] Redis conectado correctamente (verificar logs)
- [ ] Emails de prueba se envían correctamente

### Pruebas Funcionales

- [ ] Registro de nuevo usuario funciona
- [ ] Email de bienvenida se recibe
- [ ] Login de usuario funciona
- [ ] Catálogo de productos se muestra correctamente
- [ ] Crear cotización funciona
- [ ] Sistema de pagos funciona
- [ ] Recuperación de contraseña funciona
- [ ] Panel de admin accesible
- [ ] CRUD de usuarios funciona
- [ ] CRUD de categorías funciona
- [ ] Formulario de contacto funciona
- [ ] Sistema de cupones funciona

### Verificación de Seguridad

- [ ] HTTPS funciona correctamente
- [ ] HTTP redirige a HTTPS
- [ ] Headers de seguridad presentes en respuestas
- [ ] CSRF tokens presentes en formularios
- [ ] Usuarios suspendidos no pueden hacer login
- [ ] Validación de uploads funciona (solo imágenes)
- [ ] Validación de fechas funciona en cotizaciones

### Monitoreo

- [ ] Métricas de Prometheus accesibles: `http://server:9090`
- [ ] Grafana accesible: `http://server:3000`
- [ ] Dashboards de Grafana configurados
- [ ] Logs de aplicación se escriben correctamente
- [ ] Logs de Nginx se escriben correctamente

### Performance

- [ ] Tiempo de respuesta < 500ms para páginas principales
- [ ] Cache de Redis funcionando (verificar hits/misses)
- [ ] Compresión Gzip/Brotli habilitada
- [ ] Static assets con cache headers correctos

### Backup

- [ ] Backup automático de MongoDB configurado
- [ ] Backup de uploads configurado
- [ ] Procedimiento de restore documentado y probado
- [ ] Backup almacenado en ubicación segura (off-site)

---

## Monitoreo y Observabilidad

### Prometheus Metrics

La aplicación expone métricas en `/actuator/prometheus`:

```bash
# Ver métricas
curl http://localhost:8080/actuator/prometheus
```

#### Métricas Clave

- `http_server_requests_seconds`: Latencia de requests HTTP
- `jvm_memory_used_bytes`: Uso de memoria JVM
- `jvm_threads_live`: Threads activos
- `payments_created_total`: Total de pagos creados
- `payments_completed_total`: Total de pagos completados
- `payments_failed_total`: Total de pagos fallidos
- `reservations_created_total`: Total de reservas creadas
- `cache_gets_total`: Total de operaciones de cache

### Grafana Dashboards

#### Dashboard de Aplicación

Importar dashboard de Spring Boot:
- Dashboard ID: `4701` (Spring Boot Statistics)
- Dashboard ID: `11378` (JVM Micrometer)

#### Queries Útiles

```promql
# Tasa de requests por segundo
rate(http_server_requests_seconds_count[5m])

# Latencia p95
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Uso de memoria
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# Tasa de errores
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
```

### Logs

#### Ubicación de Logs

- **Aplicación**: `/opt/furent/logs/furent.log`
- **Nginx Access**: `/var/log/nginx/furent-access.log`
- **Nginx Error**: `/var/log/nginx/furent-error.log`
- **MongoDB**: `/var/log/mongodb/mongod.log`
- **Redis**: `/var/log/redis/redis-server.log`
- **Systemd**: `journalctl -u furent`

#### Ver Logs en Tiempo Real

```bash
# Logs de aplicación
sudo journalctl -u furent -f

# Logs de Nginx
sudo tail -f /var/log/nginx/furent-access.log

# Logs de MongoDB
sudo tail -f /var/log/mongodb/mongod.log

# Logs de Redis
sudo tail -f /var/log/redis/redis-server.log
```

#### Rotación de Logs

Configurar logrotate para `/etc/logrotate.d/furent`:

```
/opt/furent/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    notifempty
    create 0640 furent furent
    sharedscripts
    postrotate
        systemctl reload furent > /dev/null 2>&1 || true
    endscript
}
```

### Alertas (Opcional)

Configurar Alertmanager con Prometheus:

```yaml
# alertmanager.yml
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'email'

receivers:
- name: 'email'
  email_configs:
  - to: 'admin@example.com'
    from: 'alertmanager@example.com'
    smarthost: smtp.gmail.com:587
    auth_username: 'your-email@gmail.com'
    auth_password: 'your-app-password'
```

Reglas de alerta en `prometheus.yml`:

```yaml
groups:
- name: furent_alerts
  rules:
  - alert: HighErrorRate
    expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "Alta tasa de errores 5xx"
      
  - alert: HighMemoryUsage
    expr: (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.9
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "Uso de memoria > 90%"
      
  - alert: ApplicationDown
    expr: up{job="furent"} == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "Aplicación Furent no responde"
```

---

## Troubleshooting

### Problema: Aplicación no inicia

#### Síntomas
```
sudo systemctl status furent
● furent.service - Furent SaaS Application
   Loaded: loaded
   Active: failed
```

#### Soluciones

1. **Verificar logs**:
```bash
sudo journalctl -u furent -n 100 --no-pager
```

2. **Verificar variables de entorno**:
```bash
sudo cat /opt/furent/app/furent.env
```

3. **Verificar conectividad a MongoDB**:
```bash
mongosh "mongodb://furent_user:password@localhost:27017/furent"
```

4. **Verificar conectividad a Redis**:
```bash
redis-cli -a your-password ping
```

5. **Verificar puerto 8080 disponible**:
```bash
sudo netstat -tulpn | grep 8080
```

### Problema: Error de conexión a MongoDB

#### Síntomas
```
com.mongodb.MongoTimeoutException: Timed out after 30000 ms while waiting to connect
```

#### Soluciones

1. **Verificar que MongoDB está corriendo**:
```bash
sudo systemctl status mongod
```

2. **Verificar URI de conexión**:
```bash
echo $MONGODB_URI
# Debe ser: mongodb://usuario:password@host:27017/furent
```

3. **Verificar autenticación**:
```bash
mongosh "mongodb://furent_user:password@localhost:27017/furent" --eval "db.adminCommand('ping')"
```

4. **Verificar firewall**:
```bash
sudo ufw status
sudo ufw allow from <app-server-ip> to any port 27017
```

5. **Verificar bind IP en MongoDB**:
```bash
sudo grep bindIp /etc/mongod.conf
# Debe incluir la IP del servidor de aplicación
```

### Problema: Error de conexión a Redis

#### Síntomas
```
io.lettuce.core.RedisConnectionException: Unable to connect to localhost:6379
```

#### Soluciones

1. **Verificar que Redis está corriendo**:
```bash
sudo systemctl status redis-server
```

2. **Verificar password**:
```bash
redis-cli -a your-password ping
```

3. **Verificar configuración**:
```bash
echo $REDIS_HOST
echo $REDIS_PORT
echo $REDIS_PASSWORD
```

4. **Verificar bind en Redis**:
```bash
sudo grep bind /etc/redis/redis.conf
```

### Problema: Emails no se envían

#### Síntomas
```
org.springframework.mail.MailAuthenticationException: Authentication failed
```

#### Soluciones

1. **Verificar credenciales SMTP**:
```bash
echo $MAIL_USERNAME
echo $MAIL_PASSWORD
```

2. **Verificar que es contraseña de aplicación (Gmail)**:
   - No usar contraseña normal de Gmail
   - Usar contraseña de aplicación de 16 caracteres

3. **Probar conexión SMTP**:
```bash
telnet smtp.gmail.com 587
```

4. **Verificar logs de email**:
```bash
sudo journalctl -u furent | grep -i "email\|mail"
```

5. **Verificar límites de envío**:
   - Gmail: 500 emails/día
   - Verificar que no se alcanzó el límite

### Problema: Alta latencia en requests

#### Síntomas
- Páginas tardan > 2 segundos en cargar
- Timeouts frecuentes

#### Soluciones

1. **Verificar uso de CPU**:
```bash
top -u furent
```

2. **Verificar uso de memoria**:
```bash
free -h
sudo systemctl status furent
```

3. **Verificar cache de Redis**:
```bash
redis-cli -a your-password INFO stats
# Ver hit rate
```

4. **Verificar queries lentas en MongoDB**:
```javascript
db.setProfilingLevel(2)
db.system.profile.find().sort({ts:-1}).limit(10).pretty()
```

5. **Aumentar memoria JVM**:
```bash
# Editar /etc/systemd/system/furent.service
ExecStart=/usr/bin/java -Xms1024m -Xmx2048m -jar /opt/furent/app/furent-2.0.0.jar
sudo systemctl daemon-reload
sudo systemctl restart furent
```

### Problema: Error 502 Bad Gateway (Nginx)

#### Síntomas
```
502 Bad Gateway
nginx/1.18.0
```

#### Soluciones

1. **Verificar que la aplicación está corriendo**:
```bash
sudo systemctl status furent
curl http://localhost:8080/actuator/health
```

2. **Verificar logs de Nginx**:
```bash
sudo tail -f /var/log/nginx/furent-error.log
```

3. **Verificar configuración de proxy**:
```bash
sudo nginx -t
```

4. **Aumentar timeouts en Nginx**:
```nginx
proxy_connect_timeout 120s;
proxy_send_timeout 120s;
proxy_read_timeout 120s;
```

### Problema: Disco lleno

#### Síntomas
```
java.io.IOException: No space left on device
```

#### Soluciones

1. **Verificar espacio en disco**:
```bash
df -h
```

2. **Limpiar logs antiguos**:
```bash
sudo journalctl --vacuum-time=7d
sudo find /var/log -name "*.gz" -mtime +30 -delete
```

3. **Limpiar uploads antiguos** (con cuidado):
```bash
sudo find /opt/furent/uploads -mtime +90 -type f -delete
```

4. **Limpiar Docker** (si aplica):
```bash
docker system prune -a --volumes
```

---

## Actualizaciones

### Proceso de Actualización

1. **Backup completo**:
```bash
# Backup MongoDB
mongodump --uri="mongodb://furent_user:password@localhost:27017/furent" --out=/backup/pre-update-$(date +%Y%m%d)

# Backup uploads
sudo tar -czf /backup/uploads-$(date +%Y%m%d).tar.gz /opt/furent/uploads
```

2. **Descargar nueva versión**:
```bash
cd /tmp
wget https://github.com/your-org/furent/releases/download/v2.1.0/furent-2.1.0.jar
```

3. **Detener aplicación**:
```bash
sudo systemctl stop furent
```

4. **Reemplazar JAR**:
```bash
sudo cp /tmp/furent-2.1.0.jar /opt/furent/app/
sudo chown furent:furent /opt/furent/app/furent-2.1.0.jar
```

5. **Actualizar servicio systemd** (si cambió versión):
```bash
sudo nano /etc/systemd/system/furent.service
# Actualizar ruta del JAR
sudo systemctl daemon-reload
```

6. **Iniciar aplicación**:
```bash
sudo systemctl start furent
```

7. **Verificar**:
```bash
sudo systemctl status furent
sudo journalctl -u furent -f
curl http://localhost:8080/actuator/health
```

8. **Rollback si falla**:
```bash
sudo systemctl stop furent
sudo cp /opt/furent/app/furent-2.0.0.jar /opt/furent/app/furent-current.jar
sudo systemctl start furent
```

---

## Escalabilidad

### Escalado Horizontal

Para escalar horizontalmente (múltiples instancias):

1. **Configurar sesiones en Redis**:
```properties
spring.session.store-type=redis
```

2. **Configurar load balancer** (Nginx):
```nginx
upstream furent_backend {
    least_conn;
    server 10.0.1.10:8080;
    server 10.0.1.11:8080;
    server 10.0.1.12:8080;
}
```

3. **Compartir directorio de uploads**:
   - Usar NFS o S3 para uploads compartidos

### Escalado Vertical

Para aumentar recursos de una instancia:

1. **Aumentar memoria JVM**:
```bash
-Xms2048m -Xmx4096m
```

2. **Aumentar workers de Tomcat**:
```properties
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=10
```

3. **Aumentar pool de conexiones MongoDB**:
```properties
spring.data.mongodb.max-pool-size=100
```

---

## Seguridad Adicional

### Fail2ban para Furent

Crear `/etc/fail2ban/filter.d/furent.conf`:

```ini
[Definition]
failregex = ^.*Authentication failed for user.*<HOST>.*$
            ^.*Invalid credentials.*<HOST>.*$
ignoreregex =
```

Crear `/etc/fail2ban/jail.d/furent.conf`:

```ini
[furent]
enabled = true
port = http,https
filter = furent
logpath = /opt/furent/logs/furent.log
maxretry = 5
bantime = 3600
findtime = 600
```

Reiniciar Fail2ban:
```bash
sudo systemctl restart fail2ban
sudo fail2ban-client status furent
```

### Rate Limiting en Nginx

```nginx
# Definir zona de rate limiting
limit_req_zone $binary_remote_addr zone=furent_limit:10m rate=10r/s;

server {
    # ...
    location / {
        limit_req zone=furent_limit burst=20 nodelay;
        proxy_pass http://furent_backend;
    }
}
```

---

## Contacto y Soporte

Para soporte técnico o consultas sobre el despliegue:

- **Email**: soporte@furent.com
- **Documentación**: https://docs.furent.com
- **GitHub Issues**: https://github.com/your-org/furent/issues

---

**Última actualización**: Marzo 2026  
**Versión del documento**: 1.0  
**Versión de Furent**: 2.0.0
