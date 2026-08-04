package com.compicar.valoracion.dto;

import java.time.LocalDateTime;

import com.compicar.valoracion.Valoracion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ValoracionDTO {

    private Long id;

    @NotNull(message = "La puntuación es obligatoria")
    @Min(value = 1, message = "La puntuación mínima es 1")
    @Max(value = 5, message = "La puntuación máxima es 5")
    private Integer puntuacion;

    private String comentario;

    private LocalDateTime fecha;

    @NotNull(message = "El autor es obligatorio")
    private Long autorId;

    @NotNull(message = "La persona valorada es obligatoria")
    private Long valoradoId;

    private String slug;

    public ValoracionDTO() {
    }

    public ValoracionDTO(Valoracion valoracion) {
        this.id = valoracion.getId();
        this.puntuacion = valoracion.getPuntuacion();
        this.comentario = valoracion.getComentario();
        this.fecha = valoracion.getFecha();
        this.autorId = valoracion.getAutor() != null ? valoracion.getAutor().getId() : null;
        this.valoradoId = valoracion.getValorado() != null ? valoracion.getValorado().getId() : null;
        this.slug = valoracion.getSlug();
    }

    public ValoracionDTO(Long id, Integer puntuacion, String comentario, LocalDateTime fecha, Long autorId,
            Long valoradoId, String slug) {
        this.id = id;
        this.puntuacion = puntuacion;
        this.comentario = comentario;
        this.fecha = fecha;
        this.autorId = autorId;
        this.valoradoId = valoradoId;
        this.slug = slug;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(Integer puntuacion) {
        this.puntuacion = puntuacion;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Long getAutorId() {
        return autorId;
    }

    public void setAutorId(Long autorId) {
        this.autorId = autorId;
    }

    public Long getValoradoId() {
        return valoradoId;
    }

    public void setValoradoId(Long valoradoId) {
        this.valoradoId = valoradoId;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}