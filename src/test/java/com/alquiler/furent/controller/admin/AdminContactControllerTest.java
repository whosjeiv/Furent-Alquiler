package com.alquiler.furent.controller.admin;

import com.alquiler.furent.model.ContactMessage;
import com.alquiler.furent.service.ContactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminContactControllerTest {

    @Mock
    private ContactService contactService;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private AdminContactController controller;

    private ContactMessage testMessage;

    @BeforeEach
    void setUp() {
        testMessage = new ContactMessage();
        testMessage.setId("msg1");
        testMessage.setNombre("Juan Pérez");
        testMessage.setEmail("juan@example.com");
        testMessage.setMensaje("Consulta sobre productos");
        testMessage.setLeido(false);
        testMessage.setFechaCreacion(LocalDateTime.now());
    }

    @Test
    void listMessages_WithoutFilter_ReturnsAllMessages() {
        // Arrange
        List<ContactMessage> messages = Arrays.asList(testMessage);
        Page<ContactMessage> page = new PageImpl<>(messages);
        when(contactService.findAll(null, 0, 20)).thenReturn(page);
        when(contactService.countUnread()).thenReturn(5L);

        // Act
        String viewName = controller.listMessages(null, 0, model);

        // Assert
        assertThat(viewName).isEqualTo("admin/mensajes");
        verify(contactService).findAll(null, 0, 20);
        verify(contactService).countUnread();
        verify(model).addAttribute("mensajes", page);
        verify(model).addAttribute("estadoFiltro", null);
        verify(model).addAttribute("unreadCount", 5L);
    }

    @Test
    void listMessages_WithNoLeidoFilter_ReturnsUnreadMessages() {
        // Arrange
        List<ContactMessage> messages = Arrays.asList(testMessage);
        Page<ContactMessage> page = new PageImpl<>(messages);
        when(contactService.findAll("NO_LEIDO", 0, 20)).thenReturn(page);
        when(contactService.countUnread()).thenReturn(1L);

        // Act
        String viewName = controller.listMessages("NO_LEIDO", 0, model);

        // Assert
        assertThat(viewName).isEqualTo("admin/mensajes");
        verify(contactService).findAll("NO_LEIDO", 0, 20);
        verify(model).addAttribute("estadoFiltro", "NO_LEIDO");
    }

    @Test
    void listMessages_WithLeidoFilter_ReturnsReadMessages() {
        // Arrange
        testMessage.setLeido(true);
        List<ContactMessage> messages = Arrays.asList(testMessage);
        Page<ContactMessage> page = new PageImpl<>(messages);
        when(contactService.findAll("LEIDO", 0, 20)).thenReturn(page);
        when(contactService.countUnread()).thenReturn(0L);

        // Act
        String viewName = controller.listMessages("LEIDO", 0, model);

        // Assert
        assertThat(viewName).isEqualTo("admin/mensajes");
        verify(contactService).findAll("LEIDO", 0, 20);
        verify(model).addAttribute("estadoFiltro", "LEIDO");
    }

    @Test
    void listMessages_WithPagination_ReturnsCorrectPage() {
        // Arrange
        List<ContactMessage> messages = Arrays.asList(testMessage);
        Page<ContactMessage> page = new PageImpl<>(messages);
        when(contactService.findAll(null, 2, 20)).thenReturn(page);
        when(contactService.countUnread()).thenReturn(5L);

        // Act
        String viewName = controller.listMessages(null, 2, model);

        // Assert
        assertThat(viewName).isEqualTo("admin/mensajes");
        verify(contactService).findAll(null, 2, 20);
    }

    @Test
    void markAsRead_WithValidId_MarksMessageAsRead() {
        // Arrange
        doNothing().when(contactService).markAsRead("msg1");

        // Act
        String redirect = controller.markAsRead("msg1", redirectAttributes);

        // Assert
        assertThat(redirect).isEqualTo("redirect:/admin/mensajes");
        verify(contactService).markAsRead("msg1");
        verify(redirectAttributes).addFlashAttribute("success", "Mensaje marcado como leído");
    }

    @Test
    void markAsRead_WithException_ReturnsErrorMessage() {
        // Arrange
        doThrow(new RuntimeException("Mensaje no encontrado"))
            .when(contactService).markAsRead("invalid");

        // Act
        String redirect = controller.markAsRead("invalid", redirectAttributes);

        // Assert
        assertThat(redirect).isEqualTo("redirect:/admin/mensajes");
        verify(redirectAttributes).addFlashAttribute(eq("error"), contains("Error al marcar mensaje"));
    }

    @Test
    void listMessages_IncludesUnreadCountInModel() {
        // Arrange
        List<ContactMessage> messages = Arrays.asList(testMessage);
        Page<ContactMessage> page = new PageImpl<>(messages);
        when(contactService.findAll(null, 0, 20)).thenReturn(page);
        when(contactService.countUnread()).thenReturn(3L);

        // Act
        controller.listMessages(null, 0, model);

        // Assert
        verify(model).addAttribute("unreadCount", 3L);
    }
}
