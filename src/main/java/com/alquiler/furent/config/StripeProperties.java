package com.alquiler.furent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración de Stripe para la pasarela de pago.
 * Clave secreta, pública y webhook secret desde variables de entorno o application.properties.
 */
@Component
@ConfigurationProperties(prefix = "furent.stripe")
public class StripeProperties {

    private String secretKey = "";
    private String publishableKey = "";
    private String webhookSecret = "";
    private String currency = "usd";

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    public void setPublishableKey(String publishableKey) {
        this.publishableKey = publishableKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /** Indica si Stripe está configurado (clave secreta no vacía). */
    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }
}
