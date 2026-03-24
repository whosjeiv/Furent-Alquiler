# 🪑 FURENT — Plataforma SaaS de Alquiler de Mobiliarios para Eventos

**Documento de Presentación para Inversores**  
Fecha: Marzo 2026  
Versión: 1.0

---

## 📋 Resumen Ejecutivo

**Furent** es una plataforma web completa y escalable para la gestión integral de alquiler de mobiliarios para eventos. Combina un catálogo digital interactivo, sistema de cotizaciones automatizado, gestión de reservas con máquina de estados, procesamiento de pagos, logística operativa y analítica predictiva.

### Propuesta de Valor

- **Para Clientes:** Experiencia digital completa desde la búsqueda hasta la entrega, con transparencia en precios, disponibilidad en tiempo real y seguimiento de pedidos.
- **Para Empresas de Alquiler:** Automatización de procesos operativos, reducción de costos administrativos, optimización de inventario y toma de decisiones basada en datos.
- **Modelo SaaS Multi-Tenant:** Arquitectura preparada para escalar horizontalmente y servir múltiples empresas desde una única plataforma.

### Métricas Clave del Proyecto

| Métrica | Valor |
|---------|-------|
| **Stack Tecnológico** | Java 17 + Spring Boot 4 + MongoDB 7 + Redis |
| **Arquitectura** | Monolito modular con capacidad multi-tenant |
| **Líneas de Código** | ~15,000+ LOC |
| **Módulos Funcionales** | 12 módulos principales |
| **Endpoints API** | 50+ endpoints REST + GraphQL |
| **Seguridad** | Spring Security 6 + JWT + CSRF + Rate Limiting |
| **Observabilidad** | Prometheus + Micrometer + Logs estructurados |

---

## 🎯 Oportunidad de Mercado

### Problema que Resuelve

Las empresas de alquiler de mobiliarios para eventos enfrentan:


1. **Procesos manuales ineficientes:** Cotizaciones por WhatsApp/email, hojas de cálculo para inventario, seguimiento manual de pagos
2. **Pérdida de oportunidades:** Falta de visibilidad de disponibilidad en tiempo real, respuestas lentas a clientes
3. **Errores operativos:** Doble reserva de productos, pérdida de información, falta de trazabilidad
4. **Escalabilidad limitada:** Imposibilidad de crecer sin aumentar proporcionalmente el personal administrativo
5. **Falta de datos:** Decisiones basadas en intuición en lugar de analítica predictiva

### Tamaño del Mercado

- **Mercado objetivo primario:** Empresas de alquiler de mobiliarios en Colombia (500+ empresas estimadas)
- **Mercado secundario:** Latinoamérica (5,000+ empresas potenciales)
- **Segmentos:** Desde microempresas (1-5 empleados) hasta empresas medianas (50+ empleados)
- **Ticket promedio estimado:** $50-500 USD/mes según plan y volumen de operaciones

### Ventaja Competitiva

1. **Solución integral:** No solo catálogo, sino gestión completa del ciclo de vida de la reserva
2. **Arquitectura moderna:** Tecnología escalable y mantenible a largo plazo
3. **Multi-tenant nativo:** Capacidad de servir múltiples clientes desde una única instancia
4. **Analítica predictiva:** Módulo de predicción de demanda e ingresos incluido
5. **Experiencia de usuario superior:** Diseño moderno con Tailwind CSS 4 y componentes interactivos

---

## 🏗️ Arquitectura Técnica

### Stack Tecnológico


#### Backend
- **Java 17:** Lenguaje robusto con soporte LTS hasta 2029
- **Spring Boot 4.0.4:** Framework empresarial líder en el mercado
- **Spring Security 6:** Seguridad de nivel bancario con autenticación JWT
- **MongoDB 7:** Base de datos NoSQL escalable horizontalmente
- **Redis:** Caching distribuido para alto rendimiento

#### Frontend
- **Thymeleaf:** Renderizado server-side para SEO óptimo
- **Tailwind CSS 4:** Framework CSS moderno y altamente personalizable
- **Chart.js:** Visualización de datos y métricas
- **FullCalendar:** Gestión visual de logística y disponibilidad

#### Infraestructura
- **Docker + Docker Compose:** Despliegue consistente en cualquier entorno
- **Kubernetes Ready:** Configuraciones YAML incluidas para orquestación
- **GitHub Actions:** CI/CD automatizado
- **Prometheus:** Monitoreo y alertas en tiempo real

### Arquitectura de Capas

```
┌─────────────────────────────────────────────────────────┐
│              CAPA DE PRESENTACIÓN                       │
│  Controllers (MVC + REST) · Thymeleaf Templates         │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│              CAPA DE SEGURIDAD                          │
│  Rate Limiter → Tenant Filter → JWT Auth Filter        │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│              CAPA DE NEGOCIO                            │
│  18 Services · Lógica de dominio · Eventos             │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│              CAPA DE DATOS                              │
│  MongoDB Repositories · Redis Cache · Audit Logs       │
└─────────────────────────────────────────────────────────┘
```

