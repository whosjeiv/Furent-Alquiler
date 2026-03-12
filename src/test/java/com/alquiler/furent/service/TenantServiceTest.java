package com.alquiler.furent.service;

import com.alquiler.furent.exception.DuplicateResourceException;
import com.alquiler.furent.exception.ResourceNotFoundException;
import com.alquiler.furent.model.Tenant;
import com.alquiler.furent.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void createTenant_validData_createsTenant() {
        when(tenantRepository.existsBySlug("test-co")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        Tenant result = tenantService.createTenant("test-co", "Test Company", "admin@test.com");

        assertNotNull(result);
        assertEquals("test-co", result.getSlug());
        assertEquals("Test Company", result.getNombre());
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    void createTenant_duplicateSlug_throwsDuplicateResourceException() {
        when(tenantRepository.existsBySlug("existing")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> tenantService.createTenant("existing", "Name", "mail@t.com"));
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void createDefaultTenant_notExists_createsDefault() {
        when(tenantRepository.findBySlug("default")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        Tenant result = tenantService.createDefaultTenant();

        assertEquals("default", result.getSlug());
        assertEquals("ENTERPRISE", result.getPlan());
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    void createDefaultTenant_alreadyExists_returnsExisting() {
        Tenant existing = new Tenant("default", "Furent Default", "admin@furent.com");
        when(tenantRepository.findBySlug("default")).thenReturn(Optional.of(existing));

        Tenant result = tenantService.createDefaultTenant();

        assertEquals("default", result.getSlug());
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void findBySlug_exists_returnsTenant() {
        Tenant tenant = new Tenant("slug-1", "Tenant 1", "a@a.com");
        when(tenantRepository.findBySlug("slug-1")).thenReturn(Optional.of(tenant));

        Optional<Tenant> result = tenantService.findBySlug("slug-1");

        assertTrue(result.isPresent());
        assertEquals("slug-1", result.get().getSlug());
    }

    @Test
    void findBySlug_notExists_returnsEmpty() {
        when(tenantRepository.findBySlug("missing")).thenReturn(Optional.empty());

        Optional<Tenant> result = tenantService.findBySlug("missing");

        assertTrue(result.isEmpty());
    }

    @Test
    void getByIdOrThrow_exists_returnsTenant() {
        Tenant tenant = new Tenant("s", "N", "e@e.com");
        tenant.setId("t-1");
        when(tenantRepository.findById("t-1")).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.getByIdOrThrow("t-1");

        assertNotNull(result);
    }

    @Test
    void getByIdOrThrow_notExists_throwsResourceNotFound() {
        when(tenantRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tenantService.getByIdOrThrow("missing"));
    }

    @Test
    void updateTenant_updatesSelectedFields() {
        Tenant existing = new Tenant("s", "Old Name", "e@e.com");
        existing.setId("t-1");
        when(tenantRepository.findById("t-1")).thenReturn(Optional.of(existing));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        Tenant updates = new Tenant();
        updates.setNombre("New Name");
        updates.setPlan("PREMIUM");

        Tenant result = tenantService.updateTenant("t-1", updates);

        assertEquals("New Name", result.getNombre());
        assertEquals("PREMIUM", result.getPlan());
        assertNotNull(result.getFechaActualizacion());
    }

    @Test
    void updateTenant_notExists_throwsResourceNotFound() {
        when(tenantRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> tenantService.updateTenant("missing", new Tenant()));
    }

    @Test
    void deactivateTenant_existingTenant_deactivates() {
        Tenant tenant = new Tenant("act", "Active", "a@a.com");
        tenant.setId("t-1");
        tenant.setActivo(true);
        when(tenantRepository.findById("t-1")).thenReturn(Optional.of(tenant));

        tenantService.deactivateTenant("t-1");

        assertFalse(tenant.isActivo());
        verify(tenantRepository).save(tenant);
    }

    @Test
    void deactivateTenant_notExists_throwsResourceNotFound() {
        when(tenantRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tenantService.deactivateTenant("missing"));
    }

    @Test
    void getAllTenants_returnsList() {
        when(tenantRepository.findAll()).thenReturn(List.of(
                new Tenant("a", "A", "a@a.com"),
                new Tenant("b", "B", "b@b.com")));

        List<Tenant> result = tenantService.getAllTenants();

        assertEquals(2, result.size());
    }

    @Test
    void count_delegatesToRepository() {
        when(tenantRepository.count()).thenReturn(5L);

        assertEquals(5L, tenantService.count());
    }
}
