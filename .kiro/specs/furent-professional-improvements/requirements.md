# Requirements Document

## Introduction

Este documento especifica los requisitos para implementar las mejoras profesionales críticas de Furent, una plataforma SaaS multi-tenant de alquiler de mobiliarios para eventos. El alcance incluye la corrección de 5 bugs críticos, mejoras de seguridad esenciales, y la implementación completa de funcionalidades core (pagos, notificaciones, recuperación de contraseña, CRUD administrativo, contacto y cupones). El objetivo es elevar la calificación del sistema de 6.8/10 a 9.5/10, eliminando vulnerabilidades de seguridad y completando el MVP funcional.

## Glossary

- **System**: La plataforma Furent completa (backend Spring Boot + frontend Thymeleaf)
- **Product_Service**: Servicio que gestiona productos y catálogo
- **Reservation_Service**: Servicio que gestiona reservas y disponibilidad
- **Payment_Service**: Servicio que procesa pagos y transacciones
- **Email_Service**: Servicio que envía notificaciones por correo electrónico
- **Security_Config**: Configuración de Spring Security para autenticación y autorización
- **Admin_Controller**: Controlador para operaciones administrativas
- **API_Controller**: Controlador REST para operaciones de clientes
- **User**: Usuario del sistema (cliente o administrador)
- **Admin**: Usuario con rol administrativo
- **Product**: Mobiliario disponible para alquiler
- **Reservation**: Solicitud de alquiler de productos
- **Payment**: Transacción de pago asociada a una reserva
- **CSRF_Token**: Token de protección contra Cross-Site Request Forgery
- **Password_Reset_Token**: Token temporal para recuperación de contraseña
- **Coupon**: Cupón de descuento aplicable a reservas
- **Contact_Message**: Mensaje enviado por formulario de contacto
- **Category**: Categoría de productos (sillas, mesas, decoración, etc.)
- **Maintenance_State**: Estado de mantenimiento de un producto (OPERATIVO, EN_REPARACION)
- **Reservation_State**: Estado de una reserva (PENDIENTE, CONFIRMADA, ACTIVA, COMPLETADA, CANCELADA)
- **Payment_State**: Estado de un pago (PENDIENTE, PAGADO, FALLIDO)

## Requirements

### Requirement 1: Corrección de Bug en Productos Relacionados

**User Story:** Como usuario, quiero ver productos relacionados en la página de detalle, para que pueda explorar opciones similares sin errores del sistema.

#### Acceptance Criteria

1. WHEN THE Product_Service obtiene productos relacionados, THE Product_Service SHALL filtrar por categoriaNombre en lugar de getCategory()
2. WHEN un usuario visualiza el detalle de un producto, THE System SHALL mostrar productos relacionados sin lanzar RuntimeException
3. THE Product_Service SHALL retornar una lista vacía si no hay productos relacionados disponibles
4. FOR ALL productos con categoría válida, obtener productos relacionados y luego obtener productos relacionados nuevamente SHALL retornar el mismo conjunto de productos (idempotencia)

### Requirement 2: Corrección de Validación de Estado de Mantenimiento

**User Story:** Como administrador, quiero que el sistema calcule correctamente la disponibilidad de productos, para que los productos operativos se muestren como disponibles.

#### Acceptance Criteria

1. WHEN THE Admin_Controller calcula disponibilidad de un producto, THE Admin_Controller SHALL considerar disponible si el estado NO es "EN_REPARACION"
2. WHEN un producto tiene stock mayor a 0 y estado "OPERATIVO", THE System SHALL marcar el producto como disponible
3. WHEN un producto tiene estado "EN_REPARACION", THE System SHALL marcar el producto como no disponible independientemente del stock
4. FOR ALL productos, cambiar estado a "EN_REPARACION" y luego a "OPERATIVO" SHALL restaurar la disponibilidad si hay stock (reversibilidad)

### Requirement 3: Habilitación de Protección CSRF

**User Story:** Como usuario, quiero que mis formularios web estén protegidos contra ataques CSRF, para que mi cuenta y datos estén seguros.

#### Acceptance Criteria

