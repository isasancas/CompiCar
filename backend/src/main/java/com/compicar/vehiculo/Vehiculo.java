package com.compicar.vehiculo;

import java.util.List;

import com.compicar.persona.Persona;
import com.compicar.viaje.Viaje;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehiculo")
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String matricula;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String modelo;

    @Column(nullable = false)
    private Integer plazas;

    @Column(nullable = false)
    private Double consumo;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoVehiculo tipo;

    @ManyToOne
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @OneToMany(mappedBy = "vehiculo")
    private List<Viaje> viajes;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    // Constructores
    public Vehiculo() {
    }

    public Vehiculo(String matricula, String marca, String modelo, Integer plazas, Double consumo, Integer anio,
            TipoVehiculo tipo, Persona persona) {
        this.matricula = matricula;
        this.marca = marca;
        this.modelo = modelo;
        this.plazas = plazas;
        this.consumo = consumo;
        this.anio = anio;
        this.tipo = tipo;
        this.persona = persona;
        this.slug = "vehiculo-" + id;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getMatricula() {
        return matricula;
    }

    public String getMarca() {
        return marca;
    }

    public String getModelo() {
        return modelo;
    }

    public Integer getPlazas() {
        return plazas;
    }

    public Double getConsumo() {
        return consumo;
    }

    public Integer getAnio() {
        return anio;
    }

    public TipoVehiculo getTipo() {
        return tipo;
    }

    public Persona getPersona() {
        return persona;
    }

    public List<Viaje> getViajes() {
        return viajes;
    }

    public String getSlug() {
        return slug;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public void setPlazas(Integer plazas) {
        this.plazas = plazas;
    }

    public void setConsumo(Double consumo) {
        this.consumo = consumo;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public void setTipo(TipoVehiculo tipo) {
        this.tipo = tipo;
    }

    public void setPersona(Persona persona) {
        this.persona = persona;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    @Override
    public String toString() {
        return "Vehiculo{id=" + id + ", matricula='" + matricula + "', marca='" + marca + "', modelo='" + modelo
                + "', plazas=" + plazas + ", consumo=" + consumo + ", anio=" + anio + ", tipo=" + tipo
                + ", personaId=" + (persona != null ? persona.getId() : null) + ", slug=" + slug + "}";
    }

}
