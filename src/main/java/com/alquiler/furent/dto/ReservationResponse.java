package com.alquiler.furent.dto;

import com.alquiler.furent.model.Reservation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReservationResponse(
        String id,
        String usuarioNombre,
        String usuarioEmail,
        String tipoEvento,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        int diasAlquiler,
        String estado,
        BigDecimal subtotal,
        BigDecimal total,
        String direccionEvento,
        String metodoPago,
        List<ItemResponse> items
) {
    public record ItemResponse(
            String productoId,
            String productoNombre,
            int cantidad,
            BigDecimal precioPorDia,
            BigDecimal subtotal
    ) {}

    public static ReservationResponse from(Reservation r) {
        List<ItemResponse> items = r.getItems() == null ? List.of() :
                r.getItems().stream().map(i -> new ItemResponse(
                        i.getProductoId(),
                        i.getProductoNombre(),
                        i.getCantidad(),
                        i.getPrecioPorDia(),
                        i.getSubtotal()
                )).toList();

        return new ReservationResponse(
                r.getId(),
                r.getUsuarioNombre(),
                r.getUsuarioEmail(),
                r.getTipoEvento(),
                r.getFechaInicio(),
                r.getFechaFin(),
                r.getDiasAlquiler(),
                r.getEstado(),
                r.getSubtotal(),
                r.getTotal(),
                r.getDireccionEvento(),
                r.getMetodoPago(),
                items
        );
    }
}
