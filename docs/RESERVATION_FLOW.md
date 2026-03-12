# 🔄 Furent — Flujo de Reservas (Detallado)

> Documentación exhaustiva del ciclo de vida de una reserva en el sistema Furent.

---

## 📋 Tabla de Contenidos

1. [Visión General](#1-visión-general)
2. [Máquina de Estados](#2-máquina-de-estados)
3. [Flujo Completo Paso a Paso](#3-flujo-completo-paso-a-paso)
4. [Reglas de Negocio](#4-reglas-de-negocio)
5. [Notificaciones por Estado](#5-notificaciones-por-estado)
6. [Métricas y Eventos](#6-métricas-y-eventos)
7. [Historial de Estados](#7-historial-de-estados)
8. [Flujo de Cancelación](#8-flujo-de-cancelación)
9. [Integración con Pagos](#9-integración-con-pagos)
10. [Logística (Entrega y Recogida)](#10-logística-entrega-y-recogida)

---

## 1. Visión General

Una **reserva** en Furent representa el alquiler de uno o más mobiliarios para un evento específico. Cada reserva pasa por un ciclo de vida bien definido con transiciones de estado controladas.

### Entidades Involucradas

```mermaid
graph LR
    U["👤 Usuario"] -->|crea| R["📋 Reserva"]
    R -->|contiene| I["📦 Items"]
    I -->|referencia| P["🪑 Producto"]
    R -->|genera| PA["💰 Pago"]
    R -->|registra| SH["📜 Historial de Estados"]
    R -->|dispara| N["🔔 Notificaciones"]
    R -->|emite| E["📊 Eventos de Dominio"]
```

---

## 2. Máquina de Estados

### 2.1 Diagrama de Estados Completo

```mermaid
stateDiagram-v2
    [*] --> PENDIENTE : POST /api/cotizacion
    
    state "📋 PENDIENTE" as PENDIENTE
    state "✅ CONFIRMADA" as CONFIRMADA
    state "🟢 ACTIVA" as ACTIVA
    state "🚛 EN_CURSO" as EN_CURSO
    state "🏁 COMPLETADA" as COMPLETADA
    state "❌ CANCELADA" as CANCELADA

    PENDIENTE --> CONFIRMADA : Admin aprueba
    PENDIENTE --> CANCELADA : Cancelación

    CONFIRMADA --> ACTIVA : Pago confirmado
    CONFIRMADA --> CANCELADA : Cancelación

    ACTIVA --> EN_CURSO : Entrega mobiliario
    ACTIVA --> CANCELADA : Cancelación

    EN_CURSO --> COMPLETADA : Recogida completada
    EN_CURSO --> CANCELADA : Cancelación

    COMPLETADA --> [*] : Estado final
    CANCELADA --> [*] : Estado final
```

### 2.2 Tabla de Transiciones

```
┌──────────────┬──────────────────────────────────┬─────────────────────┐
│ Estado Actual │ Transiciones Permitidas           │ Disparador          │
├──────────────┼──────────────────────────────────┼─────────────────────┤
│ PENDIENTE    │ → CONFIRMADA                     │ Admin aprueba       │
│              │ → CANCELADA                      │ Usuario/Admin       │
├──────────────┼──────────────────────────────────┼─────────────────────┤
│ CONFIRMADA   │ → ACTIVA                         │ Pago confirmado     │
│              │ → CANCELADA                      │ Usuario/Admin       │
├──────────────┼──────────────────────────────────┼─────────────────────┤
│ ACTIVA       │ → EN_CURSO                       │ Entrega realizada   │
│              │ → CANCELADA                      │ Admin               │
├──────────────┼──────────────────────────────────┼─────────────────────┤
│ EN_CURSO     │ → COMPLETADA                     │ Recogida exitosa    │
│              │ → CANCELADA                      │ Admin               │
├──────────────┼──────────────────────────────────┼─────────────────────┤
│ COMPLETADA   │ (sin transiciones - estado final) │                     │
├──────────────┼──────────────────────────────────┼─────────────────────┤
│ CANCELADA    │ (sin transiciones - estado final) │                     │
└──────────────┴──────────────────────────────────┴─────────────────────┘
```

### 2.3 Mapa de Transiciones (Código)

```java
static final Map<String, Set<String>> TRANSITIONS = Map.of(
    "PENDIENTE",   Set.of("CONFIRMADA", "CANCELADA"),
    "CONFIRMADA",  Set.of("ACTIVA", "CANCELADA"),
    "ACTIVA",      Set.of("EN_CURSO", "CANCELADA"),
    "EN_CURSO",    Set.of("COMPLETADA", "CANCELADA"),
    "COMPLETADA",  Set.of(),
    "CANCELADA",   Set.of()
);
```

---

## 3. Flujo Completo Paso a Paso

### 3.1 Fase 1: Creación (Usuario)

```mermaid
flowchart TD
    A([🟢 Usuario inicia cotización]) --> B["Seleccionar productos<br/>del catálogo"]
    B --> C["Definir cantidades<br/>por producto"]
    C --> D["Elegir fechas<br/>(inicio y fin)"]
    D --> E["Llenar datos:<br/>• Tipo de evento<br/>• Dirección<br/>• Método de pago<br/>• Notas"]
    E --> F{"¿Aplicar cupón?"}
    
    F -->|Sí| G["Validar cupón"]
    G --> H{"¿Válido?"}
    H -->|Sí| I["Calcular descuento"]
    H -->|No| J["Mostrar error"]
    J --> E
    
    F -->|No| K["Sin descuento"]
    I --> K
    
    K --> L["POST /api/cotizacion"]
    L --> M{"Validaciones"}
    
    M -->|fechaInicio ≥ fechaFin| N["❌ 400: Fechas inválidas"]
    M -->|fechas en pasado| O["❌ 400: Fechas pasadas"]
    M -->|items vacíos| P["❌ 400: Sin items"]
    
    M -->|✅ Válido| Q["Calcular:<br/>diasAlquiler = fin - inicio<br/>subtotal = Σ(precio × días × qty)<br/>total = subtotal - descuento"]
    
    Q --> R["Guardar en MongoDB<br/>estado = PENDIENTE"]
    R --> S["Publicar<br/>ReservationCreatedEvent"]
    S --> T["Incrementar<br/>reservationsCreated"]
    T --> U([📋 Reserva PENDIENTE])

    style U fill:#ffd43b,color:#000
    style N fill:#ff6b6b,color:#fff
    style O fill:#ff6b6b,color:#fff
    style P fill:#ff6b6b,color:#fff
```

### 3.2 Fase 2: Confirmación (Admin)

```mermaid
flowchart TD
    A([📋 Reserva PENDIENTE]) --> B["Admin revisa reserva<br/>en /admin/reservas"]
    B --> C{"¿Aprobar?"}
    
    C -->|No| D["Cambiar estado<br/>→ CANCELADA"]
    D --> E["Guardar StatusHistory"]
    E --> F["Notificar usuario:<br/>'Reserva rechazada'"]
    F --> CANC([❌ CANCELADA])
    
    C -->|Sí| G["POST /admin/reservas/estado/{id}<br/>nuevoEstado = CONFIRMADA"]
    G --> H["Validar transición:<br/>PENDIENTE → CONFIRMADA ✅"]
    H --> I["Guardar StatusHistory:<br/>{anterior: PENDIENTE,<br/>nuevo: CONFIRMADA,<br/>usuario: admin,<br/>nota: 'Aprobada'}"]
    I --> J["Actualizar reserva<br/>estado = CONFIRMADA<br/>fechaActualizacion = now()"]
    J --> K["Notificar usuario:<br/>'Tu reserva ha sido confirmada'"]
    K --> L["Email: Confirmación<br/>de reserva"]
    L --> CONF([✅ CONFIRMADA])

    style A fill:#ffd43b,color:#000
    style CONF fill:#51cf66,color:#fff
    style CANC fill:#ff6b6b,color:#fff
```

### 3.3 Fase 3: Pago

```mermaid
flowchart TD
    A([✅ CONFIRMADA]) --> B["Usuario inicia pago<br/>POST /api/pagos/iniciar/{reservaId}"]
    B --> C["Crear Payment<br/>estado = PENDIENTE<br/>monto = reservation.total"]
    C --> D["Notificar usuario:<br/>'Pago registrado'"]
    D --> E["👤 Usuario envía<br/>comprobante de pago"]
    E --> F["Admin verifica<br/>comprobante"]
    F --> G{"¿Pago válido?"}
    
    G -->|No| H["failPayment()"]
    H --> I["Payment estado = FALLIDO"]
    I --> J["Notificar usuario:<br/>'Pago rechazado: {motivo}'"]
    J --> K["Reserva sigue<br/>CONFIRMADA"]
    K --> A
    
    G -->|Sí| L["confirmPayment()"]
    L --> M["Payment estado = PAGADO<br/>referencia = REF-xxx<br/>fechaPago = now()"]
    M --> N["Reserva → ACTIVA"]
    N --> O["Guardar StatusHistory"]
    O --> P["metrics: paymentsCompleted++"]
    P --> Q["metrics: addRevenue(monto)"]
    Q --> R["Notificar: 'Pago confirmado ✅'"]
    R --> S["Email: Confirmación de pago"]
    S --> T["Publicar PaymentCompletedEvent"]
    T --> ACT([🟢 ACTIVA])

    style A fill:#51cf66,color:#fff
    style ACT fill:#339af0,color:#fff
    style I fill:#ff6b6b,color:#fff
```

### 3.4 Fase 4: Logística

```mermaid
flowchart TD
    A([🟢 ACTIVA]) --> B["Admin programa<br/>entrega en /admin/logistica"]
    B --> C["Día del evento:<br/>Entregar mobiliario"]
    C --> D["Admin actualiza estado<br/>→ EN_CURSO"]
    D --> E["Guardar StatusHistory"]
    E --> F["Notificar usuario:<br/>'Mobiliario entregado'"]
    F --> G([🚛 EN_CURSO])
    
    G --> H["Después del evento:<br/>Recoger mobiliario"]
    H --> I{"¿Mobiliario<br/>en buen estado?"}
    
    I -->|Sí| J["Admin actualiza estado<br/>→ COMPLETADA"]
    J --> K["Guardar StatusHistory"]
    K --> L["Notificar usuario:<br/>'Reserva completada ✅'"]
    L --> M([🏁 COMPLETADA])
    
    I -->|Daños| N["Registrar daños<br/>en notas de estado"]
    N --> J

    style A fill:#339af0,color:#fff
    style G fill:#ff922b,color:#fff
    style M fill:#51cf66,color:#fff
```

---

## 4. Reglas de Negocio

### 4.1 Validaciones en Creación

| Regla | Descripción | Error |
|:---|:---|:---|
| Fechas válidas | `fechaInicio < fechaFin` | "La fecha de inicio debe ser anterior a la fecha de fin" |
| Fechas futuras | `fechaInicio ≥ hoy` | "Las fechas no pueden ser anteriores a hoy" |
| Items requeridos | `items.size() > 0` | "Debe incluir al menos un producto" |
| Cantidades positivas | `cantidad > 0` por item | "La cantidad debe ser mayor a 0" |

### 4.2 Validaciones en Transición de Estado

```mermaid
flowchart TD
    A["updateStatus(id, nuevoEstado)"] --> B{"¿Reserva existe?"}
    B -->|No| C["❌ ResourceNotFoundException"]
    B -->|Sí| D{"¿Transición válida?<br/>TRANSITIONS.get(actual).contains(nuevo)"}
    D -->|No| E["❌ InvalidOperationException<br/>'Transición no permitida:<br/>ACTUAL → NUEVO'"]
    D -->|Sí| F["✅ Ejecutar cambio de estado"]
```

### 4.3 Cálculo de Totales

```
diasAlquiler = fechaFin - fechaInicio (en días)

Por cada item:
  item.subtotal = item.precioPorDia × item.cantidad × diasAlquiler

reservation.subtotal = Σ(item.subtotal)
reservation.total = subtotal - descuentoCupón (si aplica)
```

---

## 5. Notificaciones por Estado

| Transición | Título | Tipo | Mensaje |
|:---|:---|:---|:---|
| → `PENDIENTE` | Cotización Recibida | INFO | "Tu cotización ha sido recibida" |
| → `CONFIRMADA` | Reserva Confirmada | SUCCESS | "Tu reserva ha sido aprobada" |
| → `ACTIVA` | Pago Confirmado | SUCCESS | "Tu pago ha sido confirmado" |
| → `EN_CURSO` | En Curso | INFO | "Tu mobiliario ha sido entregado" |
| → `COMPLETADA` | Completada | SUCCESS | "Tu reserva ha finalizado exitosamente" |
| → `CANCELADA` | Cancelada | ALERT | "Tu reserva ha sido cancelada" |

---

## 6. Métricas y Eventos

### 6.1 Métricas Micrometer

| Evento | Métrica | Tipo |
|:---|:---|:---|
| Reserva creada | `furent.reservations.created` | Counter |
| Reserva cancelada | `furent.reservations.cancelled` | Counter |
| Tiempo creación | `furent.reservation.processing.time` | Timer |

### 6.2 Eventos de Dominio (Spring Events)

```mermaid
graph LR
    RS["ReservationService"] -->|save()| RCE["ReservationCreatedEvent<br/>{reservationId, userId, total}"]
    RS -->|→ CANCELADA| RCAE["ReservationCancelledEvent<br/>{reservationId, reason}"]
    
    RCE --> FL["FurentEventListener"]
    RCAE --> FL
    
    FL --> T1["📊 ReportingService.trackEvent()"]
    FL --> T2["📧 EmailService.sendConfirmation()"]
```

---

## 7. Historial de Estados

Cada cambio de estado genera un registro en `StatusHistory`:

```mermaid
erDiagram
    RESERVATION ||--o{ STATUS_HISTORY : tiene
    
    STATUS_HISTORY {
        string id PK
        string tenantId
        string reservaId FK
        string estadoAnterior
        string estadoNuevo
        string usuarioAccion
        string nota
        datetime fecha
    }
```

**Ejemplo de historial:**

```
┌─────┬──────────────┬──────────────┬──────────┬──────────────────┐
│  #  │ Anterior     │ Nuevo        │ Usuario  │ Nota             │
├─────┼──────────────┼──────────────┼──────────┼──────────────────┤
│  1  │ -            │ PENDIENTE    │ cliente  │ Cotización nueva │
│  2  │ PENDIENTE    │ CONFIRMADA   │ admin    │ Aprobada         │
│  3  │ CONFIRMADA   │ ACTIVA       │ sistema  │ Pago confirmado  │
│  4  │ ACTIVA       │ EN_CURSO     │ admin    │ Entregado 10am   │
│  5  │ EN_CURSO     │ COMPLETADA   │ admin    │ Recogido 6pm     │
└─────┴──────────────┴──────────────┴──────────┴──────────────────┘
```

---

## 8. Flujo de Cancelación

```mermaid
flowchart TD
    A["Solicitud de cancelación"] --> B{"¿Estado actual?"}
    
    B -->|PENDIENTE| C["✅ Cancelar directamente"]
    B -->|CONFIRMADA| D["✅ Cancelar<br/>(sin pago realizado)"]
    B -->|ACTIVA| E["⚠️ Cancelar<br/>(puede tener pago)"]
    B -->|EN_CURSO| F["⚠️ Cancelar<br/>(mobiliario entregado)"]
    B -->|COMPLETADA| G["❌ No se puede cancelar<br/>(estado final)"]
    B -->|CANCELADA| H["❌ Ya está cancelada<br/>(estado final)"]
    
    C & D & E & F --> I["updateStatus(id, 'CANCELADA', usuario, 'motivo')"]
    I --> J["Guardar StatusHistory"]
    J --> K["Publicar ReservationCancelledEvent"]
    K --> L["metrics: reservationsCancelled++"]
    L --> M["Notificar usuario"]
    M --> N([❌ CANCELADA])

    G --> O["InvalidOperationException"]
    H --> P["InvalidOperationException"]

    style N fill:#ff6b6b,color:#fff
    style O fill:#ff6b6b,color:#fff
    style P fill:#ff6b6b,color:#fff
```

---

## 9. Integración con Pagos

### 9.1 Vínculo Reserva ↔ Pago

```mermaid
graph LR
    subgraph "Ciclo de Vida"
        R1["CONFIRMADA"] -->|initPayment| P1["Payment PENDIENTE"]
        P1 -->|confirmPayment| R2["ACTIVA"]
        P1 -->|failPayment| P2["Payment FALLIDO"]
        P2 -->|retry| P1
    end
```

### 9.2 Efecto del Pago en la Reserva

| Acción Pago | Efecto en Reserva |
|:---|:---|
| `initPayment()` | Crea Payment PENDIENTE. Reserva sin cambio |
| `confirmPayment()` | Payment → PAGADO. **Reserva → ACTIVA** |
| `failPayment()` | Payment → FALLIDO. **Reserva sin cambio** (sigue CONFIRMADA) |

---

## 10. Logística (Entrega y Recogida)

### 10.1 Vista del Admin

```mermaid
flowchart LR
    subgraph "Dashboard Logística (/admin/logistica)"
        A["Reservas ACTIVAS<br/>(próximas a fecha inicio)"]
        B["Reservas EN_CURSO<br/>(próximas a fecha fin)"]
    end
    
    subgraph "Acciones"
        C["📋 Generar Hoja de Ruta<br/>(PDF con entregas del día)"]
        D["✅ Marcar como EN_CURSO<br/>(entrega realizada)"]
        E["✅ Marcar como COMPLETADA<br/>(recogida exitosa)"]
    end
    
    A --> C & D
    B --> C & E
```

### 10.2 Hoja de Ruta (PDF)

El admin puede descargar un PDF con las entregas/recogidas programadas para el día:

- **GET `/admin/logistica/hoja-ruta`** → PDF generado con OpenHTMLtoPDF
- Incluye: dirección, productos, cantidades, datos del cliente
- Filtra reservas ACTIVAS con `fechaInicio = hoy` y EN_CURSO con `fechaFin = hoy`

---

## Resumen Visual del Ciclo de Vida

```mermaid
graph LR
    A["📝 Cotización<br/>(Usuario)"] -->|POST /cotizacion| B["📋 PENDIENTE"]
    B -->|Admin aprueba| C["✅ CONFIRMADA"]
    C -->|Pago confirmado| D["🟢 ACTIVA"]
    D -->|Entrega| E["🚛 EN_CURSO"]
    E -->|Recogida| F["🏁 COMPLETADA"]
    
    B -.->|Cancelar| G["❌ CANCELADA"]
    C -.->|Cancelar| G
    D -.->|Cancelar| G
    E -.->|Cancelar| G

    style A fill:#e3fafc
    style B fill:#ffd43b,color:#000
    style C fill:#69db7c,color:#000
    style D fill:#339af0,color:#fff
    style E fill:#ff922b,color:#fff
    style F fill:#51cf66,color:#fff
    style G fill:#ff6b6b,color:#fff
```

---

> 📝 **Generado automáticamente** — Furent SaaS Platform v1.0  
> Última actualización: Junio 2025
