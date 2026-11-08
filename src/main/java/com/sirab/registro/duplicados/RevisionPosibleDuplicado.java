package com.sirab.registro.duplicados;

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
@Table(name = "historial_posible_duplicado")
public class RevisionPosibleDuplicado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posible_duplicado_id", nullable = false)
    private PosibleDuplicado posibleDuplicado;

    @Enumerated(EnumType.STRING)
    @Column(name = "evento", nullable = false, length = 40)
    private TipoEventoDuplicado evento;

    @Column(name = "actor_correo", length = 254)
    private String actorCorreo;

    @Column(name = "fecha_hora", nullable = false)
    private Instant fechaHora;

    protected RevisionPosibleDuplicado() {
    }

    public RevisionPosibleDuplicado(
            PosibleDuplicado posibleDuplicado,
            TipoEventoDuplicado evento,
            String actorCorreo,
            Instant fechaHora) {
        this.posibleDuplicado = posibleDuplicado;
        this.evento = evento;
        this.actorCorreo = actorCorreo;
        this.fechaHora = fechaHora;
    }

    public TipoEventoDuplicado getEvento() {
        return evento;
    }

    public PosibleDuplicado getPosibleDuplicado() {
        return posibleDuplicado;
    }

    public String getActorCorreo() {
        return actorCorreo;
    }

    public Instant getFechaHora() {
        return fechaHora;
    }
}
