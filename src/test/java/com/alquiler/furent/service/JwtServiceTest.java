package com.alquiler.furent.service;

import com.alquiler.furent.model.RefreshToken;
import com.alquiler.furent.model.User;
import com.alquiler.furent.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private JwtService jwtService;
    private User testUser;

    // Base64 encoded 256-bit key for HMAC-SHA
    private static final String TEST_SECRET = "dGhpcyBpcyBhIHNlY3VyZSBrZXkgZm9yIGZ1cmVudCBzYWFzIHBsYXRmb3JtIDIwMjY=";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(refreshTokenRepository);
        ReflectionTestUtils.setField(jwtService, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 3600000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 2592000000L);

        testUser = new User();
        testUser.setId("user-001");
        testUser.setEmail("test@furent.com");
        testUser.setNombre("Test");
        testUser.setApellido("User");
        testUser.setRole("USER");
    }

    @Test
    void generateAccessToken_createsValidToken() {
        String token = jwtService.generateAccessToken(testUser, "tenant-1");

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String token = jwtService.generateAccessToken(testUser, "tenant-1");

        String email = jwtService.extractEmail(token);

        assertEquals("test@furent.com", email);
    }

    @Test
    void extractUserId_returnsCorrectUserId() {
        String token = jwtService.generateAccessToken(testUser, "tenant-1");

        String userId = jwtService.extractUserId(token);

        assertEquals("user-001", userId);
    }

    @Test
    void extractRole_returnsCorrectRole() {
        String token = jwtService.generateAccessToken(testUser, "tenant-1");

        String role = jwtService.extractRole(token);

        assertEquals("USER", role);
    }

    @Test
    void extractTenantId_returnsCorrectTenantId() {
        String token = jwtService.generateAccessToken(testUser, "tenant-1");

        String tenantId = jwtService.extractTenantId(token);

        assertEquals("tenant-1", tenantId);
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtService.generateAccessToken(testUser, "tenant-1");

        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_invalidToken_returnsFalse() {
        assertFalse(jwtService.isTokenValid("invalid.token.here"));
    }

    @Test
    void isTokenValid_withEmail_matchingEmail_returnsTrue() {
        String token = jwtService.generateAccessToken(testUser, "tenant-1");

        assertTrue(jwtService.isTokenValid(token, "test@furent.com"));
    }

    @Test
    void isTokenValid_withEmail_wrongEmail_returnsFalse() {
        String token = jwtService.generateAccessToken(testUser, "tenant-1");

        assertFalse(jwtService.isTokenValid(token, "other@furent.com"));
    }

    @Test
    void generateRefreshToken_revokesExistingAndCreatesNew() {
        RefreshToken oldToken = new RefreshToken("old-token", "user-001", "t-1", LocalDateTime.now().plusDays(30));
        when(refreshTokenRepository.findByUserIdAndRevokedFalse("user-001"))
                .thenReturn(List.of(oldToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken result = jwtService.generateRefreshToken(testUser, "tenant-1");

        assertNotNull(result);
        assertEquals("user-001", result.getUserId());
        assertEquals("tenant-1", result.getTenantId());
        assertTrue(oldToken.isRevoked());
        // save called: once for revoking old, once for new
        verify(refreshTokenRepository, atLeast(2)).save(any(RefreshToken.class));
    }

    @Test
    void validateRefreshToken_validToken_returnsToken() {
        RefreshToken token = new RefreshToken("tok-abc", "user-001", "t-1", LocalDateTime.now().plusDays(30));
        when(refreshTokenRepository.findByToken("tok-abc")).thenReturn(Optional.of(token));

        RefreshToken result = jwtService.validateRefreshToken("tok-abc");

        assertNotNull(result);
        assertEquals("tok-abc", result.getToken());
    }

    @Test
    void validateRefreshToken_notFound_throwsRuntime() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> jwtService.validateRefreshToken("missing"));
    }

    @Test
    void validateRefreshToken_expired_throwsRuntime() {
        RefreshToken expired = new RefreshToken("tok-exp", "user-001", "t-1", LocalDateTime.now().minusDays(1));
        when(refreshTokenRepository.findByToken("tok-exp")).thenReturn(Optional.of(expired));

        assertThrows(RuntimeException.class, () -> jwtService.validateRefreshToken("tok-exp"));
    }

    @Test
    void validateRefreshToken_revoked_throwsRuntime() {
        RefreshToken revoked = new RefreshToken("tok-rev", "user-001", "t-1", LocalDateTime.now().plusDays(30));
        revoked.setRevoked(true);
        when(refreshTokenRepository.findByToken("tok-rev")).thenReturn(Optional.of(revoked));

        assertThrows(RuntimeException.class, () -> jwtService.validateRefreshToken("tok-rev"));
    }

    @Test
    void revokeAllUserTokens_revokesAll() {
        RefreshToken t1 = new RefreshToken("t1", "user-001", "t-1", LocalDateTime.now().plusDays(30));
        RefreshToken t2 = new RefreshToken("t2", "user-001", "t-1", LocalDateTime.now().plusDays(30));
        when(refreshTokenRepository.findByUserIdAndRevokedFalse("user-001")).thenReturn(List.of(t1, t2));

        jwtService.revokeAllUserTokens("user-001");

        assertTrue(t1.isRevoked());
        assertTrue(t2.isRevoked());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void revokeAllUserTokens_noTokens_doesNothing() {
        when(refreshTokenRepository.findByUserIdAndRevokedFalse("user-999"))
                .thenReturn(Collections.emptyList());

        jwtService.revokeAllUserTokens("user-999");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void getAccessTokenExpiration_returnsConfiguredValue() {
        assertEquals(3600000L, jwtService.getAccessTokenExpiration());
    }
}
