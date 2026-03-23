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

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       EventPublisher eventPublisher, MetricsConfig metricsConfig) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.metricsConfig = metricsConfig;
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
}
