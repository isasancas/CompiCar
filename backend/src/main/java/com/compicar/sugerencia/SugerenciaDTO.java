package com.compicar.sugerencia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SugerenciaDTO {

    @NotBlank(message = "El mensaje no puede estar vacío")
    @Size(max = 2000, message = "El mensaje no puede superar los 2000 caracteres")
    private String mensaje;

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}