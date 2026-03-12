package com.alquiler.furent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades personalizadas del proyecto Furent.
 * Registra los prefijos {@code furent.*} para eliminar advertencias del IDE.
 */
@ConfigurationProperties(prefix = "furent")
public class FurentProperties {

    private Admin admin = new Admin();
    private Email email = new Email();

    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }

    public Email getEmail() { return email; }
    public void setEmail(Email email) { this.email = email; }

    public static class Admin {
        private String password = "admin123";

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class Email {
        private boolean enabled = false;
        private String from = "noreply@furent.com";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
    }
}
