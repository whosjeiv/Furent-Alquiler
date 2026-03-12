package com.alquiler.furent.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Health Indicator personalizado para Furent.
 * Verifica el estado de MongoDB y reporta información del sistema.
 *
 * Endpoint: /actuator/health
 * Muestra: estado de MongoDB, colecciones, versión de la app.
 */
@Component
public class FurentHealthIndicator implements HealthIndicator {

    private final MongoTemplate mongoTemplate;

    public FurentHealthIndicator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Health health() {
        try {
            // Verificar conectividad MongoDB
            long collections = mongoTemplate.getCollectionNames().size();

            return Health.up()
                    .withDetail("mongodb", "connected")
                    .withDetail("collections", collections)
                    .withDetail("service", "furent-backend")
                    .withDetail("version", "2.0.0")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("mongodb", "disconnected")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
