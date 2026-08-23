package com.alquiler.furent.config;

import com.alquiler.furent.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationAuditListener {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationAuditListener.class);
    private final AuditLogService auditLogService;

    public AuthenticationAuditListener(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String email = event.getAuthentication().getName();
        log.info("Login exitoso: {}", email);
        auditLogService.log(email, "LOGIN_EXITOSO", "SESION", null, "Inicio de sesión exitoso");
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String email = event.getAuthentication().getName();
        log.warn("Login fallido: {}", email);
        auditLogService.log(email, "LOGIN_FALLIDO", "SESION", null, "Intento de inicio de sesión fallido");
    }

    @EventListener
    public void onLogoutSuccess(LogoutSuccessEvent event) {
        if (event.getAuthentication() != null) {
            String email = event.getAuthentication().getName();
            log.info("Logout: {}", email);
            auditLogService.log(email, "LOGOUT", "SESION", null, "Cierre de sesión");
        }
    }
}