1. THE Security_Config SHALL habilitar protección CSRF para todas las rutas excepto "/api/**"
2. WHEN un formulario Thymeleaf es renderizado, THE System SHALL incluir un campo oculto con el CSRF_Token
3. WHEN un formulario es enviado sin CSRF_Token válido, THE System SHALL rechazar la petición con código HTTP 403
4. WHEN una petición a "/api/**" es enviada sin CSRF_Token, THE System SHALL procesar la petición normalmente
5. FOR ALL formularios web, enviar con token válido, invalidar sesión, y reenviar con mismo token SHALL fallar (tokens de un solo uso)

### Requirement 4: Validación de Tipos de Archivo en Uploads

**User Story:** Como administrador, quiero que el sistema solo acepte imágenes válidas en uploads, para prevenir vulnerabilidades de seguridad.

#### Acceptance Criteria

1. WHEN un archivo es subido, THE Admin_Controller SHALL validar que el Content-Type sea uno de: image/jpeg, image/png, image/webp, image/gif
2. WHEN un archivo con tipo no permitido es subido, THE System SHALL rechazar el upload y mostrar mensaje de error
3. WHEN un archivo mayor a 5MB es subido, THE System SHALL rechazar el upload y mostrar mensaje de error
4. WHEN un archivo válido es subido, THE System SHALL almacenar el archivo y retornar la URL
5. FOR ALL archivos válidos, subir y luego descargar SHALL retornar el mismo contenido (round-trip)

### Requirement 5: Validación de Fechas en Cotización

**User Story:** Como usuario, quiero que el sistema valide las fechas de mi cotización, para evitar crear reservas con fechas inválidas.

#### Acceptance Criteria

1. WHEN una cotización es enviada, THE API_Controller SHALL validar que fechaFin no sea anterior a fechaInicio
2. WHEN fechaFin es anterior a fechaInicio, THE System SHALL retornar error HTTP 400 con mensaje descriptivo
3. WHEN fechaInicio es anterior a la fecha actual, THE System SHALL retornar error HTTP 400
4. WHEN las fechas son válidas, THE System SHALL crear la reserva exitosamente
5. FOR ALL cotizaciones válidas, la diferencia entre fechaFin y fechaInicio SHALL ser mayor o igual a 0 días (invariante)

### Requirement 6: Headers de Seguridad HTTP

**User Story:** Como usuario, quiero que el sistema implemente headers de seguridad estándar, para proteger mi navegación contra ataques comunes.

#### Acceptance Criteria

1. THE Security_Config SHALL configurar Content-Security-Policy con directivas restrictivas
2. THE Security_Config SHALL habilitar HTTP Strict Transport Security (HSTS) con max-age de 31536000 segundos
3. THE Security_Config SHALL configurar X-Frame-Options como DENY
4. THE Security_Config SHALL habilitar X-XSS-Protection con modo block
5. WHEN una respuesta HTTP es enviada, THE System SHALL incluir todos los headers de seguridad configurados

### Requirement 7: Password de Admin Configurable

**User Story:** Como administrador de sistemas, quiero configurar la contraseña del admin inicial vía variable de entorno, para evitar usar credenciales por defecto en producción.

#### Acceptance Criteria

1. THE System SHALL leer la contraseña del admin desde la variable de entorno FURENT_ADMIN_PASSWORD
2. WHEN la variable no está definida, THE System SHALL generar un UUID aleatorio como contraseña
3. WHEN el admin inicial es creado, THE System SHALL registrar la contraseña en los logs con advertencia de seguridad
4. THE System SHALL encriptar la contraseña con BCrypt antes de almacenarla
5. WHEN el sistema inicia sin admin existente, THE System SHALL crear el admin con email "admin@furent.com"

### Requirement 8: Validación y Sanitización de Entradas

**User Story:** Como usuario, quiero que el sistema valide mis entradas, para recibir feedback claro sobre errores en formularios.

#### Acceptance Criteria

1. WHEN una cotización es enviada, THE System SHALL validar que tipoEvento contenga solo letras, números y espacios
2. WHEN el campo invitados es enviado, THE System SHALL validar que esté entre 1 y 10,000
3. WHEN el campo notas excede 1000 caracteres, THE System SHALL retornar error de validación
4. WHEN la lista de items está vacía, THE System SHALL retornar error "Debe seleccionar al menos un producto"
5. THE System SHALL aplicar Bean Validation (@Valid) en todos los DTOs de entrada
6. FOR ALL entradas válidas, enviar datos, validar, modificar y revalidar SHALL mantener las mismas reglas (consistencia)

