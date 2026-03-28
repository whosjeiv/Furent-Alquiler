# 🚀 FURENT — PLAN DE MEJORAS PROFESIONAL PARA LANZAMIENTO

> **Análisis Completo y Roadmap Estratégico**  
> **Fecha:** 27 de marzo de 2026  
> **Calificación Actual:** 6.8/10  
> **Objetivo:** 10/10 — Producto listo para mercado competitivo

---

## 📊 RESUMEN EJECUTIVO

### Calificación Actual por Área

| Área | Puntaje | Estado |
|------|---------|--------|
| **Arquitectura** | 8.5/10 | ✅ Excelente |
| **Seguridad** | 7.0/10 | ⚠️ Mejorable |
| **Funcionalidades** | 6.0/10 | ⚠️ Incompletas |
| **Testing** | 2.0/10 | 🔴 Crítico |
| **UX/UI** | 6.5/10 | ⚠️ Mejorable |
| **DevOps** | 8.0/10 | ✅ Bueno |
| **Documentación** | 8.5/10 | ✅ Excelente |
| **Código** | 6.5/10 | ⚠️ Mejorable |

**Promedio General: 6.8/10**

### Fortalezas Destacadas

✅ **Arquitectura multi-tenant robusta** con MongoDB + Redis  
✅ **Sistema de eventos** bien implementado  
✅ **Rate limiting** y protección anti-brute force  
✅ **Documentación técnica** completa (ARCHITECTURE.md, UML)  
✅ **Docker + Kubernetes** listos para producción  
✅ **Observabilidad** con Prometheus + Grafana  
✅ **Máquina de estados** para reservas con validaciones  
✅ **Anti-overbooking** con cálculo de disponibilidad real

### Debilidades Críticas

🔴 **Sin tests** (0% cobertura)  
🔴 **CSRF deshabilitado** en formularios web  
🔴 **Bugs críticos** en ProductService y validaciones  
🔴 **Funcionalidades incompletas** (pagos, recuperación contraseña)  
🔴 **UX básica** sin feedback visual moderno  
🔴 **Sin CI/CD** automatizado

---

## 🎯 PLAN DE ACCIÓN ESTRATÉGICO


### Fase 1: CRÍTICO (1-2 semanas) — Bugs y Seguridad

**Objetivo:** Eliminar bugs bloqueantes y cerrar brechas de seguridad

| Prioridad | Tarea | Impacto | Esfuerzo |
|-----------|-------|---------|----------|
| 🔴 P0 | Habilitar CSRF en formularios web | Alto | 2h |
| 🔴 P0 | Fix bug getRelatedProducts() | Alto | 30min |
| 🔴 P0 | Fix validación estado mantenimiento | Alto | 30min |
| 🔴 P0 | Validar tipos de archivo en uploads | Alto | 1h |
| 🔴 P0 | Validar fechas en cotización | Medio | 1h |
| 🟡 P1 | Implementar recuperación de contraseña | Alto | 4h |
| 🟡 P1 | Agregar headers de seguridad HTTP | Medio | 2h |
| 🟡 P1 | Cambiar password admin por defecto | Medio | 1h |

**Entregables:**
- Sistema sin bugs críticos
- CSRF habilitado con tokens
- Uploads seguros (solo imágenes)
- Password reset funcional
- Headers de seguridad (HSTS, CSP)

---

### Fase 2: FUNCIONALIDADES CORE (2-3 semanas) — Completar MVP

**Objetivo:** Cerrar funcionalidades incompletas para MVP funcional

| Prioridad | Tarea | Impacto | Esfuerzo |
|-----------|-------|---------|----------|
| 🔴 P0 | Sistema de pagos completo | Crítico | 12h |
| 🔴 P0 | Notificaciones por email | Alto | 8h |
| 🟡 P1 | Admin CRUD usuarios completo | Alto | 6h |
| 🟡 P1 | Admin CRUD categorías | Medio | 4h |
| 🟡 P1 | Formulario de contacto funcional | Medio | 3h |
| 🟡 P1 | Sistema de cupones/descuentos | Alto | 6h |
| 🟢 P2 | Verificación de email | Medio | 4h |
| 🟢 P2 | Sistema de favoritos | Bajo | 3h |

**Entregables:**
- Flujo de pagos end-to-end
- Emails transaccionales (bienvenida, confirmaciones)
- Admin puede gestionar usuarios y categorías
- Cupones de descuento aplicables
- Formulario de contacto operativo

---

### Fase 3: TESTING (2 semanas) — De 0% a 70% cobertura

**Objetivo:** Implementar suite de tests completa

| Tipo | Cobertura Objetivo | Esfuerzo |
|------|-------------------|----------|
| **Tests Unitarios** | 70% | 20h |
| **Tests de Integración** | 50% | 16h |
| **Tests E2E** | Flujos críticos | 12h |

**Tests Prioritarios:**

1. **Servicios Core** (unitarios)
   - ReservationService: validación disponibilidad, transiciones estado
   - PaymentService: flujo completo de pago
   - UserService: registro, suspensión, autenticación
   - ProductService: búsqueda, cache

2. **Controladores** (integración)
   - AuthController: login, register, refresh token
   - ApiController: cotización, favoritos, cupones
   - AdminController: CRUD productos, reservas

3. **Seguridad** (integración)
   - Rate limiting funciona
   - CSRF tokens válidos
   - JWT expiración y refresh

4. **E2E** (Selenium/Playwright)
   - Flujo completo: registro → búsqueda → cotización → pago
   - Admin: crear producto → confirmar reserva → confirmar pago

**Entregables:**
- 70% cobertura de código
- CI/CD con tests automáticos
- Reporte de cobertura (JaCoCo)

---

### Fase 4: UX/UI PROFESIONAL (2 semanas) — Modernizar interfaz

**Objetivo:** Elevar experiencia de usuario a nivel competitivo


**Mejoras UX:**

1. **Feedback Visual Moderno**
   - Loading spinners en operaciones async
   - Toasts/notificaciones (SweetAlert2 ya incluido)
   - Skeleton loaders en catálogo
   - Progress bars en formularios multi-paso

2. **Interactividad Mejorada**
   - Búsqueda con autocompletado
   - Filtros en tiempo real (categoría, precio, disponibilidad)
   - Calendario visual de disponibilidad (FullCalendar)
   - Drag & drop para imágenes en admin

3. **Responsive Avanzado**
   - Mobile-first design
   - Menú hamburguesa animado
   - Cards adaptativas
   - Tablas responsivas con scroll horizontal

4. **Microinteracciones**
   - Hover effects en productos
   - Animaciones de transición (Tailwind transitions)
   - Badge de notificaciones con contador
   - Confirmaciones visuales (checkmarks animados)

5. **Dashboard Mejorado**
   - Gráficos interactivos (Chart.js)
   - KPIs con iconos y colores
   - Timeline de reservas
   - Heatmap de disponibilidad

**Entregables:**
- UI moderna y profesional
- Experiencia fluida en mobile
- Feedback visual en todas las acciones
- Dashboard admin con métricas visuales

---

### Fase 5: INNOVACIÓN (2-3 semanas) — Diferenciadores competitivos

**Objetivo:** Agregar funcionalidades que te destaquen en el mercado

**Innovaciones Clave:**

