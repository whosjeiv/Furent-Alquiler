package com.alquiler.furent.controller;

import com.alquiler.furent.model.PasswordResetToken;
import com.alquiler.furent.model.User;
import com.alquiler.furent.service.EmailService;
import com.alquiler.furent.service.PasswordResetService;
import com.alquiler.furent.service.UserService;
import com.alquiler.furent.exception.InvalidOperationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Controlador para gestionar el flujo de recuperación de contraseña.
 * Maneja la solicitud de reset, validación de tokens y confirmación de nueva contraseña.
 */
@Controller
@Tag(name = "Recuperación de Contraseña", description = "Endpoints para recuperación de contraseña olvidada")
public class PasswordResetController {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetController.class);

    private final PasswordResetService passwordResetService;
    private final UserService userService;
    private final EmailService emailService;

    public PasswordResetController(PasswordResetService passwordResetService,
                                   UserService userService,
                                   EmailService emailService) {
        this.passwordResetService = passwordResetService;
        this.userService = userService;
        this.emailService = emailService;
    }

    /**
     * Muestra el formulario de solicitud de reset de contraseña
     */
    @Operation(summary = "Formulario de solicitud de reset", 
               description = "Muestra el formulario para solicitar recuperación de contraseña")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Formulario mostrado exitosamente")
    })
    @GetMapping("/password-reset")
    public String passwordResetForm() {
        return "password-reset";
    }

    /**
     * Procesa la solicitud de reset de contraseña.
     * No revela si el email existe por seguridad.
     */
    @Operation(summary = "Solicitar reset de contraseña", 
               description = "Procesa la solicitud de recuperación de contraseña. Envía un email con el token si el usuario existe. Por seguridad, no revela si el email está registrado")
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Solicitud procesada, redirige con mensaje genérico"),
        @ApiResponse(responseCode = "400", description = "Email inválido o faltante")
    })
    @PostMapping("/password-reset")
    public String requestPasswordReset(@RequestParam String email,
                                       RedirectAttributes redirectAttributes) {
        try {
            Optional<User> userOpt = userService.findByEmail(email);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                PasswordResetToken token = passwordResetService.createToken(user.getId());
                emailService.sendPasswordResetToken(user, token.getToken());
                log.info("Email de recuperación enviado a: {}", email);
            } else {
                // No revelar si el email existe
                log.info("Intento de reset para email no existente: {}", email);
            }
            
            // Mensaje genérico para no revelar si el email existe
            redirectAttributes.addFlashAttribute("success", 
                "Si el email existe, recibirás un enlace de recuperación en tu correo.");
        } catch (Exception e) {
            log.error("Error al procesar solicitud de reset: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", 
                "Ocurrió un error. Por favor intenta nuevamente.");
        }
        
        return "redirect:/password-reset";
    }

    /**
     * Muestra el formulario de confirmación de nueva contraseña
     */
    @Operation(summary = "Formulario de confirmación de reset", 
               description = "Muestra el formulario para establecer nueva contraseña usando un token válido")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Formulario mostrado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Token expirado o inválido")
    })
    @GetMapping("/password-reset/{token}")
    public String passwordResetConfirmForm(@PathVariable String token, Model model) {
        if (!passwordResetService.validateToken(token)) {
            model.addAttribute("error", "El enlace ha expirado o ya fue utilizado");
            return "error/token-expired";
        }
        
        model.addAttribute("token", token);
        return "password-reset-confirm";
    }

    /**
     * Procesa la confirmación de reset de contraseña
     */
    @Operation(summary = "Confirmar reset de contraseña", 
               description = "Establece la nueva contraseña usando un token válido. Valida que las contraseñas coincidan y que el token no haya expirado")
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Contraseña cambiada exitosamente, redirige a login"),
        @ApiResponse(responseCode = "400", description = "Contraseñas no coinciden, token inválido o expirado")
    })
    @PostMapping("/password-reset/confirm")
    public String confirmPasswordReset(@RequestParam String token,
                                       @RequestParam String password,
                                       @RequestParam String passwordConfirm,
                                       RedirectAttributes redirectAttributes) {
        try {
            // Validar que las contraseñas coinciden
            if (!password.equals(passwordConfirm)) {
                redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden");
                return "redirect:/password-reset/" + token;
            }
            
            // Resetear contraseña
            passwordResetService.resetPassword(token, password);
            
            redirectAttributes.addFlashAttribute("success", 
                "Contraseña cambiada exitosamente. Ya puedes iniciar sesión.");
            return "redirect:/login";
            
        } catch (InvalidOperationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/password-reset/" + token;
        } catch (Exception e) {
            log.error("Error al confirmar reset de contraseña: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", 
                "Ocurrió un error. Por favor intenta nuevamente.");
            return "redirect:/password-reset/" + token;
        }
    }
}
