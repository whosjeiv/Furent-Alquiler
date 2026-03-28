package com.alquiler.furent.controller;

import com.alquiler.furent.model.ContactMessage;
import com.alquiler.furent.service.ContactService;
import com.alquiler.furent.config.TenantContext;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for handling contact form submissions.
 * 
 * Implements:
 * - GET /contacto: Display contact form
 * - POST /contacto: Process contact form submission with validation
 * 
 * Requirements: 14.1, 14.2, 14.3, 14.4, 14.5
 */
@Controller
public class ContactController {

    private static final Logger log = LoggerFactory.getLogger(ContactController.class);

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    /**
     * Display contact form.
     * 
     * @param model Model to add attributes
     * @return contact template
     */
    @GetMapping("/contacto")
    public String contactForm(Model model) {
        if (!model.containsAttribute("contactMessage")) {
            model.addAttribute("contactMessage", new ContactMessage());
        }
        return "contact";
    }

    /**
     * Process contact form submission.
     * 
     * Validates:
     * - nombre: required, max 100 characters
     * - email: required, valid email format
     * - telefono: optional, max 20 characters
     * - asunto: optional, max 200 characters
     * - mensaje: required, max 2000 characters
     * 
     * @param message Contact message with validation
     * @param result Binding result for validation errors
     * @param redirectAttributes Redirect attributes for flash messages
     * @return redirect to contact form
     */
    @PostMapping("/contacto")
    public String submitContact(
            @Valid @ModelAttribute("contactMessage") ContactMessage message,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        
        // If validation errors, return to form
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.contactMessage", result);
            redirectAttributes.addFlashAttribute("contactMessage", message);
            return "redirect:/contacto";
        }

        try {
            // Set tenant ID for multi-tenancy
            String tenantId = TenantContext.getCurrentTenant();
            if (tenantId == null) {
                tenantId = "default";
            }
            message.setTenantId(tenantId);

            // Save contact message
            contactService.save(message);

            log.info("Contact message received from: {} ({})", message.getNombre(), message.getEmail());

            // Show success message
            redirectAttributes.addFlashAttribute("success", 
                "¡Gracias por contactarnos! Hemos recibido tu mensaje y te responderemos pronto.");

        } catch (Exception e) {
            log.error("Error saving contact message", e);
            redirectAttributes.addFlashAttribute("error", 
                "Hubo un error al enviar tu mensaje. Por favor, intenta nuevamente.");
        }

        return "redirect:/contacto";
    }
}