1. **IA/ML para Recomendaciones**
   ```java
   // Recomendaciones basadas en:
   // - Historial de reservas del usuario
   // - Productos frecuentemente alquilados juntos
   // - Tendencias por tipo de evento
   public List<Product> getRecommendations(String userId, String eventType)
   ```

2. **Cotización Inteligente**
   - Sugerencias automáticas de productos complementarios
   - Alertas de disponibilidad limitada
   - Descuentos por volumen automáticos
   - Comparador de paquetes

3. **Logística Optimizada**
   - Rutas de entrega optimizadas (Google Maps API)
   - Asignación automática de vehículos
   - Tracking en tiempo real
   - Notificaciones push al cliente

4. **Analytics Avanzado**
   - Predicción de demanda (ML)
   - Productos más rentables
   - Análisis de churn de clientes
   - Reportes personalizables

5. **Integración WhatsApp Business**
   - Notificaciones por WhatsApp
   - Chatbot para consultas
   - Confirmaciones de pago
   - Recordatorios de eventos

6. **Sistema de Fidelización**
   - Puntos por reserva
   - Niveles (Bronce, Plata, Oro)
   - Descuentos exclusivos
   - Referidos con recompensa

**Entregables:**
- Sistema de recomendaciones IA
- Logística con rutas optimizadas
- WhatsApp Business integrado
- Programa de fidelización

---

## 🔧 DETALLES TÉCNICOS POR ÁREA

### 1. BUGS CRÍTICOS A CORREGIR

#### BUG-001: getRelatedProducts() rompe la página

**Archivo:** `ProductService.java:88`

```java
// ❌ ANTES (ROTO):
.filter(p -> p.getCategory().equals(product.getCategory()))

// ✅ DESPUÉS (CORRECTO):
.filter(p -> p.getCategoriaNombre().equals(product.getCategoriaNombre()))
```

**Impacto:** RuntimeException al ver detalle de producto  
**Prioridad:** 🔴 P0  
**Esfuerzo:** 30 minutos

---

#### BUG-002: Estado de mantenimiento nunca coincide

**Archivo:** `AdminProductosController.java`

```java
// ❌ ANTES:
boolean available = stock > 0 && "OPERATIVO".equals(estadoMantenimiento);

// ✅ DESPUÉS:
boolean available = stock > 0 && !"EN_REPARACION".equals(estadoMantenimiento);
```

**Impacto:** Productos siempre marcados como no disponibles  
**Prioridad:** 🔴 P0  
**Esfuerzo:** 30 minutos

---

#### BUG-003: CSRF deshabilitado en formularios

**Archivo:** `SecurityConfig.java:95`

```java
// ❌ ANTES:
.csrf(csrf -> csrf.disable())

// ✅ DESPUÉS:
.csrf(csrf -> csrf
    .ignoringRequestMatchers("/api/**") // Solo API REST sin CSRF
)
```

**Impacto:** Vulnerabilidad CSRF en formularios web  
**Prioridad:** 🔴 P0  
**Esfuerzo:** 2 horas (incluye agregar tokens en templates)

**Templates a actualizar:**
```html
<!-- Agregar en todos los forms Thymeleaf -->
<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
```

---

#### BUG-004: Cotización acepta fechas inválidas

**Archivo:** `ApiController.java`

```java
// Agregar validación ANTES de crear reserva:
if (request.getFechaFin().isBefore(request.getFechaInicio())) {
    return ResponseEntity.badRequest().body(Map.of(
        "success", false,
        "message", "La fecha de fin no puede ser anterior a la fecha de inicio"
    ));
}
```

**Prioridad:** 🔴 P0  
**Esfuerzo:** 1 hora

---

#### BUG-005: Upload sin validación de tipo

**Archivo:** `AdminProductosController.java`

```java
private static final Set<String> ALLOWED_TYPES = Set.of(
    "image/jpeg", "image/png", "image/webp", "image/gif"
);

// En el método de upload:
if (!ALLOWED_TYPES.contains(file.getContentType())) {
    redirectAttributes.addFlashAttribute("error", 
        "Solo se permiten imágenes (JPG, PNG, WebP, GIF)");
    return "redirect:/admin/productos";
}

// Validar tamaño máximo (5MB):
if (file.getSize() > 5 * 1024 * 1024) {
    redirectAttributes.addFlashAttribute("error", 
        "La imagen no puede superar 5MB");
    return "redirect:/admin/productos";
}
```

**Impacto:** Riesgo de ejecución remota de código  
**Prioridad:** 🔴 P0  
**Esfuerzo:** 1 hora

---

### 2. SEGURIDAD — Blindaje Completo

#### SEC-001: Headers de Seguridad HTTP

**Archivo:** `SecurityConfig.java`

```java
.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp
        .policyDirectives(
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com https://cdn.jsdelivr.net; " +
            "style-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com https://fonts.googleapis.com; " +
            "font-src 'self' https://fonts.gstatic.com; " +
            "img-src 'self' data: blob: https:; " +
            "connect-src 'self';"
        ))
    .frameOptions(frame -> frame.deny())
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000)
        .preload(true))
    .xssProtection(xss -> xss.headerValue("1; mode=block"))
    .contentTypeOptions(cto -> cto.disable())
)
```

**Prioridad:** 🟡 P1  
**Esfuerzo:** 2 horas

---

#### SEC-002: Password Admin Configurable

**Archivo:** `DataInitializer.java`

```java
@Value("${furent.admin.password:${random.uuid}}")
private String adminPassword;

// En run():
if (userRepository.findByEmail("admin@furent.com").isEmpty()) {
    User admin = new User();
    admin.setEmail("admin@furent.com");
    admin.setPassword(passwordEncoder.encode(adminPassword));
    admin.setRole(RolUsuario.ADMIN);
    userRepository.save(admin);
    
    log.warn("═══════════════════════════════════════");
    log.warn("Admin creado con password: {}", adminPassword);
    log.warn("CAMBIAR INMEDIATAMENTE EN PRODUCCIÓN");
    log.warn("═══════════════════════════════════════");
}
```

```properties
# application-prod.properties
furent.admin.password=${FURENT_ADMIN_PASSWORD}
```

**Prioridad:** 🟡 P1  
**Esfuerzo:** 1 hora

---

#### SEC-003: Validación y Sanitización de Entradas

**Crear:** `dto/CotizacionRequest.java`

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
    
    @NotBlank
    @Size(max = 500)
    private String direccion;
    
    @Size(max = 1000)
    private String notas;
    
    @NotEmpty(message = "Debe seleccionar al menos un producto")
    @Valid
    private List<CartItem> items;
    
    @AssertTrue(message = "La fecha de fin debe ser posterior a la de inicio")
    public boolean isFechaFinValid() {
        if (fechaInicio == null || fechaFin == null) return true;
        return !fechaFin.isBefore(fechaInicio);
    }
}
```

**Prioridad:** 🟡 P1  
**Esfuerzo:** 4 horas

---

### 3. FUNCIONALIDADES CORE

#### FEAT-001: Sistema de Pagos Completo

**Crear:** `PaymentService.java` (expandir el existente)

```java
@Service
public class PaymentService {
    
