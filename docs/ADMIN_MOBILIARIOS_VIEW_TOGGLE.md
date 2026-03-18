# Toggle de vistas en `admin/mobiliarios`

## Objetivo

Se implementó un selector de vista para alternar entre:

- Vista compacta: nombre, código, estado y stock.
- Vista detallada: información completa del mobiliario, incluyendo descripción, ubicación operativa, fecha de adquisición, valor, responsable, imágenes y estado/historial de mantenimiento.

## Implementación técnica

Archivo principal:

- `src/main/resources/templates/admin/mobiliarios.html`

Elementos incorporados:

- Botones de alternancia con iconos diferenciados (lista y detalle).
- Persistencia del modo durante la sesión usando `sessionStorage` con la clave `admin_mobiliarios_view_mode`.
- Estados accesibles con `aria-pressed`, `aria-label` y `role="group"`.
- Etiqueta de estado visual actual (`Vista compacta` / `Vista detallada`).
- Transiciones suaves en el cambio de modo mediante clases CSS:
  - `inventory-view-transition`
  - `inventory-view-fade`
  - `view-toggle-active`

Comportamiento:

1. Al cargar la página, se intenta restaurar el modo guardado en sesión.
2. Si no existe preferencia guardada, la vista inicial es detallada.
3. Al cambiar de modo:
   - se actualiza `data-view` en `#productGrid`;
   - se sincroniza el estado visual de botones;
   - se actualizan atributos ARIA;
   - se persiste la preferencia en sesión.

## Cobertura de datos en vista detallada

Campos mostrados:

- Nombre
- Código (`id`)
- Estado de disponibilidad
- Descripción corta y completa
- Ubicación operativa (categoría)
- Fecha de adquisición (placeholder: “No registrada”)
- Valor (`precioPorDia`)
- Responsable (placeholder: “Operaciones”)
- Imágenes (conteo de galería)
- Estado de mantenimiento
- Historial de mantenimiento (`notasMantenimiento`)
- Stock y barra de nivel de inventario

## Pruebas de usabilidad ejecutadas

### Validaciones funcionales

- Alternancia de botones:
  - `Compacta` activa la vista compacta.
  - `Detallada` activa la vista detallada.
- Persistencia de sesión:
  - al recargar la página, se conserva el modo seleccionado.
- Integración con búsqueda:
  - el filtro textual continúa funcionando en ambos modos.
- Accesibilidad:
  - botones con `aria-label` y `aria-pressed` correctos por estado.

### Checklist rápido para QA manual

1. Abrir `/admin/mobiliarios`.
2. Cambiar a vista compacta y validar tarjetas simplificadas.
3. Recargar la página y verificar persistencia del modo.
4. Cambiar a vista detallada y verificar campos completos.
5. Probar en anchos móviles, tablet y desktop.
6. Navegar con teclado y activar botones de vista con Enter/Espacio.

### Helper de verificación en navegador

Se expone una función para chequeos básicos desde consola:

- `window.__runMobiliariosViewChecks()`

Retorna un objeto con:

- existencia de contenedor/grid,
- presencia de botones,
- consistencia de `data-view`,
- validez de estados ARIA.

## Mantenimiento futuro

- Si se agregan campos nuevos al modelo `Product`, incluirlos en el bloque `detailed-view-content`.
- Para cambiar persistencia de sesión a persistencia entre sesiones, reemplazar `sessionStorage` por `localStorage`.
- Si se incorporan más modos de visualización, extender:
  - `applyInventoryViewMode`
  - `updateViewToggleUI`
  - estilos `#productGrid[data-view="..."]`.
