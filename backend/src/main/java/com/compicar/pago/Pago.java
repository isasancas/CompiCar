package com.compicar.pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.compicar.reserva.Reserva;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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

    @Column(nullable = true)
    private LocalDateTime fechaPago;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoPago estado;

    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Reserva> reservas = new ArrayList<>();

    // Identificador de la transacción en Stripe para poder "congelar/capturar"
    @Column(name = "stripe_payment_intent_id", unique = true)
    private String stripePaymentIntentId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importeLiberadoConductor = BigDecimal.ZERO;

    // Constructores
    public Pago() {
    }

    public Pago(BigDecimal importeTotal, BigDecimal importeConductor, BigDecimal comision, LocalDateTime fechaCreacion,
            LocalDateTime fechaPago, EstadoPago estado, List<Reserva> reservas, String stripePaymentIntentId) {
        this.importeTotal = importeTotal;
        this.importeConductor = importeConductor;
        this.comision = comision;
        this.fechaCreacion = fechaCreacion;
        this.fechaPago = fechaPago;
        this.estado = estado;
        this.reservas = reservas;
        this.stripePaymentIntentId = stripePaymentIntentId;
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

    public List<Reserva> getReservas() {
        return reservas;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public BigDecimal getImporteLiberadoConductor() {
        return importeLiberadoConductor;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

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

    public void setReservas(List<Reserva> reservas) {
        this.reservas = reservas;
    }

    public void setStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public void setImporteLiberadoConductor(BigDecimal importeLiberadoConductor) {
        this.importeLiberadoConductor = importeLiberadoConductor;
    }

    // 1. Método helper para mantener compatibilidad con pago.getReserva(...)
    public Reserva getReserva() {
        return (reservas != null && !reservas.isEmpty()) ? reservas.get(0) : null;
    }

    // 2. Método helper para mantener compatibilidad con pago.setReserva(...)
    public void setReserva(Reserva reserva) {
        if (this.reservas == null) {
            this.reservas = new ArrayList<>();
        }
        if (reserva != null) {
            if (!this.reservas.contains(reserva)) {
                this.reservas.add(reserva);
            }
            reserva.setPago(this); // Vincula ambos lados de la relación
        }
    }

    // 3. Helper para añadir varias reservas
    public void addReserva(Reserva reserva) {
        this.reservas.add(reserva);
        reserva.setPago(this);
    }

    @Override
    public String toString() {
        return "Pago{id=" + id + ", importeTotal=" + importeTotal + ", importeConductor=" + importeConductor
                + ", comision=" + comision + ", fechaCreacion=" + fechaCreacion + ", fechaPago=" + fechaPago
                + ", estado=" + estado + ", reservaIds=" + (reservas != null ? reservas.stream().map(Reserva::getId).toList() : null) + 
                ", stripePaymentIntentId=" + stripePaymentIntentId + ", importeLiberadoConductor=" + importeLiberadoConductor + "}";
    }
    
}
