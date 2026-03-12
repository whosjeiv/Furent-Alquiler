package com.alquiler.furent.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object de dominio para direcciones de email.
 * Encapsula la validación y normalización del email.
 * Principio DDD: tipos fuertes para conceptos del dominio.
 */
public final class Email {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final String value;

    private Email(String value) {
        this.value = value.toLowerCase().trim();
    }

    public static Email of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El email no puede estar vacío");
        }
        String normalized = value.toLowerCase().trim();
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Formato de email inválido: " + value);
        }
        return new Email(normalized);
    }

    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) return false;
        return EMAIL_PATTERN.matcher(value.toLowerCase().trim()).matches();
    }

    public String getValue() {
        return value;
    }

    public String getDomain() {
        return value.substring(value.indexOf('@') + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return Objects.equals(value, email.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
