package com.alquiler.furent.controller.admin;

import com.alquiler.furent.service.ContactService;
import com.alquiler.furent.service.ReservationService;
import com.alquiler.furent.service.ReviewService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Provee atributos de modelo comunes a todos los controladores de admin.
 */
@ControllerAdvice(basePackages = "com.alquiler.furent.controller.admin")
public class AdminModelAdvice {

    private final ReservationService reservationService;
    private final ContactService contactService;
    private final ReviewService reviewService;

    public AdminModelAdvice(ReservationService reservationService, ContactService contactService, ReviewService reviewService) {
        this.reservationService = reservationService;
        this.contactService = contactService;
        this.reviewService = reviewService;
    }

    @ModelAttribute("pendientesCount")
    public long getPendientesCount() {
        return reservationService.countByEstado("PENDIENTE");
    }

    @ModelAttribute("mensajesNoLeidos")
    public long getMensajesNoLeidos() {
        return contactService.countUnread();
    }

    @ModelAttribute("notificacionesMensajesCount")
    public long getNotificacionesMensajesCount() {
        return contactService.countUnread() + reviewService.countUnanswered();
    }
}
