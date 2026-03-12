package com.alquiler.furent.service;

import com.alquiler.furent.model.ContactMessage;
import com.alquiler.furent.repository.ContactMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de gestión de mensajes de contacto.
 * Administra el buzón de mensajes recibidos desde el formulario público,
 * con soporte para lectura, conteo de no leídos y eliminación.
 *
 * @author Furent Team
 * @since 1.0
 */
@Service
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    private final ContactMessageRepository contactMessageRepository;

    public ContactService(ContactMessageRepository contactMessageRepository) {
        this.contactMessageRepository = contactMessageRepository;
    }

    public ContactMessage save(ContactMessage message) {
        log.info("Nuevo mensaje de contacto de: {}", message.getEmail());
        return contactMessageRepository.save(message);
    }

    public List<ContactMessage> getAll() {
        return contactMessageRepository.findAllByOrderByFechaCreacionDesc();
    }

    public List<ContactMessage> getUnread() {
        return contactMessageRepository.findByLeidoFalseOrderByFechaCreacionDesc();
    }

    public long countUnread() {
        return contactMessageRepository.countByLeidoFalse();
    }

    public Optional<ContactMessage> getById(String id) {
        return contactMessageRepository.findById(id);
    }

    public void markAsRead(String id) {
        contactMessageRepository.findById(id).ifPresent(m -> {
            m.setLeido(true);
            contactMessageRepository.save(m);
        });
    }

    public void delete(String id) {
        contactMessageRepository.deleteById(id);
    }
}
