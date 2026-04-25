package com.compicar.reserva;

import java.time.LocalDateTime;
import java.util.List;

import com.compicar.checkin.Checkin;
import com.compicar.pago.Pago;
import com.compicar.parada.Parada;
import com.compicar.persona.Persona;
import com.compicar.viaje.Viaje;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoReserva estado;

    @Column(nullable = false)
    private LocalDateTime fechaHoraReserva;

    @ManyToOne
    @JoinColumn(name = "persona_id", nullable = false)
    @JsonIgnoreProperties({"reservas", "viajes"})
    private Persona persona;

    @ManyToOne
    @JoinColumn(name = "parada_subida_id", nullable = false)
    private Parada paradaSubida;

    @ManyToOne
    @JoinColumn(name = "parada_bajada_id", nullable = false)
    private Parada paradaBajada;

    @OneToMany(mappedBy = "reserva")
    @JsonIgnore
    private List<Checkin> checkins;

    @OneToOne(mappedBy = "reserva")
    @JsonIgnore
    private Pago pago;

    @ManyToOne
    @JoinColumn(name = "viaje_id", nullable = false)
    @JsonIgnoreProperties({"reservas", "persona"})
    private Viaje viaje;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @Column(nullable = false)
    private Integer cantidadPlazas;

    // Constructores
    public Reserva() {
    }

    public Reserva(EstadoReserva estado, LocalDateTime fechaHoraReserva, Persona persona, Parada paradaSubida,
            Parada paradaBajada, Viaje viaje, Integer cantidadPlazas) {
        this.estado = estado;
        this.fechaHoraReserva = fechaHoraReserva;
        this.persona = persona;
        this.paradaSubida = paradaSubida;
        this.paradaBajada = paradaBajada;
        this.viaje = viaje;
        this.cantidadPlazas = cantidadPlazas;
        this.slug = "reserva-" + id;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public LocalDateTime getFechaHoraReserva() {
        return fechaHoraReserva;
    }

    public Persona getPersona() {
        return persona;
    }

    public Parada getParadaSubida() {
        return paradaSubida;
    }

    public Parada getParadaBajada() {
        return paradaBajada;
    }

    public List<Checkin> getCheckins() {
        return checkins;
    }

    public Pago getPago() {
        return pago;
    }

    public Viaje getViaje() {
        return viaje;
    }

    public String getSlug() {
        return slug;
    }

    public Integer getCantidadPlazas() {
        return cantidadPlazas;
    }

    // Setters
    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }

    public void setFechaHoraReserva(LocalDateTime fechaHoraReserva) {
        this.fechaHoraReserva = fechaHoraReserva;
    }

    public void setPersona(Persona persona) {
        this.persona = persona;
    }

    public void setParadaSubida(Parada paradaSubida) {
        this.paradaSubida = paradaSubida;
    }

    public void setParadaBajada(Parada paradaBajada) {
        this.paradaBajada = paradaBajada;
    }

    public void setCheckins(List<Checkin> checkins) {
        this.checkins = checkins;
    }

    public void setPago(Pago pago) {
        this.pago = pago;
    }

    public void setViaje(Viaje viaje) {
        this.viaje = viaje;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setCantidadPlazas(Integer cantidadPlazas) {
        this.cantidadPlazas = cantidadPlazas;
    }

    @Override
    public String toString() {
        return "Reserva{id=" + id + ", estado=" + estado + ", fechaHoraReserva=" + fechaHoraReserva
                + ", persona=" + persona.getId() + ", paradaSubida=" + paradaSubida.getId() + ", paradaBajada="
                + paradaBajada.getId() + ", viaje=" + viaje.getId() + ", slug=" + slug + ", cantidadPlazas=" + cantidadPlazas + "}";
    }

}