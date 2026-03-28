# Design Document: Furent Professional Improvements

## Overview

Este documento describe el diseño técnico completo para implementar las mejoras profesionales críticas de Furent. El sistema es una plataforma SaaS multi-tenant de alquiler de mobiliarios para eventos construida con Spring Boot y MongoDB.

### Objetivos del Diseño

1. Corregir 5 bugs críticos que afectan la funcionalidad core
2. Implementar mejoras de seguridad esenciales (CSRF, validación de uploads, headers HTTP)
3. Completar funcionalidades MVP: pagos, notificaciones email, recuperación de contraseña
4. Implementar CRUD administrativo completo para usuarios y categorías
5. Agregar sistema de cupones y formulario de contacto funcional

### Alcance

El diseño cubre:
- Corrección de bugs en ProductService y validaciones
- Configuración de seguridad mejorada (CSRF, headers, validación de entradas)
- Sistema de pagos end-to-end con estados y transiciones
- Servicio de notificaciones por email con templates HTML
- Sistema de recuperación de contraseña con tokens temporales
- CRUD completo de usuarios con suspensión y auditoría
- CRUD completo de categorías con validación de productos asociados
- Sistema de cupones con validación y aplicación de descuentos
- Formulario de contacto con gestión de mensajes

### Restricciones Técnicas

- Base de datos: MongoDB (única base de datos permitida)
- Framework: Spring Boot 3.x
- Seguridad: Spring Security con JWT para API REST
- Templates: Thymeleaf para vistas web
- Cache: Redis para optimización de consultas
- Arquitectura: Multi-tenant con aislamiento por tenantId


## Architecture

### High-Level Architecture

El sistema mantiene una arquitectura monolítica modular con separación clara de responsabilidades, preparada para evolucionar hacia microservicios.

```mermaid
flowchart TB
    subgraph Client["Cliente"]
        Web[Navegador Web]
        Mobile[App Móvil]
    end
    
    subgraph Gateway["API Gateway / Load Balancer"]
        LB[Nginx / Spring Cloud Gateway]
    end
    
    subgraph Application["Spring Boot Application"]
        subgraph Controllers["Controllers Layer"]
            WebCtrl[Web Controllers<br/>Thymeleaf]
            APICtrl[REST API Controllers<br/>JSON]
            AdminCtrl[Admin Controllers]
        end
        
        subgraph Security["Security Layer"]
            CSRF[CSRF Protection]
            JWT[JWT Auth Filter]
            RateLimit[Rate Limiting]
        end
        
        subgraph Services["Business Logic Layer"]
            ProductSvc[ProductService]
            ReservationSvc[ReservationService]
            PaymentSvc[PaymentService]
            EmailSvc[EmailService]
            UserSvc[UserService]
            CouponSvc[CouponService]
            ContactSvc[ContactService]
            PwdResetSvc[PasswordResetService]
        end
        
        subgraph Events["Event System"]
            EventPub[Event Publisher]
            EventBus[Event Bus]
            Listeners[Event Listeners]
        end
    end
    
    subgraph Data["Data Layer"]
        MongoDB[(MongoDB<br/>Multi-tenant)]
        Redis[(Redis<br/>Cache)]
    end
    
    subgraph External["External Services"]
        SMTP[SMTP Server<br/>Gmail]
        Storage[File Storage<br/>Local/S3]
    end
    
    Web --> LB
    Mobile --> LB
    LB --> WebCtrl
    LB --> APICtrl
    
    WebCtrl --> CSRF
    APICtrl --> JWT
    
    CSRF --> Services
    JWT --> Services
    RateLimit --> Services
    
    AdminCtrl --> Services
    
    Services --> EventPub
    EventPub --> EventBus
    EventBus --> Listeners
    
    Services --> MongoDB
    Services --> Redis
    
    EmailSvc --> SMTP
    Services --> Storage
```

### Component Diagram

```mermaid
flowchart LR
    subgraph Frontend["Frontend Components"]
        CatalogUI[Catálogo UI]
        CartUI[Carrito UI]
        CheckoutUI[Checkout UI]
        AdminUI[Admin Dashboard]
    end
    
    subgraph Backend["Backend Services"]
        ProductAPI[Product API]
        ReservationAPI[Reservation API]
        PaymentAPI[Payment API]
        UserAPI[User API]
        EmailAPI[Email API]
    end
    
    subgraph Data["Data Stores"]
        ProductDB[(Products)]
        ReservationDB[(Reservations)]
        PaymentDB[(Payments)]
        UserDB[(Users)]
        TokenDB[(Reset Tokens)]
        ContactDB[(Contact Messages)]
    end
    
    CatalogUI --> ProductAPI
    CartUI --> ReservationAPI
    CheckoutUI --> PaymentAPI
    AdminUI --> UserAPI
    
    ProductAPI --> ProductDB
    ReservationAPI --> ReservationDB
    PaymentAPI --> PaymentDB
    UserAPI --> UserDB
    EmailAPI --> SMTP[SMTP]
    
    PaymentAPI --> EmailAPI
    ReservationAPI --> EmailAPI
    UserAPI --> TokenDB
    ContactDB --> AdminUI
```


### Sequence Diagrams

#### Payment Flow

```mermaid
sequenceDiagram
    actor User
    participant UI as Web UI
    participant API as Payment API
    participant PaymentSvc as PaymentService
    participant ReservationSvc as ReservationService
    participant EmailSvc as EmailService
    participant DB as MongoDB
    participant Metrics as Metrics
    
    User->>UI: Iniciar Pago
    UI->>API: POST /api/pagos/iniciar/{reservaId}
    API->>ReservationSvc: getByIdOrThrow(reservaId)
    ReservationSvc->>DB: findById(reservaId)
    DB-->>ReservationSvc: Reservation
    
    ReservationSvc-->>API: Reservation (CONFIRMADA)
    API->>PaymentSvc: initPayment(reservaId, userId, metodo)
    PaymentSvc->>PaymentSvc: generateReference()
    PaymentSvc->>DB: save(Payment PENDIENTE)
    DB-->>PaymentSvc: Payment saved
    
    PaymentSvc->>Metrics: increment(paymentsCreated)
    PaymentSvc-->>API: Payment
    API-->>UI: Payment details
    UI-->>User: Mostrar referencia de pago
    
    Note over User,Admin: Usuario realiza pago offline
    
    actor Admin
    Admin->>UI: Confirmar Pago
    UI->>API: POST /admin/pagos/{id}/confirmar
    API->>PaymentSvc: confirmPayment(id, referencia, admin)
    PaymentSvc->>DB: update Payment (PAGADO)
    PaymentSvc->>ReservationSvc: updateStatus(reservaId, ENTREGADA)
    ReservationSvc->>DB: update Reservation
    
    PaymentSvc->>Metrics: increment(paymentsCompleted)
    PaymentSvc->>Metrics: addRevenue(amount)
    PaymentSvc->>EmailSvc: sendPaymentConfirmation(payment)
    EmailSvc-->>SMTP: Send email async
    
    PaymentSvc-->>API: Payment confirmed
    API-->>UI: Success
    UI-->>Admin: Confirmación
```

#### Email Notification Flow

```mermaid
sequenceDiagram
    participant Service as Business Service
    participant EmailSvc as EmailService
    participant Template as Thymeleaf Engine
    participant SMTP as Gmail SMTP
    participant User
    
    Service->>EmailSvc: sendReservationConfirmation(reservation)
    Note over EmailSvc: @Async execution
    
    EmailSvc->>EmailSvc: getUserById(userId)
    EmailSvc->>Template: process("email/confirmacion-reserva", context)
    Template-->>EmailSvc: HTML content
    
    EmailSvc->>EmailSvc: createMimeMessage()
    EmailSvc->>SMTP: send(message)
    
    alt Email sent successfully
        SMTP-->>EmailSvc: Success
        EmailSvc->>EmailSvc: log.info("Email enviado")
    else Email failed
        SMTP-->>EmailSvc: Error
        EmailSvc->>EmailSvc: log.error("Error enviando email")
        Note over EmailSvc: No interrumpe flujo principal
    end
    
    SMTP->>User: Email delivered
```

#### Password Reset Flow

