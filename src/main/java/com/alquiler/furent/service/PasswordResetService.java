package com.alquiler.furent.service;

import com.alquiler.furent.model.PasswordResetToken;
import com.alquiler.furent.model.User;
import com.alquiler.furent.repository.PasswordResetTokenRepository;
import com.alquiler.furent.exception.ResourceNotFoundException;
import com.alquiler.furent.exception.InvalidOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Servicio de restablecimiento de contraseña.
 * Gestiona tokens temporales con expiración, invalidación de tokens previos
 * y cambio seguro de contraseña con validación de longitud mínima.
 *
 * @author Furent Team
 * @since 1.0
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final PasswordResetTokenRepository tokenRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository, UserService userService,
                                PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    public PasswordResetToken createToken(String email) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Invalidar tokens previos no usados
        tokenRepository.findByUserIdAndUsedFalse(user.getId()).ifPresent(existing -> {
            existing.setUsed(true);
            tokenRepository.save(existing);
        });

        PasswordResetToken token = new PasswordResetToken(user.getId());
        log.info("Token de recuperación creado para: {}", email);
        return tokenRepository.save(token);
    }

    public Optional<PasswordResetToken> findByToken(String token) {
        return tokenRepository.findByToken(token);
    }

    public void resetPassword(String tokenStr, String newPassword) {
        PasswordResetToken token = tokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new ResourceNotFoundException("Token no encontrado"));

        if (!token.isValid()) {
            throw new InvalidOperationException("El enlace ha expirado o ya fue utilizado");
        }

        if (newPassword.length() < 6) {
            throw new InvalidOperationException("La contraseña debe tener al menos 6 caracteres");
        }

        User user = userService.findById(token.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userService.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        log.info("Contraseña restablecida exitosamente para usuario: {}", user.getEmail());
    }
}
