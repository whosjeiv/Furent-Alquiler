package com.alquiler.furent.dto;

import com.alquiler.furent.model.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ProductResponseTest {

    @Test
    void from_validProduct_mapsCorrectly() {
        Product product = new Product();
        product.setId("prod-001");
        product.setNombre("Silla Tiffany");
        product.setDescripcionCorta("Elegante silla para eventos");
        product.setPrecioPorDia(BigDecimal.valueOf(15000));
        product.setImagenUrl("/images/silla.jpg");
        product.setCategoriaNombre("Sillas");
        product.setCalificacion(4.5);
        product.setDisponible(true);

        ProductResponse dto = ProductResponse.from(product);

        assertEquals("prod-001", dto.id());
        assertEquals("Silla Tiffany", dto.nombre());
        assertEquals("Elegante silla para eventos", dto.descripcionCorta());
        assertEquals(0, BigDecimal.valueOf(15000).compareTo(dto.precioPorDia()));
        assertEquals("/images/silla.jpg", dto.imagenUrl());
        assertEquals("Sillas", dto.categoriaNombre());
        assertEquals(4.5, dto.calificacion());
        assertTrue(dto.disponible());
    }

    @Test
    void from_nullFields_handlesGracefully() {
        Product product = new Product();
        product.setId("prod-002");
        product.setNombre("Mesa");

        ProductResponse dto = ProductResponse.from(product);

        assertEquals("prod-002", dto.id());
        assertEquals("Mesa", dto.nombre());
        assertNull(dto.descripcionCorta());
    }
}
