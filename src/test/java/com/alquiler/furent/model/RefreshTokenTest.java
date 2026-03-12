package com.alquiler.furent.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenTest {

    @Test
    void constructor_setsDefaults() {
        RefreshToken token = new RefreshToken();

        assertNotNull(token.getCreatedAt());
        assertFalse(token.isRevoked());
    }

    @Test
    void constructor_withArgs_setsAllFields() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);
        RefreshToken token = new RefreshToken("tok-123", "user-1", "tenant-1", expiresAt);

        assertEquals("tok-123", token.getToken());
        assertEquals("user-1", token.getUserId());
        assertEquals("tenant-1", token.getTenantId());
        assertEquals(expiresAt, token.getExpiresAt());
        assertFalse(token.isRevoked());
    }

    @Test
    void isValid_freshToken_returnsTrue() {
        RefreshToken token = new RefreshToken("tok", "u", "t", LocalDateTime.now().plusDays(30));

        assertTrue(token.isValid());
    }

    @Test
    void isValid_expiredToken_returnsFalse() {
        RefreshToken token = new RefreshToken("tok", "u", "t", LocalDateTime.now().minusDays(1));

        assertFalse(token.isValid());
    }

    @Test
    void isValid_revokedToken_returnsFalse() {
        RefreshToken token = new RefreshToken("tok", "u", "t", LocalDateTime.now().plusDays(30));
        token.setRevoked(true);

        assertFalse(token.isValid());
    }

    @Test
    void isValid_expiredAndRevoked_returnsFalse() {
        RefreshToken token = new RefreshToken("tok", "u", "t", LocalDateTime.now().minusDays(1));
        token.setRevoked(true);

        assertFalse(token.isValid());
    }
}
