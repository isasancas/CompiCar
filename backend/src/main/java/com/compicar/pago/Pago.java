package com.compicar.pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.compicar.reserva.Reserva;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importeTotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importeConductor;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal comision;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime fechaPago;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoPago estado;

    @OneToOne
    @JoinColumn(name = "reserva_id", nullable = false, unique = true)
    private Reserva reserva;

    // Constructores
    public Pago() {
    }

    public Pago(BigDecimal importeTotal, BigDecimal importeConductor, BigDecimal comision, LocalDateTime fechaCreacion,
            LocalDateTime fechaPago, EstadoPago estado, Reserva reserva) {
        this.importeTotal = importeTotal;
        this.importeConductor = importeConductor;
        this.comision = comision;
        this.fechaCreacion = fechaCreacion;
        this.fechaPago = fechaPago;
        this.estado = estado;
        this.reserva = reserva;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public BigDecimal getImporteTotal() {
        return importeTotal;
    }

    public BigDecimal getImporteConductor() {
        return importeConductor;
    }

    public BigDecimal getComision() {
        return comision;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public Reserva getReserva() {
        return reserva;
    }

    // Setters
    public void setImporteTotal(BigDecimal importeTotal) {
        this.importeTotal = importeTotal;
    }

    public void setImporteConductor(BigDecimal importeConductor) {
        this.importeConductor = importeConductor;
    }

    public void setComision(BigDecimal comision) {
        this.comision = comision;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public void setEstado(EstadoPago estado) {
        this.estado = estado;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    @Override
    public String toString() {
        return "Pago{id=" + id + ", importeTotal=" + importeTotal + ", importeConductor=" + importeConductor
                + ", comision=" + comision + ", fechaCreacion=" + fechaCreacion + ", fechaPago=" + fechaPago
                + ", estado=" + estado + ", reservaId=" + (reserva != null ? reserva.getId() : null) + "}";
    }
    
}
