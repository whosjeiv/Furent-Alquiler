package com.alquiler.furent.config;

import com.alquiler.furent.model.User;
import com.alquiler.furent.service.NotificationService;
import com.alquiler.furent.service.UserService;
import org.springframework.security.core.Authentication;
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
            Optional<User> user = userService.findByEmail(auth.getName());
            return user.orElse(null);
        }
        return null;
    }

    @ModelAttribute("unreadNotifications")
    public long unreadNotifications(Authentication auth) {
        if (auth != null && auth.isAuthenticated() &&
                !"anonymousUser".equals(auth.getPrincipal())) {
            Optional<User> user = userService.findByEmail(auth.getName());
            if (user.isPresent()) {
                return notificationService.countUnread(user.get().getId());
            }
        }
        return 0;
    }
}
