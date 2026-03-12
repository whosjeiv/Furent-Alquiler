package com.alquiler.furent.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PasswordResetTokenTest {

    @Test
    void constructor_setsDefaults() {
        PasswordResetToken token = new PasswordResetToken();

        assertNotNull(token.getToken());
        assertNotNull(token.getExpiresAt());
        assertFalse(token.isUsed());
    }

    @Test
    void constructor_withUserId_setsUserId() {
        PasswordResetToken token = new PasswordResetToken("user-1");

        assertEquals("user-1", token.getUserId());
        assertNotNull(token.getToken());
        assertFalse(token.isUsed());
    }

    @Test
    void isValid_freshToken_returnsTrue() {
        PasswordResetToken token = new PasswordResetToken("user-1");

        assertTrue(token.isValid());
    }

    @Test
    void isValid_expiredToken_returnsFalse() {
        PasswordResetToken token = new PasswordResetToken("user-1");
        token.setExpiresAt(LocalDateTime.now().minusHours(2));

        assertFalse(token.isValid());
    }

    @Test
    void isValid_usedToken_returnsFalse() {
        PasswordResetToken token = new PasswordResetToken("user-1");
        token.setUsed(true);

        assertFalse(token.isValid());
    }

    @Test
    void isExpired_futureDate_returnsFalse() {
        PasswordResetToken token = new PasswordResetToken("user-1");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));

        assertFalse(token.isExpired());
    }

    @Test
    void isExpired_pastDate_returnsTrue() {
        PasswordResetToken token = new PasswordResetToken("user-1");
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        assertTrue(token.isExpired());
    }
}
