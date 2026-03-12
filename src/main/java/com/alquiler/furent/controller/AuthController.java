package com.alquiler.furent.controller;

import com.alquiler.furent.config.TenantContext;
import com.alquiler.furent.dto.AuthResponse;
import com.alquiler.furent.dto.LoginRequest;
import com.alquiler.furent.dto.UserResponse;
import com.alquiler.furent.model.RefreshToken;
import com.alquiler.furent.model.User;
import com.alquiler.furent.service.JwtService;
import com.alquiler.furent.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador de autenticación REST (JWT).
 * Endpoints: /api/auth/login, /api/auth/register, /api/auth/refresh, /api/auth/logout
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Endpoints de autenticación JWT: login, registro, refresh y logout")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(summary = "Iniciar sesión", description = "Autentica al usuario con email y contraseña, devuelve tokens JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso, devuelve accessToken y refreshToken"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas"),
            @ApiResponse(responseCode = "403", description = "Cuenta suspendida")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String tenantId = request.getTenantId() != null ? request.getTenantId() : TenantContext.getCurrentTenant();
        if (tenantId == null) tenantId = "default";

        User user = userService.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }

        if (!user.isActivo()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Cuenta suspendida", "razon", user.getRazonSuspension() != null ? user.getRazonSuspension() : ""));
        }

        String accessToken = jwtService.generateAccessToken(user, tenantId);
        RefreshToken refreshToken = jwtService.generateRefreshToken(user, tenantId);

        log.info("Login JWT exitoso: {} tenant={}", user.getEmail(), tenantId);

        return ResponseEntity.ok(AuthResponse.of(
                accessToken,
                refreshToken.getToken(),
                jwtService.getAccessTokenExpiration(),
                UserResponse.from(user),
                tenantId
        ));
    }

    @Operation(summary = "Registrar usuario", description = "Crea una nueva cuenta de usuario y devuelve tokens JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Campos obligatorios faltantes o contraseña muy corta"),
            @ApiResponse(responseCode = "409", description = "El email ya está registrado")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        String nombre = body.get("nombre");
        String apellido = body.get("apellido");
        String telefono = body.get("telefono");
        String tenantId = body.getOrDefault("tenantId", TenantContext.getCurrentTenant());
        if (tenantId == null) tenantId = "default";

        if (email == null || password == null || nombre == null || apellido == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Faltan campos obligatorios: email, password, nombre, apellido"));
        }

        if (password.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La contraseña debe tener al menos 6 caracteres"));
        }

        try {
            User user = userService.register(email, password, nombre, apellido, telefono != null ? telefono : "");
            user.setTenantId(tenantId);
            userService.save(user);

            String accessToken = jwtService.generateAccessToken(user, tenantId);
            RefreshToken refreshToken = jwtService.generateRefreshToken(user, tenantId);

            log.info("Registro JWT exitoso: {} tenant={}", user.getEmail(), tenantId);

            return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.of(
                    accessToken,
                    refreshToken.getToken(),
                    jwtService.getAccessTokenExpiration(),
                    UserResponse.from(user),
                    tenantId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Refrescar token", description = "Genera un nuevo accessToken usando un refreshToken válido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens renovados exitosamente"),
            @ApiResponse(responseCode = "400", description = "refreshToken no proporcionado"),
            @ApiResponse(responseCode = "401", description = "refreshToken inválido o expirado")
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshTokenStr = body.get("refreshToken");
        if (refreshTokenStr == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "refreshToken es obligatorio"));
        }

        try {
            RefreshToken refreshToken = jwtService.validateRefreshToken(refreshTokenStr);
            User user = userService.findById(refreshToken.getUserId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            String newAccessToken = jwtService.generateAccessToken(user, refreshToken.getTenantId());
            RefreshToken newRefreshToken = jwtService.generateRefreshToken(user, refreshToken.getTenantId());

            return ResponseEntity.ok(AuthResponse.of(
                    newAccessToken,
                    newRefreshToken.getToken(),
                    jwtService.getAccessTokenExpiration(),
                    UserResponse.from(user),
                    refreshToken.getTenantId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Cerrar sesión", description = "Revoca todos los tokens del usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión cerrada exitosamente")
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            try {
                String userId = jwtService.extractUserId(jwt);
                jwtService.revokeAllUserTokens(userId);
                log.info("Logout JWT: userId={}", userId);
            } catch (Exception e) {
                log.warn("Error en logout JWT: {}", e.getMessage());
            }
        }
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada exitosamente"));
    }

    @Operation(summary = "Obtener usuario actual", description = "Devuelve los datos del usuario autenticado a partir del token JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos del usuario autenticado"),
            @ApiResponse(responseCode = "401", description = "Token no proporcionado o inválido"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token no proporcionado"));
        }

        String jwt = authHeader.substring(7);
        if (!jwtService.isTokenValid(jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token inválido"));
        }

        String userId = jwtService.extractUserId(jwt);
        User user = userService.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado"));
        }

        return ResponseEntity.ok(UserResponse.from(user));
    }
}
