package com.compicar.viaje.dto;

import java.math.BigDecimal;

public class PrecioTrayectoResponseDTO {

    private BigDecimal precioMinimoPasajero;
    private BigDecimal precioMaximoPasajero;
    private BigDecimal costeTotalCombustible;
    private BigDecimal litrosEstimados;
    private BigDecimal precioCombustibleLitro;
    private String fuente;
    private String detalle;

    public BigDecimal getPrecioMinimoPasajero() {
        return precioMinimoPasajero;
    }

    public void setPrecioMinimoPasajero(BigDecimal precioMinimoPasajero) {
        this.precioMinimoPasajero = precioMinimoPasajero;
    }

    public BigDecimal getPrecioMaximoPasajero() {
        return precioMaximoPasajero;
    }

    public void setPrecioMaximoPasajero(BigDecimal precioMaximoPasajero) {
        this.precioMaximoPasajero = precioMaximoPasajero;
    }

    public BigDecimal getCosteTotalCombustible() {
        return costeTotalCombustible;
    }

    public void setCosteTotalCombustible(BigDecimal costeTotalCombustible) {
        this.costeTotalCombustible = costeTotalCombustible;
    }

    public BigDecimal getLitrosEstimados() {
        return litrosEstimados;
    }

    public void setLitrosEstimados(BigDecimal litrosEstimados) {
        this.litrosEstimados = litrosEstimados;
    }

    public BigDecimal getPrecioCombustibleLitro() {
        return precioCombustibleLitro;
    }

    public void setPrecioCombustibleLitro(BigDecimal precioCombustibleLitro) {
        this.precioCombustibleLitro = precioCombustibleLitro;
    }

    public String getFuente() {
        return fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = fuente;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
}
