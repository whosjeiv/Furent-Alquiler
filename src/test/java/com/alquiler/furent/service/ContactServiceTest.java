package com.alquiler.furent.service;

import com.alquiler.furent.model.ContactMessage;
import com.alquiler.furent.repository.ContactMessageRepository;
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
class ContactServiceTest {

    @Mock private ContactMessageRepository contactMessageRepository;
    @InjectMocks private ContactService contactService;

    @Test
    void save_shouldPersistMessage() {
        ContactMessage msg = new ContactMessage();
        msg.setEmail("test@furent.com");
        msg.setNombre("Juan");
        msg.setAsunto("Consulta");
        msg.setMensaje("Necesito información");

        when(contactMessageRepository.save(any(ContactMessage.class))).thenReturn(msg);

        ContactMessage result = contactService.save(msg);

        assertNotNull(result);
        assertEquals("test@furent.com", result.getEmail());
        verify(contactMessageRepository).save(msg);
    }

    @Test
    void getAll_returnsAllMessages() {
        when(contactMessageRepository.findAllByOrderByFechaCreacionDesc())
                .thenReturn(List.of(new ContactMessage(), new ContactMessage()));

        List<ContactMessage> result = contactService.getAll();

        assertEquals(2, result.size());
    }

    @Test
    void getUnread_returnsUnreadOnly() {
        when(contactMessageRepository.findByLeidoFalseOrderByFechaCreacionDesc())
                .thenReturn(List.of(new ContactMessage()));

        List<ContactMessage> result = contactService.getUnread();

        assertEquals(1, result.size());
    }

    @Test
    void countUnread_returnsCorrectCount() {
        when(contactMessageRepository.countByLeidoFalse()).thenReturn(3L);

        long count = contactService.countUnread();

        assertEquals(3, count);
    }

    @Test
    void getById_existingId_returnsMessage() {
        ContactMessage msg = new ContactMessage();
        msg.setNombre("Test");
        when(contactMessageRepository.findById("msg-1")).thenReturn(Optional.of(msg));

        Optional<ContactMessage> result = contactService.getById("msg-1");

        assertTrue(result.isPresent());
        assertEquals("Test", result.get().getNombre());
    }

    @Test
    void getById_nonExisting_returnsEmpty() {
        when(contactMessageRepository.findById("xxx")).thenReturn(Optional.empty());

        Optional<ContactMessage> result = contactService.getById("xxx");

        assertTrue(result.isEmpty());
    }

    @Test
    void markAsRead_existingMessage_setsReadFlag() {
        ContactMessage msg = new ContactMessage();
        msg.setLeido(false);
        when(contactMessageRepository.findById("msg-1")).thenReturn(Optional.of(msg));

        contactService.markAsRead("msg-1");

        assertTrue(msg.isLeido());
        verify(contactMessageRepository).save(msg);
    }

    @Test
    void markAsRead_nonExistingMessage_doesNothing() {
        when(contactMessageRepository.findById("xxx")).thenReturn(Optional.empty());

        contactService.markAsRead("xxx");

        verify(contactMessageRepository, never()).save(any());
    }

    @Test
    void delete_delegatesToRepository() {
        contactService.delete("msg-1");

        verify(contactMessageRepository).deleteById("msg-1");
    }
}
