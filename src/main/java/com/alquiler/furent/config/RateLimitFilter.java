package com.alquiler.furent.config;

import com.alquiler.furent.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro de rate limiting por IP y protección contra brute force.
 *
 * Límites:
 * - Global API:  100 req/min por IP
 * - Login:         5 intentos/min por IP (protección brute force)
 * - Register:      3 intentos/min por IP
 * - Otros auth:   10 req/min por IP
 *
 * Los contadores se resetean automáticamente cada 60 segundos.
 */
@Component
@Order(0)
@ConditionalOnProperty(name = "furent.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final int MAX_LOGIN_PER_MINUTE = 5;
    private static final int MAX_REGISTER_PER_MINUTE = 3;
    private static final int MAX_AUTH_REQUESTS_PER_MINUTE = 10;
    private static final long WINDOW_MS = 60_000;

    private final ConcurrentHashMap<String, RateInfo> clients = new ConcurrentHashMap<>();
    private final AuditLogService auditLogService;

    public RateLimitFilter(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();
        String method = request.getMethod();
        long now = System.currentTimeMillis();

        // Limpiar buckets expirados (probabilístico, evita crecimiento)
        if (Math.random() < 0.01) {
            clients.entrySet().removeIf(e -> now - e.getValue().windowStart > WINDOW_MS * 2);
        }

        // Determinar límite según path
        int limit;
        String bucketKey;
        boolean isLogin = "/api/auth/login".equals(path) && "POST".equals(method);
        boolean isRegister = "/api/auth/register".equals(path) && "POST".equals(method);

        if (isLogin) {
            limit = MAX_LOGIN_PER_MINUTE;
            bucketKey = clientIp + ":login";
        } else if (isRegister) {
            limit = MAX_REGISTER_PER_MINUTE;
            bucketKey = clientIp + ":register";
        } else if (path.startsWith("/api/auth/")) {
            limit = MAX_AUTH_REQUESTS_PER_MINUTE;
            bucketKey = clientIp + ":auth";
        } else {
            limit = MAX_REQUESTS_PER_MINUTE;
            bucketKey = clientIp + ":general";
        }

        RateInfo info = clients.compute(bucketKey, (key, existing) -> {
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new RateInfo(now);
            }
            return existing;
        });

        int count = info.counter.incrementAndGet();

        // Rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - count)));

        if (count > limit) {
            // Auditoría de seguridad para brute force
            if (isLogin) {
                log.warn("BRUTE FORCE bloqueado: IP {} excedió {} intentos de login/min", clientIp, MAX_LOGIN_PER_MINUTE);
                auditLogService.logSecurity("SISTEMA", "BRUTE_FORCE_BLOCKED",
                        "IP " + clientIp + " bloqueada por exceder límite de login: " + MAX_LOGIN_PER_MINUTE + " intentos/min");
            } else {
                log.warn("Rate limit exceeded: IP {} en {} (count: {})", clientIp, path, count);
            }

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json; charset=UTF-8");
            response.setHeader("Retry-After", "60");
            String msg = isLogin
                    ? "Demasiados intentos de login. Espera 1 minuto antes de reintentar."
                    : "Demasiadas solicitudes. Intenta de nuevo más tarde.";
            response.getWriter().write(
                    "{\"timestamp\":\"" + java.time.LocalDateTime.now() + "\"," +
                    "\"status\":429," +
                    "\"error\":\"Too Many Requests\"," +
                    "\"message\":\"" + msg + "\"}"
            );
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/") || path.startsWith("/js/") ||
               path.startsWith("/images/") || path.startsWith("/uploads/") ||
               path.startsWith("/actuator/");
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isEmpty()) {
            return xForwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RateInfo {
        final long windowStart;
        final AtomicInteger counter;

        RateInfo(long windowStart) {
            this.windowStart = windowStart;
            this.counter = new AtomicInteger(0);
        }
    }
}
