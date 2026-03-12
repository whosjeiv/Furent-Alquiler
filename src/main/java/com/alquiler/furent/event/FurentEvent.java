package com.alquiler.furent.event;

import org.springframework.context.ApplicationEvent;

/**
 * Evento base para todos los eventos del dominio FURENT.
 * Incluye tenantId para trazabilidad multi-tenant.
 */
public abstract class FurentEvent extends ApplicationEvent {

    private final String tenantId;
    private final String userId;

    protected FurentEvent(Object source, String tenantId, String userId) {
        super(source);
        this.tenantId = tenantId;
        this.userId = userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getUserId() {
        return userId;
    }
}
