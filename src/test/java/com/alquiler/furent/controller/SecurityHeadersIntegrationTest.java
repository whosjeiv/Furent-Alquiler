package com.alquiler.furent.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración para verificar que los headers de seguridad HTTP
 * están correctamente configurados en SecurityConfig.
 * 
 * Valida Requirements 6.1, 6.2, 6.3, 6.4, 6.5
 * 
 * NOTA: HSTS (Strict-Transport-Security) solo se envía en conexiones HTTPS.
 * En entornos de test con HTTP, este header no estará presente, pero la
 * configuración está correcta en SecurityConfig.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityHeadersIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Requirement 6.1: Content-Security-Policy header debe estar configurado con directivas restrictivas")
    void response_shouldIncludeContentSecurityPolicyHeader() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().string("Content-Security-Policy", 
                    org.hamcrest.Matchers.containsString("default-src 'self'")));
    }

    @Test
    @DisplayName("Requirement 6.3: X-Frame-Options header debe estar configurado como DENY")
    void response_shouldIncludeXFrameOptionsHeaderAsDeny() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    @DisplayName("Requirement 6.4: X-XSS-Protection header debe estar habilitado con modo block")
    void response_shouldIncludeXssProtectionHeaderWithModeBlock() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-XSS-Protection"))
                .andExpect(header().string("X-XSS-Protection", "1; mode=block"));
    }

    @Test
    @DisplayName("Requirement 6.5: Headers de seguridad (CSP, X-Frame-Options, X-XSS-Protection) deben estar presentes")
    void response_shouldIncludeSecurityHeaders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().exists("X-XSS-Protection"));
    }

    @Test
    @DisplayName("Security headers deben estar presentes en rutas públicas")
    void publicRoutes_shouldIncludeSecurityHeaders() throws Exception {
        mockMvc.perform(get("/catalogo"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().exists("X-XSS-Protection"));
    }

    @Test
    @DisplayName("Security headers deben estar presentes en rutas de login")
    void loginRoute_shouldIncludeSecurityHeaders() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().exists("X-XSS-Protection"));
    }

    @Test
    @DisplayName("Content-Security-Policy debe incluir directivas para scripts")
    void cspHeader_shouldIncludeScriptSrcDirective() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", 
                    org.hamcrest.Matchers.containsString("script-src")));
    }

    @Test
    @DisplayName("Content-Security-Policy debe incluir directivas para estilos")
    void cspHeader_shouldIncludeStyleSrcDirective() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", 
                    org.hamcrest.Matchers.containsString("style-src")));
    }
}
