# Implementation Plan: Furent Professional Improvements

## Overview

Este plan de implementación convierte el diseño técnico en tareas concretas de código para completar las mejoras profesionales críticas de Furent. El sistema es una plataforma SaaS multi-tenant de alquiler de mobiliarios construida con Spring Boot y MongoDB.

El plan sigue un enfoque incremental donde cada tarea construye sobre las anteriores, asegurando que el código se integre progresivamente sin dejar componentes huérfanos.

## Implementation Approach

- **Lenguaje**: Java 17 con Spring Boot 3.x
- **Base de datos**: MongoDB con índices optimizados
- **Seguridad**: Spring Security con CSRF habilitado
- **Testing**: JUnit 5 + jqwik para property-based testing
- **Orden de implementación**: Bugs → Seguridad → Pagos → Email → Features → Testing

## Tasks

### Phase 1: Bug Fixes and Security (Prioridad P0)

- [x] 1. Corregir bug en ProductService.getRelatedProducts()
  - Modificar archivo `src/main/java/com/alquiler/furent/service/ProductService.java`
  - Cambiar `.filter(p -> p.getCategory().equals(product.getCategory()))` por `.filter(p -> p.getCategoriaNombre().equals(product.getCategoriaNombre()))`
  - Asegurar que el método retorna lista vacía si no hay productos relacionados
  - Verificar que no lanza RuntimeException
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 2. Corregir validación de estado de mantenimiento en AdminProductosController
  - Modificar archivo `src/main/java/com/alquiler/furent/controller/AdminProductosController.java`
  - Cambiar lógica de `calculateAvailability()` para considerar disponible si estado NO es "EN_REPARACION"
  - Cambiar `"OPERATIVO".equals(estadoMantenimiento)` por `!"EN_REPARACION".equals(estadoMantenimiento)`
  - _Requirements: 2.1, 2.2, 2.3_

- [x] 3. Habilitar protección CSRF en SecurityConfig
  - Modificar archivo `src/main/java/com/alquiler/furent/config/SecurityConfig.java`
  - Cambiar `.csrf(csrf -> csrf.disable())` por `.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))`
  - Habilitar CSRF para todas las rutas excepto `/api/**`
  - Agregar tokens CSRF en todos los formularios Thymeleaf con `<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>`
  - Verificar que formularios sin token válido retornan HTTP 403
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 4. Implementar validación de fechas en ApiController
  - Modificar archivo `src/main/java/com/alquiler/furent/controller/ApiController.java`
  - En método `createQuote()`, agregar validación que `fechaFin` no sea anterior a `fechaInicio`
  - Agregar validación que `fechaInicio` no sea anterior a fecha actual
  - Retornar HTTP 400 con mensaje descriptivo si las fechas son inválidas
  - _Requirements: 5.1, 5.2, 5.3_

- [x] 5. Implementar validación de tipos de archivo en AdminProductosController
  - Modificar archivo `src/main/java/com/alquiler/furent/controller/AdminProductosController.java`
  - Crear método `validateImageFile(MultipartFile file)` que valide Content-Type en {image/jpeg, image/png, image/webp, image/gif}
  - Validar que el tamaño del archivo no exceda 5MB
  - Lanzar `InvalidOperationException` con mensaje descriptivo si la validación falla
  - Llamar a `validateImageFile()` antes de guardar cualquier archivo
  - _Requirements: 4.1, 4.2, 4.3_

- [x] 6. Implementar headers de seguridad HTTP en SecurityConfig
  - Modificar archivo `src/main/java/com/alquiler/furent/config/SecurityConfig.java`
  - Configurar Content-Security-Policy con directivas restrictivas
  - Habilitar HTTP Strict Transport Security (HSTS) con max-age de 31536000 segundos
  - Configurar X-Frame-Options como DENY
  - Habilitar X-XSS-Protection con modo block
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [x] 7. Hacer password de admin configurable vía variable de entorno
  - Modificar archivo `src/main/java/com/alquiler/furent/config/DataInitializer.java`
  - Agregar `@Value("${furent.admin.password:${random.uuid}}")` para leer password de variable de entorno
  - Si la variable no está definida, generar UUID aleatorio como contraseña
  - Registrar la contraseña en logs con advertencia de seguridad
  - Encriptar la contraseña con BCrypt antes de almacenarla
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 8. Implementar validación y sanitización de entradas con Bean Validation
  - Crear archivo `src/main/java/com/alquiler/furent/dto/CotizacionRequest.java`
  - Agregar anotaciones de validación: @NotBlank, @Size, @Pattern, @Min, @Max, @Email
  - Validar que `tipoEvento` contenga solo letras, números y espacios con @Pattern
  - Validar que `invitados` esté entre 1 y 10,000
  - Validar que `notas` no exceda 1000 caracteres
  - Validar que `items` no esté vacío
  - Agregar método `@AssertTrue isFechaFinValid()` para validar que fechaFin >= fechaInicio
  - Aplicar `@Valid` en todos los endpoints que reciben DTOs
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 9. Checkpoint - Verificar bugs corregidos y seguridad implementada
  - Ejecutar la aplicación y verificar que no hay errores de compilación
  - Probar manualmente que productos relacionados se muestran sin errores
  - Verificar que formularios web incluyen tokens CSRF
  - Verificar que uploads solo aceptan imágenes válidas
  - Verificar que headers de seguridad están presentes en respuestas HTTP
  - Preguntar al usuario si hay dudas o problemas antes de continuar


### Phase 2: Payment System (Prioridad P0)

- [x] 10. Crear modelo Payment con índices MongoDB
  - Crear archivo `src/main/java/com/alquiler/furent/model/Payment.java`
  - Definir campos: id, tenantId, reservaId, usuarioId, monto, metodoPago, estado, referencia, fechaCreacion, fechaPago
  - Agregar anotación `@Document(collection = "payments")`
  - Agregar índices compuestos con `@CompoundIndexes` para tenantId+usuarioId, tenantId+estado, reservaId
  - Agregar índice único para referencia
  - _Requirements: 9.1, 9.2_

- [x] 11. Crear PaymentRepository
  - Crear archivo `src/main/java/com/alquiler/furent/repository/PaymentRepository.java`
  - Extender `MongoRepository<Payment, String>`
  - Agregar métodos: `List<Payment> findByUsuarioId(String userId)`, `List<Payment> findByReservaId(String reservaId)`, `Optional<Payment> findByReferencia(String referencia)`
  - _Requirements: 9.1_

- [x] 12. Implementar PaymentService con lógica de negocio
  - Crear archivo `src/main/java/com/alquiler/furent/service/PaymentService.java`
  - Implementar método `initPayment(String reservaId, String userId, MetodoPago metodo)` que crea Payment con estado PENDIENTE
  - Generar referencia única con formato "PAY-XXXXXXXX" (8 caracteres alfanuméricos)
  - Validar que la reserva esté en estado CONFIRMADA antes de crear el pago
  - Lanzar `InvalidOperationException` si la reserva no está CONFIRMADA
  - Crear notificación para el usuario cuando se inicia el pago
  - _Requirements: 9.1, 9.2, 9.10_

