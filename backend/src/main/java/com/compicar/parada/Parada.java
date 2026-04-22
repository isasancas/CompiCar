package com.compicar.parada;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.compicar.viaje.Viaje;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "parada")
public class Parada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false)
    private String localizacion;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoParada tipo;

    @Column(nullable = false)
    private Integer orden;

    @ManyToOne
    @JoinColumn(name = "viaje_id", nullable = false)
    private Viaje viaje;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitud;

    // Constructores
    public Parada() {
    }

    public Parada(LocalDateTime fechaHora, String localizacion, TipoParada tipo, Integer orden, Viaje viaje) {
        this.fechaHora = fechaHora;
        this.localizacion = localizacion;
        this.tipo = tipo;
        this.orden = orden;
        this.viaje = viaje;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getLocalizacion() {
        return localizacion;
    }

    public Viaje getViaje() {
        return viaje;
    }

    public TipoParada getTipo() {
        return tipo;
    }

    public Integer getOrden() {
        return orden;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    // Setters
    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public void setLocalizacion(String localizacion) {
        this.localizacion = localizacion;
    }

    public void setViaje(Viaje viaje) {
        this.viaje = viaje;
    }

    public void setTipo(TipoParada tipo) {
        this.tipo = tipo;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public void setLatitud(BigDecimal latitud) {
        this.latitud = latitud;
    }

    public void setLongitud(BigDecimal longitud) {
        this.longitud = longitud;
    }

    @Override
    public String toString() {
        return "Parada{" +
                "id=" + id +
                ", fechaHora=" + fechaHora +
                ", localizacion='" + localizacion + '\'' +
                ", tipo=" + tipo +
                ", orden=" + orden +
                ", viajeId=" + (viaje != null ? viaje.getId() : null) +
                ", latitud=" + latitud +
                ", longitud=" + longitud +
                '}';
    }
    
}
