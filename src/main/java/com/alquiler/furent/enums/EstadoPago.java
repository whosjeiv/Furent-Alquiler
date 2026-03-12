package com.alquiler.furent.enums;

public enum EstadoPago {
    PENDIENTE("Pendiente"),
    PAGADO("Pagado"),
    FALLIDO("Fallido"),
    REEMBOLSADO("Reembolsado");

    private final String displayName;

    EstadoPago(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