- [x] 13. Implementar métodos de confirmación y rechazo de pagos en PaymentService
  - En archivo `src/main/java/com/alquiler/furent/service/PaymentService.java`
  - Implementar método `confirmPayment(String paymentId, String referencia, String admin)` que cambia estado a PAGADO
  - Actualizar fechaPago cuando se confirma el pago
  - Cambiar reserva asociada a estado ENTREGADA
  - Incrementar métrica `paymentsCompleted` en MetricsConfig
  - Enviar email de confirmación al usuario (llamar a EmailService)
  - Implementar método `failPayment(String paymentId, String reason, String admin)` que cambia estado a FALLIDO
  - Validar que el pago esté en estado PENDIENTE antes de procesarlo
  - Lanzar `InvalidOperationException` si el pago ya fue procesado
  - _Requirements: 9.3, 9.4, 9.5, 9.7, 9.8, 9.9_

- [ ]* 14. Escribir property test para idempotencia de confirmación de pagos
  - **Property 11: Payment Idempotence**
  - **Validates: Requirements 9.9, 9.11**
  - Crear archivo `src/test/java/com/alquiler/furent/service/PaymentServicePropertyTest.java`
  - Usar jqwik para generar pagos en estado PENDIENTE
  - Verificar que confirmar un pago una vez cambia estado a PAGADO
  - Verificar que intentar confirmar el mismo pago nuevamente lanza `InvalidOperationException`

- [x] 15. Crear PaymentController para API REST
  - Crear archivo `src/main/java/com/alquiler/furent/controller/PaymentController.java`
  - Agregar anotación `@RestController` y `@RequestMapping("/api/pagos")`
  - Implementar endpoint `POST /api/pagos/iniciar/{reservaId}` que recibe método de pago y retorna Payment creado
  - Implementar endpoint `GET /api/pagos/mis-pagos` que retorna lista de pagos del usuario autenticado
  - Implementar endpoint `GET /api/pagos/{id}` que retorna un pago por ID
  - _Requirements: 9.1, 9.2_

- [x] 16. Crear AdminPagosController para gestión web
  - Crear archivo `src/main/java/com/alquiler/furent/controller/AdminPagosController.java`
  - Agregar anotación `@Controller` y `@RequestMapping("/admin/pagos")`
  - Implementar endpoint `GET /admin/pagos` que lista todos los pagos con paginación
  - Implementar endpoint `POST /admin/pagos/{id}/confirmar` que confirma un pago con referencia
  - Implementar endpoint `POST /admin/pagos/{id}/rechazar` que rechaza un pago con razón
  - Agregar mensajes flash de éxito/error con RedirectAttributes
  - _Requirements: 9.3, 9.5_

- [x] 17. Integrar pagos con métricas de Prometheus
  - Modificar archivo `src/main/java/com/alquiler/furent/config/MetricsConfig.java`
  - Agregar contadores: `paymentsCreated`, `paymentsCompleted`, `paymentsFailed`
  - Agregar gauge para `totalRevenue`
  - Incrementar métricas en PaymentService cuando se crean, confirman o rechazan pagos
  - _Requirements: 9.8_

- [x] 18. Crear templates Thymeleaf para administración de pagos
  - Crear archivo `src/main/resources/templates/admin/pagos.html`
  - Mostrar tabla con lista de pagos: referencia, usuario, monto, estado, fecha
  - Agregar botones de "Confirmar" y "Rechazar" para pagos PENDIENTES
  - Agregar formulario modal para ingresar referencia de confirmación
  - Agregar formulario modal para ingresar razón de rechazo
  - Incluir token CSRF en todos los formularios
  - _Requirements: 9.3, 9.5_

- [x] 19. Checkpoint - Verificar sistema de pagos completo
  - Ejecutar la aplicación y verificar que no hay errores
  - Probar flujo completo: crear reserva → iniciar pago → confirmar pago
  - Verificar que la reserva cambia a ENTREGADA cuando se confirma el pago
  - Verificar que las métricas se incrementan correctamente
  - Preguntar al usuario si hay dudas antes de continuar

### Phase 3: Email Notifications (Prioridad P0)

- [x] 20. Configurar JavaMailSender en application.properties
  - Modificar archivo `src/main/resources/application.properties`
  - Agregar configuración SMTP de Gmail: host, port, username, password
  - Configurar propiedades de autenticación y STARTTLS
  - Agregar configuración de pool de threads para envío asíncrono
  - _Requirements: 10.6_

- [x] 21. Crear EmailService con métodos asíncronos
  - Crear archivo `src/main/java/com/alquiler/furent/service/EmailService.java`
  - Agregar anotación `@Service` y configurar `@Async` en métodos de envío
  - Implementar método `sendWelcomeEmail(User user)` para email de bienvenida
  - Implementar método `sendReservationConfirmation(Reservation reservation)` para confirmación de reserva
  - Implementar método `sendStatusChange(Reservation reservation, String oldStatus)` para cambios de estado
  - Implementar método `sendPasswordResetToken(User user, String token)` para recuperación de contraseña
  - Implementar método `sendPaymentConfirmation(Payment payment)` para recibo de pago
  - Implementar método privado `sendHtmlEmail(String to, String subject, String html)` que maneja errores sin interrumpir flujo
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.8_

- [ ]* 22. Escribir property test para manejo de errores en envío de emails
  - **Property 14: Email Error Handling**
  - **Validates: Requirements 10.8**
  - Crear archivo `src/test/java/com/alquiler/furent/service/EmailServicePropertyTest.java`
  - Simular fallo en envío de email (SMTP no disponible)
  - Verificar que el error se registra en logs
  - Verificar que no se lanza excepción que interrumpa el flujo principal

- [x] 23. Crear template email/bienvenida.html
  - Crear archivo `src/main/resources/templates/email/bienvenida.html`
  - Usar Thymeleaf para renderizar HTML con datos del usuario
  - Incluir nombre del usuario, mensaje de bienvenida y link al catálogo
  - Aplicar estilos CSS inline para compatibilidad con clientes de email
  - _Requirements: 10.1, 10.10_

- [x] 24. Crear template email/confirmacion-reserva.html
  - Crear archivo `src/main/resources/templates/email/confirmacion-reserva.html`
  - Incluir detalles de la reserva: productos, fechas, total, dirección
  - Mostrar tabla con lista de productos reservados
  - Incluir información de contacto y próximos pasos
  - _Requirements: 10.2, 10.10_

- [x] 25. Crear template email/cambio-estado.html
  - Crear archivo `src/main/resources/templates/email/cambio-estado.html`
  - Mostrar estado anterior y nuevo estado de la reserva
  - Incluir mensaje explicativo del cambio
  - Agregar link para ver detalles de la reserva
  - _Requirements: 10.3, 10.10_

- [x] 26. Crear template email/reset-password.html
  - Crear archivo `src/main/resources/templates/email/reset-password.html`
  - Incluir nombre del usuario y link con token de reset
  - Mostrar tiempo de expiración del token (1 hora)
  - Agregar advertencia de seguridad si no solicitó el reset
  - _Requirements: 10.5, 10.10_

- [x] 27. Crear template email/recibo-pago.html
  - Crear archivo `src/main/resources/templates/email/recibo-pago.html`
  - Mostrar detalles del pago: referencia, monto, método, fecha
  - Incluir resumen de la reserva asociada
  - Agregar información de contacto para soporte
  - _Requirements: 10.4, 10.10_

