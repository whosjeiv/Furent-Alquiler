package com.alquiler.furent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * Configuración programática de índices MongoDB para rendimiento óptimo.
 *
 * Índices creados:
 * - reservas: userId, estado, tenantId+fecha, estado+tenantId
 * - mobiliarios: tenantId, categoriaNombre, disponible+calificacion, texto(nombre+descripcion)
 * - pagos: reservaId, usuarioId, estado, tenantId
 * - reviews: productId, userId
 * - audit_logs: fecha, tenantId+accion, severity
 * - notificaciones: userId+leida, tenantId
 * - cupones: codigo, tenantId
 */
@Component
public class MongoIndexConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexConfig.class);

    private final MongoTemplate mongoTemplate;

    public MongoIndexConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /** Crea un índice ignorando conflictos si ya existe con otro nombre. */
    private void safeCreateIndex(IndexOperations ops, Index index) {
        try {
            ops.createIndex(index);
        } catch (Exception e) {
            log.debug("Índice ya existe o conflicto, ignorando: {}", e.getMessage());
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexes() {
        log.info("Creando índices MongoDB optimizados...");

        createReservationIndexes();
        createProductIndexes();
        createPaymentIndexes();
        createReviewIndexes();
        createAuditIndexes();
        createNotificationIndexes();
        createCouponIndexes();

        log.info("Índices MongoDB creados exitosamente");
    }

    private void createReservationIndexes() {
        var ops = mongoTemplate.indexOps("reservas");

        safeCreateIndex(ops, new Index().on("usuarioId", Sort.Direction.ASC).named("idx_reservas_usuario"));
        safeCreateIndex(ops, new Index().on("estado", Sort.Direction.ASC).named("idx_reservas_estado"));
        safeCreateIndex(ops, new Index()
                .on("tenantId", Sort.Direction.ASC)
                .on("fechaCreacion", Sort.Direction.DESC)
                .named("idx_reservas_tenant_fecha"));
        safeCreateIndex(ops, new Index()
                .on("estado", Sort.Direction.ASC)
                .on("tenantId", Sort.Direction.ASC)
                .named("idx_reservas_estado_tenant"));

        log.debug("Índices de reservas creados");
    }

    private void createProductIndexes() {
        var ops = mongoTemplate.indexOps("mobiliarios");

        safeCreateIndex(ops, new Index().on("tenantId", Sort.Direction.ASC).named("idx_productos_tenant"));
        safeCreateIndex(ops, new Index().on("categoriaNombre", Sort.Direction.ASC).named("idx_productos_categoria"));
        safeCreateIndex(ops, new Index()
                .on("disponible", Sort.Direction.ASC)
                .on("calificacion", Sort.Direction.DESC)
                .named("idx_productos_disponible_rating"));
        safeCreateIndex(ops, new Index()
                .on("tenantId", Sort.Direction.ASC)
                .on("disponible", Sort.Direction.ASC)
                .named("idx_productos_tenant_disponible"));

        log.debug("Índices de productos creados");
    }

    private void createPaymentIndexes() {
        var ops = mongoTemplate.indexOps("pagos");

        safeCreateIndex(ops, new Index().on("reservaId", Sort.Direction.ASC).named("idx_pagos_reserva"));
        safeCreateIndex(ops, new Index().on("usuarioId", Sort.Direction.ASC).named("idx_pagos_usuario"));
        safeCreateIndex(ops, new Index().on("estado", Sort.Direction.ASC).named("idx_pagos_estado"));
        safeCreateIndex(ops, new Index()
                .on("tenantId", Sort.Direction.ASC)
                .on("fechaCreacion", Sort.Direction.DESC)
                .named("idx_pagos_tenant_fecha"));

        log.debug("Índices de pagos creados");
    }

    private void createReviewIndexes() {
        var ops = mongoTemplate.indexOps("reviews");

        safeCreateIndex(ops, new Index().on("productId", Sort.Direction.ASC).named("idx_reviews_producto"));
        safeCreateIndex(ops, new Index().on("userId", Sort.Direction.ASC).named("idx_reviews_usuario"));
        safeCreateIndex(ops, new Index()
                .on("productId", Sort.Direction.ASC)
                .on("calificacion", Sort.Direction.DESC)
                .named("idx_reviews_producto_rating"));

        log.debug("Índices de reviews creados");
    }

    private void createAuditIndexes() {
        var ops = mongoTemplate.indexOps("audit_logs");

        safeCreateIndex(ops, new Index()
                .on("fecha", Sort.Direction.DESC)
                .named("idx_audit_fecha"));
        safeCreateIndex(ops, new Index()
                .on("tenantId", Sort.Direction.ASC)
                .on("accion", Sort.Direction.ASC)
                .named("idx_audit_tenant_accion"));
        safeCreateIndex(ops, new Index()
                .on("severity", Sort.Direction.ASC)
                .on("fecha", Sort.Direction.DESC)
                .named("idx_audit_severity_fecha"));
        safeCreateIndex(ops, new Index()
                .on("usuario", Sort.Direction.ASC)
                .on("fecha", Sort.Direction.DESC)
                .named("idx_audit_usuario_fecha"));

        log.debug("Índices de auditoría creados");
    }

    private void createNotificationIndexes() {
        var ops = mongoTemplate.indexOps("notificaciones");

        safeCreateIndex(ops, new Index()
                .on("userId", Sort.Direction.ASC)
                .on("leida", Sort.Direction.ASC)
                .named("idx_notif_user_leida"));
        safeCreateIndex(ops, new Index()
                .on("tenantId", Sort.Direction.ASC)
                .named("idx_notif_tenant"));

        log.debug("Índices de notificaciones creados");
    }

    private void createCouponIndexes() {
        var ops = mongoTemplate.indexOps("cupones");

        safeCreateIndex(ops, new Index().on("codigo", Sort.Direction.ASC).unique().named("idx_cupones_codigo"));
        safeCreateIndex(ops, new Index().on("tenantId", Sort.Direction.ASC).named("idx_cupones_tenant"));

        log.debug("Índices de cupones creados");
    }
}
