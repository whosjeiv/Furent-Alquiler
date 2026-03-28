package com.alquiler.furent.config;

import com.alquiler.furent.model.Category;
import com.alquiler.furent.model.Permission;
import com.alquiler.furent.model.Product;
import com.alquiler.furent.model.Review;
import com.alquiler.furent.repository.CategoryRepository;
import com.alquiler.furent.repository.PermissionRepository;
import com.alquiler.furent.repository.ProductRepository;
import com.alquiler.furent.repository.ReviewRepository;
import com.alquiler.furent.service.TenantService;
import com.alquiler.furent.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

        private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

        private final ProductRepository productRepository;
        private final CategoryRepository categoryRepository;
        private final ReviewRepository reviewRepository;
        private final UserService userService;
        private final TenantService tenantService;
        private final PermissionRepository permissionRepository;

        @Value("${furent.admin.password:${random.uuid}}")
        private String adminPassword;

        public DataInitializer(ProductRepository productRepository, CategoryRepository categoryRepository,
                        ReviewRepository reviewRepository,
                        UserService userService, TenantService tenantService,
                        PermissionRepository permissionRepository) {
                this.productRepository = productRepository;
                this.categoryRepository = categoryRepository;
                this.reviewRepository = reviewRepository;
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
                log.warn("═══════════════════════════════════════════════════════════════");
                log.warn("Admin creado con contraseña: {}", adminPassword);
                log.warn("CAMBIAR INMEDIATAMENTE EN PRODUCCIÓN VÍA FURENT_ADMIN_PASSWORD");
                log.warn("═══════════════════════════════════════════════════════════════");

                // Only seed if collections are empty
                if (categoryRepository.count() == 0) {
                        seedCategories();
                        log.info("Categorías seed creadas");
                }
                if (productRepository.count() == 0) {
                        seedProducts();
                        log.info("Productos seed creados");
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