```mermaid
sequenceDiagram
    actor User
    participant UI as Web UI
    participant API as Auth Controller
    participant PwdSvc as PasswordResetService
    participant UserSvc as UserService
    participant EmailSvc as EmailService
    participant DB as MongoDB
    
    User->>UI: Solicitar reset
    UI->>API: POST /password-reset (email)
    API->>UserSvc: findByEmail(email)
    
    alt Email exists
        UserSvc-->>API: User found
        API->>PwdSvc: createToken(userId)
        PwdSvc->>DB: invalidate old tokens
        PwdSvc->>DB: save new token (expires +1h)
        DB-->>PwdSvc: Token saved
        
        PwdSvc-->>API: Token
        API->>EmailSvc: sendPasswordResetToken(user, token)
        EmailSvc-->>SMTP: Send reset link
        API-->>UI: "Revisa tu email"
    else Email not found
        UserSvc-->>API: Not found
        Note over API: No revelar si existe
        API-->>UI: "Revisa tu email"
    end
    
    User->>Email: Click reset link
    Email->>UI: GET /password-reset/{token}
    UI->>API: Validate token
    API->>PwdSvc: validateToken(token)
    PwdSvc->>DB: findByToken(token)
    
    alt Token valid
        DB-->>PwdSvc: Token (not used, not expired)
        PwdSvc-->>API: Valid
        API-->>UI: Show reset form
        
        User->>UI: Submit new password
        UI->>API: POST /password-reset/confirm
        API->>PwdSvc: resetPassword(token, newPassword)
        PwdSvc->>UserSvc: updatePassword(userId, encrypted)
        PwdSvc->>DB: mark token as used
        PwdSvc-->>API: Success
        API-->>UI: "Contraseña cambiada"
    else Token invalid
        DB-->>PwdSvc: Token (used or expired)
        PwdSvc-->>API: InvalidOperationException
        API-->>UI: "Token inválido"
    end
```


### Payment State Machine

```mermaid
stateDiagram-v2
    [*] --> PENDIENTE: initPayment()
    
    PENDIENTE --> PAGADO: confirmPayment()
    PENDIENTE --> FALLIDO: failPayment()
    
    PAGADO --> [*]
    FALLIDO --> [*]
    
    note right of PENDIENTE
        Usuario inicia pago
        Genera referencia PAY-XXXXXXXX
        Espera confirmación admin
    end note
    
    note right of PAGADO
        Admin confirma pago
        Actualiza fechaPago
        Cambia reserva a ENTREGADA
        Envía email confirmación
        Incrementa métricas
    end note
    
    note right of FALLIDO
        Admin rechaza pago
        Registra razón
        Notifica usuario
        Incrementa métricas fallidos
    end note
```

### Reservation State Machine (Existing)

```mermaid
stateDiagram-v2
    [*] --> PENDIENTE: create()
    
    PENDIENTE --> CONFIRMADA: confirm()
    PENDIENTE --> CANCELADA: cancel()
    
    CONFIRMADA --> ENTREGADA: deliver() / confirmPayment()
    CONFIRMADA --> CANCELADA: cancel()
    
    ENTREGADA --> COMPLETADA: complete()
    ENTREGADA --> CANCELADA: cancel()
    
    COMPLETADA --> [*]
    CANCELADA --> [*]
    
    note right of CONFIRMADA
        Admin confirma disponibilidad
        Productos reservados
    end note
    
    note right of ENTREGADA
        Pago confirmado
        Productos entregados
        Evento en curso
    end note
```


## Components and Interfaces

### New Services

#### PaymentService

Gestiona el ciclo de vida completo de pagos.

```java
@Service
public class PaymentService {
    
    /**
     * Inicia un nuevo pago para una reserva confirmada.
     * 
     * @param reservaId ID de la reserva (debe estar en estado CONFIRMADA)
     * @param userId ID del usuario que realiza el pago
     * @param metodo Método de pago (EFECTIVO, TRANSFERENCIA, TARJETA)
     * @return Payment creado con estado PENDIENTE
     * @throws ResourceNotFoundException si la reserva no existe
     * @throws InvalidOperationException si la reserva no está CONFIRMADA
     */
    Payment initPayment(String reservaId, String userId, MetodoPago metodo);
    
    /**
     * Confirma un pago pendiente (acción de admin).
     * 
     * @param paymentId ID del pago
     * @param referencia Referencia de pago externa (comprobante)
     * @param admin Email del admin que confirma
     * @return Payment actualizado con estado PAGADO
     * @throws ResourceNotFoundException si el pago no existe
     * @throws InvalidOperationException si el pago ya fue procesado
     */
    Payment confirmPayment(String paymentId, String referencia, String admin);
    
    /**
     * Rechaza un pago pendiente (acción de admin).
     * 
     * @param paymentId ID del pago
     * @param reason Razón del rechazo
     * @param admin Email del admin que rechaza
     * @return Payment actualizado con estado FALLIDO
     * @throws ResourceNotFoundException si el pago no existe
     * @throws InvalidOperationException si el pago ya fue procesado
     */
    Payment failPayment(String paymentId, String reason, String admin);
    
    /**
     * Obtiene un pago por ID o lanza excepción.
     */
    Payment getByIdOrThrow(String id);
    
    /**
     * Obtiene todos los pagos de un usuario.
     */
    List<Payment> getByUsuarioId(String userId);
    
    /**
     * Genera una referencia única de pago.
     * Formato: PAY-XXXXXXXX (8 caracteres alfanuméricos)
     */
    String generateReference();
}
```

#### EmailService

Envía notificaciones por email de forma asíncrona.

```java
@Service
public class EmailService {
    
    /**
     * Envía email de bienvenida a nuevo usuario.
     * Ejecuta de forma asíncrona (@Async).
     * 
     * @param user Usuario registrado
     */
    @Async
    void sendWelcomeEmail(User user);
    
    /**
     * Envía email de confirmación de reserva.
     * Incluye detalles de productos, fechas y total.
     * 
     * @param reservation Reserva confirmada
     */
    @Async
    void sendReservationConfirmation(Reservation reservation);
    
    /**
     * Envía email notificando cambio de estado de reserva.
     * 
     * @param reservation Reserva actualizada
     * @param oldStatus Estado anterior
     */
    @Async
    void sendStatusChange(Reservation reservation, String oldStatus);
    
    /**
     * Envía email con token de recuperación de contraseña.
     * Incluye link con token válido por 1 hora.
     * 
     * @param user Usuario que solicita reset
     * @param token Token UUID generado
     */
    @Async
    void sendPasswordResetToken(User user, String token);
    
    /**
     * Envía recibo de pago confirmado.
     * Formato HTML con detalles de pago y reserva.
     * 
     * @param payment Pago confirmado
     */
    @Async
    void sendPaymentConfirmation(Payment payment);
    
    /**
     * Envía email HTML usando template Thymeleaf.
     * Maneja errores sin interrumpir flujo principal.
     * 
     * @param to Email destinatario
     * @param subject Asunto del email
     * @param html Contenido HTML renderizado
     */
    void sendHtmlEmail(String to, String subject, String html);
}
```

#### PasswordResetService

Gestiona tokens de recuperación de contraseña.

```java
@Service
public class PasswordResetService {
    
    /**
     * Crea un token de reset de contraseña.
     * Invalida todos los tokens anteriores del usuario.
     * Token expira en 1 hora.
     * 
     * @param userId ID del usuario
     * @return Token creado
     */
    PasswordResetToken createToken(String userId);
    
    /**
     * Resetea la contraseña usando un token válido.
     * Marca el token como usado.
     * Encripta la nueva contraseña con BCrypt.
     * 
     * @param token Token UUID
     * @param newPassword Nueva contraseña en texto plano
     * @throws ResourceNotFoundException si el token no existe
     * @throws InvalidOperationException si el token expiró o ya fue usado
     */
    void resetPassword(String token, String newPassword);
    
    /**
     * Valida si un token es válido (no usado y no expirado).
     * 
     * @param token Token UUID
     * @return true si es válido
     */
    boolean validateToken(String token);
}
```

#### ContactService

Gestiona mensajes del formulario de contacto.

```java
@Service
public class ContactService {
    
    /**
     * Guarda un nuevo mensaje de contacto.
     * Estado inicial: NO_LEIDO
     * 
     * @param message Mensaje a guardar
     * @return Mensaje guardado
     */
    ContactMessage save(ContactMessage message);
    
    /**
     * Obtiene todos los mensajes no leídos.
     * 
     * @return Lista de mensajes NO_LEIDO
     */
    List<ContactMessage> findUnread();
    
    /**
     * Marca un mensaje como leído.
     * 
     * @param id ID del mensaje
     * @throws ResourceNotFoundException si el mensaje no existe
     */
    void markAsRead(String id);
    
    /**
     * Cuenta mensajes no leídos.
     * 
     * @return Número de mensajes NO_LEIDO
     */
    long countUnread();
    
    /**
     * Obtiene mensajes paginados con filtros.
     * 
     * @param estado Filtro por estado (opcional)
     * @param page Número de página
     * @param size Tamaño de página
     * @return Página de mensajes
     */
    Page<ContactMessage> findAll(String estado, int page, int size);
}
```