- [x] 28. Integrar EmailService con eventos del sistema
  - Modificar `UserService` para enviar email de bienvenida al registrar usuario
  - Modificar `ReservationService` para enviar email al confirmar reserva
  - Modificar `ReservationService` para enviar email al cambiar estado
  - Modificar `PaymentService` para enviar email al confirmar pago
  - Asegurar que los emails se envían de forma asíncrona sin bloquear operaciones
  - _Requirements: 10.1, 10.2, 10.3, 10.4_

- [x] 29. Checkpoint - Verificar sistema de notificaciones por email
  - Configurar credenciales SMTP de prueba
  - Probar envío de cada tipo de email manualmente
  - Verificar que los emails se reciben correctamente
  - Verificar que los templates se renderizan con datos correctos
  - Verificar que errores de email no interrumpen flujo principal
  - Preguntar al usuario si hay dudas antes de continuar

### Phase 4: Password Reset (Prioridad P1)

- [x] 30. Crear modelo PasswordResetToken con índices
  - Crear archivo `src/main/java/com/alquiler/furent/model/PasswordResetToken.java`
  - Definir campos: id, tenantId, userId, token (UUID), expiresAt, used, createdAt
  - Agregar anotación `@Document(collection = "password_reset_tokens")`
  - Agregar índices compuestos para tenantId+userId y token único
  - Implementar método `isExpired()` que verifica si el token expiró
  - Implementar método `isValid()` que verifica si el token no está usado y no expiró
  - _Requirements: 11.1, 11.5_

- [x] 31. Crear PasswordResetTokenRepository
  - Crear archivo `src/main/java/com/alquiler/furent/repository/PasswordResetTokenRepository.java`
  - Extender `MongoRepository<PasswordResetToken, String>`
  - Agregar métodos: `Optional<PasswordResetToken> findByToken(String token)`, `List<PasswordResetToken> findByUserId(String userId)`
  - _Requirements: 11.1_

- [x] 32. Implementar PasswordResetService
  - Crear archivo `src/main/java/com/alquiler/furent/service/PasswordResetService.java`
  - Implementar método `createToken(String userId)` que crea token con expiración de 1 hora
  - Invalidar todos los tokens anteriores del usuario antes de crear uno nuevo
  - Implementar método `resetPassword(String token, String newPassword)` que valida token y actualiza contraseña
  - Marcar token como usado después de resetear contraseña
  - Encriptar nueva contraseña con BCrypt antes de almacenar
  - Lanzar `InvalidOperationException` si el token expiró o ya fue usado
  - Implementar método `validateToken(String token)` que verifica si un token es válido
  - _Requirements: 11.1, 11.2, 11.4, 11.5, 11.7_

- [ ]* 33. Escribir property test para irreversibilidad de tokens de reset
  - **Property 17: Password Reset Token Irreversibility**
  - **Validates: Requirements 11.10**
  - Crear archivo `src/test/java/com/alquiler/furent/service/PasswordResetServicePropertyTest.java`
  - Generar tokens válidos con jqwik
  - Usar token para resetear contraseña
  - Verificar que el token se marca como usado
  - Verificar que intentar usar el mismo token nuevamente lanza `InvalidOperationException`

- [x] 34. Crear PasswordResetController con endpoints
  - Crear archivo `src/main/java/com/alquiler/furent/controller/PasswordResetController.java`
  - Agregar anotación `@Controller`
  - Implementar endpoint `GET /password-reset` que muestra formulario de solicitud
  - Implementar endpoint `POST /password-reset` que recibe email y crea token
  - No revelar si el email existe (mensaje genérico para seguridad)
  - Implementar endpoint `GET /password-reset/{token}` que valida token y muestra formulario de confirmación
  - Implementar endpoint `POST /password-reset/confirm` que recibe token y nueva contraseña
  - Validar que las contraseñas coinciden antes de resetear
  - _Requirements: 11.3, 11.4, 11.6, 11.8_

- [x] 35. Crear templates web para password reset
  - Crear archivo `src/main/resources/templates/password-reset-request.html` con formulario de solicitud
  - Crear archivo `src/main/resources/templates/password-reset-confirm.html` con formulario de nueva contraseña
  - Crear archivo `src/main/resources/templates/error/token-expired.html` para tokens inválidos
  - Incluir tokens CSRF en todos los formularios
  - Agregar validación de frontend para contraseñas coincidentes
  - _Requirements: 11.4, 11.6_

- [x] 36. Integrar password reset con EmailService
  - Modificar `PasswordResetController` para enviar email con token al usuario
  - Llamar a `emailService.sendPasswordResetToken(user, token)` después de crear token
  - Verificar que el email incluye link válido con el token
  - _Requirements: 11.8_

- [x] 37. Checkpoint - Verificar sistema de recuperación de contraseña
  - Probar flujo completo: solicitar reset → recibir email → resetear contraseña
  - Verificar que tokens expiran después de 1 hora
  - Verificar que tokens usados no se pueden reutilizar
  - Verificar que la contraseña se encripta correctamente
  - Preguntar al usuario si hay dudas antes de continuar

### Phase 5: Admin CRUD Users (Prioridad P1)

- [x] 38. Extender modelo User con campos de suspensión
  - Modificar archivo `src/main/java/com/alquiler/furent/model/User.java`
  - Agregar campos: estado (ACTIVO, SUSPENDIDO_TEMPORAL, ELIMINADO), fechaSuspension, razonSuspension, deleted (boolean)
  - Agregar índice para campo estado
  - Implementar método `isSuspended()` que verifica si el usuario está suspendido
  - Implementar método `isActive()` que verifica si el usuario está activo y no eliminado
  - _Requirements: 12.1, 12.2, 12.4_

- [x] 39. Implementar métodos de suspensión en UserService
  - Modificar archivo `src/main/java/com/alquiler/furent/service/UserService.java`
  - Implementar método `suspendUser(String userId, String reason, String admin)` que cambia estado a SUSPENDIDO_TEMPORAL
  - Implementar método `activateUser(String userId, String admin)` que cambia estado a ACTIVO
  - Implementar método `changeUserRole(String userId, RolUsuario newRole, String admin)` que actualiza el rol
  - Implementar método `softDeleteUser(String userId, String admin)` que marca deleted=true sin borrar datos
  - Registrar todas las acciones en AuditLogService
  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.6_

- [ ]* 40. Escribir property test para reversibilidad de suspensión de usuarios
  - **Property 19: User Suspension Reversibility**
  - **Validates: Requirements 12.9**
  - Crear archivo `src/test/java/com/alquiler/furent/service/UserServicePropertyTest.java`
  - Generar usuarios activos con jqwik
  - Suspender usuario y verificar que estado cambia a SUSPENDIDO_TEMPORAL
  - Activar usuario y verificar que estado cambia a ACTIVO
  - Verificar que el usuario puede hacer login después de activarse

- [x] 41. Modificar SecurityConfig para rechazar login de usuarios suspendidos
  - Modificar archivo `src/main/java/com/alquiler/furent/config/SecurityConfig.java`
  - Agregar validación en `UserDetailsService` que verifica si el usuario está suspendido
  - Lanzar `AccountSuspendedException` con mensaje "Cuenta suspendida" si el usuario está suspendido
  - Agregar handler en `GlobalExceptionHandler` para `AccountSuspendedException`
  - _Requirements: 12.5_

