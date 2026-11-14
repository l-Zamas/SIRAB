package com.sirab.registro;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.Immutable;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Immutable
@Table(
        name = "asignacion_ayuda",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_asignacion_ayuda_idempotencia",
                    columnNames = "clave_idempotencia"),
            @UniqueConstraint(
                    name = "uk_asignacion_evaluacion_regla",
                    columnNames = {"evaluacion_id", "regla_asignacion_id"}),
            @UniqueConstraint(
                    name = "uk_asignacion_id_familia_articulo_tipo",
                    columnNames = {"id", "familia_id", "articulo_ayuda_id", "tipo_ayuda"})
        })
public class AsignacionAyuda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "familia_id", nullable = false)
    private Familia familia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "articulo_ayuda_id", nullable = false)
    private ArticuloAyuda articulo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ayuda", nullable = false, length = 30)
    private Necesidad tipoAyuda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluacion_id")
    private EvaluacionAsignacionAyuda evaluacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regla_asignacion_id")
    private ReglaAsignacionAyuda regla;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revision_sanitaria_id")
    private RevisionSanitariaExterna revisionSanitaria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrigenAsignacionAyuda origen;

    @Column(name = "clave_idempotencia", nullable = false, length = 120)
    private String claveIdempotencia;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal cantidad;

    @Column(nullable = false, length = 40)
    private String unidad;

    @Column(name = "asignada_at", nullable = false)
    private Instant asignadaAt;

    @Column(name = "usuario_correo", nullable = false, length = 254)
    private String usuarioCorreo;

    protected AsignacionAyuda() {
    }

    public AsignacionAyuda(
            Familia familia,
            ArticuloAyuda articulo,
            EvaluacionAsignacionAyuda evaluacion,
            ReglaAsignacionAyuda regla,
            RevisionSanitariaExterna revisionSanitaria,
            OrigenAsignacionAyuda origen,
            String claveIdempotencia,
            BigDecimal cantidad,
            String unidad,
            Instant asignadaAt,
            String usuarioCorreo) {
        this.familia = familia;
        this.articulo = articulo;
        this.tipoAyuda = articulo.getTipoAyuda();
        this.evaluacion = evaluacion;
        this.regla = regla;
        this.revisionSanitaria = revisionSanitaria;
        this.origen = origen;
        this.claveIdempotencia = claveIdempotencia;
        this.cantidad = cantidad;
        this.unidad = unidad;
        this.asignadaAt = asignadaAt;
        this.usuarioCorreo = usuarioCorreo;
    }

    public Long getId() {
        return id;
    }

    public Familia getFamilia() {
        return familia;
    }

    public ArticuloAyuda getArticulo() {
        return articulo;
    }

    public Necesidad getTipoAyuda() {
        return tipoAyuda;
    }

    public EvaluacionAsignacionAyuda getEvaluacion() {
        return evaluacion;
    }

    public ReglaAsignacionAyuda getRegla() {
        return regla;
    }

    public RevisionSanitariaExterna getRevisionSanitaria() {
        return revisionSanitaria;
    }

    public OrigenAsignacionAyuda getOrigen() {
        return origen;
    }

    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public String getUnidad() {
        return unidad;
    }

    public Instant getAsignadaAt() {
        return asignadaAt;
    }

    public String getUsuarioCorreo() {
        return usuarioCorreo;
    }
}