### Modified Services

#### ProductService (Bug Fixes)

```java
@Service
public class ProductService {
    
    /**
     * Obtiene productos relacionados por categoría.
     * 
     * FIX: Usar categoriaNombre en lugar de getCategory()
     * 
     * @param productId ID del producto actual
     * @param category Nombre de la categoría
     * @return Lista de hasta 8 productos relacionados
     */
    List<Product> getRelatedProducts(String productId, String category) {
        // FIX: Filtrar por categoriaNombre
        List<Product> sameCategory = productRepository.findByCategoriaNombre(category)
            .stream()
            .filter(p -> !p.getId().equals(productId))
            .collect(Collectors.toCollection(ArrayList::new));
        
        if (sameCategory.size() >= 8) {
            return sameCategory.stream().limit(8).toList();
        }
        
        // Completar con productos de otras categorías
        List<Product> allProducts = productRepository.findAll();
        List<Product> others = allProducts.stream()
            .filter(p -> !p.getId().equals(productId) && 
                        !p.getCategoriaNombre().equals(category))
            .limit(8 - sameCategory.size())
            .toList();
        
        sameCategory.addAll(others);
        return sameCategory;
    }
}
```

#### CouponService

```java
@Service
public class CouponService {
    
    /**
     * Valida un cupón para aplicar a una reserva.
     * Verifica: existencia, vigencia, límite de usos.
     * 
     * @param codigo Código del cupón
     * @return Cupón válido
     * @throws InvalidOperationException si el cupón es inválido
     */
    Coupon validateCoupon(String codigo);
    
    /**
     * Aplica un descuento de cupón al total.
     * 
     * @param coupon Cupón validado
     * @param total Total original
     * @return Total con descuento aplicado
     */
    BigDecimal applyDiscount(Coupon coupon, BigDecimal total);
    
    /**
     * Incrementa el contador de usos de un cupón.
     * 
     * @param couponId ID del cupón
     * @throws InvalidOperationException si alcanzó el límite
     */
    void incrementUsage(String couponId);
    
    /**
     * Algoritmo de aplicación de descuento:
     * 
     * totalConDescuento = total * (1 - descuento/100)
     * 
     * Ejemplo:
     * - Total: $1000
     * - Descuento: 15%
     * - Resultado: $1000 * (1 - 0.15) = $850
     */
}
```

### Modified Controllers

#### AdminProductosController (Bug Fixes)

```java
@Controller
@RequestMapping("/admin/productos")
public class AdminProductosController {
    
    /**
     * Calcula disponibilidad de un producto.
     * 
     * FIX: Considerar disponible si estado NO es "EN_REPARACION"
     * 
     * @param product Producto a evaluar
     * @return true si está disponible
     */
    private boolean calculateAvailability(Product product) {
        int stock = product.getStock();
        String estadoMantenimiento = product.getEstadoMantenimiento();
        
        // FIX: Disponible si tiene stock Y NO está en reparación
        return stock > 0 && !"EN_REPARACION".equals(estadoMantenimiento);
    }
    
    /**
     * Valida archivo de imagen antes de upload.
     * 
     * FIX: Validar Content-Type y tamaño
     * 
     * @param file Archivo subido
     * @throws InvalidOperationException si el archivo es inválido
     */
    private void validateImageFile(MultipartFile file) {
        // Validar Content-Type
        Set<String> allowedTypes = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
        );
        
        if (!allowedTypes.contains(file.getContentType())) {
            throw new InvalidOperationException(
                "Solo se permiten imágenes (JPG, PNG, WebP, GIF)"
            );
        }
        
        // Validar tamaño máximo 5MB
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new InvalidOperationException(
                "La imagen no puede superar 5MB"
            );
        }
    }
}
```

#### ApiController (Bug Fixes)

```java
@RestController
@RequestMapping("/api")
public class ApiController {
    
    /**
     * Crea una cotización/reserva.
     * 
     * FIX: Validar fechas antes de crear
     * 
     * @param request Datos de la cotización
     * @return Reserva creada
     */
    @PostMapping("/cotizacion")
    public ResponseEntity<?> createQuote(@Valid @RequestBody CotizacionRequest request) {
        // FIX: Validar que fechaFin no sea anterior a fechaInicio
        if (request.getFechaFin().isBefore(request.getFechaInicio())) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "La fecha de fin no puede ser anterior a la fecha de inicio"
            ));
        }
        
        // FIX: Validar que fechaInicio no sea en el pasado
        if (request.getFechaInicio().isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "La fecha de inicio no puede ser en el pasado"
            ));
        }
        
        // Crear reserva...
    }
}
```

### New Controllers

#### PaymentController

```java
@RestController
@RequestMapping("/api/pagos")
public class PaymentController {
    
    @PostMapping("/iniciar/{reservaId}")
    public ResponseEntity<?> initPayment(
            @PathVariable String reservaId,
            @RequestParam MetodoPago metodo,
            Principal principal);
    
    @GetMapping("/mis-pagos")
    public ResponseEntity<List<Payment>> myPayments(Principal principal);
    
    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(@PathVariable String id);
}

@Controller
@RequestMapping("/admin/pagos")
public class AdminPagosController {
    
    @GetMapping
    public String listPayments(Model model);
    
    @PostMapping("/{id}/confirmar")
    public String confirmPayment(
            @PathVariable String id,
            @RequestParam String referencia,
            Principal principal,
            RedirectAttributes redirectAttributes);
    
    @PostMapping("/{id}/rechazar")
    public String failPayment(
            @PathVariable String id,
            @RequestParam String razon,
            Principal principal,
            RedirectAttributes redirectAttributes);
}
```

#### PasswordResetController

```java
@Controller
public class PasswordResetController {
    
    @GetMapping("/password-reset")
    public String passwordResetForm();
    
    @PostMapping("/password-reset")
    public String requestPasswordReset(
            @RequestParam String email,
            RedirectAttributes redirectAttributes);
    
    @GetMapping("/password-reset/{token}")
    public String passwordResetConfirmForm(
            @PathVariable String token,
            Model model);
    
    @PostMapping("/password-reset/confirm")
    public String confirmPasswordReset(
            @RequestParam String token,
            @RequestParam String password,
            @RequestParam String passwordConfirm,
            RedirectAttributes redirectAttributes);
}
```

#### ContactController

```java
@Controller
public class ContactController {
    
    @GetMapping("/contacto")
    public String contactForm(Model model);
    
    @PostMapping("/contacto")
    public String submitContact(
            @Valid @ModelAttribute ContactMessage message,
            BindingResult result,
            RedirectAttributes redirectAttributes);
}

@Controller
@RequestMapping("/admin/mensajes")
public class AdminContactController {
    
    @GetMapping
    public String listMessages(
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            Model model);
    
    @PostMapping("/{id}/leer")
    public String markAsRead(
            @PathVariable String id,
            RedirectAttributes redirectAttributes);
}
```


## Data Models

### New Models

#### Payment

```java
@Document(collection = "payments")
@CompoundIndexes({
    @CompoundIndex(name = "idx_tenant_usuario", def = "{'tenantId': 1, 'usuarioId': 1}"),
    @CompoundIndex(name = "idx_tenant_estado", def = "{'tenantId': 1, 'estado': 1}"),
    @CompoundIndex(name = "idx_reserva", def = "{'reservaId': 1}")
})
public class Payment {
    
    @Id
    private String id;
    
    @Indexed
    private String tenantId;
    
    @Indexed
    private String reservaId;
    
    @Indexed
    private String usuarioId;
    
    private BigDecimal monto;
    
    private String metodoPago; // EFECTIVO, TRANSFERENCIA, TARJETA
    
    @Indexed
    private String estado; // PENDIENTE, PAGADO, FALLIDO
    
    private String referencia; // PAY-XXXXXXXX
    
    private LocalDateTime fechaCreacion;
    
    private LocalDateTime fechaPago; // Cuando se confirma
    
    // Getters & Setters
}
```

#### PasswordResetToken