- [x] 42. Crear AdminUsuariosController con CRUD completo
  - Crear archivo `src/main/java/com/alquiler/furent/controller/AdminUsuariosController.java`
  - Agregar anotación `@Controller` y `@RequestMapping("/admin/usuarios")`
  - Implementar endpoint `GET /admin/usuarios` que lista usuarios con paginación y filtros
  - Implementar endpoint `POST /admin/usuarios/{id}/suspender` que suspende un usuario
  - Implementar endpoint `POST /admin/usuarios/{id}/activar` que activa un usuario
  - Implementar endpoint `POST /admin/usuarios/{id}/rol` que cambia el rol
  - Implementar endpoint `DELETE /admin/usuarios/{id}` que realiza soft delete
  - Agregar búsqueda por email o nombre
  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.7, 12.8_

- [x] 43. Crear templates Thymeleaf para gestión de usuarios
  - Crear archivo `src/main/resources/templates/admin/usuarios.html`
  - Mostrar tabla con lista de usuarios: email, nombre, rol, estado, fecha registro
  - Agregar filtros por rol y estado
  - Agregar barra de búsqueda por email o nombre
  - Agregar botones de acción: suspender, activar, cambiar rol, eliminar
  - Incluir modales para confirmar acciones y solicitar razón de suspensión
  - Incluir tokens CSRF en todos los formularios
  - _Requirements: 12.7, 12.8_

- [x] 44. Checkpoint - Verificar CRUD de usuarios completo
  - Probar suspender usuario y verificar que no puede hacer login
  - Probar activar usuario suspendido y verificar que puede hacer login
  - Probar cambiar rol de usuario y verificar permisos
  - Probar soft delete y verificar que los datos se preservan
  - Verificar que todas las acciones se registran en AuditLog
  - Preguntar al usuario si hay dudas antes de continuar

### Phase 6: Admin CRUD Categories (Prioridad P1)

- [x] 45. Implementar validación de categorías en CategoryService
  - Modificar archivo `src/main/java/com/alquiler/furent/service/CategoryService.java`
  - Implementar método `validateUniqueName(String nombre, String excludeId)` que verifica unicidad de nombres
  - Implementar método `canDelete(String categoryId)` que verifica si hay productos asociados
  - Lanzar `InvalidOperationException` si se intenta crear categoría con nombre duplicado
  - Lanzar `InvalidOperationException` si se intenta eliminar categoría con productos asociados
  - _Requirements: 13.1, 13.3_

- [ ]* 46. Escribir property test para unicidad de nombres de categorías
  - **Property 23: Category Name Uniqueness**
  - **Validates: Requirements 13.1, 13.8**
  - Crear archivo `src/test/java/com/alquiler/furent/service/CategoryServicePropertyTest.java`
  - Generar pares de categorías con jqwik
  - Verificar que si dos categorías tienen IDs diferentes, sus nombres deben ser diferentes
  - Intentar crear categoría con nombre duplicado y verificar que lanza excepción

- [x] 47. Crear AdminCategoriasController
  - Crear archivo `src/main/java/com/alquiler/furent/controller/AdminCategoriasController.java`
  - Agregar anotación `@Controller` y `@RequestMapping("/admin/categorias")`
  - Implementar endpoint `GET /admin/categorias` que lista categorías con contador de productos
  - Implementar endpoint `POST /admin/categorias/guardar` que crea o edita categoría
  - Implementar endpoint `DELETE /admin/categorias/{id}` que elimina categoría si no tiene productos
  - Validar nombre único antes de guardar
  - Registrar acciones en AuditLogService
  - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5, 13.7_

- [x] 48. Crear templates Thymeleaf para gestión de categorías
  - Crear archivo `src/main/resources/templates/admin/categorias.html`
  - Mostrar tabla con lista de categorías: nombre, descripción, icono, cantidad de productos
  - Agregar botones de editar y eliminar
  - Agregar formulario modal para crear/editar categoría
  - Mostrar advertencia si se intenta eliminar categoría con productos
  - Incluir tokens CSRF en todos los formularios
  - _Requirements: 13.5, 13.6_

- [x] 49. Checkpoint - Verificar CRUD de categorías completo
  - Probar crear categoría con nombre único
  - Probar editar categoría y verificar que se actualiza
  - Probar eliminar categoría sin productos
  - Probar que no se puede eliminar categoría con productos asociados
  - Verificar que no se pueden crear categorías con nombres duplicados
  - Preguntar al usuario si hay dudas antes de continuar

### Phase 7: Contact Form (Prioridad P1)

- [x] 50. Crear modelo ContactMessage con validaciones
  - Crear archivo `src/main/java/com/alquiler/furent/model/ContactMessage.java`
  - Definir campos: id, tenantId, nombre, email, telefono, asunto, mensaje, leido, fechaCreacion
  - Agregar anotación `@Document(collection = "contact_messages")`
  - Agregar validaciones Bean Validation: @NotBlank, @Email, @Size
  - Agregar índices compuestos para tenantId+leido y fechaCreacion
  - Inicializar leido=false y fechaCreacion=now en constructor
  - _Requirements: 14.1, 14.2, 14.3, 14.4_

- [x] 51. Crear ContactMessageRepository
  - Crear archivo `src/main/java/com/alquiler/furent/repository/ContactMessageRepository.java`
  - Extender `MongoRepository<ContactMessage, String>`
  - Agregar métodos: `List<ContactMessage> findByLeido(boolean leido)`, `long countByLeido(boolean leido)`
  - Agregar método con paginación: `Page<ContactMessage> findByLeido(boolean leido, Pageable pageable)`
  - _Requirements: 14.6, 14.9_

- [x] 52. Implementar ContactService
  - Crear archivo `src/main/java/com/alquiler/furent/service/ContactService.java`
  - Implementar método `save(ContactMessage message)` que guarda mensaje con estado NO_LEIDO
  - Implementar método `findUnread()` que retorna mensajes no leídos
  - Implementar método `markAsRead(String id)` que cambia estado a LEIDO
  - Implementar método `countUnread()` que cuenta mensajes no leídos
  - Implementar método `findAll(String estado, int page, int size)` con paginación y filtros
  - _Requirements: 14.1, 14.6, 14.7, 14.8, 14.9_

- [x]* 53. Escribir property test para invariante de contador de mensajes no leídos
  - **Property 27: Unread Message Counter Invariant**
  - **Validates: Requirements 14.10**
  - Crear archivo `src/test/java/com/alquiler/furent/service/ContactServicePropertyTest.java`
  - Generar lista de mensajes con estados aleatorios usando jqwik
  - Verificar que `countUnread()` siempre es igual al número de mensajes con estado NO_LEIDO
  - Marcar mensajes como leídos y verificar que el contador se actualiza correctamente

- [x] 54. Crear ContactController con formulario
  - Crear archivo `src/main/java/com/alquiler/furent/controller/ContactController.java`
  - Agregar anotación `@Controller`
  - Implementar endpoint `GET /contacto` que muestra formulario de contacto
  - Implementar endpoint `POST /contacto` que recibe y guarda mensaje
  - Aplicar validación con `@Valid` y mostrar errores en formulario
  - Mostrar mensaje de confirmación después de enviar
  - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5_

