package com.compicar.reserva.dto;

import java.math.BigDecimal;
import java.util.List;

public class ReservaGrupoDTO {

    private int totalViajesReservados;
    private BigDecimal precioTotal;
    private List<ReservaDTO> reservas;

    public ReservaGrupoDTO(int totalViajesReservados, BigDecimal precioTotal, List<ReservaDTO> reservas) {
        this.totalViajesReservados = totalViajesReservados;
        this.precioTotal = precioTotal;
        this.reservas = reservas;
    }

    // Getters y Setters
    public int getTotalViajesReservados() {
        return totalViajesReservados;
    }

    public BigDecimal getPrecioTotal() {
        return precioTotal;
    }

    public List<ReservaDTO> getReservas() {
        return reservas;
    }
}