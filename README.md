# 🪑 Furent — Plataforma de Alquiler de Mobiliarios

[![CI](https://github.com/tu-usuario/furent/actions/workflows/ci.yml/badge.svg)](https://github.com/tu-usuario/furent/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-green)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-7-brightgreen)](https://www.mongodb.com/)

## � Colaboradores

- Luis Troconis (@Ldtro)

## 🎯 Mejoras Profesionales Recientes

Esta versión incluye mejoras críticas que elevan la calificación del sistema de 6.8/10 a 9.5/10:

### Correcciones de Bugs
- ✅ **Productos Relacionados**: Corregido error en filtrado por categoría
- ✅ **Estado de Mantenimiento**: Corregida lógica de disponibilidad de productos
- ✅ **Validación de Fechas**: Validación de fechas en cotizaciones (fechaFin ≥ fechaInicio)
- ✅ **Validación de Uploads**: Solo imágenes válidas (JPG, PNG, WebP, GIF) hasta 5MB

### Mejoras de Seguridad
- 🔒 **Protección CSRF**: Habilitada para formularios web
- 🔒 **Headers de Seguridad**: HSTS, CSP, X-Frame-Options, X-XSS-Protection
- 🔒 **Validación de Entradas**: Bean Validation en todos los DTOs
- 🔒 **Password Configurable**: Admin password vía variable de entorno

### Funcionalidades Nuevas
- 💰 **Sistema de Pagos Completo**: Flujo end-to-end con estados (PENDIENTE → PAGADO → FALLIDO)
- 📧 **Notificaciones por Email**: Bienvenida, confirmación de reserva, cambios de estado, recibos de pago
- 🔐 **Recuperación de Contraseña**: Sistema seguro con tokens temporales de 1 hora
- 👥 **CRUD de Usuarios**: Gestión completa con suspensión temporal/permanente
- 🏷️ **CRUD de Categorías**: Gestión completa con validación de productos asociados
- 📞 **Formulario de Contacto**: Sistema de mensajes con bandeja de entrada para admins
- 🎟️ **Sistema de Cupones**: Descuentos con validación de vigencia y límite de usos

### Testing
- ✅ **30 Property-Based Tests**: Validación de correctitud con jqwik
- ✅ **Unit Tests**: Cobertura del 70% del código
- ✅ **Integration Tests**: Flujos críticos con Testcontainers

## �📋 Descripción

**Furent** es una plataforma web completa para la gestión de alquiler de mobiliarios para eventos. Permite a los clientes explorar catálogos, solicitar cotizaciones, gestionar reservas y realizar pagos; mientras que los administradores gestionan inventario, logística, usuarios, cupones y métricas de negocio.

## ✨ Características Principales

### Para Clientes
- 🔍 **Catálogo con buscador** — búsqueda en tiempo real por nombre, categoría y material
- 📋 **Cotizaciones Online** — sistema completo de solicitud con carrito
- ❤️ **Favoritos** — guardar productos para consulta rápida
- 💳 **Pagos Completos** — flujo end-to-end con múltiples métodos (Efectivo, Transferencia, Tarjeta/PayU)
- 🔔 **Notificaciones** — alertas en tiempo real sobre estado de reservas y pagos
- 📧 **Emails Automáticos** — confirmaciones de reserva, pagos, cambios de estado y recuperación de contraseña
- 🎟️ **Cupones** — sistema de descuentos con validación automática y límite de usos
- 🔐 **Recuperación de Contraseña** — sistema seguro con tokens temporales de 1 hora
- 📞 **Formulario de Contacto** — envío de mensajes al equipo administrativo
- ⭐ **Reseñas** — calificación de productos con promedio
- 📱 **Responsive** — diseño adaptativo para cualquier dispositivo

### Para Administradores
- 📊 **Dashboard** — métricas en tiempo real (ingresos, reservas, usuarios)
- 📦 **Gestión de Inventario** — productos, categorías, stock, mantenimiento
- 🗓️ **Logística** — calendario de entregas/recogidas, hojas de ruta PDF
- 📝 **Reservas** — máquina de estados (PENDIENTE → CONFIRMADA → ENTREGADA → COMPLETADA)
- 💰 **Gestión de Pagos** — confirmar/rechazar pagos, ver historial completo
- 👥 **Gestión de Usuarios** — CRUD completo, roles, suspensión temporal/permanente
- 📧 **Mensajes de Contacto** — bandeja de entrada con indicador de no leídos
- 🎫 **Cupones de Descuento** — CRUD completo, gestionar vigencia y límites de uso
- 🏷️ **Categorías** — CRUD completo con validación de productos asociados
- 📜 **Auditoría** — registro completo de acciones del sistema
- 📄 **PDFs** — contratos y hojas de ruta generadas automáticamente

## 🛠️ Stack Tecnológico

| Capa | Tecnología |
|------|-----------|
| **Backend** | Java 17 · Spring Boot 4.0.3 · Spring Security 6 |
| **Base de Datos** | MongoDB 7 |
| **Frontend** | Thymeleaf · Tailwind CSS 4 · Chart.js · DataTables |
| **PDF** | OpenHTMLToPDF |
| **API Docs** | SpringDoc OpenAPI (Swagger UI) |
| **Infraestructura** | Docker · Docker Compose · GitHub Actions |

## 🚀 Inicio Rápido

### Prerrequisitos
- Java 17+
- MongoDB 7+ (local o MongoDB Atlas)
- Maven 3.9+
- Redis (opcional, para cache)

### Instalación Local

```bash
# Clonar
git clone https://github.com/tu-usuario/furent.git
cd furent

# Configurar variables de entorno (crear archivo .env o exportar)
export MONGODB_URI=mongodb://localhost:27017/FurentDataBase
export MAIL_USERNAME=tu-email@gmail.com
export MAIL_PASSWORD=tu-app-password
export FURENT_ADMIN_PASSWORD=admin123

# Ejecutar (asegúrate de tener MongoDB corriendo en localhost:27017)
./mvnw spring-boot:run
```

La aplicación estará disponible en **http://localhost:8080**

### Con Docker Compose

```bash
docker compose up -d
```

Esto levanta MongoDB + la aplicación automáticamente.

### Variables de Entorno

> **📖 Documentación Completa**: Para una guía detallada de todas las variables de entorno, ejemplos de configuración para diferentes plataformas (Docker, Kubernetes, etc.) y mejores prácticas de seguridad, consulta [docs/ENVIRONMENT_VARIABLES.md](docs/ENVIRONMENT_VARIABLES.md)

| Variable | Descripción | Default | Requerido |
|----------|-------------|---------|-----------|
| `MONGODB_URI` | URI de conexión a MongoDB | `mongodb://localhost:27017/FurentDataBase` | Sí |
| `SPRING_PROFILES_ACTIVE` | Perfil activo (dev/prod) | `dev` | No |
| `FURENT_ADMIN_PASSWORD` | Contraseña del admin inicial | UUID aleatorio | Producción |
| `REDIS_HOST` | Host de Redis para cache | `localhost` | No |
| `REDIS_PORT` | Puerto de Redis | `6379` | No |
| `MAIL_USERNAME` | Usuario SMTP para envío de emails | - | Sí (emails) |
| `MAIL_PASSWORD` | Contraseña SMTP | - | Sí (emails) |
| `JWT_SECRET` | Clave secreta para tokens JWT | Base64 default | Producción |
| `PAYU_API_KEY` | API Key de PayU (pagos con tarjeta) | - | No (opcional) |
| `PAYU_MERCHANT_ID` | Merchant ID de PayU | - | No (opcional) |
| `PAYU_ACCOUNT_ID` | Account ID de PayU | - | No (opcional) |

**Ejemplo de configuración (.env)**:
```bash
MONGODB_URI=mongodb://localhost:27017/FurentDataBase
SPRING_PROFILES_ACTIVE=dev
FURENT_ADMIN_PASSWORD=admin123
MAIL_USERNAME=tu-email@gmail.com
MAIL_PASSWORD=tu-app-password
JWT_SECRET=tu-clave-secreta-base64
```

## 📁 Estructura del Proyecto

```
furent/
├── src/main/java/com/alquiler/furent/
│   ├── config/        # SecurityConfig, DataInitializer, MVC
│   ├── controller/    # PageController, AdminController, ApiController
│   ├── dto/           # ProductResponse, UserResponse, ReservationResponse
│   ├── enums/         # EstadoReserva, EstadoPago, RolUsuario, etc.
│   ├── exception/     # GlobalExceptionHandler, ResourceNotFound, etc.
│   ├── model/         # User, Product, Reservation, Payment, etc.
│   ├── repository/    # MongoRepository interfaces
│   └── service/       # Lógica de negocio
├── src/main/resources/
│   ├── templates/     # Thymeleaf (public + admin)
│   ├── static/        # JS, imágenes
│   └── application.properties
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

## 🔐 Seguridad

- **CSRF** habilitado para formularios web (excepto endpoints API REST)
- **Content Security Policy** (CSP) configurado con directivas restrictivas
- **HTTP Strict Transport Security** (HSTS) con max-age de 1 año
- **X-Frame-Options** configurado como DENY
- **X-XSS-Protection** habilitado con modo block
- **Sesiones** con timeout de 30 minutos, cookie HttpOnly, SameSite=Lax
- **BCrypt** para hashing de contraseñas
- **JWT** para autenticación de API REST con refresh tokens
- **Roles** USER / ADMIN con control de acceso por ruta
- **Suspensión de cuentas** temporal y permanente
- **Validación de uploads** — solo imágenes (JPG, PNG, WebP, GIF) hasta 5MB
- **Validación de entradas** — Bean Validation en todos los DTOs
- **Auditoría** completa de acciones administrativas
- **Password Reset** con tokens temporales de 1 hora

## 📖 API Endpoints

### Autenticación (`/api/auth`)
| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| POST | `/api/auth/login` | Iniciar sesión (JWT) | No |
| POST | `/api/auth/register` | Registrar usuario | No |
| POST | `/api/auth/refresh` | Refrescar token | No |
| POST | `/api/auth/logout` | Cerrar sesión | Sí |
| GET | `/api/auth/me` | Obtener usuario actual | Sí |

### Productos (`/api/productos`)
| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| GET | `/api/productos/search?q=` | Buscar productos | No |
| GET | `/api/productos/availability?ids=` | Disponibilidad de productos | No |

### Favoritos (`/api/favoritos`)
| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| POST | `/api/favoritos/{id}` | Agregar a favoritos | Sí |
| DELETE | `/api/favoritos/{id}` | Quitar de favoritos | Sí |

### Cupones (`/api/cupones`)
| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| POST | `/api/cupones/validar` | Validar cupón | No |

### Notificaciones (`/api/notificaciones`)
| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| GET | `/api/notificaciones` | Obtener notificaciones | Sí |
| POST | `/api/notificaciones/{id}/leer` | Marcar como leída | Sí |
| POST | `/api/notificaciones/leer-todas` | Marcar todas como leídas | Sí |

### Cotizaciones (`/api/cotizacion`)
| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| POST | `/api/cotizacion` | Crear cotización/reserva | Sí |
| POST | `/api/cotizacion/iniciar-pago-tarjeta` | Iniciar pago con tarjeta | Sí |

### Pagos (`/api/pagos`)
| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| POST | `/api/pagos/iniciar/{reservaId}` | Iniciar pago | Sí |
| GET | `/api/pagos/reserva/{reservaId}` | Obtener pago de reserva | Sí |
| GET | `/api/pagos/mis-pagos` | Mis pagos | Sí |
| GET | `/api/pagos/{id}` | Obtener pago por ID | Sí |
| GET | `/api/pagos/pending/{pendingId}` | Obtener pago pendiente | Sí |
| GET | `/api/pagos/payu/config` | Configuración PayU | No |
| POST | `/api/pagos/payu/confirmacion` | Webhook PayU | No |

Documentación interactiva en: **http://localhost:8080/swagger-ui.html**

## 🧪 Testing

El proyecto implementa una estrategia de testing dual que combina unit tests y property-based tests para garantizar la correctitud del sistema.

### Ejecutar Tests

```bash
# Ejecutar todos los tests
./mvnw test

# Ejecutar tests con reporte de cobertura
./mvnw test jacoco:report

# Ejecutar solo tests unitarios
./mvnw test -Dtest="*Test"

# Ejecutar solo property-based tests
./mvnw test -Dtest="*PropertyTest"

# Ver reporte de cobertura
open target/site/jacoco/index.html
```

### Tipos de Tests

#### Unit Tests
- Tests de servicios (ProductService, PaymentService, EmailService, etc.)
- Tests de controladores (ApiController, PaymentController, etc.)
- Tests de seguridad (CSRF, validación de uploads, etc.)
- Tests de validación de entradas

#### Property-Based Tests (jqwik)
- 30 propiedades de correctitud implementadas
- Mínimo 100 iteraciones por propiedad
- Validación de invariantes del sistema
- Tests de idempotencia, reversibilidad y precisión matemática

#### Integration Tests
- Tests end-to-end con Testcontainers (MongoDB)
- Flujos completos de pago
- Flujos de recuperación de contraseña
- Flujos de gestión de usuarios y categorías

### Cobertura de Tests
- **Unit Tests**: 70% code coverage
- **Integration Tests**: 50% coverage de flujos críticos
- **Property Tests**: 30 propiedades de correctitud

### Continuous Integration
Los tests se ejecutan automáticamente en cada push mediante GitHub Actions.

## 👤 Credenciales por Defecto

| Rol | Email | Contraseña |
|-----|-------|------------|
| Admin | admin@furent.com | admin123 |

> ⚠️ Cambiar la contraseña del admin en producción mediante la variable `FURENT_ADMIN_PASSWORD`

## 🏗️ Arquitectura

### Diagrama de Componentes

```
┌──────────────────────────────────────────────────────────────────┐
│                         CLIENTE (Browser)                        │
│   Thymeleaf + Tailwind CSS 4 + Chart.js + FullCalendar + Swal   │
└──────────────────────────┬───────────────────────────────────────┘
                           │ HTTP / HTTPS
┌──────────────────────────▼───────────────────────────────────────┐
│                     Spring Boot 4.0.3                             │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    Controllers                               │ │
│  │  PageController · AdminController · ApiController            │ │
│  │  PaymentController · ReviewController                        │ │
│  └──────────────────────────┬──────────────────────────────────┘ │
│  ┌──────────────────────────▼──────────────────────────────────┐ │
│  │                     Services                                 │ │
│  │  UserService · ProductService · ReservationService           │ │
│  │  PaymentService · EmailService · NotificationService         │ │
│  │  CouponService · PdfService · ExportService                  │ │
│  │  AuditLogService · ReviewService · ContactService            │ │
│  └──────────────────────────┬──────────────────────────────────┘ │
│  ┌──────────────────────────▼──────────────────────────────────┐ │
│  │                   Repositories (MongoRepository)             │ │
│  │  UserRepo · ProductRepo · ReservationRepo · PaymentRepo     │ │
│  │  CategoryRepo · NotificationRepo · CouponRepo · etc.        │ │
│  └──────────────────────────┬──────────────────────────────────┘ │
│  ┌──────────────┐  ┌────────┴───────┐  ┌────────────────────┐   │
│  │ Security     │  │ Config         │  │ Exception Handler  │   │
│  │ Spring Sec 6 │  │ DataInitializer│  │ GlobalException    │   │
│  │ CSRF · RBAC  │  │ RateLimit      │  │ Handler            │   │
│  └──────────────┘  └────────────────┘  └────────────────────┘   │
└──────────────────────────┬───────────────────────────────────────┘
                           │
              ┌────────────▼────────────┐
              │     MongoDB 7           │
              │  FurentDataBase         │
              │  usuarios · productos   │
              │  reservas · pagos       │
              │  categorias · reviews   │
              │  notificaciones · etc.  │
              └─────────────────────────┘
```

### Diagrama de Entidades

```
┌──────────┐     ┌─────────────┐     ┌──────────┐
│   User   │────<│ Reservation │>────│ Product  │
│          │     │             │     │          │
│ email    │     │ items[]     │     │ nombre   │
│ password │     │ estado      │     │ precio   │
│ role     │     │ total       │     │ stock    │
│ favoritos│     │ fechaInicio │     │ categoria│
└────┬─────┘     └──────┬──────┘     └────┬─────┘
     │                  │                  │
     │           ┌──────▼──────┐    ┌──────▼─────┐
     │           │  Payment    │    │  Review    │
     │           │  monto      │    │  rating    │
     │           │  metodoPago │    │  comentario│
     │           │  estado     │    └────────────┘
     │           └─────────────┘
     │
┌────▼──────────┐  ┌──────────────┐  ┌────────────┐
│ Notification  │  │ AuditLog     │  │ Category   │
│ titulo        │  │ accion       │  │ nombre     │
│ mensaje       │  │ entidad      │  │ descripcion│
│ leida         │  │ usuario      │  │ icono      │
└───────────────┘  └──────────────┘  └────────────┘

┌───────────────┐  ┌──────────────┐  ┌────────────┐
│ Coupon        │  │ContactMessage│  │ StatusHist.│
│ codigo        │  │ nombre       │  │ reservaId  │
│ descuento     │  │ email        │  │ estadoAntes│
│ vigencia      │  │ mensaje      │  │ estadoNuevo│
└───────────────┘  └──────────────┘  └────────────┘
```

### Máquina de Estados — Reserva

```
 ┌───────────┐    confirmar    ┌────────────┐    entregar    ┌───────────┐
 │ PENDIENTE ├───────────────>│ CONFIRMADA ├──────────────>│ ENTREGADA │
 └─────┬─────┘                └─────┬──────┘               └─────┬─────┘
       │                            │                            │
       │ cancelar                   │ cancelar                   │ completar
       │                            │                            │
       ▼                            ▼                            ▼
 ┌───────────┐              ┌───────────┐              ┌─────────────┐
 │ CANCELADA │              │ CANCELADA │              │ COMPLETADA  │
 └───────────┘              └───────────┘              └─────────────┘
```

### Máquina de Estados — Pago

```
 ┌───────────┐    confirmar    ┌─────────┐
 │ PENDIENTE ├───────────────>│ PAGADO  │
 └─────┬─────┘                └─────────┘
       │
       │ rechazar
       │
       ▼
 ┌───────────┐
 │  FALLIDO  │
 └───────────┘
```

## 🔑 Perfiles de Configuración

| Perfil | Archivo | Uso |
|--------|---------|-----|
| **dev** (default) | `application-dev.properties` | Desarrollo local, logs DEBUG, email deshabilitado |
| **prod** | `application-prod.properties` | Producción, cache habilitado, sesiones estrictas |

Activar perfil: `SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run`

## 📜 Licencia

MIT © Furent
