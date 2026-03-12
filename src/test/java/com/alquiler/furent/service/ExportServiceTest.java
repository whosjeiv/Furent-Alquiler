package com.alquiler.furent.service;

import com.alquiler.furent.model.Product;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.model.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExportServiceTest {

    private final ExportService exportService = new ExportService();

    @Test
    void exportProductsCsv_withProducts_generatesValidCsv() {
        Product p = new Product();
        p.setId("p1");
        p.setNombre("Silla Tiffany");
        p.setCategoriaNombre("Sillas");
        p.setPrecioPorDia(BigDecimal.valueOf(15000));
        p.setStock(50);
        p.setEstadoMantenimiento("BUENO");
        p.setDisponible(true);
        p.setMaterial("Madera");
        p.setCalificacion(4.5);

        byte[] csv = exportService.exportProductsCsv(List.of(p));
        String content = new String(csv);

        assertTrue(content.contains("ID,Nombre"));
        assertTrue(content.contains("p1"));
        assertTrue(content.contains("Silla Tiffany"));
        assertTrue(content.contains("Sillas"));
        assertTrue(content.contains("15000"));
        assertTrue(content.contains("Sí"));
    }

    @Test
    void exportProductsCsv_emptyList_returnsOnlyHeader() {
        byte[] csv = exportService.exportProductsCsv(List.of());
        String content = new String(csv);

        assertTrue(content.startsWith("ID,Nombre"));
        assertEquals(1, content.split("\n").length);
    }

    @Test
    void exportProductsCsv_withCommaInName_escapesCorrectly() {
        Product p = new Product();
        p.setId("p2");
        p.setNombre("Mesa, grande");
        p.setDisponible(false);

        byte[] csv = exportService.exportProductsCsv(List.of(p));
        String content = new String(csv);

        assertTrue(content.contains("\"Mesa, grande\""));
    }

    @Test
    void exportUsersCsv_withUsers_generatesValidCsv() {
        User u = new User();
        u.setId("u1");
        u.setNombre("Juan");
        u.setApellido("Pérez");
        u.setEmail("juan@test.com");
        u.setTelefono("3001234567");
        u.setRole("USER");
        u.setActivo(true);

        byte[] csv = exportService.exportUsersCsv(List.of(u));
        String content = new String(csv);

        assertTrue(content.contains("ID,Nombre"));
        assertTrue(content.contains("Juan"));
        assertTrue(content.contains("Pérez"));
        assertTrue(content.contains("juan@test.com"));
        assertTrue(content.contains("Sí"));
    }

    @Test
    void exportReservationsCsv_withReservations_generatesValidCsv() {
        Reservation r = new Reservation();
        r.setId("r1");
        r.setUsuarioNombre("María");
        r.setUsuarioEmail("maria@test.com");
        r.setTipoEvento("Boda");
        r.setEstado("CONFIRMADA");
        r.setTotal(BigDecimal.valueOf(250000));

        byte[] csv = exportService.exportReservationsCsv(List.of(r));
        String content = new String(csv);

        assertTrue(content.contains("ID,Cliente"));
        assertTrue(content.contains("r1"));
        assertTrue(content.contains("María"));
        assertTrue(content.contains("Boda"));
    }
}
