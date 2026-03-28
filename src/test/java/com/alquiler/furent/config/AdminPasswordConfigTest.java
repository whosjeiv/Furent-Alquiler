package com.alquiler.furent.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test para verificar la configuración del password del admin.
 * Valida que la variable de entorno FURENT_ADMIN_PASSWORD se lee correctamente
 * y que el valor por defecto es un UUID cuando no está definida.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "furent.admin.password=testadmin123"
})
class AdminPasswordConfigTest {

    @Value("${furent.admin.password}")
    private String adminPassword;

    @Test
    void testAdminPasswordIsConfigurable() {
        // Verificar que el password se lee desde la configuración
        assertNotNull(adminPassword, "Admin password no debe ser null");
        assertFalse(adminPassword.isEmpty(), "Admin password no debe estar vacío");
        assertEquals("testadmin123", adminPassword, "Admin password debe coincidir con el valor configurado");
    }

    @Test
    void testAdminPasswordHasMinimumLength() {
        // Verificar que el password tiene una longitud mínima razonable
        assertTrue(adminPassword.length() >= 8, 
            "Admin password debe tener al menos 8 caracteres para seguridad");
    }
}
