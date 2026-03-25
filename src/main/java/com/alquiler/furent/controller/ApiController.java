package com.alquiler.furent.controller;

import com.alquiler.furent.config.FeatureFlags;
import com.alquiler.furent.config.PayUProperties;
import com.alquiler.furent.config.TenantContext;
import com.alquiler.furent.dto.CotizacionRequest;
import com.alquiler.furent.dto.ProductResponse;
import com.alquiler.furent.model.PendingCardPayment;
import com.alquiler.furent.model.Product;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.model.Reservation.ItemReserva;
import com.alquiler.furent.model.User;
import com.alquiler.furent.repository.PendingCardPaymentRepository;
import com.alquiler.furent.repository.ReservationRepository;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api")
@Tag(name = "API Pública", description = "Endpoints de la API de Furent")
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    private static final String CODIGO_EFECTIVO_PREFIX = "FRNT-";
    private static final String CODIGO_EFECTIVO_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODIGO_EFECTIVO_LENGTH = 8;
    private static final ObjectMapper RESERVATION_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final ProductService productService;
    private final NotificationService notificationService;
    private final CouponService couponService;
    private final EmailService emailService;
    private final PendingCardPaymentRepository pendingCardPaymentRepository;
    private final FeatureFlags featureFlags;
    private final PayUProperties payUProperties;

    public ApiController(ReservationService reservationService, ReservationRepository reservationRepository,
            UserService userService, AuditLogService auditLogService, ProductService productService,
            NotificationService notificationService, CouponService couponService, EmailService emailService,
            PendingCardPaymentRepository pendingCardPaymentRepository,
            FeatureFlags featureFlags, PayUProperties payUProperties) {
        this.reservationService = reservationService;
        this.reservationRepository = reservationRepository;
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.productService = productService;
        this.notificationService = notificationService;
        this.couponService = couponService;
        this.emailService = emailService;
        this.pendingCardPaymentRepository = pendingCardPaymentRepository;
        this.featureFlags = featureFlags;
        this.payUProperties = payUProperties;
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
        // Obtener todas las reservas activas de los productos solicitados
        List<Reservation> reservations = reservationService.getActiveReservationsByProductIds(ids);
        
        // Obtener información de stock de cada producto
        Map<String, Integer> productStocks = new HashMap<>();
        Map<String, String> productNames = new HashMap<>();
        for (String productId : ids) {
            productService.getProductById(productId).ifPresent(product -> {
                productStocks.put(productId, product.getStock());
                productNames.put(productId, product.getNombre());
            });
        }
        
        // Agrupar fechas ocupadas por producto
        Map<String, Map<LocalDate, Integer>> reservedByProductAndDate = new HashMap<>();
        
        for (Reservation reservation : reservations) {
            if (reservation.getFechaInicio() == null || reservation.getFechaFin() == null) {
                continue;
            }
            
            for (Reservation.ItemReserva item : reservation.getItems()) {
                if (!ids.contains(item.getProductoId())) {
                    continue;
                }
                
                String productId = item.getProductoId();
                reservedByProductAndDate.putIfAbsent(productId, new HashMap<>());
                Map<LocalDate, Integer> dateMap = reservedByProductAndDate.get(productId);
                
                // Marcar cada fecha del rango con la cantidad reservada
                LocalDate currentDate = reservation.getFechaInicio();
                while (!currentDate.isAfter(reservation.getFechaFin())) {
                    dateMap.merge(currentDate, item.getCantidad(), Integer::sum);
                    currentDate = currentDate.plusDays(1);
                }
            }
        }
        
        // Identificar fechas donde AL MENOS UN producto está completamente agotado
        Map<LocalDate, List<String>> fullyOccupiedDates = new HashMap<>();
        
        for (Map.Entry<String, Map<LocalDate, Integer>> entry : reservedByProductAndDate.entrySet()) {
            String productId = entry.getKey();
            Map<LocalDate, Integer> dateReservations = entry.getValue();
            Integer totalStock = productStocks.get(productId);
            
            if (totalStock == null || totalStock == 0) {
                continue;
            }
            
            for (Map.Entry<LocalDate, Integer> dateEntry : dateReservations.entrySet()) {
                LocalDate date = dateEntry.getKey();
                Integer reserved = dateEntry.getValue();
                
                // Si este producto está completamente agotado en esta fecha
                if (reserved >= totalStock) {
                    fullyOccupiedDates.putIfAbsent(date, new ArrayList<>());
                    fullyOccupiedDates.get(date).add(productNames.getOrDefault(productId, "Producto #" + productId));
                }
            }
        }
        
        // Convertir fechas ocupadas a rangos continuos
        List<Map<String, String>> occupiedRanges = new ArrayList<>();
        if (fullyOccupiedDates.isEmpty()) {
            return ResponseEntity.ok(occupiedRanges);
        }
        
        List<LocalDate> sortedDates = fullyOccupiedDates.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
        
        LocalDate rangeStart = null;
        LocalDate previousDate = null;
        List<String> rangeProducts = new ArrayList<>();
        
        for (LocalDate date : sortedDates) {
            if (rangeStart == null) {
                rangeStart = date;
                previousDate = date;
                rangeProducts = new ArrayList<>(fullyOccupiedDates.get(date));
            } else if (date.equals(previousDate.plusDays(1))) {
                // Continuar el rango
                previousDate = date;
                // Agregar productos de esta fecha
                for (String product : fullyOccupiedDates.get(date)) {
                    if (!rangeProducts.contains(product)) {
                        rangeProducts.add(product);
                    }
                }
            } else {
                // Cerrar rango anterior
                Map<String, String> range = new HashMap<>();
                range.put("start", rangeStart.toString());
                range.put("end", previousDate.plusDays(1).toString());
                range.put("products", String.join(", ", rangeProducts));
                occupiedRanges.add(range);
                
                // Iniciar nuevo rango
                rangeStart = date;
                previousDate = date;
                rangeProducts = new ArrayList<>(fullyOccupiedDates.get(date));
            }
        }
        
        // Cerrar último rango
        if (rangeStart != null) {
            Map<String, String> range = new HashMap<>();
            range.put("start", rangeStart.toString());
            range.put("end", previousDate.plusDays(1).toString());
            range.put("products", String.join(", ", rangeProducts));
            occupiedRanges.add(range);
        }
        
        return ResponseEntity.ok(occupiedRanges);
    }

    @GetMapping("/productos/{id}/stock-disponible")
    public ResponseEntity<Map<String, Object>> getAvailableStock(
            @PathVariable String id,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        
        Optional<Product> productOpt = productService.getProductById(id);
        if (productOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Product product = productOpt.get();
        int totalStock = product.getStock();
        
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        
        // Obtener TODAS las reservas activas del producto
        List<Reservation> activeReservations = reservationService.getActiveReservationsByProductId(id);
        
        log.info("Consultando stock disponible para producto {} del {} al {}", id, startDate, endDate);
        log.info("Stock total: {}, Reservas activas encontradas: {}", totalStock, activeReservations.size());
        
        // Calcular stock disponible mínimo en el rango de fechas
        int minAvailableStock = totalStock;
        LocalDate currentDate = start;
        
        while (!currentDate.isAfter(end)) {
            final LocalDate checkDate = currentDate;
            
            // Sumar cantidades reservadas para esta fecha específica
            int reservedOnDate = 0;
            for (Reservation reservation : activeReservations) {
                if (reservation.getFechaInicio() == null || reservation.getFechaFin() == null) {
                    continue;
                }
                
                // Verificar si la fecha actual está dentro del rango de la reserva
                if (!checkDate.isBefore(reservation.getFechaInicio()) && !checkDate.isAfter(reservation.getFechaFin())) {
                    // Sumar solo las cantidades de este producto específico
                    for (Reservation.ItemReserva item : reservation.getItems()) {
                        if (item.getProductoId().equals(id)) {
                            reservedOnDate += item.getCantidad();
                            log.debug("Fecha {}: Reserva {} tiene {} unidades", checkDate, reservation.getId(), item.getCantidad());
                        }
                    }
                }
            }
            
            int availableOnDate = totalStock - reservedOnDate;
            log.debug("Fecha {}: Reservadas={}, Disponibles={}", checkDate, reservedOnDate, availableOnDate);
            
            if (availableOnDate < minAvailableStock) {
                minAvailableStock = availableOnDate;
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        log.info("Stock mínimo disponible en el rango: {}", minAvailableStock);
        
        Map<String, Object> response = new HashMap<>();
        response.put("productId", id);
        response.put("totalStock", totalStock);
        response.put("availableStock", Math.max(0, minAvailableStock));
        response.put("startDate", startDate);
        response.put("endDate", endDate);
        
        return ResponseEntity.ok(response);
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

        String metodoPago = req.getMetodoPago();
        if (metodoPago != null && (metodoPago.equals("TARJETA") || metodoPago.equals("Credit Card"))) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Para pagar con tarjeta usa el botón «Pagar con tarjeta». La reserva se creará solo cuando el pago sea exitoso."
            ));
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
        res.setHoraEntrega(req.getHoraEntrega());
        res.setEstado("PENDIENTE");

        long days = 1;
        if (req.getFechaInicio() != null && req.getFechaFin() != null) {
            days = ChronoUnit.DAYS.between(req.getFechaInicio(), req.getFechaFin()) + 1;
            if (days < 1) days = 1;
        }
        res.setDiasAlquiler((int) days);

        // Build items with PRICES FROM DATABASE (never trust frontend prices)
        BigDecimal totalItems = BigDecimal.ZERO;
        if (req.getItems() != null) {
            java.util.List<ItemReserva> items = new java.util.ArrayList<>();
            for (CotizacionRequest.CartItem ci : req.getItems()) {
                var productOpt = productService.getProductById(ci.getId());
                if (productOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message",
                            "El producto '" + ci.getName() + "' ya no existe en el catálogo."));
                }
                var product = productOpt.get();
                ItemReserva item = new ItemReserva();
                item.setProductoId(product.getId());
                item.setProductoNombre(product.getNombre());
                item.setProductoImagen(product.getImagenUrl());
                // Use DB price, NOT frontend price
                item.setPrecioPorDia(product.getPrecioPorDia());
                item.setCantidad(ci.getQty());
                item.setSubtotal(product.getPrecioPorDia().multiply(BigDecimal.valueOf(ci.getQty())));
                items.add(item);
                totalItems = totalItems.add(item.getSubtotal());
            }
            res.setItems(items);
        }

        res.setSubtotal(totalItems);
        BigDecimal totalBruto = totalItems.multiply(BigDecimal.valueOf(days));

        // === ANTI-OVERBOOKING: Validate stock availability for requested dates ===
        Map<String, String> availabilityErrors = reservationService.validateAvailability(res);
        if (!availabilityErrors.isEmpty()) {
            String errorMessage = String.join(" | ", availabilityErrors.values());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Stock insuficiente para las fechas seleccionadas.");
            errorResponse.put("stockErrors", availabilityErrors);
            errorResponse.put("details", errorMessage);
            return ResponseEntity.status(409).body(errorResponse);
        }

        // Aplicar cupón de descuento si se proporcionó
        BigDecimal descuento = BigDecimal.ZERO;
        String couponToConsume = null;
        if (req.getCodigoCupon() != null && !req.getCodigoCupon().isBlank()) {
            Map<String, Object> cuponResult = couponService.validateCoupon(req.getCodigoCupon(), totalBruto);
            if (Boolean.TRUE.equals(cuponResult.get("valido"))) {
                descuento = (BigDecimal) cuponResult.get("descuento");
                res.setCodigoCupon(req.getCodigoCupon().toUpperCase().trim());
                res.setDescuento(descuento);
                couponToConsume = req.getCodigoCupon(); // Defer usage until after save
            }
        }
        res.setTotal(totalBruto.subtract(descuento));

        if ("Efectivo en Oficina".equals(metodoPago)) {
            String codigo = generarCodigoPagoEfectivoUnico();
            res.setCodigoPagoEfectivo(codigo);
        }

        reservationService.save(res);

        if (couponToConsume != null) {
            couponService.useCoupon(couponToConsume);
        }

        auditLogService.log(user.getEmail(), "CREAR_COTIZACION", "RESERVA", res.getId(),
                "Nueva cotización creada por el usuario");

        notificationService.notify(user.getId(), "Cotización Creada",
                "Tu cotización ha sido enviada exitosamente y está pendiente de revisión.",
                "SUCCESS", "/panel");

        if (user.isNotificacionesEmail()) {
            emailService.sendReservationConfirmation(user.getEmail(), res.getId(), res.getTotal());
            if (res.getCodigoPagoEfectivo() != null) {
                emailService.sendCashPaymentCodeEmail(user.getEmail(), user.getNombreCompleto(),
                        res.getCodigoPagoEfectivo(), res.getTotal(), res.getId());
            }
        }

        log.info("Cotización creada por {} - Total: ${}", user.getEmail(), String.format("%,.0f", res.getTotal()));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cotización guardada exitosamente");
        response.put("reservationId", res.getId());
        if (res.getCodigoPagoEfectivo() != null) {
            response.put("codigoPagoEfectivo", res.getCodigoPagoEfectivo());
        }
        if (descuento.compareTo(BigDecimal.ZERO) > 0) {
            response.put("descuento", descuento);
            response.put("totalFinal", res.getTotal());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cotizacion/iniciar-pago-tarjeta")
    public ResponseEntity<?> iniciarPagoTarjeta(@Valid @RequestBody CotizacionRequest req, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Debes iniciar sesión"));
        }
        User user = userService.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Usuario no encontrado"));
        }
        if (!featureFlags.isPayuEnabled() || !payUProperties.isConfigured()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "El pago con tarjeta no está disponible porque no se han configurado las claves de PayU."));
        }

        if (req.getFechaInicio() != null && req.getFechaFin() != null && req.getFechaFin().isBefore(req.getFechaInicio())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "La fecha de fin no puede ser anterior a la de inicio."));
        }

        Reservation res = new Reservation();
        res.setUsuarioId(user.getId());
        res.setUsuarioNombre(user.getNombreCompleto());
        res.setUsuarioEmail(user.getEmail());
        res.setTenantId(TenantContext.getCurrentTenant() != null ? TenantContext.getCurrentTenant() : "default");
        res.setTipoEvento(req.getTipoEvento());
        res.setFechaInicio(req.getFechaInicio());
        res.setFechaFin(req.getFechaFin());
        res.setDireccionEvento(req.getDireccion());
        res.setNotasEvento(req.getNotas());
        res.setMetodoPago("TARJETA");
        res.setHoraEntrega(req.getHoraEntrega());
        res.setEstado("PENDIENTE");

        long days = 1;
        if (req.getFechaInicio() != null && req.getFechaFin() != null) {
            days = ChronoUnit.DAYS.between(req.getFechaInicio(), req.getFechaFin()) + 1;
            if (days < 1) days = 1;
        }
        res.setDiasAlquiler((int) days);

        BigDecimal totalItems = BigDecimal.ZERO;
        if (req.getItems() != null) {
            java.util.List<ItemReserva> items = new java.util.ArrayList<>();
            for (CotizacionRequest.CartItem ci : req.getItems()) {
                var productOpt = productService.getProductById(ci.getId());
                if (productOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", "El producto '" + ci.getName() + "' ya no existe."));
                }
                var product = productOpt.get();
                ItemReserva item = new ItemReserva();
                item.setProductoId(product.getId());
                item.setProductoNombre(product.getNombre());
                item.setProductoImagen(product.getImagenUrl());
                item.setPrecioPorDia(product.getPrecioPorDia());
                item.setCantidad(ci.getQty());
                item.setSubtotal(product.getPrecioPorDia().multiply(BigDecimal.valueOf(ci.getQty())));
                items.add(item);
                totalItems = totalItems.add(item.getSubtotal());
            }
            res.setItems(items);
        }
        res.setSubtotal(totalItems);
        BigDecimal totalBruto = totalItems.multiply(BigDecimal.valueOf(days));

        Map<String, String> availabilityErrors = reservationService.validateAvailability(res);
        if (!availabilityErrors.isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Stock insuficiente para las fechas seleccionadas.");
            err.put("stockErrors", availabilityErrors);
            return ResponseEntity.status(409).body(err);
        }

        BigDecimal descuento = BigDecimal.ZERO;
        if (req.getCodigoCupon() != null && !req.getCodigoCupon().isBlank()) {
            Map<String, Object> cuponResult = couponService.validateCoupon(req.getCodigoCupon(), totalBruto);
            if (Boolean.TRUE.equals(cuponResult.get("valido"))) {
                descuento = (BigDecimal) cuponResult.get("descuento");
                res.setCodigoCupon(req.getCodigoCupon().toUpperCase().trim());
                res.setDescuento(descuento);
            }
        }
        res.setTotal(totalBruto.subtract(descuento));

        try {
            String reservationJson = RESERVATION_MAPPER.writeValueAsString(res);
            PendingCardPayment pending = new PendingCardPayment();
            pending.setUsuarioId(user.getId());
            pending.setTenantId(res.getTenantId());
            pending.setReservationDataJson(reservationJson);
            pending.setTotal(res.getTotal());
            pending = pendingCardPaymentRepository.save(pending);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("pendingId", pending.getId());
            response.put("total", res.getTotal());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error iniciar pago tarjeta: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Error al preparar el pago: " + e.getMessage()));
        }
    }

    private String generarCodigoPagoEfectivoUnico() {
        Random r = new Random();
        for (int intentos = 0; intentos < 50; intentos++) {
            StringBuilder sb = new StringBuilder(CODIGO_EFECTIVO_PREFIX);
            for (int i = 0; i < CODIGO_EFECTIVO_LENGTH; i++) {
                sb.append(CODIGO_EFECTIVO_CHARS.charAt(r.nextInt(CODIGO_EFECTIVO_CHARS.length())));
            }
            String codigo = sb.toString();
            if (reservationRepository.findByCodigoPagoEfectivo(codigo).isEmpty()) {
                return codigo;
            }
        }
        return CODIGO_EFECTIVO_PREFIX + System.currentTimeMillis();
    }

    private User getAuthUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        return userService.findByEmail(auth.getName()).orElse(null);
    }
}
