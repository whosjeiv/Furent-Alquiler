# 📐 Furent — Diagramas UML

> Diagramas UML detallados del sistema Furent usando notación Mermaid.

---

## 📋 Tabla de Contenidos

1. [Diagrama de Clases Completo](#1-diagrama-de-clases-completo)
2. [Diagrama de Secuencia — Reserva Completa](#2-diagrama-de-secuencia--reserva-completa)
3. [Diagrama de Secuencia — Autenticación](#3-diagrama-de-secuencia--autenticación)
4. [Diagrama de Secuencia — Pago](#4-diagrama-de-secuencia--pago)
5. [Diagrama de Actividad — Crear Cotización](#5-diagrama-de-actividad--crear-cotización)
6. [Diagrama de Actividad — Validar Cupón](#6-diagrama-de-actividad--validar-cupón)
7. [Diagrama de Casos de Uso](#7-diagrama-de-casos-de-uso)
8. [Diagrama de Paquetes](#8-diagrama-de-paquetes)
9. [Diagrama ER (Entidad-Relación)](#9-diagrama-er-entidad-relación)

---

## 1. Diagrama de Clases Completo

### 1.1 Controllers

```mermaid
classDiagram
    class AuthController {
        -UserService userService
        -JwtService jwtService
        -PasswordEncoder passwordEncoder
        -AuditLogService auditLogService
        +login(LoginRequest) AuthResponse
        +register(email, pass, nombre, apellido, tel) AuthResponse
        +refresh(refreshToken) Map
        +logout(Authentication) Map
    }

    class ApiController {
        -ProductService productService
        -UserService userService
        -CouponService couponService
        -NotificationService notificationService
        -ReservationService reservationService
        -PaymentService paymentService
        +searchProducts(keyword) List~Product~
        +addFavorite(productoId, auth) ResponseEntity
        +removeFavorite(productoId, auth)
        +validateCoupon(codigo, monto) Map
        +getNotifications(auth) List~Notification~
        +markAsRead(id) ResponseEntity
        +markAllAsRead(auth) ResponseEntity
        +createCotizacion(CotizacionRequest, auth) ResponseEntity
    }

    class PaymentController {
        -PaymentService paymentService
        +initPayment(reservaId, auth) ResponseEntity
        +getPaymentByReserva(reservaId) ResponseEntity
        +getMyPayments(auth) ResponseEntity
    }

    class PageController {
        -ProductService productService
        -CategoryRepository categoryRepository
        -ReservationService reservationService
        -ReviewService reviewService
        -UserService userService
        -ContactService contactService
        +index(Model) String
        +catalog(category, page, search, Model) String
        +productDetail(id, Model) String
        +cart() String
        +login() String
        +register() String
        +panel(auth, Model) String
        +contact() String
        +submitContact(ContactMessage) String
    }

    class ReviewController {
        -ReviewService reviewService
        +submitReview(productId, rating, comment, auth) String
    }
```

### 1.2 Admin Controllers

```mermaid
classDiagram
    class AdminDashboardController {
        -ReservationService reservationService
        -ProductService productService
        -UserService userService
        -PaymentService paymentService
        -AuditLogService auditLogService
        +dashboard(Model) String
        +logistica(Model) String
    }

    class AdminReservasController {
        -ReservationService reservationService
        -PdfService pdfService
        -NotificationService notificationService
        +reservas(Model) String
        +changeStatus(id, estado, nota, auth) String
        +downloadContract(id) ResponseEntity
        +downloadRoadmap() ResponseEntity
    }

    class AdminProductosController {
        -ProductService productService
        -CategoryRepository categoryRepository
        +mobiliarios(Model) String
        +saveProduct(Product, MultipartFile) String
    }

    class AdminCuponesController {
        -CouponService couponService
        +cupones(Model) String
        +saveCoupon(Coupon) String
        +deleteCoupon(id) String
    }

    class AdminPagosController {
        -PaymentService paymentService
        -ReservationService reservationService
        +getAllPayments() List~Payment~
        +confirmPayment(id, auth) ResponseEntity
    }

    class AdminMensajesController {
        -ContactService contactService
        +mensajes(Model) String
        +markAsRead(id) String
        +deleteMessage(id) String
    }

    class AdminExportController {
        -ExportService exportService
        -ProductService productService
        -ReservationService reservationService
        -UserService userService
        +exportProducts() ResponseEntity~byte[]~
        +exportReservations() ResponseEntity~byte[]~
        +exportUsers() ResponseEntity~byte[]~
    }
```

### 1.3 Services

```mermaid
classDiagram
    class UserService {
        <<Service>>
        -UserRepository userRepository
        -PasswordEncoder passwordEncoder
        -TenantContext tenantContext
        -EventPublisher eventPublisher
        -MetricsConfig metricsConfig
        +register(email, pass, nombre, apellido, tel) User
        +createAdmin(email, pass, nombre, apellido) User
        +findByEmail(email) Optional~User~
        +findById(id) Optional~User~
        +getAllUsers() List~User~
        +loadUserByUsername(email) UserDetails
        +save(User) User
        +deleteUser(id)
        +count() long
    }

    class ReservationService {
        <<Service>>
        -ReservationRepository reservationRepository
        -StatusHistoryRepository statusHistoryRepository
        -EventPublisher eventPublisher
        -MetricsConfig metricsConfig
        -NotificationService notificationService
        +save(Reservation) Reservation
        +getByIdOrThrow(id) Reservation
        +getByUsuarioId(userId) List
        +getActiveReservations() List
        +getPendingReservations() List
        +updateStatus(id, newStatus) Reservation
        +updateStatus(id, newStatus, usuario, nota) Reservation
        +getStatusHistory(reservaId) List~StatusHistory~
        +calculateTotalRevenue() BigDecimal
        +getRevenueByDay() Map
        +getStatusDistribution() Map
        +count() long
        +countByEstado(estado) long
    }

    class PaymentService {
        <<Service>>
        -PaymentRepository paymentRepository
        -ReservationService reservationService
        -NotificationService notificationService
        -EventPublisher eventPublisher
        -MetricsConfig metricsConfig
        +initPayment(reservaId, userId, metodo) Payment
        +confirmPayment(paymentId, ref, admin) Payment
        +failPayment(paymentId, reason, admin) Payment
        +getPaymentsByUser(userId) List
        +getPaymentByReserva(reservaId) Optional
        +getAllPayments() List
    }

    class ProductService {
        <<Service>>
        -ProductRepository productRepository
        -CategoryRepository categoryRepository
        +getAllProducts() List~Product~
        +getFeaturedProducts() List~Product~
        +getProductById(id) Optional~Product~
        +getProductByIdOrThrow(id) Product
        +getProductsByCategory(cat) List
        +getProductsPaginated(cat, page, size) Page
        +searchProducts(keyword) List
        +getAllCategories() List~Category~
        +countProducts() long
        +countCategories() long
    }

    class JwtService {
        <<Service>>
        -RefreshTokenRepository refreshTokenRepository
        -String jwtSecret
        +generateAccessToken(User, tenantId) String
        +generateRefreshToken(User, tenantId) RefreshToken
        +extractEmail(token) String
        +extractUserId(token) String
        +extractRole(token) String
        +extractTenantId(token) String
        +isTokenValid(token) boolean
        +isTokenValid(token, email) boolean
        +validateRefreshToken(token) RefreshToken
        +revokeAllUserTokens(userId)
        +getAccessTokenExpiration() long
    }

    class NotificationService {
        <<Service>>
        -NotificationRepository notificationRepository
        +notify(userId, titulo, msg, tipo, link) Notification
        +getRecentNotifications(userId) List
        +getUnreadNotifications(userId) List
        +countUnread(userId) long
        +markAsRead(id)
        +markAllAsRead(userId)
    }

    class AuditLogService {
        <<Service>>
        -AuditLogRepository auditLogRepository
        +log(usuario, accion, entidad, id, detalle)
        +logAdvanced(usuario, accion, entidad, id, detalle, severity, metadata)
        +logSecurity(usuario, accion, detalle)
        +getRecentLogs() List~AuditLog~
        +getLogsByUser(usuario) List
        +getLogsByAction(accion) List
    }

    UserService --> ReservationService : usado por
    ReservationService --> PaymentService : actualiza estado
    PaymentService --> ReservationService : confirma → ACTIVA
    PaymentService --> NotificationService : notifica usuario
    ReservationService --> NotificationService : notifica cambios
```

### 1.4 Domain Models

```mermaid
classDiagram
    class User {
        +String id
        +String tenantId
        +String email
        +String password
        +String nombre
        +String apellido
        +String telefono
        +RolUsuario role
        +boolean activo
        +String razonSuspension
        +LocalDateTime fechaInicioSuspension
        +LocalDateTime fechaFinSuspension
        +boolean suspensionPermanente
        +List~String~ favoritos
    }

    class Reservation {
        +String id
        +String tenantId
        +String usuarioId
        +String usuarioNombre
        +String usuarioEmail
        +List~ItemReserva~ items
        +LocalDate fechaInicio
        +LocalDate fechaFin
        +int diasAlquiler
        +BigDecimal subtotal
        +BigDecimal total
        +String estado
        +String metodoPago
        +String direccionEvento
        +String tipoEvento
        +LocalDateTime fechaCreacion
    }

    class ItemReserva {
        +String productoId
        +String productoNombre
        +String productoImagen
        +BigDecimal precioPorDia
        +int cantidad
        +BigDecimal subtotal
    }

    class Payment {
        +String id
        +String reservaId
        +String usuarioId
        +BigDecimal monto
        +String metodoPago
        +String estado
        +String referencia
        +LocalDateTime fechaPago
    }

    class Product {
        +String id
        +String nombre
        +String descripcion
        +BigDecimal precioPorDia
        +String imagenUrl
        +String categoriaId
        +String categoriaNombre
        +double calificacion
        +boolean disponible
        +int stock
        +EstadoMantenimiento estadoMantenimiento
    }

    class Category {
        +String id
        +String nombre
        +String icono
        +String slug
        +int cantidadProductos
    }

    class Review {
        +String id
        +String productId
        +String userId
        +String userName
        +int rating
        +String comment
        +LocalDateTime createdAt
    }

    class Coupon {
        +String id
        +String codigo
        +String tipo
        +BigDecimal valor
        +LocalDate validoDesde
        +LocalDate validoHasta
        +int usosMaximos
        +int usosActuales
        +BigDecimal montoMinimo
        +boolean activo
        +isValid() boolean
        +calcularDescuento(BigDecimal) BigDecimal
    }

    class Notification {
        +String id
        +String userId
        +String titulo
        +String mensaje
        +String tipo
        +boolean leida
        +LocalDateTime fecha
    }

    class Tenant {
        +String id
        +String slug
        +String nombre
        +String plan
        +boolean activo
        +String adminEmail
    }

    class StatusHistory {
        +String id
        +String reservaId
        +String estadoAnterior
        +String estadoNuevo
        +String usuarioAccion
        +String nota
        +LocalDateTime fecha
    }

    class AuditLog {
        +String id
        +String usuario
        +String accion
        +String entidad
        +String detalle
        +String severity
        +LocalDateTime fecha
    }

    User "1" --o "*" Reservation : crea
    User "1" --o "*" Review : escribe
    Reservation "1" *-- "*" ItemReserva : contiene
    Reservation "1" --o "0..1" Payment : tiene pago
    Reservation "1" --o "*" StatusHistory : historial
    Product "*" --o "1" Category : categoría
    Product "1" --o "*" Review : reseñas
    Tenant "1" --o "*" User : usuarios
```

### 1.5 Enumeraciones

```mermaid
classDiagram
    class EstadoReserva {
        <<enumeration>>
        PENDIENTE
        CONFIRMADA
        ACTIVA
        EN_CURSO
        COMPLETADA
        CANCELADA
    }

    class EstadoPago {
        <<enumeration>>
        PENDIENTE
        PAGADO
        FALLIDO
        REEMBOLSADO
    }

    class MetodoPago {
        <<enumeration>>
        TRANSFERENCIA
        NEQUI
        DAVIPLATA
        EFECTIVO
        TARJETA
    }

    class RolUsuario {
        <<enumeration>>
        USER
        MANAGER
        ADMIN
        SUPER_ADMIN
    }

    class EstadoMantenimiento {
        <<enumeration>>
        EXCELENTE
        BUENO
        REGULAR
        EN_REPARACION
    }
```

---

## 2. Diagrama de Secuencia — Reserva Completa

```mermaid
sequenceDiagram
    actor U as 👤 Usuario
    actor A as 👨‍💼 Admin
    participant AC as ApiController
    participant RS as ReservationService
    participant PS as PaymentService
    participant NS as NotificationService
    participant ES as EmailService
    participant AS as AuditLogService
    participant MC as MetricsConfig
    participant DB as 🍃 MongoDB

    rect rgb(230, 240, 255)
        Note over U, DB: FASE 1: Creación de Cotización
        U->>AC: POST /api/cotizacion {items, fechas, dirección}
        AC->>RS: save(reservation)
        RS->>RS: Validar fechaInicio < fechaFin
        RS->>RS: Validar fechas no pasadas
        RS->>RS: Calcular total = Σ(precio × días × cantidad)
        RS->>DB: reservationRepository.save(PENDIENTE)
        RS->>MC: reservationsCreated.increment()
        RS->>RS: publish(ReservationCreatedEvent)
        RS-->>AC: Reservation
        AC-->>U: 201 Created
    end

    rect rgb(255, 245, 230)
        Note over U, DB: FASE 2: Confirmación por Admin
        A->>RS: updateStatus(id, "CONFIRMADA", admin, "Aprobada")
        RS->>RS: Validar PENDIENTE → CONFIRMADA ✅
        RS->>DB: statusHistoryRepository.save(history)
        RS->>DB: reservationRepository.save()
        RS->>NS: notify(userId, "Reserva Confirmada")
        RS->>ES: sendStatusChange(email, id, "CONFIRMADA")
    end

    rect rgb(230, 255, 230)
        Note over U, DB: FASE 3: Pago
        U->>AC: POST /api/pagos/iniciar/{reservaId}
        AC->>PS: initPayment(reservaId, userId, "NEQUI")
        PS->>RS: getByIdOrThrow(reservaId)
        PS->>DB: paymentRepository.save(Payment PENDIENTE)
        PS->>NS: notify(userId, "Pago Registrado")
        PS-->>AC: Payment
        AC-->>U: 200 OK

        Note over A: Admin verifica comprobante
        A->>PS: confirmPayment(paymentId, "REF-12345", admin)
        PS->>DB: payment.estado = "PAGADO", fechaPago = now()
        PS->>RS: updateStatus(reservaId, "ACTIVA")
        RS->>DB: statusHistoryRepository.save()
        PS->>MC: paymentsCompleted.increment()
        PS->>MC: addRevenue(monto)
        PS->>NS: notify(userId, "Pago Confirmado ✅")
        PS->>ES: sendPaymentConfirmation(email, reservaId)
        PS->>PS: publish(PaymentCompletedEvent)
    end

    rect rgb(255, 250, 230)
        Note over U, DB: FASE 4: Logística
        A->>RS: updateStatus(id, "EN_CURSO", admin, "Entregado")
        RS->>DB: save histories
        RS->>NS: notify(userId, "Mobiliario entregado")

        Note over A: Después del evento...
        A->>RS: updateStatus(id, "COMPLETADA", admin, "Recogido")
        RS->>DB: save histories
        RS->>NS: notify(userId, "Reserva completada ✅")
    end
```

---

## 3. Diagrama de Secuencia — Autenticación

### 3.1 Registro

```mermaid
sequenceDiagram
    actor U as 👤 Usuario
    participant AC as AuthController
    participant US as UserService
    participant JS as JwtService
    participant PE as PasswordEncoder
    participant EP as EventPublisher
    participant ES as EmailService
    participant DB as 🍃 MongoDB

    U->>AC: POST /api/auth/register {email, pass, nombre, apellido, tel}
    AC->>US: findByEmail(email)
    US->>DB: userRepository.findByEmail()
    
    alt Email ya existe
        DB-->>US: User
        US-->>AC: present
        AC-->>U: 409 Conflict "El email ya está registrado"
    end

    DB-->>US: empty
    AC->>US: register(email, pass, nombre, apellido, tel)
    US->>PE: encode(password)
    PE-->>US: $2a$10$...hashed
    US->>DB: userRepository.save(User role=USER)
    US->>EP: publish(UserRegisteredEvent)
    EP->>ES: sendWelcomeEmail(email, nombre)
    US-->>AC: User

    AC->>JS: generateAccessToken(user, tenantId)
    JS-->>AC: JWT (1 hora)
    AC->>JS: generateRefreshToken(user, tenantId)
    JS->>DB: refreshTokenRepository.save(30 días)
    JS-->>AC: RefreshToken

    AC-->>U: AuthResponse {accessToken, refreshToken, user}
```

### 3.2 Login

```mermaid
sequenceDiagram
    actor U as 👤 Usuario
    participant RL as RateLimitFilter
    participant AC as AuthController
    participant US as UserService
    participant JS as JwtService
    participant PE as PasswordEncoder
    participant AS as AuditLogService
    participant DB as 🍃 MongoDB

    U->>RL: POST /api/auth/login {email, password}
    RL->>RL: Check bucket IP:login (≤5/min)
    
    alt Rate limit excedido
        RL->>AS: logSecurity("SISTEMA", "BRUTE_FORCE_BLOCKED", ...)
        RL-->>U: 429 Too Many Requests
    end

    RL->>AC: forward request
    AC->>US: findByEmail(email)
    
    alt Usuario no encontrado
        US-->>AC: empty
        AC-->>U: 401 "Credenciales inválidas"
    end

    AC->>PE: matches(rawPassword, user.password)
    
    alt Password incorrecta
        AC-->>U: 401 "Credenciales inválidas"
    end

    AC->>US: loadUserByUsername(email)
    Note over US: Verifica suspensión:<br/>permanente, temporal, activo

    alt Cuenta suspendida
        US-->>AC: throw AccountSuspendedException
        AC-->>U: 403 {reason, duration, permanent}
    end

    AC->>JS: generateAccessToken(user, tenantId)
    AC->>JS: generateRefreshToken(user, tenantId)
    JS->>DB: Revocar tokens anteriores
    JS->>DB: save(RefreshToken)
    
    AC->>AS: log(email, "LOGIN_SUCCESS", ...)
    AC-->>U: AuthResponse {accessToken, refreshToken, user, tenantId}
```

---

## 4. Diagrama de Secuencia — Pago

```mermaid
sequenceDiagram
    actor U as 👤 Usuario
    actor A as 👨‍💼 Admin
    participant PC as PaymentController
    participant AP as AdminPagosController
    participant PS as PaymentService
    participant RS as ReservationService
    participant NS as NotificationService
    participant MC as MetricsConfig
    participant DB as 🍃 MongoDB

    Note over U, DB: Iniciar Pago
    U->>PC: POST /api/pagos/iniciar/{reservaId}
    PC->>PS: initPayment(reservaId, userId, "TRANSFERENCIA")
    PS->>RS: getByIdOrThrow(reservaId)
    RS-->>PS: Reservation {total: 850000}
    PS->>DB: save(Payment {monto: 850000, estado: PENDIENTE})
    PS->>NS: notify(userId, "Pago registrado por $850,000")
    PS-->>U: Payment

    Note over A, DB: Admin Confirma Pago
    A->>AP: POST /admin/pagos/confirmar/{paymentId}
    AP->>PS: confirmPayment(paymentId, ref, admin)
    PS->>DB: payment.estado = PAGADO
    PS->>DB: payment.referencia = ref
    PS->>DB: payment.fechaPago = now()
    PS->>RS: updateStatus(reservaId, "ACTIVA")
    RS->>DB: statusHistory + reservation save
    PS->>MC: paymentsCompleted++
    PS->>MC: addRevenue(850000)
    PS->>NS: notify(userId, "Pago Confirmado ✅")
    PS-->>AP: Payment
    AP-->>A: redirect /admin/pagos
```

---

## 5. Diagrama de Actividad — Crear Cotización

```mermaid
flowchart TD
    A([🟢 Inicio]) --> B["Usuario selecciona<br/>productos del catálogo"]
    B --> C["Agrega al carrito<br/>(cantidades + fechas)"]
    C --> D["Llena formulario:<br/>tipo evento, dirección, notas"]
    D --> E{"¿Tiene cupón?"}
    
    E -->|Sí| F["POST /api/cupones/validar"]
    F --> G{"¿Cupón válido?"}
    G -->|Sí| H["Aplicar descuento<br/>al total"]
    G -->|No| I["Mostrar error:<br/>cupón inválido"]
    I --> D
    
    E -->|No| J["Calcular total sin descuento"]
    H --> J
    
    J --> K["POST /api/cotizacion<br/>(CotizacionRequest)"]
    K --> L{"¿Fechas válidas?"}
    
    L -->|No| M["400: Fecha inicio debe<br/>ser antes de fecha fin"]
    M --> D
    
    L -->|Sí| N["Crear Reservation<br/>estado = PENDIENTE"]
    N --> O["Guardar en MongoDB"]
    O --> P["Publicar<br/>ReservationCreatedEvent"]
    P --> Q["Incrementar métrica<br/>reservationsCreated"]
    Q --> R["Enviar notificación<br/>al usuario"]
    R --> S([🔴 Fin: Reserva creada])

    style A fill:#51cf66
    style S fill:#ff6b6b,color:#fff
    style N fill:#339af0,color:#fff
```

---

## 6. Diagrama de Actividad — Validar Cupón

```mermaid
flowchart TD
    A([🟢 Inicio]) --> B["Recibir código y monto total"]
    B --> C["Buscar cupón por código<br/>(case-insensitive)"]
    C --> D{"¿Cupón existe?"}
    
    D -->|No| E["Return: {válido: false,<br/>mensaje: 'Cupón no encontrado'}"]
    
    D -->|Sí| F{"coupon.isValid()?"}
    F --> F1{"¿Activo?"}
    F1 -->|No| G["Return: {válido: false,<br/>mensaje: 'Cupón inactivo'}"]
    
    F1 -->|Sí| F2{"¿Dentro de fechas<br/>válidas?"}
    F2 -->|No| H["Return: {válido: false,<br/>mensaje: 'Cupón expirado'}"]
    
    F2 -->|Sí| F3{"¿Usos disponibles?"}
    F3 -->|No| I["Return: {válido: false,<br/>mensaje: 'Usos agotados'}"]
    
    F3 -->|Sí| J{"monto ≥ montoMínimo?"}
    J -->|No| K["Return: {válido: false,<br/>mensaje: 'Monto mínimo no alcanzado'}"]
    
    J -->|Sí| L{"¿Tipo de descuento?"}
    L -->|PORCENTAJE| M["descuento = total × valor / 100"]
    L -->|MONTO_FIJO| N["descuento = min(valor, total)"]
    
    M & N --> O["montoFinal = total - descuento"]
    O --> P["Return: {válido: true,<br/>descuento, montoFinal,<br/>mensaje: 'Cupón aplicado'}"]

    style A fill:#51cf66
    style E fill:#ff6b6b,color:#fff
    style G fill:#ff6b6b,color:#fff
    style H fill:#ff6b6b,color:#fff
    style I fill:#ff6b6b,color:#fff
    style K fill:#ff6b6b,color:#fff
    style P fill:#51cf66,color:#fff
```

---

## 7. Diagrama de Casos de Uso

```mermaid
graph TB
    subgraph "Sistema Furent"
        subgraph "Públicas"
            UC1["Ver catálogo de<br/>mobiliarios"]
            UC2["Buscar productos"]
            UC3["Ver detalle de<br/>producto"]
            UC4["Registrarse"]
            UC5["Iniciar sesión"]
            UC6["Enviar mensaje<br/>de contacto"]
        end
        
        subgraph "Usuario Autenticado"
            UC7["Crear cotización /<br/>reserva"]
            UC8["Ver mis reservas"]
            UC9["Iniciar pago"]
            UC10["Validar cupón"]
            UC11["Agregar a favoritos"]
            UC12["Escribir reseña"]
            UC13["Ver notificaciones"]
            UC14["Marcar notificación<br/>como leída"]
            UC15["Cerrar sesión"]
            UC16["Recuperar contraseña"]
        end
        
        subgraph "Administrador"
            UC17["Ver dashboard KPIs"]
            UC18["Gestionar productos"]
            UC19["Confirmar/rechazar<br/>reservas"]
            UC20["Confirmar pagos"]
            UC21["Gestionar cupones"]
            UC22["Ver mensajes<br/>de contacto"]
            UC23["Exportar datos CSV"]
            UC24["Generar PDF contrato"]
            UC25["Ver hoja de ruta<br/>(logística)"]
            UC26["Ver logs de auditoría"]
        end
    end

    User["👤 Usuario"]
    Admin["👨‍💼 Admin"]

    User --> UC1 & UC2 & UC3 & UC4 & UC5 & UC6
    User --> UC7 & UC8 & UC9 & UC10 & UC11 & UC12 & UC13 & UC14 & UC15 & UC16
    Admin --> UC17 & UC18 & UC19 & UC20 & UC21 & UC22 & UC23 & UC24 & UC25 & UC26
```

---

## 8. Diagrama de Paquetes

```mermaid
graph TB
    subgraph "com.alquiler.furent"
        subgraph "controller"
            C1["AuthController"]
            C2["ApiController"]
            C3["PaymentController"]
            C4["PageController"]
            C5["ReviewController"]
            subgraph "controller.admin"
                CA1["AdminDashboardController"]
                CA2["AdminReservasController"]
                CA3["AdminProductosController"]
                CA4["AdminCuponesController"]
                CA5["AdminPagosController"]
                CA6["AdminMensajesController"]
                CA7["AdminExportController"]
                CA8["AdminModelAdvice"]
            end
        end

        subgraph "service"
            S1["UserService"]
            S2["ReservationService"]
            S3["PaymentService"]
            S4["ProductService"]
            S5["CouponService"]
            S6["NotificationService"]
            S7["ReviewService"]
            S8["JwtService"]
            S9["AuditLogService"]
            S10["ContactService"]
            S11["EmailService"]
            S12["PasswordResetService"]
            S13["PdfService"]
            S14["ExportService"]
            S15["TenantService"]
            S16["ReportingService"]
        end

        subgraph "model"
            M1["User"]
            M2["Reservation"]
            M3["Payment"]
            M4["Product"]
            M5["Category"]
            M6["Review"]
            M7["Coupon"]
            M8["Notification"]
            M9["AuditLog"]
            M10["Tenant"]
            M11["Email VO"]
            M12["Money VO"]
        end

        subgraph "repository"
            R1["18 MongoRepository<br/>interfaces"]
        end

        subgraph "config"
            CF1["SecurityConfig"]
            CF2["JwtAuthFilter"]
            CF3["RateLimitFilter"]
            CF4["TenantFilter"]
            CF5["CacheConfig"]
            CF6["MetricsConfig"]
        end

        subgraph "exception"
            EX1["ResourceNotFoundException"]
            EX2["ApiExceptionHandler"]
            EX3["GlobalExceptionHandler"]
        end

        subgraph "event"
            EV1["7 Domain Events"]
            EV2["EventPublisher"]
            EV3["FurentEventListener"]
        end

        subgraph "dto"
            D1["AuthResponse"]
            D2["CotizacionRequest"]
            D3["ProductResponse"]
            D4["ReservationResponse"]
        end
    end

    controller --> service
    service --> repository
    service --> model
    repository --> model
    controller --> dto
    service --> event
    config --> service
```

---

## 9. Diagrama ER (Entidad-Relación)

```mermaid
erDiagram
    TENANT ||--o{ USER : "tiene"
    TENANT ||--o{ PRODUCT : "contiene"
    TENANT ||--o{ CATEGORY : "tiene"
    
    USER ||--o{ RESERVATION : "crea"
    USER ||--o{ PAYMENT : "realiza"
    USER ||--o{ REVIEW : "escribe"
    USER ||--o{ NOTIFICATION : "recibe"
    USER ||--o{ REFRESH_TOKEN : "posee"
    USER ||--o{ PASSWORD_RESET_TOKEN : "solicita"
    USER ||--o{ SESSION : "tiene"
    USER ||--o{ AUDIT_LOG : "genera"
    
    RESERVATION ||--|{ ITEM_RESERVA : "contiene"
    RESERVATION ||--o| PAYMENT : "tiene pago"
    RESERVATION ||--o{ STATUS_HISTORY : "historial"
    
    PRODUCT }o--|| CATEGORY : "pertenece a"
    PRODUCT ||--o{ REVIEW : "recibe"
    PRODUCT ||--o{ ITEM_RESERVA : "referenciado en"
    
    COUPON ||--o{ RESERVATION : "se aplica a"
    
    USER {
        string id PK
        string tenantId FK
        string email UK
        string password
        string nombre
        string apellido
        string role
        boolean activo
        datetime fechaCreacion
    }
    
    RESERVATION {
        string id PK
        string tenantId FK
        string usuarioId FK
        date fechaInicio
        date fechaFin
        decimal subtotal
        decimal total
        string estado
        string direccionEvento
        datetime fechaCreacion
    }
    
    PAYMENT {
        string id PK
        string tenantId FK
        string reservaId FK
        string usuarioId FK
        decimal monto
        string estado
        string referencia
        datetime fechaPago
    }
    
    PRODUCT {
        string id PK
        string tenantId FK
        string nombre
        decimal precioPorDia
        string categoriaId FK
        double calificacion
        int stock
        boolean disponible
    }
    
    REVIEW {
        string id PK
        string productId FK
        string userId FK
        int rating
        string comment
        datetime createdAt
    }
    
    COUPON {
        string id PK
        string codigo UK
        string tipo
        decimal valor
        date validoDesde
        date validoHasta
        int usosMaximos
        int usosActuales
    }
    
    NOTIFICATION {
        string id PK
        string userId FK
        string titulo
        string mensaje
        string tipo
        boolean leida
        datetime fecha
    }
    
    TENANT {
        string id PK
        string slug UK
        string nombre
        string plan
        boolean activo
        string adminEmail
    }
```

---

> 📝 **Generado automáticamente** — Furent SaaS Platform v1.0  
> Todos los diagramas usan notación **Mermaid** — renderizables en GitHub, GitLab, VS Code y cualquier visor Markdown compatible.
