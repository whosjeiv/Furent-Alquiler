package com.alquiler.furent.enums;

public enum MetodoPago {
    TRANSFERENCIA("Transferencia Bancaria"),
    NEQUI("Nequi"),
    DAVIPLATA("Daviplata"),
    EFECTIVO("Efectivo"),
    TARJETA("Tarjeta de Crédito/Débito");

    private final String displayName;

    MetodoPago(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
