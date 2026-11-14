package com.sirab.registro;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Immutable
@Table(
        name = "evaluacion_asignacion_ayuda",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_evaluacion_asignacion_idempotencia",
                    columnNames = "clave_idempotencia"),
            @UniqueConstraint(
                    name = "uk_evaluacion_asignacion_revision",
                    columnNames = {"familia_id", "revision"}),
            @UniqueConstraint(
                    name = "uk_evaluacion_id_familia",
                    columnNames = {"id", "familia_id"})
        })
public class EvaluacionAsignacionAyuda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "familia_id", nullable = false)
    private Familia familia;

    @Column(nullable = false)
    private int revision;

    @Column(name = "clave_idempotencia", nullable = false, length = 100)
    private String claveIdempotencia;

    @Column(name = "evaluada_at", nullable = false)
    private Instant evaluadaAt;

    @Column(name = "usuario_correo", nullable = false, length = 254)
    private String usuarioCorreo;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "evaluacion_asignacion_necesidad",
            joinColumns = @JoinColumn(name = "evaluacion_id"))
    @Column(name = "necesidad", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Set<Necesidad> necesidades = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "evaluacion_asignacion_situacion",
            joinColumns = @JoinColumn(name = "evaluacion_id"))
    @Column(name = "situacion", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private Set<SituacionFamiliar> situaciones = new HashSet<>();

    protected EvaluacionAsignacionAyuda() {
    }

    public EvaluacionAsignacionAyuda(
            Familia familia,
            int revision,
            String claveIdempotencia,
            Instant evaluadaAt,
            String usuarioCorreo,
            Set<Necesidad> necesidades,
            Set<SituacionFamiliar> situaciones) {
        this.familia = familia;
        this.revision = revision;
        this.claveIdempotencia = claveIdempotencia;
        this.evaluadaAt = evaluadaAt;
        this.usuarioCorreo = usuarioCorreo;
        this.necesidades = new HashSet<>(necesidades);
        this.situaciones = new HashSet<>(situaciones);
    }

    public Long getId() {
        return id;
    }

    public Familia getFamilia() {
        return familia;
    }

    public int getRevision() {
        return revision;
    }

    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public Instant getEvaluadaAt() {
        return evaluadaAt;
    }

    public String getUsuarioCorreo() {
        return usuarioCorreo;
    }

    public Set<Necesidad> getNecesidades() {
        return Set.copyOf(necesidades);
    }

    public Set<SituacionFamiliar> getSituaciones() {
        return Set.copyOf(situaciones);
    }
}
