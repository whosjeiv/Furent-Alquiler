package com.alquiler.furent.enums;

public enum RolUsuario {
    USER("Usuario"),
    MANAGER("Gerente"),
    ADMIN("Administrador"),
    SUPER_ADMIN("Super Administrador");

    private final String displayName;

    RolUsuario(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