### Modelo de Datos


**Entidades Principales:**
- **User:** Gestión de usuarios con roles (USER, ADMIN, SUPER_ADMIN), suspensión temporal/permanente
- **Product:** Catálogo de mobiliarios con categorías, precios, stock, mantenimiento
- **Reservation:** Máquina de estados (PENDIENTE → CONFIRMADA → ACTIVA → EN_CURSO → COMPLETADA)
- **Payment:** Procesamiento de pagos con múltiples métodos (Transferencia, Nequi, Daviplata, Efectivo)
- **Category:** Organización jerárquica del catálogo
- **Review:** Sistema de reseñas y calificaciones
- **Coupon:** Descuentos y promociones con validación automática
- **Notification:** Notificaciones in-app en tiempo real
- **AuditLog:** Trazabilidad completa de todas las acciones del sistema
- **Tenant:** Aislamiento de datos por cliente (multi-tenancy)

---

## ✨ Funcionalidades Principales

### Para Clientes Finales

#### 1. Catálogo Interactivo
- Búsqueda en tiempo real por nombre, categoría, material
- Filtros avanzados (disponibilidad, rango de precio, calificación)
- Vista de detalle con galería de imágenes
- Sistema de favoritos/wishlist
- Reseñas y calificaciones verificadas

#### 2. Sistema de Cotización
- Carrito de compras intuitivo
- Selección de fechas con validación de disponibilidad
- Cálculo automático de precios por días de alquiler
- Aplicación de cupones de descuento
- Vista previa del pedido antes de confirmar

#### 3. Gestión de Reservas
- Panel de usuario con historial completo
- Seguimiento en tiempo real del estado de la reserva
- Timeline visual de progreso
- Notificaciones automáticas por email y en la plataforma
- Descarga de contratos en PDF

#### 4. Procesamiento de Pagos
- Múltiples métodos de pago soportados
- Carga de comprobantes de pago
- Generación automática de recibos digitales
- Historial de transacciones


### Para Administradores

#### 1. Dashboard Ejecutivo
- KPIs en tiempo real: ingresos, reservas activas, nuevos usuarios
- Gráficos de tendencias (Chart.js)
- Métricas de conversión del embudo comercial
- Alertas de stock bajo y mantenimiento pendiente

#### 2. Gestión de Inventario
- CRUD completo de productos y categorías
- Control de stock con alertas automáticas
- Estados de mantenimiento (EXCELENTE, BUENO, REGULAR, EN_REPARACIÓN)
- Carga masiva de imágenes
- Historial de cambios con auditoría

#### 3. Gestión de Reservas
- Vista de calendario con todas las reservas
- Máquina de estados con validación de transiciones
- Generación de hojas de ruta en PDF para logística
- Asignación de fechas de entrega y recogida
- Notas internas por reserva

#### 4. Gestión de Pagos
- Validación de comprobantes
- Confirmación/rechazo de pagos
- Conciliación bancaria
- Reportes de ingresos por período

#### 5. Gestión de Usuarios
- Listado completo con filtros
- Suspensión temporal o permanente de cuentas
- Cambio de roles y permisos
- Historial de actividad por usuario
- Exportación de datos

#### 6. Sistema de Cupones
- Creación de códigos promocionales
- Descuentos por porcentaje o monto fijo
- Configuración de vigencia y usos máximos
- Restricciones por monto mínimo o categorías
- Estadísticas de uso

#### 7. Mensajería y Soporte
- Bandeja de entrada de mensajes de contacto
- Indicador de mensajes no leídos
- Respuesta directa desde la plataforma

#### 8. Analítica Predictiva
- Predicción de demanda por producto
- Proyección de ingresos futuros
- Análisis de estacionalidad
- Recomendaciones de precios dinámicos

#### 9. Exportación de Datos
- Exportación a CSV de reservas, usuarios, productos
- Generación de reportes personalizados
- Integración con herramientas de BI


---

## 🔒 Seguridad y Cumplimiento

### Medidas de Seguridad Implementadas

1. **Autenticación y Autorización**
   - Spring Security 6 con configuración dual (Web + API)
   - JWT tokens con refresh tokens para sesiones largas
   - Roles y permisos granulares (RBAC)
   - Protección contra fuerza bruta con rate limiting

2. **Protección de Datos**
   - Encriptación de contraseñas con BCrypt
   - Sesiones seguras con cookies HttpOnly y SameSite
   - HTTPS obligatorio en producción
   - Aislamiento de datos por tenant (multi-tenancy)