- [x] 55. Crear AdminContactController para gestión
  - Crear archivo `src/main/java/com/alquiler/furent/controller/AdminContactController.java`
  - Agregar anotación `@Controller` y `@RequestMapping("/admin/mensajes")`
  - Implementar endpoint `GET /admin/mensajes` que lista mensajes con paginación
  - Agregar filtro por estado (LEIDO/NO_LEIDO)
  - Implementar endpoint `POST /admin/mensajes/{id}/leer` que marca mensaje como leído
  - Mostrar badge con contador de mensajes no leídos en navegación
  - _Requirements: 14.6, 14.7, 14.8, 14.9_

- [x] 56. Crear templates Thymeleaf para contacto y admin
  - Crear archivo `src/main/resources/templates/contacto.html` con formulario público
  - Crear archivo `src/main/resources/templates/admin/mensajes.html` con lista de mensajes
  - Mostrar tabla con mensajes ordenados por fecha descendente
  - Agregar badge visual para mensajes no leídos
  - Agregar filtros por estado
  - Incluir tokens CSRF en todos los formularios
  - _Requirements: 14.5, 14.8_

- [x] 57. Checkpoint - Verificar formulario de contacto completo
  - Probar enviar mensaje desde formulario público
  - Verificar que el mensaje aparece en panel de admin
  - Verificar que el contador de no leídos se actualiza
  - Probar marcar mensaje como leído
  - Verificar validaciones de campos obligatorios
  - Preguntar al usuario si hay dudas antes de continuar

### Phase 8: Coupon System (Prioridad P1)

- [x] 58. Extender modelo Coupon con métodos de validación
  - Modificar archivo `src/main/java/com/alquiler/furent/model/Coupon.java`
  - Agregar campos si no existen: usosActuales (contador de usos)
  - Implementar método `isVigente()` que verifica si la fecha actual está dentro del rango
  - Implementar método `hasReachedLimit()` que verifica si alcanzó el límite de usos
  - Implementar método `isValid()` que combina todas las validaciones
  - _Requirements: 15.1, 15.2, 15.3_

- [x] 59. Implementar CouponService con validación y aplicación de descuentos
  - Crear archivo `src/main/java/com/alquiler/furent/service/CouponService.java`
  - Implementar método `validateCoupon(String codigo)` que verifica existencia, vigencia y límite de usos
  - Lanzar `InvalidOperationException` con razón específica si el cupón es inválido
  - Implementar método `applyDiscount(Coupon coupon, BigDecimal total)` que calcula total con descuento
  - Fórmula: `totalConDescuento = total * (1 - descuento/100)`
  - Implementar método `incrementUsage(String couponId)` que incrementa contador de usos
  - Lanzar `InvalidOperationException` si alcanzó el límite de usos
  - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5, 15.6, 15.8, 15.10_

- [x]* 60. Escribir property test para precisión de descuento de cupones
  - **Property 29: Coupon Discount Precision**
  - **Validates: Requirements 15.10**
  - Crear archivo `src/test/java/com/alquiler/furent/service/CouponServicePropertyTest.java`
  - Generar cupones con descuentos aleatorios (1-100%) usando jqwik
  - Generar totales aleatorios (1.00 - 10000.00)
  - Aplicar descuento y verificar que el resultado es exactamente `total * (1 - descuento/100)`
  - Verificar que el total con descuento siempre es menor o igual al total original

- [x]* 61. Escribir property test para límite de usos de cupones
  - **Property 30: Coupon Usage Limit**
  - **Validates: Requirements 15.11**
  - En archivo `src/test/java/com/alquiler/furent/service/CouponServicePropertyTest.java`
  - Generar cupones con límites de uso aleatorios usando jqwik
  - Usar cupón hasta alcanzar el límite y verificar que todas las operaciones tienen éxito
  - Intentar usar cupón una vez más y verificar que lanza `InvalidOperationException`

- [x] 62. Crear endpoint POST /api/cupones/validar
  - Modificar archivo `src/main/java/com/alquiler/furent/controller/ApiController.java` o crear `CouponController.java`
  - Implementar endpoint que recibe código de cupón y retorna validación
  - Retornar información del cupón si es válido: descuento, vigencia
  - Retornar error con razón específica si es inválido
  - _Requirements: 15.1, 15.2, 15.3, 15.5_

- [x] 63. Integrar cupones en cotización (ApiController)
  - Modificar método `createQuote()` en `ApiController.java`
  - Agregar campo opcional `codigoCupon` en `CotizacionRequest`
  - Si se proporciona cupón, validar y aplicar descuento al total
  - Guardar código de cupón en la reserva
  - Incrementar contador de usos del cupón al crear la reserva
  - Mostrar total original y total con descuento en respuesta
  - _Requirements: 15.4, 15.6, 15.9_

- [x] 64. Crear AdminCuponesController para gestión
  - Crear archivo `src/main/java/com/alquiler/furent/controller/AdminCuponesController.java`
  - Agregar anotación `@Controller` y `@RequestMapping("/admin/cupones")`
  - Implementar endpoint `GET /admin/cupones` que lista cupones
  - Implementar endpoint `POST /admin/cupones/guardar` que crea o edita cupón
  - Implementar endpoint `DELETE /admin/cupones/{id}` que elimina cupón
  - Validar que el descuento no exceda 100%
  - Mostrar contador de usos actuales vs límite
  - _Requirements: 15.7, 15.8_

- [x] 65. Crear template admin/cupones.html
  - Crear archivo `src/main/resources/templates/admin/cupones.html`
  - Mostrar tabla con cupones: código, descuento, vigencia, usos, estado
  - Agregar formulario modal para crear/editar cupón
  - Mostrar indicador visual de cupones activos/inactivos
  - Mostrar barra de progreso de usos (usosActuales / limiteUsos)
  - Incluir tokens CSRF en todos los formularios
  - _Requirements: 15.7_

- [x] 66. Checkpoint - Verificar sistema de cupones completo
  - Crear cupón de prueba con descuento del 15%
  - Probar aplicar cupón en cotización y verificar descuento correcto
  - Probar que cupón expirado no se puede usar
  - Probar que cupón con límite de usos alcanzado no se puede usar
  - Verificar que el contador de usos se incrementa correctamente
  - Preguntar al usuario si hay dudas antes de continuar

### Phase 9: Property-Based Testing (Prioridad P0)

- [x] 67. Configurar jqwik para property-based testing
  - Agregar dependencia jqwik en `pom.xml` con versión 1.8.2
  - Crear archivo `src/test/resources/jqwik.properties` con configuración
  - Configurar mínimo 100 iteraciones por property test
  - Configurar shrinking bounded a 1000 iteraciones
  - _Requirements: Testing Strategy_

- [ ]* 68. Escribir property test para idempotencia de productos relacionados
  - **Property 1: Related Products Idempotence**
  - **Validates: Requirements 1.4**
  - Crear archivo `src/test/java/com/alquiler/furent/service/ProductServicePropertyTest.java`
  - Generar productos con categorías válidas usando jqwik
  - Llamar a `getRelatedProducts()` dos veces con el mismo producto
  - Verificar que ambas llamadas retornan el mismo conjunto de productos

