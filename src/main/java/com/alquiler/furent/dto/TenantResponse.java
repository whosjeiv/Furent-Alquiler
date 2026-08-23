package com.alquiler.furent.dto;

import com.alquiler.furent.model.Tenant;

public record TenantResponse(
    String id,
    String slug,
    String nombre,
    String plan,
    boolean activo,
    String logoUrl
) {
    public static TenantResponse from(Tenant t) {
        return new TenantResponse(
            t.getId(), t.getSlug(), t.getNombre(),
            t.getPlan(), t.isActivo(), t.getLogoUrl()
        );
    }
}
