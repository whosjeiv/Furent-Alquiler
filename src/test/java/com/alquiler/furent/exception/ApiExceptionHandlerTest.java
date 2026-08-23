package com.alquiler.furent.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiExceptionHandlerTest {

    private ApiExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new ApiExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void handleNotFound_returns404() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleNotFound(new ResourceNotFoundException("Product", "123"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertNotNull(response.getBody().get("timestamp"));
        assertEquals("/api/test", response.getBody().get("path"));
    }

    @Test
    void handleDuplicate_returns409() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleDuplicate(new DuplicateResourceException("Email ya existe"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Email ya existe", response.getBody().get("message"));
    }

    @Test
    void handleInvalidOperation_returns400() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleInvalidOperation(new InvalidOperationException("Transición inválida"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
    }

    @Test
    void handleSuspended_returns403WithDetails() {
        AccountSuspendedException ex = new AccountSuspendedException(
                "Cuenta suspendida", "Fraude", "30 días", false);

        ResponseEntity<Map<String, Object>> response = handler.handleSuspended(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Fraude", response.getBody().get("reason"));
        assertEquals("30 días", response.getBody().get("duration"));
        assertEquals(false, response.getBody().get("permanent"));
    }

    @Test
    void handleSuspended_permanent_flagIsTrue() {
        AccountSuspendedException ex = new AccountSuspendedException(
                "Cuenta suspendida permanentemente", "Violación de TOS", null, true);

        ResponseEntity<Map<String, Object>> response = handler.handleSuspended(ex, request);

        assertEquals(true, response.getBody().get("permanent"));
    }

    @Test
    void handleAccessDenied_returns403() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleAccessDenied(new AccessDeniedException("Denied"), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("No tienes permiso para realizar esta acción", response.getBody().get("message"));
    }

    @Test
    void handleIllegalArgument_returns400() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalArgument(new IllegalArgumentException("ID inválido"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("ID inválido", response.getBody().get("message"));
    }

    @Test
    void handleRateLimit_returns429() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleRateLimit(new TooManyRequestsException("Demasiadas solicitudes"), request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals(429, response.getBody().get("status"));
    }

    @Test
    void handleGeneral_returns500() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGeneral(new RuntimeException("Unexpected"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().get("status"));
        assertEquals("Ha ocurrido un error inesperado. Por favor, intenta de nuevo.",
                response.getBody().get("message"));
    }

    @Test
    void allResponses_containRequiredFields() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleNotFound(new ResourceNotFoundException("X", "1"), request);

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("status"));
        assertTrue(body.containsKey("error"));
        assertTrue(body.containsKey("message"));
        assertTrue(body.containsKey("path"));
    }
}