### Requirement 9: Sistema de Pagos Completo

**User Story:** Como usuario, quiero iniciar y completar pagos para mis reservas, para que pueda confirmar mi alquiler de mobiliarios.

#### Acceptance Criteria

1. WHEN un usuario inicia un pago, THE Payment_Service SHALL crear un Payment con estado PENDIENTE
2. WHEN un pago es iniciado, THE Payment_Service SHALL generar una referencia única con formato "PAY-XXXXXXXX"
3. WHEN un admin confirma un pago, THE Payment_Service SHALL cambiar el estado a PAGADO y actualizar fechaPago
4. WHEN un pago es confirmado, THE System SHALL cambiar la reserva asociada a estado ENTREGADA
5. WHEN un admin rechaza un pago, THE Payment_Service SHALL cambiar el estado a FALLIDO
6. WHEN un pago cambia de estado, THE System SHALL crear una notificación para el usuario
7. WHEN un pago es confirmado, THE System SHALL enviar email de confirmación al usuario
8. WHEN un pago es confirmado, THE System SHALL incrementar la métrica paymentsCompleted
9. IF un pago ya fue procesado, THEN THE Payment_Service SHALL rechazar cambios de estado con InvalidOperationException
10. THE Payment_Service SHALL solo permitir pagos para reservas en estado CONFIRMADA
11. FOR ALL pagos, iniciar pago, confirmar pago, y luego intentar confirmar nuevamente SHALL fallar (idempotencia)

### Requirement 10: Notificaciones por Email

**User Story:** Como usuario, quiero recibir emails sobre eventos importantes, para estar informado del estado de mis reservas y pagos.

#### Acceptance Criteria

1. WHEN un usuario se registra, THE Email_Service SHALL enviar email de bienvenida
2. WHEN una reserva es confirmada, THE Email_Service SHALL enviar email de confirmación con detalles
3. WHEN el estado de una reserva cambia, THE Email_Service SHALL enviar email notificando el cambio
4. WHEN un pago es confirmado, THE Email_Service SHALL enviar recibo de pago en formato HTML
5. WHEN un usuario solicita reset de contraseña, THE Email_Service SHALL enviar email con token y link
6. THE Email_Service SHALL enviar todos los emails de forma asíncrona usando @Async
7. THE Email_Service SHALL usar templates Thymeleaf para renderizar HTML
8. WHEN falla el envío de un email, THE System SHALL registrar el error en logs sin interrumpir el flujo principal
9. THE Email_Service SHALL configurar el remitente como "Furent" con el email configurado
10. FOR ALL emails enviados, el contenido SHALL incluir información específica del evento (no genérico)

### Requirement 11: Recuperación de Contraseña

**User Story:** Como usuario, quiero recuperar mi contraseña si la olvido, para poder acceder nuevamente a mi cuenta.

#### Acceptance Criteria

1. WHEN un usuario solicita reset de contraseña, THE System SHALL crear un Password_Reset_Token con expiración de 1 hora
2. WHEN un token es creado, THE System SHALL invalidar todos los tokens anteriores del mismo usuario
3. WHEN un usuario solicita reset con email inexistente, THE System SHALL mostrar mensaje genérico sin revelar si el email existe
4. WHEN un usuario confirma reset con token válido, THE System SHALL cambiar la contraseña y marcar el token como usado
5. WHEN un token expirado o usado es utilizado, THE System SHALL rechazar la operación con InvalidOperationException
6. WHEN las contraseñas no coinciden en confirmación, THE System SHALL mostrar error de validación
7. WHEN una contraseña es reseteada, THE System SHALL encriptarla con BCrypt antes de almacenar
8. THE System SHALL enviar email con link de reset al usuario
9. THE Password_Reset_Token SHALL contener: userId, token UUID, expiresAt, used, createdAt
10. FOR ALL tokens válidos, usar token para reset SHALL marcar el token como usado permanentemente (irreversible)

### Requirement 12: CRUD Completo de Usuarios (Admin)

**User Story:** Como administrador, quiero gestionar usuarios completamente, para mantener control sobre las cuentas del sistema.

#### Acceptance Criteria

