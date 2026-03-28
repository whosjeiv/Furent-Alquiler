package com.alquiler.furent.controller;

import com.alquiler.furent.model.ContactMessage;
import com.alquiler.furent.service.ContactService;
import com.alquiler.furent.config.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactControllerTest {

    @Mock
    private ContactService contactService;

    @Mock
    private Model model;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private ContactController contactController;

    private ContactMessage validMessage;

    @BeforeEach
    void setUp() {
        validMessage = new ContactMessage();
        validMessage.setNombre("Juan Pérez");
        validMessage.setEmail("juan@example.com");
        validMessage.setTelefono("+57 300 000 0000");
        validMessage.setAsunto("Consulta general");
        validMessage.setMensaje("Hola, me gustaría obtener más información sobre sus servicios.");
    }

    @Test
    void testContactForm_ShouldReturnContactTemplate() {
        // When
        String viewName = contactController.contactForm(model);

        // Then
        assertEquals("contact", viewName);
        verify(model).addAttribute(eq("contactMessage"), any(ContactMessage.class));
    }

    @Test
    void testContactForm_ShouldNotAddMessageIfAlreadyPresent() {
        // Given
        when(model.containsAttribute("contactMessage")).thenReturn(true);

        // When
        String viewName = contactController.contactForm(model);

        // Then
        assertEquals("contact", viewName);
        verify(model, never()).addAttribute(eq("contactMessage"), any());
    }

    @Test
    void testSubmitContact_WithValidData_ShouldSaveAndRedirect() {
        // Given
        when(bindingResult.hasErrors()).thenReturn(false);
        TenantContext.setCurrentTenant("tenant1");

        // When
        String result = contactController.submitContact(validMessage, bindingResult, redirectAttributes);

        // Then
        assertEquals("redirect:/contacto", result);
        
        ArgumentCaptor<ContactMessage> messageCaptor = ArgumentCaptor.forClass(ContactMessage.class);
        verify(contactService).save(messageCaptor.capture());
        
        ContactMessage savedMessage = messageCaptor.getValue();
        assertEquals("Juan Pérez", savedMessage.getNombre());
        assertEquals("juan@example.com", savedMessage.getEmail());
        assertEquals("tenant1", savedMessage.getTenantId());
        
        verify(redirectAttributes).addFlashAttribute(eq("success"), anyString());
        
        // Cleanup
        TenantContext.clear();
    }

    @Test
    void testSubmitContact_WithValidationErrors_ShouldRedirectWithErrors() {
        // Given
        when(bindingResult.hasErrors()).thenReturn(true);

        // When
        String result = contactController.submitContact(validMessage, bindingResult, redirectAttributes);

        // Then
        assertEquals("redirect:/contacto", result);
        verify(contactService, never()).save(any());
        verify(redirectAttributes).addFlashAttribute(eq("org.springframework.validation.BindingResult.contactMessage"), eq(bindingResult));
        verify(redirectAttributes).addFlashAttribute(eq("contactMessage"), eq(validMessage));
    }

    @Test
    void testSubmitContact_WithNullTenant_ShouldUseDefaultTenant() {
        // Given
        when(bindingResult.hasErrors()).thenReturn(false);
        TenantContext.clear();

        // When
        String result = contactController.submitContact(validMessage, bindingResult, redirectAttributes);

        // Then
        assertEquals("redirect:/contacto", result);
        
        ArgumentCaptor<ContactMessage> messageCaptor = ArgumentCaptor.forClass(ContactMessage.class);
        verify(contactService).save(messageCaptor.capture());
        
        ContactMessage savedMessage = messageCaptor.getValue();
        assertEquals("default", savedMessage.getTenantId());
    }

    @Test
    void testSubmitContact_WithServiceException_ShouldShowErrorMessage() {
        // Given
        when(bindingResult.hasErrors()).thenReturn(false);
        when(contactService.save(any())).thenThrow(new RuntimeException("Database error"));
        TenantContext.setCurrentTenant("tenant1");

        // When
        String result = contactController.submitContact(validMessage, bindingResult, redirectAttributes);

        // Then
        assertEquals("redirect:/contacto", result);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
        
        // Cleanup
        TenantContext.clear();
    }

    @Test
    void testSubmitContact_ShouldSetTenantIdBeforeSaving() {
        // Given
        when(bindingResult.hasErrors()).thenReturn(false);
        TenantContext.setCurrentTenant("custom-tenant");

        // When
        contactController.submitContact(validMessage, bindingResult, redirectAttributes);

        // Then
        ArgumentCaptor<ContactMessage> messageCaptor = ArgumentCaptor.forClass(ContactMessage.class);
        verify(contactService).save(messageCaptor.capture());
        
        ContactMessage savedMessage = messageCaptor.getValue();
        assertEquals("custom-tenant", savedMessage.getTenantId());
        
        // Cleanup
        TenantContext.clear();
    }
}
