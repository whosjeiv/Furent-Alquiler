package com.alquiler.furent.service;

import com.alquiler.furent.model.ContactMessage;
import com.alquiler.furent.repository.ContactMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactMessageRepository contactMessageRepository;

    @InjectMocks
    private ContactService contactService;

    private ContactMessage testMessage;

    @BeforeEach
    void setUp() {
        testMessage = new ContactMessage();
        testMessage.setId("msg1");
        testMessage.setNombre("Juan Pérez");
        testMessage.setEmail("juan@example.com");
        testMessage.setMensaje("Consulta sobre productos");
        testMessage.setLeido(false);
        testMessage.setTenantId("tenant1");
    }

    @Test
    void save_ShouldSaveMessage() {
        // Arrange
        when(contactMessageRepository.save(any(ContactMessage.class))).thenReturn(testMessage);
        
        // Act
        ContactMessage result = contactService.save(testMessage);
        
        // Assert
        assertNotNull(result);
        verify(contactMessageRepository).save(testMessage);
    }

    @Test
    void findUnread_ShouldReturnUnreadMessages() {
        // Arrange
        List<ContactMessage> unreadMessages = Arrays.asList(testMessage);
        when(contactMessageRepository.findByLeidoFalseOrderByFechaCreacionDesc()).thenReturn(unreadMessages);
        
        // Act
        List<ContactMessage> result = contactService.findUnread();
        
        // Assert
        assertEquals(1, result.size());
        assertFalse(result.get(0).isLeido());
        verify(contactMessageRepository).findByLeidoFalseOrderByFechaCreacionDesc();
    }

    @Test
    void markAsRead_ShouldUpdateMessageStatus() {
        // Arrange
        when(contactMessageRepository.findById("msg1")).thenReturn(Optional.of(testMessage));
        when(contactMessageRepository.save(any(ContactMessage.class))).thenReturn(testMessage);
        
        // Act
        contactService.markAsRead("msg1");
        
        // Assert
        assertTrue(testMessage.isLeido());
        verify(contactMessageRepository).findById("msg1");
        verify(contactMessageRepository).save(testMessage);
    }

    @Test
    void markAsRead_WithNonExistentMessage_ShouldNotThrowException() {
        // Arrange
        when(contactMessageRepository.findById("nonexistent")).thenReturn(Optional.empty());
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> contactService.markAsRead("nonexistent"));
        verify(contactMessageRepository).findById("nonexistent");
        verify(contactMessageRepository, never()).save(any());
    }

    @Test
    void countUnread_ShouldReturnCorrectCount() {
        // Arrange
        when(contactMessageRepository.countByLeidoFalse()).thenReturn(5L);
        
        // Act
        long count = contactService.countUnread();
        
        // Assert
        assertEquals(5L, count);
        verify(contactMessageRepository).countByLeidoFalse();
    }

    @Test
    void findAll_WithoutFilter_ShouldReturnAllMessages() {
        // Arrange
        List<ContactMessage> messages = Arrays.asList(testMessage);
        Page<ContactMessage> page = new PageImpl<>(messages);
        
        when(contactMessageRepository.findAll(any(Pageable.class))).thenReturn(page);
        
        // Act
        Page<ContactMessage> result = contactService.findAll(null, 0, 10);
        
        // Assert
        assertEquals(1, result.getContent().size());
        verify(contactMessageRepository).findAll(any(Pageable.class));
    }

    @Test
    void findAll_WithLeidoFilter_ShouldReturnFilteredMessages() {
        // Arrange
        List<ContactMessage> messages = Arrays.asList(testMessage);
        Page<ContactMessage> page = new PageImpl<>(messages);
        
        when(contactMessageRepository.findByLeido(eq(true), any(Pageable.class))).thenReturn(page);
        
        // Act
        Page<ContactMessage> result = contactService.findAll("LEIDO", 0, 10);
        
        // Assert
        assertEquals(1, result.getContent().size());
        verify(contactMessageRepository).findByLeido(eq(true), any(Pageable.class));
    }

    @Test
    void findAll_WithNoLeidoFilter_ShouldReturnUnreadMessages() {
        // Arrange
        List<ContactMessage> messages = Arrays.asList(testMessage);
        Page<ContactMessage> page = new PageImpl<>(messages);
        
        when(contactMessageRepository.findByLeido(eq(false), any(Pageable.class))).thenReturn(page);
        
        // Act
        Page<ContactMessage> result = contactService.findAll("NO_LEIDO", 0, 10);
        
        // Assert
        assertEquals(1, result.getContent().size());
        verify(contactMessageRepository).findByLeido(eq(false), any(Pageable.class));
    }

    @Test
    void findAll_WithEmptyFilter_ShouldReturnAllMessages() {
        // Arrange
        List<ContactMessage> messages = Arrays.asList(testMessage);
        Page<ContactMessage> page = new PageImpl<>(messages);
        
        when(contactMessageRepository.findAll(any(Pageable.class))).thenReturn(page);
        
        // Act
        Page<ContactMessage> result = contactService.findAll("", 0, 10);
        
        // Assert
        assertEquals(1, result.getContent().size());
        verify(contactMessageRepository).findAll(any(Pageable.class));
    }
}
