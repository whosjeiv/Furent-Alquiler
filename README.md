<<<<<<< HEAD
# 🪑 Furent — Plataforma de Alquiler de Mobiliarios

[![CI](https://github.com/tu-usuario/furent/actions/workflows/ci.yml/badge.svg)](https://github.com/tu-usuario/furent/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-green)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-7-brightgreen)](https://www.mongodb.com/)

## 📋 Descripción

**Furent** es una plataforma web completa para la gestión de alquiler de mobiliarios para eventos. Permite a los clientes explorar catálogos, solicitar cotizaciones, gestionar reservas y realizar pagos; mientras que los administradores gestionan inventario, logística, usuarios, cupones y métricas de negocio.

## ✨ Características Principales

### Para Clientes
- 🔍 **Catálogo con buscador** — búsqueda en tiempo real por nombre, categoría y material
- 📋 **Cotizaciones Online** — sistema completo de solicitud con carrito
- ❤️ **Favoritos** — guardar productos para consulta rápida
- 💳 **Pagos** — flujo completo con múltiples métodos (Nequi, Daviplata, Transferencia, Efectivo)
- 🔔 **Notificaciones** — alertas en tiempo real sobre estado de reservas y pagos
- 🎟️ **Cupones** — sistema de descuentos con validación automática
- ⭐ **Reseñas** — calificación de productos con promedio
- 📱 **Responsive** — diseño adaptativo para cualquier dispositivo

### Para Administradores
- 📊 **Dashboard** — métricas en tiempo real (ingresos, reservas, usuarios)
- 📦 **Gestión de Inventario** — productos, categorías, stock, mantenimiento
- 🗓️ **Logística** — calendario de entregas/recogidas, hojas de ruta PDF
- 📝 **Reservas** — máquina de estados (PENDIENTE → CONFIRMADA → ACTIVA → COMPLETADA)
- 👥 **Gestión de Usuarios** — roles, suspensión temporal/permanente
- 📧 **Mensajes de Contacto** — bandeja de entrada con indicador de no leídos
- 🎫 **Cupones de Descuento** — crear, editar, gestionar vigencia
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

### Instalación Local

```bash
# Clonar
git clone https://github.com/tu-usuario/furent.git
cd furent

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

| Variable | Descripción | Default |
|----------|-------------|---------|
| `SPRING_DATA_MONGODB_URI` | URI de conexión a MongoDB | `mongodb://localhost:27017/furent` |
| `SPRING_PROFILES_ACTIVE` | Perfil activo | `dev` |
| `FURENT_ADMIN_PASSWORD` | Contraseña del admin inicial | `admin123` |

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

- **CSRF** habilitado (excepto endpoints API REST)
- **Content Security Policy** (CSP) configurado
- **Sesiones** con timeout, cookie HttpOnly, SameSite=Lax
- **BCrypt** para hashing de contraseñas
- **Roles** USER / ADMIN con control de acceso por ruta
- **Suspensión de cuentas** temporal y permanente
- **Auditoría** completa de acciones administrativas

## 📖 API Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/productos/search?q=` | Buscar productos |
| POST | `/api/cotizacion` | Crear cotización |
| POST/DELETE | `/api/favoritos/{id}` | Gestionar favoritos |
| GET | `/api/notificaciones` | Obtener notificaciones |
| POST | `/api/cupones/validar` | Validar cupón |
| POST | `/api/pagos/iniciar/{id}` | Iniciar pago |
| GET | `/api/pagos/mis-pagos` | Mis pagos |

Documentación interactiva en: **http://localhost:8080/swagger-ui.html**

## 🧪 Testing

```bash
# Ejecutar todos los tests
./mvnw test

# Tests con reporte
./mvnw test -Dmaven.test.failure.ignore=false
```

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
 ┌───────────┐    confirmar    ┌────────────┐    activar     ┌─────────┐
 │ PENDIENTE ├───────────────>│ CONFIRMADA ├──────────────>│ ACTIVA  │
 └─────┬─────┘                └─────┬──────┘               └────┬────┘
       │                            │                           │
       │ cancelar                   │ cancelar                  │ en curso
       │                            │                           │
       ▼                            ▼                    ┌──────▼──────┐
 ┌───────────┐              ┌───────────┐                │  EN_CURSO   │
 │ CANCELADA │◄─────────────│ CANCELADA │◄───────────────┤             │
 └───────────┘              └───────────┘                └──────┬──────┘
                                                                │
                                                                │ completar
                                                                ▼
                                                         ┌─────────────┐
                                                         │ COMPLETADA  │
                                                         └─────────────┘
```

## 🔑 Perfiles de Configuración

| Perfil | Archivo | Uso |
|--------|---------|-----|
| **dev** (default) | `application-dev.properties` | Desarrollo local, logs DEBUG, email deshabilitado |
| **prod** | `application-prod.properties` | Producción, cache habilitado, sesiones estrictas |

Activar perfil: `SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run`

## 📜 Licencia

MIT © Furent
=======
# FURENT
Programacion del proyecto FURENT - Alquiler de Mobiliarios
>>>>>>> abb589d02aa02c624115b133507a2c1ebb6fba25
