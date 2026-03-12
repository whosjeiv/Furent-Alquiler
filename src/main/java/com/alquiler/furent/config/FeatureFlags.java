package com.alquiler.furent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Sistema de Feature Flags para controlar funcionalidades sin deploy.
 * Permite habilitar/deshabilitar features de forma granular via properties.
 *
 * Uso en servicios:
 *   if (featureFlags.isCacheEnabled()) { ... }
 *
 * Configuración en application.properties:
 *   furent.features.cache-enabled=true
 *   furent.features.notifications-enabled=true
 */
@Component
@ConfigurationProperties(prefix = "furent.features")
public class FeatureFlags {

    private boolean cacheEnabled = true;
    private boolean notificationsEnabled = true;
    private boolean reviewsEnabled = true;
    private boolean exportPdfEnabled = true;
    private boolean analyticsEnabled = true;

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public boolean isReviewsEnabled() {
        return reviewsEnabled;
    }

    public void setReviewsEnabled(boolean reviewsEnabled) {
        this.reviewsEnabled = reviewsEnabled;
    }

    public boolean isExportPdfEnabled() {
        return exportPdfEnabled;
    }

    public void setExportPdfEnabled(boolean exportPdfEnabled) {
        this.exportPdfEnabled = exportPdfEnabled;
    }

    public boolean isAnalyticsEnabled() {
        return analyticsEnabled;
    }

    public void setAnalyticsEnabled(boolean analyticsEnabled) {
        this.analyticsEnabled = analyticsEnabled;
    }
}