```java
@Document(collection = "password_reset_tokens")
@CompoundIndexes({
    @CompoundIndex(name = "idx_tenant_user", def = "{'tenantId': 1, 'userId': 1}"),
    @CompoundIndex(name = "idx_token", def = "{'token': 1}", unique = true)
})
public class PasswordResetToken {
    
    @Id
    private String id;
    
    @Indexed
    private String tenantId;
    
    @Indexed
    private String userId;
    
    @Indexed
    private String token; // UUID
    
    private LocalDateTime expiresAt; // +1 hora desde creación
    
    private boolean used;
    
    private LocalDateTime createdAt;
    
    /**
     * Verifica si el token ha expirado.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Verifica si el token es válido (no usado y no expirado).
     */
    public boolean isValid() {
        return !used && !isExpired();
    }
    
    // Getters & Setters
}
```

#### ContactMessage

```java
@Document(collection = "contact_messages")
@CompoundIndexes({
    @CompoundIndex(name = "idx_tenant_leido", def = "{'tenantId': 1, 'leido': 1}"),
    @CompoundIndex(name = "idx_fecha", def = "{'fechaCreacion': -1}")
})
public class ContactMessage {
    
    @Id
    private String id;
    
    @Indexed
    private String tenantId;
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String nombre;
    
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String email;
    
    @Size(max = 20, message = "Máximo 20 caracteres")
    private String telefono;
    
    @Size(max = 200, message = "Máximo 200 caracteres")
    private String asunto;
    
    @NotBlank(message = "El mensaje es obligatorio")
    @Size(max = 2000, message = "Máximo 2000 caracteres")
    private String mensaje;
    
    private boolean leido;
    
    @Indexed
    private LocalDateTime fechaCreacion;
    
    public ContactMessage() {
        this.fechaCreacion = LocalDateTime.now();
        this.leido = false;
    }
    
    // Getters & Setters
}
```

#### Coupon (Existing, Extended)

```java
@Document(collection = "coupons")
@CompoundIndexes({
    @CompoundIndex(name = "idx_tenant_codigo", def = "{'tenantId': 1, 'codigo': 1}", unique = true),
    @CompoundIndex(name = "idx_tenant_activo", def = "{'tenantId': 1, 'activo': 1}")
})
public class Coupon {
    
    @Id
    private String id;
    
    @Indexed
    private String tenantId;
    
    @Indexed
    private String codigo; // Ej: VERANO2024
    
    private int descuento; // Porcentaje: 5, 10, 15, etc.
    
    private LocalDate fechaInicio;
    
    private LocalDate fechaFin;
    
    private int limiteUsos; // Máximo número de usos
    
    private int usosActuales; // Contador de usos
    
    private boolean activo;
    
    private LocalDateTime fechaCreacion;
    
    /**
     * Verifica si el cupón está vigente.
     */
    public boolean isVigente() {
        LocalDate now = LocalDate.now();
        return !now.isBefore(fechaInicio) && !now.isAfter(fechaFin);
    }
    
    /**
     * Verifica si el cupón alcanzó el límite de usos.
     */
    public boolean hasReachedLimit() {
        return usosActuales >= limiteUsos;
    }
    
    /**
     * Verifica si el cupón es válido para usar.
     */
    public boolean isValid() {
        return activo && isVigente() && !hasReachedLimit();
    }
    
    // Getters & Setters
}
```

### Modified Models

#### User (Extended)

```java
@Document(collection = "users")
public class User {
    
    // Existing fields...
    
    @Indexed
    private String estado; // ACTIVO, SUSPENDIDO_TEMPORAL, ELIMINADO
    
    private LocalDateTime fechaSuspension;
    
    private String razonSuspension;
    
    private boolean deleted; // Soft delete flag
    
    /**
     * Verifica si el usuario está suspendido.
     */
    public boolean isSuspended() {
        return "SUSPENDIDO_TEMPORAL".equals(estado);
    }
    
    /**
     * Verifica si el usuario está activo.
     */
    public boolean isActive() {
        return "ACTIVO".equals(estado) && !deleted;
    }
    
    // Getters & Setters
}
```

### DTOs

#### CotizacionRequest

```java
@Data
public class CotizacionRequest {
    
    @NotBlank(message = "El tipo de evento es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ0-9\\s]+$", 
             message = "Solo letras, números y espacios")
    private String tipoEvento;
    
    @Min(value = 1, message = "Debe haber al menos 1 invitado")
    @Max(value = 10000, message = "Máximo 10,000 invitados")
    private int invitados;
    
    @NotNull(message = "La fecha de inicio es obligatoria")
    @FutureOrPresent(message = "La fecha debe ser hoy o posterior")
    private LocalDate fechaInicio;
    
    @NotNull(message = "La fecha de fin es obligatoria")
    @Future(message = "La fecha de fin debe ser futura")
    private LocalDate fechaFin;
    
    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 500, message = "Máximo 500 caracteres")
    private String direccion;
    
    @Size(max = 1000, message = "Máximo 1000 caracteres")
    private String notas;
    
    @NotEmpty(message = "Debe seleccionar al menos un producto")
    @Valid
    private List<CartItem> items;
    
    private String codigoCupon; // Opcional
    
    /**
     * Valida que fechaFin no sea anterior a fechaInicio.
     */
    @AssertTrue(message = "La fecha de fin debe ser posterior a la de inicio")
    public boolean isFechaFinValid() {
        if (fechaInicio == null || fechaFin == null) return true;
        return !fechaFin.isBefore(fechaInicio);
    }
}

@Data
class CartItem {
    @NotBlank
    private String productoId;
    
    @Min(1)
    private int cantidad;
}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property Reflection

After analyzing all acceptance criteria, I identified the following redundancies and consolidations:

- Properties 2.1, 2.2, 2.3 can be combined into a single comprehensive availability calculation property
- Properties 3.2 and 3.3 can be combined into a single CSRF validation property
- Properties 4.1, 4.2, 4.3 can be combined into a single file upload validation property
- Properties 5.1, 5.2, 5.3 can be combined into a single date validation property
- Properties 9.3 and 9.4 can be combined into a single payment confirmation property
- Properties 12.1 and 12.2 can be combined into a single user suspension property
- Properties 13.1 and 13.8 are redundant (both test category name uniqueness)
- Properties 14.2, 14.3, 14.4 can be combined into a single contact message validation property
- Properties 15.1, 15.2, 15.3 can be combined into a single coupon validation property

The following properties provide unique validation value and will be implemented:

### Property 1: Related Products Idempotence

*For any* product with a valid category, calling getRelatedProducts twice should return the same set of products.

**Validates: Requirements 1.4**

### Property 2: Maintenance State Availability Calculation

*For any* product with stock > 0, the product should be available if and only if its maintenance state is NOT "EN_REPARACION".

**Validates: Requirements 2.1, 2.2, 2.3**

### Property 3: Maintenance State Reversibility

*For any* product with stock > 0, changing state to "EN_REPARACION" and then back to "OPERATIVO" should restore the original availability.

**Validates: Requirements 2.4**

### Property 4: CSRF Token Protection

*For any* web form submission, submitting with a valid CSRF token should succeed, but resubmitting with the same token after session invalidation should fail with HTTP 403.

**Validates: Requirements 3.2, 3.3, 3.5**

### Property 5: File Upload Validation

*For any* file upload, the system should accept only files with Content-Type in {image/jpeg, image/png, image/webp, image/gif} and size ≤ 5MB, rejecting all others.

**Validates: Requirements 4.1, 4.2, 4.3**

### Property 6: File Upload Round Trip

*For any* valid image file, uploading and then downloading should return identical content.

**Validates: Requirements 4.5**

### Property 7: Date Validation Invariant

*For any* quotation request, the system should enforce that fechaFin ≥ fechaInicio and fechaInicio ≥ today.

**Validates: Requirements 5.1, 5.2, 5.3, 5.5**

### Property 8: Input Validation Consistency

*For any* valid input, validating, modifying with valid changes, and revalidating should apply the same validation rules consistently.

**Validates: Requirements 8.6**

### Property 9: Payment Initialization

*For any* confirmed reservation, initiating a payment should create a Payment with estado=PENDIENTE and a unique reference matching pattern "PAY-[A-Z0-9]{8}".

**Validates: Requirements 9.1, 9.2**

### Property 10: Payment Confirmation Side Effects

*For any* pending payment, confirming the payment should change its estado to PAGADO, update fechaPago, change the associated reservation to ENTREGADA, send a confirmation email, and increment the paymentsCompleted metric.

**Validates: Requirements 9.3, 9.4, 9.7, 9.8**

### Property 11: Payment Idempotence

*For any* payment, confirming it once should succeed, but attempting to confirm it again should throw InvalidOperationException.

**Validates: Requirements 9.9, 9.11**

### Property 12: Payment Reservation State Validation

*For any* reservation not in estado CONFIRMADA, attempting to initiate a payment should throw InvalidOperationException.

**Validates: Requirements 9.10**

### Property 13: Email Notification on Events

*For any* user registration, reservation confirmation, reservation state change, or payment confirmation, the system should send an email containing specific event data (not generic placeholders).

**Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.10**

### Property 14: Email Error Handling

*For any* email sending failure, the system should log the error without interrupting the main business flow.

**Validates: Requirements 10.8**

### Property 15: Password Reset Token Creation

*For any* user requesting password reset, the system should create a token with expiresAt = now + 1 hour and invalidate all previous tokens for that user.

**Validates: Requirements 11.1, 11.2**

### Property 16: Password Reset Token Validation

*For any* expired or used token, attempting to reset password should throw InvalidOperationException.

**Validates: Requirements 11.5**

### Property 17: Password Reset Token Irreversibility

*For any* valid token, using it to reset password should mark it as used permanently, preventing reuse.

**Validates: Requirements 11.10**

### Property 18: Password Encryption

*For any* password reset, the new password should be encrypted with BCrypt before storage.

**Validates: Requirements 11.7**

### Property 19: User Suspension Reversibility

*For any* active user, suspending and then activating should restore full access (estado=ACTIVO, login succeeds).

**Validates: Requirements 12.9**

### Property 20: Suspended User Login Rejection

*For any* suspended user, attempting to login should fail with message "Cuenta suspendida".

**Validates: Requirements 12.5**

### Property 21: Admin Action Audit Logging

*For any* admin action on users or categories, the system should create an entry in AuditLog.

**Validates: Requirements 12.6, 13.7**

### Property 22: User Soft Delete

*For any* user deletion, the system should set deleted=true while preserving all user data (email, nombre, etc.).

**Validates: Requirements 12.4**

### Property 23: Category Name Uniqueness

*For any* two categories with different IDs, their names must be different.

**Validates: Requirements 13.1, 13.8**

### Property 24: Category Deletion with Products

*For any* category with associated products, attempting to delete it should throw InvalidOperationException.

**Validates: Requirements 13.3**

### Property 25: Contact Message Validation

*For any* contact message submission, the system should validate that nombre, email, and mensaje are not blank, email has valid format, and mensaje length ≤ 2000 characters.

**Validates: Requirements 14.2, 14.3, 14.4**

### Property 26: Contact Message State Change

*For any* contact message, opening it should change estado from NO_LEIDO to LEIDO.

**Validates: Requirements 14.7**

### Property 27: Unread Message Counter Invariant

*For any* point in time, the unread message counter should equal the number of messages with estado=NO_LEIDO.

**Validates: Requirements 14.10**

### Property 28: Coupon Validation

*For any* coupon, it should be valid if and only if: (1) it exists, (2) current date is within [fechaInicio, fechaFin], and (3) usosActuales < limiteUsos.

**Validates: Requirements 15.1, 15.2, 15.3**

### Property 29: Coupon Discount Precision

*For any* coupon with descuento d% and total t, applying the coupon should result in totalConDescuento = t × (1 - d/100).

**Validates: Requirements 15.10**

### Property 30: Coupon Usage Limit

*For any* coupon with limiteUsos L, using it L times should succeed, but the (L+1)th attempt should fail.

**Validates: Requirements 15.11**


## Error Handling

### Exception Hierarchy

El sistema utiliza una jerarquía de excepciones personalizada para manejar errores de negocio:

```java
// Base exception
public class FurentException extends RuntimeException {
    public FurentException(String message) {
        super(message);
    }
    
