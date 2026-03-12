package com.alquiler.furent.controller;

import com.alquiler.furent.model.Product;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.model.User;
import com.alquiler.furent.model.Review;
import com.alquiler.furent.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class PageController {

    private static final Logger log = LoggerFactory.getLogger(PageController.class);

    private final ProductService productService;
    private final ReservationService reservationService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final ReviewService reviewService;
    private final ContactService contactService;
    private final NotificationService notificationService;
    private final PasswordResetService passwordResetService;

    public PageController(ProductService productService, ReservationService reservationService,
            UserService userService, PasswordEncoder passwordEncoder, ReviewService reviewService,
            ContactService contactService, NotificationService notificationService,
            PasswordResetService passwordResetService) {
        this.productService = productService;
        this.reservationService = reservationService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.reviewService = reviewService;
        this.contactService = contactService;
        this.notificationService = notificationService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("featuredProducts", productService.getFeaturedProducts());
        return "index";
    }

    @GetMapping("/catalogo")
    public String catalog(@RequestParam(required = false) String category,
                          @RequestParam(required = false) String q,
                          @RequestParam(defaultValue = "0") int page,
                          Model model) {
        model.addAttribute("categories", productService.getAllCategories());

        if (q != null && !q.trim().isEmpty()) {
            List<Product> products = productService.searchProducts(q.trim());
            model.addAttribute("products", products);
            model.addAttribute("searchQuery", q.trim());
            model.addAttribute("selectedCategory", "Búsqueda: " + q.trim());
        } else {
            org.springframework.data.domain.Page<Product> productPage =
                    productService.getProductsPaginated(category, page, 12);
            model.addAttribute("products", productPage.getContent());
            model.addAttribute("productPage", productPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("selectedCategory", category != null ? category : "Todos");
        }

        return "catalog";
    }

    @GetMapping("/producto/{id}")
    public String productDetail(@PathVariable String id, Model model) {
        Optional<Product> product = productService.getProductById(id);
        if (product.isPresent()) {
            model.addAttribute("product", product.get());
            model.addAttribute("relatedProducts",
                    productService.getRelatedProducts(id, product.get().getCategory()));

            // Load reviews
            List<Review> reviews = reviewService.getReviewsByProduct(id);
            model.addAttribute("reviews", reviews);

            // Calculate average rating
            double avgRating = 0;
            if (!reviews.isEmpty()) {
                avgRating = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
            }
            model.addAttribute("avgRating", avgRating);
            model.addAttribute("totalReviews", reviews.size());

            // Load occupied date ranges for availability calendar
            List<Reservation> productReservations = reservationService.getActiveReservationsByProductId(id);
            List<Map<String, String>> occupiedRanges = productReservations.stream().map(r -> {
                Map<String, String> range = new HashMap<>();
                range.put("start", r.getFechaInicio() != null ? r.getFechaInicio().toString() : "");
                range.put("end", r.getFechaFin() != null ? r.getFechaFin().plusDays(1).toString() : "");
                return range;
            }).collect(Collectors.toList());
            model.addAttribute("occupiedRanges", occupiedRanges);

            return "product-detail";
        }
        return "redirect:/catalogo";
    }

    @GetMapping("/panel")
    public String userPanel(Model model, Authentication auth) {
        List<Reservation> userRes = new ArrayList<>();
        List<Reservation> active = new ArrayList<>();
        List<Reservation> pending = new ArrayList<>();
        List<Reservation> completed = new ArrayList<>();

        if (auth != null) {
            String email = auth.getName();
            Optional<User> user = userService.findByEmail(email);
            if (user.isPresent()) {
                User u = user.get();
                model.addAttribute("currentUser", u);
                String uId = u.getId();
                userRes = reservationService.getByUsuarioId(uId);

                active = userRes.stream()
                        .filter(r -> "ACTIVA".equals(r.getEstado()) || "CONFIRMADA".equals(r.getEstado()))
                        .collect(Collectors.toList());
                pending = userRes.stream().filter(r -> "PENDIENTE".equals(r.getEstado()))
                        .collect(Collectors.toList());
                completed = userRes.stream().filter(r -> "COMPLETADA".equals(r.getEstado()))
                        .collect(Collectors.toList());

                BigDecimal totalGastado = userRes.stream()
                        .filter(r -> "COMPLETADA".equals(r.getEstado()) || "ACTIVA".equals(r.getEstado())
                                || "CONFIRMADA".equals(r.getEstado()))
                        .map(Reservation::getTotal)
                        .filter(t -> t != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                model.addAttribute("totalGastado", totalGastado);

                // Favoritos
                if (u.getFavoritos() != null && !u.getFavoritos().isEmpty()) {
                    model.addAttribute("favoritos", productService.getProductsByIds(u.getFavoritos()));
                } else {
                    model.addAttribute("favoritos", new ArrayList<>());
                }

                // Notificaciones
                model.addAttribute("notificaciones", notificationService.getRecentNotifications(uId));
                model.addAttribute("notificacionesNoLeidas", notificationService.countUnread(uId));
            }
        }

        model.addAttribute("reservations", userRes);
        model.addAttribute("activeReservations", active);
        model.addAttribute("pendingReservations", pending);
        model.addAttribute("completedReservations", completed);
        model.addAttribute("activeCount", active.size());
        model.addAttribute("pendingCount", pending.size());
        model.addAttribute("completedCount", completed.size());

        return "user-panel";
    }

    @GetMapping("/nosotros")
    public String about(Model model) {
        model.addAttribute("pageTitle", "Nosotros");
        return "about";
    }

    @GetMapping("/contacto")
    public String contact(Model model) {
        model.addAttribute("pageTitle", "Contacto");
        return "contact";
    }

    @PostMapping("/contacto")
    public String submitContact(@RequestParam String nombre, @RequestParam String email,
                                @RequestParam(required = false) String telefono,
                                @RequestParam String asunto, @RequestParam String mensaje,
                                RedirectAttributes redirectAttributes) {
        com.alquiler.furent.model.ContactMessage msg = new com.alquiler.furent.model.ContactMessage();
        msg.setNombre(nombre);
        msg.setEmail(email);
        msg.setTelefono(telefono != null ? telefono : "");
        msg.setAsunto(asunto);
        msg.setMensaje(mensaje);
        contactService.save(msg);
        log.info("Nuevo mensaje de contacto de: {}", email);
        redirectAttributes.addFlashAttribute("success", "¡Mensaje enviado exitosamente! Te responderemos pronto.");
        return "redirect:/contacto";
    }

    @GetMapping("/faq")
    public String faq(Model model) {
        model.addAttribute("pageTitle", "FAQ");
        return "faq";
    }

    @GetMapping("/cotizacion")
    public String cotizacion(Model model) {
        model.addAttribute("pageTitle", "Solicitar cotización");
        return "cotizacion";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("pageTitle", "Iniciar sesión");
        return "login";
    }

    @GetMapping("/logout")
    public String logoutPage() {
        return "redirect:/?logout=true";
    }

    @GetMapping("/inicio-rapido")
    public String quickLogin(Model model) {
        model.addAttribute("pageTitle", "Inicio rápido");
        return "quick-login";
    }

    @GetMapping("/registro")
    public String register(Model model) {
        model.addAttribute("pageTitle", "Crear cuenta");
        return "register";
    }

    @PostMapping("/registro")
    public String registerUser(@RequestParam String email, @RequestParam String password,
            @RequestParam String nombre, @RequestParam String apellido,
            @RequestParam(required = false) String telefono,
            RedirectAttributes redirectAttributes) {
        try {
            userService.register(email, password, nombre, apellido, telefono != null ? telefono : "");
            redirectAttributes.addFlashAttribute("success", "¡Cuenta creada exitosamente! Inicia sesión.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/registro";
        }
    }

    @GetMapping("/configuracion")
    public String settings(Model model) {
        model.addAttribute("pageTitle", "Configuración");
        return "settings";
    }

    @PostMapping("/configuracion/perfil")
    public String updateProfile(@RequestParam String nombre, @RequestParam String apellido,
            @RequestParam String telefono,
            Authentication auth, RedirectAttributes redirectAttributes) {
        if (auth != null) {
            Optional<User> optUser = userService.findByEmail(auth.getName());
            if (optUser.isPresent()) {
                User user = optUser.get();
                user.setNombre(nombre);
                user.setApellido(apellido);
                user.setTelefono(telefono);
                userService.save(user);
                redirectAttributes.addFlashAttribute("success", "Perfil actualizado correctamente");
            }
        }
        return "redirect:/configuracion";
    }

    @PostMapping("/configuracion/password")
    public String updatePassword(@RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication auth, RedirectAttributes redirectAttributes) {
        if (auth != null) {
            Optional<User> optUser = userService.findByEmail(auth.getName());
            if (optUser.isPresent()) {
                User user = optUser.get();
                if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                    redirectAttributes.addFlashAttribute("passwordError", "La contraseña actual es incorrecta");
                    return "redirect:/configuracion";
                }
                if (!newPassword.equals(confirmPassword)) {
                    redirectAttributes.addFlashAttribute("passwordError", "Las contraseñas no coinciden");
                    return "redirect:/configuracion";
                }
                if (newPassword.length() < 6) {
                    redirectAttributes.addFlashAttribute("passwordError",
                            "La contraseña debe tener al menos 6 caracteres");
                    return "redirect:/configuracion";
                }
                user.setPassword(passwordEncoder.encode(newPassword));
                userService.save(user);
                redirectAttributes.addFlashAttribute("success", "Contraseña actualizada correctamente");
            }
        }
        return "redirect:/configuracion";
    }

    @PostMapping("/configuracion/preferencias")
    public String updatePreferences(@RequestParam String idioma, @RequestParam String moneda,
            @RequestParam String apariencia,
            Authentication auth, RedirectAttributes redirectAttributes) {
        if (auth != null) {
            Optional<User> optUser = userService.findByEmail(auth.getName());
            if (optUser.isPresent()) {
                User user = optUser.get();
                user.setIdioma(idioma);
                user.setMoneda(moneda);
                user.setApariencia(apariencia);
                userService.save(user);
                redirectAttributes.addFlashAttribute("success", "Preferencias actualizadas correctamente");
            }
        }
        return "redirect:/configuracion";
    }

    // === Password Reset ===

    @GetMapping("/password-reset")
    public String showPasswordResetRequest(Model model) {
        model.addAttribute("pageTitle", "Recuperar contraseña");
        return "password-reset";
    }

    @PostMapping("/password-reset")
    public String requestPasswordReset(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            var token = passwordResetService.createToken(email);
            // En producción se enviaría por email. Aquí mostramos el enlace.
            String resetUrl = "/password-reset/" + token.getToken();
            log.info("Enlace de recuperación generado para {}: {}", email, resetUrl);
            redirectAttributes.addFlashAttribute("success",
                    "Si el correo existe, recibirás un enlace de recuperación. Revisa tu bandeja.");
            redirectAttributes.addFlashAttribute("resetLink", resetUrl);
        } catch (RuntimeException e) {
            // No revelamos si el email existe o no por seguridad
            redirectAttributes.addFlashAttribute("success",
                    "Si el correo existe, recibirás un enlace de recuperación. Revisa tu bandeja.");
        }
        return "redirect:/password-reset";
    }

    @GetMapping("/password-reset/{token}")
    public String showResetForm(@PathVariable String token, Model model, RedirectAttributes redirectAttributes) {
        var resetToken = passwordResetService.findByToken(token);
        if (resetToken.isEmpty() || !resetToken.get().isValid()) {
            redirectAttributes.addFlashAttribute("error", "El enlace es inválido o ha expirado.");
            return "redirect:/password-reset";
        }
        model.addAttribute("token", token);
        model.addAttribute("pageTitle", "Nueva contraseña");
        return "password-reset-confirm";
    }

    @PostMapping("/password-reset/confirm")
    public String confirmPasswordReset(@RequestParam String token, @RequestParam String password,
                                       @RequestParam String confirmPassword,
                                       RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden");
            return "redirect:/password-reset/" + token;
        }
        try {
            passwordResetService.resetPassword(token, password);
            redirectAttributes.addFlashAttribute("success", "¡Contraseña actualizada! Ya puedes iniciar sesión.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/password-reset";
        }
    }
}
