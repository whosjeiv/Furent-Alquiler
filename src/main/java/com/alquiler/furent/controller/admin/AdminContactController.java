package com.alquiler.furent.controller.admin;

import com.alquiler.furent.model.ContactMessage;
import com.alquiler.furent.service.ContactService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/mensajes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContactController {

    private final ContactService contactService;

    public AdminContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    public String listMessages(@RequestParam(required = false) String estado,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {
        int size = 20;
        Page<ContactMessage> mensajesPage = contactService.findAll(estado, page, size);
        
        model.addAttribute("mensajes", mensajesPage);
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("unreadCount", contactService.countUnread());
        
        return "admin/mensajes";
    }

    @PostMapping("/{id}/leer")
    public String markAsRead(@PathVariable String id,
                            RedirectAttributes redirectAttributes) {
        try {
            contactService.markAsRead(id);
            redirectAttributes.addFlashAttribute("success", "Mensaje marcado como leído");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al marcar mensaje: " + e.getMessage());
        }
        return "redirect:/admin/mensajes";
    }
}
