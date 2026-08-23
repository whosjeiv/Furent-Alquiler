package com.alquiler.furent.service;

import com.alquiler.furent.model.Notification;
import com.alquiler.furent.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @InjectMocks private NotificationService notificationService;

    @Test
    void notify_createsAndSavesNotification() {
        Notification expected = new Notification("user-1", "Titulo", "Mensaje", "INFO", "/link");
        when(notificationRepository.save(any(Notification.class))).thenReturn(expected);

        Notification result = notificationService.notify("user-1", "Titulo", "Mensaje", "INFO", "/link");

        assertNotNull(result);
        assertEquals("Titulo", result.getTitulo());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void getRecentNotifications_returnsTopTen() {
        Notification n1 = new Notification("user-1", "T1", "M1", "INFO", "/l1");
        when(notificationRepository.findTop10ByUserIdOrderByFechaDesc("user-1"))
                .thenReturn(List.of(n1));

        List<Notification> result = notificationService.getRecentNotifications("user-1");

        assertEquals(1, result.size());
        verify(notificationRepository).findTop10ByUserIdOrderByFechaDesc("user-1");
    }

    @Test
    void getUnreadNotifications_returnsUnread() {
        when(notificationRepository.findByUserIdAndLeidaFalseOrderByFechaDesc("user-1"))
                .thenReturn(List.of());

        List<Notification> result = notificationService.getUnreadNotifications("user-1");

        assertTrue(result.isEmpty());
    }

    @Test
    void countUnread_delegatesToRepository() {
        when(notificationRepository.countByUserIdAndLeidaFalse("user-1")).thenReturn(5L);

        long count = notificationService.countUnread("user-1");

        assertEquals(5, count);
    }

    @Test
    void markAsRead_existingNotification_marksAsRead() {
        Notification notif = new Notification("user-1", "T", "M", "INFO", "/l");
        notif.setLeida(false);
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(notif));

        notificationService.markAsRead("notif-1");

        assertTrue(notif.isLeida());
        verify(notificationRepository).save(notif);
    }

    @Test
    void markAsRead_nonExisting_doesNothing() {
        when(notificationRepository.findById("xxx")).thenReturn(Optional.empty());

        notificationService.markAsRead("xxx");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAllAsRead_marksAllUnreadForUser() {
        Notification n1 = new Notification("user-1", "T1", "M1", "INFO", "/l1");
        n1.setLeida(false);
        Notification n2 = new Notification("user-1", "T2", "M2", "INFO", "/l2");
        n2.setLeida(false);

        when(notificationRepository.findByUserIdAndLeidaFalseOrderByFechaDesc("user-1"))
                .thenReturn(List.of(n1, n2));

        notificationService.markAllAsRead("user-1");

        assertTrue(n1.isLeida());
        assertTrue(n2.isLeida());
        verify(notificationRepository).saveAll(List.of(n1, n2));
    }
}