    public Payment initPayment(String reservaId, String userId, MetodoPago metodo) {
        Reservation reservation = reservationService.getByIdOrThrow(reservaId);
        
        // Validar que la reserva esté CONFIRMADA
        if (!EstadoReserva.CONFIRMADA.name().equals(reservation.getEstado())) {
            throw new InvalidOperationException(
                "Solo se pueden pagar reservas confirmadas");
        }
        
        Payment payment = new Payment();
        payment.setReservaId(reservaId);
        payment.setUsuarioId(userId);
        payment.setMonto(reservation.getTotal());
        payment.setMetodoPago(metodo.name());
        payment.setEstado(EstadoPago.PENDIENTE.name());
        payment.setReferencia(generateReference());
        
        Payment saved = paymentRepository.save(payment);
        
        // Notificar usuario
        notificationService.create(userId, 
            "Pago Registrado", 
            "Tu pago de " + formatMoney(saved.getMonto()) + " ha sido registrado",
            "/panel/pagos/" + saved.getId());
        
        return saved;
    }
    
    public Payment confirmPayment(String paymentId, String referencia, String admin) {
        Payment payment = getByIdOrThrow(paymentId);
        
        if (!EstadoPago.PENDIENTE.name().equals(payment.getEstado())) {
            throw new InvalidOperationException("El pago ya fue procesado");
        }
        
        payment.setEstado(EstadoPago.PAGADO.name());
        payment.setReferencia(referencia);
        payment.setFechaPago(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);
        
        // Cambiar reserva a ACTIVA
        reservationService.updateStatus(payment.getReservaId(), 
            EstadoReserva.ENTREGADA.name(), admin, 
            "Pago confirmado: " + referencia);
        
        // Métricas
        metricsConfig.getPaymentsCompleted().increment();
        metricsConfig.addRevenue(payment.getMonto());
        
        // Notificar usuario
        notificationService.create(payment.getUsuarioId(),
            "Pago Confirmado ✅",
            "Tu pago ha sido confirmado. Ref: " + referencia,
            "/panel/reservas/" + payment.getReservaId());
        
        // Email
        emailService.sendPaymentConfirmation(saved);
        
        // Evento
        eventPublisher.publish(new PaymentCompletedEvent(this, saved));
        
        return saved;
    }
    
    public Payment failPayment(String paymentId, String reason, String admin) {
        Payment payment = getByIdOrThrow(paymentId);
        payment.setEstado(EstadoPago.FALLIDO.name());
        Payment saved = paymentRepository.save(payment);
        
        metricsConfig.getPaymentsFailed().increment();
        
        notificationService.create(payment.getUsuarioId(),
            "Pago Rechazado",
            "Tu pago fue rechazado: " + reason,
            "/panel/pagos/" + paymentId);
        
        auditLogService.log("PAYMENT_REJECTED", "Payment", paymentId, 
            admin, "Razón: " + reason);
        
        return saved;
    }
    
    private String generateReference() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
```

**Endpoints:** `PaymentController.java`

```java
@RestController
@RequestMapping("/api/pagos")
public class PaymentController {
    
    @PostMapping("/iniciar/{reservaId}")
    public ResponseEntity<?> initPayment(
            @PathVariable String reservaId,
            @RequestParam MetodoPago metodo,
            Principal principal) {
        Payment payment = paymentService.initPayment(reservaId, 
            principal.getName(), metodo);
        return ResponseEntity.ok(payment);
    }
    
