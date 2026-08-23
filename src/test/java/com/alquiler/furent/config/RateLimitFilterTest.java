package com.alquiler.furent.config;

import com.alquiler.furent.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock private AuditLogService auditLogService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(auditLogService);
    }

    @Test
    void normalRequest_passesThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response).setHeader(eq("X-RateLimit-Limit"), eq("100"));
    }

    @Test
    void loginEndpoint_hasStricterLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response).setHeader(eq("X-RateLimit-Limit"), eq("5"));
    }

    @Test
    void registerEndpoint_hasStricterLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/register");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("10.0.0.2");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response).setHeader(eq("X-RateLimit-Limit"), eq("3"));
    }

    @Test
    void exceedingLimit_returns429() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        when(request.getRequestURI()).thenReturn("/api/auth/register");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("10.0.0.3");
        when(response.getWriter()).thenReturn(pw);

        // Exceed register limit of 3
        for (int i = 0; i < 3; i++) {
            filter.doFilterInternal(request, response, chain);
        }
        filter.doFilterInternal(request, response, chain);

        verify(response).setStatus(429);
        verify(response).setHeader("Retry-After", "60");
    }

    @Test
    void loginBruteForce_logsSecurityEvent() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("10.0.0.4");
        when(response.getWriter()).thenReturn(pw);

        // Exceed login limit of 5
        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, chain);
        }
        filter.doFilterInternal(request, response, chain);

        verify(auditLogService).logSecurity(eq("SISTEMA"), eq("BRUTE_FORCE_BLOCKED"), contains("10.0.0.4"));
    }

    @Test
    void staticAssets_shouldNotFilter() {
        when(request.getRequestURI()).thenReturn("/css/styles.css");
        assertTrue(filter.shouldNotFilter(request));

        when(request.getRequestURI()).thenReturn("/js/app.js");
        assertTrue(filter.shouldNotFilter(request));

        when(request.getRequestURI()).thenReturn("/images/logo.png");
        assertTrue(filter.shouldNotFilter(request));

        when(request.getRequestURI()).thenReturn("/actuator/health");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void apiPaths_shouldFilter() {
        when(request.getRequestURI()).thenReturn("/api/products");
        assertFalse(filter.shouldNotFilter(request));
    }

    @Test
    void xForwardedFor_extractsFirstIp() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void differentIps_haveSeparateLimits() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/register");
        when(request.getMethod()).thenReturn("POST");

        // IP 1 makes 3 requests (at limit)
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");
        for (int i = 0; i < 3; i++) {
            filter.doFilterInternal(request, response, chain);
        }

        // IP 2 should still be able to make requests
        when(request.getRemoteAddr()).thenReturn("10.0.0.6");
        filter.doFilterInternal(request, response, chain);

        // chain should be called 4 times total (3 for IP1 + 1 for IP2)
        verify(chain, times(4)).doFilter(request, response);
    }
}
