package com.compicar.persona.dto;

import com.compicar.persona.Persona;

public class ActualizarPerfilDTO {
    
    private String nombre;
    private String primerApellido;
    private String segundoApellido;
    private String email;
    private String telefono;

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
            String telefono) {
        this.nombre = nombre;
        this.primerApellido = primerApellido;
        this.segundoApellido = segundoApellido;
        this.email = email;
        this.telefono = telefono;
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

    @Override
    public String toString() {
        return "ActualizarPerfilDTO{nombre='" + nombre + "', primerApellido='" + primerApellido
                + "', segundoApellido='" + segundoApellido + "', email='" + email + "', telefono='" + telefono + "'}";
    }
}
