package com.compicar.pago;

import java.util.List;

import com.compicar.persona.Persona;

public interface PagoService {
    
    Pago crearPago(String usuarioEmail, Long reservaId);
    List<Pago> obtenerPagosPorPersona(Persona persona);
    Pago obtenerPagoPorId(Long pagoId);
    Pago pagoCompletado(Long pagoId);
    Pago pagoFallido(Long pagoId);
    Pago pagoReembolsado(Long pagoId);
    Pago actualizarPago(String usuarioEmail, Long reservaId, Pago pagoActualizado);
}