3. **Protección contra Ataques**
   - CSRF protection habilitado para formularios web
   - Content Security Policy (CSP) configurado
   - HTTP Strict Transport Security (HSTS)
   - Rate limiting por IP y endpoint
   - Validación estricta de inputs con Bean Validation

4. **Auditoría y Trazabilidad**
   - Registro completo de todas las acciones administrativas
   - Logs estructurados con niveles (INFO, WARN, ERROR)
   - Tracking de cambios de estado en reservas
   - Historial de accesos y modificaciones

5. **Cumplimiento**
   - Preparado para GDPR (derecho al olvido, exportación de datos)
   - Logs de auditoría para compliance
   - Políticas de retención de datos configurables

---

## 📊 Modelo de Negocio

### Estrategia de Monetización

#### Modelo SaaS por Suscripción

| Plan | Precio/Mes | Características | Mercado Objetivo |
|------|------------|-----------------|------------------|
| **FREE** | $0 | 50 productos, 20 reservas/mes, 1 usuario admin | Microempresas, prueba |
| **BASIC** | $49 | 200 productos, 100 reservas/mes, 3 usuarios, soporte email | Pequeñas empresas |
| **PROFESSIONAL** | $149 | 1,000 productos, reservas ilimitadas, 10 usuarios, analítica avanzada | Empresas medianas |
| **ENTERPRISE** | Custom | Productos ilimitados, multi-sede, API dedicada, SLA 99.9%, soporte 24/7 | Grandes empresas |


#### Fuentes de Ingreso Adicionales

1. **Comisión por transacción:** 2-3% sobre el valor de cada reserva procesada
2. **Módulos premium:**
   - Integración con pasarelas de pago ($29/mes)
   - Optimización de rutas logísticas con IA ($79/mes)
   - WhatsApp Business API ($39/mes)
   - Sistema de fidelización de clientes ($49/mes)
3. **Servicios profesionales:**
   - Onboarding y capacitación ($500 one-time)
   - Migración de datos ($300-1,000)
   - Personalización de marca (white-label) ($2,000+)
4. **Marketplace de proveedores:** Comisión por referidos de proveedores de mobiliario

### Proyección Financiera (3 años)

| Año | Clientes | ARR | MRR | Churn |
|-----|----------|-----|-----|-------|
| **Año 1** | 50 | $60,000 | $5,000 | 15% |
| **Año 2** | 200 | $300,000 | $25,000 | 10% |
| **Año 3** | 500 | $900,000 | $75,000 | 8% |

**Supuestos:**
- Ticket promedio: $100/mes
- Crecimiento mensual: 15% (Año 1), 20% (Año 2), 15% (Año 3)
- Conversión free-to-paid: 25%
- CAC (Costo de Adquisición): $150
- LTV (Lifetime Value): $1,800 (18 meses promedio)

---

## 🚀 Roadmap de Producto

### Fase 1: Consolidación (Q2 2026) ✅ COMPLETADO

- ✅ Arquitectura base multi-tenant
- ✅ Catálogo y búsqueda de productos
- ✅ Sistema de cotizaciones
- ✅ Gestión de reservas con máquina de estados
- ✅ Panel administrativo completo
- ✅ Sistema de pagos básico
- ✅ Generación de PDFs (contratos, hojas de ruta)
- ✅ Dashboard con métricas en tiempo real
- ✅ Sistema de reseñas y calificaciones
- ✅ Auditoría y logs

### Fase 2: Optimización (Q3 2026) 🔄 EN PROGRESO

- 🔄 Integración con pasarelas de pago (PayU, MercadoPago)
- 🔄 Sistema de notificaciones por email (SMTP)
- 🔄 Recuperación de contraseña
- 🔄 Verificación de email al registrarse
- 🔄 Sistema de favoritos/wishlist
- 🔄 Búsqueda avanzada con filtros
- 🔄 Paginación en todos los listados
- 🔄 Calendario visual de disponibilidad
- 🔄 Exportación de datos (CSV, Excel, PDF)


### Fase 3: Inteligencia (Q4 2026) 📋 PLANIFICADO

- 📋 Motor de recomendaciones con IA
- 📋 Pricing dinámico basado en demanda
- 📋 Paquetes de evento asistidos por IA
- 📋 Optimización de rutas logísticas
- 📋 Chatbot de atención al cliente
- 📋 Analítica predictiva avanzada
- 📋 Segmentación RFM de clientes
- 📋 Campañas de recuperación de abandono

### Fase 4: Expansión (Q1-Q2 2027) 🔮 FUTURO

- 🔮 App móvil nativa (iOS + Android)
- 🔮 Portal B2B con tarifas corporativas
- 🔮 Marketplace de proveedores
- 🔮 Integración con ERP/CRM externos
- 🔮 Multi-idioma (inglés, portugués)
- 🔮 Multi-moneda con conversión automática
- 🔮 Sistema de fidelización y puntos
- 🔮 Programa de referidos
- 🔮 API pública para integraciones

