package com.sirab.registro;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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
        name = "regla_asignacion_ayuda",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_regla_asignacion_version_criterio",
                    columnNames = {"articulo_ayuda_id", "version", "criterio_codigo"}),
            @UniqueConstraint(
                    name = "uk_regla_id_articulo",
                    columnNames = {"id", "articulo_ayuda_id"})
        })
public class ReglaAsignacionAyuda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "articulo_ayuda_id", nullable = false)
    private ArticuloAyuda articulo;

    @Column(nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_criterio", nullable = false, length = 30)
    private TipoCriterioAsignacion tipoCriterio;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private SituacionFamiliar situacion;

    @Column(name = "criterio_codigo", nullable = false, length = 50)
    private String criterioCodigo;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal cantidad;

    @Column(nullable = false, length = 40)
    private String unidad;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @Column(name = "creado_at", nullable = false)
    private Instant creadoAt;

    @Column(name = "creado_por", nullable = false, length = 254)
    private String creadoPor;

    protected ReglaAsignacionAyuda() {
    }

    public ReglaAsignacionAyuda(
            ArticuloAyuda articulo,
            int version,
            TipoCriterioAsignacion tipoCriterio,
            SituacionFamiliar situacion,
            BigDecimal cantidad,
            String unidad,
            LocalDate vigenteDesde,
            LocalDate vigenteHasta,
            Instant creadoAt,
            String creadoPor) {
        this.articulo = articulo;
        this.version = version;
        this.tipoCriterio = tipoCriterio;
        this.situacion = situacion;
        this.criterioCodigo = tipoCriterio == TipoCriterioAsignacion.BASE
                ? "BASE"
                : situacion == null ? null : situacion.name();
        this.cantidad = cantidad;
        this.unidad = unidad;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
        this.creadoAt = creadoAt;
        this.creadoPor = creadoPor;
    }

    public Long getId() {
        return id;
    }

    public ArticuloAyuda getArticulo() {
        return articulo;
    }

    public int getVersion() {
        return version;
    }

    public TipoCriterioAsignacion getTipoCriterio() {
        return tipoCriterio;
    }

    public SituacionFamiliar getSituacion() {
        return situacion;
    }

    public String getCriterioCodigo() {
        return criterioCodigo;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public String getUnidad() {
        return unidad;
    }

    public LocalDate getVigenteDesde() {
        return vigenteDesde;
    }

    public LocalDate getVigenteHasta() {
        return vigenteHasta;
    }

    public Instant getCreadoAt() {
        return creadoAt;
    }

    public String getCreadoPor() {
        return creadoPor;
    }
}
