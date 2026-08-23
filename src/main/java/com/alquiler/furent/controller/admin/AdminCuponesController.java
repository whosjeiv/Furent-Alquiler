package com.alquiler.furent.controller.admin;

import com.alquiler.furent.model.Coupon;
import com.alquiler.furent.service.CouponService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
public class AdminCuponesController {

    private final CouponService couponService;

    public AdminCuponesController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/cupones")
    public String listCoupons(Model model) {
        model.addAttribute("cupones", couponService.getAll());
        return "admin/cupones";
    }

    @PostMapping("/cupones/guardar")
    public String saveCoupon(@RequestParam String codigo, @RequestParam String tipo,
            @RequestParam BigDecimal valor, @RequestParam String validoDesde,
            @RequestParam String validoHasta, @RequestParam int usosMaximos,
            @RequestParam(defaultValue = "0") BigDecimal montoMinimo,
            @RequestParam(required = false) String id,
            RedirectAttributes redirectAttributes) {
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
        return "redirect:/admin/cupones";
    }

    @PostMapping("/cupones/eliminar/{id}")
    public String deleteCoupon(@PathVariable String id, RedirectAttributes redirectAttributes) {
        couponService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Cupón eliminado");
        return "redirect:/admin/cupones";
    }
}
