# FURENT --- Arquitectura Profesional 1000/10

## Plataforma SaaS Escalable (MongoDB Edition)

Autor del diseño: **Principal Software Architect AI**\
Objetivo: Transformar **FURENT** desde un proyecto académico a una
**plataforma SaaS empresarial escalable**.

Este documento describe:

-   Arquitectura ideal
-   Microservicios completos
-   Diseño SaaS
-   Estrategia de migración
-   Stack tecnológico
-   Diagramas
-   Estrategia de escalabilidad

⚠️ Restricción técnica del proyecto:

**Toda la persistencia debe utilizar MongoDB.**

No se utilizarán otras bases de datos.

------------------------------------------------------------------------

# 1. Visión del Proyecto

**FURENT** es una plataforma para gestión de **alquiler de mobiliario
para eventos**.

Ejemplos de clientes:

-   Empresas de eventos
-   Organizadores de bodas
-   Empresas de alquiler de mobiliario
-   Empresas de alquiler de equipos
-   Empresas logísticas

La evolución del sistema será hacia:

**Plataforma SaaS multiempresa**.

Cada empresa tendrá:

-   usuarios
-   inventario
-   reservas
-   pagos
-   reportes

Todo aislado por **tenant**.

------------------------------------------------------------------------

# 2. Arquitectura General

La arquitectura final será:

-   Microservices Architecture
-   Event Driven Architecture
-   API Gateway Pattern
-   MongoDB per Service
-   SaaS Multi‑Tenant
-   Cloud Native

------------------------------------------------------------------------

## Diagrama de Arquitectura

``` mermaid
flowchart LR

Client[Web / Mobile]

Gateway[API Gateway]

Auth[Identity Service]
User[User Service]
Product[Product Service]
Reservation[Reservation Service]
Payment[Payment Service]
Notification[Notification Service]
Review[Review Service]
Audit[Audit Service]
Analytics[Reporting Service]

MongoAuth[(MongoDB Auth)]
MongoUser[(MongoDB User)]
MongoProduct[(MongoDB Product)]
MongoReservation[(MongoDB Reservation)]
MongoPayment[(MongoDB Payment)]
MongoNotification[(MongoDB Notification)]
MongoReview[(MongoDB Review)]
MongoAudit[(MongoDB Audit)]
MongoAnalytics[(MongoDB Analytics)]

Client --> Gateway

Gateway --> Auth
Gateway --> User
Gateway --> Product
Gateway --> Reservation
Gateway --> Payment
Gateway --> Review

Auth --> MongoAuth
User --> MongoUser
Product --> MongoProduct
Reservation --> MongoReservation
Payment --> MongoPayment
Notification --> MongoNotification
Review --> MongoReview
Audit --> MongoAudit
Analytics --> MongoAnalytics
```

------------------------------------------------------------------------

# 3. Microservicios del Sistema

## Identity Service

Responsable de:

-   autenticación
-   registro
-   JWT
-   refresh tokens
-   control de roles

Colecciones Mongo:

    users
    roles
    permissions
    sessions
    password_resets

------------------------------------------------------------------------

## User Service

Responsable de:

-   perfil del usuario
-   configuración
-   preferencias
-   historial

Colecciones:

    user_profiles
    user_addresses
    user_preferences

------------------------------------------------------------------------

## Product Service

Responsable de:

-   catálogo
-   inventario
-   categorías
-   disponibilidad

Colecciones:

    products
    categories
    inventory
    product_images

------------------------------------------------------------------------

## Reservation Service

Responsable de:

-   reservas
-   calendario
-   estados

Colecciones:

    reservations
    reservation_items
    reservation_status_history

------------------------------------------------------------------------

## Payment Service

Responsable de:

-   pagos
-   facturas
-   transacciones

Colecciones:

    payments
    transactions
    invoices
    refunds

------------------------------------------------------------------------

## Coupon Service

Colecciones:

    coupons
    coupon_usage

------------------------------------------------------------------------

## Review Service

Colecciones:

    reviews
    ratings

------------------------------------------------------------------------

