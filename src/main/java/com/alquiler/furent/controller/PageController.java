package com.alquiler.furent.controller;

import com.alquiler.furent.model.Product;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.model.User;
import com.alquiler.furent.model.UserAddress;
import com.alquiler.furent.model.Review;
import com.alquiler.furent.repository.PendingCardPaymentRepository;
import com.alquiler.furent.config.PayUProperties;
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
import jakarta.servlet.http.HttpServletRequest;

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
    private final PendingCardPaymentRepository pendingCardPaymentRepository;
    private final PayUService payUService;
    private final PayUProperties payUProperties;
    private final EmailService emailService;
    private final TotpService totpService;

    public PageController(ProductService productService, ReservationService reservationService,
            UserService userService, PasswordEncoder passwordEncoder, ReviewService reviewService,
            ContactService contactService, NotificationService notificationService,
            PasswordResetService passwordResetService, PendingCardPaymentRepository pendingCardPaymentRepository,
            PayUService payUService, PayUProperties payUProperties, EmailService emailService,
            TotpService totpService) {
        this.productService = productService;
        this.reservationService = reservationService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.reviewService = reviewService;
        this.contactService = contactService;
        this.notificationService = notificationService;
        this.passwordResetService = passwordResetService;
        this.pendingCardPaymentRepository = pendingCardPaymentRepository;
        this.payUService = payUService;
        this.payUProperties = payUProperties;
        this.emailService = emailService;
        this.totpService = totpService;
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
        // Compute real product counts per category
        List<com.alquiler.furent.model.Category> categories = productService.getAllCategories();
        List<Product> allProducts = productService.getAllProducts();
        Map<String, Long> countsByCategory = allProducts.stream()
                .filter(p -> p.getCategoriaNombre() != null)
                .collect(Collectors.groupingBy(Product::getCategoriaNombre, Collectors.counting()));
        for (com.alquiler.furent.model.Category cat : categories) {
            cat.setCantidadProductos(countsByCategory.getOrDefault(cat.getNombre(), 0L).intValue());
        }
        model.addAttribute("categories", categories);

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
            Product p = product.get();
            model.addAttribute("product", p);

            // Related products: ONLY same category, excluding the current product
            List<Product> related = productService.getProductsByCategory(p.getCategoriaNombre())
                    .stream()
                    .filter(rp -> !rp.getId().equals(id))
                    .limit(4)
                    .collect(Collectors.toList());
            model.addAttribute("relatedProducts", related);

            // Load reviews
            List<Review> reviews = reviewService.getReviewsByProduct(id);
            model.addAttribute("reviews", reviews);

            // Calculate average rating from real reviews (0 if no reviews)
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
                        .filter(r -> "ENTREGADA".equals(r.getEstado()) || "CONFIRMADA".equals(r.getEstado()))
                        .collect(Collectors.toList());
                pending = userRes.stream().filter(r -> "PENDIENTE".equals(r.getEstado()))
                        .collect(Collectors.toList());
                completed = userRes.stream().filter(r -> "COMPLETADA".equals(r.getEstado()))
                        .collect(Collectors.toList());

                BigDecimal totalGastado = userRes.stream()
                        .filter(r -> "COMPLETADA".equals(r.getEstado()) || "ENTREGADA".equals(r.getEstado())
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

    @GetMapping("/pago/tarjeta")
    public String pagoTarjeta(@RequestParam(required = false) String reservaId,
                              @RequestParam(required = false) String pendingId,
                              Model model, Authentication auth, HttpServletRequest request) {
        if (auth == null || !auth.isAuthenticated()) {
            String q = pendingId != null ? "pendingId=" + pendingId : (reservaId != null ? "reservaId=" + reservaId : "");
            return "redirect:/login?redirect=/pago/tarjeta" + (q.isEmpty() ? "" : "?" + q);
        }
        Optional<User> userOpt = userService.findByEmail(auth.getName());
        if (userOpt.isEmpty()) return "redirect:/login";

        if (pendingId != null && (reservaId == null || reservaId.isBlank())) {
            var pendingOpt = pendingCardPaymentRepository.findById(pendingId);
            if (pendingOpt.isEmpty() || !pendingOpt.get().getUsuarioId().equals(userOpt.get().getId())) {
                return "redirect:/panel";
            }
            var pending = pendingOpt.get();
            model.addAttribute("pendingId", pendingId);
            model.addAttribute("total", pending.getTotal());
            model.addAttribute("reservaId", null);
            model.addAttribute("reserva", null);
        } else if (reservaId != null && !reservaId.isBlank()) {
            Optional<Reservation> resOpt = reservationService.getById(reservaId);
            if (resOpt.isEmpty() || !resOpt.get().getUsuarioId().equals(userOpt.get().getId())) {
                return "redirect:/panel";
            }
            model.addAttribute("reservaId", reservaId);
            model.addAttribute("reserva", resOpt.get());
            model.addAttribute("pendingId", null);
            model.addAttribute("total", resOpt.get().getTotal());
        } else {
            return "redirect:/panel";
        }

        // Configuración para PayU
        String referenceCode = pendingId != null ? pendingId : reservaId;
        BigDecimal total = (BigDecimal) model.getAttribute("total");
        String signature = payUService.generateSignature(referenceCode, total, payUProperties.getCurrency());
        String amountFormatted = payUService.getAmountFormatted(total);
        
        // Generar URLs absolutas
        String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), request.getContextPath());
        String responseUrl = baseUrl + "/pago/resultado";
        String confirmationUrl = baseUrl + "/api/pagos/payu/confirmacion";

        model.addAttribute("payuUrl", payUProperties.getUrl());
        model.addAttribute("payuApiKey", payUProperties.getApiKey());
        model.addAttribute("payuMerchantId", payUProperties.getMerchantId());
        model.addAttribute("payuAccountId", payUProperties.getAccountId());
        model.addAttribute("payuCurrency", payUProperties.getCurrency());
        model.addAttribute("payuReferenceCode", referenceCode);
        model.addAttribute("payuAmount", amountFormatted);
        model.addAttribute("payuSignature", signature);
        model.addAttribute("payuTest", payUProperties.getTest());
        model.addAttribute("payuBuyerEmail", userOpt.get().getEmail());
        model.addAttribute("payuBuyerFullName", userOpt.get().getNombreCompleto());
        model.addAttribute("payuResponseUrl", responseUrl);
        model.addAttribute("payuConfirmationUrl", confirmationUrl);

        log.info("Iniciando pago tarjeta: referenceCode={}, total={}, payuUrl={}", 
                referenceCode, total, payUProperties.getUrl());
        log.info("URLs de retorno: responseUrl={}, confirmationUrl={}", responseUrl, confirmationUrl);

        model.addAttribute("pageTitle", "Pago con tarjeta");
        return "pago-tarjeta";
    }

    @GetMapping("/pago/resultado")
    public String pagoResultado(@RequestParam Map<String, String> params, Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";
        
        String state = params.get("transactionState");
        String reference = params.get("referenceCode");
        String transactionId = params.get("transactionId");
        String message = params.get("message");
        String txValue = params.get("TX_VALUE");
        
        model.addAttribute("state", state);
        model.addAttribute("reference", reference);
        model.addAttribute("transactionId", transactionId);
        model.addAttribute("message", message);
        model.addAttribute("txValue", txValue);
        
        // Determinar el mensaje amigable
        String statusTitle = "Procesando pago";
        String statusClass = "text-surface-500";
        String statusIcon = "clock";
        
        if ("4".equals(state)) {
            statusTitle = "¡Pago exitoso!";
            statusClass = "text-green-600";
            statusIcon = "check-circle";
        } else if ("6".equals(state)) {
            statusTitle = "Pago rechazado";
            statusClass = "text-red-600";
            statusIcon = "x-circle";
        } else if ("7".equals(state)) {
            statusTitle = "Pago pendiente";
            statusClass = "text-amber-600";
            statusIcon = "clock";
        }
        
        model.addAttribute("statusTitle", statusTitle);
        model.addAttribute("statusClass", statusClass);
        model.addAttribute("statusIcon", statusIcon);
        
        return "payment-result";
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
        
        try {
            emailService.sendContactNotification("valdeslastresjosedaniel@gmail.com", 
                    nombre, email, asunto, mensaje, msg.getTelefono());
        } catch (Exception e) {
            log.error("No se pudo enviar notificacion de contacto por email: {}", e.getMessage());
        }
        
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
    public String settings(Model model, Authentication auth) {
        model.addAttribute("pageTitle", "Configuración");
        if (auth != null) {
            Optional<User> optUser = userService.findByEmail(auth.getName());
            optUser.ifPresent(user -> model.addAttribute("currentUser", user));
        }
        return "settings";
    }

    @PostMapping("/configuracion/perfil")
    public String updateProfile(@RequestParam String nombre, @RequestParam String apellido,
            Authentication auth, RedirectAttributes redirectAttributes) {
        if (auth != null) {
            Optional<User> optUser = userService.findByEmail(auth.getName());
            if (optUser.isPresent()) {
                User user = optUser.get();
                user.setNombre(nombre);
                user.setApellido(apellido);
                userService.save(user);
                redirectAttributes.addFlashAttribute("success", "Perfil básico actualizado correctamente");
            }
        }
        return "redirect:/configuracion";
    }

    @PostMapping("/configuracion/personal")
    public String updatePersonal(@RequestParam(required = false) String telefono,
            @RequestParam(required = false) String tipoDocumento,
            @RequestParam(required = false) String documentoIdentidad,
            @RequestParam(required = false) String fechaNacimiento,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) String estadoCivil,
            @RequestParam(required = false) String empresa,
            @RequestParam(required = false) String cargo,
            Authentication auth, RedirectAttributes redirectAttributes) {
        if (auth != null) {
            Optional<User> optUser = userService.findByEmail(auth.getName());
            if (optUser.isPresent()) {
                User user = optUser.get();
                user.setTelefono(telefono);
                user.setTipoDocumento(tipoDocumento);
                user.setDocumentoIdentidad(documentoIdentidad);
                user.setGenero(genero);
                user.setEstadoCivil(estadoCivil);
                user.setEmpresa(empresa);
                user.setCargo(cargo);

                if (fechaNacimiento != null && !fechaNacimiento.trim().isEmpty()) {
                    try {
                        user.setFechaNacimiento(java.time.LocalDate.parse(fechaNacimiento));
                    } catch (Exception e) {
                        log.warn("Formato de fecha invalido: {}", fechaNacimiento);
                    }
                } else {
                    user.setFechaNacimiento(null);
                }

                userService.save(user);
                redirectAttributes.addFlashAttribute("successPersonal", "Información personal actualizada");
            }
        }
        return "redirect:/configuracion";
    }

    @PostMapping("/configuracion/direccion/guardar")
    public String saveAddress(@RequestParam String alias, @RequestParam String direccion,
                              @RequestParam String ciudad, @RequestParam String estadoProvincia,
                              @RequestParam String codigoPostal, @RequestParam String pais,
                              @RequestParam(required = false) boolean isPredeterminada,
                              Authentication auth, RedirectAttributes redirectAttributes) {
        if (auth != null) {
            Optional<User> optUser = userService.findByEmail(auth.getName());
            if (optUser.isPresent()) {
                User user = optUser.get();
                if (user.getDirecciones() == null) {
                    user.setDirecciones(new ArrayList<>());
                }
                
                // Si la nueva es por defecto, desmarcar las demas
                if (isPredeterminada || user.getDirecciones().isEmpty()) {
                    user.getDirecciones().forEach(d -> d.setPredeterminada(false));
                    isPredeterminada = true;
                }

                UserAddress addr = new UserAddress(alias, direccion, ciudad, estadoProvincia, pais, codigoPostal, isPredeterminada);
                user.getDirecciones().add(addr);
                userService.save(user);
                redirectAttributes.addFlashAttribute("success", "Dirección agregada correctamente");
            }
        }
        return "redirect:/configuracion";
    }

    @PostMapping("/configuracion/direccion/eliminar/{id}")
    public String deleteAddress(@PathVariable String id, Authentication auth, RedirectAttributes redirectAttributes) {
        if (auth != null) {
            Optional<User> optUser = userService.findByEmail(auth.getName());
            if (optUser.isPresent()) {
                User user = optUser.get();
                if (user.getDirecciones() != null) {
                    boolean removed = user.getDirecciones().removeIf(d -> d.getId().equals(id));
                    if (removed) {
                        // Ensure at least one default if we didn't remove the last one
                        if (!user.getDirecciones().isEmpty() && user.getDirecciones().stream().noneMatch(UserAddress::isPredeterminada)) {
                            user.getDirecciones().get(0).setPredeterminada(true);
                        }
                        userService.save(user);
                        redirectAttributes.addFlashAttribute("success", "Dirección eliminada");
                    }
                }
            }
        }
        return "redirect:/configuracion";
    }

    @PostMapping("/configuracion/password")
    public String updatePassword(@RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication auth, RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
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
                // 3-of-5 password rules
                int score = 0;
                if (newPassword.length() >= 8) score++;
                if (newPassword.chars().anyMatch(Character::isUpperCase)) score++;
                if (newPassword.chars().anyMatch(Character::isLowerCase)) score++;
                if (newPassword.chars().anyMatch(Character::isDigit)) score++;
                if (newPassword.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) score++;
                if (score < 3) {
                    redirectAttributes.addFlashAttribute("passwordError",
                            "La contraseña es muy débil. Incluye mayúsculas, minúsculas, números y caracteres especiales.");
                    return "redirect:/configuracion";
                }
                user.setPassword(passwordEncoder.encode(newPassword));
                userService.save(user);
                // Send security alert email
                try {
                    String baseUrl = request.getScheme() + "://" + request.getServerName()
                            + (request.getServerPort() != 80 && request.getServerPort() != 443
                                ? ":" + request.getServerPort() : "");
                    var token = passwordResetService.createToken(user.getEmail());
                    String resetUrl = baseUrl + "/password-reset/" + token.getToken();
                    emailService.sendPasswordChangedEmail(user.getEmail(), user.getNombre(), resetUrl);
                } catch (Exception ex) {
                    log.warn("No se pudo enviar email de confirmación de cambio de contraseña: {}", ex.getMessage());
                }
                redirectAttributes.addFlashAttribute("success", "Contraseña actualizada correctamente");
            }
        }
        return "redirect:/configuracion";
    }

    @PostMapping("/configuracion/cerrar-sesiones")
    public String cerrarSesiones(@RequestParam String password,
            Authentication auth, RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        if (auth != null) {
            Optional<User> optUser = userService.findByEmail(auth.getName());
            if (optUser.isPresent()) {
                User user = optUser.get();
                if (!passwordEncoder.matches(password, user.getPassword())) {
                    redirectAttributes.addFlashAttribute("sessionError", "Contraseña incorrecta");
                    return "redirect:/configuracion";
                }
                // Invalidate current session — Spring Security Remember-Me tokens
                // are stored per-user; clearing them forces re-login on other devices.
                try {
                    var session = request.getSession(false);
                    if (session != null) session.invalidate();
                } catch (Exception ignored) {}
                redirectAttributes.addFlashAttribute("success", "Sesiones cerradas en otros dispositivos. Por favor inicia sesión de nuevo.");
                return "redirect:/login?manual=true";
            }
        }
        return "redirect:/configuracion";
    }

    @PostMapping("/configuracion/eliminar-cuenta")
    public String eliminarCuenta(@RequestParam String password,
            Authentication auth, RedirectAttributes redirectAttributes) {
        if (auth != null) {
            Optional<User> optUser = userService.findByEmail(auth.getName());
            if (optUser.isPresent()) {
                User user = optUser.get();
                if (!passwordEncoder.matches(password, user.getPassword())) {
                    redirectAttributes.addFlashAttribute("deleteError", "Contraseña incorrecta. No se eliminó la cuenta.");
                    return "redirect:/configuracion";
                }
                userService.deleteUser(user.getId());
                log.info("Cuenta eliminada por el usuario: {}", user.getEmail());
                return "redirect:/login?manual=true";
            }
        }
        return "redirect:/configuracion";
    }

    // === 2FA TOTP ===

    @GetMapping("/configuracion/2fa/setup")
    public String setup2fa(Authentication auth, Model model) {
        if (auth == null) return "redirect:/login";
        Optional<User> optUser = userService.findByEmail(auth.getName());
        if (optUser.isEmpty()) return "redirect:/configuracion";
        User user = optUser.get();
        String secret = totpService.generateSecret();
        String qrUri = totpService.generateQrDataUri(user.getEmail(), secret);
        model.addAttribute("totpSecret", secret);
        model.addAttribute("totpQr", qrUri);
        model.addAttribute("pageTitle", "Configurar 2FA");
        return "settings-2fa-setup";
    }

    @PostMapping("/configuracion/2fa/enable")
    public String enable2fa(@RequestParam String secret, @RequestParam String code,
            Authentication auth, RedirectAttributes redirectAttributes) {
        if (auth == null) return "redirect:/login";
        Optional<User> optUser = userService.findByEmail(auth.getName());
        if (optUser.isEmpty()) return "redirect:/configuracion";
        User user = optUser.get();
        if (!totpService.verifyCode(secret, code)) {
            redirectAttributes.addFlashAttribute("totpError", "Código incorrecto. Inténtalo de nuevo.");
            return "redirect:/configuracion/2fa/setup";
        }
        user.setTotpSecret(secret);
        user.setTotpEnabled(true);
        userService.save(user);
        redirectAttributes.addFlashAttribute("success", "¡Autenticación de dos pasos activada exitosamente!");
        return "redirect:/configuracion";
    }

    @PostMapping("/configuracion/2fa/disable")
    public String disable2fa(@RequestParam String code,
            Authentication auth, RedirectAttributes redirectAttributes) {
        if (auth == null) return "redirect:/login";
        Optional<User> optUser = userService.findByEmail(auth.getName());
        if (optUser.isEmpty()) return "redirect:/configuracion";
        User user = optUser.get();
        if (!totpService.verifyCode(user.getTotpSecret(), code)) {
            redirectAttributes.addFlashAttribute("totpDisableError", "Código incorrecto. 2FA no desactivado.");
            return "redirect:/configuracion";
        }
        user.setTotpSecret(null);
        user.setTotpEnabled(false);
        userService.save(user);
        redirectAttributes.addFlashAttribute("success", "Autenticación de dos pasos desactivada.");
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

    @PostMapping("/configuracion/notificaciones")
    public String updateNotificaciones(@RequestParam(required = false) String notificacionesEmail,
            Authentication auth, RedirectAttributes redirectAttributes) {
        if (auth != null) {
            Optional<User> optUser = userService.findByEmail(auth.getName());
            if (optUser.isPresent()) {
                User user = optUser.get();
                user.setNotificacionesEmail("on".equals(notificacionesEmail));
                userService.save(user);
                redirectAttributes.addFlashAttribute("success", "Preferencias de notificación actualizadas");
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
    public String requestPasswordReset(@RequestParam String email, RedirectAttributes redirectAttributes,
                                       HttpServletRequest request) {
        try {
            var token = passwordResetService.createToken(email);
            // Build absolute reset URL
            String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + (request.getServerPort() != 80 && request.getServerPort() != 443
                        ? ":" + request.getServerPort() : "");
            String resetUrl = baseUrl + "/password-reset/" + token.getToken();
            log.info("Enlace de recuperación generado para {}: {}", email, resetUrl);

            // Send the email
            emailService.sendPasswordResetEmail(email, resetUrl);
            log.info("Email de recuperación enviado exitosamente a: {}", email);

            redirectAttributes.addFlashAttribute("success",
                    "Te hemos enviado un enlace de recuperación. Revisa tu bandeja de entrada y spam.");
        } catch (RuntimeException e) {
            log.error("Error en recuperación de contraseña para {}: {}", email, e.getMessage(), e);
            // Don't reveal if email exists or not - always show same message
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
                                       RedirectAttributes redirectAttributes,
                                       HttpServletRequest request) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden");
            return "redirect:/password-reset/" + token;
        }
        try {
            // Find user before resetting so we have the email
            var tokenOpt = passwordResetService.findByToken(token);
            passwordResetService.resetPassword(token, password);
            // Send success / security alert email
            if (tokenOpt.isPresent()) {
                try {
                    String userEmail = tokenOpt.get().getUserId() != null
                        ? userService.findById(tokenOpt.get().getUserId()).map(u -> u.getEmail()).orElse(null)
                        : null;
                    if (userEmail != null) {
                        String nombre = userService.findByEmail(userEmail).map(u -> u.getNombre()).orElse("Usuario");
                        String baseUrl = request.getScheme() + "://" + request.getServerName()
                                + (request.getServerPort() != 80 && request.getServerPort() != 443
                                    ? ":" + request.getServerPort() : "");
                        var newToken = passwordResetService.createToken(userEmail);
                        String resetUrl = baseUrl + "/password-reset/" + newToken.getToken();
                        emailService.sendPasswordChangedEmail(userEmail, nombre, resetUrl);
                    }
                } catch (Exception ex) {
                    log.warn("No se pudo enviar email de confirmación tras reset: {}", ex.getMessage());
                }
            }
            redirectAttributes.addFlashAttribute("success", "¡Contraseña actualizada! Ya puedes iniciar sesión.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/password-reset";
        }
    }
}
