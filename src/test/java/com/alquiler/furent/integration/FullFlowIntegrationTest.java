package com.alquiler.furent.integration;

import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.repository.ReservationRepository;
import com.alquiler.furent.repository.UserRepository;
import com.alquiler.furent.repository.RefreshTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración que verifican el flujo completo
 * desde el controlador HTTP hasta la base de datos MongoDB.
 *
 * Usa Testcontainers para levantar MongoDB automáticamente en Docker.
 * Si Docker no está disponible, los tests se saltan automáticamente.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullFlowIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8.0");

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanDatabase() {
        reservationRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ============================================================
    // FLUJO DE AUTENTICACIÓN COMPLETO
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("Registro → Login → /me: flujo JWT completo verificado en MongoDB")
    void authFlow_registerLoginAndGetMe() throws Exception {
        // 1. REGISTRO — POST /api/auth/register
        Map<String, String> registerBody = Map.of(
                "email", "integration@test.com",
                "password", "password123",
                "nombre", "Test",
                "apellido", "User",
                "telefono", "3001234567"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("integration@test.com"));

        // Verificar que el usuario existe en MongoDB
        assertTrue(userRepository.findByEmail("integration@test.com").isPresent(),
                "El usuario debe existir en MongoDB tras el registro");

        // 2. LOGIN — POST /api/auth/login
        Map<String, String> loginBody = Map.of(
                "email", "integration@test.com",
                "password", "password123"
        );

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(loginResponse).get("accessToken").asText();

        // 3. GET /api/auth/me — verificar usuario autenticado
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("integration@test.com"))
                .andExpect(jsonPath("$.nombre").value("Test"))
                .andExpect(jsonPath("$.apellido").value("User"));
    }

    @Test
    @Order(2)
    @DisplayName("Registro duplicado devuelve 409 Conflict")
    void register_duplicateEmail_returns409() throws Exception {
        Map<String, String> body = Map.of(
                "email", "duplicado@test.com",
                "password", "password123",
                "nombre", "Dup",
                "apellido", "Test"
        );

        // Primer registro — éxito
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        // Segundo registro con mismo email — conflicto
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    @Order(3)
    @DisplayName("Login con credenciales inválidas devuelve 401")
    void login_invalidCredentials_returns401() throws Exception {
        Map<String, String> body = Map.of(
                "email", "noexiste@test.com",
                "password", "wrongpassword"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciales inválidas"));
    }

    // ============================================================
    // FLUJO DE COTIZACIÓN COMPLETO
    // ============================================================

    @Test
    @Order(4)
    @DisplayName("Cotización: Registro → Login → Crear cotización → Verificar en MongoDB")
    void cotizacionFlow_createAndVerifyInDatabase() throws Exception {
        // 1. Registrar usuario
        String token = registerAndGetToken("cotizacion@test.com", "password123", "Carlos", "Test");

        // 2. Crear cotización — POST /api/cotizacion
        LocalDate inicio = LocalDate.now().plusDays(5);
        LocalDate fin = LocalDate.now().plusDays(8);

        Map<String, Object> cotizacion = Map.of(
                "tipoEvento", "Boda",
                "invitados", 100,
                "fechaInicio", inicio.toString(),
                "fechaFin", fin.toString(),
                "direccion", "Calle 123, Bogotá",
                "notas", "Evento de prueba integración",
                "metodoPago", "TRANSFERENCIA",
                "items", List.of(Map.of(
                        "id", "prod-001",
                        "name", "Silla Tiffany",
                        "image", "/img/silla.jpg",
                        "price", 25000,
                        "qty", 50
                ))
        );

        MvcResult result = mockMvc.perform(post("/api/cotizacion")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cotizacion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.reservationId").isNotEmpty())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String reservationId = objectMapper.readTree(responseBody).get("reservationId").asText();

        // 3. Verificar en MongoDB
        Reservation savedReservation = reservationRepository.findById(reservationId).orElse(null);
        assertNotNull(savedReservation, "La reserva debe existir en MongoDB");
        assertEquals("PENDIENTE", savedReservation.getEstado());
        assertEquals("Boda", savedReservation.getTipoEvento());
        assertEquals("Calle 123, Bogotá", savedReservation.getDireccionEvento());
        assertEquals(inicio, savedReservation.getFechaInicio());
        assertEquals(fin, savedReservation.getFechaFin());

        // Verificar cálculo de total: 25000 * 50 items * 3 días = 3,750,000
        assertNotNull(savedReservation.getTotal());
        assertEquals(0, savedReservation.getTotal().compareTo(new BigDecimal("3750000")),
                "Total debe ser 25000 × 50 × 3 días = 3,750,000");
    }

    @Test
    @Order(5)
    @DisplayName("Cotización sin autenticación devuelve 403 Forbidden")
    void cotizacion_withoutAuth_returns401() throws Exception {
        Map<String, Object> cotizacion = Map.of(
                "tipoEvento", "Fiesta",
                "invitados", 50,
                "fechaInicio", LocalDate.now().plusDays(1).toString(),
                "fechaFin", LocalDate.now().plusDays(2).toString(),
                "direccion", "Calle 456",
                "metodoPago", "EFECTIVO",
                "items", List.of(Map.of(
                        "id", "prod-001", "name", "Mesa", "price", 50000, "qty", 10
                ))
        );

        mockMvc.perform(post("/api/cotizacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cotizacion)))
                .andExpect(status().isForbidden());
    }

    // ============================================================
    // FLUJO DE ESTADO DE RESERVA — MÁQUINA DE ESTADOS
    // ============================================================

    @Test
    @Order(6)
    @DisplayName("Máquina de estados: cotización persistida con estado PENDIENTE en MongoDB")
    void reservationState_savedAsPendiente() throws Exception {
        String token = registerAndGetToken("estado@test.com", "password123", "Ana", "Estado");

        LocalDate inicio = LocalDate.now().plusDays(10);
        LocalDate fin = LocalDate.now().plusDays(12);

        Map<String, Object> cotizacion = Map.of(
                "tipoEvento", "Conferencia",
                "invitados", 200,
                "fechaInicio", inicio.toString(),
                "fechaFin", fin.toString(),
                "direccion", "Centro de convenciones",
                "metodoPago", "TARJETA",
                "items", List.of(Map.of(
                        "id", "prod-002", "name", "Tarima", "price", 500000, "qty", 1
                ))
        );

        MvcResult result = mockMvc.perform(post("/api/cotizacion")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cotizacion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String reservationId = objectMapper.readTree(
                result.getResponse().getContentAsString()).get("reservationId").asText();

        // Verificar estado PENDIENTE en MongoDB
        Reservation reserva = reservationRepository.findById(reservationId).orElseThrow();
        assertEquals("PENDIENTE", reserva.getEstado());
        assertEquals("estado@test.com", reserva.getUsuarioEmail());

        // Verificar que aparece en la lista de pendientes
        List<Reservation> pendientes = reservationRepository.findByEstado("PENDIENTE");
        assertTrue(pendientes.stream().anyMatch(r -> r.getId().equals(reservationId)),
                "La reserva debe aparecer en las pendientes");
    }

    // ============================================================
    // BÚSQUEDA DE PRODUCTOS (endpoint público)
    // ============================================================

    @Test
    @Order(7)
    @DisplayName("Búsqueda de productos funciona sin autenticación")
    void searchProducts_withoutAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/productos/search")
                        .param("q", "silla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ============================================================
    // REFRESH TOKEN
    // ============================================================

    @Test
    @Order(8)
    @DisplayName("Refresh token genera nuevo accessToken válido")
    void refreshToken_generatesNewAccessToken() throws Exception {
        // Registrar y obtener refresh token
        Map<String, String> registerBody = Map.of(
                "email", "refresh@test.com",
                "password", "password123",
                "nombre", "Refresh",
                "apellido", "Test"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated())
                .andReturn();

        String refreshToken = objectMapper.readTree(
                registerResult.getResponse().getContentAsString()).get("refreshToken").asText();

        // Usar refresh token para obtener nuevo access token
        Map<String, String> refreshBody = Map.of("refreshToken", refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private String registerAndGetToken(String email, String password, String nombre, String apellido)
            throws Exception {
        Map<String, String> body = Map.of(
                "email", email,
                "password", password,
                "nombre", nombre,
                "apellido", apellido
        );

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}