## Notification Service

Responsable de:

-   emails
-   notificaciones internas

Colecciones:

    notifications
    email_queue
    notification_logs

------------------------------------------------------------------------

## Audit Service

Responsable de:

-   registro de eventos
-   auditoría

Colecciones:

    audit_logs
    security_logs
    activity_logs

------------------------------------------------------------------------

## Reporting Service

Responsable de:

-   reportes
-   exportaciones

Colecciones:

    analytics_events
    report_cache

------------------------------------------------------------------------

# 4. Event Driven Architecture

El sistema se comunica mediante **eventos**.

Eventos principales:

    UserRegistered
    ReservationCreated
    ReservationCancelled
    PaymentCompleted
    ProductUpdated
    ReviewCreated

Ejemplo de flujo:

    ReservationCreated
    → Notification Service
    → Payment Service
    → Audit Service

Diagrama:

``` mermaid
flowchart LR

Reservation --> EventBus
EventBus --> Notification
EventBus --> Payment
EventBus --> Audit
```

------------------------------------------------------------------------

# 5. Modelo SaaS Multi‑Tenant

Cada documento incluirá:

    tenantId

Ejemplo:

``` json
{
  "tenantId": "empresa_123",
  "name": "Mesa Redonda",
  "price": 30
}
```

Esto permite:

-   múltiples empresas
-   aislamiento de datos
-   escalabilidad

------------------------------------------------------------------------

# 6. Estrategia Monolito → Microservicios

### Etapa 1

Mantener monolito actual.

Separar módulos:

-   users
-   products
-   reservations
-   payments

------------------------------------------------------------------------

### Etapa 2

Crear API Gateway.

Extraer:

Identity Service.

------------------------------------------------------------------------

### Etapa 3

Extraer:

Product Service\
Reservation Service

------------------------------------------------------------------------

### Etapa 4

Extraer:

Payments\
Notifications

------------------------------------------------------------------------

### Etapa 5

Activar Event Bus.

------------------------------------------------------------------------

### Etapa 6

Escalado horizontal.

------------------------------------------------------------------------

# 7. Stack Tecnológico Recomendado

Backend:

-   Java
-   Spring Boot
-   Spring Cloud
-   Spring Security

Arquitectura:

-   API Gateway
-   Microservices
-   Event Driven

Mensajería:

-   Kafka o RabbitMQ

Base de datos:

-   **MongoDB (única base de datos)**

Cache:

-   Redis

Infraestructura:

-   Docker
-   Kubernetes

Observabilidad:

-   Prometheus
-   Grafana
-   ELK Stack

CI/CD:

-   GitHub Actions

------------------------------------------------------------------------

# 8. Seguridad

Implementar:

-   JWT
-   OAuth2
-   Rate limiting
-   RBAC

Roles:

    ADMIN
    MANAGER
    CLIENT

------------------------------------------------------------------------

# 9. Observabilidad

Implementar:

-   Prometheus
-   Grafana
-   OpenTelemetry
-   Jaeger

------------------------------------------------------------------------

# 10. Escalabilidad

Kubernetes permitirá:

-   autoscaling
-   rolling updates
-   resiliencia

------------------------------------------------------------------------

# 11. Roadmap de Implementación

### Fase 1

Refactorizar backend actual.

------------------------------------------------------------------------

### Fase 2

Separar módulos internos.

------------------------------------------------------------------------

### Fase 3

Implementar autenticación profesional.

------------------------------------------------------------------------

### Fase 4

Crear Event Bus.

------------------------------------------------------------------------

### Fase 5

Extraer microservicios.

------------------------------------------------------------------------

### Fase 6

Implementar Kubernetes.

------------------------------------------------------------------------

### Fase 7

Convertir en SaaS multi‑tenant.

------------------------------------------------------------------------

# 12. Nivel Final del Proyecto

Cuando todo esté implementado:

FURENT será:

-   SaaS escalable
-   Arquitectura cloud‑native
-   Plataforma multiempresa
-   Backend empresarial

Nivel estimado:

**Arquitectura 10/10**
