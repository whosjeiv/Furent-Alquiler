package com.alquiler.furent.service;

import com.alquiler.furent.config.TenantContext;
import com.alquiler.furent.model.User;
import com.alquiler.furent.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(OAuth2UserService.class);

    private final UserRepository userRepository;

    public OAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String providerId = oauth2User.getAttribute("sub");
        String email = oauth2User.getAttribute("email");
        String nombre = oauth2User.getAttribute("given_name");
        String apellido = oauth2User.getAttribute("family_name");
        String profileImageUrl = oauth2User.getAttribute("picture");

        log.info("OAuth2 login attempt - Provider: {}, Email: {}", provider, email);

        // Buscar o crear usuario
        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Actualizar información del proveedor si cambió
            if (user.getProvider() == null || !user.getProvider().equals(provider)) {
                user.setProvider(provider);
                user.setProviderId(providerId);
                user.setProfileImageUrl(profileImageUrl);
                user.setUltimaSesion(LocalDateTime.now());
                userRepository.save(user);
                log.info("Usuario existente actualizado con OAuth2: {}", email);
            }
        } else {
            // Crear nuevo usuario desde OAuth2
            user = new User();
            user.setEmail(email);
            user.setNombre(nombre != null ? nombre : "");
            user.setApellido(apellido != null ? apellido : "");
            user.setProvider(provider);
            user.setProviderId(providerId);
            user.setProfileImageUrl(profileImageUrl);
            user.setRole("USER");
            user.setActivo(true);
            user.setFechaCreacion(LocalDateTime.now());
            user.setUltimaSesion(LocalDateTime.now());
            
            String tenantId = TenantContext.getCurrentTenant();
            if (tenantId == null) tenantId = "default";
            user.setTenantId(tenantId);
            
            // No necesita password para OAuth2
            user.setPassword("");
            
            userRepository.save(user);
            log.info("Nuevo usuario creado desde OAuth2: {}", email);
        }

        return oauth2User;
    }
}
