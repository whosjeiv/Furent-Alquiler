package com.alquiler.furent.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    UserResponse user,
    String tenantId
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn,
                                   UserResponse user, String tenantId) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user, tenantId);
    }
}
