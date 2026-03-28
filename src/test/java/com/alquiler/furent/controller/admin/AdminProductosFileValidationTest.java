package com.alquiler.furent.controller.admin;

import com.alquiler.furent.exception.InvalidOperationException;
import com.alquiler.furent.service.AuditLogService;
import com.alquiler.furent.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para validación de tipos de archivo en AdminProductosController.
 * Valida Requirements 4.1, 4.2, 4.3
 */
@ExtendWith(MockitoExtension.class)
class AdminProductosFileValidationTest {

    @Mock
    private ProductService productService;

    @Mock
    private AuditLogService auditLogService;

    private AdminProductosController controller;
    private Method validateImageFileMethod;

    @BeforeEach
    void setUp() throws Exception {
        controller = new AdminProductosController(productService, auditLogService);
        
        // Acceder al método privado validateImageFile usando reflexión
        validateImageFileMethod = AdminProductosController.class.getDeclaredMethod(
            "validateImageFile", MultipartFile.class
        );
        validateImageFileMethod.setAccessible(true);
    }

    /**
     * Requirement 4.1: Validar Content-Type permitidos
     */
    @Test
    void validateImageFile_shouldAcceptJpegImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            "test image content".getBytes()
        );

        assertDoesNotThrow(() -> {
            try {
                validateImageFileMethod.invoke(controller, file);
            } catch (Exception e) {
                throw e.getCause();
            }
        });
    }

    @Test
    void validateImageFile_shouldAcceptPngImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.png",
            "image/png",
            "test image content".getBytes()
        );

        assertDoesNotThrow(() -> {
            try {
                validateImageFileMethod.invoke(controller, file);
            } catch (Exception e) {
                throw e.getCause();
            }
        });
    }

    @Test
    void validateImageFile_shouldAcceptWebpImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.webp",
            "image/webp",
            "test image content".getBytes()
        );

        assertDoesNotThrow(() -> {
            try {
                validateImageFileMethod.invoke(controller, file);
            } catch (Exception e) {
                throw e.getCause();
            }
        });
    }

    @Test
    void validateImageFile_shouldAcceptGifImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.gif",
            "image/gif",
            "test image content".getBytes()
        );

        assertDoesNotThrow(() -> {
            try {
                validateImageFileMethod.invoke(controller, file);
            } catch (Exception e) {
                throw e.getCause();
            }
        });
    }

    /**
     * Requirement 4.1: Rechazar tipos de archivo no permitidos
     */
    @Test
    void validateImageFile_shouldRejectPdfFile() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "test pdf content".getBytes()
        );

        Exception exception = assertThrows(Exception.class, () -> {
            try {
                validateImageFileMethod.invoke(controller, file);
            } catch (Exception e) {
                throw e.getCause();
            }
        });

        assertTrue(exception instanceof InvalidOperationException);
        assertTrue(exception.getMessage().contains("Solo se permiten imágenes"));
    }

    @Test
    void validateImageFile_shouldRejectTextFile() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "test text content".getBytes()
        );

        Exception exception = assertThrows(Exception.class, () -> {
            try {
                validateImageFileMethod.invoke(controller, file);
            } catch (Exception e) {
                throw e.getCause();
            }
        });

        assertTrue(exception instanceof InvalidOperationException);
        assertTrue(exception.getMessage().contains("Solo se permiten imágenes"));
    }

    @Test
    void validateImageFile_shouldRejectExecutableFile() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "malicious.exe",
            "application/x-msdownload",
            "malicious content".getBytes()
        );

        Exception exception = assertThrows(Exception.class, () -> {
            try {
                validateImageFileMethod.invoke(controller, file);
            } catch (Exception e) {
                throw e.getCause();
            }
        });

        assertTrue(exception instanceof InvalidOperationException);
        assertTrue(exception.getMessage().contains("Solo se permiten imágenes"));
    }

    /**
     * Requirement 4.2: Validar tamaño máximo de 5MB
     */
    @Test
    void validateImageFile_shouldAcceptFileUnder5MB() throws Exception {
        // Crear archivo de 4MB
        byte[] content = new byte[4 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "large.jpg",
            "image/jpeg",
            content
        );

        assertDoesNotThrow(() -> {
            try {
                validateImageFileMethod.invoke(controller, file);
            } catch (Exception e) {
                throw e.getCause();
            }
        });
    }

    @Test
    void validateImageFile_shouldAcceptFileExactly5MB() throws Exception {
        // Crear archivo de exactamente 5MB
        byte[] content = new byte[5 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "exact5mb.jpg",
            "image/jpeg",
            content
        );

        assertDoesNotThrow(() -> {
            try {
                validateImageFileMethod.invoke(controller, file);
            } catch (Exception e) {
                throw e.getCause();
            }
        });
    }

    /**
     * Requirement 4.3: Rechazar archivos mayores a 5MB
     */
    @Test
    void validateImageFile_shouldRejectFileOver5MB() {
        // Crear archivo de 6MB
        byte[] content = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "toolarge.jpg",
            "image/jpeg",
            content
        );

        Exception exception = assertThrows(Exception.class, () -> {
            try {
                validateImageFileMethod.invoke(controller, file);
            } catch (Exception e) {
                throw e.getCause();
            }
        });

        assertTrue(exception instanceof InvalidOperationException);
        assertTrue(exception.getMessage().contains("no puede superar 5MB"));
    }

    @Test
    void validateImageFile_shouldRejectFileWayOver5MB() {
        // Crear archivo de 10MB
        byte[] content = new byte[10 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "huge.jpg",
            "image/jpeg",
            content
        );

        Exception exception = assertThrows(Exception.class, () -> {
            try {
                validateImageFileMethod.invoke(controller, file);
            } catch (Exception e) {
                throw e.getCause();
            }
        });

        assertTrue(exception instanceof InvalidOperationException);
        assertTrue(exception.getMessage().contains("no puede superar 5MB"));
    }

    /**
     * Requirement 4.3: Rechazar archivos vacíos
     */
    @Test
    void validateImageFile_shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "empty.jpg",
            "image/jpeg",
            new byte[0]
        );

        Exception exception = assertThrows(Exception.class, () -> {
            try {
                validateImageFileMethod.invoke(controller, file);
            } catch (Exception e) {
                throw e.getCause();
            }
        });

        assertTrue(exception instanceof InvalidOperationException);
        assertTrue(exception.getMessage().contains("Debes seleccionar un archivo"));
    }

    /**
     * Test de validación de múltiples tipos de contenido maliciosos
     */
    @Test
    void validateImageFile_shouldRejectVariousMaliciousTypes() {
        String[] maliciousTypes = {
            "application/x-sh",
            "application/javascript",
            "text/html",
            "application/zip",
            "application/x-executable"
        };

        for (String contentType : maliciousTypes) {
            MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicious",
                contentType,
                "malicious content".getBytes()
            );

            Exception exception = assertThrows(Exception.class, () -> {
                try {
                    validateImageFileMethod.invoke(controller, file);
                } catch (Exception e) {
                    throw e.getCause();
                }
            }, "Should reject content type: " + contentType);

            assertTrue(exception instanceof InvalidOperationException);
        }
    }
}