- [ ]* 69. Escribir property test para cálculo de disponibilidad por estado de mantenimiento
  - **Property 2: Maintenance State Availability Calculation**
  - **Validates: Requirements 2.1, 2.2, 2.3**
  - En archivo `src/test/java/com/alquiler/furent/service/ProductServicePropertyTest.java`
  - Generar productos con stock > 0 y estados de mantenimiento aleatorios
  - Verificar que disponibilidad es true si y solo si estado NO es "EN_REPARACION"

- [ ]* 70. Escribir property test para reversibilidad de estado de mantenimiento
  - **Property 3: Maintenance State Reversibility**
  - **Validates: Requirements 2.4**
  - En archivo `src/test/java/com/alquiler/furent/service/ProductServicePropertyTest.java`
  - Generar productos con stock > 0
  - Cambiar estado a "EN_REPARACION" y verificar que no está disponible
  - Cambiar estado a "OPERATIVO" y verificar que vuelve a estar disponible

- [ ]* 71. Escribir property test para protección CSRF
  - **Property 4: CSRF Token Protection**
  - **Validates: Requirements 3.2, 3.3, 3.5**
  - Crear archivo `src/test/java/com/alquiler/furent/security/CSRFProtectionPropertyTest.java`
  - Generar formularios web con tokens CSRF válidos
  - Enviar formulario con token válido y verificar que tiene éxito (HTTP 200)
  - Invalidar sesión y reenviar con mismo token
  - Verificar que falla con HTTP 403

- [ ]* 72. Escribir property test para validación de uploads de archivos
  - **Property 5: File Upload Validation**
  - **Validates: Requirements 4.1, 4.2, 4.3**
  - Crear archivo `src/test/java/com/alquiler/furent/controller/FileUploadPropertyTest.java`
  - Generar archivos con Content-Types aleatorios usando jqwik
  - Verificar que solo se aceptan {image/jpeg, image/png, image/webp, image/gif}
  - Generar archivos con tamaños aleatorios
  - Verificar que se rechazan archivos > 5MB

- [ ]* 73. Escribir property test para round trip de uploads
  - **Property 6: File Upload Round Trip**
  - **Validates: Requirements 4.5**
  - En archivo `src/test/java/com/alquiler/furent/controller/FileUploadPropertyTest.java`
  - Generar imágenes válidas con contenido aleatorio
  - Subir imagen y obtener URL
  - Descargar imagen desde URL
  - Verificar que el contenido descargado es idéntico al original

- [ ]* 74. Escribir property test para invariante de validación de fechas
  - **Property 7: Date Validation Invariant**
  - **Validates: Requirements 5.1, 5.2, 5.3, 5.5**
  - Crear archivo `src/test/java/com/alquiler/furent/controller/ApiControllerPropertyTest.java`
  - Generar pares de fechas aleatorias usando jqwik
  - Verificar que el sistema acepta solo cotizaciones donde fechaFin >= fechaInicio
  - Verificar que el sistema acepta solo cotizaciones donde fechaInicio >= hoy

- [ ]* 75. Escribir property test para consistencia de validación de entradas
  - **Property 8: Input Validation Consistency**
  - **Validates: Requirements 8.6**
  - En archivo `src/test/java/com/alquiler/furent/controller/ApiControllerPropertyTest.java`
  - Generar entradas válidas aleatorias
  - Validar entrada, modificar con cambios válidos, revalidar
  - Verificar que las mismas reglas de validación se aplican consistentemente

- [ ]* 76. Escribir property test para inicialización de pagos
  - **Property 9: Payment Initialization**
  - **Validates: Requirements 9.1, 9.2**
  - Ya implementado en tarea 14, verificar que existe

- [ ]* 77. Escribir property test para efectos secundarios de confirmación de pagos
  - **Property 10: Payment Confirmation Side Effects**
  - **Validates: Requirements 9.3, 9.4, 9.7, 9.8**
  - Crear archivo `src/test/java/com/alquiler/furent/service/PaymentServicePropertyTest.java`
  - Generar pagos pendientes aleatorios
  - Confirmar pago y verificar: estado=PAGADO, fechaPago actualizada, reserva=ENTREGADA, email enviado, métrica incrementada

- [ ]* 78. Escribir property test para validación de estado de reserva en pagos
  - **Property 12: Payment Reservation State Validation**
  - **Validates: Requirements 9.10**
  - En archivo `src/test/java/com/alquiler/furent/service/PaymentServicePropertyTest.java`
  - Generar reservas con estados aleatorios diferentes de CONFIRMADA
  - Intentar iniciar pago y verificar que lanza `InvalidOperationException`

- [ ]* 79. Escribir property test para notificaciones por email en eventos
  - **Property 13: Email Notification on Events**
  - **Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.10**
  - Ya implementado en tarea 22, verificar que existe

- [ ]* 80. Escribir property test para creación de tokens de reset
  - **Property 15: Password Reset Token Creation**
  - **Validates: Requirements 11.1, 11.2**
  - Crear archivo `src/test/java/com/alquiler/furent/service/PasswordResetServicePropertyTest.java`
  - Generar usuarios aleatorios
  - Crear token y verificar que expiresAt = now + 1 hora
  - Crear segundo token para mismo usuario y verificar que el primero se invalida

- [ ]* 81. Escribir property test para validación de tokens expirados
  - **Property 16: Password Reset Token Validation**
  - **Validates: Requirements 11.5**
  - En archivo `src/test/java/com/alquiler/furent/service/PasswordResetServicePropertyTest.java`
  - Generar tokens expirados y usados
  - Intentar resetear contraseña y verificar que lanza `InvalidOperationException`

- [ ]* 82. Escribir property test para encriptación de contraseñas
  - **Property 18: Password Encryption**
  - **Validates: Requirements 11.7**
  - En archivo `src/test/java/com/alquiler/furent/service/PasswordResetServicePropertyTest.java`
  - Generar contraseñas aleatorias en texto plano
  - Resetear contraseña y verificar que se almacena encriptada con BCrypt
  - Verificar que la contraseña encriptada no es igual a la original

- [ ]* 83. Escribir property test para rechazo de login de usuarios suspendidos
  - **Property 20: Suspended User Login Rejection**
  - **Validates: Requirements 12.5**
  - Crear archivo `src/test/java/com/alquiler/furent/service/UserServicePropertyTest.java`
  - Generar usuarios suspendidos aleatorios
  - Intentar login y verificar que falla con mensaje "Cuenta suspendida"

- [ ]* 84. Escribir property test para auditoría de acciones de admin
  - **Property 21: Admin Action Audit Logging**
  - **Validates: Requirements 12.6, 13.7**
  - En archivo `src/test/java/com/alquiler/furent/service/UserServicePropertyTest.java`
  - Generar acciones de admin aleatorias (suspender, activar, cambiar rol, eliminar)
  - Ejecutar acción y verificar que se crea entrada en AuditLog
  - Verificar que la entrada contiene: acción, recurso, usuario admin, timestamp

