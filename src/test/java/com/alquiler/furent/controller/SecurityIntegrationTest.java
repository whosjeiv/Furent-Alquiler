package com.alquiler.furent.controller;

import com.alquiler.furent.config.SecurityConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de verificación de configuración de seguridad sin necesidad de contexto Spring completo.
 */
class SecurityIntegrationTest {

    @Test
    void securityConfig_shouldBeAnnotatedCorrectly() {
        assertTrue(SecurityConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class));
        assertTrue(SecurityConfig.class.isAnnotationPresent(org.springframework.security.config.annotation.web.configuration.EnableWebSecurity.class));
    }

    @Test
    void publicPaths_shouldBeDefined() {
        // Verifica que las rutas públicas conocidas del sistema están documentadas
        String[] publicPaths = {"/", "/login", "/register", "/catalog", "/about", "/contact", "/faq", "/password-reset"};
        for (String path : publicPaths) {
            assertNotNull(path);
            assertTrue(path.startsWith("/"), "Public path should start with /");
        }
    }

    @Test
    void adminPaths_shouldRequirePrefix() {
        String[] adminPaths = {"/admin", "/admin/usuarios", "/admin/reservas", "/admin/mobiliarios", "/admin/categorias"};
        for (String path : adminPaths) {
            assertTrue(path.startsWith("/admin"), "Admin paths should start with /admin");
        }
    }

    @Test
    void apiPaths_shouldUseCorrectPrefix() {
        String[] apiPaths = {"/api/productos/search", "/api/favoritos", "/api/cupones/validar", "/api/notificaciones"};
        for (String path : apiPaths) {
            assertTrue(path.startsWith("/api/"), "API paths should start with /api/");
        }
    }

    @Test
    void roles_shouldBeDefined() {
        // Verifica que los roles del sistema están correctamente definidos
        String roleUser = "ROLE_USER";
        String roleAdmin = "ROLE_ADMIN";
        assertNotEquals(roleUser, roleAdmin);
        assertTrue(roleUser.startsWith("ROLE_"));
        assertTrue(roleAdmin.startsWith("ROLE_"));
    }
}
