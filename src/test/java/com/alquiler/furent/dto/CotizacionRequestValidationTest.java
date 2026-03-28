package com.alquiler.furent.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CotizacionRequest - Bean Validation")
class CotizacionRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private CotizacionRequest createValidRequest() {
        CotizacionRequest request = new CotizacionRequest();
        request.setTipoEvento("Boda");
        request.setInvitados(100);
        request.setFechaInicio(LocalDate.now().plusDays(5));
        request.setFechaFin(LocalDate.now().plusDays(7));
        request.setDireccion("Calle 123 #45-67");
        request.setNotas("Notas del evento");
        request.setMetodoPago("Efectivo en Oficina");
        
        List<CotizacionRequest.CartItem> items = new ArrayList<>();
        CotizacionRequest.CartItem item = new CotizacionRequest.CartItem();
        item.setId("prod123");
        item.setName("Mesa");
        item.setPrice(BigDecimal.valueOf(50));
        item.setQty(10);
        items.add(item);
        
        request.setItems(items);
        return request;
    }

    @Test
    @DisplayName("Request válido no debe tener violaciones")
    void validRequest_shouldHaveNoViolations() {
        CotizacionRequest request = createValidRequest();
        Set<ConstraintViolation<CotizacionRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "No debería haber violaciones en un request válido");
    }

    @Test
    @DisplayName("tipoEvento con caracteres especiales debe fallar validación @Pattern")
    void tipoEvento_withSpecialCharacters_shouldFailValidation() {
        CotizacionRequest request = createValidRequest();
        request.setTipoEvento("Boda@2024!");
        
        Set<ConstraintViolation<CotizacionRequest>> violations = validator.validate(request);
        
        boolean hasPatternViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("tipoEvento") &&
                          v.getMessage().contains("letras, números y espacios"));
        
        assertTrue(hasPatternViolation, "Debe fallar validación @Pattern para caracteres especiales");
    }

    @Test
    @DisplayName("tipoEvento con letras acentuadas debe pasar validación")
    void tipoEvento_withAccentedLetters_shouldPassValidation() {
        CotizacionRequest request = createValidRequest();
        request.setTipoEvento("Celebración Año Nuevo");
        
        Set<ConstraintViolation<CotizacionRequest>> violations = validator.validate(request);
        
        boolean hasPatternViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("tipoEvento"));
        
        assertFalse(hasPatternViolation, "Debe aceptar letras acentuadas");
    }

    @Test
    @DisplayName("invitados = 0 debe fallar validación @Min")
    void invitados_zero_shouldFailValidation() {
        CotizacionRequest request = createValidRequest();
        request.setInvitados(0);
        
        Set<ConstraintViolation<CotizacionRequest>> violations = validator.validate(request);
        
        boolean hasMinViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("invitados") &&
                          v.getMessage().contains("al menos 1"));
        
        assertTrue(hasMinViolation, "Debe fallar validación @Min para invitados = 0");
    }

    @Test
    @DisplayName("invitados = 10001 debe fallar validación @Max")
    void invitados_overLimit_shouldFailValidation() {
        CotizacionRequest request = createValidRequest();
        request.setInvitados(10001);
        
        Set<ConstraintViolation<CotizacionRequest>> violations = validator.validate(request);
        
        boolean hasMaxViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("invitados") &&
                          v.getMessage().contains("10,000"));
        
        assertTrue(hasMaxViolation, "Debe fallar validación @Max para invitados > 10,000");
    }

    @Test
    @DisplayName("invitados = 10000 debe pasar validación")
    void invitados_atLimit_shouldPassValidation() {
        CotizacionRequest request = createValidRequest();
        request.setInvitados(10000);
        
        Set<ConstraintViolation<CotizacionRequest>> violations = validator.validate(request);
        
        boolean hasInvitadosViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("invitados"));
        
        assertFalse(hasInvitadosViolation, "Debe aceptar invitados = 10,000");
    }

    @Test
    @DisplayName("notas con más de 1000 caracteres debe fallar validación @Size")
    void notas_overLimit_shouldFailValidation() {
        CotizacionRequest request = createValidRequest();
        String longNotes = "a".repeat(1001);
        request.setNotas(longNotes);
        
        Set<ConstraintViolation<CotizacionRequest>> violations = validator.validate(request);
        
        boolean hasSizeViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("notas") &&
                          v.getMessage().contains("1000"));
        
        assertTrue(hasSizeViolation, "Debe fallar validación @Size para notas > 1000 caracteres");
    }

    @Test
    @DisplayName("items vacío debe fallar validación @NotEmpty")
    void items_empty_shouldFailValidation() {
        CotizacionRequest request = createValidRequest();
        request.setItems(new ArrayList<>());
        
        Set<ConstraintViolation<CotizacionRequest>> violations = validator.validate(request);
        
        boolean hasNotEmptyViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("items") &&
                          v.getMessage().contains("al menos un producto"));
        
        assertTrue(hasNotEmptyViolation, "Debe fallar validación @NotEmpty para items vacío");
    }

    @Test
    @DisplayName("fechaFin anterior a fechaInicio debe fallar validación @AssertTrue")
    void fechaFin_beforeFechaInicio_shouldFailValidation() {
        CotizacionRequest request = createValidRequest();
        request.setFechaInicio(LocalDate.now().plusDays(10));
        request.setFechaFin(LocalDate.now().plusDays(5));
        
        Set<ConstraintViolation<CotizacionRequest>> violations = validator.validate(request);
        
        boolean hasAssertTrueViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("fechaFinValid") &&
                          v.getMessage().contains("fecha de fin"));
        
        assertTrue(hasAssertTrueViolation, "Debe fallar validación @AssertTrue cuando fechaFin < fechaInicio");
    }

    @Test
    @DisplayName("fechaFin igual a fechaInicio debe pasar validación")
    void fechaFin_equalToFechaInicio_shouldPassValidation() {
        CotizacionRequest request = createValidRequest();
        LocalDate fecha = LocalDate.now().plusDays(5);
        request.setFechaInicio(fecha);
        request.setFechaFin(fecha);
        
        Set<ConstraintViolation<CotizacionRequest>> violations = validator.validate(request);
        
        boolean hasDateViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("fechaFinValid"));
        
        assertFalse(hasDateViolation, "Debe aceptar fechaFin igual a fechaInicio");
    }
}
