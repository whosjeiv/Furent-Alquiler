package com.alquiler.furent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "pagos_trabajadores")
public class WorkerPayment {

    @Id
    private String id;

    @Indexed
    private String trabajadorNombre;

    private String trabajadorEmail;
    private String trabajadorRol;
    private String periodoTipo; // MENSUAL o QUINCENAL
    private LocalDate periodoInicio;
    private LocalDate periodoFin;
    private BigDecimal monto;
    private BigDecimal tarifaHora;
    private Integer horasTrabajadas;
    private Integer eventosAtendidos;
    private String moneda;
    private String notas;
    private String estado; // GENERADO, ENVIADO
    private LocalDateTime fechaCreacion;

    public WorkerPayment() {
        this.estado = "GENERADO";
        this.fechaCreacion = LocalDateTime.now();
        this.moneda = "USD";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTrabajadorNombre() {
        return trabajadorNombre;
    }

    public void setTrabajadorNombre(String trabajadorNombre) {
        this.trabajadorNombre = trabajadorNombre;
    }

    public String getTrabajadorEmail() {
        return trabajadorEmail;
    }

    public void setTrabajadorEmail(String trabajadorEmail) {
        this.trabajadorEmail = trabajadorEmail;
    }

    public String getTrabajadorRol() {
        return trabajadorRol;
    }

    public void setTrabajadorRol(String trabajadorRol) {
        this.trabajadorRol = trabajadorRol;
    }

    public String getPeriodoTipo() {
        return periodoTipo;
    }

    public void setPeriodoTipo(String periodoTipo) {
        this.periodoTipo = periodoTipo;
    }

    public LocalDate getPeriodoInicio() {
        return periodoInicio;
    }

    public void setPeriodoInicio(LocalDate periodoInicio) {
        this.periodoInicio = periodoInicio;
    }

    public LocalDate getPeriodoFin() {
        return periodoFin;
    }

    public void setPeriodoFin(LocalDate periodoFin) {
        this.periodoFin = periodoFin;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public BigDecimal getTarifaHora() {
        return tarifaHora;
    }

    public void setTarifaHora(BigDecimal tarifaHora) {
        this.tarifaHora = tarifaHora;
    }

    public Integer getHorasTrabajadas() {
        return horasTrabajadas;
    }

    public void setHorasTrabajadas(Integer horasTrabajadas) {
        this.horasTrabajadas = horasTrabajadas;
    }

    public Integer getEventosAtendidos() {
        return eventosAtendidos;
    }

    public void setEventosAtendidos(Integer eventosAtendidos) {
        this.eventosAtendidos = eventosAtendidos;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}

