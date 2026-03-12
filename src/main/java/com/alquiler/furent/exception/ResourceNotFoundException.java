package com.alquiler.furent.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String entity, String id) {
        super(String.format("%s con ID '%s' no encontrado", entity, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
