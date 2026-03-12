package com.alquiler.furent.service;

import com.alquiler.furent.model.Tenant;
import com.alquiler.furent.repository.TenantRepository;
import com.alquiler.furent.exception.DuplicateResourceException;
import com.alquiler.furent.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de gestión de tenants (empresas) para el modelo SaaS multi-tenant.
 */
@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Tenant createTenant(String slug, String nombre, String adminEmail) {
        if (tenantRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Ya existe un tenant con el slug: " + slug);
        }
        Tenant tenant = new Tenant(slug, nombre, adminEmail);
        log.info("Nuevo tenant creado: {} ({})", nombre, slug);
        return tenantRepository.save(tenant);
    }

    public Tenant createDefaultTenant() {
        Optional<Tenant> existing = tenantRepository.findBySlug("default");
        if (existing.isPresent()) {
            return existing.get();
        }
        Tenant tenant = new Tenant("default", "Furent Default", "admin@furent.com");
        tenant.setPlan("ENTERPRISE");
        log.info("Tenant por defecto creado");
        return tenantRepository.save(tenant);
    }

    public Optional<Tenant> findBySlug(String slug) {
        return tenantRepository.findBySlug(slug);
    }

    public Optional<Tenant> findById(String id) {
        return tenantRepository.findById(id);
    }

    public Tenant getByIdOrThrow(String id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", id));
    }

    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    public Tenant updateTenant(String id, Tenant updates) {
        Tenant tenant = getByIdOrThrow(id);
        if (updates.getNombre() != null) tenant.setNombre(updates.getNombre());
        if (updates.getDescripcion() != null) tenant.setDescripcion(updates.getDescripcion());
        if (updates.getLogoUrl() != null) tenant.setLogoUrl(updates.getLogoUrl());
        if (updates.getDominio() != null) tenant.setDominio(updates.getDominio());
        if (updates.getPlan() != null) tenant.setPlan(updates.getPlan());
        if (updates.getTelefono() != null) tenant.setTelefono(updates.getTelefono());
        if (updates.getDireccion() != null) tenant.setDireccion(updates.getDireccion());
        if (updates.getCiudad() != null) tenant.setCiudad(updates.getCiudad());
        if (updates.getPais() != null) tenant.setPais(updates.getPais());
        tenant.setFechaActualizacion(LocalDateTime.now());
        return tenantRepository.save(tenant);
    }

    public void deactivateTenant(String id) {
        Tenant tenant = getByIdOrThrow(id);
        tenant.setActivo(false);
        tenant.setFechaActualizacion(LocalDateTime.now());
        tenantRepository.save(tenant);
        log.info("Tenant desactivado: {}", tenant.getSlug());
    }

    public long count() {
        return tenantRepository.count();
    }
}
