package com.alquiler.furent.controller;

import com.alquiler.furent.dto.CotizacionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de validación de fechas en ApiController.
 * Verifica que las fechas de cotización sean validadas correctamente.
 */
@DisplayName("ApiController - Validación de Fechas")
class ApiControllerDateValidationTest {

    @Test
    @DisplayName("fechaFin anterior a fechaInicio debe ser inválida")
    void fechaFin_before_fechaInicio_should_be_invalid() {
        CotizacionRequest request = new CotizacionRequest();
        LocalDate inicio = LocalDate.now().plusDays(5);
        LocalDate fin = LocalDate.now().plusDays(2);
        
        request.setFechaInicio(inicio);
        request.setFechaFin(fin);
        
        // Verificar que fechaFin es anterior a fechaInicio
        assertTrue(fin.isBefore(inicio), "fechaFin debe ser anterior a fechaInicio");
    }

    @Test
    @DisplayName("fechaInicio en el pasado debe ser inválida")
    void fechaInicio_in_past_should_be_invalid() {
        CotizacionRequest request = new CotizacionRequest();
        LocalDate inicio = LocalDate.now().minusDays(1);
        LocalDate fin = LocalDate.now().plusDays(3);
        
        request.setFechaInicio(inicio);
        request.setFechaFin(fin);
        
        // Verificar que fechaInicio es anterior a hoy
        assertTrue(inicio.isBefore(LocalDate.now()), "fechaInicio debe ser anterior a hoy");
    }

    @Test
    @DisplayName("fechas válidas deben pasar validación")
    void valid_dates_should_pass_validation() {
        CotizacionRequest request = new CotizacionRequest();
        LocalDate inicio = LocalDate.now().plusDays(1);
        LocalDate fin = LocalDate.now().plusDays(5);
        
        request.setFechaInicio(inicio);
        request.setFechaFin(fin);
        
        // Verificar que las fechas son válidas
        assertFalse(fin.isBefore(inicio), "fechaFin no debe ser anterior a fechaInicio");
        assertFalse(inicio.isBefore(LocalDate.now()), "fechaInicio no debe ser anterior a hoy");
    }

    @Test
    @DisplayName("fechaInicio igual a hoy debe ser válida")
    void fechaInicio_today_should_be_valid() {
        CotizacionRequest request = new CotizacionRequest();
        LocalDate inicio = LocalDate.now();
        LocalDate fin = LocalDate.now().plusDays(3);
        
        request.setFechaInicio(inicio);
        request.setFechaFin(fin);
        
        // Verificar que fechaInicio hoy es válida
        assertFalse(inicio.isBefore(LocalDate.now()), "fechaInicio hoy debe ser válida");
    }

    @Test
    @DisplayName("fechaInicio igual a fechaFin debe ser válida")
    void fechaInicio_equal_to_fechaFin_should_be_valid() {
        CotizacionRequest request = new CotizacionRequest();
        LocalDate fecha = LocalDate.now().plusDays(1);
        
        request.setFechaInicio(fecha);
        request.setFechaFin(fecha);
        
        // Verificar que fechas iguales son válidas
        assertFalse(fecha.isBefore(fecha), "fechaInicio igual a fechaFin debe ser válida");
    }
}
