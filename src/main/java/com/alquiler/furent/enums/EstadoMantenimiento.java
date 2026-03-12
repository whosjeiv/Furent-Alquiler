package com.alquiler.furent.enums;

public enum EstadoMantenimiento {
    EXCELENTE("Excelente"),
    BUENO("Bueno"),
    REGULAR("Regular"),
    EN_REPARACION("En Reparación");

    private final String displayName;

    EstadoMantenimiento(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
