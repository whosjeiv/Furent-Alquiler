package com.alquiler.furent.controller;

import com.alquiler.furent.model.User;
import com.alquiler.furent.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración para verificar la protección CSRF en formularios web.
 * 
 * Valida Requirement 3: Habilitación de Protección CSRF
 * - Acceptance Criteria 3.1: CSRF habilitado excepto para /api/**
 * - Acceptance Criteria 3.3: Formularios sin token válido retornan HTTP 403
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CsrfProtectionIntegrationTest {

    @LocalServerPort
    private int port;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        
        userRepository.deleteAll();
        
        // Crear usuario de prueba
        User testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setNombre("Test");
        testUser.setApellido("User");
        testUser.setRole("ROLE_USER");
        testUser.setActivo(true);
        userRepository.save(testUser);
    }

    /**
     * Test: Formulario de login sin token CSRF debe retornar HTTP 403
     * Valida AC 3.3: Formularios sin token válido retornan HTTP 403
     */
    @Test
    void loginForm_withoutCsrfToken_shouldReturn403() throws Exception {
        mockMvc.perform(post("/login")
                .param("email", "test@example.com")
                .param("password", "password123"))
                .andExpect(status().isForbidden());
    }

    /**
     * Test: Formulario de login con token CSRF válido debe procesar correctamente
     * Valida AC 3.2: Formularios con token válido se procesan correctamente
     */
    @Test
    void loginForm_withValidCsrfToken_shouldProcessSuccessfully() throws Exception {
        mockMvc.perform(post("/login")
                .param("email", "test@example.com")
                .param("password", "password123")
                .with(csrf()))
                .andExpect(status().is3xxRedirection()); // Redirect después de login exitoso
    }

    /**
     * Test: Formulario de registro sin token CSRF debe retornar HTTP 403
     * Valida AC 3.3: Formularios sin token válido retornan HTTP 403
     */
    @Test
    void registerForm_withoutCsrfToken_shouldReturn403() throws Exception {
        mockMvc.perform(post("/registro")
                .param("nombre", "John")
                .param("apellido", "Doe")
                .param("email", "john@example.com")
                .param("password", "SecurePass123!"))
                .andExpect(status().isForbidden());
    }

    /**
     * Test: Formulario de registro con token CSRF válido debe procesar correctamente
     * Valida AC 3.2: Formularios con token válido se procesan correctamente
     */
    @Test
    void registerForm_withValidCsrfToken_shouldProcessSuccessfully() throws Exception {
        mockMvc.perform(post("/registro")
                .param("nombre", "John")
                .param("apellido", "Doe")
                .param("email", "john@example.com")
                .param("password", "SecurePass123!")
                .with(csrf()))
                .andExpect(status().is3xxRedirection()); // Redirect después de registro exitoso
    }

    /**
     * Test: Peticiones a /api/** sin token CSRF deben procesarse normalmente
     * Valida AC 3.4: Peticiones a /api/** se procesan sin token CSRF
     */
    @Test
    void apiEndpoint_withoutCsrfToken_shouldProcessNormally() throws Exception {
        // Las peticiones a /api/** no requieren CSRF token
        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk()); // O cualquier status que no sea 403
    }

    /**
     * Test: Formulario de password reset sin token CSRF debe retornar HTTP 403
     * Valida AC 3.3: Formularios sin token válido retornan HTTP 403
     */
    @Test
    void passwordResetForm_withoutCsrfToken_shouldReturn403() throws Exception {
        mockMvc.perform(post("/password-reset")
                .param("email", "test@example.com"))
                .andExpect(status().isForbidden());
    }

    /**
     * Test: Formulario de password reset con token CSRF válido debe procesar correctamente
     * Valida AC 3.2: Formularios con token válido se procesan correctamente
     */
    @Test
    void passwordResetForm_withValidCsrfToken_shouldProcessSuccessfully() throws Exception {
        mockMvc.perform(post("/password-reset")
                .param("email", "test@example.com")
                .with(csrf()))
                .andExpect(status().is3xxRedirection()); // Redirect después de solicitud exitosa
    }
}
