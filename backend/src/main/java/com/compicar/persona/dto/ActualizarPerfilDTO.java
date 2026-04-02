package com.compicar.persona.dto;

import com.compicar.persona.Persona;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ActualizarPerfilDTO {
    
    @NotBlank(message = "El nombre no puede estar vacío")
    private String nombre;

    @NotBlank(message = "El primer apellido no puede estar vacío")
    private String primerApellido;
    private String segundoApellido;

    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "El email debe ser válido")
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "El teléfono no es válido")
    private String telefono;

    private String contrasenaActual;

    public ActualizarPerfilDTO() {
    }

    public ActualizarPerfilDTO(Persona persona) {
        this.nombre = persona.getNombre();
        this.primerApellido = persona.getPrimerApellido();
        this.segundoApellido = persona.getSegundoApellido();
        this.email = persona.getEmail();
        this.telefono = persona.getTelefono();
    }

    public ActualizarPerfilDTO(String nombre, String primerApellido, String segundoApellido, String email,
            String telefono, String contrasenaActual) {
        this.nombre = nombre;
        this.primerApellido = primerApellido;
        this.segundoApellido = segundoApellido;
        this.email = email;
        this.telefono = telefono;
        this.contrasenaActual = contrasenaActual;
    }

    public String getNombre() {
        return nombre;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public String getSegundoApellido() {
        return segundoApellido;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getContrasenaActual() {
        return contrasenaActual;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setPrimerApellido(String primerApellido) {
        this.primerApellido = primerApellido;
    }

    public void setSegundoApellido(String segundoApellido) {
        this.segundoApellido = segundoApellido;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public void setContrasenaActual(String contrasenaActual) {
        this.contrasenaActual = contrasenaActual;
    }

    @Override
    public String toString() {
        return "ActualizarPerfilDTO{nombre='" + nombre + "', primerApellido='" + primerApellido
                + "', segundoApellido='" + segundoApellido + "', email='" + email + "', telefono='" + telefono + "', contrasenaActual='" + contrasenaActual + "'}";
    }
}