- [ ]* 85. Escribir property test para soft delete de usuarios
  - **Property 22: User Soft Delete**
  - **Validates: Requirements 12.4**
  - En archivo `src/test/java/com/alquiler/furent/service/UserServicePropertyTest.java`
  - Generar usuarios aleatorios
  - Realizar soft delete y verificar que deleted=true
  - Verificar que todos los datos del usuario se preservan (email, nombre, etc.)

- [ ]* 86. Escribir property test para eliminación de categorías con productos
  - **Property 24: Category Deletion with Products**
  - **Validates: Requirements 13.3**
  - Ya implementado en tarea 46, verificar que existe

- [ ]* 87. Escribir property test para validación de mensajes de contacto
  - **Property 25: Contact Message Validation**
  - **Validates: Requirements 14.2, 14.3, 14.4**
  - Crear archivo `src/test/java/com/alquiler/furent/service/ContactServicePropertyTest.java`
  - Generar mensajes con campos aleatorios
  - Verificar que se rechazan mensajes con nombre, email o mensaje vacíos
  - Verificar que se rechazan emails con formato inválido
  - Verificar que se rechazan mensajes > 2000 caracteres

- [ ]* 88. Escribir property test para cambio de estado de mensajes
  - **Property 26: Contact Message State Change**
  - **Validates: Requirements 14.7**
  - En archivo `src/test/java/com/alquiler/furent/service/ContactServicePropertyTest.java`
  - Generar mensajes con estado NO_LEIDO
  - Abrir mensaje y verificar que estado cambia a LEIDO

- [ ]* 89. Escribir property test para validación de cupones
  - **Property 28: Coupon Validation**
  - **Validates: Requirements 15.1, 15.2, 15.3**
  - Crear archivo `src/test/java/com/alquiler/furent/service/CouponServicePropertyTest.java`
  - Generar cupones con diferentes estados (activo, expirado, límite alcanzado)
  - Verificar que cupón es válido si y solo si: existe, está vigente, no alcanzó límite de usos

- [x] 90. Checkpoint - Verificar property-based tests completos
  - Ejecutar todos los property tests con `mvn test`
  - Verificar que todos los tests pasan con al menos 100 iteraciones
  - Revisar logs de jqwik para ver casos generados
  - Verificar que los tests cubren las 30 propiedades de corrección
  - Preguntar al usuario si hay dudas antes de continuar

### Phase 10: Unit Testing (Prioridad P0)

- [ ]* 91. Escribir unit tests para ProductService
  - Crear archivo `src/test/java/com/alquiler/furent/service/ProductServiceTest.java`
  - Test: `testGetRelatedProducts_WithValidCategory_ReturnsRelatedProducts()`
  - Test: `testGetRelatedProducts_WithNoRelatedProducts_ReturnsEmptyList()`
  - Test: `testGetRelatedProducts_DoesNotThrowException()`
  - Usar Testcontainers para MongoDB
  - _Requirements: 1.1, 1.2, 1.3_

- [ ]* 92. Escribir unit tests para PaymentService
  - Crear archivo `src/test/java/com/alquiler/furent/service/PaymentServiceTest.java`
  - Test: `testInitPayment_WithConfirmedReservation_CreatesPayment()`
  - Test: `testInitPayment_WithNonConfirmedReservation_ThrowsException()`
  - Test: `testConfirmPayment_UpdatesStateAndReservation()`
  - Test: `testConfirmPayment_AlreadyProcessed_ThrowsException()`
  - Test: `testFailPayment_UpdatesStateAndNotifies()`
  - Usar Testcontainers para MongoDB
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.9_

- [ ]* 93. Escribir unit tests para EmailService
  - Crear archivo `src/test/java/com/alquiler/furent/service/EmailServiceTest.java`
  - Test: `testSendWelcomeEmail_SendsEmailAsync()`
  - Test: `testSendPaymentConfirmation_IncludesPaymentDetails()`
  - Test: `testSendEmail_OnFailure_LogsErrorWithoutThrowing()`
  - Usar mocks para JavaMailSender
  - _Requirements: 10.1, 10.4, 10.8_

- [ ]* 94. Escribir unit tests para PasswordResetService
  - Crear archivo `src/test/java/com/alquiler/furent/service/PasswordResetServiceTest.java`
  - Test: `testCreateToken_InvalidatesOldTokens()`
  - Test: `testCreateToken_SetsExpirationOneHour()`
  - Test: `testResetPassword_WithValidToken_UpdatesPassword()`
  - Test: `testResetPassword_WithExpiredToken_ThrowsException()`
  - Test: `testResetPassword_WithUsedToken_ThrowsException()`
  - Usar Testcontainers para MongoDB
  - _Requirements: 11.1, 11.2, 11.4, 11.5_

- [ ]* 95. Escribir unit tests para CouponService
  - Crear archivo `src/test/java/com/alquiler/furent/service/CouponServiceTest.java`
  - Test: `testValidateCoupon_WithValidCoupon_ReturnsTrue()`
  - Test: `testValidateCoupon_WithExpiredCoupon_ThrowsException()`
  - Test: `testValidateCoupon_WithMaxUsage_ThrowsException()`
  - Test: `testApplyDiscount_CalculatesCorrectly()`
  - Usar Testcontainers para MongoDB
  - _Requirements: 15.1, 15.2, 15.3, 15.4_

- [ ]* 96. Escribir unit tests para UserService
  - Crear archivo `src/test/java/com/alquiler/furent/service/UserServiceTest.java`
  - Test: `testSuspendUser_ChangesStateToSuspended()`
  - Test: `testActivateUser_ChangesStateToActive()`
  - Test: `testSoftDeleteUser_PreservesData()`
  - Test: `testChangeUserRole_UpdatesRole()`
  - Usar Testcontainers para MongoDB
  - _Requirements: 12.1, 12.2, 12.3, 12.4_

- [ ]* 97. Escribir unit tests para ContactService
  - Crear archivo `src/test/java/com/alquiler/furent/service/ContactServiceTest.java`
  - Test: `testSave_CreatesMessageWithUnreadState()`
  - Test: `testMarkAsRead_ChangesState()`
  - Test: `testCountUnread_ReturnsCorrectCount()`
  - Usar Testcontainers para MongoDB
  - _Requirements: 14.1, 14.7_

- [ ]* 98. Escribir integration tests para ApiController
  - Crear archivo `src/test/java/com/alquiler/furent/controller/ApiControllerIntegrationTest.java`
  - Test: `testCreateQuote_WithValidDates_CreatesReservation()`
  - Test: `testCreateQuote_WithInvalidDates_ReturnsBadRequest()`
  - Test: `testCreateQuote_WithPastDate_ReturnsBadRequest()`
  - Test: `testCreateQuote_WithCoupon_AppliesDiscount()`
  - Usar `@SpringBootTest` y `@AutoConfigureMockMvc`
  - _Requirements: 5.1, 5.2, 5.3, 15.4_

- [ ]* 99. Escribir integration tests para AdminProductosController
  - Crear archivo `src/test/java/com/alquiler/furent/controller/AdminProductosControllerIntegrationTest.java`
  - Test: `testUploadImage_WithValidImage_Succeeds()`
  - Test: `testUploadImage_WithInvalidType_ReturnsError()`
  - Test: `testUploadImage_WithLargeFile_ReturnsError()`
  - Usar `@SpringBootTest` y `@AutoConfigureMockMvc`
  - _Requirements: 4.1, 4.2, 4.3_

