package com.compicar.persona.dto;

public class SubirFotoRequestDTO {
    private String foto; // Base64 encoded

    public SubirFotoRequestDTO() {
    }

    public SubirFotoRequestDTO(String foto) {
        this.foto = foto;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }
}
