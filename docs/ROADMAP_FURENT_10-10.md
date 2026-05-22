# 🚀 FURENT — ROADMAP COMPLETO PARA CALIFICACIÓN 10/10

> **Proyecto:** Furent - Plataforma de Alquiler de Mobiliarios para Eventos  
> **Fecha del informe:** 7 de marzo de 2026  
> **Estado actual:** 6.2/10  
> **Objetivo:** 10/10 — Proyecto robusto, funcional, seguro y profesional

---

## 📋 ÍNDICE

1. [Bugs Críticos a Corregir YA](#1--bugs-críticos-a-corregir-ya)
2. [Seguridad — De Vulnerable a Blindado](#2--seguridad--de-vulnerable-a-blindado)
3. [Funcionalidades Incompletas a Cerrar](#3--funcionalidades-incompletas-a-cerrar)
4. [Nuevas Funcionalidades Necesarias](#4--nuevas-funcionalidades-necesarias)
5. [Calidad de Código y Mejores Prácticas](#5--calidad-de-código-y-mejores-prácticas)
6. [Testing — De 0 Tests a Cobertura Completa](#6--testing--de-0-tests-a-cobertura-completa)
7. [Frontend — UX/UI de Nivel Profesional](#7--frontend--uxui-de-nivel-profesional)
8. [Infraestructura y DevOps](#8--infraestructura-y-devops)
9. [Documentación Profesional](#9--documentación-profesional)
10. [Innovaciones Diferenciadoras](#10--innovaciones-diferenciadoras)
11. [Plan de Ejecución por Fases](#11--plan-de-ejecución-por-fases)
12. [Checklist Final 10/10](#12--checklist-final-1010)

---

## 1. 🔴 BUGS CRÍTICOS A CORREGIR YA

### BUG-001: `getRelatedProducts()` rompe la página de detalle

**Archivo:** `ProductService.java`  
**Problema:** Llama a `p.getCategory()` que NO EXISTE en `Product.java` (el campo es `categoriaNombre`).  
**Impacto:** RuntimeException al entrar a `/producto/{id}`.  
**Fix:**
```java
// ANTES (ROTO):
.filter(p -> p.getCategory().equals(product.getCategory()))

// DESPUÉS (CORRECTO):
.filter(p -> p.getCategoriaNombre().equals(product.getCategoriaNombre()))
```

---

### BUG-002: Estado de mantenimiento NUNCA coincide

**Archivo:** `AdminController.java` (línea ~233)  
**Problema:** Al guardar producto, compara con `"OPERATIVO"` pero los productos se inicializan con `"EXCELENTE"`, `"BUENO"`, `"REGULAR"`, `"EN_REPARACION"`.  
**Fix:**
```java
// ANTES:
boolean available = stock > 0 && "OPERATIVO".equals(estadoMantenimiento);

// DESPUÉS:
boolean available = stock > 0 && !"EN_REPARACION".equals(estadoMantenimiento);
```

---

### BUG-003: CSRF completamente deshabilitado

**Archivo:** `SecurityConfig.java`  
**Problema:** `.csrf(csrf -> csrf.disable())` deja el sistema vulnerable a ataques CSRF.  
**Fix:** Habilitar CSRF para formularios e ignorar solo las rutas API REST:
```java
.csrf(csrf -> csrf
    .ignoringRequestMatchers("/api/**")
    // Thymeleaf maneja tokens CSRF automáticamente
)
```

---

### BUG-004: Cotización acepta fechas inválidas

**Archivo:** `ApiController.java`  
**Problema:** Si `fechaFin < fechaInicio`, permite crear reserva con `days = 1`.  
**Fix:** Validar y rechazar con error 400:
```java
if (request.getFechaFin().isBefore(request.getFechaInicio())) {
    return ResponseEntity.badRequest().body(Map.of(
        "success", false,
        "message", "La fecha de fin no puede ser anterior a la fecha de inicio"
    ));
}
```

---

### BUG-005: `getFeaturedProducts()` retorna siempre los mismos

**Archivo:** `ProductService.java`  
**Problema:** `all.subList(0, 6)` retorna los primeros 6 de la lista sin criterio.  
**Fix:** Agregar campo `destacado` al modelo o usar calificación:
```java
public List<Product> getFeaturedProducts() {
    return productRepository.findByDisponibleTrue()
        .stream()
        .sorted(Comparator.comparingDouble(Product::getCalificacion).reversed())
        .limit(6)
        .collect(Collectors.toList());
}
```

---

### BUG-006: `countByEstado()` carga toda la lista en memoria

**Archivo:** `ReservationService.java`  
**Problema:** `findByEstado(estado).size()` descarga TODOS los documentos solo para contar.  
**Fix:** Agregar al repositorio:
```java
// ReservationRepository.java
long countByEstado(String estado);
```

---

### BUG-007: Upload de archivos sin validación de tipo

**Archivo:** `AdminController.java`  
**Problema:** Acepta cualquier archivo (`.exe`, `.sh`, `.php`). Riesgo de ejecución remota.  
**Fix:**
```java
private static final Set<String> ALLOWED_TYPES = Set.of(
    "image/jpeg", "image/png", "image/webp", "image/gif"
);

if (!ALLOWED_TYPES.contains(file.getContentType())) {
    redirectAttributes.addFlashAttribute("error", "Solo se permiten imágenes");
    return "redirect:/admin/mobiliarios";
}
```

---

## 2. 🛡️ SEGURIDAD — De Vulnerable a Blindado

### SEC-001: Habilitar protección CSRF para formularios
```java
// SecurityConfig.java - Solo ignorar CSRF en API REST
.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
```
- Thymeleaf inyecta `_csrf` tokens automáticamente
- En AJAX (app.js), agregar header `X-CSRF-TOKEN`:
```javascript
const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
fetch(url, {
    headers: { [csrfHeader]: csrfToken, 'Content-Type': 'application/json' }
});
```

### SEC-002: Rate Limiting (Anti fuerza bruta)
```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.1</version>
</dependency>
```
Implementar filtro:
```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    private Bucket createBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1))))
            .build();
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
            HttpServletResponse response, FilterChain chain) {
        String ip = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(ip, k -> createBucket());
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.getWriter().write("Demasiadas solicitudes. Intenta más tarde.");
        }
    }
}
```

### SEC-003: Cambiar contraseña admin por defecto
```java
// DataInitializer.java
@Value("${furent.admin.password:${random.value}}")
private String adminPassword;

// En run():
String pwd = adminPassword;
log.warn("=== Admin creado con password: {} ===", pwd); // Solo visible en logs al primer inicio
```
```properties
# application.properties (desarrollo)
furent.admin.password=Admin_Furent_2026!
```

### SEC-004: Headers de seguridad HTTP
```java
// SecurityConfig.java
.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp
        .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com; style-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: blob:;"))
    .frameOptions(frame -> frame.deny())
    .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
)
```

### SEC-005: Validar y sanitizar TODAS las entradas
```java
// CotizacionRequest.java
@Data
public class CotizacionRequest {
    @NotBlank(message = "El tipo de evento es obligatorio")
    @Size(max = 100)
    private String tipoEvento;
    
    @Min(value = 1, message = "Debe haber al menos 1 invitado")
    @Max(value = 10000)
    private int invitados;
    
    @NotNull(message = "La fecha de inicio es obligatoria")
    @FutureOrPresent(message = "La fecha debe ser hoy o posterior")
    private LocalDate fechaInicio;
    
    @NotNull(message = "La fecha de fin es obligatoria")
    @Future(message = "La fecha de fin debe ser futura")
    private LocalDate fechaFin;
    
    @NotBlank @Size(max = 500)
    private String direccion;
    
    @Size(max = 1000)
    private String notas;
    
    @NotBlank
    private String metodoPago;
    
    @NotEmpty(message = "Debe seleccionar al menos un producto")
    @Valid
    private List<CartItem> items;
}
```

### SEC-006: Protección contra inyección NoSQL (MongoDB)
```java
// Nunca interpolar strings directamente en queries
// Usar siempre Spring Data Repository methods o @Query con parámetros:
@Query("{ 'nombre': { $regex: ?0, $options: 'i' } }")
List<Product> searchByNombre(String keyword);
```

### SEC-007: Manejo seguro de sesiones
```properties
# application.properties
server.servlet.session.timeout=30m
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.same-site=lax
```

### SEC-008: Auditoría completa de acciones sensibles

Extender el `AuditLogService` para registrar:
- Login exitoso / fallido
- Cambio de contraseña
- Cambio de rol de usuario
- Eliminación de productos/usuarios
- Cambio de estado de reserva
- Descarga de PDFs
- Suspensión/activación de cuentas

---

## 3. 🔧 FUNCIONALIDADES INCOMPLETAS A CERRAR

### FEAT-001: Sistema de Pagos Completo

**Estado actual:** Modelo `Payment` existe pero cero lógica.  
**Implementar:**

```
PaymentController.java
├── POST /pago/iniciar/{reservaId}     → Iniciar proceso de pago
├── POST /pago/confirmar               → Confirmar pago (simulado o real)
├── GET  /pago/comprobante/{id}        → Ver comprobante
└── POST /pago/webhook                 → Webhook para pasarela externa

PaymentService.java
├── initPayment(reservaId, method)     → Crear registro Payment PENDIENTE
├── confirmPayment(paymentId, ref)     → Marcar como PAGADO + actualizar reserva
├── failPayment(paymentId, reason)     → Marcar como FALLIDO
├── getPaymentsByUser(userId)          → Historial de pagos
├── getPaymentByReserva(reservaId)     → Pago de una reserva
└── generateReceipt(paymentId)         → PDF de recibo
```

**Flujo de pago:**
```
1. Usuario crea cotización → Reserva PENDIENTE
2. Admin confirma cotización → Reserva CONFIRMADA
3. Usuario va a /pago/iniciar/{id}
4. Elige método: Transferencia, Nequi, Daviplata, Efectivo
5. Sube comprobante (imagen) o se genera referencia
6. Admin valida pago → Payment PAGADO → Reserva ACTIVA
7. PDF de recibo generado automáticamente
```

**Métodos de pago a soportar:**
- Transferencia bancaria (con datos de cuenta)
- Nequi / Daviplata (números de contacto)
- Efectivo (al momento de entrega)
- Tarjeta (integración futura con ePayco/MercadoPago)

---

### FEAT-002: Admin CRUD Completo de Usuarios

**Endpoints a crear en `AdminController`:**
```java
@PostMapping("/admin/usuarios/{id}/suspender")
public String suspendUser(@PathVariable String id,
        @RequestParam String razon,
        @RequestParam(required = false) String duracion,
        @RequestParam(required = false) boolean permanente) {
    // Lógica de suspensión con auditoría
}

@PostMapping("/admin/usuarios/{id}/activar")
public String activateUser(@PathVariable String id) {
    // Reactivar cuenta + limpiar banderas
}

@PostMapping("/admin/usuarios/{id}/rol")
public String changeRole(@PathVariable String id, @RequestParam String nuevoRol) {
    // Solo SUPER_ADMIN puede cambiar roles
}

@PostMapping("/admin/usuarios/{id}/eliminar")
public String deleteUser(@PathVariable String id) {
    // Soft delete, no hard delete
}
```

**En el template `usuarios.html`:**
- Tabla con paginación de todos los usuarios
- Filtros: activos, suspendidos, por rol
- Modal de suspensión con campos: razón, tipo (temporal/indefinida/permanente), duración
- Botón de activar
- Botón de cambiar rol
- Historial de actividad del usuario

---

### FEAT-003: Admin CRUD Completo de Categorías

**Endpoints en `AdminController`:**
```java
@PostMapping("/admin/categorias/guardar")
public String saveCategory(@RequestParam String nombre,
        @RequestParam String descripcion,
        @RequestParam String icono,
        RedirectAttributes redirectAttributes) {
    // Crear o actualizar categoría
    // Generar slug automático
    // Registrar en auditoría
}

@PostMapping("/admin/categorias/{id}/eliminar")
public String deleteCategory(@PathVariable String id) {
    // Verificar que no tenga productos asociados
    // Si tiene, mostrar error
}
```

---

### FEAT-004: Gestión Completa de Reservas (Admin)

**Endpoints adicionales:**
```java
@PostMapping("/admin/reservas/{id}/estado")
public String updateReservationStatus(@PathVariable String id,
        @RequestParam String nuevoEstado,
        @RequestParam(required = false) String nota) {
    // Cambiar estado con validación de transiciones:
    // PENDIENTE → CONFIRMADA → ACTIVA → COMPLETADA
    // Cualquier estado → CANCELADA
    // Enviar notificación al usuario
    // Registrar en auditoría
}

@GetMapping("/admin/reservas/{id}")
public String reservationDetail(@PathVariable String id, Model model) {
    // Vista detallada con timeline de estados
    // Info del usuario, productos, pago asociado
}
```

**Máquina de estados de reserva:**
```
PENDIENTE ──── admin confirma ────→ CONFIRMADA
CONFIRMADA ─── pago recibido ─────→ ACTIVA
ACTIVA ──────── entrega hecha ────→ EN_CURSO
EN_CURSO ────── recogida hecha ───→ COMPLETADA
(cualquiera) ── cancelación ──────→ CANCELADA
```

---

### FEAT-005: Formulario de Contacto Funcional

**Crear:**
```java
// model/ContactMessage.java
@Document(collection = "mensajes_contacto")
public class ContactMessage {
    @Id private String id;
    private String nombre;
    private String email;
    private String telefono;
    private String asunto;
    private String mensaje;
    private boolean leido;
    private LocalDateTime fechaCreacion;
}

// repository/ContactMessageRepository.java
public interface ContactMessageRepository extends MongoRepository<ContactMessage, String> {
    List<ContactMessage> findByLeidoFalseOrderByFechaCreacionDesc();
    long countByLeidoFalse();
}
```

**Endpoint:**
```java
@PostMapping("/contacto")
public String submitContact(@Valid ContactForm form, BindingResult result, 
        RedirectAttributes redirectAttributes) {
    if (result.hasErrors()) { ... }
    contactService.save(form);
    emailService.sendContactNotification(form); // Notificar al admin
    redirectAttributes.addFlashAttribute("success", "Mensaje enviado correctamente");
    return "redirect:/contacto";
}
```

**Admin view:** Agregar en el panel admin una sección "Mensajes" con badge de no leídos.

---

### FEAT-006: Eliminar o Implementar GraphQL

**Opción A — Eliminarlo** (si no se va a usar):
```xml
<!-- Quitar del pom.xml -->
<!-- <dependency>spring-boot-starter-graphql</dependency> -->
```

**Opción B — Implementarlo** (recomendado para diferenciarse):

```graphql
# src/main/resources/graphql/schema.graphqls
type Query {
    productos(categoria: String, disponible: Boolean): [Producto!]!
    producto(id: ID!): Producto
    categorias: [Categoria!]!
    misReservas: [Reserva!]!
}

type Mutation {
    crearCotizacion(input: CotizacionInput!): Reserva!
    crearResena(input: ResenaInput!): Review!
}

type Producto {
    id: ID!
    nombre: String!
    descripcion: String!
    precioPorDia: Float!
    imagenUrl: String
    categoria: Categoria
    calificacion: Float
    disponible: Boolean!
    resenas: [Review!]!
}

type Categoria {
    id: ID!
    nombre: String!
    descripcion: String
    icono: String
    productos: [Producto!]!
}

type Reserva {
    id: ID!
    items: [ItemReserva!]!
    fechaInicio: String!
    fechaFin: String!
    total: Float!
    estado: String!
}
```

```java
@Controller
public class ProductGraphQLController {
    @QueryMapping
    public List<Product> productos(@Argument String categoria, @Argument Boolean disponible) { ... }
    
    @QueryMapping
    public Product producto(@Argument String id) { ... }
    
    @SchemaMapping(typeName = "Producto", field = "resenas")
    public List<Review> reviews(Product product) { ... }
}
```

---

## 4. ✨ NUEVAS FUNCIONALIDADES NECESARIAS

### FEAT-NEW-001: Sistema de Notificaciones por Email

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

```properties
# application.properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

**Implementar `EmailService.java`:**
```java
@Service
public class EmailService {
    @Async
    public void sendWelcomeEmail(User user) { ... }
    
    @Async
    public void sendReservationConfirmation(Reservation reservation) { ... }
    
    @Async
    public void sendStatusChange(Reservation reservation, String oldStatus) { ... }
    
    @Async
    public void sendPasswordResetToken(User user, String token) { ... }
    
    @Async
    public void sendPaymentConfirmation(Payment payment) { ... }
    
    @Async
    public void sendContactNotification(ContactMessage message) { ... }
}
```

**Emails a enviar:**
| Evento | Destinatario | Template |
|--------|-------------|----------|
| Nuevo registro | Usuario | Bienvenida + verificación |
| Cotización creada | Usuario + Admin | Resumen de pedido |
| Estado cambia | Usuario | Notificación de estado |
| Pago confirmado | Usuario | Recibo digital |
| Cuenta suspendida | Usuario | Razón + duración |
| Mensaje de contacto | Admin | Datos del mensaje |
| Recordatorio de evento | Usuario | 2 días antes del evento |
| Password reset | Usuario | Token + link |

**Templates HTML para emails** en `templates/email/`:
- `bienvenida.html`
- `confirmacion-reserva.html`
- `cambio-estado.html`
- `recibo-pago.html`
- `reset-password.html`

---

### FEAT-NEW-002: Recuperación de Contraseña

**Flujo:**
```
1. GET  /password-reset          → Formulario pedir email
2. POST /password-reset          → Genera token + envía email
3. GET  /password-reset/{token}  → Formulario nueva contraseña
4. POST /password-reset/confirm  → Cambia contraseña + invalida token
```

**Modelo:**
```java
@Document(collection = "password_reset_tokens")
public class PasswordResetToken {
    @Id private String id;
    private String userId;
    private String token; // UUID aleatorio
    private LocalDateTime expiresAt; // +1 hora
    private boolean used;
}
```

---

### FEAT-NEW-003: Búsqueda Global de Productos

**Backend:**
```java
// ProductRepository.java
@Query("{ $or: [ " +
       "{ 'nombre': { $regex: ?0, $options: 'i' } }, " +
       "{ 'descripcion': { $regex: ?0, $options: 'i' } }, " +
       "{ 'categoriaNombre': { $regex: ?0, $options: 'i' } }, " +
       "{ 'material': { $regex: ?0, $options: 'i' } } " +
       "] }")
List<Product> searchProducts(String keyword);
```

**Endpoint:**
```java
@GetMapping("/catalogo")
public String catalog(@RequestParam(required = false) String q,
                      @RequestParam(required = false) String categoria,
                      Model model) {
    List<Product> products;
    if (q != null && !q.isBlank()) {
        products = productService.searchProducts(q);
        model.addAttribute("searchQuery", q);
    } else if (categoria != null) {
        products = productService.getProductsByCategory(categoria);
    } else {
        products = productService.getAllProducts();
    }
    model.addAttribute("products", products);
    return "catalog";
}
```

**Frontend:** Barra de búsqueda en el navbar + autocompletado:
```javascript
// Debounce search con fetch a /api/productos/search?q=...
let timeout;
searchInput.addEventListener('input', (e) => {
    clearTimeout(timeout);
    timeout = setTimeout(() => {
        fetch(`/api/productos/search?q=${encodeURIComponent(e.target.value)}`)
            .then(r => r.json())
            .then(renderSuggestions);
    }, 300);
});
```

---

### FEAT-NEW-004: Sistema de Favoritos / Wishlist

```java
// Agregar al modelo User.java
private List<String> favoritos = new ArrayList<>(); // IDs de productos

// Endpoints
@PostMapping("/api/favoritos/{productoId}")    // Agregar
@DeleteMapping("/api/favoritos/{productoId}")  // Quitar
@GetMapping("/panel/favoritos")                // Ver lista
```

---

### FEAT-NEW-005: Notificaciones In-App (Tiempo Real)

```java
@Document(collection = "notificaciones")
public class Notification {
    @Id private String id;
    private String userId;
    private String titulo;
    private String mensaje;
    private String tipo; // INFO, SUCCESS, WARNING, ALERT
    private String link; // URL a donde navegar
    private boolean leida;
    private LocalDateTime fecha;
}
```

**Implementar con Server-Sent Events (SSE):**
```java
@GetMapping(value = "/api/notificaciones/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<Notification> streamNotifications(Principal principal) {
    // Emitir notificaciones nuevas en tiempo real
}
```

**Frontend:** Badge con contador en el navbar + dropdown con lista de notificaciones.

---

### FEAT-NEW-006: Paginación en Todos los Listados

**Backend:**
```java
// ProductRepository.java
Page<Product> findByCategoriaNombre(String categoria, Pageable pageable);
Page<Product> findByDisponibleTrue(Pageable pageable);

// ProductService.java
public Page<Product> getProducts(String categoria, int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
    if (categoria != null) {
        return productRepository.findByCategoriaNombre(categoria, pageable);
    }
    return productRepository.findAll(pageable);
}
```

**En templates:**
```html
<!-- Componente de paginación reutilizable -->
<nav th:fragment="pagination(page, baseUrl)">
    <div class="flex justify-center gap-2 mt-8">
        <a th:if="${page.hasPrevious()}" 
           th:href="@{${baseUrl}(page=${page.number - 1})}">← Anterior</a>
        <span th:each="i : ${#numbers.sequence(0, page.totalPages - 1)}"
              th:class="${i == page.number} ? 'bg-furent-500 text-white' : 'bg-surface-100'">
            <a th:href="@{${baseUrl}(page=${i})}" th:text="${i + 1}"></a>
        </span>
        <a th:if="${page.hasNext()}" 
           th:href="@{${baseUrl}(page=${page.number + 1})}">Siguiente →</a>
    </div>
</nav>
```

**Aplicar en:** catálogo (12 por página), admin productos, admin reservas, admin usuarios.

---

### FEAT-NEW-007: Dashboard de Usuario Mejorado

El panel de usuario actual solo muestra reservas. Expandirlo con:

1. **Resumen visual:** Total gastado, reservas activas, próximo evento
2. **Timeline de reservas:** Visualización cronológica
3. **Historial de pagos:** Tabla con recibos descargables
4. **Favoritos:** Grid de productos guardados
5. **Notificaciones recientes:** Últimas 5 alertas
6. **Perfil completado %:** Barra de progreso
7. **Reseñas dejadas:** Lista con opción de editar

---

### FEAT-NEW-008: Calendario Visual de Disponibilidad

**Para productos:**
```javascript
// Integrar FullCalendar.js o implementar custom
// Mostrar en product-detail.html:
// - Fechas ocupadas en rojo
// - Fechas disponibles en verde
// - Fecha seleccionada en naranja
```

**Para admin (logística):**
- Calendario interactivo con eventos drag & drop
- Vista mensual / semanal / diaria
- Color por estado: pendiente (amarillo), confirmada (azul), activa (verde)

---

### FEAT-NEW-009: Sistema de Descuentos y Cupones

```java
@Document(collection = "cupones")
public class Coupon {
    @Id private String id;
    private String codigo; // "BODA2026", "PRIMERA10"
    private String tipo; // PORCENTAJE, MONTO_FIJO
    private double valor; // 10 (%) o 50000 (COP)
    private LocalDate validoDesde;
    private LocalDate validoHasta;
    private int usosMaximos;
    private int usosActuales;
    private double montoMinimo; // Monto mínimo de orden
    private boolean activo;
    private List<String> categoriasAplicables; // null = todas
}
```

**Flujo:**
```
1. Usuario ingresa código en cotización (paso 4)
2. POST /api/cupones/validar {codigo, montoTotal}
3. Retorna: {valido, descuento, montoFinal, mensaje}
4. Se aplica al total de la reserva
```

---

### FEAT-NEW-010: Exportación de Datos (Admin)

```java
@GetMapping("/admin/exportar/reservas")
public ResponseEntity<byte[]> exportReservations(
        @RequestParam(required = false) String estado,
        @RequestParam(required = false) String formato) { // csv, excel, pdf
    // Generar archivo según formato
}

@GetMapping("/admin/exportar/usuarios")
public ResponseEntity<byte[]> exportUsers() { ... }

@GetMapping("/admin/exportar/productos")
public ResponseEntity<byte[]> exportProducts() { ... }
```

**Formatos:** CSV (sin dependencia extra), PDF (ya tienen OpenHTMLToPDF).  
**Agregar Apache POI para Excel:**
```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

---

### FEAT-NEW-011: Historial de Cambios de Estado (Timeline)

```java
@Document(collection = "estado_historial")
public class StatusHistory {
    @Id private String id;
    private String reservaId;
    private String estadoAnterior;
    private String estadoNuevo;
    private String usuarioAccion; // Quién hizo el cambio
    private String nota;
    private LocalDateTime fecha;
}
```

Mostrar como timeline visual en el detalle de reserva:
```
PENDIENTE ──→ CONFIRMADA ──→ ACTIVA ──→ COMPLETADA
  3 Mar         5 Mar         10 Mar       12 Mar
```

---

### FEAT-NEW-012: Multi-Idioma (i18n)

```properties
# messages.properties (español - default)
nav.home=Inicio
nav.catalog=Catálogo
nav.about=Nosotros
nav.contact=Contacto
product.price=Precio por día
product.available=Disponible
product.unavailable=No disponible

# messages_en.properties (inglés)
nav.home=Home
nav.catalog=Catalog
...
```

```java
// config/LocaleConfig.java
@Bean
public LocaleResolver localeResolver() {
    SessionLocaleResolver resolver = new SessionLocaleResolver();
    resolver.setDefaultLocale(new Locale("es"));
    return resolver;
}

@Bean
public LocaleChangeInterceptor localeInterceptor() {
    LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
    interceptor.setParamName("lang");
    return interceptor;
}
```

En templates: `th:text="#{nav.home}"` en lugar de texto hardcodeado.

---

### FEAT-NEW-013: Verificación de Email al Registrarse

```
1. POST /registro → crea user con emailVerificado = false
2. Envía email con link: /verificar-email/{token}
3. GET /verificar-email/{token} → activa cuenta
4. Login solo permitido si emailVerificado == true
```

---

## 5. 🧹 CALIDAD DE CÓDIGO Y MEJORES PRÁCTICAS

### QA-001: Reemplazar Strings mágicos por Enums

```java
// enums/EstadoReserva.java
public enum EstadoReserva {
    PENDIENTE, CONFIRMADA, ACTIVA, EN_CURSO, COMPLETADA, CANCELADA;
}

// enums/EstadoMantenimiento.java
public enum EstadoMantenimiento {
    EXCELENTE, BUENO, REGULAR, EN_REPARACION;
}

// enums/RolUsuario.java
public enum RolUsuario {
    USER, ADMIN, SUPER_ADMIN;
}

// enums/MetodoPago.java
public enum MetodoPago {
    TRANSFERENCIA, NEQUI, DAVIPLATA, EFECTIVO, TARJETA;
}

// enums/EstadoPago.java
public enum EstadoPago {
    PENDIENTE, PAGADO, FALLIDO, REEMBOLSADO;
}
```

---

### QA-002: Manejo Global de Excepciones

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(Model model) {
        model.addAttribute("error", "No tienes permiso para acceder a esta página");
        return "error/403";
    }
    
    @ExceptionHandler(AccountSuspendedException.class)
    public String handleSuspended(AccountSuspendedException ex, Model model) {
        model.addAttribute("reason", ex.getReason());
        model.addAttribute("duration", ex.getDuration());
        return "error/suspended";
    }
    
    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, Model model) {
        log.error("Error inesperado", ex);
        model.addAttribute("error", "Ha ocurrido un error inesperado");
        return "error/500";
    }
}
```

**Crear páginas de error:**
- `templates/error/404.html` — Página no encontrada
- `templates/error/403.html` — Acceso denegado
- `templates/error/500.html` — Error del servidor
- `templates/error/suspended.html` — Cuenta suspendida

---

### QA-003: Logging profesional con SLF4J

**Reemplazar en TODOS los archivos:**
```java
// ANTES:
System.out.println("...");
System.err.println("Error: " + e.getMessage());

// DESPUÉS:
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService {
    public void register(...) {
        log.info("Nuevo registro de usuario: {}", email);
        log.warn("Intento de registro con email duplicado: {}", email);
        log.error("Error al registrar usuario: {}", email, exception);
    }
}
```

---

### QA-004: Excepciones personalizadas

```java
// exception/ResourceNotFoundException.java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String entity, String id) {
        super(String.format("%s con ID '%s' no encontrado", entity, id));
    }
}

// exception/InvalidOperationException.java
public class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String message) {
        super(message);
    }
}

// exception/DuplicateResourceException.java
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
```

Usar en servicios:
```java
public Product getProductById(String id) {
    return productRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
}
```

---

### QA-005: DTOs para TODAS las respuestas (no exponer modelos directamente)

```java
// dto/ProductResponse.java
public record ProductResponse(
    String id,
    String nombre,
    String descripcionCorta,
    double precioPorDia,
    String imagenUrl,
    String categoriaNombre,
    double calificacion,
    boolean disponible
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
            p.getId(), p.getNombre(), p.getDescripcionCorta(),
            p.getPrecioPorDia(), p.getImagenUrl(), p.getCategoriaNombre(),
            p.getCalificacion(), p.isDisponible()
        );
    }
}

// dto/UserResponse.java (NUNCA exponer password)
public record UserResponse(
    String id, String email, String nombre, 
    String apellido, String role, boolean activo
) {}

// dto/ReservationResponse.java
public record ReservationResponse(
    String id, List<ItemReserva> items,
    LocalDate fechaInicio, LocalDate fechaFin,
    double total, String estado,
    LocalDateTime fechaCreacion
) {}
```

---

### QA-006: Configuración por perfiles (dev / prod)

```properties
# application.properties (compartido)
spring.application.name=Furent

# application-dev.properties
server.port=8080
spring.data.mongodb.uri=mongodb://localhost:27017/FurentDataBase
spring.thymeleaf.cache=false
logging.level.com.alquiler.furent=DEBUG
furent.admin.password=admin123
furent.uploads.path=./uploads/

# application-prod.properties
server.port=${PORT:8080}
spring.data.mongodb.uri=${MONGODB_URI}
spring.thymeleaf.cache=true
logging.level.com.alquiler.furent=WARN
furent.admin.password=${ADMIN_PASSWORD}
furent.uploads.path=/var/furent/uploads/
```

Activar perfil:
```properties
# application.properties
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
```

---

### QA-007: Estructura de paquetes mejorada

```
com.alquiler.furent/
├── FurentApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── WebMvcConfig.java
│   ├── LocaleConfig.java
│   ├── AsyncConfig.java
│   └── DataInitializer.java
├── controller/
│   ├── PageController.java
│   ├── AdminController.java
│   ├── ApiController.java
│   ├── PaymentController.java
│   └── ReviewController.java
├── controller/api/          ← NUEVO: REST API separada
│   ├── ProductApiController.java
│   ├── AuthApiController.java
│   └── NotificationApiController.java
├── service/
│   ├── UserService.java
│   ├── ProductService.java
│   ├── ReservationService.java
│   ├── PaymentService.java
│   ├── ReviewService.java
│   ├── EmailService.java     ← NUEVO
│   ├── CouponService.java    ← NUEVO
│   ├── NotificationService.java ← NUEVO
│   ├── PdfService.java
│   └── AuditLogService.java
├── repository/
│   ├── UserRepository.java
│   ├── ProductRepository.java
│   ├── ReservationRepository.java
│   ├── PaymentRepository.java
│   ├── ReviewRepository.java
│   ├── CategoryRepository.java
│   ├── AuditLogRepository.java
│   ├── ContactMessageRepository.java  ← NUEVO
│   ├── CouponRepository.java          ← NUEVO
│   ├── NotificationRepository.java    ← NUEVO
│   └── PasswordResetTokenRepository.java ← NUEVO
├── model/
│   ├── User.java
│   ├── Product.java
│   ├── Reservation.java
│   ├── Category.java
│   ├── Payment.java
│   ├── Review.java
│   ├── AuditLog.java
│   ├── ContactMessage.java     ← NUEVO
│   ├── Coupon.java             ← NUEVO
│   ├── Notification.java       ← NUEVO
│   ├── PasswordResetToken.java ← NUEVO
│   └── StatusHistory.java      ← NUEVO
├── dto/
│   ├── CotizacionRequest.java
│   ├── ProductResponse.java    ← NUEVO
│   ├── UserResponse.java       ← NUEVO
│   ├── LoginRequest.java       ← NUEVO
│   └── RegisterRequest.java    ← NUEVO
├── enums/                      ← NUEVO
│   ├── EstadoReserva.java
│   ├── EstadoMantenimiento.java
│   ├── RolUsuario.java
│   ├── MetodoPago.java
│   └── EstadoPago.java
└── exception/
    ├── AccountSuspendedException.java
    ├── ResourceNotFoundException.java   ← NUEVO
    ├── InvalidOperationException.java   ← NUEVO
    ├── DuplicateResourceException.java  ← NUEVO
    └── GlobalExceptionHandler.java      ← NUEVO
```

---

## 6. 🧪 TESTING — De 0 Tests a Cobertura Completa

### TEST-001: Dependencias de testing

```xml
<!-- pom.xml (ya debería tener con starter-test) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>de.flapdoodle.embed</groupId>
    <artifactId>de.flapdoodle.embed.mongo.spring3x</artifactId>
    <version>4.11.0</version>
    <scope>test</scope>
</dependency>
```

---

### TEST-002: Tests unitarios de servicios

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;
    
    @Test
    void register_withNewEmail_shouldCreateUser() {
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        
        User result = userService.register("test@test.com", "pass", "Juan", "Test", "3001234567");
        
        assertNotNull(result);
        assertEquals("test@test.com", result.getEmail());
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void register_withExistingEmail_shouldThrowException() {
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);
        
        assertThrows(DuplicateResourceException.class, () -> 
            userService.register("existing@test.com", "pass", "Juan", "Test", "3001234567")
        );
    }
    
    @Test
    void loadUserByUsername_withSuspendedAccount_shouldThrow() {
        User user = new User();
        user.setEmail("suspended@test.com");
        user.setActivo(false);
        user.setRazonSuspension("Violación de términos");
        when(userRepository.findByEmail("suspended@test.com")).thenReturn(Optional.of(user));
        
        assertThrows(AccountSuspendedException.class, () ->
            userService.loadUserByUsername("suspended@test.com")
        );
    }
}
```

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock private ProductRepository productRepository;
    @InjectMocks private ProductService productService;
    
    @Test
    void getFeaturedProducts_shouldReturnTop6ByRating() { ... }
    
    @Test
    void getRelatedProducts_shouldExcludeCurrentProduct() { ... }
    
    @Test
    void getProductById_withInvalidId_shouldThrow() { ... }
}
```

```java
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    @Test void createReservation_shouldCalculateTotalCorrectly() { ... }
    @Test void updateStatus_withInvalidTransition_shouldThrow() { ... }
    @Test void calculateTotalRevenue_shouldOnlyCountCompletedAndActive() { ... }
}
```

---

### TEST-003: Tests de integración de controllers

```java
@SpringBootTest
@AutoConfigureMockMvc
class PageControllerIntegrationTest {
    @Autowired private MockMvc mockMvc;
    
    @Test
    void homePage_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andExpect(model().attributeExists("products", "categories"));
    }
    
    @Test
    void adminPage_withoutAuth_shouldRedirect() throws Exception {
        mockMvc.perform(get("/admin"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPage_withAdmin_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/admin"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/dashboard"));
    }
}
```

---

### TEST-004: Tests de repositorios

```java
@DataMongoTest
class ProductRepositoryTest {
    @Autowired private ProductRepository productRepository;
    
    @Test
    void findByCategoriaNombre_shouldReturnMatchingProducts() { ... }
    
    @Test
    void findByDisponibleTrue_shouldExcludeUnavailable() { ... }
}
```

---

### TEST-005: Tests de seguridad

```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {
    @Test void publicPages_shouldBeAccessible() { ... }
    @Test void protectedPages_shouldRequireAuth() { ... }
    @Test void adminPages_shouldRequireAdminRole() { ... }
    @Test void csrf_shouldProtectForms() { ... }
    @Test void suspendedUser_shouldNotLogin() { ... }
}
```

---

### TEST-006: Meta de cobertura

| Capa | Cobertura mínima |
|------|-----------------|
| Services | 90% |
| Controllers | 80% |
| Repositories | 70% |
| Models/DTOs | 60% |
| **Global** | **80%** |

Configurar JaCoCo:
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```

---

## 7. 🎨 FRONTEND — UX/UI de Nivel Profesional

### UI-001: Páginas de error estilizadas

Crear templates para:
- `error/404.html` — Ilustración + "Volver al inicio" + buscar productos
- `error/403.html` — Candado + "No tienes acceso"
- `error/500.html` — Disculpa + "Intentar de nuevo"
- `error/suspended.html` — Razón + duración + contacto soporte

---

### UI-002: Skeleton Loading States

En lugar de mostrar la página en blanco mientras carga:
```html
<!-- Skeleton para cards de producto -->
<div class="animate-pulse">
    <div class="bg-surface-200 h-48 rounded-xl"></div>
    <div class="mt-4 h-4 bg-surface-200 rounded w-3/4"></div>
    <div class="mt-2 h-4 bg-surface-200 rounded w-1/2"></div>
</div>
```

---

### UI-003: Dark Mode completo

Ya tienen el campo `apariencia` en User. Implementar:
```javascript
// Toggle dark mode
document.documentElement.classList.toggle('dark');

// Persistir en localStorage + backend
fetch('/configuracion/preferencias', {
    method: 'POST',
    body: new FormData(/* apariencia: 'dark' */)
});
```

En Tailwind 4:
```html
<html class="dark">
<!-- Usar dark: prefix en todos los componentes -->
<div class="bg-white dark:bg-surface-900 text-surface-900 dark:text-surface-50">
```

---

### UI-004: Animaciones mejoradas y micro-interacciones

```css
/* Hover en cards con elevación */
.product-card {
    @apply transition-all duration-300;
}
.product-card:hover {
    @apply -translate-y-2 shadow-xl;
}

/* Botón de agregar al carrito con feedback */
.btn-add-cart:active {
    @apply scale-95;
}

/* Números animados en dashboard */
@keyframes countUp {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
}
```

---

### UI-005: Componentes faltantes en el frontend

1. **Breadcrumbs:** Navegación contextual en catalog y product-detail
2. **Empty states:** Ilustraciones cuando no hay datos (sin reservas, sin favoritos)
3. **Confirmación de acciones destructivas:** Modal "¿Estás seguro?" antes de cancelar/eliminar
4. **Indicador de carga en formularios:** Spinner en botones al enviar
5. **Carousel de imágenes:** Múltiples fotos por producto (slider)
6. **Zoom de imagen:** Lightbox en product-detail
7. **Filtros pegajosos (sticky):** Sidebar de filtros que sigue al scroll en catálogo
8. **Tabla responsive:** Admin tables horizontally scrollable en mobile

---

### UI-006: Accesibilidad (WCAG 2.1 AA)

- [ ] Contraste de color mínimo 4.5:1 (verificar furent-500 naranja sobre blanco)
- [ ] `aria-label` en todos los botones de ícono
- [ ] `alt` descriptivo en todas las imágenes
- [ ] Navegación por teclado (Tab order, focus visible)
- [ ] `role="alert"` en toast notifications
- [ ] Skip to content link
- [ ] Form labels asociados con `for` attribute

---

### UI-007: Optimización de rendimiento frontend

```html
<!-- Lazy loading de imágenes -->
<img th:src="${product.imagenUrl}" loading="lazy" alt="..." />

<!-- Preconnect a CDNs -->
<link rel="preconnect" href="https://fonts.googleapis.com" />
<link rel="preconnect" href="https://cdn.tailwindcss.com" />

<!-- Prefetch de páginas probables -->
<link rel="prefetch" href="/catalogo" />
```

---

## 8. 🏗️ INFRAESTRUCTURA Y DEVOPS

### INFRA-001: Dockerización

```dockerfile
# Dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATA_MONGODB_URI=mongodb://mongo:27017/FurentDataBase
      - SPRING_PROFILES_ACTIVE=prod
    depends_on:
      - mongo
    volumes:
      - uploads:/app/uploads
  
  mongo:
    image: mongo:7
    ports:
      - "27017:27017"
    volumes:
      - mongo-data:/data/db

volumes:
  mongo-data:
  uploads:
```

---

### INFRA-002: CI/CD con GitHub Actions

```yaml
# .github/workflows/ci.yml
name: Furent CI/CD

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      mongo:
        image: mongo:7
        ports:
          - 27017:27017
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Cache Maven
        uses: actions/cache@v4
        with:
          path: ~/.m2
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
      - name: Run Tests
        run: ./mvnw verify
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v4
        with:
          file: target/site/jacoco/jacoco.xml

  build:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      - name: Build Docker image
        run: docker build -t furent:latest .
      - name: Deploy
        run: echo "Deploy a producción"
```

---

### INFRA-003: MongoDB Atlas (Producción)

```properties
# application-prod.properties
spring.data.mongodb.uri=${MONGODB_URI}
# Ejemplo: mongodb+srv://user:pass@cluster.mongodb.net/FurentDataBase?retryWrites=true
```

Pasos:
1. Crear cluster en MongoDB Atlas (free tier)
2. Crear usuario de base de datos
3. Whitelist IP o 0.0.0.0/0 para cloud deployments
4. Usar connection string en variable de entorno

---

### INFRA-004: Almacenamiento de archivos en la nube

Migrar de uploads locales a **Cloudinary** (gratis hasta 25 créditos/mes) o **AWS S3**:

```java
// service/FileStorageService.java
public interface FileStorageService {
    String upload(MultipartFile file, String folder);
    void delete(String publicId);
}

// service/CloudinaryStorageService.java
@Service
@Profile("prod")
public class CloudinaryStorageService implements FileStorageService {
    private final Cloudinary cloudinary;
    
    public String upload(MultipartFile file, String folder) {
        Map result = cloudinary.uploader().upload(file.getBytes(), 
            ObjectUtils.asMap("folder", "furent/" + folder));
        return (String) result.get("secure_url");
    }
}

// service/LocalStorageService.java
@Service
@Profile("dev")
public class LocalStorageService implements FileStorageService { ... }
```

---

### INFRA-005: Monitoreo y observabilidad

```properties
# application.properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=when-authorized
management.info.env.enabled=true

info.app.name=Furent
info.app.version=1.0.0
info.app.description=Plataforma de alquiler de mobiliarios
```

**Spring Boot Actuator** ya está en el pom.xml. Endpoints disponibles:
- `/actuator/health` — Estado del sistema y MongoDB
- `/actuator/metrics` — Métricas de JVM y requests
- `/actuator/info` — Info de la aplicación

---

## 9. 📖 DOCUMENTACIÓN PROFESIONAL

### DOC-001: README.md completo

```markdown
# 🪑 Furent — Plataforma de Alquiler de Mobiliarios

## Descripción
Furent es una plataforma web profesional para el alquiler de mobiliario...

## Tech Stack
- Java 17, Spring Boot 4.0.3, MongoDB
- Thymeleaf, Tailwind CSS 4
- Spring Security 6, BCrypt

## Requisitos
- Java 17+
- MongoDB 7+
- Maven 3.8+

## Instalación
git clone ...
cd furent
./mvnw spring-boot:run

## Variables de entorno
| Variable | Descripción | Default |
|----------|------------|---------|
| MONGODB_URI | Connection string | mongodb://localhost:27017/FurentDataBase |
| ADMIN_PASSWORD | Contraseña del admin | admin123 |
| MAIL_USERNAME | Email SMTP | - |
| MAIL_PASSWORD | Password SMTP | - |

## Usuarios de prueba
| Email | Password | Rol |
|-------|----------|-----|
| admin@furent.com | Admin_Furent_2026! | ADMIN |

## Endpoints principales
...

## Estructura del proyecto
...

## Screenshots
...

## Licencia
MIT
```

---

### DOC-002: JavaDoc en clases públicas

Todas las clases de servicio, controladores y modelos deben tener:
```java
/**
 * Servicio de gestión de usuarios.
 * Maneja registro, autenticación, suspensión y preferencias.
 * 
 * @author Furent Team
 * @since 1.0.0
 */
@Service
public class UserService implements UserDetailsService { ... }
```

---

### DOC-003: API Documentation con Swagger/OpenAPI

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

Accesible en: `/swagger-ui.html`

```java
@Operation(summary = "Crear cotización", description = "Crea una nueva cotización de alquiler")
@ApiResponse(responseCode = "200", description = "Cotización creada exitosamente")
@PostMapping("/api/cotizacion")
public ResponseEntity<?> createQuote(@Valid @RequestBody CotizacionRequest request) { ... }
```

---

### DOC-004: Diagramas de arquitectura

Crear con Mermaid en el README o docs separados:
- Diagrama de componentes (controllers → services → repos → MongoDB)
- Diagrama de entidad-relación (User ↔ Reservation ↔ Product)
- Diagrama de flujo de cotización
- Diagrama de máquina de estados de reserva

---

## 10. 💡 INNOVACIONES DIFERENCIADORAS

Estas funcionalidades van más allá de lo esperado y catapultan el proyecto a nivel profesional:

### INNOV-001: Generador de Presupuesto PDF Instantáneo
Desde el catálogo, el usuario arma su selección y puede descargar un PDF elegante con:
- Logo de Furent
- Lista de productos seleccionados con imagen miniatura
- Cálculo desglosado (subtotal, días, descuento si aplica, total)
- Datos de contacto de Furent
- Código QR que enlaza a la cotización online

---

### INNOV-002: Widget de Chat con WhatsApp Business
```html
<!-- Botón flotante de WhatsApp -->
<a href="https://wa.me/57XXXXXXXXX?text=Hola,%20me%20interesa%20cotizar%20mobiliario"
   class="fixed bottom-6 right-6 bg-green-500 text-white p-4 rounded-full shadow-xl hover:bg-green-600 z-50"
   target="_blank" rel="noopener">
    <svg><!-- WhatsApp icon --></svg>
</a>
```

---

### INNOV-003: Comparador de Productos
Permitir seleccionar 2-4 productos y ver tabla side-by-side:
```
| Característica       | Silla Chiavari | Silla Ghost  |
|---------------------|----------------|--------------|
| Material            | Madera/Metal   | Policarbonato|
| Precio/día          | $60,000        | $48,000      |
| Stock               | 200            | 150          |
| Calificación        | 4.8            | 4.6          |
```

---

### INNOV-004: Estimador de Costos Interactivo
Landing page con calculadora visual:
1. *¿Qué tipo de evento?* → Boda / Corporativo / Cumpleaños / Otro
2. *¿Cuántos invitados?* → Slider 10 - 500
3. *¿Qué necesitas?* → Checkboxes: Sillas, Mesas, Carpas, Decoración
4. *Resultado:* Estimación en tiempo real con rango de precios

---

### INNOV-005: Dashboard Admin con Gráficas Avanzadas (Chart.js)
- Gráfica de barras: Ingresos por mes (últimos 12 meses)
- Gráfica de pie: Distribución por tipo de evento
- Gráfica de línea: Tendencia de reservas semanales
- Gráfica de donut: Top 5 productos más alquilados
- Indicadores: Tasa de conversión (cotización → reserva), ticket promedio

---

### INNOV-006: Sistema de Paquetes Predefinidos
```java
@Document(collection = "paquetes")
public class Package {
    @Id private String id;
    private String nombre; // "Boda Clásica 100 personas"
    private String descripcion;
    private List<PackageItem> items; // Productos incluidos con cantidades
    private double precioTotal;
    private double descuento; // % off vs. individual
    private String imagenUrl;
    private String tipoEvento;
    private int personasMinimas;
    private int personasMaximas;
}
```
Mostrar en la home como "Paquetes Populares" con CTA directo.

---

### INNOV-007: Recomendaciones Inteligentes
- "Clientes que alquilaron esto también alquilaron..."
- "Completa tu evento" → Productos complementarios automáticos
- Basado en historial de reservas previas

---

### INNOV-008: QR en Contratos PDF
Cada contrato PDF tiene un código QR único que:
- Enlaza al estado en tiempo real de la reserva
- El equipo de logística puede escanearlo para confirmar entrega/recogida
- El cliente puede verificar autenticidad del contrato

---

### INNOV-009: Modo de Vista Previa de Evento
En la cotización, mostrar una vista previa visual:
- Render de distribución de mesas (grid visual)
- Foto de referencia de montaje similar
- "Así lucirá tu evento con 10 mesas redondas + 80 sillas Chiavari"

---

### INNOV-010: API Pública con Documentación
Para integraciones futuras (wedding planners, event agencies):
```
GET  /api/v1/productos?categoria=sillas&disponible=true
GET  /api/v1/productos/{id}
GET  /api/v1/categorias
POST /api/v1/cotizaciones
GET  /api/v1/cotizaciones/{id}/estado
```
Con API keys, rate limiting y documentación Swagger.

---

## 11. 📅 PLAN DE EJECUCIÓN POR FASES

### FASE 1: CRÍTICA — Corregir Bugs y Seguridad (Prioridad máxima)
```
□ BUG-001: Fix getRelatedProducts()
□ BUG-002: Fix estado mantenimiento
□ BUG-003: Habilitar CSRF
□ BUG-004: Validar fechas en cotización
□ BUG-005: Productos destacados por rating
□ BUG-006: countByEstado con query count
□ BUG-007: Validar tipo de archivo en uploads
□ SEC-001–008: Todas las mejoras de seguridad
□ QA-001: Crear enums para strings mágicos
□ QA-002: GlobalExceptionHandler
□ QA-003: Logging con SLF4J
```

### FASE 2: FUNCIONAL — Cerrar lo Incompleto
```
□ FEAT-001: Sistema de pagos completo
□ FEAT-002: Admin CRUD usuarios
□ FEAT-003: Admin CRUD categorías
□ FEAT-004: Gestión de reservas completa
□ FEAT-005: Formulario de contacto funcional
□ FEAT-006: Decidir GraphQL (implementar o eliminar)
□ FEAT-NEW-006: Paginación en todos los listados
```

### FASE 3: FEATURES — Nuevas Funcionalidades
```
□ FEAT-NEW-001: Emails transaccionales
□ FEAT-NEW-002: Recuperación de contraseña
□ FEAT-NEW-003: Búsqueda global
□ FEAT-NEW-004: Favoritos / Wishlist
□ FEAT-NEW-005: Notificaciones in-app
□ FEAT-NEW-007: Dashboard usuario mejorado
□ FEAT-NEW-008: Calendario visual
□ FEAT-NEW-009: Cupones y descuentos
□ FEAT-NEW-013: Verificación de email
```

### FASE 4: CALIDAD — Testing y Código
```
□ TEST-001–006: Suite completa de tests (80%+ coverage)
□ QA-004: Excepciones personalizadas
□ QA-005: DTOs para respuestas
□ QA-006: Perfiles dev/prod
□ QA-007: Reestructuración de paquetes
```

### FASE 5: INFRAESTRUCTURA
```
□ INFRA-001: Docker + docker-compose
□ INFRA-002: CI/CD GitHub Actions
□ INFRA-003: MongoDB Atlas
□ INFRA-004: Cloudinary para archivos
□ INFRA-005: Monitoreo con Actuator
```

### FASE 6: PRESENTACIÓN — Frontend + Docs
```
□ UI-001–007: Mejoras de UX/UI
□ DOC-001–004: Documentación completa
□ INNOV-001–010: Features diferenciadoras (seleccionar las que apliquen)
```

---

## 12. ✅ CHECKLIST FINAL 10/10

### Funcionalidad Completa
- [ ] Registro con verificación de email
- [ ] Login / logout con remember-me
- [ ] Recuperación de contraseña
- [ ] Catálogo con búsqueda, filtros y paginación
- [ ] Detalle de producto con calendario de disponibilidad
- [ ] Sistema de cotización multi-step
- [ ] Sistema de pagos funcional (al menos 3 métodos)
- [ ] Panel de usuario con historial completo
- [ ] Reseñas con rating
- [ ] Favoritos / wishlist
- [ ] Formulario de contacto funcional
- [ ] Notificaciones por email
- [ ] Notificaciones in-app

### Panel Admin Completo
- [ ] Dashboard con KPIs y gráficas avanzadas
- [ ] CRUD completo de productos con imágenes
- [ ] CRUD completo de categorías
- [ ] Gestión de usuarios (suspensión, roles, búsqueda)
- [ ] Gestión de reservas (estados, timeline, detalles)
- [ ] Gestión de pagos (confirmar, rechazar)
- [ ] Logística con calendario visual
- [ ] Mensajes de contacto
- [ ] Gestión de cupones
- [ ] Exportación de datos (CSV/PDF/Excel)
- [ ] Logs de auditoría completos
- [ ] Generación de PDFs (contratos, hojas de ruta, recibos)

### Seguridad
- [ ] CSRF habilitado para formularios
- [ ] Rate limiting en login y API
- [ ] Validación completa de inputs (@Valid en todos los DTOs)
- [ ] Headers de seguridad (CSP, HSTS, X-Frame-Options)
- [ ] Upload de archivos validado por tipo y tamaño
- [ ] Contraseña admin configurable por variable de entorno
- [ ] Sesiones seguras (HttpOnly, Secure, SameSite)
- [ ] Auditoría de todas las acciones sensibles
- [ ] Sin inyección NoSQL posible
- [ ] Sanitización de outputs en templates

### Calidad de Código
- [ ] 0 bugs conocidos
- [ ] Enums para todos los strings repetidos
- [ ] Excepciones personalizadas + handler global
- [ ] Logging profesional con SLF4J
- [ ] DTOs para todas las respuestas
- [ ] Perfiles dev/prod separados
- [ ] Variables de entorno para secrets
- [ ] Tests unitarios (80%+ coverage)
- [ ] Tests de integración para controllers
- [ ] Tests de seguridad

### UX/UI
- [ ] Responsive design (mobile-first)
- [ ] Dark mode funcional
- [ ] Skeleton loading
- [ ] Páginas de error estilizadas (404, 403, 500)
- [ ] Empty states con ilustraciones
- [ ] Breadcrumbs en navegación
- [ ] Confirmación de acciones destructivas
- [ ] Accesibilidad WCAG 2.1 AA
- [ ] Lazy loading de imágenes
- [ ] Animaciones suaves y micro-interacciones

### Infraestructura
- [ ] Dockerfile multi-stage
- [ ] docker-compose.yml (app + mongo)
- [ ] CI/CD con GitHub Actions
- [ ] MongoDB Atlas para producción
- [ ] Almacenamiento cloud para archivos
- [ ] Health checks / monitoring

### Documentación
- [ ] README.md profesional con setup, screenshots, API docs
- [ ] JavaDoc en clases públicas
- [ ] Swagger/OpenAPI para REST API
- [ ] Diagramas de arquitectura

### Innovación (Bonus)
- [ ] PDF de presupuesto instantáneo con QR
- [ ] Widget de WhatsApp Business
- [ ] Comparador de productos
- [ ] Estimador de costos interactivo
- [ ] Paquetes predefinidos para eventos
- [ ] Recomendaciones inteligentes

---

> **Cuando TODOS los checkboxes estén marcados, el proyecto estará al 10/10.**  
> No es solo código — es producto, seguridad, experiencia de usuario y profesionalismo.

---

*Generado por CODEX para el proyecto Furent — 7 de marzo de 2026*