    @GetMapping("/mis-pagos")
    public ResponseEntity<List<Payment>> myPayments(Principal principal) {
        return ResponseEntity.ok(
            paymentService.getByUsuarioId(principal.getName()));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.getByIdOrThrow(id));
    }
}
```

**Admin endpoints:** `AdminPagosController.java`

```java
@PostMapping("/admin/pagos/{id}/confirmar")
public String confirmPayment(
        @PathVariable String id,
        @RequestParam String referencia,
        Principal principal,
        RedirectAttributes redirectAttributes) {
    try {
        paymentService.confirmPayment(id, referencia, principal.getName());
        redirectAttributes.addFlashAttribute("success", 
            "Pago confirmado correctamente");
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/admin/pagos";
}

@PostMapping("/admin/pagos/{id}/rechazar")
public String failPayment(
        @PathVariable String id,
        @RequestParam String razon,
        Principal principal,
        RedirectAttributes redirectAttributes) {
    paymentService.failPayment(id, razon, principal.getName());
    redirectAttributes.addFlashAttribute("info", "Pago rechazado");
    return "redirect:/admin/pagos";
}
```

**Prioridad:** 🔴 P0  
**Esfuerzo:** 12 horas

---

#### FEAT-002: Notificaciones por Email

**Crear:** `EmailService.java`

```java
@Service
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Async
    public void sendWelcomeEmail(User user) {
        Context context = new Context();
        context.setVariable("nombre", user.getNombre());
        context.setVariable("email", user.getEmail());
        
        String html = templateEngine.process("email/bienvenida", context);
        
        sendHtmlEmail(user.getEmail(), 
            "Bienvenido a Furent 🎉", 
            html);
    }
    
    @Async
    public void sendReservationConfirmation(Reservation reservation) {
        User user = userService.getById(reservation.getUsuarioId())
            .orElseThrow();
        
        Context context = new Context();
        context.setVariable("reservation", reservation);
        context.setVariable("user", user);
        
        String html = templateEngine.process("email/confirmacion-reserva", context);
        
        sendHtmlEmail(user.getEmail(),
            "Reserva Confirmada - " + reservation.getId(),
            html);
    }
    
    @Async
    public void sendStatusChange(Reservation reservation, String oldStatus) {
        User user = userService.getById(reservation.getUsuarioId())
            .orElseThrow();
        
        Context context = new Context();
        context.setVariable("reservation", reservation);
        context.setVariable("oldStatus", oldStatus);
        context.setVariable("newStatus", reservation.getEstado());
        
        String html = templateEngine.process("email/cambio-estado", context);
        
        sendHtmlEmail(user.getEmail(),
            "Actualización de Reserva - " + reservation.getId(),
            html);
    }
    
    @Async
    public void sendPasswordResetToken(User user, String token) {
        Context context = new Context();
        context.setVariable("nombre", user.getNombre());
        context.setVariable("resetLink", 
            "https://furent.com/password-reset/" + token);
        
        String html = templateEngine.process("email/reset-password", context);
        
        sendHtmlEmail(user.getEmail(),
            "Recuperación de Contraseña",
            html);
    }
    
    @Async
    public void sendPaymentConfirmation(Payment payment) {
        Reservation reservation = reservationService.getById(payment.getReservaId())
            .orElseThrow();
        User user = userService.getById(payment.getUsuarioId())
            .orElseThrow();
        
        Context context = new Context();
        context.setVariable("payment", payment);
        context.setVariable("reservation", reservation);
        context.setVariable("user", user);
        
        String html = templateEngine.process("email/recibo-pago", context);
        
        sendHtmlEmail(user.getEmail(),
            "Recibo de Pago - " + payment.getReferencia(),
            html);
    }
    
    private void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, "Furent");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            
            mailSender.send(message);
            log.info("Email enviado a {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Error enviando email a {}", to, e);
        }
    }
}
```

**Templates:** `templates/email/bienvenida.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); 
                  color: white; padding: 30px; text-align: center; }
        .content { background: #f9fafb; padding: 30px; }
        .button { display: inline-block; padding: 12px 24px; 
                  background: #667eea; color: white; text-decoration: none; 
                  border-radius: 6px; margin-top: 20px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>¡Bienvenido a Furent!</h1>
        </div>
        <div class="content">
            <p>Hola <strong th:text="${nombre}">Usuario</strong>,</p>
            <p>Gracias por registrarte en Furent, tu plataforma de alquiler de mobiliarios para eventos.</p>
            <p>Ahora puedes:</p>
            <ul>
                <li>Explorar nuestro catálogo completo</li>
                <li>Solicitar cotizaciones instantáneas</li>
                <li>Gestionar tus reservas</li>
                <li>Recibir notificaciones en tiempo real</li>
            </ul>
            <a href="https://furent.com/catalogo" class="button">Explorar Catálogo</a>
        </div>
    </div>
</body>
</html>
```

**Configuración:** `application.properties`

```properties
# Gmail SMTP
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

**Prioridad:** 🔴 P0  
**Esfuerzo:** 8 horas

---


#### FEAT-003: Recuperación de Contraseña

**Modelo:** `PasswordResetToken.java`

```java
@Document(collection = "password_reset_tokens")
@Data
public class PasswordResetToken {
    @Id
    private String id;
    private String tenantId;
    private String userId;
    private String token; // UUID
    private LocalDateTime expiresAt; // +1 hora
    private boolean used;
    private LocalDateTime createdAt;
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isValid() {
        return !used && !isExpired();
    }
}
```

**Servicio:** `PasswordResetService.java`

```java
@Service
public class PasswordResetService {
    
    public PasswordResetToken createToken(String userId) {
        // Invalidar tokens anteriores
        tokenRepository.findByUserId(userId)
            .forEach(t -> {
                t.setUsed(true);
                tokenRepository.save(t);
            });
        
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(userId);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setCreatedAt(LocalDateTime.now());
        
        return tokenRepository.save(token);
    }
    
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
            .orElseThrow(() -> new ResourceNotFoundException("Token", token));
        
        if (!resetToken.isValid()) {
            throw new InvalidOperationException(
                "El token ha expirado o ya fue usado");
        }
        
        User user = userService.getByIdOrThrow(resetToken.getUserId());
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.save(user);
        
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
        
        log.info("Contraseña reseteada para usuario {}", user.getEmail());
    }
}
```

**Controller:**

```java
@GetMapping("/password-reset")
public String passwordResetForm() {
    return "password-reset-request";
}

@PostMapping("/password-reset")
public String requestPasswordReset(@RequestParam String email,
        RedirectAttributes redirectAttributes) {
    Optional<User> userOpt = userService.findByEmail(email);
    if (userOpt.isEmpty()) {
        // No revelar si el email existe (seguridad)
        redirectAttributes.addFlashAttribute("info",
            "Si el email existe, recibirás instrucciones");
        return "redirect:/password-reset";
    }
    
    User user = userOpt.get();
    PasswordResetToken token = passwordResetService.createToken(user.getId());
    emailService.sendPasswordResetToken(user, token.getToken());
    
    redirectAttributes.addFlashAttribute("success",
        "Revisa tu email para continuar");
    return "redirect:/login";
}

@GetMapping("/password-reset/{token}")
public String passwordResetConfirmForm(@PathVariable String token, Model model) {
    // Validar que el token existe y es válido
    PasswordResetToken resetToken = tokenRepository.findByToken(token)
        .orElseThrow(() -> new ResourceNotFoundException("Token", token));
    
    if (!resetToken.isValid()) {
        model.addAttribute("error", "El token ha expirado o es inválido");
        return "error/token-expired";
    }
    
    model.addAttribute("token", token);
    return "password-reset-confirm";
}

@PostMapping("/password-reset/confirm")
public String confirmPasswordReset(
        @RequestParam String token,
        @RequestParam String password,
        @RequestParam String passwordConfirm,
        RedirectAttributes redirectAttributes) {
    
    if (!password.equals(passwordConfirm)) {
        redirectAttributes.addFlashAttribute("error", 
            "Las contraseñas no coinciden");
        return "redirect:/password-reset/" + token;
    }
    
    passwordResetService.resetPassword(token, password);
    
    redirectAttributes.addFlashAttribute("success",
        "Contraseña cambiada correctamente");
    return "redirect:/login";
}
```

**Prioridad:** 🟡 P1  
**Esfuerzo:** 4 horas

---

### 4. TESTING — Suite Completa

#### TEST-001: Tests Unitarios (Servicios)

**Ejemplo:** `ReservationServiceTest.java`

```java
@SpringBootTest
@Testcontainers
class ReservationServiceTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = 
        new MongoDBContainer("mongo:7");
    
    @Autowired
    private ReservationService reservationService;
    
    @Autowired
    private ProductRepository productRepository;
    
    @BeforeEach
    void setUp() {
        // Limpiar DB
        reservationRepository.deleteAll();
        productRepository.deleteAll();
    }
    
    @Test
    void testValidateAvailability_SufficientStock_ReturnsNoErrors() {
        // Given
        Product product = createProduct("Silla", 10);
        productRepository.save(product);
        
        Reservation reservation = new Reservation();
        reservation.setFechaInicio(LocalDate.now().plusDays(1));
        reservation.setFechaFin(LocalDate.now().plusDays(3));
        reservation.setItems(List.of(
            createItem(product.getId(), "Silla", 5)
        ));
        
        // When
        Map<String, String> errors = reservationService.validateAvailability(reservation);
        
        // Then
        assertThat(errors).isEmpty();
    }
    
    @Test
    void testValidateAvailability_InsufficientStock_ReturnsError() {
        // Given
        Product product = createProduct("Silla", 10);
        productRepository.save(product);
        
        // Reserva existente que ocupa 8 sillas
        Reservation existing = new Reservation();
        existing.setEstado(EstadoReserva.CONFIRMADA.name());
        existing.setFechaInicio(LocalDate.now().plusDays(1));
        existing.setFechaFin(LocalDate.now().plusDays(3));
        existing.setItems(List.of(createItem(product.getId(), "Silla", 8)));
        reservationRepository.save(existing);
        
        // Nueva reserva que pide 5 sillas (total 13 > 10 stock)
        Reservation newReservation = new Reservation();
        newReservation.setFechaInicio(LocalDate.now().plusDays(2));
        newReservation.setFechaFin(LocalDate.now().plusDays(4));
        newReservation.setItems(List.of(createItem(product.getId(), "Silla", 5)));
        
        // When
        Map<String, String> errors = reservationService.validateAvailability(newReservation);
        
        // Then
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(product.getId()))
            .contains("solo hay 2 disponibles");
    }
    
    @Test
    void testUpdateStatus_ValidTransition_Success() {
        // Given
        Reservation reservation = new Reservation();
        reservation.setEstado(EstadoReserva.PENDIENTE.name());
        Reservation saved = reservationRepository.save(reservation);
        
        // When
        reservationService.updateStatus(saved.getId(), 
            EstadoReserva.CONFIRMADA.name(), "admin", "Aprobado");
        
        // Then
        Reservation updated = reservationRepository.findById(saved.getId()).get();
        assertThat(updated.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA.name());
    }
    
    @Test
    void testUpdateStatus_InvalidTransition_ThrowsException() {
        // Given
        Reservation reservation = new Reservation();
        reservation.setEstado(EstadoReserva.COMPLETADA.name());
        Reservation saved = reservationRepository.save(reservation);
        
        // When/Then
        assertThatThrownBy(() -> 
            reservationService.updateStatus(saved.getId(), 
                EstadoReserva.PENDIENTE.name()))
            .isInstanceOf(InvalidOperationException.class)
            .hasMessageContaining("Transición no válida");
    }
}
```

**Prioridad:** 🔴 P0  
**Esfuerzo:** 20 horas (todos los servicios)

---

#### TEST-002: Tests de Integración (Controllers)

**Ejemplo:** `AuthControllerIntegrationTest.java`

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }
    
    @Test
    void testLogin_ValidCredentials_ReturnsJWT() throws Exception {
        // Given
        User user = new User();
        user.setEmail("test@furent.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(RolUsuario.USER);
        userRepository.save(user);
        
        // When/Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "test@furent.com",
                        "password": "password123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.user.email").value("test@furent.com"));
    }
    
    @Test
    void testLogin_InvalidPassword_Returns401() throws Exception {
        // Given
        User user = new User();
        user.setEmail("test@furent.com");
        user.setPassword(passwordEncoder.encode("password123"));
        userRepository.save(user);
        
        // When/Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "test@furent.com",
                        "password": "wrongpassword"
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testRegister_ValidData_CreatesUser() throws Exception {
        // When/Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "nuevo@furent.com",
                        "password": "Password123!",
                        "nombre": "Juan",
                        "apellido": "Pérez",
                        "telefono": "3001234567"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists());
        
        // Verify user was created
        Optional<User> created = userRepository.findByEmail("nuevo@furent.com");
        assertThat(created).isPresent();
        assertThat(created.get().getNombre()).isEqualTo("Juan");
    }
}
```

**Prioridad:** 🔴 P0  
**Esfuerzo:** 16 horas

---

#### TEST-003: Tests E2E (Selenium/Playwright)

**Ejemplo:** `ReservationFlowE2ETest.java`

```java
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
class ReservationFlowE2ETest {
    