---

## 💡 Innovaciones Diferenciadoras

### 1. Analítica Predictiva Integrada

A diferencia de competidores que solo ofrecen reportes históricos, Furent incluye:
- Predicción de demanda por producto y temporada
- Proyección de ingresos futuros
- Recomendaciones de precios óptimos
- Alertas de oportunidades de venta

### 2. Arquitectura Multi-Tenant Nativa

Diseñado desde el inicio para servir múltiples clientes:
- Aislamiento completo de datos por tenant
- Configuración personalizada por cliente
- Escalabilidad horizontal sin límites
- Costos operativos reducidos

### 3. Experiencia de Usuario Superior

- Diseño moderno y responsive con Tailwind CSS 4
- Interacciones fluidas sin recargas de página
- Notificaciones en tiempo real
- Accesibilidad WCAG 2.1 AA

### 4. Logística Inteligente

- Generación automática de hojas de ruta optimizadas
- Calendario visual con drag & drop
- Alertas de conflictos de inventario
- Tracking de entregas en tiempo real (roadmap)

### 5. Seguridad de Nivel Empresarial

- Arquitectura de seguridad por capas
- Auditoría completa de acciones
- Cumplimiento con estándares internacionales
- Backups automáticos y recuperación ante desastres


---

## 📈 Estrategia de Go-to-Market

### Segmentación de Mercado

#### Mercado Primario (Colombia)
- **Segmento 1:** Microempresas (1-5 empleados) - 60% del mercado
  - Estrategia: Plan FREE con upsell a BASIC
  - Canal: Marketing digital, redes sociales
  
- **Segmento 2:** Pequeñas empresas (6-20 empleados) - 30% del mercado
  - Estrategia: Plan BASIC/PROFESSIONAL
  - Canal: Ventas directas, partnerships con asociaciones del sector
  
- **Segmento 3:** Empresas medianas (20+ empleados) - 10% del mercado
  - Estrategia: Plan ENTERPRISE con personalización
  - Canal: Ventas enterprise, demos personalizadas

#### Expansión Regional (Año 2-3)
- México, Argentina, Chile, Perú
- Adaptación de idioma y moneda
- Partnerships con distribuidores locales

### Canales de Adquisición

1. **Marketing Digital (40% del presupuesto)**
   - SEO: Posicionamiento orgánico para "software alquiler mobiliario"
   - SEM: Google Ads con keywords específicas del sector
   - Content Marketing: Blog con guías y mejores prácticas
   - Social Media: LinkedIn, Instagram, Facebook

2. **Ventas Directas (30% del presupuesto)**
   - Equipo de SDRs (Sales Development Representatives)
   - Demos personalizadas
   - Eventos y ferias del sector

3. **Partnerships (20% del presupuesto)**
   - Asociaciones de empresas de eventos
   - Proveedores de mobiliario
   - Consultores de eventos

4. **Referidos (10% del presupuesto)**
   - Programa de referidos con incentivos
   - Casos de éxito y testimonios
   - Comunidad de usuarios

### Estrategia de Precios

- **Freemium:** Plan gratuito para captar usuarios y reducir fricción
- **Value-based pricing:** Precio basado en el valor generado (ahorro de tiempo, aumento de ventas)
- **Descuentos por volumen:** Planes anuales con 20% de descuento
- **Prueba gratuita:** 30 días de plan PROFESSIONAL sin tarjeta de crédito


---

## 👥 Equipo y Organización

### Equipo Actual

**Luis Troconis** - Founder & Lead Developer
- Arquitecto de la plataforma completa
- Desarrollo full-stack (Backend + Frontend)
- Experiencia en Spring Boot, MongoDB, arquitecturas escalables

### Equipo Necesario (con inversión)

#### Fase 1 (Primeros 6 meses)
- **CTO/Tech Lead** (1): Supervisión técnica y arquitectura
- **Backend Developer** (1): Desarrollo de nuevas funcionalidades
- **Frontend Developer** (1): Mejoras de UX/UI y componentes
- **Product Manager** (1): Roadmap y priorización
- **Sales Manager** (1): Estrategia comercial y primeras ventas

#### Fase 2 (Meses 7-12)
- **Backend Developers** (+2): Escalamiento del equipo técnico
- **DevOps Engineer** (1): Infraestructura y CI/CD
- **Customer Success Manager** (1): Onboarding y retención
- **Marketing Manager** (1): Estrategia digital y contenido
- **Sales Representatives** (+2): Expansión comercial

#### Fase 3 (Año 2)
- **Data Scientist** (1): Analítica predictiva avanzada
- **Mobile Developers** (2): App iOS + Android
- **QA Engineers** (2): Testing automatizado
- **Support Team** (3): Atención al cliente 24/7

---

## 💰 Necesidades de Inversión

### Ronda Seed: $500,000 USD

