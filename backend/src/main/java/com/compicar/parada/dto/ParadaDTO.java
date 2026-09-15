package com.compicar.parada.dto;

import java.math.BigDecimal;

public class ParadaDTO {
    private Long id;
    private String localizacion;
    private String tipo;
    private Integer orden;
    private BigDecimal latitud;
    private BigDecimal longitud;

    public ParadaDTO() {
    }

    public ParadaDTO(Long id, String localizacion, String tipo, Integer orden) {
        this.id = id;
        this.localizacion = localizacion;
        this.tipo = tipo;
        this.orden = orden;
    }

    public ParadaDTO(Long id, String localizacion, String tipo, Integer orden,
            BigDecimal latitud, BigDecimal longitud) {
        this(id, localizacion, tipo, orden);
        this.latitud = latitud;
        this.longitud = longitud;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLocalizacion() {
        return localizacion;
    }

    public void setLocalizacion(String localizacion) {
        this.localizacion = localizacion;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public void setLatitud(BigDecimal latitud) {
        this.latitud = latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public void setLongitud(BigDecimal longitud) {
        this.longitud = longitud;
    }
}