    private WebDriver driver;
    
    @BeforeEach
    void setUp() {
        driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }
    
    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
    
    @Test
    void testCompleteReservationFlow() {
        // 1. Ir a la página de inicio
        driver.get("http://localhost:8080");
        assertThat(driver.getTitle()).contains("Furent");
        
        // 2. Ir al catálogo
        driver.findElement(By.linkText("Catálogo")).click();
        assertThat(driver.getCurrentUrl()).contains("/catalogo");
        
        // 3. Buscar producto
        WebElement searchBox = driver.findElement(By.id("search"));
        searchBox.sendKeys("Silla");
        searchBox.submit();
        
        // 4. Seleccionar primer producto
        driver.findElement(By.cssSelector(".product-card")).click();
        assertThat(driver.getCurrentUrl()).contains("/producto/");
        
        // 5. Agregar al carrito
        driver.findElement(By.id("cantidad")).sendKeys("5");
        driver.findElement(By.id("add-to-cart")).click();
        
        // 6. Ir a cotización
        driver.findElement(By.linkText("Cotizar")).click();
        assertThat(driver.getCurrentUrl()).contains("/cotizacion");
        
        // 7. Llenar formulario
        driver.findElement(By.id("tipoEvento")).sendKeys("Boda");
        driver.findElement(By.id("fechaInicio")).sendKeys("2026-04-01");
        driver.findElement(By.id("fechaFin")).sendKeys("2026-04-03");
        driver.findElement(By.id("direccion")).sendKeys("Calle 123");
        
        // 8. Enviar cotización
        driver.findElement(By.id("submit-cotizacion")).click();
        
        // 9. Verificar confirmación
        WebElement success = driver.findElement(By.className("alert-success"));
        assertThat(success.getText()).contains("Cotización creada");
    }
}
```

**Prioridad:** 🟡 P1  
**Esfuerzo:** 12 horas

---

### 5. CI/CD AUTOMATIZADO

**Crear:** `.github/workflows/ci.yml`

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      mongodb:
        image: mongo:7
        ports:
          - 27017:27017
      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven
    
    - name: Run tests
      run: ./mvnw clean test
    
    - name: Generate coverage report
      run: ./mvnw jacoco:report
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v4
      with:
        file: ./target/site/jacoco/jacoco.xml
    
    - name: Build JAR
      run: ./mvnw package -DskipTests
    
    - name: Build Docker image
      run: docker build -t furent:${{ github.sha }} .
    
    - name: Run security scan
      uses: aquasecurity/trivy-action@master
      with:
        image-ref: furent:${{ github.sha }}
        format: 'sarif'
        output: 'trivy-results.sarif'
    
    - name: Upload security scan results
      uses: github/codeql-action/upload-sarif@v3
      with:
        sarif_file: 'trivy-results.sarif'

  deploy:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
    - name: Deploy to production
      run: |
        echo "Deploying to Kubernetes..."
        # kubectl apply -f k8s/
```

**Prioridad:** 🟡 P1  
**Esfuerzo:** 4 horas

---

### 6. MEJORAS UX/UI

#### UI-001: Loading States y Feedback Visual

**Crear:** `static/js/ui-feedback.js`

```javascript
// Loading spinner global
function showLoading(message = 'Cargando...') {
    Swal.fire({
        title: message,
        allowOutsideClick: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });
}

function hideLoading() {
    Swal.close();
}

// Toast notifications
function showToast(type, message) {
    const Toast = Swal.mixin({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        timerProgressBar: true
    });
    
    Toast.fire({
        icon: type, // success, error, warning, info
        title: message
    });
}

// Skeleton loaders en catálogo
function showSkeletonLoaders(count = 6) {
    const container = document.getElementById('products-grid');
    container.innerHTML = '';
    
    for (let i = 0; i < count; i++) {
        container.innerHTML += `
            <div class="animate-pulse">
                <div class="bg-gray-300 h-48 rounded-lg"></div>
                <div class="mt-4 h-4 bg-gray-300 rounded w-3/4"></div>
                <div class="mt-2 h-4 bg-gray-300 rounded w-1/2"></div>
            </div>
        `;
    }
}

// Progress bar para formularios multi-paso
class FormProgress {
    constructor(steps) {
        this.steps = steps;
        this.currentStep = 0;
    }
    
    next() {
        if (this.currentStep < this.steps.length - 1) {
            this.currentStep++;
            this.render();
        }
    }
    
    prev() {
        if (this.currentStep > 0) {
            this.currentStep--;
            this.render();
        }
    }
    
    render() {
        const progress = ((this.currentStep + 1) / this.steps.length) * 100;
        document.getElementById('progress-bar').style.width = `${progress}%`;
        document.getElementById('step-indicator').textContent = 
            `Paso ${this.currentStep + 1} de ${this.steps.length}`;
    }
}
```

**Prioridad:** 🟡 P1  
**Esfuerzo:** 6 horas

---

#### UI-002: Búsqueda con Autocompletado