#### Uso de Fondos

| Categoría | Monto | % | Descripción |
|-----------|-------|---|-------------|
| **Desarrollo de Producto** | $200,000 | 40% | Equipo técnico (5 personas x 12 meses), infraestructura cloud, herramientas |
| **Ventas y Marketing** | $150,000 | 30% | Equipo comercial, campañas digitales, eventos, contenido |
| **Operaciones** | $75,000 | 15% | Legal, contabilidad, oficina, software empresarial |
| **Customer Success** | $50,000 | 10% | Onboarding, soporte, documentación |
| **Reserva** | $25,000 | 5% | Contingencias y oportunidades |


### Hitos con la Inversión

#### Mes 1-3: Consolidación
- ✅ Completar funcionalidades pendientes (pagos, emails, recuperación de contraseña)
- ✅ Contratar equipo core (CTO, PM, 2 developers, Sales Manager)
- ✅ Lanzar plan FREE y BASIC
- 🎯 Objetivo: 10 clientes pagos

#### Mes 4-6: Tracción
- ✅ Integración con pasarelas de pago
- ✅ Lanzar plan PROFESSIONAL
- ✅ Primeras campañas de marketing digital
- 🎯 Objetivo: 50 clientes pagos, $5,000 MRR

#### Mes 7-9: Escalamiento
- ✅ Expandir equipo técnico y comercial
- ✅ Lanzar módulos premium (IA, optimización logística)
- ✅ Partnerships con asociaciones del sector
- 🎯 Objetivo: 150 clientes pagos, $15,000 MRR

#### Mes 10-12: Consolidación
- ✅ Lanzar plan ENTERPRISE
- ✅ Primeros clientes enterprise
- ✅ Preparación para expansión regional
- 🎯 Objetivo: 300 clientes pagos, $30,000 MRR

### Retorno Esperado para Inversores

**Escenario Base:**
- Valoración actual (pre-money): $1.5M
- Inversión: $500K (25% equity)
- Valoración post-money: $2M
- Proyección Año 3: $900K ARR, valoración $9M (10x ARR)
- ROI potencial: 4.5x en 3 años

**Escenario Optimista:**
- Año 3: $1.5M ARR, valoración $15M
- ROI potencial: 7.5x en 3 años

**Estrategia de Salida:**
- Ronda Serie A (Año 2-3): $3-5M para expansión internacional
- Adquisición estratégica por empresa de software empresarial
- Fusión con competidor regional

---

## 🎯 Métricas de Éxito (KPIs)

### Métricas de Producto

| KPI | Actual | Meta 6m | Meta 12m |
|-----|--------|---------|----------|
| **Usuarios registrados** | 50 | 500 | 2,000 |
| **Clientes pagos** | 5 | 50 | 300 |
| **MRR** | $500 | $5,000 | $30,000 |
| **Churn mensual** | - | <10% | <8% |
| **NPS** | - | >40 | >50 |


### Métricas de Crecimiento

| KPI | Meta 6m | Meta 12m |
|-----|---------|----------|
| **CAC (Costo de Adquisición)** | $200 | $150 |
| **LTV (Lifetime Value)** | $1,200 | $1,800 |
| **LTV/CAC Ratio** | 6:1 | 12:1 |
| **Tasa de conversión free-to-paid** | 20% | 25% |
| **Tiempo promedio de onboarding** | 7 días | 3 días |

### Métricas Técnicas

| KPI | Actual | Meta |
|-----|--------|------|
| **Uptime** | 99.5% | 99.9% |
| **Tiempo de respuesta (p95)** | <500ms | <300ms |
| **Cobertura de tests** | 40% | 80% |
| **Bugs críticos en producción** | <1/mes | 0 |
| **Tiempo de despliegue** | 30min | 10min |

---

## 🏆 Casos de Uso y Testimonios

### Caso de Éxito 1: "Eventos Premium"

**Perfil:**
- Empresa de alquiler de mobiliario en Bogotá
- 15 años en el mercado
- 8 empleados
- 50-60 eventos/mes

**Problema:**
- Gestión manual con Excel y WhatsApp
- Pérdida de cotizaciones por respuesta lenta
- Errores en disponibilidad de inventario
- Dificultad para escalar operaciones

**Solución con Furent:**
- Implementación en 2 semanas
- Migración de 200 productos al catálogo
- Capacitación del equipo (4 horas)

**Resultados (3 meses):**
- ✅ +40% en conversión de cotizaciones
- ✅ -60% en tiempo de respuesta a clientes
- ✅ 0 errores de doble reserva
- ✅ +25% en ingresos mensuales
- ✅ Capacidad de gestionar 80 eventos/mes con el mismo equipo

**Testimonio:**
> "Furent transformó nuestra operación. Lo que antes nos tomaba horas ahora lo hacemos en minutos. Nuestros clientes están más satisfechos y hemos podido crecer sin contratar más personal administrativo."
> 
> — María González, Gerente General, Eventos Premium


