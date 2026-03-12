package com.alquiler.furent.service;

import com.alquiler.furent.model.Coupon;
import com.alquiler.furent.repository.CouponRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

/**
 * Servicio de gestión y validación de cupones de descuento.
 * Verifica vigencia, monto mínimo y usos disponibles antes de aplicar descuentos.
 *
 * @author Furent Team
 * @since 1.0
 */
@Service
public class CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    public Map<String, Object> validateCoupon(String codigo, BigDecimal montoTotal) {
        Map<String, Object> result = new HashMap<>();
        Optional<Coupon> opt = couponRepository.findByCodigoIgnoreCase(codigo);

        if (opt.isEmpty()) {
            result.put("valido", false);
            result.put("mensaje", "Cupón no encontrado");
            return result;
        }

        Coupon coupon = opt.get();

        if (!coupon.isValid()) {
            result.put("valido", false);
            result.put("mensaje", "El cupón ha expirado o ya no está disponible");
            return result;
        }

        BigDecimal minimo = coupon.getMontoMinimo() != null ? coupon.getMontoMinimo() : BigDecimal.ZERO;
        if (montoTotal.compareTo(minimo) < 0) {
            result.put("valido", false);
            result.put("mensaje", "El monto mínimo para este cupón es $" + String.format("%,.0f", minimo));
            return result;
        }

        BigDecimal descuento = coupon.calcularDescuento(montoTotal);
        result.put("valido", true);
        result.put("descuento", descuento);
        result.put("montoFinal", montoTotal.subtract(descuento));
        result.put("mensaje", "Cupón aplicado: -$" + String.format("%,.0f", descuento));
        return result;
    }

    public void useCoupon(String codigo) {
        couponRepository.findByCodigoIgnoreCase(codigo).ifPresent(c -> {
            c.setUsosActuales(c.getUsosActuales() + 1);
            couponRepository.save(c);
            log.info("Cupón {} usado. Usos: {}/{}", codigo, c.getUsosActuales(), c.getUsosMaximos());
        });
    }

    @CacheEvict(value = "coupons", allEntries = true)
    public Coupon save(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    @Cacheable(value = "coupons")
    public List<Coupon> getAll() {
        return couponRepository.findAll();
    }

    public Optional<Coupon> getById(String id) {
        return couponRepository.findById(id);
    }

    @CacheEvict(value = "coupons", allEntries = true)
    public void delete(String id) {
        couponRepository.deleteById(id);
    }
}