- [ ]* 100. Escribir integration tests para PaymentController
  - Crear archivo `src/test/java/com/alquiler/furent/controller/PaymentControllerIntegrationTest.java`
  - Test: `testInitPayment_WithValidReservation_ReturnsPayment()`
  - Test: `testConfirmPayment_AsAdmin_UpdatesPayment()`
  - Usar `@SpringBootTest` y `@AutoConfigureMockMvc`
  - _Requirements: 9.1, 9.3_

- [ ]* 101. Escribir security tests para CSRF
  - Crear archivo `src/test/java/com/alquiler/furent/security/CSRFProtectionTest.java`
  - Test: `testFormSubmission_WithoutCSRF_Returns403()`
  - Test: `testFormSubmission_WithValidCSRF_Succeeds()`
  - Test: `testAPIEndpoint_WithoutCSRF_Succeeds()`
  - Usar `@SpringBootTest` y `@AutoConfigureMockMvc`
  - _Requirements: 3.2, 3.3, 3.4_

- [ ]* 102. Escribir security tests para file uploads
  - Crear archivo `src/test/java/com/alquiler/furent/security/FileUploadSecurityTest.java`
  - Test: `testUpload_WithExecutableFile_Rejected()`
  - Test: `testUpload_WithScriptFile_Rejected()`
  - Test: `testUpload_WithValidImage_Accepted()`
  - Usar `@SpringBootTest` y `@AutoConfigureMockMvc`
  - _Requirements: 4.1, 4.2_

- [x] 103. Checkpoint - Verificar cobertura de tests
  - Ejecutar `mvn clean test` para correr todos los tests
  - Generar reporte de cobertura con JaCoCo: `mvn jacoco:report`
  - Verificar que la cobertura es >= 70%
  - Revisar reporte en `target/site/jacoco/index.html`
  - Identificar áreas con baja cobertura y agregar tests si es necesario
  - Preguntar al usuario si hay dudas antes de continuar

### Phase 11: CI/CD and Documentation (Prioridad P1)

- [x] 104. Configurar GitHub Actions para CI/CD
  - Crear archivo `.github/workflows/test.yml`
  - Configurar workflow que ejecuta tests en cada push y pull request
  - Configurar JDK 17 con Temurin distribution
  - Ejecutar `mvn clean test` en el workflow
  - Generar reporte de cobertura con JaCoCo
  - Subir reporte a Codecov (opcional)
  - _Requirements: Testing Strategy_

- [x] 105. Actualizar README.md con nuevas funcionalidades
  - Modificar archivo `README.md`
  - Agregar sección de funcionalidades implementadas: pagos, emails, password reset, cupones, contacto
  - Actualizar instrucciones de configuración con variables de entorno necesarias
  - Agregar sección de testing con comandos para ejecutar tests
  - Documentar endpoints nuevos de API REST
  - _Requirements: Documentation_

- [ ] 106. Actualizar documentación de API con Swagger
  - Verificar que Swagger UI está habilitado en `/swagger-ui.html`
  - Agregar anotaciones `@Operation`, `@ApiResponse` en controllers nuevos
  - Documentar endpoints de pagos, cupones, password reset
  - Agregar ejemplos de request/response
  - _Requirements: Documentation_

- [x] 107. Crear guía de despliegue
  - Crear archivo `docs/DEPLOYMENT.md`
  - Documentar variables de entorno requeridas para producción
  - Documentar proceso de configuración de SMTP
  - Documentar creación de índices MongoDB
  - Documentar configuración de Redis
  - Incluir checklist de pre-deployment y post-deployment
  - _Requirements: Documentation_

- [x] 108. Configurar variables de entorno para producción
  - Crear archivo `application-prod.properties` con placeholders
  - Documentar todas las variables de entorno necesarias:
    - `MONGODB_URI`: URI de conexión a MongoDB
    - `REDIS_HOST`, `REDIS_PORT`: Configuración de Redis
    - `FURENT_ADMIN_PASSWORD`: Contraseña del admin inicial
    - `JWT_SECRET`: Secret key para JWT
    - `MAIL_USERNAME`, `MAIL_PASSWORD`: Credenciales SMTP
    - `UPLOAD_DIR`: Directorio de uploads
  - _Requirements: Configuration_

- [x] 109. Final checkpoint - Verificar implementación completa
  - Ejecutar la aplicación en modo producción
  - Verificar que todos los bugs están corregidos
  - Verificar que todas las funcionalidades nuevas funcionan correctamente
  - Ejecutar suite completa de tests y verificar que todos pasan
  - Revisar logs para detectar errores o warnings
  - Verificar que la documentación está actualizada
  - Preguntar al usuario si hay dudas o ajustes finales

## Notes

### Tasks Marked with `*` are Optional

Las tareas marcadas con `*` son tests opcionales que pueden omitirse para un MVP más rápido. Sin embargo, se recomienda implementarlas para garantizar la calidad y correctitud del sistema.

### Requirements Coverage

Cada tarea referencia los requisitos específicos que implementa, asegurando trazabilidad completa desde requisitos hasta código.

### Checkpoints

Los checkpoints están distribuidos estratégicamente para validar el progreso incremental y permitir al usuario revisar el trabajo antes de continuar con la siguiente fase.

### Property-Based Tests

Las 30 propiedades de corrección del diseño están mapeadas a property tests específicos usando jqwik. Cada property test incluye:
- Número de propiedad del diseño
- Requisitos que valida
- Mínimo 100 iteraciones por test

### Integration Strategy

El plan sigue un enfoque de integración continua donde cada tarea construye sobre las anteriores:
1. Bugs y seguridad primero (base estable)
2. Sistema de pagos (funcionalidad crítica)
3. Notificaciones email (soporte a pagos)
4. Features adicionales (password reset, CRUD, cupones, contacto)
5. Testing comprehensivo (validación de calidad)
6. CI/CD y documentación (preparación para producción)

### Estimated Effort

- Phase 1 (Bugs & Security): 12 horas
- Phase 2 (Payment System): 16 horas
- Phase 3 (Email Notifications): 12 horas
- Phase 4 (Password Reset): 8 horas
- Phase 5 (Admin CRUD Users): 10 horas
- Phase 6 (Admin CRUD Categories): 6 horas
- Phase 7 (Contact Form): 6 horas
- Phase 8 (Coupon System): 8 horas
- Phase 9 (Property-Based Testing): 20 horas
- Phase 10 (Unit Testing): 16 horas
- Phase 11 (CI/CD & Docs): 6 horas

**Total estimado: 120 horas (~3 semanas a tiempo completo)**

### Success Criteria

La implementación se considera completa cuando:
- ✅ Todos los 5 bugs críticos están corregidos
- ✅ CSRF está habilitado y funcionando
- ✅ Sistema de pagos end-to-end operativo
- ✅ Notificaciones por email funcionando
- ✅ Recuperación de contraseña operativa
- ✅ CRUD de usuarios y categorías completo
- ✅ Sistema de cupones funcionando
- ✅ Formulario de contacto operativo
- ✅ 30 property tests implementados y pasando
- ✅ Cobertura de tests >= 70%
- ✅ CI/CD configurado y funcionando
- ✅ Documentación actualizada