```javascript
// Debounced search con autocompletado
let searchTimeout;
const searchInput = document.getElementById('search');
const suggestionsBox = document.getElementById('suggestions');

searchInput.addEventListener('input', (e) => {
    clearTimeout(searchTimeout);
    const query = e.target.value.trim();
    
    if (query.length < 2) {
        suggestionsBox.classList.add('hidden');
        return;
    }
    
    searchTimeout = setTimeout(async () => {
        const response = await fetch(`/api/productos/search?q=${encodeURIComponent(query)}`);
        const products = await response.json();
        
        renderSuggestions(products);
    }, 300);
});

function renderSuggestions(products) {
    if (products.length === 0) {
        suggestionsBox.classList.add('hidden');
        return;
    }
    
    suggestionsBox.innerHTML = products.slice(0, 5).map(p => `
        <a href="/producto/${p.id}" 
           class="flex items-center gap-3 p-3 hover:bg-gray-100 transition">
            <img src="${p.imagenUrl}" class="w-12 h-12 object-cover rounded">
            <div>
                <div class="font-medium">${p.nombre}</div>
                <div class="text-sm text-gray-600">$${p.precioPorDia.toLocaleString()}/día</div>
            </div>
        </a>
    `).join('');
    
    suggestionsBox.classList.remove('hidden');
}
```

**Prioridad:** 🟡 P1  
**Esfuerzo:** 4 horas

---


### 7. INNOVACIONES DIFERENCIADORAS

#### INNOV-001: Sistema de Recomendaciones IA

```java
@Service
public class RecommendationService {
    
    /**
     * Recomendaciones basadas en:
     * 1. Historial de reservas del usuario
     * 2. Productos frecuentemente alquilados juntos
     * 3. Tendencias por tipo de evento
     */
    public List<Product> getRecommendations(String userId, String eventType) {
        // Obtener historial del usuario
        List<Reservation> userHistory = reservationService.getByUsuarioId(userId);
        Set<String> userProductIds = userHistory.stream()
            .flatMap(r -> r.getItems().stream())
            .map(Reservation.ItemReserva::getProductoId)
            .collect(Collectors.toSet());
        
        // Productos que otros usuarios alquilaron junto con los del usuario
        List<String> relatedProductIds = findFrequentlyRentedTogether(userProductIds);
        
        // Productos populares para el tipo de evento
        List<String> popularForEvent = findPopularForEventType(eventType);
        
        // Combinar y rankear
        Set<String> recommendedIds = new LinkedHashSet<>();
        recommendedIds.addAll(relatedProductIds);
        recommendedIds.addAll(popularForEvent);
        
        // Excluir productos que el usuario ya tiene en favoritos
        User user = userService.getByIdOrThrow(userId);
        recommendedIds.removeAll(user.getFavoritos());
        
        return productService.getProductsByIds(
            new ArrayList<>(recommendedIds).subList(0, Math.min(8, recommendedIds.size()))
        );
    }
    
    private List<String> findFrequentlyRentedTogether(Set<String> productIds) {
        // Encontrar reservas que contengan alguno de estos productos
        List<Reservation> relatedReservations = reservationRepository.findAll().stream()
            .filter(r -> r.getItems().stream()
                .anyMatch(item -> productIds.contains(item.getProductoId())))
            .collect(Collectors.toList());
        
        // Contar frecuencia de otros productos en esas reservas
        Map<String, Long> frequency = relatedReservations.stream()
            .flatMap(r -> r.getItems().stream())
            .map(Reservation.ItemReserva::getProductoId)
            .filter(id -> !productIds.contains(id))
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        
        // Ordenar por frecuencia descendente
        return frequency.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .limit(10)
            .collect(Collectors.toList());
    }
    
    private List<String> findPopularForEventType(String eventType) {
        if (eventType == null) return List.of();
        
        // Productos más alquilados para este tipo de evento
        return reservationRepository.findAll().stream()
            .filter(r -> eventType.equalsIgnoreCase(r.getTipoEvento()))
            .flatMap(r -> r.getItems().stream())
            .collect(Collectors.groupingBy(
                Reservation.ItemReserva::getProductoId, 
                Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .limit(10)
            .collect(Collectors.toList());
    }
}
```

**Endpoint:**

```java
@GetMapping("/api/recomendaciones")
public ResponseEntity<List<Product>> getRecommendations(
        @RequestParam(required = false) String eventType,
        Principal principal) {
    List<Product> recommendations = recommendationService.getRecommendations(
        principal.getName(), eventType);
    return ResponseEntity.ok(recommendations);
}
```

**Prioridad:** 🟢 P2  
**Esfuerzo:** 8 horas

---

#### INNOV-002: Predicción de Demanda (ML)

```java
@Service
public class DemandPredictionService {
    
    /**
     * Predice la demanda de productos para los próximos 30 días
     * usando regresión lineal simple basada en histórico
     */
    public Map<String, Double> predictDemand(int daysAhead) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusMonths(6);
        
        // Obtener reservas de los últimos 6 meses
        List<Reservation> historicalReservations = reservationRepository
            .findByFechaCreacionBetween(
                startDate.atStartOfDay(), 
                today.atTime(23, 59, 59));
        
        // Agrupar por producto y contar demanda por semana
        Map<String, List<Integer>> weeklyDemand = new HashMap<>();
        
        for (Reservation r : historicalReservations) {
            for (Reservation.ItemReserva item : r.getItems()) {
                String productId = item.getProductoId();
                int week = (int) ChronoUnit.WEEKS.between(startDate, 
                    r.getFechaCreacion().toLocalDate());
                
                weeklyDemand.putIfAbsent(productId, new ArrayList<>());
                List<Integer> weeks = weeklyDemand.get(productId);
                
                // Asegurar que la lista tenga suficiente tamaño
                while (weeks.size() <= week) {
                    weeks.add(0);
                }
                weeks.set(week, weeks.get(week) + item.getCantidad());
            }
        }
        
        // Calcular tendencia (regresión lineal simple)
        Map<String, Double> predictions = new HashMap<>();
        
        for (Map.Entry<String, List<Integer>> entry : weeklyDemand.entrySet()) {
            String productId = entry.getKey();
            List<Integer> demand = entry.getValue();
            
            // Calcular pendiente de la tendencia
            double avgDemand = demand.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
            
            // Predicción simple: promedio de las últimas 4 semanas
            double recentAvg = demand.stream()
                .skip(Math.max(0, demand.size() - 4))
                .mapToInt(Integer::intValue)
                .average()
                .orElse(avgDemand);
            
            predictions.put(productId, recentAvg * (daysAhead / 7.0));
        }
        
        return predictions;
    }
    
    /**
     * Identifica productos con riesgo de desabastecimiento
     */
    public List<ProductAlert> getStockAlerts() {
        Map<String, Double> predictedDemand = predictDemand(30);
        List<ProductAlert> alerts = new ArrayList<>();
        
        for (Map.Entry<String, Double> entry : predictedDemand.entrySet()) {
            String productId = entry.getKey();
            double predicted = entry.getValue();
            
            Product product = productService.getProductById(productId).orElse(null);
            if (product == null) continue;
            
            int currentStock = product.getStock();
            
            if (predicted > currentStock * 0.8) {
                alerts.add(new ProductAlert(
                    product,
                    "STOCK_BAJO",
                    String.format("Demanda predicha (%.0f) cerca del stock (%d)", 
                        predicted, currentStock)
                ));
            }
        }
        
        return alerts;
    }
}
```

**Admin Dashboard:**

