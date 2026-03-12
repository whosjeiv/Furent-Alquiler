package com.alquiler.furent.config;

import com.alquiler.furent.model.Tenant;
import com.alquiler.furent.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filtro que extrae el tenantId del header X-Tenant-ID o del subdominio
 * y lo almacena en TenantContext para toda la duración del request.
 */
@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);
    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String DEFAULT_TENANT = "default";

    private final TenantRepository tenantRepository;

    public TenantFilter(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String tenantId = resolveTenantId(request);
            TenantContext.setCurrentTenant(tenantId);
            log.debug("Tenant resuelto: {} para {}", tenantId, request.getRequestURI());
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveTenantId(HttpServletRequest request) {
        // 1. Intentar desde header
        String headerTenant = request.getHeader(TENANT_HEADER);
        if (headerTenant != null && !headerTenant.isBlank()) {
            return headerTenant;
        }

        // 2. Intentar desde subdominio (tenant.furent.com)
        String host = request.getServerName();
        if (host != null && host.contains(".")) {
            String subdomain = host.split("\\.")[0];
            if (!subdomain.equals("www") && !subdomain.equals("localhost") && !subdomain.equals("api")) {
                Optional<Tenant> tenant = tenantRepository.findBySlug(subdomain);
                if (tenant.isPresent()) {
                    return tenant.get().getId();
                }
            }
        }

        // 3. Fallback: tenant por defecto
        return DEFAULT_TENANT;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/") || path.startsWith("/js/") ||
               path.startsWith("/images/") || path.startsWith("/uploads/") ||
               path.startsWith("/actuator/");
    }
}
