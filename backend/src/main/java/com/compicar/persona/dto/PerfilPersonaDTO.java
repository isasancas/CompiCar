package com.compicar.persona.dto;

import java.util.List;

import com.compicar.persona.Persona;

public class PerfilPersonaDTO {

    private Long id;
    private String nombre;
    private String primerApellido;
    private String segundoApellido;
    private String email;
    private String telefono;
    private Double reputacion;
    private String slug;
    private List<String> preferenciasViaje;

    public PerfilPersonaDTO() {
    }

    public PerfilPersonaDTO(Persona persona) {
        this.id = persona.getId();
        this.nombre = persona.getNombre();
        this.primerApellido = persona.getPrimerApellido();
        this.segundoApellido = persona.getSegundoApellido();
        this.email = persona.getEmail();
        this.telefono = persona.getTelefono();
        this.reputacion = persona.getReputacion();
        this.slug = persona.getSlug();
        this.preferenciasViaje = persona.getPreferenciasViaje();
    }

    public PerfilPersonaDTO(Long id, String nombre, String primerApellido, String segundoApellido, String email,
            String telefono, Double reputacion, String slug, List<String> preferenciasViaje) {
        this.id = id;
        this.nombre = nombre;
        this.primerApellido = primerApellido;
        this.segundoApellido = segundoApellido;
        this.email = email;
        this.telefono = telefono;
        this.reputacion = reputacion;
        this.slug = slug;
        this.preferenciasViaje = preferenciasViaje;
    }

    public Long getId() {
        return id;
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

    public Double getReputacion() {
        return reputacion;
    }

    public String getSlug() {
        return slug;
    }

    public List<String> getPreferenciasViaje() {
        return preferenciasViaje;
    }

    public void setId(Long id) {
        this.id = id;
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

    public void setReputacion(Double reputacion) {
        this.reputacion = reputacion;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setPreferenciasViaje(List<String> preferenciasViaje) {
        this.preferenciasViaje = preferenciasViaje;
    }

    @Override
    public String toString() {
        return "PerfilPersonaDTO{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", primerApellido='" + primerApellido + '\'' +
                ", segundoApellido='" + segundoApellido + '\'' +
                ", email='" + email + '\'' +
                ", telefono='" + telefono + '\'' +
                ", reputacion=" + reputacion +
                ", slug='" + slug + '\'' +
                ", preferenciasViaje=" + preferenciasViaje +
                '}';
    }

}