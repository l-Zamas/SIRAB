package com.sirab.registro;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "historial_estado_solicitud")
public class EstadoSolicitudHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "familia_id", nullable = false)
    private Familia familia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoRegistro estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoRegistro estadoNuevo;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_tipo", nullable = false, length = 20)
    private ActorTipoSolicitud actorTipo;

    @Column(name = "usuario_correo", length = 254)
    private String usuarioCorreo;

    @Column(name = "fecha_hora", nullable = false)
    private Instant fechaHora;

    @Column(length = 1000)
    private String observacion;

    protected EstadoSolicitudHistorial() {
    }

    public EstadoSolicitudHistorial(
            Familia familia,
            EstadoRegistro estadoAnterior,
            EstadoRegistro estadoNuevo,
            ActorTipoSolicitud actorTipo,
            String usuarioCorreo,
            Instant fechaHora,
            String observacion) {
        this.familia = familia;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.actorTipo = actorTipo;
        this.usuarioCorreo = usuarioCorreo;
        this.fechaHora = fechaHora;
        this.observacion = observacion;
    }

    public Long getId() {
        return id;
    }

    public EstadoRegistro getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoRegistro getEstadoNuevo() {
        return estadoNuevo;
    }

    public ActorTipoSolicitud getActorTipo() {
        return actorTipo;
    }

    public String getUsuarioCorreo() {
        return usuarioCorreo;
    }

    public Instant getFechaHora() {
        return fechaHora;
    }

    public String getObservacion() {
        return observacion;
    }
}
