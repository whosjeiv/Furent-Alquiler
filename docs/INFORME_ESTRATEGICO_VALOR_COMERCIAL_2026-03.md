# Informe estratégico de valor comercial — Furent

Fecha: 2026-03-18  
Alcance: Producto, UX, seguridad, escalabilidad, diferenciación y roadmap de ejecución.

## 1) Resumen ejecutivo

Furent ya tiene una base sólida para escalar comercialmente: catálogo, cotización, reservas, pagos, logística, dashboard admin, analítica predictiva inicial, métricas con Prometheus y arquitectura multi-tenant.  

El principal cuello de botella para aumentar valor comercial no es “falta de features”, sino:

- Baja madurez de analítica de comportamiento del usuario (funnel y conversión incompletos).
- Riesgos de seguridad por configuración sensible expuesta en entornos no productivos.
- Oportunidades claras de monetización B2B aún no empaquetadas (planes, SLA, pricing dinámico, automation de logística).

Conclusión: con una hoja de ruta de 90 días, Furent puede pasar de plataforma operativa “completa” a plataforma “comercialmente optimizada” con impacto directo en conversión, ticket promedio y retención.

## 2) Diagnóstico del estado actual

### Fortalezas

- Arquitectura modular monolítica con camino a SaaS y multi-tenant ya modelado.
- Seguridad base robusta: dual filter chain, CSRF web, CSP/HSTS, rate limiting y auditoría.
- Caching Redis con TTL por dominio y observabilidad con métricas de negocio.
- Módulos comerciales relevantes ya construidos: cupones, reseñas, notificaciones, exportación, predicción.

### Gaps críticos

- Instrumentación de analytics de producto infrautilizada (el servicio existe, la captura de eventos no está generalizada).
- Procesos analíticos en memoria con `findAll()` para reportes/predicciones, riesgo de degradación al crecer datos.
- Configuración con secretos y defaults inseguros en archivos base.
- Accesibilidad y UX sin programa formal de medición continua (Lighthouse/WCAG sistemático, embudo y cohortes).

## 3) Evaluación UX basada en señales actuales

### Señales disponibles hoy

- Feedback explícito:
  - Reseñas por producto con rating/comentario.
  - Bandeja de mensajes de contacto para soporte comercial.
- Señales operativas:
  - Dashboard admin con ingresos, reservas, usuarios y estados.
  - Predicciones simples de demanda/ingresos/reservas.
- Señales de plataforma:
  - Métricas de negocio en Micrometer (reservas, pagos, usuarios, reseñas, revenue).

### Hallazgos UX/Producto

- Existe capacidad de medir “qué pasó” (eventos de negocio), pero no está completo el “por qué pasó” (funnel UX: búsqueda → vista detalle → selección fechas → pago).
- UX visual es fuerte en admin y catálogo, pero la accesibilidad no evidencia una cobertura homogénea WCAG por flujo completo.
- Falta un sistema de feedback estructurado post-servicio (NPS/CSAT), clave para crecimiento por referidos y retención.

### Recomendación UX inmediata

Implementar una capa de analítica de comportamiento mínima viable:

- Eventos front y back unificados para funnel comercial.
- Segmentación por tenant, canal, tipo de evento y cohorte.
- Dashboard de conversión con abandono por etapa.

## 4) Oportunidades de funcionalidades diferenciadoras (alto valor comercial)

### A) Motor inteligente de disponibilidad y pricing dinámico

- Precio sugerido por fecha, demanda histórica, estacionalidad y ocupación logística.
- Recomendación de fechas alternativas cuando un rango está saturado.
- Impacto: +ticket promedio, +tasa de cierre en semanas pico, menor fricción comercial.

### B) Paquetes de evento asistidos por IA de negocio

- “Paquetes recomendados” por tipo de evento, presupuesto e invitados.
- Up-sell automático (decoración premium, montaje extendido, extras de última milla).
- Impacto: +AOV (Average Order Value), menor tiempo de cotización.

### C) Portal B2B con SLA y reabastecimiento inteligente

- Perfiles de empresa con tarifas por volumen, historial y condiciones negociadas.
- Recompra en un clic de eventos recurrentes.
- Impacto: mayor LTV y menor CAC relativo.

### D) Logística operativa con optimización de rutas

- Enriquecer la hoja de ruta con secuenciación óptima y ventanas de entrega.
- Estado en tiempo real para equipo en campo (check-in/check-out georreferenciado).
- Impacto: reducción de costos operativos y mejora de puntualidad.

### E) Programa de confianza y reputación verificable

- Reseñas verificadas post-entrega con evidencia temporal.
- Score de confiabilidad de servicio por zona/temporada.
- Impacto: mayor conversión en nuevos clientes.

## 5) Rendimiento y escalabilidad — mejoras propuestas

## Quick wins (2–4 semanas)

