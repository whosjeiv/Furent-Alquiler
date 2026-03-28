package com.alquiler.furent.service;

import com.alquiler.furent.config.MetricsConfig;
import com.alquiler.furent.event.EventPublisher;
import com.alquiler.furent.exception.AccountSuspendedException;
import com.alquiler.furent.exception.DuplicateResourceException;
import com.alquiler.furent.model.User;
import com.alquiler.furent.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EventPublisher eventPublisher;
    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private MetricsConfig metricsConfig;
    @InjectMocks
    private UserService userService;

    @Test
    void register_withNewEmail_shouldCreateUser() {
        when(userRepository.existsByEmail("nuevo@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        User saved = new User("nuevo@gmail.com", "encoded", "Juan", "Test", "3001234567", "USER");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = userService.register("nuevo@gmail.com", "password123", "Juan", "Test", "3001234567");

        assertNotNull(result);
        assertEquals("nuevo@gmail.com", result.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_withExistingEmail_shouldThrowDuplicate() {
        when(userRepository.existsByEmail("existe@gmail.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () ->
                userService.register("existe@gmail.com", "Password123!", "A", "B", "123"));
    }

    @Test
    void loadUserByUsername_withActiveUser_shouldReturnUserDetails() {
        User user = new User();
        user.setEmail("activo@test.com");
        user.setPassword("encoded");
        user.setRole("USER");
        user.setActivo(true);
        when(userRepository.findByEmail("activo@test.com")).thenReturn(Optional.of(user));

        UserDetails details = userService.loadUserByUsername("activo@test.com");

        assertEquals("activo@test.com", details.getUsername());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void loadUserByUsername_withNonExistentEmail_shouldThrow() {
        when(userRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
                userService.loadUserByUsername("noexiste@test.com"));
    }

    @Test
    void loadUserByUsername_withSuspendedUser_shouldThrow() {
        User user = new User();
        user.setEmail("suspendido@test.com");
        user.setPassword("encoded");
        user.setRole("USER");
        user.setActivo(false);
        user.setRazonSuspension("Violación de términos");
        when(userRepository.findByEmail("suspendido@test.com")).thenReturn(Optional.of(user));

        assertThrows(AccountSuspendedException.class, () ->
                userService.loadUserByUsername("suspendido@test.com"));
    }

    @Test
    void findByEmail_shouldDelegateToRepository() {
        User user = new User();
        user.setEmail("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail("test@test.com");

        assertTrue(result.isPresent());
        assertEquals("test@test.com", result.get().getEmail());
    }

    @Test
    void findByEmail_notFound_shouldReturnEmpty() {
        when(userRepository.findByEmail("nada@test.com")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByEmail("nada@test.com");

        assertTrue(result.isEmpty());
    }
}
