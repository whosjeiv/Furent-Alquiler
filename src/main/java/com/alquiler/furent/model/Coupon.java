package com.alquiler.furent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Document(collection = "cupones")
public class Coupon {

    @Id
    private String id;

    private String tenantId;
    private String codigo;
    private String tipo; // PORCENTAJE, MONTO_FIJO
    private BigDecimal valor;
    private LocalDate validoDesde;
    private LocalDate validoHasta;
    private int usosMaximos;
    private int usosActuales;
    private BigDecimal montoMinimo;
    private boolean activo;
    private List<String> categoriasAplicables;

    public Coupon() {
        this.activo = true;
        this.usosActuales = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public LocalDate getValidoDesde() { return validoDesde; }
    public void setValidoDesde(LocalDate validoDesde) { this.validoDesde = validoDesde; }

    public LocalDate getValidoHasta() { return validoHasta; }
    public void setValidoHasta(LocalDate validoHasta) { this.validoHasta = validoHasta; }

    public int getUsosMaximos() { return usosMaximos; }
    public void setUsosMaximos(int usosMaximos) { this.usosMaximos = usosMaximos; }

    public int getUsosActuales() { return usosActuales; }
    public void setUsosActuales(int usosActuales) { this.usosActuales = usosActuales; }

    public BigDecimal getMontoMinimo() { return montoMinimo; }
    public void setMontoMinimo(BigDecimal montoMinimo) { this.montoMinimo = montoMinimo; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public List<String> getCategoriasAplicables() { return categoriasAplicables; }
    public void setCategoriasAplicables(List<String> categoriasAplicables) { this.categoriasAplicables = categoriasAplicables; }

    public boolean isValid() {
        LocalDate now = LocalDate.now();
        return activo
                && (validoDesde == null || !now.isBefore(validoDesde))
                && (validoHasta == null || !now.isAfter(validoHasta))
                && (usosMaximos <= 0 || usosActuales < usosMaximos);
    }

    public BigDecimal calcularDescuento(BigDecimal monto) {
        if (!isValid() || monto.compareTo(montoMinimo != null ? montoMinimo : BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        BigDecimal valorAbs = valor.abs();
        if ("PORCENTAJE".equals(tipo)) {
            return monto.multiply(valorAbs).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            return valorAbs.min(monto);
        }
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
