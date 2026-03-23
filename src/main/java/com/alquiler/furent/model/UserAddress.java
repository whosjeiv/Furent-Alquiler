package com.alquiler.furent.model;

import java.util.UUID;

public class UserAddress {
    
    private String id;
    private String alias; // Casa, Oficina, etc
    private String direccion;
    private String ciudad;
    private String estadoProvincia;
    private String pais;
    private String codigoPostal;
    private boolean predeterminada;

    public UserAddress() {
        this.id = UUID.randomUUID().toString();
    }

    public UserAddress(String alias, String direccion, String ciudad, String estadoProvincia, String pais, String codigoPostal, boolean predeterminada) {
        this();
        this.alias = alias;
        this.direccion = direccion;
        this.ciudad = ciudad;
        this.estadoProvincia = estadoProvincia;
        this.pais = pais;
        this.codigoPostal = codigoPostal;
        this.predeterminada = predeterminada;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getEstadoProvincia() {
        return estadoProvincia;
    }

    public void setEstadoProvincia(String estadoProvincia) {
        this.estadoProvincia = estadoProvincia;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal = codigoPostal;
    }

    public boolean isPredeterminada() {
        return predeterminada;
    }

    public void setPredeterminada(boolean predeterminada) {
        this.predeterminada = predeterminada;
    }
}
