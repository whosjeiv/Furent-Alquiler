package com.alquiler.furent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI furentOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Furent API — Alquiler de Mobiliarios")
                        .version("1.0.0")
                        .description("API REST para gestión de alquiler de mobiliarios para eventos. "
                                + "Incluye autenticación JWT, catálogo de productos, cotizaciones, "
                                + "reservas, pagos, y notificaciones en tiempo real.")
                        .contact(new Contact()
                                .name("Furent Team")
                                .email("soporte@furent.co"))
                        .license(new License()
                                .name("Privada")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer JWT"))
                .components(new Components()
                        .addSecuritySchemes("Bearer JWT", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtenido del endpoint /api/auth/login")));
    }
}
