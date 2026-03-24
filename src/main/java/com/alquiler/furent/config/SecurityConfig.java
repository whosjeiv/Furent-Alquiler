package com.alquiler.furent.config;

import com.alquiler.furent.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import com.alquiler.furent.exception.AccountSuspendedException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final UserService userService;
    private final JwtAuthFilter jwtAuthFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;
    private final com.alquiler.furent.service.OAuth2UserService oauth2UserService;

    public SecurityConfig(@Lazy UserService userService, @Lazy JwtAuthFilter jwtAuthFilter,
                          CorsConfigurationSource corsConfigurationSource,
                          OAuth2LoginSuccessHandler oauth2LoginSuccessHandler,
                          com.alquiler.furent.service.OAuth2UserService oauth2UserService) {
        this.userService = userService;
        this.jwtAuthFilter = jwtAuthFilter;
        this.corsConfigurationSource = corsConfigurationSource;
        this.oauth2LoginSuccessHandler = oauth2LoginSuccessHandler;
        this.oauth2UserService = oauth2UserService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.userDetailsService(userService).passwordEncoder(passwordEncoder());
        return authBuilder.build();
    }

    @Bean
    public AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (isAdmin) {
                response.sendRedirect("/admin");
            } else {
                response.sendRedirect("/");
            }
        };
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            if (exception instanceof AccountSuspendedException) {
                AccountSuspendedException ase = (AccountSuspendedException) exception;
                String reason = URLEncoder.encode(ase.getReason(), StandardCharsets.UTF_8);
                String duration = URLEncoder.encode(ase.getDuration(), StandardCharsets.UTF_8);
                String email = URLEncoder.encode(request.getParameter("email"), StandardCharsets.UTF_8);
                response.sendRedirect(
                        "/login?suspended=true&reason=" + reason + "&duration=" + duration
                                + "&email=" + email);
            } else {
                response.sendRedirect("/login?error=true");
            }
        };
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/auth/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/register")
                        .permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/catalogo", "/producto/**",
                                "/css/**", "/js/**", "/images/**", "/uploads/**",
                                "/nosotros", "/contacto", "/faq",
                                "/login", "/registro", "/inicio-rapido",
                                "/api/auth/**", "/api/productos/search",
                                "/api/webhooks/**",
                                "/password-reset", "/password-reset/**",
                                "/verificar-email/**",
                                "/actuator/health", "/actuator/info",
                                "/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/api/pagos/payu/confirmacion")
                        .permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/panel", "/configuracion", "/configuracion/**", "/cotizacion",
                                "/cotizacion/**", "/pago/**",
                                "/api/cotizacion", "/api/favoritos/**",
                                "/api/notificaciones/**", "/api/cupones/**",
                                "/api/pagos/**")
                        .authenticated()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(roleBasedSuccessHandler())
                        .failureHandler(authenticationFailureHandler())
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oauth2UserService))
                        .successHandler(oauth2LoginSuccessHandler)
                        .permitAll())
                .rememberMe(remember -> remember
                        .key("furent-remember-me-key-2026")
                        .tokenValiditySeconds(30 * 24 * 60 * 60) // 30 days
                        .userDetailsService(userService))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**", "/api/pagos/payu/confirmacion"))
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                                .preload(true))
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.tailwindcss.com https://cdn.jsdelivr.net https://unpkg.com https://code.jquery.com https://cdn.datatables.net https://cdnjs.cloudflare.com; " +
                                        "style-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com https://fonts.googleapis.com https://cdn.jsdelivr.net https://unpkg.com https://cdn.datatables.net https://cdnjs.cloudflare.com; " +
                                        "font-src 'self' data: https://fonts.gstatic.com; " +
                                        "img-src 'self' data: blob: https: *.payulatam.com; " +
                                        "connect-src 'self' https://cdn.jsdelivr.net https://api.payulatam.com https://*.payulatam.com; " +
                                        "frame-src https://checkout.payulatam.com;"))
                        .frameOptions(frame -> frame.deny()));

        return http.build();
    }
}
