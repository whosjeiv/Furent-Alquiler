package com.alquiler.furent.config;

import jakarta.servlet.MultipartConfigElement;
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${furent.upload.inspiration-dir:uploads/inspiration}")
    private String inspirationUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get("uploads");
        String uploadPath = uploadDir.toFile().getAbsolutePath();
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + uploadPath + "/");

        Path inspirationDir = Paths.get(inspirationUploadDir);
        String inspirationPath = inspirationDir.toAbsolutePath().toString();
        registry.addResourceHandler("/uploads/inspiration/**")
                .addResourceLocations("file:" + inspirationPath + "/");
    }

    /**
     * Configuración del multipart a nivel de Servlet.
     * Usa java.io.tmpdir como ubicación temporal y elimina todos los límites de tamaño.
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        String tempDir = System.getProperty("java.io.tmpdir");
        // location, maxFileSize, maxRequestSize, fileSizeThreshold
        // -1L = sin límite de tamaño
        return new MultipartConfigElement(tempDir, -1L, -1L, 0);
    }

    /**
     * Personalización de Tomcat embebido para desactivar TODOS los límites
     * que causan el error HTTP 413 "Content Too Large".
     *
     * En Tomcat 11 (Spring Boot 4.x) se introdujeron nuevos límites:
     * - maxPartCount: máximo de partes en un multipart request (default 50)
     * - maxParameterCount: máximo de parámetros totales (default 1000)
     * Ambos deben ser desactivados (-1) para formularios con muchos campos + archivos.
     */
    @Bean
    public ServletWebServerFactory servletWebServerFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addConnectorCustomizers((Connector connector) -> {
            // Sin límite de tamaño de POST body
            connector.setMaxPostSize(-1);
            // Sin límite de tamaño de datos que Tomcat "traga" en uploads abortados
            connector.setMaxSavePostSize(-1);
            // Sin límite de cantidad de parámetros (query string + body)
            connector.setMaxParameterCount(-1);
        });
        factory.addContextCustomizers(context -> {
            context.setAllowCasualMultipartParsing(true);
        });
        return factory;
    }
}
