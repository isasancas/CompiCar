package com.compicar.valoracion;

import java.time.LocalDateTime;

import com.compicar.persona.Persona;
import com.compicar.viaje.Viaje;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "valoracion")
public class Valoracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer puntuacion;

    private String comentario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @ManyToOne
    @JoinColumn(name = "autor_id", nullable = false)
    @JsonIgnore
    private Persona autor;

    @ManyToOne
    @JoinColumn(name = "valorado_id", nullable = false)
    @JsonIgnore
    private Persona valorado;

    @ManyToOne
    @JoinColumn(name = "viaje_id", nullable = false)
    @JsonIgnore
    private Viaje viaje;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    // Constructores
    public Valoracion() {
        this.fecha = LocalDateTime.now();
    }

    public Valoracion(Integer puntuacion, String comentario, Persona autor, Persona valorado, Viaje viaje) {
        this.puntuacion = puntuacion;
        this.comentario = comentario;
        this.autor = autor;
        this.valorado = valorado;
        this.viaje = viaje; 
        this.fecha = LocalDateTime.now();
        this.slug = "valoracion-" + id;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Integer getPuntuacion() {
        return puntuacion;
    }

    public String getComentario() {
        return comentario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public Persona getAutor() {
        return autor;
    }

    public Persona getValorado() {
        return valorado;
    }

    public Viaje getViaje() {
        return viaje;
    }

    public String getSlug() {
        return slug;
    }

    // Setters
    public void setPuntuacion(Integer puntuacion) {
        this.puntuacion = puntuacion;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public void setAutor(Persona autor) {
        this.autor = autor;
    }

    public void setValorado(Persona valorado) {
        this.valorado = valorado;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public void setViaje(Viaje viaje) { 
        this.viaje = viaje;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    @Override
    public String toString() {
        return "Valoracion{id=" + id + ", puntuacion=" + puntuacion + ", comentario='" + comentario + "', fecha=" + fecha
                + ", autor=" + autor.getId() + ", valorado=" + valorado.getId() 
                + ", viaje=" + (viaje != null ? viaje.getId() : null) + ", slug=" + slug + "}";
    }
}