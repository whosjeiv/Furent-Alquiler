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

    public PasswordResetToken createToken(String userId) {
        // Invalidar todos los tokens anteriores del usuario
        tokenRepository.findByUserId(userId).forEach(existing -> {
            if (!existing.isUsed()) {
                existing.setUsed(true);
                tokenRepository.save(existing);
            }
        });

        PasswordResetToken token = new PasswordResetToken(userId);
        log.info("Token de recuperación creado para usuario ID: {}", userId);
        return tokenRepository.save(token);
    }

    public Optional<PasswordResetToken> findByToken(String token) {
        return tokenRepository.findByToken(token);
    }

    public boolean validateToken(String tokenStr) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(tokenStr);
        return tokenOpt.isPresent() && tokenOpt.get().isValid();
    }

    public void resetPassword(String tokenStr, String newPassword) {
        PasswordResetToken token = tokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new ResourceNotFoundException("Token no encontrado"));

        if (!token.isValid()) {
            throw new InvalidOperationException("El enlace ha expirado o ya fue utilizado");
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new InvalidOperationException("La contraseña debe tener al menos 8 caracteres");
        }
        int score = 0;
        if (newPassword.length() >= 8) score++;
        if (newPassword.chars().anyMatch(Character::isUpperCase)) score++;
        if (newPassword.chars().anyMatch(Character::isLowerCase)) score++;
        if (newPassword.chars().anyMatch(Character::isDigit)) score++;
        if (newPassword.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) score++;
        if (score < 3) {
            throw new InvalidOperationException("La contraseña es muy débil. Incluye mayúsculas, minúsculas, números y caracteres especiales.");
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
