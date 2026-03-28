package com.alquiler.furent.controller.admin;

import com.alquiler.furent.exception.InvalidOperationException;
import com.alquiler.furent.model.Coupon;
import com.alquiler.furent.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Controlador para gestión administrativa de cupones de descuento.
 * Permite CRUD completo: listar, crear, editar, eliminar.
 * Valida que los descuentos porcentuales no excedan el 100%.
 */
@Controller
@RequestMapping("/admin/cupones")
@Tag(name = "Admin - Cupones", description = "Gestión administrativa de cupones de descuento (solo administradores)")
public class AdminCuponesController {

    private final CouponService couponService;

    public AdminCuponesController(CouponService couponService) {
        this.couponService = couponService;
    }

    /**
     * Lista todos los cupones con contador de usos actuales vs límite.
     * 
     * @param model Modelo para la vista
     * @return Vista de lista de cupones
     */
    @Operation(summary = "Listar cupones", 
               description = "Lista todos los cupones del sistema con información de usos actuales y límites")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de cupones obtenida exitosamente"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - requiere rol ADMIN")
    })
    @GetMapping
    public String listCoupons(Model model) {
        model.addAttribute("cupones", couponService.getAll());
        return "admin/cupones";
    }

    /**
     * Guarda un cupón (crear o editar).
     * Valida que el descuento porcentual no exceda 100%.
     * 
     * @param codigo Código del cupón
     * @param tipo Tipo de cupón (PORCENTAJE o MONTO_FIJO)
     * @param valor Valor del descuento
     * @param validoDesde Fecha de inicio de vigencia
     * @param validoHasta Fecha de fin de vigencia
     * @param usosMaximos Límite de usos
     * @param montoMinimo Monto mínimo para aplicar el cupón
     * @param id ID del cupón (para edición)
     * @param redirectAttributes Atributos para mensaje flash
     * @return Redirección a lista de cupones
     */
    @Operation(summary = "Guardar cupón", 
               description = "Crea un nuevo cupón o actualiza uno existente. Valida que el descuento porcentual no exceda 100% y que todos los valores sean positivos")
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Cupón guardado exitosamente, redirige a lista de cupones"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos (descuento > 100%, valores negativos, etc.)"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - requiere rol ADMIN")
    })
    @PostMapping("/guardar")
    public String saveCoupon(@RequestParam String codigo, @RequestParam String tipo,
            @RequestParam BigDecimal valor, @RequestParam String validoDesde,
            @RequestParam String validoHasta, @RequestParam int usosMaximos,
            @RequestParam(defaultValue = "0") BigDecimal montoMinimo,
            @RequestParam(required = false) String id,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Validar que el descuento porcentual no exceda 100%
            if ("PORCENTAJE".equals(tipo) && valor.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new InvalidOperationException("El descuento porcentual no puede exceder el 100%");
            }
            
            // Validar que el valor sea positivo
            if (valor.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidOperationException("El valor del descuento debe ser mayor a 0");
            }
            
            // Validar que usosMaximos sea positivo
            if (usosMaximos <= 0) {
                throw new InvalidOperationException("El límite de usos debe ser mayor a 0");
            }
            
            Coupon coupon;
            if (id != null && !id.isEmpty()) {
                coupon = couponService.getById(id).orElse(new Coupon());
            } else {
                coupon = new Coupon();
            }
            
            coupon.setCodigo(codigo.toUpperCase().trim());
            coupon.setTipo(tipo);
            coupon.setValor(valor);
            coupon.setValidoDesde(LocalDate.parse(validoDesde));
            coupon.setValidoHasta(LocalDate.parse(validoHasta));
            coupon.setUsosMaximos(usosMaximos);
            coupon.setMontoMinimo(montoMinimo);
            coupon.setActivo(true);
            
            couponService.save(coupon);
            redirectAttributes.addFlashAttribute("success", "Cupón guardado exitosamente");
            
        } catch (InvalidOperationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar cupón: " + e.getMessage());
        }
        
        return "redirect:/admin/cupones";
    }

    /**
     * Elimina un cupón.
     * 
     * @param id ID del cupón
     * @param redirectAttributes Atributos para mensaje flash
     * @return Redirección a lista de cupones
     */
    @Operation(summary = "Eliminar cupón", 
               description = "Elimina un cupón del sistema de forma permanente")
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Cupón eliminado exitosamente, redirige a lista de cupones"),
        @ApiResponse(responseCode = "404", description = "Cupón no encontrado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - requiere rol ADMIN")
    })
    @DeleteMapping("/{id}")
    public String deleteCoupon(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            couponService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Cupón eliminado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar cupón: " + e.getMessage());
        }
        return "redirect:/admin/cupones";
    }
}