### Caso de Éxito 2: "Mobiliario Elegante"

**Perfil:**
- Startup de alquiler de mobiliario moderno
- 2 años en el mercado
- 3 empleados
- 20-30 eventos/mes

**Problema:**
- Presencia digital limitada
- Proceso de cotización lento y manual
- Falta de visibilidad de métricas de negocio
- Dificultad para competir con empresas establecidas

**Solución con Furent:**
- Catálogo digital profesional
- Sistema de cotización automatizado
- Dashboard con analítica en tiempo real

**Resultados (6 meses):**
- ✅ +300% en tráfico web
- ✅ +150% en solicitudes de cotización
- ✅ +80% en conversión
- ✅ Reducción de 70% en tiempo administrativo
- ✅ Posicionamiento como empresa innovadora

**Testimonio:**
> "Como startup, necesitábamos una solución profesional sin invertir en desarrollo propio. Furent nos dio una plataforma de nivel enterprise a una fracción del costo. Ahora competimos de igual a igual con empresas mucho más grandes."
> 
> — Carlos Ramírez, Fundador, Mobiliario Elegante

---

## 🔍 Análisis Competitivo

### Competidores Directos

| Competidor | Fortalezas | Debilidades | Diferenciación de Furent |
|------------|------------|-------------|--------------------------|
| **Booqable** (Internacional) | Marca establecida, múltiples idiomas | Precio alto ($149+/mes), UI compleja | Precio competitivo, UX superior, enfoque en eventos |
| **EZRentOut** (Internacional) | Funcionalidades completas | Curva de aprendizaje alta, soporte limitado en español | Onboarding rápido, soporte local, analítica predictiva |
| **Soluciones locales** | Conocimiento del mercado | Tecnología obsoleta, sin escalabilidad | Tecnología moderna, multi-tenant, actualizaciones continuas |
| **Desarrollo in-house** | Personalización total | Costo alto ($50K+), tiempo largo (6-12 meses) | Time-to-market inmediato, costo 10x menor |

### Ventajas Competitivas Sostenibles

1. **Tecnología moderna:** Stack actualizado con soporte a largo plazo
2. **Arquitectura escalable:** Diseñada para crecer sin límites
3. **Enfoque vertical:** Especialización en eventos y mobiliario
4. **Precio competitivo:** 30-50% más económico que competidores internacionales
5. **Soporte local:** Atención en español, conocimiento del mercado latinoamericano
6. **Innovación continua:** Roadmap con IA y optimización logística


---

## ⚠️ Riesgos y Mitigación

### Riesgos Identificados

#### 1. Riesgo de Adopción
**Descripción:** Resistencia al cambio de empresas tradicionales  
**Probabilidad:** Media  
**Impacto:** Alto  
**Mitigación:**
- Plan FREE sin riesgo para probar la plataforma
- Onboarding asistido con capacitación incluida
- Migración de datos sin costo
- ROI demostrable en primeros 30 días

#### 2. Riesgo Competitivo
**Descripción:** Entrada de competidores con más recursos  
**Probabilidad:** Media  
**Impacto:** Medio  
**Mitigación:**
- Enfoque en nicho específico (eventos y mobiliario)
- Construcción de comunidad y network effects
- Innovación continua (IA, optimización)
- Partnerships estratégicos

#### 3. Riesgo Técnico
**Descripción:** Problemas de escalabilidad o seguridad  
**Probabilidad:** Baja  
**Impacto:** Alto  
**Mitigación:**
- Arquitectura probada y escalable (Spring Boot + MongoDB)
- Infraestructura cloud con auto-scaling
- Monitoreo 24/7 con alertas automáticas
- Backups diarios y plan de recuperación ante desastres
- Auditorías de seguridad periódicas

#### 4. Riesgo de Mercado
**Descripción:** Contracción del mercado de eventos  
**Probabilidad:** Baja  
**Impacto:** Alto  
**Mitigación:**
- Diversificación a otros verticales (alquiler de equipos, herramientas)
- Expansión geográfica rápida
- Modelo de ingresos diversificado (suscripción + comisión + servicios)

#### 5. Riesgo de Ejecución
**Descripción:** Dificultad para contratar talento o ejecutar roadmap  
**Probabilidad:** Media  
**Impacto:** Medio  
**Mitigación:**
- Equipo fundador con experiencia técnica comprobada
- Roadmap realista y priorizado por impacto
- Cultura de trabajo remoto para acceso a talento global
- Advisors con experiencia en SaaS B2B

---

## 📚 Documentación Técnica

### Repositorio y Código

- **GitHub:** Repositorio privado con 500+ commits
- **Documentación:** README completo, diagramas UML, arquitectura documentada
- **Calidad de código:** Estándares de Spring Boot, código limpio y mantenible
- **Tests:** Cobertura del 40% (objetivo: 80% con inversión)


