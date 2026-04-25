package com.compicar.notificacion;

import java.time.LocalDateTime;
import com.compicar.persona.Persona;
import jakarta.persistence.*;

@Entity
@Table(name = "notificacion")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private boolean leida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receptor_id", nullable = false)
    private Persona receptor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacion tipo;

    public Notificacion() {
        this.fechaCreacion = LocalDateTime.now();
        this.leida = false;
    }

    public Notificacion(String mensaje, Persona receptor, TipoNotificacion tipo) {
        this();
        this.mensaje = mensaje;
        this.receptor = receptor;
        this.tipo = tipo;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
    public Persona getReceptor() { return receptor; }
    public void setReceptor(Persona receptor) { this.receptor = receptor; }
    public TipoNotificacion getTipo() { return tipo; }
    public void setTipo(TipoNotificacion tipo) { this.tipo = tipo; }
}