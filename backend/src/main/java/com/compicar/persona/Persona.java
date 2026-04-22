package com.compicar.persona;

import java.util.List;

import com.compicar.reserva.Reserva;
import com.compicar.valoracion.Valoracion;
import com.compicar.vehiculo.Vehiculo;
import com.compicar.viaje.Viaje;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    private List<Vehiculo> vehiculos;

    @OneToMany(mappedBy = "persona")
    private List<Reserva> reservas;

    @OneToMany(mappedBy = "persona")
    private List<Viaje> viajes;

    @OneToMany(mappedBy = "autor")
    private List<Valoracion> valoracionesEmitidas;

    @OneToMany(mappedBy = "valorado")
    private List<Valoracion> valoracionesRecibidas;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;


    // Atributo derivado
    public Double getReputacion() {
        if (valoracionesRecibidas == null || valoracionesRecibidas.isEmpty()) {
            return 0.0;
        }

        return valoracionesRecibidas.stream().mapToDouble(Valoracion::getPuntuacion).average().orElse(0.0);
    }

    // Constructores
    public Persona() {
    }

    public Persona(String nombre, String primerApellido, String segundoApellido, String contrasena, String email,
            String telefono) {
        this.nombre = nombre;
        this.primerApellido = primerApellido;
        this.segundoApellido = segundoApellido;
        this.contrasena = contrasena;
        this.email = email;
        this.telefono = telefono;
        this.slug = "persona-" + id;
    }

    public Persona(String nombre, String primerApellido, String segundoApellido, String contrasena, String email,
            String telefono, List<Vehiculo> vehiculos, List<Reserva> reservas, List<Viaje> viajes,
            List<Valoracion> valoracionesEmitidas, List<Valoracion> valoracionesRecibidas) {
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
    }

    //Getters
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

    //Setters
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

    @Override
    public String toString() {
        return "Persona{id=" + id + ", nombre='" + nombre + "', primerApellido='" + primerApellido
                + "', segundoApellido='" + segundoApellido + "', email='" + email + "', telefono='" + telefono
                + "', reputacion=" + getReputacion() + ", slug=" + slug + "}";
    }
}
