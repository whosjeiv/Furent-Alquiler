package com.alquiler.furent.controller.admin;

import com.alquiler.furent.service.ContactService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminMensajesController {

    private final ContactService contactService;

    public AdminMensajesController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping("/mensajes")
    public String listMessages(Model model) {
        model.addAttribute("mensajes", contactService.getAll());
        model.addAttribute("noLeidos", contactService.countUnread());
        return "admin/mensajes";
    }

    @PostMapping("/mensajes/leer/{id}")
    public String markMessageRead(@PathVariable String id, RedirectAttributes redirectAttributes) {
        contactService.markAsRead(id);
        return "redirect:/admin/mensajes";
    }

    @PostMapping("/mensajes/eliminar/{id}")
    public String deleteMessage(@PathVariable String id, RedirectAttributes redirectAttributes) {
        contactService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Mensaje eliminado");
        return "redirect:/admin/mensajes";
    }
}