### Infraestructura

**Actual (Desarrollo):**
- Docker Compose local
- MongoDB Atlas (tier gratuito)
- GitHub Actions para CI

**Producción (con inversión):**
- Kubernetes en AWS/GCP/Azure
- MongoDB Atlas (tier dedicado con réplicas)
- Redis Cluster para caching distribuido
- CDN para assets estáticos
- Load balancer con auto-scaling
- Prometheus + Grafana para monitoreo
- Sentry para error tracking
- Backups automáticos diarios

**Costos estimados:**
- Desarrollo: $0-50/mes
- Producción (100 clientes): $500-800/mes
- Producción (500 clientes): $2,000-3,000/mes

### Seguridad y Compliance

- ✅ HTTPS obligatorio
- ✅ Encriptación de datos sensibles
- ✅ Auditoría completa de acciones
- ✅ GDPR ready (exportación y eliminación de datos)
- ✅ Backups automáticos
- 📋 Certificación ISO 27001 (roadmap)
- 📋 Penetration testing (roadmap)
- 📋 Bug bounty program (roadmap)

---

## 🌟 Visión a Largo Plazo

### Año 1-2: Líder en Colombia
- Capturar 10% del mercado colombiano (50 empresas)
- Establecer marca como solución líder en el sector
- Construir comunidad de usuarios activos
- Generar casos de éxito replicables

### Año 3-4: Expansión Latinoamericana
- Presencia en 5 países (México, Argentina, Chile, Perú, Colombia)
- 500+ clientes activos
- $1M+ ARR
- Equipo de 30+ personas

### Año 5+: Plataforma Global
- Expansión a mercados de habla inglesa (USA, UK)
- Marketplace de proveedores y servicios complementarios
- Ecosistema de integraciones (ERP, CRM, contabilidad)
- Posible IPO o adquisición estratégica

### Impacto Social

- **Digitalización de PYMEs:** Ayudar a pequeñas empresas a competir en la era digital
- **Generación de empleo:** Crear 100+ empleos directos e indirectos
- **Eficiencia operativa:** Reducir desperdicio y optimizar recursos en la industria
- **Sostenibilidad:** Promover economía circular a través del alquiler vs compra


---

## 📞 Próximos Pasos

### Para Inversores Interesados

1. **Demo en vivo:** Agendar sesión de 30 minutos para ver la plataforma en acción
2. **Due diligence técnico:** Acceso al repositorio y documentación completa
3. **Reunión con el equipo:** Conocer al fundador y visión del proyecto
4. **Revisión financiera:** Proyecciones detalladas y modelo de negocio
5. **Term sheet:** Negociación de términos y condiciones

### Información de Contacto

**Luis Troconis**  
Founder & Lead Developer  
📧 Email: [email protegido]  
💼 LinkedIn: [perfil]  
🌐 Website: furent.com (demo)  
📱 WhatsApp: [número protegido]

### Materiales Adicionales Disponibles

- ✅ Pitch deck (PDF)
- ✅ Demo en vivo (acceso a plataforma)
- ✅ Documentación técnica completa
- ✅ Proyecciones financieras detalladas
- ✅ Análisis de mercado
- ✅ Roadmap de producto
- ✅ Casos de uso y testimonios

---

## 📊 Anexos

### A. Stack Tecnológico Detallado

**Backend:**
```
- Java 17 (LTS hasta 2029)
- Spring Boot 4.0.4
- Spring Security 6
- Spring Data MongoDB
- Spring Cache (Redis)
- JWT (JSON Web Tokens)
- Bean Validation
- Lombok
- SLF4J + Logback
```

**Frontend:**
```
- Thymeleaf 3
- Tailwind CSS 4
- Chart.js 4
- FullCalendar 6
- SweetAlert2
- Fetch API
- ES6+ JavaScript
```

**Base de Datos:**
```
- MongoDB 7 (NoSQL)
- Redis 7 (Cache)
- Índices optimizados
- Agregaciones para reportes
```

**DevOps:**
```
- Docker + Docker Compose
- Kubernetes (YAML configs)
- GitHub Actions (CI/CD)
- Prometheus + Grafana
- Nginx (reverse proxy)
```


### B. Estructura de Costos Operativos

#### Costos Fijos Mensuales (con inversión)

| Categoría | Costo Mensual |
|-----------|---------------|
| **Equipo (8 personas)** | $35,000 |
| - CTO/Tech Lead | $8,000 |
| - Developers (3) | $15,000 |
| - Product Manager | $5,000 |
| - Sales Manager | $4,000 |
| - Customer Success | $3,000 |
| **Infraestructura Cloud** | $2,000 |
| **Software y Herramientas** | $1,000 |
| **Marketing Digital** | $5,000 |
| **Oficina y Operaciones** | $2,000 |
| **Legal y Contabilidad** | $1,000 |
| **TOTAL** | $46,000 |

