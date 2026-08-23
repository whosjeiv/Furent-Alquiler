package com.alquiler.furent.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoHandler(NoHandlerFoundException ex, Model model) {
        log.warn("Página no encontrada: {}", ex.getRequestURL());
        model.addAttribute("error", "La página que buscas no existe");
        return "error/404";
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        model.addAttribute("error", "No tienes permiso para acceder a esta página");
        return "error/403";
    }

    @ExceptionHandler(AccountSuspendedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleSuspended(AccountSuspendedException ex, Model model) {
        log.warn("Intento de acceso de cuenta suspendida: {}", ex.getMessage());
        model.addAttribute("reason", ex.getReason());
        model.addAttribute("duration", ex.getDuration());
        model.addAttribute("permanent", ex.isPermanent());
        return "error/suspended";
    }

    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleDuplicate(DuplicateResourceException ex, Model model) {
        log.warn("Recurso duplicado: {}", ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "error/500";
    }

    @ExceptionHandler(InvalidOperationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInvalidOperation(InvalidOperationException ex, Model model) {
        log.warn("Operación inválida: {}", ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "error/500";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception ex, Model model) {
        log.error("Error inesperado", ex);
        model.addAttribute("error", "Ha ocurrido un error inesperado. Por favor, intenta de nuevo.");
        return "error/500";
    }
}
