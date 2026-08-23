package com.alquiler.furent.config;

import com.alquiler.furent.model.User;
import com.alquiler.furent.service.NotificationService;
import com.alquiler.furent.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.Optional;

@ControllerAdvice
public class GlobalModelAdvice {

    private final UserService userService;
    private final NotificationService notificationService;

    public GlobalModelAdvice(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @ModelAttribute("currentUser")
    public User currentUser(Authentication auth) {
        if (auth != null && auth.isAuthenticated() &&
                !"anonymousUser".equals(auth.getPrincipal())) {
            
            String email = null;
            
            // Manejar OAuth2 Authentication
            if (auth instanceof OAuth2AuthenticationToken) {
                OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) auth;
                email = oauth2Token.getPrincipal().getAttribute("email");
            } else {
                // Autenticación tradicional (email/password)
                email = auth.getName();
            }
            
            if (email != null) {
                Optional<User> user = userService.findByEmail(email);
                return user.orElse(null);
            }
        }
        return null;
    }

    @ModelAttribute("unreadNotifications")
    public long unreadNotifications(Authentication auth) {
        if (auth != null && auth.isAuthenticated() &&
                !"anonymousUser".equals(auth.getPrincipal())) {
            
            String email = null;
            
            // Manejar OAuth2 Authentication
            if (auth instanceof OAuth2AuthenticationToken) {
                OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) auth;
                email = oauth2Token.getPrincipal().getAttribute("email");
            } else {
                // Autenticación tradicional (email/password)
                email = auth.getName();
            }
            
            if (email != null) {
                Optional<User> user = userService.findByEmail(email);
                if (user.isPresent()) {
                    return notificationService.countUnread(user.get().getId());
                }
            }
        }
        return 0;
    }
}
