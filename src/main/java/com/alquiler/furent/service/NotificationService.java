package com.alquiler.furent.service;

import com.alquiler.furent.model.Notification;
import com.alquiler.furent.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio de notificaciones en tiempo real para usuarios.
 * Gestiona creación, lectura y marcado de notificaciones persistidas en MongoDB.
 *
 * @author Furent Team
 * @since 1.0
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @CacheEvict(value = "notifications", key = "#userId")
    public Notification notify(String userId, String titulo, String mensaje, String tipo, String link) {
        Notification notif = new Notification(userId, titulo, mensaje, tipo, link);
        log.debug("Notificación creada para usuario {}: {}", userId, titulo);
        return notificationRepository.save(notif);
    }

    public List<Notification> getRecentNotifications(String userId) {
        return notificationRepository.findTop10ByUserIdOrderByFechaDesc(userId);
    }

    @Cacheable(value = "notifications", key = "#userId + ':unread'")
    public List<Notification> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndLeidaFalseOrderByFechaDesc(userId);
    }

    public long countUnread(String userId) {
        return notificationRepository.countByUserIdAndLeidaFalse(userId);
    }

    @CacheEvict(value = "notifications", allEntries = true)
    public void markAsRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setLeida(true);
            notificationRepository.save(n);
        });
    }

    @CacheEvict(value = "notifications", key = "#userId")
    public void markAllAsRead(String userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndLeidaFalseOrderByFechaDesc(userId);
        unread.forEach(n -> n.setLeida(true));
        notificationRepository.saveAll(unread);
    }
}
