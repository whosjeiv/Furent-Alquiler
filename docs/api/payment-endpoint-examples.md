# Payment API Endpoints - Ejemplos de Uso

## GET /api/pagos/{id}

Obtiene un pago por su ID. El usuario autenticado debe ser el dueño del pago.

### Requisitos
- Usuario autenticado (JWT token)
- El usuario debe ser el dueño del pago

### Respuestas

#### 200 OK - Pago encontrado y usuario es el dueño
```json
{
  "id": "payment123",
  "reservaId": "reserva456",
  "monto": 1000.00,
  "metodoPago": "EFECTIVO",
  "estado": "PENDIENTE",
  "referencia": "PAY-ABC12345",
  "fechaCreacion": "2024-01-15T10:30:00",
  "fechaPago": null
}
```

#### 401 Unauthorized - Usuario no autenticado
```json
{
  "error": "No autenticado"
}
```

#### 403 Forbidden - Usuario no es el dueño del pago
```json
{
  "error": "No autorizado para ver este pago"
}
```

#### 404 Not Found - Pago no existe
```json
{
  "error": "Pago no encontrado"
}
```

### Ejemplo de uso con curl

```bash
# Obtener un pago por ID
curl -X GET http://localhost:8080/api/pagos/payment123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## POST /api/pagos/iniciar/{reservaId}

Inicia un pago para una reserva confirmada.

### Request Body
```json
{
  "metodoPago": "EFECTIVO"
}
```

### Respuesta 200 OK
```json
{
  "success": true,
  "paymentId": "payment123",
  "monto": 1000.00,
  "metodoPago": "EFECTIVO"
}
```

## GET /api/pagos/mis-pagos

Obtiene todos los pagos del usuario autenticado.

### Respuesta 200 OK
```json
[
  {
    "id": "payment123",
    "reservaId": "reserva456",
    "monto": 1000.00,
    "metodoPago": "EFECTIVO",
    "estado": "PENDIENTE",
    "referencia": "PAY-ABC12345",
    "fechaCreacion": "2024-01-15T10:30:00",
    "fechaPago": null
  },
  {
    "id": "payment789",
    "reservaId": "reserva012",
    "monto": 500.00,
    "metodoPago": "TARJETA",
    "estado": "PAGADO",
    "referencia": "PAY-XYZ67890",
    "fechaCreacion": "2024-01-10T14:20:00",
    "fechaPago": "2024-01-10T14:25:00"
  }
]
```
