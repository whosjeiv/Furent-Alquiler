package com.alquiler.furent.service;

import com.alquiler.furent.model.User;
import com.alquiler.furent.repository.UserRepository;
import com.alquiler.furent.enums.RolUsuario;
import com.alquiler.furent.exception.DuplicateResourceException;
import com.alquiler.furent.config.TenantContext;
import com.alquiler.furent.config.MetricsConfig;
import com.alquiler.furent.event.EventPublisher;
import com.alquiler.furent.event.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.alquiler.furent.exception.AccountSuspendedException;

/**
 * Servicio de gestión de usuarios y autenticación.
 * Implementa {@link UserDetailsService} para integración con Spring Security.
 * Maneja registro, búsqueda, activación/suspensión y carga de credenciales.
 *
 * @author Furent Team
 * @since 1.0
 */
@Service
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;
    private final MetricsConfig metricsConfig;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       EventPublisher eventPublisher, MetricsConfig metricsConfig,
                       EmailService emailService, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.metricsConfig = metricsConfig;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        // === Lógica de Suspensión Dinámica ===
        LocalDateTime now = LocalDateTime.now();
        boolean isSuspended = false;

        // 1. Verificación por bandera manual
        if (!user.isActivo()) {
            isSuspended = true;
        }

        // 2. Verificación por ventana de tiempo (si tiene fechas programadas)
        if (user.getFechaInicioSuspension() != null && now.isAfter(user.getFechaInicioSuspension())) {
            if (user.isSuspensionPermanente()) {
                isSuspended = true;
            } else if (user.getFechaFinSuspension() != null && now.isBefore(user.getFechaFinSuspension())) {
                isSuspended = true;
            } else if (user.getFechaFinSuspension() != null && now.isAfter(user.getFechaFinSuspension())) {
                // Auto-reactivación lógica si ya pasó el tiempo
                if (!user.isActivo()) {
                    user.setActivo(true);
                    user.setRazonSuspension(null);
                    user.setFechaFinSuspension(null);
                    user.setFechaInicioSuspension(null);
                    userRepository.save(user);
                    isSuspended = false;
                }
            } else if (user.getFechaFinSuspension() == null && !user.isActivo()) {
                // Indefinido y marcado como inactivo
                isSuspended = true;
            }
        }

        if (isSuspended) {
            String reason = user.getRazonSuspension() != null ? user.getRazonSuspension()
                    : "Incumplimiento de términos";
            String duration;
            if (user.isSuspensionPermanente()) {
                duration = "Permanente (Sin opción a retorno)";
            } else if (user.getFechaFinSuspension() != null) {
                duration = "Hasta el "
                        + user.getFechaFinSuspension().format(DateTimeFormatter.ofPattern("dd MMM yyyy 'a las' HH:mm"));
            } else {
                duration = "Indefinida";
            }
            throw new AccountSuspendedException("Cuenta suspendida", reason, duration, user.isSuspensionPermanente());
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
    }

    private static final Set<String> ALLOWED_EMAIL_DOMAINS = Set.of(
            "gmail.com", "outlook.com", "outlook.es", "hotmail.com", "hotmail.es", "live.com"
    );

    public User register(String email, String password, String nombre, String apellido, String telefono) {
        // Validate email domain
        if (email == null || !email.contains("@")) {
            throw new RuntimeException("Formato de email inválido");
        }
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        if (!ALLOWED_EMAIL_DOMAINS.contains(domain)) {
            throw new RuntimeException("Solo aceptamos correos de Gmail, Outlook o Hotmail");
        }

        // Validate password strength (must pass at least 3 of 5 rules)
        if (password == null || password.length() < 8) {
            throw new RuntimeException("La contraseña debe tener al menos 8 caracteres");
        }
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.chars().anyMatch(Character::isUpperCase)) score++;
        if (password.chars().anyMatch(Character::isLowerCase)) score++;
        if (password.chars().anyMatch(Character::isDigit)) score++;
        if (password.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) score++;
        if (score < 3) {
            throw new RuntimeException("La contraseña es muy débil. Incluye mayúsculas, minúsculas, números y caracteres especiales.");
        }

        if (userRepository.existsByEmail(email)) {
            log.warn("Intento de registro con email duplicado: {}", email);
            throw new DuplicateResourceException("El email ya está registrado");
        }
        User user = new User(email, passwordEncoder.encode(password), nombre, apellido, telefono, RolUsuario.USER.name());
        User saved = userRepository.save(user);
        metricsConfig.getUsersRegistered().increment();
        log.info("Nuevo usuario registrado: {}", email);

        String tenantId = TenantContext.getCurrentTenant() != null ? TenantContext.getCurrentTenant() : "default";
        eventPublisher.publish(new UserRegisteredEvent(this, saved, tenantId));

        // Enviar email de bienvenida de forma asíncrona
        try {
            emailService.sendWelcomeEmail(saved);
        } catch (Exception e) {
            log.error("Error al enviar email de bienvenida a {}: {}", email, e.getMessage());
            // No interrumpir el flujo de registro si falla el email
        }

        return saved;
    }

    public User createAdmin(String email, String password, String nombre, String apellido) {
        if (userRepository.existsByEmail(email)) {
            return userRepository.findByEmail(email).get();
        }
        User admin = new User(email, passwordEncoder.encode(password), nombre, apellido, "", RolUsuario.ADMIN.name());
        return userRepository.save(admin);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Page<User> getUsersPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        return userRepository.findAll(pageable);
    }

    public long count() {
        return userRepository.count();
    }

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Suspende un usuario temporalmente.
     * 
     * @param userId ID del usuario a suspender
     * @param reason Razón de la suspensión
     * @param admin Email del administrador que realiza la acción
     * @return Usuario suspendido
     */
    public User suspendUser(String userId, String reason, String admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        user.setEstado("SUSPENDIDO_TEMPORAL");
        user.setActivo(false);
        user.setRazonSuspension(reason);
        user.setFechaSuspension(LocalDateTime.now());
        user.setFechaInicioSuspension(LocalDateTime.now());
        
        User saved = userRepository.save(user);
        
        // Registrar en audit log
        auditLogService.log(admin, "USER_SUSPENDED", "User", userId, 
                "Usuario " + user.getEmail() + " suspendido. Razón: " + reason);
        log.info("Usuario {} suspendido por {}", user.getEmail(), admin);
        
        return saved;
    }

    /**
     * Activa un usuario suspendido.
     * 
     * @param userId ID del usuario a activar
     * @param admin Email del administrador que realiza la acción
     * @return Usuario activado
     */
    public User activateUser(String userId, String admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        user.setEstado("ACTIVO");
        user.setActivo(true);
        user.setRazonSuspension(null);
        user.setFechaSuspension(null);
        user.setFechaInicioSuspension(null);
        user.setFechaFinSuspension(null);
        user.setSuspensionPermanente(false);
        
        User saved = userRepository.save(user);
        
        // Registrar en audit log
        auditLogService.log(admin, "USER_ACTIVATED", "User", userId, 
                "Usuario " + user.getEmail() + " activado");
        log.info("Usuario {} activado por {}", user.getEmail(), admin);
        
        return saved;
    }

    /**
     * Cambia el rol de un usuario.
     * 
     * @param userId ID del usuario
     * @param newRole Nuevo rol (USER, ADMIN)
     * @param admin Email del administrador que realiza la acción
     * @return Usuario actualizado
     */
    public User changeUserRole(String userId, String newRole, String admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        String oldRole = user.getRole();
        user.setRole(newRole);
        
        User saved = userRepository.save(user);
        
        // Registrar en audit log
        auditLogService.log(admin, "USER_ROLE_CHANGED", "User", userId, 
                "Rol de usuario " + user.getEmail() + " cambiado de " + oldRole + " a " + newRole);
        log.info("Rol de usuario {} cambiado de {} a {} por {}", user.getEmail(), oldRole, newRole, admin);
        
        return saved;
    }

    /**
     * Realiza soft delete de un usuario (marca como eliminado sin borrar datos).
     * 
     * @param userId ID del usuario
     * @param admin Email del administrador que realiza la acción
     * @return Usuario marcado como eliminado
     */
    public User softDeleteUser(String userId, String admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        user.setDeleted(true);
        user.setEstado("ELIMINADO");
        user.setActivo(false);
        
        User saved = userRepository.save(user);
        
        // Registrar en audit log
        auditLogService.log(admin, "USER_SOFT_DELETED", "User", userId, 
                "Usuario " + user.getEmail() + " marcado como eliminado");
        log.info("Usuario {} marcado como eliminado por {}", user.getEmail(), admin);
        
        return saved;
    }

    /**
     * Actualiza la contraseña de un usuario.
     * 
     * @param userId ID del usuario
     * @param newPassword Nueva contraseña (será encriptada)
     */
    public void updatePassword(String userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("Contraseña actualizada para usuario {}", user.getEmail());
    }
}
