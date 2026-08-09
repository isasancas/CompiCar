package com.compicar.pago;

public class PagoRequest {
    private Long viajeId;
    private Integer cantidadPlazas;

    // Getters y Setters
    public Long getViajeId() { return viajeId; }
    public void setViajeId(Long viajeId) { this.viajeId = viajeId; }
    public Integer getCantidadPlazas() { return cantidadPlazas; }
    public void setCantidadPlazas(Integer cantidadPlazas) { this.cantidadPlazas = cantidadPlazas; }
}
