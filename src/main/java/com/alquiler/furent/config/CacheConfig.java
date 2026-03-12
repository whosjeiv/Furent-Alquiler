package com.alquiler.furent.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuración de cache con Redis.
 * Define TTLs específicos por dominio para optimizar rendimiento y frescura de datos.
 *
 * Caches definidos:
 * - products: 10 min (datos consultados frecuentemente)
 * - categories: 30 min (datos que cambian poco)
 * - product-detail: 5 min (detalle individual)
 * - featured-products: 10 min (productos destacados)
 * - tenant-config: 15 min (configuración de tenant)
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "furent.features.cache-enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("products", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("categories", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("product-detail", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("featured-products", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("tenant-config", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("user-profile", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("product-count", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("notifications", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put("reviews", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("coupons", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
