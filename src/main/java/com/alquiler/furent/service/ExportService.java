package com.alquiler.furent.service;

import com.alquiler.furent.model.Product;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.model.User;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio de exportación de datos a formato CSV.
 * Genera archivos CSV con escape correcto para productos, reservas y usuarios.
 * Los datos se codifican en UTF-8.
 *
 * @author Furent Team
 * @since 1.0
 */
@Service
public class ExportService {

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public byte[] exportProductsCsv(List<Product> products) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Nombre,Categoría,Precio/Día,Stock,Estado,Disponible,Material,Calificación\n");
        for (Product p : products) {
            sb.append(escapeCsv(p.getId())).append(",");
            sb.append(escapeCsv(p.getNombre())).append(",");
            sb.append(escapeCsv(p.getCategoriaNombre())).append(",");
            sb.append(p.getPrecioPorDia()).append(",");
            sb.append(p.getStock()).append(",");
            sb.append(escapeCsv(p.getEstadoMantenimiento())).append(",");
            sb.append(p.isDisponible() ? "Sí" : "No").append(",");
            sb.append(escapeCsv(p.getMaterial())).append(",");
            sb.append(p.getCalificacion()).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] exportReservationsCsv(List<Reservation> reservations) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Cliente,Email,Tipo Evento,Estado,Fecha Inicio,Fecha Fin,Días,Subtotal,Total,Método Pago,Dirección,Fecha Creación\n");
        for (Reservation r : reservations) {
            sb.append(escapeCsv(r.getId())).append(",");
            sb.append(escapeCsv(r.getUsuarioNombre())).append(",");
            sb.append(escapeCsv(r.getUsuarioEmail())).append(",");
            sb.append(escapeCsv(r.getTipoEvento())).append(",");
            sb.append(escapeCsv(r.getEstado())).append(",");
            sb.append(r.getFechaInicio()).append(",");
            sb.append(r.getFechaFin()).append(",");
            sb.append(r.getDiasAlquiler()).append(",");
            sb.append(r.getSubtotal()).append(",");
            sb.append(r.getTotal()).append(",");
            sb.append(escapeCsv(r.getMetodoPago())).append(",");
            sb.append(escapeCsv(r.getDireccionEvento())).append(",");
            sb.append(r.getFechaCreacion()).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] exportUsersCsv(List<User> users) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Nombre,Apellido,Email,Teléfono,Rol,Activo,Fecha Registro\n");
        for (User u : users) {
            sb.append(escapeCsv(u.getId())).append(",");
            sb.append(escapeCsv(u.getNombre())).append(",");
            sb.append(escapeCsv(u.getApellido())).append(",");
            sb.append(escapeCsv(u.getEmail())).append(",");
            sb.append(escapeCsv(u.getTelefono())).append(",");
            sb.append(escapeCsv(u.getRole())).append(",");
            sb.append(u.isActivo() ? "Sí" : "No").append(",");
            sb.append(u.getFechaCreacion()).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