```java
@GetMapping("/admin/predicciones")
public String predictions(Model model) {
    Map<String, Double> demand = demandPredictionService.predictDemand(30);
    List<ProductAlert> alerts = demandPredictionService.getStockAlerts();
    
    model.addAttribute("predictions", demand);
    model.addAttribute("alerts", alerts);
    return "admin/predictions";
}
```

**Prioridad:** 🟢 P2  
**Esfuerzo:** 12 horas

---

#### INNOV-003: Integración WhatsApp Business

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.twilio.sdk</groupId>
    <artifactId>twilio</artifactId>
    <version>10.0.0</version>
</dependency>
```

```java
@Service
public class WhatsAppService {
    
    @Value("${twilio.account.sid}")
    private String accountSid;
    
    @Value("${twilio.auth.token}")
    private String authToken;
    
    @Value("${twilio.whatsapp.from}")
    private String fromNumber; // whatsapp:+14155238886
    
    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }
    
    @Async
    public void sendReservationConfirmation(Reservation reservation) {
        User user = userService.getById(reservation.getUsuarioId()).orElseThrow();
        
        String message = String.format(
            "🎉 *Reserva Confirmada*\n\n" +
            "Hola %s,\n\n" +
            "Tu reserva #%s ha sido confirmada.\n\n" +
            "*Fecha:* %s al %s\n" +
            "*Total:* $%s\n\n" +
            "Ver detalles: https://furent.com/panel/reservas/%s",
            user.getNombre(),
            reservation.getId().substring(0, 8),
            reservation.getFechaInicio(),
            reservation.getFechaFin(),
            formatMoney(reservation.getTotal()),
            reservation.getId()
        );
        
        sendMessage(user.getTelefono(), message);
    }
    
    @Async
    public void sendPaymentReminder(Reservation reservation) {
        User user = userService.getById(reservation.getUsuarioId()).orElseThrow();
        
        String message = String.format(
            "⏰ *Recordatorio de Pago*\n\n" +
            "Hola %s,\n\n" +
            "Tu reserva #%s está pendiente de pago.\n\n" +
            "*Monto:* $%s\n\n" +
            "Pagar ahora: https://furent.com/pago/%s",
            user.getNombre(),
            reservation.getId().substring(0, 8),
            formatMoney(reservation.getTotal()),
            reservation.getId()
        );
        
        sendMessage(user.getTelefono(), message);
    }
    
    @Async
    public void sendEventReminder(Reservation reservation) {
        User user = userService.getById(reservation.getUsuarioId()).orElseThrow();
        
        String message = String.format(
            "📅 *Recordatorio de Evento*\n\n" +
            "Hola %s,\n\n" +
            "Tu evento es mañana (%s).\n\n" +
            "Nuestro equipo entregará el mobiliario en:\n" +
            "%s\n\n" +
            "¿Alguna duda? Responde este mensaje.",
            user.getNombre(),
            reservation.getFechaInicio(),
            reservation.getDireccionEvento()
        );
        
        sendMessage(user.getTelefono(), message);
    }
    
    private void sendMessage(String to, String body) {
        try {
            // Formatear número: +57 300 123 4567 -> whatsapp:+573001234567
            String formattedTo = "whatsapp:+57" + to.replaceAll("[^0-9]", "");
            
            Message message = Message.creator(
                new PhoneNumber(formattedTo),
                new PhoneNumber(fromNumber),
                body
            ).create();
            
            log.info("WhatsApp enviado a {}: {}", to, message.getSid());
        } catch (Exception e) {
            log.error("Error enviando WhatsApp a {}", to, e);
        }
    }
}
```

**Scheduler para recordatorios:**

```java
@Component
public class WhatsAppScheduler {
    
    @Scheduled(cron = "0 0 10 * * *") // Todos los días a las 10am
    public void sendEventReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        
        List<Reservation> upcomingEvents = reservationRepository
            .findByFechaInicioAndEstado(tomorrow, EstadoReserva.ENTREGADA.name());
        
        for (Reservation reservation : upcomingEvents) {
            whatsAppService.sendEventReminder(reservation);
        }
    }
    
    @Scheduled(cron = "0 0 18 * * *") // Todos los días a las 6pm
    public void sendPaymentReminders() {
        List<Reservation> pendingPayment = reservationRepository
            .findByEstado(EstadoReserva.CONFIRMADA.name()).stream()
            .filter(r -> ChronoUnit.DAYS.between(r.getFechaCreacion().toLocalDate(), 
                LocalDate.now()) >= 2)
            .collect(Collectors.toList());
        
        for (Reservation reservation : pendingPayment) {
            whatsAppService.sendPaymentReminder(reservation);
        }
    }
}
```

**Prioridad:** 🟢 P2  
**Esfuerzo:** 10 horas

---

#### INNOV-004: Sistema de Fidelización

```java
@Document(collection = "loyalty_points")
@Data
public class LoyaltyAccount {
    @Id
    private String id;
    private String userId;
    private int points;
    private String tier; // BRONZE, SILVER, GOLD, PLATINUM
    private LocalDateTime lastUpdated;
    private List<PointTransaction> transactions;
    
    @Data
    public static class PointTransaction {
        private int points;
        private String reason;
        private String reservationId;
        private LocalDateTime date;
    }
}

@Service
public class LoyaltyService {
    
    private static final Map<String, Integer> TIER_THRESHOLDS = Map.of(
        "BRONZE", 0,
        "SILVER", 1000,
        "GOLD", 5000,
        "PLATINUM", 15000
    );
    
    private static final Map<String, Double> TIER_DISCOUNTS = Map.of(
        "BRONZE", 0.0,
        "SILVER", 0.05,  // 5%
        "GOLD", 0.10,    // 10%
        "PLATINUM", 0.15 // 15%
    );
    
    public void awardPoints(String userId, int points, String reason, String reservationId) {
        LoyaltyAccount account = getOrCreateAccount(userId);
        
        account.setPoints(account.getPoints() + points);
        account.getTransactions().add(new LoyaltyAccount.PointTransaction(
            points, reason, reservationId, LocalDateTime.now()
        ));
        
        // Actualizar tier si corresponde
        updateTier(account);
        
        loyaltyRepository.save(account);
        
        // Notificar usuario
        notificationService.create(userId,
            "Puntos Ganados 🎁",
            String.format("Has ganado %d puntos. Total: %d", points, account.getPoints()),
            "/panel/fidelizacion");
    }
    
    public void awardPointsForReservation(Reservation reservation) {
        // 1 punto por cada $1000 COP gastados
        int points = reservation.getTotal().divide(BigDecimal.valueOf(1000))
            .intValue();
        
        awardPoints(reservation.getUsuarioId(), points, 
            "Reserva completada", reservation.getId());
    }
    
    public BigDecimal applyLoyaltyDiscount(String userId, BigDecimal amount) {
        LoyaltyAccount account = getOrCreateAccount(userId);
        double discount = TIER_DISCOUNTS.get(account.getTier());
        
        return amount.multiply(BigDecimal.valueOf(1 - discount));
    }
    
    public void redeemPoints(String userId, int points) {
        LoyaltyAccount account = getOrCreateAccount(userId);
        
        if (account.getPoints() < points) {
            throw new InvalidOperationException("Puntos insuficientes");
        }
        
        account.setPoints(account.getPoints() - points);
        account.getTransactions().add(new LoyaltyAccount.PointTransaction(
            -points, "Redención de puntos", null, LocalDateTime.now()
        ));
        
        loyaltyRepository.save(account);
    }
    