- Reemplazar consultas analíticas de `findAll()` por agregaciones MongoDB (pipeline).
- Reducir payloads en vistas críticas con DTO de lectura para listados.
- Completar cache-aside por endpoint de alto tráfico y revisar invalidaciones masivas.
- Activar carga diferida de imágenes y optimizar assets críticos.

## Mediano plazo (1–2 trimestres)

- Extraer “Analytics/Reporting” como módulo aislado con jobs incrementales.
- Introducir colas para tareas no críticas (notificaciones, reportes pesados, exportaciones).
- Observabilidad avanzada: SLO por endpoint (p95, error budget), tracing distribuido.

## 6) Tecnologías emergentes recomendadas para el sector

- IA aplicada a negocio:
  - Recomendador de mobiliario por evento.
  - Asistente conversacional para cotización guiada (WhatsApp + web).
- Optimización operativa:
  - Motor de ruteo y asignación de recursos.
- Inteligencia comercial:
  - Segmentación RFM automática y campañas de recuperación de abandono.
- Gobierno de plataforma:
  - Feature flags por tenant/plan para experimentar sin riesgo.

## 7) Riesgos y prioridades de seguridad

### Riesgos prioritarios

- Eliminar secretos hardcodeados y defaults sensibles en configuración.
- Endurecer política de subida de archivos con límites y validación estricta.
- Revisar CSP para reducir superficies `unsafe-*` y dependencias de terceros.
- Endurecer cookies/sesión también en ambientes de staging.

### Controles recomendados

- Secret manager centralizado.
- Rotación y escaneo automático de secretos.
- SAST + DAST en pipeline.
- Auditoría de privilegios y pruebas de autorización por rol/tenant.

## 8) Accesibilidad y compatibilidad multiplataforma

### Accesibilidad

- Programa WCAG 2.1 AA por flujo crítico (catálogo, checkout, panel admin).
- Validación de contraste, foco visible, navegación teclado, roles ARIA, estados dinámicos.
- Pruebas con lector de pantalla para flujos de compra.

### Compatibilidad

- Matriz de soporte mínima: Chrome, Edge, Safari, Firefox + móviles iOS/Android.
- Pruebas visuales de regresión en breakpoints clave.
- Tolerancia a degradación cuando CDN externos fallen.

## 9) Roadmap priorizado (impacto comercial vs esfuerzo técnico)

### Fase 1 (0–30 días) — “Fundación de crecimiento medible”

1. Instrumentación completa del funnel de conversión.
2. Hardening de secretos/configuración y uploads.
3. Dashboard comercial con KPIs de embudo y cohortes.

Impacto esperado:

- +visibilidad de pérdidas de conversión.
- -riesgo operativo y reputacional.

### Fase 2 (31–60 días) — “Aceleración de ventas”

1. Paquetes recomendados y up-sell contextual.
2. Pricing sugerido por demanda y fechas.
3. Automatización de recuperación de abandono.

Impacto esperado:

- +conversión y +ticket promedio.

### Fase 3 (61–90 días) — “Escala operacional y B2B”

1. Optimización de rutas de logística.
2. Portal B2B con condiciones comerciales por volumen.
3. Segmentación avanzada por tenant/cohorte para retención.

Impacto esperado:

- -coste por operación.
- +retención y LTV.

## 10) KPIs por línea de iniciativa

### Embudo comercial

- Conversión catálogo → cotización.
- Conversión cotización → reserva.
- Conversión reserva → pago confirmado.
- Abandono por etapa y por dispositivo.

### Monetización

- Ticket promedio (AOV).
- Ingreso por cliente (ARPU / por tenant).
- Uplift por recomendaciones y paquetes.

### Operación logística

- Cumplimiento de entregas a tiempo.
- Costo logístico por reserva.
- Reprogramaciones por conflicto de inventario.

### Experiencia y retención

- NPS/CSAT post-servicio.
- Tasa de recompra a 30/60/90 días.
- Tiempo medio de respuesta a mensajes de contacto.

### Plataforma técnica

- p95 de endpoints críticos.
- Error rate por flujo de negocio.
- Hit ratio de caché y tiempo de generación de reportes.

## 11) Plan de validación del éxito

Para cada funcionalidad nueva:

1. Definir hipótesis comercial (ej. “paquetes aumentan AOV en 12%”).
2. Definir baseline de 4 semanas.
3. Lanzar con feature flag por segmento.
4. Medir impacto con ventana mínima de 2 ciclos de compra.
5. Escalar o descartar según ROI.

## 12) Recomendación final

Priorizar de inmediato:

1) observabilidad comercial del funnel,  
2) seguridad de configuración y uploads,  
3) motor de recomendaciones + pricing dinámico ligero.

Esta secuencia maximiza retorno: primero medir, luego optimizar ingresos, después escalar operación.

