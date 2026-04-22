package com.compicar.viaje.dto;

public class ParadaDTO {
    private Long id;
    private String localizacion;
    private String tipo;
    private Integer orden;

    public ParadaDTO() {
    }

    public ParadaDTO(Long id, String localizacion, String tipo, Integer orden) {
        this.id = id;
        this.localizacion = localizacion;
        this.tipo = tipo;
        this.orden = orden;
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
}