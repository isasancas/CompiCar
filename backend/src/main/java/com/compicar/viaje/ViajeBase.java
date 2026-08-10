package com.compicar.viaje;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.compicar.persona.Persona;
import com.compicar.vehiculo.Vehiculo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class ViajeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaHoraSalida;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoViaje estado;

    @Column(nullable = false)
    private Integer plazasDisponibles;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @ManyToOne
    @JoinColumn(name = "persona_id", nullable = false)
    @JsonIgnoreProperties({"viajes", "reservas", "vehiculos"})
    private Persona persona;

    @ManyToOne
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    public ViajeBase() {
    }

    public ViajeBase(LocalDateTime fechaHoraSalida, EstadoViaje estado, Integer plazasDisponibles, BigDecimal precio, Persona persona, Vehiculo vehiculo,
        String slug
    ) {
        this.fechaHoraSalida = fechaHoraSalida;
        this.estado = estado;
        this.plazasDisponibles = plazasDisponibles;
        this.precio = precio;
        this.persona = persona;
        this.vehiculo = vehiculo;
        this.slug = slug;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public LocalDateTime getFechaHoraSalida() {
        return fechaHoraSalida;
    }

    public EstadoViaje getEstado() {
        return estado;
    }

    public Integer getPlazasDisponibles() {
        return plazasDisponibles;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public String getSlug() {
        return slug;
    }

    public Persona getPersona() {
        return persona;
    }

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    // Setters
    public void setFechaHoraSalida(LocalDateTime fechaHoraSalida) {
        this.fechaHoraSalida = fechaHoraSalida;
    }

    public void setEstado(EstadoViaje estado) {
        this.estado = estado;
    }

    public void setPlazasDisponibles(Integer plazasDisponibles) {
        this.plazasDisponibles = plazasDisponibles;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setPersona(Persona persona) {
        this.persona = persona;
    }

    public void setVehiculo(Vehiculo vehiculo) {
        this.vehiculo = vehiculo;
    }
}
