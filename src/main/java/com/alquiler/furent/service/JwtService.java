package com.alquiler.furent.service;

import com.alquiler.furent.model.RefreshToken;
import com.alquiler.furent.model.User;
import com.alquiler.furent.repository.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

/**
 * Servicio de gestión de JWT (JSON Web Tokens).
 * Genera access tokens, refresh tokens, valida y extrae claims.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${furent.jwt.secret:dGhpcyBpcyBhIHNlY3VyZSBrZXkgZm9yIGZ1cmVudCBzYWFzIHBsYXRmb3JtIDIwMjY=}")
    private String jwtSecret;

    @Value("${furent.jwt.access-token-expiration:3600000}")
    private long accessTokenExpiration; // 1 hora por defecto

    @Value("${furent.jwt.refresh-token-expiration:2592000000}")
    private long refreshTokenExpiration; // 30 días por defecto

    private final RefreshTokenRepository refreshTokenRepository;

    public JwtService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String generateAccessToken(User user, String tenantId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .claim("tenantId", tenantId)
                .claim("nombre", user.getNombreCompleto())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public RefreshToken generateRefreshToken(User user, String tenantId) {
        // Revocar tokens anteriores
        refreshTokenRepository.findByUserIdAndRevokedFalse(user.getId())
                .forEach(t -> {
                    t.setRevoked(true);
                    refreshTokenRepository.save(t);
                });

        String tokenStr = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);

        RefreshToken refreshToken = new RefreshToken(tokenStr, user.getId(), tenantId, expiresAt);
        return refreshTokenRepository.save(refreshToken);
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractUserId(String token) {
        return extractClaims(token).get("userId", String.class);
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public String extractTenantId(String token) {
        return extractClaims(token).get("tenantId", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenValid(String token, String email) {
        try {
            String tokenEmail = extractEmail(token);
            return email.equals(tokenEmail) && isTokenValid(token);
        } catch (Exception e) {
            return false;
        }
    }

    public RefreshToken validateRefreshToken(String tokenStr) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new RuntimeException("Refresh token no encontrado"));

        if (!refreshToken.isValid()) {
            throw new RuntimeException("Refresh token expirado o revocado");
        }

        return refreshToken;
    }

    public void revokeAllUserTokens(String userId) {
        refreshTokenRepository.findByUserIdAndRevokedFalse(userId)
                .forEach(t -> {
                    t.setRevoked(true);
                    refreshTokenRepository.save(t);
                });
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
