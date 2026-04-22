package com.compicar.checkin;

import java.time.LocalDateTime;

import com.compicar.parada.Parada;
import com.compicar.reserva.Reserva;

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
@Table(name = "checkin")
public class Checkin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaHoraConductor;

    @Column(nullable = false)
    private LocalDateTime fechaHoraPasajero;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoCheckin tipo;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoCheckin estado;

    @ManyToOne
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @ManyToOne
    @JoinColumn(name = "parada_id", nullable = false)
    private Parada parada;

    // Constructores
    public Checkin() {
    }

    public Checkin(LocalDateTime fechaHoraConductor, LocalDateTime fechaHoraPasajero, TipoCheckin tipo,
            EstadoCheckin estado, Reserva reserva, Parada parada) {
        this.fechaHoraConductor = fechaHoraConductor;
        this.fechaHoraPasajero = fechaHoraPasajero;
        this.tipo = tipo;
        this.estado = estado;
        this.reserva = reserva;
        this.parada = parada;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public LocalDateTime getFechaHoraConductor() {
        return fechaHoraConductor;
    }

    public LocalDateTime getFechaHoraPasajero() {
        return fechaHoraPasajero;
    }

    public TipoCheckin getTipo() {
        return tipo;
    }

    public EstadoCheckin getEstado() {
        return estado;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public Parada getParada() {
        return parada;
    }

    // Setters
    public void setFechaHoraConductor(LocalDateTime fechaHoraConductor) {
        this.fechaHoraConductor = fechaHoraConductor;
    }

    public void setFechaHoraPasajero(LocalDateTime fechaHoraPasajero) {
        this.fechaHoraPasajero = fechaHoraPasajero;
    }

    public void setTipo(TipoCheckin tipo) {
        this.tipo = tipo;
    }

    public void setEstado(EstadoCheckin estado) {
        this.estado = estado;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public void setParada(Parada parada) {
        this.parada = parada;
    }

    @Override
    public String toString() {
        return "Checkin{" +
                "id=" + id +
                ", fechaHoraConductor=" + fechaHoraConductor +
                ", fechaHoraPasajero=" + fechaHoraPasajero +
                ", tipo=" + tipo +
                ", estado=" + estado +
                ", reservaId=" + (reserva != null ? reserva.getId() : null) +
                ", paradaId=" + (parada != null ? parada.getId() : null) +
                '}';
    }
    
}