    public FurentException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Resource not found (404)
public class ResourceNotFoundException extends FurentException {
    public ResourceNotFoundException(String resourceType, String id) {
        super(String.format("%s con ID '%s' no encontrado", resourceType, id));
    }
}

// Invalid operation (400)
public class InvalidOperationException extends FurentException {
    public InvalidOperationException(String message) {
        super(message);
    }
}

// Account suspended (403)
public class AccountSuspendedException extends FurentException {
    private final String reason;
    private final String duration;
    
    public AccountSuspendedException(String reason, String duration) {
        super("Cuenta suspendida: " + reason);
        this.reason = reason;
        this.duration = duration;
    }
    
    public String getReason() { return reason; }
    public String getDuration() { return duration; }
}
```

### Global Exception Handler

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "success", false,
                "error", ex.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
    }
    
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<?> handleInvalidOperation(InvalidOperationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "success", false,
                "error", ex.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "success", false,
                "errors", errors,
                "timestamp", LocalDateTime.now()
            ));
    }
    
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<?> handleOptimisticLocking(OptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of(
                "success", false,
                "error", "El recurso fue modificado por otro usuario. Por favor, recarga e intenta de nuevo.",
                "timestamp", LocalDateTime.now()
            ));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericError(Exception ex) {
        log.error("Error inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "success", false,
                "error", "Error interno del servidor",
                "timestamp", LocalDateTime.now()
            ));
    }
}
```

### Error Handling Strategies

#### Payment Errors

```java
// En PaymentService
public Payment confirmPayment(String paymentId, String referencia, String admin) {
    Payment payment = getByIdOrThrow(paymentId);
    
    // Validar estado
    if (!EstadoPago.PENDIENTE.name().equals(payment.getEstado())) {
        throw new InvalidOperationException(
            "El pago ya fue procesado. Estado actual: " + payment.getEstado()
        );
    }
    
    try {
        // Actualizar pago
        payment.setEstado(EstadoPago.PAGADO.name());
        payment.setReferencia(referencia);
        payment.setFechaPago(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);
        
        // Actualizar reserva
        reservationService.updateStatus(
            payment.getReservaId(), 
            EstadoReserva.ENTREGADA.name(), 
            admin, 
            "Pago confirmado: " + referencia
        );
        
        // Enviar email (async, no interrumpe si falla)
        emailService.sendPaymentConfirmation(saved);
        
        return saved;
    } catch (OptimisticLockingFailureException e) {
        throw new InvalidOperationException(
            "El pago fue modificado por otro usuario. Intenta de nuevo."
        );
    }
}
```

#### Email Errors

```java
// En EmailService
@Async
public void sendPaymentConfirmation(Payment payment) {
    try {
        // Renderizar template
        Context context = new Context();
        context.setVariable("payment", payment);
        String html = templateEngine.process("email/recibo-pago", context);
        
        // Enviar email
        sendHtmlEmail(user.getEmail(), "Recibo de Pago", html);
        
        log.info("Email de pago enviado a {}", user.getEmail());
    } catch (Exception e) {
        // NO interrumpir flujo principal
        log.error("Error enviando email de pago a {}: {}", 
            user.getEmail(), e.getMessage(), e);
        
        // Opcional: guardar en cola de reintentos
        emailRetryQueue.add(new EmailRetry(payment.getId(), "payment_confirmation"));
    }
}
```

#### File Upload Errors

```java
// En AdminProductosController
@PostMapping("/upload")
public String uploadImage(@RequestParam("file") MultipartFile file,
                         RedirectAttributes redirectAttributes) {
    try {
        // Validar archivo
        validateImageFile(file);
        
        // Guardar archivo
        String filename = fileStorageService.store(file);
        String url = "/uploads/" + filename;
        
        redirectAttributes.addFlashAttribute("success", "Imagen subida correctamente");
        redirectAttributes.addFlashAttribute("imageUrl", url);
        
    } catch (InvalidOperationException e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
    } catch (IOException e) {
        log.error("Error guardando archivo", e);
        redirectAttributes.addFlashAttribute("error", 
            "Error al guardar el archivo. Intenta de nuevo.");
    }
    
    return "redirect:/admin/productos";
}

private void validateImageFile(MultipartFile file) {
    if (file.isEmpty()) {
        throw new InvalidOperationException("Debes seleccionar un archivo");
    }
    
    Set<String> allowedTypes = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    
    if (!allowedTypes.contains(file.getContentType())) {
        throw new InvalidOperationException(
            "Solo se permiten imágenes (JPG, PNG, WebP, GIF)"
        );
    }
    
    long maxSize = 5 * 1024 * 1024; // 5MB
    if (file.getSize() > maxSize) {
        throw new InvalidOperationException(
            "La imagen no puede superar 5MB"
        );
    }
}
```

#### Validation Errors

```java
// En ApiController
@PostMapping("/cotizacion")
public ResponseEntity<?> createQuote(@Valid @RequestBody CotizacionRequest request,
                                    BindingResult result) {
    // Bean Validation automática por @Valid
    if (result.hasErrors()) {
        Map<String, String> errors = new HashMap<>();
        result.getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "errors", errors
        ));
    }
    
    // Validaciones adicionales
    if (request.getFechaFin().isBefore(request.getFechaInicio())) {
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "message", "La fecha de fin no puede ser anterior a la fecha de inicio"
        ));
    }
    
    // Crear reserva...
}
```


## Testing Strategy

### Dual Testing Approach

El sistema implementará una estrategia de testing dual que combina:

1. **Unit Tests**: Para casos específicos, ejemplos concretos y edge cases
2. **Property-Based Tests**: Para propiedades universales que deben cumplirse con cualquier entrada válida

Ambos tipos de tests son complementarios y necesarios:
- Los unit tests capturan bugs concretos y validan comportamientos específicos
- Los property tests verifican correctitud general y descubren edge cases inesperados

### Property-Based Testing Configuration

**Framework**: Utilizaremos **jqwik** para property-based testing en Java.

```xml
<!-- pom.xml -->
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.8.2</version>
    <scope>test</scope>
</dependency>
```

**Configuración**:
- Mínimo 100 iteraciones por property test (configurado en jqwik.properties)
- Cada test debe referenciar la propiedad del diseño mediante comentario
- Formato de tag: `Feature: furent-professional-improvements, Property {número}: {texto}`

```properties
# src/test/resources/jqwik.properties
jqwik.tries.default = 100
jqwik.reporting.onlyFailures = false
jqwik.shrinking.bounded = 1000
```

### Unit Testing Strategy

#### Services to Test

**ProductService**:
- `testGetRelatedProducts_WithValidCategory_ReturnsRelatedProducts()`
- `testGetRelatedProducts_WithNoRelatedProducts_ReturnsEmptyList()`
- `testGetRelatedProducts_DoesNotThrowException()`

**PaymentService**:
- `testInitPayment_WithConfirmedReservation_CreatesPayment()`
- `testInitPayment_WithNonConfirmedReservation_ThrowsException()`
- `testConfirmPayment_UpdatesStateAndReservation()`
- `testConfirmPayment_AlreadyProcessed_ThrowsException()`
- `testFailPayment_UpdatesStateAndNotifies()`

**EmailService**:
- `testSendWelcomeEmail_SendsEmailAsync()`
- `testSendPaymentConfirmation_IncludesPaymentDetails()`
- `testSendEmail_OnFailure_LogsErrorWithoutThrowing()`

**PasswordResetService**:
- `testCreateToken_InvalidatesOldTokens()`
- `testCreateToken_SetsExpirationOneHour()`
- `testResetPassword_WithValidToken_UpdatesPassword()`
- `testResetPassword_WithExpiredToken_ThrowsException()`
- `testResetPassword_WithUsedToken_ThrowsException()`

**CouponService**:
- `testValidateCoupon_WithValidCoupon_ReturnsTrue()`
- `testValidateCoupon_WithExpiredCoupon_ThrowsException()`
- `testValidateCoupon_WithMaxUsage_ThrowsException()`
- `testApplyDiscount_CalculatesCorrectly()`

#### Controllers to Test

**ApiController**:
- `testCreateQuote_WithValidDates_CreatesReservation()`
- `testCreateQuote_WithInvalidDates_ReturnsBadRequest()`
- `testCreateQuote_WithPastDate_ReturnsBadRequest()`

**AdminProductosController**:
- `testUploadImage_WithValidImage_Succeeds()`
- `testUploadImage_WithInvalidType_ReturnsError()`
- `testUploadImage_WithLargeFile_ReturnsError()`

**PaymentController**:
- `testInitPayment_WithValidReservation_ReturnsPayment()`
- `testConfirmPayment_AsAdmin_UpdatesPayment()`

#### Security Tests

**CSRF Protection**:
- `testFormSubmission_WithoutCSRF_Returns403()`
- `testFormSubmission_WithValidCSRF_Succeeds()`
- `testAPIEndpoint_WithoutCSRF_Succeeds()`

**File Upload Validation**:
- `testUpload_WithExecutableFile_Rejected()`
- `testUpload_WithScriptFile_Rejected()`
- `testUpload_WithValidImage_Accepted()`

### Property-Based Testing Strategy

#### Example Property Test

```java
@Property
@Label("Feature: furent-professional-improvements, Property 1: Related Products Idempotence")
void relatedProductsAreIdempotent(@ForAll("validProducts") Product product) {
    // Arrange
    String category = product.getCategoriaNombre();
    
    // Act
    List<Product> related1 = productService.getRelatedProducts(product.getId(), category);
    List<Product> related2 = productService.getRelatedProducts(product.getId(), category);
    
    // Assert
    assertThat(related1).containsExactlyInAnyOrderElementsOf(related2);
}

@Provide
Arbitrary<Product> validProducts() {
    return Combinators.combine(
        Arbitraries.strings().alpha().ofLength(24), // id
        Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(50), // nombre
        Arbitraries.of("Sillas", "Mesas", "Decoración", "Iluminación") // categoria
    ).as((id, nombre, categoria) -> {
        Product p = new Product();
        p.setId(id);
        p.setNombre(nombre);
        p.setCategoriaNombre(categoria);
        return p;
    });
}
```

#### Property Tests to Implement

**Property 2: Maintenance State Availability**
```java
@Property
@Label("Feature: furent-professional-improvements, Property 2: Maintenance State Availability Calculation")
void productAvailabilityDependsOnMaintenanceState(
    @ForAll @IntRange(min = 1, max = 100) int stock,
    @ForAll("maintenanceStates") String estado) {
    
    Product product = new Product();
    product.setStock(stock);
    product.setEstadoMantenimiento(estado);
    
    boolean available = calculateAvailability(product);
    boolean expected = stock > 0 && !"EN_REPARACION".equals(estado);
    
    assertThat(available).isEqualTo(expected);
}
```

**Property 7: Date Validation Invariant**
```java
@Property
@Label("Feature: furent-professional-improvements, Property 7: Date Validation Invariant")
void quotationDatesAreValid(
    @ForAll("futureDates") LocalDate fechaInicio,
    @ForAll("futureDates") LocalDate fechaFin) {
    
    CotizacionRequest request = new CotizacionRequest();
    request.setFechaInicio(fechaInicio);
    request.setFechaFin(fechaFin);
    
    boolean isValid = !fechaFin.isBefore(fechaInicio) && 
                     !fechaInicio.isBefore(LocalDate.now());
    
    if (isValid) {
        // Should not throw
        assertDoesNotThrow(() -> apiController.createQuote(request));
    } else {
        // Should throw or return error
        assertThrows(InvalidOperationException.class, 
            () -> apiController.createQuote(request));
    }
}
```

**Property 11: Payment Idempotence**
```java
@Property
@Label("Feature: furent-professional-improvements, Property 11: Payment Idempotence")
void paymentConfirmationIsIdempotent(@ForAll("pendingPayments") Payment payment) {
    // First confirmation should succeed
    Payment confirmed = paymentService.confirmPayment(
        payment.getId(), "REF-123", "admin@furent.com"
    );
    assertThat(confirmed.getEstado()).isEqualTo("PAGADO");
    
    // Second confirmation should fail
    assertThrows(InvalidOperationException.class, () -> 
        paymentService.confirmPayment(payment.getId(), "REF-456", "admin@furent.com")
    );
}
```

**Property 29: Coupon Discount Precision**
```java
@Property
@Label("Feature: furent-professional-improvements, Property 29: Coupon Discount Precision")
void couponDiscountIsCalculatedPrecisely(
    @ForAll @IntRange(min = 1, max = 100) int descuento,
    @ForAll @BigRange(min = "1.00", max = "10000.00") BigDecimal total) {
    
    Coupon coupon = new Coupon();
    coupon.setDescuento(descuento);
    
    BigDecimal result = couponService.applyDiscount(coupon, total);
    BigDecimal expected = total.multiply(
        BigDecimal.ONE.subtract(
            BigDecimal.valueOf(descuento).divide(BigDecimal.valueOf(100))
        )
    );
    
    assertThat(result).isEqualByComparingTo(expected);
    assertThat(result).isLessThanOrEqualTo(total);
}
```

### Integration Testing

**Test Containers**: Usar Testcontainers para MongoDB en tests de integración.

```java
@SpringBootTest
@Testcontainers
class PaymentServiceIntegrationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = 
        new MongoDBContainer("mongo:7");
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    @Test
    void testPaymentFlowEndToEnd() {
        // Create reservation
        Reservation reservation = createTestReservation();
        reservation.setEstado("CONFIRMADA");
        reservationRepository.save(reservation);
        
        // Init payment
        Payment payment = paymentService.initPayment(
            reservation.getId(), 
            reservation.getUsuarioId(), 
            MetodoPago.EFECTIVO
        );
        
        assertThat(payment.getEstado()).isEqualTo("PENDIENTE");
        assertThat(payment.getReferencia()).matches("PAY-[A-Z0-9]{8}");
        
        // Confirm payment
        Payment confirmed = paymentService.confirmPayment(
            payment.getId(), 
            "COMP-123", 
            "admin@furent.com"
        );
        
        assertThat(confirmed.getEstado()).isEqualTo("PAGADO");
        assertThat(confirmed.getFechaPago()).isNotNull();
        
        // Verify reservation updated
        Reservation updated = reservationRepository.findById(reservation.getId()).get();
        assertThat(updated.getEstado()).isEqualTo("ENTREGADA");
    }
}
```

### Test Coverage Goals

- **Unit Tests**: 70% code coverage
- **Integration Tests**: 50% coverage of critical flows
- **Property Tests**: All 30 correctness properties implemented
- **E2E Tests**: 5 critical user journeys

### CI/CD Integration

```yaml
# .github/workflows/test.yml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run tests
        run: mvn clean test
      
      - name: Generate coverage report
        run: mvn jacoco:report
      
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          file: ./target/site/jacoco/jacoco.xml
```


## Configuration and Security

### Security Configuration Updates

#### CSRF Protection

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ... existing config ...
            .csrf(csrf -> csrf
                // Habilitar CSRF para formularios web
                // Deshabilitar solo para API REST
                .ignoringRequestMatchers("/api/**")
            )
            .headers(headers -> headers
                // HTTP Strict Transport Security
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000) // 1 año
                    .preload(true)
                )
                // Content Security Policy
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com https://cdn.jsdelivr.net; " +
                        "style-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com https://fonts.googleapis.com; " +
                        "font-src 'self' https://fonts.gstatic.com; " +
                        "img-src 'self' data: blob: https:; " +
                        "connect-src 'self';"
                    )
                )
                // X-Frame-Options
                .frameOptions(frame -> frame.deny())
                // X-XSS-Protection
                .xssProtection(xss -> xss.headerValue("1; mode=block"))
            );
        
        return http.build();
    }
}
```

#### Thymeleaf CSRF Token

Todos los formularios Thymeleaf deben incluir el token CSRF:

```html
<form th:action="@{/admin/productos/guardar}" method="post">
    <!-- CSRF Token (automático con Thymeleaf) -->
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    
    <!-- Campos del formulario -->
    <input type="text" name="nombre" required/>
    
    <button type="submit">Guardar</button>
