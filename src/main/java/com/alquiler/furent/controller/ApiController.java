package com.alquiler.furent.controller;

import com.alquiler.furent.dto.CotizacionRequest;
import com.alquiler.furent.dto.ProductResponse;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.model.Reservation.ItemReserva;
import com.alquiler.furent.model.User;
import com.alquiler.furent.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "API Pública", description = "Endpoints de la API de Furent")
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    private final ReservationService reservationService;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final ProductService productService;
    private final NotificationService notificationService;
    private final CouponService couponService;

    public ApiController(ReservationService reservationService, UserService userService,
            AuditLogService auditLogService, ProductService productService,
            NotificationService notificationService, CouponService couponService) {
        this.reservationService = reservationService;
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.productService = productService;
        this.notificationService = notificationService;
        this.couponService = couponService;
    }

    // === BÚSQUEDA DE PRODUCTOS ===
    @Operation(summary = "Buscar productos", description = "Busca productos por nombre, descripción, categoría o material")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de productos encontrados"),
            @ApiResponse(responseCode = "400", description = "Parámetro de búsqueda inválido")
    })
    @GetMapping("/productos/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam String q) {
        List<ProductResponse> results = productService.searchProducts(q).stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @GetMapping("/productos/availability")
    public ResponseEntity<List<Map<String, String>>> getBulkAvailability(@RequestParam List<String> ids) {
        List<Reservation> reservations = reservationService.getActiveReservationsByProductIds(ids);
        List<Map<String, String>> occupiedRanges = reservations.stream().map(r -> {
            Map<String, String> range = new HashMap<>();
            range.put("start", r.getFechaInicio() != null ? r.getFechaInicio().toString() : "");
            range.put("end", r.getFechaFin() != null ? r.getFechaFin().plusDays(1).toString() : "");
            
            // Only list products that were actually requested and are in this reservation
            String productNames = r.getItems().stream()
                    .filter(item -> ids.contains(item.getProductoId()))
                    .map(item -> {
                        String name = item.getProductoNombre();
                        // Fallback to fetch from DB if name is null (for older reservations)
                        if (name == null || name.isBlank()) {
                            name = productService.getProductById(item.getProductoId())
                                    .map(com.alquiler.furent.model.Product::getNombre)
                                    .orElse("Producto #" + item.getProductoId());
                        }
                        return name;
                    })
                    .distinct()
                    .collect(Collectors.joining(", "));
            
            range.put("products", productNames.isEmpty() ? "Mobiliario no especificado" : productNames);
            return range;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(occupiedRanges);
    }

    // === FAVORITOS ===
    @Operation(summary = "Agregar favorito", description = "Agrega un producto a la lista de favoritos del usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto agregado a favoritos"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @PostMapping("/favoritos/{productoId}")
    public ResponseEntity<Map<String, Object>> addFavorite(@PathVariable String productoId, Authentication auth) {
        User user = getAuthUser(auth);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));

        if (!user.getFavoritos().contains(productoId)) {
            user.getFavoritos().add(productoId);
            userService.save(user);
        }
        return ResponseEntity.ok(Map.of("success", true, "favorito", true, "totalFavoritos", user.getFavoritos().size()));
    }

    @Operation(summary = "Quitar favorito", description = "Elimina un producto de la lista de favoritos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto eliminado de favoritos"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    @DeleteMapping("/favoritos/{productoId}")
    public ResponseEntity<Map<String, Object>> removeFavorite(@PathVariable String productoId, Authentication auth) {
        User user = getAuthUser(auth);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));

        user.getFavoritos().remove(productoId);
        userService.save(user);
        return ResponseEntity.ok(Map.of("success", true, "favorito", false, "totalFavoritos", user.getFavoritos().size()));
    }

    // === CUPONES ===
    @Operation(summary = "Validar cupón", description = "Valida un código de cupón y calcula el descuento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado de validación del cupón"),
            @ApiResponse(responseCode = "400", description = "Código de cupón o monto inválido")
    })
    @PostMapping("/cupones/validar")
    public ResponseEntity<Map<String, Object>> validateCoupon(@RequestBody Map<String, Object> request) {
        String codigo = (String) request.get("codigo");
        BigDecimal montoTotal = new BigDecimal(((Number) request.get("montoTotal")).toString());
        Map<String, Object> result = couponService.validateCoupon(codigo, montoTotal);
        return ResponseEntity.ok(result);
    }

    // === NOTIFICACIONES ===
    @Operation(summary = "Obtener notificaciones", description = "Lista las notificaciones del usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de notificaciones del usuario"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    @GetMapping("/notificaciones")
    public ResponseEntity<?> getNotifications(Authentication auth) {
        User user = getAuthUser(auth);
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of(
                "notificaciones", notificationService.getRecentNotifications(user.getId()),
                "noLeidas", notificationService.countUnread(user.getId())
        ));
    }

    @Operation(summary = "Marcar notificación como leída", description = "Marca una notificación específica como leída")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificación marcada como leída"),
            @ApiResponse(responseCode = "404", description = "Notificación no encontrada")
    })
    @PostMapping("/notificaciones/{id}/leer")
    public ResponseEntity<?> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @Operation(summary = "Marcar todas las notificaciones como leídas", description = "Marca todas las notificaciones del usuario como leídas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Todas las notificaciones marcadas como leídas"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    @PostMapping("/notificaciones/leer-todas")
    public ResponseEntity<?> markAllAsRead(Authentication auth) {
        User user = getAuthUser(auth);
        if (user == null) return ResponseEntity.status(401).build();
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(Map.of("success", true));
    }

    // === COTIZACIÓN ===
    @Operation(summary = "Crear cotización", description = "Crea una nueva cotización de alquiler de mobiliario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cotización creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de cotización inválidos (fechas, items)"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    @PostMapping("/cotizacion")
    public ResponseEntity<?> submitCotizacion(@Valid @RequestBody CotizacionRequest req, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Debes iniciar sesión para cotizar"));
        }

        User user = userService.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Usuario no encontrado"));
        }

        // Validar fechas
        if (req.getFechaInicio() != null && req.getFechaFin() != null) {
            if (req.getFechaFin().isBefore(req.getFechaInicio())) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message",
                        "La fecha de fin no puede ser anterior a la fecha de inicio"));
            }
        }

        Reservation res = new Reservation();
        res.setUsuarioId(user.getId());
        res.setUsuarioNombre(user.getNombreCompleto());
        res.setUsuarioEmail(user.getEmail());
        res.setTipoEvento(req.getTipoEvento());
        res.setFechaInicio(req.getFechaInicio());
        res.setFechaFin(req.getFechaFin());
        res.setDireccionEvento(req.getDireccion());
        res.setNotasEvento(req.getNotas());
        res.setMetodoPago(req.getMetodoPago());
        res.setEstado("PENDIENTE");

        long days = 1;
        if (req.getFechaInicio() != null && req.getFechaFin() != null) {
            days = ChronoUnit.DAYS.between(req.getFechaInicio(), req.getFechaFin()) + 1;
            if (days < 1) days = 1;
        }
        res.setDiasAlquiler((int) days);

        BigDecimal totalItems = BigDecimal.ZERO;
        if (req.getItems() != null) {
            res.setItems(req.getItems().stream().map(i -> {
                ItemReserva item = new ItemReserva();
                item.setProductoId(i.getId());
                item.setProductoNombre(i.getName());
                item.setProductoImagen(i.getImage());
                item.setPrecioPorDia(i.getPrice());
                item.setCantidad(i.getQty());
                item.setSubtotal(i.getPrice().multiply(BigDecimal.valueOf(i.getQty())));
                return item;
            }).collect(Collectors.toList()));
            totalItems = res.getItems().stream()
                    .map(ItemReserva::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        res.setSubtotal(totalItems);
        BigDecimal totalBruto = totalItems.multiply(BigDecimal.valueOf(days));

        // Aplicar cupón de descuento si se proporcionó
        BigDecimal descuento = BigDecimal.ZERO;
        if (req.getCodigoCupon() != null && !req.getCodigoCupon().isBlank()) {
            Map<String, Object> cuponResult = couponService.validateCoupon(req.getCodigoCupon(), totalBruto);
            if (Boolean.TRUE.equals(cuponResult.get("valido"))) {
                descuento = (BigDecimal) cuponResult.get("descuento");
                res.setCodigoCupon(req.getCodigoCupon().toUpperCase().trim());
                res.setDescuento(descuento);
                couponService.useCoupon(req.getCodigoCupon());
            }
        }
        res.setTotal(totalBruto.subtract(descuento));

        reservationService.save(res);

        auditLogService.log(user.getEmail(), "CREAR_COTIZACION", "RESERVA", res.getId(),
                "Nueva cotización creada por el usuario");

        // Notificación al usuario
        notificationService.notify(user.getId(), "Cotización Creada",
                "Tu cotización ha sido enviada exitosamente y está pendiente de revisión.",
                "SUCCESS", "/panel");

        log.info("Cotización creada por {} - Total: ${}", user.getEmail(), String.format("%,.0f", res.getTotal()));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cotización guardada exitosamente");
        response.put("reservationId", res.getId());
        if (descuento.compareTo(BigDecimal.ZERO) > 0) {
            response.put("descuento", descuento);
            response.put("totalFinal", res.getTotal());
        }
        return ResponseEntity.ok(response);
    }

    private User getAuthUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        return userService.findByEmail(auth.getName()).orElse(null);
    }
}
