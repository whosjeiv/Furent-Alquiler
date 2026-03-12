package com.alquiler.furent.dto;

import com.alquiler.furent.model.Product;
import java.math.BigDecimal;

public record ProductResponse(
    String id,
    String nombre,
    String descripcionCorta,
    BigDecimal precioPorDia,
    String imagenUrl,
    String categoriaNombre,
    double calificacion,
    boolean disponible
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
            p.getId(), p.getNombre(), p.getDescripcionCorta(),
            p.getPrecioPorDia(), p.getImagenUrl(), p.getCategoriaNombre(),
            p.getCalificacion(), p.isDisponible()
        );
    }
}
