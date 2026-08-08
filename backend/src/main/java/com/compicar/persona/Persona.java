package com.compicar.persona;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.compicar.reserva.Reserva;
import com.compicar.valoracion.Valoracion;
import com.compicar.vehiculo.Vehiculo;
import com.compicar.viaje.Viaje;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "persona")
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String primerApellido;

    private String segundoApellido;

    @Column(nullable = false)
    private String contrasena;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true)
    private String telefono;

    @OneToMany(mappedBy = "persona")
    @JsonIgnore
    private List<Vehiculo> vehiculos;

    @OneToMany(mappedBy = "persona")
    @JsonIgnore
    private List<Reserva> reservas;

    @OneToMany(mappedBy = "persona")
    @JsonIgnore
    private List<Viaje> viajes;

    @OneToMany(mappedBy = "autor")
    @JsonIgnore
    private List<Valoracion> valoracionesEmitidas;

    @OneToMany(mappedBy = "valorado")
    @JsonIgnore
    private List<Valoracion> valoracionesRecibidas;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String foto;

    @Column(nullable = false)
    private Integer numeroCancelaciones = 0;

    @ElementCollection
    @CollectionTable(
        name = "persona_preferencia_viaje",
        joinColumns = @JoinColumn(name = "persona_id")
    )
    @Column(name = "preferencia")
    private List<String> preferenciasViaje = new ArrayList<>();

    @Column(name = "stripe_pasajero_id", unique = true)
    private String stripePasajeroId;

    @Column(name = "stripe_conductor_id", unique = true)
    private String stripeConductorId;

    @Column(name = "fondos_totales", nullable = false)
    private BigDecimal fondosTotales = BigDecimal.ZERO;

    @Column(name = "fondos_actuales", nullable = false)
    private BigDecimal fondosActuales = BigDecimal.ZERO;

    public Double getReputacion() {
        if (valoracionesRecibidas == null || valoracionesRecibidas.isEmpty()) {
            return 0.0;
        }

        return valoracionesRecibidas.stream().mapToDouble(Valoracion::getPuntuacion).average().orElse(0.0);
    }

    public Persona() {
    }

    public Persona(String nombre, String primerApellido, String segundoApellido, String contrasena, String email,
            String telefono, String stripePasajeroId, String stripeConductorId) {
        this.nombre = nombre;
        this.primerApellido = primerApellido;
        this.segundoApellido = segundoApellido;
        this.contrasena = contrasena;
        this.email = email;
        this.telefono = telefono;
        this.slug = "persona-" + id;
        this.numeroCancelaciones = 0;
        this.stripePasajeroId = stripePasajeroId;
        this.stripeConductorId = stripeConductorId;
    }

    public Persona(String nombre, String primerApellido, String segundoApellido, String contrasena, String email,
            String telefono, List<Vehiculo> vehiculos, List<Reserva> reservas, List<Viaje> viajes,
            List<Valoracion> valoracionesEmitidas, List<Valoracion> valoracionesRecibidas, String stripePasajeroId, String stripeConductorId) {
        this.nombre = nombre;
        this.primerApellido = primerApellido;
        this.segundoApellido = segundoApellido;
        this.contrasena = contrasena;
        this.email = email;
        this.telefono = telefono;
        this.slug = "persona-" + id;
        this.vehiculos = vehiculos;
        this.reservas = reservas;
        this.viajes = viajes;
        this.valoracionesEmitidas = valoracionesEmitidas;
        this.valoracionesRecibidas = valoracionesRecibidas;
        this.numeroCancelaciones = 0;
        this.stripePasajeroId = stripePasajeroId;
        this.stripeConductorId = stripeConductorId;
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

    public String getContrasena() {
        return contrasena;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public List<Vehiculo> getVehiculos() {
        return vehiculos;
    }

    public List<Reserva> getReservas() {
        return reservas;
    }

    public List<Viaje> getViajes() {
        return viajes;
    }

    public List<Valoracion> getValoracionesEmitidas() {
        return valoracionesEmitidas;
    }

    public List<Valoracion> getValoracionesRecibidas() {
        return valoracionesRecibidas;
    }

    public String getSlug() {
        return slug;
    }

    public String getFoto() {
        return foto;
    }

    public List<String> getPreferenciasViaje() {
        return preferenciasViaje;
    }

    public Integer getNumeroCancelaciones() {
        return numeroCancelaciones;
    }

    public String getStripePasajeroId() {
        return stripePasajeroId;
    }

    public String getStripeConductorId() {
        return stripeConductorId;
    }

    public BigDecimal getFondosTotales() {
        return fondosTotales;
    }

    public BigDecimal getFondosActuales() {
        return fondosActuales;
    }

    public void incrementarCancelaciones() {
        if (this.numeroCancelaciones == null) {
            this.numeroCancelaciones = 0;
        }
        this.numeroCancelaciones++;
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

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public void setVehiculos(List<Vehiculo> vehiculos) {
        this.vehiculos = vehiculos;
    }

    public void setReservas(List<Reserva> reservas) {
        this.reservas = reservas;
    }

    public void setViajes(List<Viaje> viajes) {
        this.viajes = viajes;
    }

    public void setValoracionesEmitidas(List<Valoracion> valoracionesEmitidas) {
        this.valoracionesEmitidas = valoracionesEmitidas;
    }

    public void setValoracionesRecibidas(List<Valoracion> valoracionesRecibidas) {
        this.valoracionesRecibidas = valoracionesRecibidas;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public void setPreferenciasViaje(List<String> nuevasPreferencias) {
        if (this.preferenciasViaje == null) {
            this.preferenciasViaje = new ArrayList<>();
        }

        this.preferenciasViaje.clear();
        if (nuevasPreferencias != null) {
            this.preferenciasViaje.addAll(nuevasPreferencias);
        }
    }

    public void setNumeroCancelaciones(Integer numeroCancelaciones) {
        this.numeroCancelaciones = numeroCancelaciones;
    }

    public void setStripePasajeroId(String stripePasajeroId) {
        this.stripePasajeroId = stripePasajeroId;
    }

    public void setStripeConductorId(String stripeConductorId) {
        this.stripeConductorId = stripeConductorId;
    }

    public void setFondosTotales(BigDecimal fondosTotales) {
        this.fondosTotales = fondosTotales;
    }

    public void setFondosActuales(BigDecimal fondosActuales) {
        this.fondosActuales = fondosActuales;
    }

    @Override
    public String toString() {
        return "Persona{id=" + id + ", nombre='" + nombre + "', primerApellido='" + primerApellido
                + "', segundoApellido='" + segundoApellido + "', email='" + email + "', telefono='" + telefono
                + "', reputacion=" + getReputacion() + ", numeroCancelaciones=" + numeroCancelaciones + ", slug=" + 
                slug +  ", stripePasajeroId=" + stripePasajeroId + ", stripeConductorId=" + stripeConductorId + 
                ", fondosTotales=" + fondosTotales + ", fondosActuales=" + fondosActuales + "}";
    }
}