1. WHEN un admin suspende un usuario, THE System SHALL cambiar el estado a SUSPENDIDO_TEMPORAL
2. WHEN un admin activa un usuario suspendido, THE System SHALL cambiar el estado a ACTIVO
3. WHEN un admin cambia el rol de un usuario, THE System SHALL actualizar el campo role
4. WHEN un admin elimina un usuario, THE System SHALL realizar soft delete (marcar como eliminado sin borrar)
5. WHEN un usuario suspendido intenta login, THE System SHALL rechazar con mensaje "Cuenta suspendida"
6. WHEN un admin realiza una acción sobre un usuario, THE System SHALL registrar la acción en AuditLog
7. THE System SHALL mostrar lista paginada de usuarios con filtros por rol y estado
8. THE System SHALL permitir búsqueda de usuarios por email o nombre
9. FOR ALL usuarios, suspender y luego activar SHALL restaurar el acceso completo (reversibilidad)

### Requirement 13: CRUD Completo de Categorías (Admin)

**User Story:** Como administrador, quiero gestionar categorías de productos, para organizar el catálogo eficientemente.

#### Acceptance Criteria

1. WHEN un admin crea una categoría, THE System SHALL validar que el nombre sea único
2. WHEN un admin edita una categoría, THE System SHALL actualizar nombre, descripción e icono
3. WHEN un admin intenta eliminar una categoría con productos asociados, THE System SHALL rechazar con mensaje de error
4. WHEN un admin elimina una categoría sin productos, THE System SHALL eliminar la categoría exitosamente
5. THE System SHALL mostrar lista de categorías con contador de productos asociados
6. THE System SHALL permitir asignar un icono CSS a cada categoría
7. WHEN una categoría es creada o editada, THE System SHALL registrar la acción en AuditLog
8. FOR ALL categorías, crear, editar nombre, y verificar unicidad SHALL prevenir duplicados (invariante)

### Requirement 14: Formulario de Contacto Funcional

**User Story:** Como usuario, quiero enviar mensajes de contacto, para comunicarme con el equipo de Furent.

#### Acceptance Criteria

1. WHEN un usuario envía un mensaje de contacto, THE System SHALL crear un Contact_Message con estado NO_LEIDO
2. THE System SHALL validar que nombre, email y mensaje sean obligatorios
3. THE System SHALL validar que el email tenga formato válido
4. THE System SHALL validar que el mensaje no exceda 2000 caracteres
5. WHEN un mensaje es enviado, THE System SHALL mostrar confirmación al usuario
6. WHEN un admin visualiza mensajes, THE System SHALL mostrar badge con contador de no leídos
7. WHEN un admin abre un mensaje, THE System SHALL cambiar el estado a LEIDO
8. THE System SHALL mostrar lista paginada de mensajes ordenados por fecha descendente
9. THE System SHALL permitir filtrar mensajes por estado (LEIDO/NO_LEIDO)
10. FOR ALL mensajes, el contador de no leídos SHALL ser igual al número de mensajes con estado NO_LEIDO (invariante)

### Requirement 15: Sistema de Cupones y Descuentos

**User Story:** Como usuario, quiero aplicar cupones de descuento a mis reservas, para obtener precios reducidos.

#### Acceptance Criteria

1. WHEN un usuario valida un cupón, THE System SHALL verificar que el código exista
2. WHEN un cupón es validado, THE System SHALL verificar que esté dentro del rango de vigencia
3. WHEN un cupón es validado, THE System SHALL verificar que no haya alcanzado el límite de usos
4. WHEN un cupón válido es aplicado, THE System SHALL calcular el descuento según el porcentaje configurado
5. WHEN un cupón inválido es aplicado, THE System SHALL retornar error con razón específica
6. WHEN un cupón es usado en una reserva, THE System SHALL incrementar el contador de usos
7. THE System SHALL soportar cupones con descuento porcentual (5%, 10%, 15%, etc.)
8. THE System SHALL validar que el descuento no exceda el 100%
9. WHEN una cotización incluye un cupón, THE System SHALL mostrar el total original y el total con descuento
10. FOR ALL cupones, aplicar descuento SHALL reducir el total en el porcentaje exacto configurado (precisión matemática)
11. FOR ALL cupones con límite de usos, usar el cupón hasta el límite y luego intentar usar nuevamente SHALL fallar (límite estricto)