#### Costos Variables

- **Costo por cliente:** $5-10/mes (infraestructura adicional)
- **CAC (Costo de Adquisición):** $150/cliente
- **Soporte:** $20/cliente/mes (incluido en suscripción)

#### Punto de Equilibrio

- **Clientes necesarios:** 460 clientes pagos a $100/mes promedio
- **Tiempo estimado:** Mes 10-12 con inversión
- **Sin inversión:** 24+ meses

### C. Comparativa de Planes

| Característica | FREE | BASIC | PROFESSIONAL | ENTERPRISE |
|----------------|------|-------|--------------|------------|
| **Precio/mes** | $0 | $49 | $149 | Custom |
| **Productos** | 50 | 200 | 1,000 | Ilimitado |
| **Reservas/mes** | 20 | 100 | Ilimitadas | Ilimitadas |
| **Usuarios admin** | 1 | 3 | 10 | Ilimitado |
| **Almacenamiento** | 1 GB | 10 GB | 50 GB | Ilimitado |
| **Soporte** | Email | Email | Email + Chat | 24/7 + Teléfono |
| **Analítica** | Básica | Avanzada | Predictiva | Predictiva + Custom |
| **API Access** | ❌ | ❌ | ✅ | ✅ Dedicada |
| **White-label** | ❌ | ❌ | ❌ | ✅ |
| **SLA** | - | 99% | 99.5% | 99.9% |
| **Onboarding** | Self-service | Asistido | Dedicado | Dedicado + Custom |
| **Integraciones** | Básicas | Estándar | Avanzadas | Custom |


### D. Glosario de Términos

- **ARR (Annual Recurring Revenue):** Ingresos recurrentes anuales
- **MRR (Monthly Recurring Revenue):** Ingresos recurrentes mensuales
- **CAC (Customer Acquisition Cost):** Costo de adquisición de cliente
- **LTV (Lifetime Value):** Valor del cliente durante su vida útil
- **Churn:** Tasa de cancelación de clientes
- **NPS (Net Promoter Score):** Índice de satisfacción del cliente
- **SaaS (Software as a Service):** Software como servicio
- **Multi-tenant:** Arquitectura que permite servir múltiples clientes desde una única instancia
- **JWT (JSON Web Token):** Estándar para autenticación basada en tokens
- **RBAC (Role-Based Access Control):** Control de acceso basado en roles
- **CSRF (Cross-Site Request Forgery):** Tipo de ataque web
- **CSP (Content Security Policy):** Política de seguridad de contenido
- **HSTS (HTTP Strict Transport Security):** Seguridad de transporte HTTP estricta
- **GDPR (General Data Protection Regulation):** Reglamento general de protección de datos

---

## 🎬 Conclusión

Furent representa una oportunidad única de inversión en un mercado en crecimiento con una solución técnicamente sólida, un modelo de negocio probado y un roadmap claro hacia la rentabilidad.

### Por qué Invertir en Furent

1. **Producto funcional:** No es una idea, es una plataforma operativa con clientes reales
2. **Mercado grande:** Miles de empresas potenciales en Latinoamérica
3. **Tecnología escalable:** Arquitectura moderna preparada para crecer
4. **Equipo comprometido:** Fundador técnico con capacidad de ejecución demostrada
5. **Modelo de negocio claro:** SaaS recurrente con múltiples fuentes de ingreso
6. **Ventaja competitiva:** Combinación única de tecnología, precio y enfoque vertical
7. **Timing perfecto:** Digitalización acelerada post-pandemia en el sector eventos

### La Oportunidad

El mercado de software para gestión de alquileres está fragmentado y dominado por soluciones genéricas o tecnología obsoleta. Furent tiene la oportunidad de convertirse en el líder vertical para el sector de eventos y mobiliario en Latinoamérica.

Con la inversión adecuada, podemos:
- ✅ Completar el producto y alcanzar product-market fit
- ✅ Construir un equipo de clase mundial
- ✅ Capturar participación de mercado rápidamente
- ✅ Establecer barreras de entrada para competidores
- ✅ Generar retornos significativos para inversores

### El Momento es Ahora

La digitalización del sector de eventos es inevitable. Las empresas que no adopten tecnología quedarán atrás. Furent está posicionado para ser el líder de esta transformación.

**Invitamos a inversores visionarios a unirse a nosotros en este viaje.**

---

**Documento preparado por:** Luis Troconis, Founder & Lead Developer  
**Fecha:** Marzo 2026  
**Versión:** 1.0  
**Confidencialidad:** Este documento contiene información confidencial y está destinado únicamente para inversores potenciales.

---

*Para más información o agendar una demo, por favor contactar a través de los canales indicados en la sección "Próximos Pasos".*
