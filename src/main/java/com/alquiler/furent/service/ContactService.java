package com.alquiler.furent.service;

import com.alquiler.furent.model.ContactMessage;
import com.alquiler.furent.repository.ContactMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    public List<ContactMessage> findUnread() {
        return getUnread();
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
    
    public Page<ContactMessage> findAll(String estado, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        
        if ("NO_LEIDO".equalsIgnoreCase(estado)) {
            return contactMessageRepository.findByLeido(false, pageable);
        } else if ("LEIDO".equalsIgnoreCase(estado)) {
            return contactMessageRepository.findByLeido(true, pageable);
        } else {
            return contactMessageRepository.findAll(pageable);
        }
    }
}
