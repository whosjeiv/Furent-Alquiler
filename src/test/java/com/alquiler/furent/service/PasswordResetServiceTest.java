package com.alquiler.furent.service;

import com.alquiler.furent.exception.InvalidOperationException;
import com.alquiler.furent.exception.ResourceNotFoundException;
import com.alquiler.furent.model.PasswordResetToken;
import com.alquiler.furent.model.User;
import com.alquiler.furent.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private UserService userService;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private PasswordResetService passwordResetService;

    @Test
    void createToken_validUser_createsToken() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("test@test.com");
        when(userService.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findByUserIdAndUsedFalse("user-1")).thenReturn(Optional.empty());
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(i -> i.getArgument(0));

        PasswordResetToken result = passwordResetService.createToken("test@test.com");

        assertNotNull(result);
        assertEquals("user-1", result.getUserId());
        assertNotNull(result.getToken());
        assertFalse(result.isUsed());
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void createToken_nonExistentUser_throwsResourceNotFound() {
        when(userService.findByEmail("noone@test.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> passwordResetService.createToken("noone@test.com"));
    }

    @Test
    void createToken_invalidatesPreviousToken() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("test@test.com");
        PasswordResetToken oldToken = new PasswordResetToken("user-1");
        oldToken.setUsed(false);

        when(userService.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findByUserIdAndUsedFalse("user-1")).thenReturn(Optional.of(oldToken));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(i -> i.getArgument(0));

        passwordResetService.createToken("test@test.com");

        assertTrue(oldToken.isUsed());
        // save called twice: once for old token, once for new
        verify(tokenRepository, times(2)).save(any(PasswordResetToken.class));
    }

    @Test
    void resetPassword_validToken_changesPassword() {
        PasswordResetToken token = new PasswordResetToken("user-1");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setUsed(false);

        User user = new User();
        user.setId("user-1");
        user.setEmail("test@test.com");

        when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));
        when(userService.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass123")).thenReturn("encoded");

        passwordResetService.resetPassword(token.getToken(), "newPass123");

        assertEquals("encoded", user.getPassword());
        assertTrue(token.isUsed());
        verify(userService).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void resetPassword_tokenNotFound_throwsResourceNotFound() {
        when(tokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> passwordResetService.resetPassword("bad-token", "newPass123"));
    }

    @Test
    void resetPassword_expiredToken_throwsInvalidOperation() {
        PasswordResetToken token = new PasswordResetToken("user-1");
        token.setExpiresAt(LocalDateTime.now().minusHours(2));
        token.setUsed(false);

        when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        assertThrows(InvalidOperationException.class,
                () -> passwordResetService.resetPassword(token.getToken(), "newPass123"));
    }

    @Test
    void resetPassword_usedToken_throwsInvalidOperation() {
        PasswordResetToken token = new PasswordResetToken("user-1");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setUsed(true);

        when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        assertThrows(InvalidOperationException.class,
                () -> passwordResetService.resetPassword(token.getToken(), "newPass123"));
    }

    @Test
    void resetPassword_shortPassword_throwsInvalidOperation() {
        PasswordResetToken token = new PasswordResetToken("user-1");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setUsed(false);

        when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        assertThrows(InvalidOperationException.class,
                () -> passwordResetService.resetPassword(token.getToken(), "abc"));
    }

    @Test
    void findByToken_delegatesToRepository() {
        PasswordResetToken token = new PasswordResetToken("user-1");
        when(tokenRepository.findByToken("tok-1")).thenReturn(Optional.of(token));

        Optional<PasswordResetToken> result = passwordResetService.findByToken("tok-1");

        assertTrue(result.isPresent());
    }
}
