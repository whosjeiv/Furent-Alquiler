package com.alquiler.furent.dto;

import com.alquiler.furent.model.User;

public record UserResponse(
    String id,
    String email,
    String nombre,
    String apellido,
    String role,
    boolean activo
) {
    public static UserResponse from(User u) {
        return new UserResponse(
            u.getId(), u.getEmail(), u.getNombre(),
            u.getApellido(), u.getRole(), u.isActivo()
        );
    }
}
