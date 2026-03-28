package com.alquiler.furent.service;

import com.alquiler.furent.exception.InvalidOperationException;
import com.alquiler.furent.model.Coupon;
import com.alquiler.furent.repository.CouponRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /**
     * Valida un cupón verificando existencia, vigencia y límite de usos.
     * 
     * @param codigo Código del cupón a validar
     * @return Cupón válido
     * @throws InvalidOperationException si el cupón no existe, no está vigente o alcanzó el límite de usos
     */
    public Coupon validateCoupon(String codigo) {
        Optional<Coupon> opt = couponRepository.findByCodigoIgnoreCase(codigo);
        
        if (opt.isEmpty()) {
            throw new InvalidOperationException("Cupón no encontrado");
        }
        
        Coupon coupon = opt.get();
        
        if (!coupon.isActivo()) {
            throw new InvalidOperationException("El cupón no está activo");
        }
        
        if (!coupon.isVigente()) {
            throw new InvalidOperationException("El cupón ha expirado o aún no es válido");
        }
        
        if (coupon.hasReachedLimit()) {
            throw new InvalidOperationException("El cupón ha alcanzado su límite de usos");
        }
        
        return coupon;
    }

    /**
     * Aplica un descuento de cupón al total.
     * Fórmula: totalConDescuento = total * (1 - descuento/100)
     * 
     * @param coupon Cupón a aplicar
     * @param total Total original
     * @return Total con descuento aplicado
     */
    public BigDecimal applyDiscount(Coupon coupon, BigDecimal total) {
        if ("PORCENTAJE".equals(coupon.getTipo())) {
            // totalConDescuento = total * (1 - descuento/100)
            BigDecimal descuentoDecimal = coupon.getValor().divide(BigDecimal.valueOf(100));
            BigDecimal factor = BigDecimal.ONE.subtract(descuentoDecimal);
            return total.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        } else if ("MONTO_FIJO".equals(coupon.getTipo())) {
            // Restar el monto fijo, pero no permitir valores negativos
            BigDecimal resultado = total.subtract(coupon.getValor());
            return resultado.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }
        return total;
    }

    /**
     * Incrementa el contador de usos de un cupón.
     * 
     * @param couponId ID del cupón
     * @throws InvalidOperationException si el cupón alcanzó el límite de usos
     */
    public void incrementUsage(String couponId) {
        Optional<Coupon> opt = couponRepository.findById(couponId);
        
        if (opt.isEmpty()) {
            throw new InvalidOperationException("Cupón no encontrado");
        }
        
        Coupon coupon = opt.get();
        
        if (coupon.hasReachedLimit()) {
            throw new InvalidOperationException("El cupón ha alcanzado su límite de usos");
        }
        
        coupon.setUsosActuales(coupon.getUsosActuales() + 1);
        couponRepository.save(coupon);
        
        log.info("Cupón {} incrementado. Usos: {}/{}", coupon.getCodigo(), coupon.getUsosActuales(), coupon.getUsosMaximos());
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