</form>
```

### Email Configuration

#### application.properties

```properties
# Gmail SMTP Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true

# Email sender configuration
furent.email.from=${MAIL_USERNAME}
furent.email.fromName=Furent

# Async configuration
spring.task.execution.pool.core-size=2
spring.task.execution.pool.max-size=5
spring.task.execution.pool.queue-capacity=100
spring.task.execution.thread-name-prefix=email-
```

#### Email Templates Location

```
src/main/resources/templates/email/
├── bienvenida.html
├── confirmacion-reserva.html
├── cambio-estado.html
├── reset-password.html
└── recibo-pago.html
```

### Admin Password Configuration

#### application.properties

```properties
# Admin password configuration
# En desarrollo: usa valor por defecto
# En producción: DEBE configurarse via variable de entorno
furent.admin.password=${FURENT_ADMIN_PASSWORD:${random.uuid}}
```

#### DataInitializer

```java
@Component
public class DataInitializer implements CommandLineRunner {
    
    @Value("${furent.admin.password}")
    private String adminPassword;
    
    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@furent.com").isEmpty()) {
            User admin = new User();
            admin.setEmail("admin@furent.com");
            admin.setNombre("Administrador");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(RolUsuario.ADMIN);
            admin.setEstado("ACTIVO");
            userRepository.save(admin);
            