## Correctness Properties for Property-Based Testing

### Property 1: Productos Relacionados - Idempotencia
```
FOR ALL productos p con categoría válida:
  relatedProducts = getRelatedProducts(p)
  relatedProducts2 = getRelatedProducts(p)
  ASSERT relatedProducts == relatedProducts2
```

### Property 2: Estado de Mantenimiento - Reversibilidad
```
FOR ALL productos p con stock > 0:
  initialAvailability = isAvailable(p)
  setMaintenanceState(p, "EN_REPARACION")
  ASSERT isAvailable(p) == false
  setMaintenanceState(p, "OPERATIVO")
  ASSERT isAvailable(p) == initialAvailability
```

### Property 3: CSRF Token - Un Solo Uso
```
FOR ALL formularios f:
  token = generateCSRFToken()
  response1 = submitForm(f, token)
  ASSERT response1.status == 200
  invalidateSession()
  response2 = submitForm(f, token)
  ASSERT response2.status == 403
```

### Property 4: Upload de Archivos - Round Trip
```
FOR ALL archivos válidos a (imagen JPEG/PNG/WEBP/GIF < 5MB):
  url = upload(a)
  downloaded = download(url)
  ASSERT contentEquals(a, downloaded)
```

### Property 5: Validación de Fechas - Invariante
```
FOR ALL cotizaciones c con fechas válidas:
  ASSERT c.fechaFin >= c.fechaInicio
  ASSERT daysBetween(c.fechaInicio, c.fechaFin) >= 0
```

### Property 6: Pagos - Idempotencia de Confirmación
```
FOR ALL pagos p en estado PENDIENTE:
  confirmPayment(p.id)
  ASSERT p.estado == "PAGADO"
  EXPECT_EXCEPTION(confirmPayment(p.id), InvalidOperationException)
```

### Property 7: Password Reset Token - Irreversibilidad
```
FOR ALL tokens t válidos:
  resetPassword(t.token, "newPassword")
  ASSERT t.used == true
  EXPECT_EXCEPTION(resetPassword(t.token, "anotherPassword"), InvalidOperationException)
```

### Property 8: Suspensión de Usuarios - Reversibilidad
```
FOR ALL usuarios u activos:
  suspend(u.id)
  ASSERT u.estado == "SUSPENDIDO_TEMPORAL"
  ASSERT login(u.email, u.password) == FAIL
  activate(u.id)
  ASSERT u.estado == "ACTIVO"
  ASSERT login(u.email, u.password) == SUCCESS
```

### Property 9: Categorías - Unicidad de Nombres
```
FOR ALL categorías c1, c2:
  IF c1.id != c2.id THEN
    ASSERT c1.nombre != c2.nombre
```

### Property 10: Contador de Mensajes No Leídos - Invariante
```
FOR ALL momentos t:
  unreadCount = getUnreadMessagesCount()
  actualUnread = countMessages(estado == "NO_LEIDO")
  ASSERT unreadCount == actualUnread
```

### Property 11: Cupones - Precisión de Descuento
```
FOR ALL cupones c con descuento d%:
  FOR ALL totales t > 0:
    totalConDescuento = applyCoupon(c, t)
    ASSERT totalConDescuento == t * (1 - d/100)
    ASSERT totalConDescuento <= t
```

### Property 12: Cupones - Límite de Usos
```
FOR ALL cupones c con límite L:
  FOR i = 1 TO L:
    ASSERT useCoupon(c) == SUCCESS
  ASSERT useCoupon(c) == FAIL
```

### Property 13: Email Templates - Contenido Específico
```
FOR ALL emails e generados:
  ASSERT e.body.contains(specificData)
  ASSERT NOT e.body.contains("[PLACEHOLDER]")
```

### Property 14: Validación de Entradas - Consistencia
```
FOR ALL entradas válidas i:
  result1 = validate(i)
  modify(i, validChange)
  result2 = validate(i)
  ASSERT sameRulesApplied(result1, result2)
```

### Property 15: Soft Delete - Preservación de Datos
```
FOR ALL usuarios u:
  originalData = clone(u)
  softDelete(u.id)
  ASSERT u.deleted == true
  ASSERT u.email == originalData.email
  ASSERT u.nombre == originalData.nombre
```
