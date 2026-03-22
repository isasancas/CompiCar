package com.compicar.autenticacion.inicioSesion;

import jakarta.validation.constraints.NotBlank;

public class InicioSesion {
    
    @NotBlank
    String email;

    @NotBlank
    String contrasena;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }
}