            log.warn("═══════════════════════════════════════");
            log.warn("Admin creado con password: {}", adminPassword);
            log.warn("CAMBIAR INMEDIATAMENTE EN PRODUCCIÓN");
            log.warn("═══════════════════════════════════════");
        }
    }
}
```

### File Upload Configuration

#### application.properties

```properties
# File upload configuration
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB

# File storage location
furent.upload.dir=${UPLOAD_DIR:./uploads}
```

#### FileStorageService

```java
@Service
public class FileStorageService {
    
    private final Path uploadDir;
    
    public FileStorageService(@Value("${furent.upload.dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear directorio de uploads", e);
        }
    }
    
    public String store(MultipartFile file) throws IOException {
        // Generar nombre único
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = UUID.randomUUID().toString() + extension;
        
        // Guardar archivo
        Path targetLocation = this.uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        
        return filename;
    }
    
    public Resource load(String filename) {
        try {
            Path file = uploadDir.resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File", filename);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File", filename);
        }
    }
}
```

### Database Indexes

#### MongoDB Indexes

```javascript
// Payments collection
db.payments.createIndex({ "tenantId": 1, "usuarioId": 1 });
db.payments.createIndex({ "tenantId": 1, "estado": 1 });
db.payments.createIndex({ "reservaId": 1 });
db.payments.createIndex({ "referencia": 1 }, { unique: true });

// Password reset tokens collection
db.password_reset_tokens.createIndex({ "tenantId": 1, "userId": 1 });
db.password_reset_tokens.createIndex({ "token": 1 }, { unique: true });
db.password_reset_tokens.createIndex({ "expiresAt": 1 }, { expireAfterSeconds: 0 });

// Contact messages collection
db.contact_messages.createIndex({ "tenantId": 1, "leido": 1 });
db.contact_messages.createIndex({ "fechaCreacion": -1 });

// Users collection (add estado index)
db.users.createIndex({ "estado": 1 });
db.users.createIndex({ "deleted": 1 });

// Coupons collection
db.coupons.createIndex({ "tenantId": 1, "codigo": 1 }, { unique: true });
db.coupons.createIndex({ "tenantId": 1, "activo": 1 });
```

### Environment Variables

#### Production Environment

```bash
# Database
MONGODB_URI=mongodb://user:pass@host:27017/furent
REDIS_HOST=redis.example.com
REDIS_PORT=6379

# Security
FURENT_ADMIN_PASSWORD=SecurePassword123!
JWT_SECRET=your-256-bit-secret-key

# Email
MAIL_USERNAME=noreply@furent.com
MAIL_PASSWORD=app-specific-password

# File Storage
UPLOAD_DIR=/var/furent/uploads

# Application
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod
```

### Monitoring and Metrics

#### Prometheus Metrics

```java
@Configuration
public class MetricsConfig {
    
    private final MeterRegistry registry;
    
    // Payment metrics
    private final Counter paymentsCreated;
    private final Counter paymentsCompleted;
    private final Counter paymentsFailed;
    private final Gauge totalRevenue;
    
    // Email metrics
    private final Counter emailsSent;
    private final Counter emailsFailed;
    
    // Password reset metrics
    private final Counter passwordResetRequests;
    private final Counter passwordResetCompleted;
    
    public MetricsConfig(MeterRegistry registry) {
        this.registry = registry;
        
        this.paymentsCreated = Counter.builder("furent.payments.created")
            .description("Total payments created")
            .register(registry);
        
        this.paymentsCompleted = Counter.builder("furent.payments.completed")
            .description("Total payments completed")
            .register(registry);
        
        this.paymentsFailed = Counter.builder("furent.payments.failed")
            .description("Total payments failed")
            .register(registry);
        
        this.emailsSent = Counter.builder("furent.emails.sent")
            .description("Total emails sent")
            .register(registry);
        
        this.emailsFailed = Counter.builder("furent.emails.failed")
            .description("Total emails failed")
            .register(registry);
        
        this.passwordResetRequests = Counter.builder("furent.password_reset.requests")
            .description("Total password reset requests")
            .register(registry);
        
        this.passwordResetCompleted = Counter.builder("furent.password_reset.completed")
            .description("Total password resets completed")
            .register(registry);
    }
    
    // Getters for metrics
}
```

### Logging Configuration

#### logback-spring.xml

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/furent.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/furent.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Application loggers -->
    <logger name="com.alquiler.furent" level="INFO"/>
    <logger name="com.alquiler.furent.service.PaymentService" level="DEBUG"/>
    <logger name="com.alquiler.furent.service.EmailService" level="DEBUG"/>
    
    <!-- Security logger -->
    <logger name="org.springframework.security" level="INFO"/>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```


## Implementation Notes

### Bug Fixes Priority

Los siguientes bugs deben corregirse primero antes de implementar nuevas funcionalidades:

1. **BUG-001: getRelatedProducts()** - Cambiar `getCategory()` a `categoriaNombre`
2. **BUG-002: Estado de mantenimiento** - Cambiar lógica de disponibilidad
3. **BUG-003: CSRF deshabilitado** - Habilitar CSRF excepto para `/api/**`
4. **BUG-004: Validación de fechas** - Agregar validación en ApiController
5. **BUG-005: Upload sin validación** - Agregar validación de Content-Type y tamaño

### Implementation Order

#### Phase 1: Bug Fixes and Security (Week 1)

1. Corregir los 5 bugs críticos
2. Habilitar CSRF con tokens en formularios
3. Agregar headers de seguridad HTTP
4. Implementar validación de uploads
5. Hacer password admin configurable

#### Phase 2: Core Functionality (Week 2-3)

1. Implementar PaymentService completo
2. Implementar EmailService con templates
3. Implementar PasswordResetService
4. Crear endpoints de pagos (API + Admin)
5. Crear endpoints de password reset

#### Phase 3: Admin Features (Week 3-4)

1. Extender UserService con suspensión y soft delete
2. Implementar CRUD completo de usuarios
3. Implementar CRUD completo de categorías
4. Agregar auditoría de acciones admin
5. Implementar búsqueda y filtros

#### Phase 4: Additional Features (Week 4-5)

1. Implementar CouponService
2. Implementar ContactService
3. Crear formulario de contacto
4. Crear panel de mensajes admin
5. Integrar cupones en cotización

#### Phase 5: Testing (Week 5-6)

1. Escribir unit tests para servicios
2. Escribir property-based tests
3. Escribir integration tests
4. Configurar CI/CD con tests
5. Generar reporte de cobertura

### Database Migration Strategy

#### New Collections

```javascript
// Crear colecciones nuevas
db.createCollection("payments");
db.createCollection("password_reset_tokens");
db.createCollection("contact_messages");

// Crear índices
db.payments.createIndex({ "tenantId": 1, "usuarioId": 1 });
db.payments.createIndex({ "reservaId": 1 });
db.password_reset_tokens.createIndex({ "token": 1 }, { unique: true });
db.contact_messages.createIndex({ "fechaCreacion": -1 });
```

#### Existing Collections Updates

```javascript
// Agregar campos nuevos a users
db.users.updateMany(
    { estado: { $exists: false } },
    { $set: { estado: "ACTIVO", deleted: false } }
);

// Agregar índices nuevos
db.users.createIndex({ "estado": 1 });
db.users.createIndex({ "deleted": 1 });

// Agregar campos a coupons si no existen
db.coupons.updateMany(
    { usosActuales: { $exists: false } },
    { $set: { usosActuales: 0 } }
);
```

### API Endpoints Summary

#### New REST Endpoints

```
POST   /api/pagos/iniciar/{reservaId}     - Iniciar pago
GET    /api/pagos/mis-pagos                - Listar mis pagos
GET    /api/pagos/{id}                     - Obtener pago por ID

POST   /api/cupones/validar                - Validar cupón
GET    /api/cupones/{codigo}               - Obtener cupón por código

POST   /password-reset                     - Solicitar reset
GET    /password-reset/{token}             - Validar token
POST   /password-reset/confirm             - Confirmar reset

POST   /contacto                           - Enviar mensaje
```

#### New Admin Endpoints

```
GET    /admin/pagos                        - Listar pagos
POST   /admin/pagos/{id}/confirmar         - Confirmar pago
POST   /admin/pagos/{id}/rechazar          - Rechazar pago

GET    /admin/usuarios                     - Listar usuarios
POST   /admin/usuarios/{id}/suspender      - Suspender usuario
POST   /admin/usuarios/{id}/activar        - Activar usuario
POST   /admin/usuarios/{id}/rol            - Cambiar rol
DELETE /admin/usuarios/{id}                - Eliminar usuario (soft)

GET    /admin/categorias                   - Listar categorías
POST   /admin/categorias/guardar           - Crear/editar categoría
DELETE /admin/categorias/{id}              - Eliminar categoría

GET    /admin/mensajes                     - Listar mensajes
POST   /admin/mensajes/{id}/leer           - Marcar como leído

GET    /admin/cupones                      - Listar cupones
POST   /admin/cupones/guardar              - Crear/editar cupón
DELETE /admin/cupones/{id}                 - Eliminar cupón
```

### Performance Considerations

#### Caching Strategy

- **Products**: Cache completo con Redis (TTL: 1 hora)
- **Categories**: Cache completo con Redis (TTL: 24 horas)
- **Payments**: No cachear (datos transaccionales)
- **Users**: Cachear solo para autenticación (TTL: 15 minutos)

#### Database Optimization

- Usar índices compuestos para queries frecuentes
- Implementar paginación en todas las listas
- Usar proyecciones para reducir datos transferidos
- Implementar TTL index para tokens expirados

#### Async Operations

- Envío de emails: Siempre asíncrono con @Async
- Generación de reportes: Asíncrono con cola
- Notificaciones: Asíncrono con event bus
- Auditoría: Asíncrono con event listeners

### Security Checklist

- [x] CSRF habilitado para formularios web
- [x] Headers de seguridad HTTP configurados
- [x] Validación de tipos de archivo en uploads
- [x] Validación de tamaño de archivos
- [x] Password admin configurable por variable de entorno
- [x] Passwords encriptados con BCrypt
- [x] Tokens de reset con expiración de 1 hora
- [x] Validación de entradas con Bean Validation
- [x] Sanitización de inputs en DTOs
- [x] Rate limiting en endpoints críticos (existente)
- [x] JWT con expiración y refresh (existente)
- [x] Multi-tenant con aislamiento por tenantId (existente)

### Deployment Checklist

#### Pre-deployment

- [ ] Ejecutar todos los tests (unit + integration + property)
- [ ] Verificar cobertura de tests ≥ 70%
- [ ] Ejecutar análisis de seguridad (OWASP Dependency Check)
- [ ] Revisar logs de errores en staging
- [ ] Verificar configuración de variables de entorno
- [ ] Crear backup de base de datos

#### Deployment

- [ ] Aplicar migraciones de base de datos
- [ ] Desplegar nueva versión con rolling update
- [ ] Verificar health checks
- [ ] Monitorear métricas de Prometheus
- [ ] Verificar logs de aplicación
- [ ] Ejecutar smoke tests en producción

#### Post-deployment

- [ ] Verificar funcionalidades críticas manualmente
- [ ] Monitorear tasa de errores
- [ ] Verificar envío de emails
- [ ] Verificar procesamiento de pagos
- [ ] Revisar feedback de usuarios
- [ ] Documentar issues encontrados

### Rollback Plan

En caso de problemas críticos en producción:

1. **Rollback inmediato**: Revertir a versión anterior con Kubernetes
2. **Verificar estado**: Comprobar que la versión anterior funciona
3. **Análisis**: Identificar causa raíz del problema
4. **Fix**: Corregir en desarrollo y re-testear
5. **Re-deploy**: Desplegar nueva versión corregida

### Documentation Updates

Actualizar la siguiente documentación:

- [ ] README.md con nuevas funcionalidades
- [ ] API documentation (Swagger)
- [ ] Admin user guide
- [ ] Deployment guide
- [ ] Troubleshooting guide
- [ ] Architecture diagram
- [ ] Database schema

### Future Enhancements

Funcionalidades para considerar en futuras iteraciones:

1. **Pagos online**: Integración con pasarelas de pago (Stripe, PayU)
2. **Notificaciones push**: Implementar con Firebase Cloud Messaging
3. **Chat en vivo**: Soporte en tiempo real con WebSockets
4. **Reportes avanzados**: Dashboard con analytics y predicciones
5. **App móvil**: Aplicación nativa iOS/Android
6. **Integración WhatsApp**: Notificaciones y chatbot
7. **Sistema de fidelización**: Puntos y recompensas
8. **Recomendaciones IA**: Machine learning para sugerencias

---

## Conclusion

Este diseño proporciona una base sólida para implementar las mejoras profesionales críticas de Furent. La arquitectura mantiene la separación de responsabilidades, implementa patrones de diseño probados, y establece una estrategia de testing comprehensiva que garantiza la calidad del código.

Las correcciones de bugs, mejoras de seguridad y funcionalidades nuevas elevarán la calificación del sistema de 6.8/10 a 9.5/10, completando el MVP funcional y preparando la plataforma para producción.

La implementación debe seguir el orden propuesto (bugs → seguridad → funcionalidades → testing) para minimizar riesgos y maximizar el valor entregado en cada fase.