    private void updateTier(LoyaltyAccount account) {
        String newTier = "BRONZE";
        
        if (account.getPoints() >= TIER_THRESHOLDS.get("PLATINUM")) {
            newTier = "PLATINUM";
        } else if (account.getPoints() >= TIER_THRESHOLDS.get("GOLD")) {
            newTier = "GOLD";
        } else if (account.getPoints() >= TIER_THRESHOLDS.get("SILVER")) {
            newTier = "SILVER";
        }
        
        if (!newTier.equals(account.getTier())) {
            String oldTier = account.getTier();
            account.setTier(newTier);
            
            // Notificar upgrade
            notificationService.create(account.getUserId(),
                "¡Nivel Mejorado! 🎉",
                String.format("Has ascendido de %s a %s. Ahora tienes %d%% de descuento.",
                    oldTier, newTier, (int)(TIER_DISCOUNTS.get(newTier) * 100)),
                "/panel/fidelizacion");
        }
    }
}
```

**Event Listener:**

```java
@EventListener
public void onReservationCompleted(ReservationCompletedEvent event) {
    loyaltyService.awardPointsForReservation(event.getReservation());
}
```

**Prioridad:** 🟢 P2  
**Esfuerzo:** 10 horas

---

## 📋 CHECKLIST FINAL 10/10

### Bugs y Seguridad (Crítico)
- [ ] Fix getRelatedProducts() bug
- [ ] Fix estado mantenimiento bug
- [ ] Habilitar CSRF en formularios
- [ ] Validar tipos de archivo en uploads
- [ ] Validar fechas en cotización
- [ ] Headers de seguridad HTTP
- [ ] Password admin configurable
- [ ] Validación y sanitización de entradas

### Funcionalidades Core
- [ ] Sistema de pagos completo (init, confirm, fail)
- [ ] Notificaciones por email (5 templates)
- [ ] Recuperación de contraseña
- [ ] Admin CRUD usuarios completo
- [ ] Admin CRUD categorías
- [ ] Formulario de contacto funcional
- [ ] Sistema de cupones/descuentos
- [ ] Verificación de email
- [ ] Sistema de favoritos

### Testing
- [ ] Tests unitarios servicios (70% cobertura)
- [ ] Tests integración controllers
- [ ] Tests E2E flujos críticos
- [ ] CI/CD con GitHub Actions
- [ ] Reporte de cobertura (JaCoCo)

### UX/UI
- [ ] Loading spinners y feedback visual
- [ ] Búsqueda con autocompletado
- [ ] Skeleton loaders
- [ ] Progress bars en formularios
- [ ] Toasts/notificaciones
- [ ] Responsive mobile-first
- [ ] Dashboard con gráficos interactivos

### Innovaciones
- [ ] Sistema de recomendaciones IA
- [ ] Predicción de demanda (ML)
- [ ] Integración WhatsApp Business
- [ ] Sistema de fidelización
- [ ] Logística con rutas optimizadas

### DevOps
- [ ] CI/CD automatizado
- [ ] Security scanning (Trivy)
- [ ] Monitoreo con Prometheus/Grafana
- [ ] Logs estructurados (JSON)
- [ ] Health checks configurados

### Documentación
- [ ] README actualizado
- [ ] API docs (Swagger)
- [ ] Guía de despliegue
- [ ] Changelog
- [ ] Contributing guidelines

---

## 🎯 ROADMAP DE EJECUCIÓN

### Sprint 1 (Semana 1-2): CRÍTICO
**Objetivo:** Sistema estable sin bugs críticos

- Día 1-2: Corregir todos los bugs (BUG-001 a BUG-005)
- Día 3-4: Habilitar CSRF y headers de seguridad
- Día 5-7: Implementar recuperación de contraseña
- Día 8-10: Sistema de pagos completo
- Día 11-14: Notificaciones por email (5 templates)

**Entregable:** Sistema funcional sin bugs, pagos operativos

---

### Sprint 2 (Semana 3-4): FUNCIONALIDADES
**Objetivo:** Completar MVP funcional

- Día 1-3: Admin CRUD usuarios y categorías
- Día 4-5: Formulario de contacto
- Día 6-8: Sistema de cupones/descuentos
- Día 9-10: Verificación de email
- Día 11-12: Sistema de favoritos
- Día 13-14: Paginación en todos los listados

**Entregable:** MVP completo con todas las funcionalidades core

---

### Sprint 3 (Semana 5-6): TESTING
**Objetivo:** 70% cobertura de tests

- Día 1-5: Tests unitarios (servicios)
- Día 6-9: Tests de integración (controllers)
- Día 10-12: Tests E2E (flujos críticos)
- Día 13-14: CI/CD con GitHub Actions

**Entregable:** Suite de tests completa, CI/CD automatizado

---

### Sprint 4 (Semana 7-8): UX/UI
**Objetivo:** Interfaz moderna y profesional

- Día 1-3: Loading states y feedback visual
- Día 4-5: Búsqueda con autocompletado
- Día 6-7: Skeleton loaders y animaciones
- Día 8-10: Dashboard mejorado con gráficos
- Día 11-12: Responsive mobile-first
- Día 13-14: Microinteracciones y polish

**Entregable:** UI/UX de nivel competitivo

---

### Sprint 5 (Semana 9-11): INNOVACIÓN
**Objetivo:** Diferenciadores competitivos

- Día 1-4: Sistema de recomendaciones IA
- Día 5-8: Predicción de demanda (ML)
- Día 9-13: Integración WhatsApp Business
- Día 14-18: Sistema de fidelización
- Día 19-21: Logística optimizada

**Entregable:** Funcionalidades únicas que te destacan

---

## 💰 ESTIMACIÓN DE ESFUERZO

| Fase | Esfuerzo | Costo (USD)* |
|------|----------|--------------|
| Sprint 1: Crítico | 80h | $4,000 |
| Sprint 2: Funcionalidades | 70h | $3,500 |
| Sprint 3: Testing | 60h | $3,000 |
| Sprint 4: UX/UI | 50h | $2,500 |
| Sprint 5: Innovación | 80h | $4,000 |
| **TOTAL** | **340h** | **$17,000** |

*Asumiendo $50/hora para desarrollador senior

---

## 🚀 RESULTADO ESPERADO

Al completar este plan, Furent será:

✅ **Robusto** — Sin bugs, 70% cobertura de tests  
✅ **Seguro** — CSRF, validaciones, headers de seguridad  
✅ **Completo** — Todas las funcionalidades core operativas  
✅ **Moderno** — UI/UX profesional y responsive  
✅ **Innovador** — IA, WhatsApp, fidelización  
✅ **Escalable** — CI/CD, Kubernetes, monitoreo  
✅ **Documentado** — README, API docs, guías

**Calificación Final Esperada: 9.5/10** 🎯

---

## 📞 PRÓXIMOS PASOS

1. **Revisar y priorizar** este plan según tus necesidades
2. **Definir equipo** (desarrolladores, QA, diseñador)
3. **Configurar entorno** (GitHub, CI/CD, staging)
4. **Iniciar Sprint 1** con bugs críticos
5. **Iterar y ajustar** según feedback

¿Listo para llevar Furent al siguiente nivel? 🚀

