package com.compicar.autenticacion.registro;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class Registro {
    
    @NotBlank(message = "El username no puede estar vacío")
    String username;
    
    @NotBlank(message = "La contraseña no puede estar vacía")
    String contrasena;
    
    @NotBlank(message = "El nombre no puede estar vacío")
    String nombre;
    
    @NotBlank(message = "El primer apellido no puede estar vacío")
    String primerApellido;

    String segundoApellido;
    
    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "El email no es válido")
    String email;
    
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "El teléfono no es válido")
    String numTelefono;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public void setPrimerApellido(String primerApellido) {
        this.primerApellido = primerApellido;
    }

    public String getSegundoApellido() {
        return segundoApellido;
    }

    public void setSegundoApellido(String segundoApellido) {
        this.segundoApellido = segundoApellido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNumTelefono() {
        return numTelefono;
    }

    public void setNumTelefono(String numTelefono) {
        this.numTelefono = numTelefono;
    }

}
