# Swagger/OpenAPI Documentation

## Overview

This document describes the Swagger/OpenAPI documentation added to the Furent API as part of task 106.

## Swagger UI Access

The Swagger UI is accessible at: **`/swagger-ui.html`** or **`/swagger-ui/index.html`**

The OpenAPI JSON specification is available at: **`/v3/api-docs`**

## Documented Endpoints

### 1. Payment Endpoints (`/api/pagos`)

**Tag:** Pagos

All payment-related endpoints have been documented with:
- Operation summaries and descriptions
- Request/response examples
- HTTP status codes (200, 400, 401, 403, 404)
- Authentication requirements

**Endpoints:**
- `GET /api/pagos/pending/{pendingId}` - Get pending payment details
- `GET /api/pagos/payu/config` - Get PayU configuration
- `POST /api/pagos/payu/confirmacion` - PayU webhook for payment confirmation
- `POST /api/pagos/iniciar/{reservaId}` - Initialize a new payment
- `GET /api/pagos/reserva/{reservaId}` - Get payment by reservation
- `GET /api/pagos/mis-pagos` - Get user's payments
- `GET /api/pagos/{id}` - Get payment by ID

**Example Request/Response:**

```json
// POST /api/pagos/iniciar/{reservaId}
// Request Body:
{
  "metodoPago": "EFECTIVO"
}

// Response:
{
  "success": true,
  "paymentId": "65f1a2b3c4d5e6f7g8h9i0j1",
  "monto": 1500.00,
  "metodoPago": "EFECTIVO"
}
```

### 2. Admin Payment Endpoints (`/admin/pagos`)

**Tag:** Admin - Pagos

Administrative payment management endpoints:
- `GET /admin/pagos` - List all payments (paginated)
- `POST /admin/pagos/{id}/confirmar` - Confirm a pending payment
- `POST /admin/pagos/{id}/rechazar` - Reject a pending payment

**Security:** All endpoints require `ROLE_ADMIN`

### 3. Coupon Endpoints (`/api/cupones`)

**Tag:** API Pública

Coupon validation endpoint already documented in ApiController:
- `POST /api/cupones/validar` - Validate coupon code and calculate discount

**Example Request/Response:**

```json
// POST /api/cupones/validar
// Request:
{
  "codigo": "VERANO2024",
  "montoTotal": 1000.00
}

// Response:
{
  "valido": true,
  "descuento": 15,
  "montoDescuento": 150.00,
  "montoFinal": 850.00
}
```

### 4. Admin Coupon Endpoints (`/admin/cupones`)

**Tag:** Admin - Cupones

Administrative coupon management:
- `GET /admin/cupones` - List all coupons
- `POST /admin/cupones/guardar` - Create or update coupon
- `DELETE /admin/cupones/{id}` - Delete coupon

**Validation Rules:**
- Percentage discounts cannot exceed 100%
- All values must be positive
- Usage limits must be greater than 0

### 5. Password Reset Endpoints

**Tag:** Recuperación de Contraseña

Password recovery flow endpoints:
- `GET /password-reset` - Show password reset request form
- `POST /password-reset` - Request password reset (sends email)
- `GET /password-reset/{token}` - Show password reset confirmation form
- `POST /password-reset/confirm` - Confirm password reset with token

**Security Features:**
- Does not reveal if email exists (generic message)
- Tokens expire after 1 hour
- Tokens are single-use only
- Passwords are encrypted with BCrypt

**Example Flow:**

```
1. User requests reset: POST /password-reset?email=user@example.com
   → Response: "Si el email existe, recibirás un enlace de recuperación"

2. User receives email with token link: /password-reset/{token}

3. User submits new password: POST /password-reset/confirm
   → Parameters: token, password, passwordConfirm
   → Response: Redirect to /login with success message
```

## OpenAPI Configuration

The OpenAPI configuration is defined in `OpenApiConfig.java`:

- **Title:** Furent API — Alquiler de Mobiliarios
- **Version:** 1.0.0
- **Security:** Bearer JWT authentication
- **Contact:** soporte@furent.co

## Authentication

Most endpoints require JWT authentication. To use authenticated endpoints in Swagger UI:

1. Obtain a JWT token from `/api/auth/login`
2. Click the "Authorize" button in Swagger UI
3. Enter: `Bearer <your-token>`
4. Click "Authorize"

## Testing with Swagger UI

1. Start the application
2. Navigate to `http://localhost:8080/swagger-ui.html`
3. Browse available endpoints by tag
4. Click on an endpoint to see details
5. Click "Try it out" to test the endpoint
6. Fill in parameters and click "Execute"

## Notes

- All endpoints include proper HTTP status codes
- Request/response examples are provided where applicable
- Admin endpoints are clearly marked with security requirements
- Error responses include descriptive messages
- The documentation follows OpenAPI 3.0 specification
