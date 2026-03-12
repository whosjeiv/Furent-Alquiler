package com.alquiler.furent.config;

import jakarta.servlet.MultipartConfigElement;
import org.apache.catalina.connector.Connector;
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
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get("uploads");
        String uploadPath = uploadDir.toFile().getAbsolutePath();
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + uploadPath + "/");
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        // -1 = sin límite de tamaño; fileSizeThreshold=0 para escribir directo a disco
        return new MultipartConfigElement("", -1L, -1L, 0);
    }

    @Bean
    public ServletWebServerFactory servletWebServerFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addConnectorCustomizers((Connector connector) -> {
            connector.setMaxPostSize(-1);
            connector.setMaxSavePostSize(-1);
        });
        // Desactivar el límite de cantidad de partes multipart
        factory.addContextCustomizers(context -> {
            context.setAllowCasualMultipartParsing(true);
        });
        return factory;
    }
}
