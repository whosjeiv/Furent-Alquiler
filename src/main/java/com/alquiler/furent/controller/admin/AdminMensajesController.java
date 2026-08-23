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
    private final com.alquiler.furent.service.ReviewService reviewService;
    private final com.alquiler.furent.repository.ProductRepository productRepository;

    public AdminMensajesController(ContactService contactService, 
                                 com.alquiler.furent.service.ReviewService reviewService,
                                 com.alquiler.furent.repository.ProductRepository productRepository) {
        this.contactService = contactService;
        this.reviewService = reviewService;
        this.productRepository = productRepository;
    }

    @GetMapping("/mensajes")
    public String listMessagesAndReviews(Model model) {
        model.addAttribute("mensajes", contactService.getAll());
        model.addAttribute("noLeidos", contactService.countUnread());
        
        var reviews = reviewService.getAllReviews();
        model.addAttribute("resenas", reviews);
        
        // Map product IDs to names for the UI
        java.util.Map<String, String> productNames = new java.util.HashMap<>();
        for (com.alquiler.furent.model.Review r : reviews) {
            if (r.getProductId() != null && !productNames.containsKey(r.getProductId())) {
                productRepository.findById(r.getProductId())
                    .ifPresent(p -> productNames.put(r.getProductId(), p.getNombre()));
            }
        }
        model.addAttribute("productNames", productNames);
        
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

    @PostMapping("/resenas/eliminar/{id}")
    public String deleteReview(@PathVariable String id, RedirectAttributes redirectAttributes) {
        reviewService.deleteReview(id);
        redirectAttributes.addFlashAttribute("success", "Reseña eliminada");
        return "redirect:/admin/mensajes";
    }

    @PostMapping("/resenas/responder/{id}")
    public String respondToReview(@PathVariable String id, @RequestParam String respuesta, RedirectAttributes redirectAttributes) {
        reviewService.respondToReview(id, respuesta);
        redirectAttributes.addFlashAttribute("success", "Respuesta enviada");
        return "redirect:/admin/mensajes";
    }
}
