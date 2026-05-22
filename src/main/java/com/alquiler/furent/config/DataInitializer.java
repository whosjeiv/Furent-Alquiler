package com.alquiler.furent.config;

import com.alquiler.furent.model.Category;
import com.alquiler.furent.model.Permission;
import com.alquiler.furent.model.Product;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.model.Review;
import com.alquiler.furent.repository.CategoryRepository;
import com.alquiler.furent.repository.PermissionRepository;
import com.alquiler.furent.repository.ProductRepository;
import com.alquiler.furent.repository.ReservationRepository;
import com.alquiler.furent.repository.ReviewRepository;
import com.alquiler.furent.service.TenantService;
import com.alquiler.furent.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class DataInitializer implements CommandLineRunner {

        private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

        private final ProductRepository productRepository;
        private final CategoryRepository categoryRepository;
        private final ReviewRepository reviewRepository;
        private final ReservationRepository reservationRepository;
        private final UserService userService;
        private final TenantService tenantService;
        private final PermissionRepository permissionRepository;

        @Value("${furent.admin.password:admin123}")
        private String adminPassword;

        public DataInitializer(ProductRepository productRepository, CategoryRepository categoryRepository,
                        ReviewRepository reviewRepository, ReservationRepository reservationRepository,
                        UserService userService, TenantService tenantService,
                        PermissionRepository permissionRepository) {
                this.productRepository = productRepository;
                this.categoryRepository = categoryRepository;
                this.reviewRepository = reviewRepository;
                this.reservationRepository = reservationRepository;
                this.userService = userService;
                this.tenantService = tenantService;
                this.permissionRepository = permissionRepository;
        }

        @Override
        public void run(String... args) throws Exception {
                // Crear tenant default
                tenantService.createDefaultTenant();
                log.info("=== Tenant default inicializado ===");

                // Inicializar permisos RBAC
                initializePermissions();
                log.info("=== Permisos RBAC inicializados ===");

                // Create default admin with configurable password
                userService.createAdmin("admin@furent.com", adminPassword, "Admin", "Furent");
                log.info("=== Admin inicializado: admin@furent.com ===");

                // Only seed if collections are empty
                if (categoryRepository.count() == 0) {
                        seedCategories();
                        log.info("Categorías seed creadas");
                }
                if (productRepository.count() == 0) {
                        seedProducts();
                        log.info("Productos seed creados");
                }

                // Seed reservations for predictive model (only if no seed data exists yet)
                if (reservationRepository.countByNotasEvento("Reserva seed para modelo predictivo") == 0) {
                        seedReservations();
                        log.info("=== Reservaciones seed creadas para modelo predictivo ===");
                }

                // Sync product ratings from actual reviews in DB
                syncProductRatings();
        }

        private void seedCategories() {
                categoryRepository
                                .save(new Category("Sillas", "Sillas elegantes para todo tipo de eventos", "chair",
                                                "sillas", 24));
                categoryRepository.save(
                                new Category("Mesas", "Mesas de todos los estilos y tamaños", "table", "mesas", 18));
                categoryRepository
                                .save(new Category("Carpas", "Carpas profesionales para eventos al aire libre", "tent",
                                                "carpas", 12));
                categoryRepository.save(new Category("Decoración", "Elementos decorativos para ambientar tu evento",
                                "decoration", "decoracion", 32));
        }

        private void seedProducts() {
                productRepository.save(new Product("Silla Chiavari Dorada",
                                "La silla Chiavari dorada es un clásico atemporal para eventos elegantes. Fabricada en madera de haya con acabado dorado brillante, ofrece comodidad y sofisticación. Incluye cojín de asiento acolchado en color blanco.",
                                "Elegancia clásica para bodas y galas",
                                BigDecimal.valueOf(60000), "/images/silla-chiavari.jpg", null, "Sillas",
                                4.8, 124, true, "Madera de haya", "40 x 42 x 92 cm", "Dorado", 1, 200, 150, 10,
                                "EXCELENTE"));

                productRepository.save(new Product("Mesa Redonda Cristal",
                                "Mesa redonda con superficie de cristal templado de 10mm y base de acero inoxidable. Diseño contemporáneo que aporta luminosidad y amplitud a cualquier espacio.",
                                "Diseño moderno con cristal templado",
                                BigDecimal.valueOf(180000), "/images/mesa-cristal.jpg", null, "Mesas",
                                4.6, 89, true, "Cristal templado / Acero", "150 cm diámetro x 75 cm alto",
                                "Transparente/Plata", 1, 50,
                                35, 5, "EXCELENTE"));

                productRepository.save(new Product("Carpa Pagoda Premium",
                                "Carpa tipo pagoda de 5x5 metros con estructura de aluminio anodizado y lona PVC impermeable de alta resistencia. Incluye sistema de iluminación LED integrado.",
                                "Estructura premium para eventos al aire libre",
                                BigDecimal.valueOf(480000), "/images/carpa-pagoda.jpg", null, "Carpas",
                                4.9, 67, true, "Aluminio / PVC", "5 x 5 x 3.5 m", "Blanco", 1, 10, 8, 2, "EXCELENTE"));

                productRepository.save(new Product("Centro de Mesa Floral",
                                "Arreglo floral artificial de alta calidad con base de cristal. Combina rosas, hortensias y eucalipto en tonos pastel.",
                                "Arreglo premium con flores artificiales de alta calidad",
                                BigDecimal.valueOf(100000), "/images/centro-mesa.jpg", null, "Decoración",
                                4.7, 156, true, "Flores artificiales / Cristal", "30 x 30 x 45 cm", "Pastel", 1, 100,
                                80, 15, "EXCELENTE"));

                productRepository.save(new Product("Silla Ghost Transparente",
                                "Silla de policarbonato transparente inspirada en el diseño de Philippe Starck. Ligera, resistente y apilable.",
                                "Diseño contemporáneo y versátil",
                                BigDecimal.valueOf(48000), "/images/silla-ghost.jpg", null, "Sillas",
                                4.5, 98, true, "Policarbonato", "38 x 40 x 90 cm", "Transparente", 1, 300, 200, 20,
                                "EXCELENTE"));

                productRepository.save(new Product("Mesa Rectangular Imperial",
                                "Mesa rectangular de madera maciza con acabado rústico-elegante. Ideal para banquetes largos y ferias gastronómicas.",
                                "Madera maciza con acabado rústico-elegante",
                                BigDecimal.valueOf(220000), "/images/mesa-imperial.jpg", null, "Mesas",
                                4.8, 73, true, "Madera maciza", "240 x 100 x 76 cm", "Natural", 1, 30, 22, 5,
                                "EXCELENTE"));

                productRepository.save(new Product("Guirnalda LED Cálida",
                                "Guirnalda de luces LED con 200 bombillas de luz cálida distribuidas en 20 metros de cable transparente. Resistente al agua (IP65).",
                                "Iluminación mágica para cualquier evento",
                                BigDecimal.valueOf(72000), "/images/guirnalda-led.jpg", null, "Decoración",
                                4.9, 203, true, "LED / Cable PVC", "20 m", "Luz cálida", 1, 100, 75, 10, "EXCELENTE"));

                productRepository.save(new Product("Carpa Tipo Hangar",
                                "Estructura de gran formato tipo hangar de 10x20 metros. Ideal para grandes eventos, ferias y exposiciones.",
                                "Gran formato para ferias y exposiciones",
                                BigDecimal.valueOf(1000000), "/images/carpa-hangar.jpg", null, "Carpas",
                                4.7, 34, true, "Acero galvanizado / PVC", "10 x 20 x 4 m", "Blanco", 1, 5, 3, 1,
                                "EXCELENTE"));

                productRepository.save(new Product("Mantelería Premium Blanca",
                                "Set completo de mantelería premium que incluye mantel, camino de mesa y 10 servilletas.",
                                "Set completo con acabado satinado",
                                BigDecimal.valueOf(32000), "/images/manteleria.jpg", null, "Decoración",
                                4.4, 187, true, "Poliéster satinado", "Varios tamaños", "Blanco", 1, 200, 160, 20,
                                "EXCELENTE"));
        }

        /**
         * Genera reservaciones mock distribuidas en los últimos 30 días para alimentar
         * el modelo predictivo J48 de Weka. Se crean datos variados con distintos
         * tipos de evento, cantidades y días (semana vs fin de semana) para que el
         * árbol de decisión produzca splits significativos.
         */
        private void seedReservations() {
                Random rng = new Random(42); // seed fija para reproducibilidad
                LocalDate today = LocalDate.now();

                // Productos disponibles con sus precios por día
                String[][] productos = {
                        {"seed-silla-chiavari", "Silla Chiavari Dorada", "/images/silla-chiavari.jpg", "60000"},
                        {"seed-mesa-cristal", "Mesa Redonda Cristal", "/images/mesa-cristal.jpg", "180000"},
                        {"seed-carpa-pagoda", "Carpa Pagoda Premium", "/images/carpa-pagoda.jpg", "480000"},
                        {"seed-centro-mesa", "Centro de Mesa Floral", "/images/centro-mesa.jpg", "100000"},
                        {"seed-silla-ghost", "Silla Ghost Transparente", "/images/silla-ghost.jpg", "48000"},
                        {"seed-mesa-imperial", "Mesa Rectangular Imperial", "/images/mesa-imperial.jpg", "220000"},
                        {"seed-guirnalda", "Guirnalda LED Cálida", "/images/guirnalda-led.jpg", "72000"},
                        {"seed-manteleria", "Mantelería Premium Blanca", "/images/manteleria.jpg", "32000"}
                };

                String[] tiposEvento = {"Boda", "Corporativo", "Cumpleaños", "Social", "Graduación"};
                String[] estados = {"CONFIRMADA", "COMPLETADA", "ENTREGADA"};
                String[] direcciones = {
                        "Salón Royal, Calle 80 #45-12", "Finca Villa Real, Km 5 vía La Calera",
                        "Hotel Hilton, Carrera 7 #73-55", "Centro de Convenciones, Av. 68",
                        "Club El Nogal, Carrera 9 #76-33", "Hacienda San Fernando, Chía"
                };
                String[][] clientes = {
                        {"seed-user-1", "María García", "maria.garcia@email.com"},
                        {"seed-user-2", "Carlos Rodríguez", "carlos.rod@email.com"},
                        {"seed-user-3", "Ana Martínez", "ana.martinez@email.com"},
                        {"seed-user-4", "Pedro Sánchez", "pedro.sanchez@email.com"},
                        {"seed-user-5", "Laura Díaz", "laura.diaz@email.com"},
                        {"seed-user-6", "Empresa ABC S.A.S", "eventos@empresaabc.com"},
                        {"seed-user-7", "Juan López", "juan.lopez@email.com"},
                        {"seed-user-8", "Sofía Herrera", "sofia.herrera@email.com"}
                };

                // Generar reservaciones para los últimos 30 días
                for (int dayOffset = 30; dayOffset >= 0; dayOffset--) {
                        LocalDate fecha = today.minusDays(dayOffset);
                        boolean esFinDeSemana = fecha.getDayOfWeek() == DayOfWeek.SATURDAY
                                        || fecha.getDayOfWeek() == DayOfWeek.SUNDAY;

                        // Fines de semana: 2-3 reservas grandes (Bodas/Social) → demanda ALTA
                        // Entre semana: 0-2 reservas pequeñas (Corporativo/Cumpleaños) → demanda BAJA/MEDIA
                        int numReservas;
                        if (esFinDeSemana) {
                                numReservas = 2 + rng.nextInt(2); // 2-3
                        } else {
                                numReservas = rng.nextInt(3); // 0-2
                        }

                        for (int r = 0; r < numReservas; r++) {
                                Reservation reserva = new Reservation();
                                reserva.setTenantId("default");

                                // Cliente aleatorio
                                String[] cliente = clientes[rng.nextInt(clientes.length)];
                                reserva.setUsuarioId(cliente[0]);
                                reserva.setUsuarioNombre(cliente[1]);
                                reserva.setUsuarioEmail(cliente[2]);

                                // Tipo de evento según día
                                String tipoEvento;
                                if (esFinDeSemana) {
                                        tipoEvento = rng.nextDouble() < 0.6 ? "Boda" : tiposEvento[rng.nextInt(tiposEvento.length)];
                                } else {
                                        tipoEvento = rng.nextDouble() < 0.5 ? "Corporativo" : tiposEvento[2 + rng.nextInt(3)];
                                }
                                reserva.setTipoEvento(tipoEvento);

                                // Items de la reserva
                                List<Reservation.ItemReserva> items = new ArrayList<>();
                                int numItems = 1 + rng.nextInt(4); // 1-4 productos distintos
                                BigDecimal subtotalReserva = BigDecimal.ZERO;

                                for (int i = 0; i < numItems; i++) {
                                        String[] prod = productos[rng.nextInt(productos.length)];
                                        BigDecimal precioPorDia = new BigDecimal(prod[3]);

                                        // Cantidad según tipo de evento
                                        int cantidad;
                                        if (tipoEvento.equals("Boda")) {
                                                cantidad = 10 + rng.nextInt(41); // 10-50 uds
                                        } else if (tipoEvento.equals("Corporativo")) {
                                                cantidad = 2 + rng.nextInt(9); // 2-10 uds
                                        } else {
                                                cantidad = 5 + rng.nextInt(16); // 5-20 uds
                                        }

                                        Reservation.ItemReserva item = new Reservation.ItemReserva(
                                                        prod[0], prod[1], prod[2], precioPorDia, cantidad);
                                        items.add(item);
                                        subtotalReserva = subtotalReserva.add(
                                                        precioPorDia.multiply(BigDecimal.valueOf(cantidad)));
                                }

                                reserva.setItems(items);
                                reserva.setFechaInicio(fecha);
                                int diasAlquiler = 1 + rng.nextInt(3); // 1-3 días
                                reserva.setDiasAlquiler(diasAlquiler);
                                reserva.setFechaFin(fecha.plusDays(diasAlquiler));

                                BigDecimal totalConDias = subtotalReserva.multiply(BigDecimal.valueOf(diasAlquiler));
                                reserva.setSubtotal(totalConDias);
                                reserva.setDescuento(BigDecimal.ZERO);
                                reserva.setTotal(totalConDias);

                                reserva.setEstado(estados[rng.nextInt(estados.length)]);
                                reserva.setEstadoPago("PAGADO");
                                reserva.setMontoAbonado(totalConDias);
                                reserva.setMetodoPago(rng.nextBoolean() ? "TRANSFERENCIA" : "EFECTIVO");
                                reserva.setDireccionEvento(direcciones[rng.nextInt(direcciones.length)]);
                                reserva.setNotasEvento("Reserva seed para modelo predictivo");
                                reserva.setHoraEntrega(String.format("%02d:00", 8 + rng.nextInt(10)));

                                // Timestamps coherentes
                                LocalDateTime creacion = fecha.minusDays(3 + rng.nextInt(5)).atTime(9 + rng.nextInt(10), rng.nextInt(60));
                                reserva.setFechaCreacion(creacion);
                                reserva.setFechaActualizacion(creacion.plusHours(rng.nextInt(48)));

                                reservationRepository.save(reserva);
                        }
                }
                log.info("Reservaciones seed: {} registros creados para {} días", reservationRepository.count(), 31);
        }

        private void initializePermissions() {
                if (permissionRepository.count() > 0) return;

                // USER: puede ver catálogo, crear cotizaciones, gestionar sus reservas
                Permission userPerms = new Permission();
                userPerms.setRoleName("USER");
                userPerms.setTenantId("default");
                userPerms.setPermissions(List.of(
                        "PRODUCT_VIEW", "CATEGORY_VIEW", "RESERVATION_CREATE", "RESERVATION_VIEW_OWN",
                        "RESERVATION_CANCEL_OWN", "PAYMENT_CREATE", "PAYMENT_VIEW_OWN",
                        "REVIEW_CREATE", "REVIEW_VIEW", "COUPON_VALIDATE",
                        "NOTIFICATION_VIEW_OWN", "PROFILE_VIEW", "PROFILE_EDIT",
                        "FAVORITE_MANAGE"
                ));
                permissionRepository.save(userPerms);

                // MANAGER: puede gestionar productos, ver reservas, moderar reviews
                Permission managerPerms = new Permission();
                managerPerms.setRoleName("MANAGER");
                managerPerms.setTenantId("default");
                managerPerms.setPermissions(List.of(
                        "PRODUCT_VIEW", "PRODUCT_CREATE", "PRODUCT_EDIT", "PRODUCT_DELETE",
                        "CATEGORY_VIEW", "CATEGORY_CREATE", "CATEGORY_EDIT",
                        "RESERVATION_VIEW_ALL", "RESERVATION_UPDATE_STATUS",
                        "PAYMENT_VIEW_ALL", "REVIEW_VIEW", "REVIEW_MODERATE",
                        "COUPON_VIEW", "COUPON_CREATE", "COUPON_EDIT",
                        "NOTIFICATION_VIEW_OWN", "NOTIFICATION_SEND",
                        "REPORT_VIEW", "EXPORT_DATA"
                ));
                permissionRepository.save(managerPerms);

                // ADMIN: control total del tenant
                Permission adminPerms = new Permission();
                adminPerms.setRoleName("ADMIN");
                adminPerms.setTenantId("default");
                adminPerms.setPermissions(List.of(
                        "PRODUCT_VIEW", "PRODUCT_CREATE", "PRODUCT_EDIT", "PRODUCT_DELETE",
                        "CATEGORY_VIEW", "CATEGORY_CREATE", "CATEGORY_EDIT", "CATEGORY_DELETE",
                        "RESERVATION_VIEW_ALL", "RESERVATION_UPDATE_STATUS", "RESERVATION_DELETE",
                        "PAYMENT_VIEW_ALL", "PAYMENT_REFUND",
                        "REVIEW_VIEW", "REVIEW_MODERATE", "REVIEW_DELETE",
                        "COUPON_VIEW", "COUPON_CREATE", "COUPON_EDIT", "COUPON_DELETE",
                        "USER_VIEW_ALL", "USER_EDIT", "USER_SUSPEND", "USER_DELETE",
                        "NOTIFICATION_VIEW_ALL", "NOTIFICATION_SEND",
                        "REPORT_VIEW", "REPORT_GENERATE", "EXPORT_DATA",
                        "AUDIT_VIEW", "SETTINGS_MANAGE"
                ));
                permissionRepository.save(adminPerms);

                // SUPER_ADMIN: control total de la plataforma + tenants
                Permission superAdminPerms = new Permission();
                superAdminPerms.setRoleName("SUPER_ADMIN");
                superAdminPerms.setTenantId("default");
                superAdminPerms.setPermissions(List.of(
                        "TENANT_CREATE", "TENANT_VIEW_ALL", "TENANT_EDIT", "TENANT_DELETE",
                        "TENANT_SUSPEND", "PLATFORM_SETTINGS",
                        "PRODUCT_VIEW", "PRODUCT_CREATE", "PRODUCT_EDIT", "PRODUCT_DELETE",
                        "CATEGORY_VIEW", "CATEGORY_CREATE", "CATEGORY_EDIT", "CATEGORY_DELETE",
                        "RESERVATION_VIEW_ALL", "RESERVATION_UPDATE_STATUS", "RESERVATION_DELETE",
                        "PAYMENT_VIEW_ALL", "PAYMENT_REFUND",
                        "REVIEW_VIEW", "REVIEW_MODERATE", "REVIEW_DELETE",
                        "COUPON_VIEW", "COUPON_CREATE", "COUPON_EDIT", "COUPON_DELETE",
                        "USER_VIEW_ALL", "USER_EDIT", "USER_SUSPEND", "USER_DELETE", "USER_CHANGE_ROLE",
                        "NOTIFICATION_VIEW_ALL", "NOTIFICATION_SEND", "NOTIFICATION_BROADCAST",
                        "REPORT_VIEW", "REPORT_GENERATE", "EXPORT_DATA",
                        "AUDIT_VIEW", "AUDIT_EXPORT", "SETTINGS_MANAGE",
                        "ANALYTICS_VIEW", "ANALYTICS_EXPORT"
                ));
                permissionRepository.save(superAdminPerms);

                log.info("Permisos RBAC inicializados: USER, MANAGER, ADMIN, SUPER_ADMIN");
        }

        /**
         * Recalculates calificacion and cantidadResenas for every product
         * based on real reviews stored in the database.
         */
        private void syncProductRatings() {
                List<Product> allProducts = productRepository.findAll();
                int updated = 0;
                for (Product product : allProducts) {
                        List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(product.getId());
                        double newRating;
                        int newCount;
                        if (reviews.isEmpty()) {
                                newRating = 0;
                                newCount = 0;
                        } else {
                                newRating = Math.round(
                                        reviews.stream().mapToInt(Review::getRating).average().orElse(0.0) * 10.0
                                ) / 10.0;
                                newCount = reviews.size();
                        }
                        if (product.getCalificacion() != newRating || product.getCantidadResenas() != newCount) {
                                product.setCalificacion(newRating);
                                product.setCantidadResenas(newCount);
                                productRepository.save(product);
                                updated++;
                        }
                }
                if (updated > 0) {
                        log.info("=== Sincronización de calificaciones: {} productos actualizados ===", updated);
                }
        }
}
