package com.sirab.registro;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

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
@Table(
        name = "entrega_ayuda",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_entrega_id_tipo", columnNames = {"id", "tipo_ayuda"}),
            @UniqueConstraint(
                    name = "uk_entrega_id_articulo_tipo",
                    columnNames = {"id", "articulo_ayuda_id", "tipo_ayuda"})
        })
@Immutable
public class EntregaAyuda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "familia_id", nullable = false)
    private Familia familia;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ayuda", nullable = false, length = 30)
    private Necesidad tipoAyuda;

    @Column(nullable = true, length = 100)
    private String cantidad;

    @Column(name = "fecha_entrega")
    private LocalDate fechaEntrega;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignacion_ayuda_id")
    private AsignacionAyuda asignacion;

    @Column(name = "articulo_ayuda_id")
    private Long articuloAyudaId;

    @Column(name = "cantidad_valor", precision = 18, scale = 6)
    private BigDecimal cantidadValor;

    @Column(length = 40)
    private String unidad;

    @Column(name = "fecha_hora_entrega")
    private Instant fechaHoraEntrega;

    @Column(name = "registrada_at", nullable = false)
    private Instant registradaAt;

    @Column(name = "usuario_correo", nullable = false, length = 254)
    private String usuarioCorreo;

    @Column(name = "satisface_necesidad_completa", nullable = false)
    private boolean satisfaceNecesidadCompleta;

    @Column(length = 1000)
    private String observacion;

    protected EntregaAyuda() {
    }

    public EntregaAyuda(
            Familia familia,
            Necesidad tipoAyuda,
            String cantidad,
            LocalDate fechaEntrega,
            Instant registradaAt,
            String usuarioCorreo,
            boolean satisfaceNecesidadCompleta,
            String observacion) {
        this.familia = familia;
        this.tipoAyuda = tipoAyuda;
        this.cantidad = cantidad;
        this.fechaEntrega = fechaEntrega;
        this.registradaAt = registradaAt;
        this.usuarioCorreo = usuarioCorreo;
        this.satisfaceNecesidadCompleta = satisfaceNecesidadCompleta;
        this.observacion = observacion;
    }

    public EntregaAyuda(
            Familia familia,
            Necesidad tipoAyuda,
            AsignacionAyuda asignacion,
            BigDecimal cantidadValor,
            String unidad,
            Instant fechaHoraEntrega,
            Instant registradaAt,
            String usuarioCorreo,
            String observacion) {
        this.familia = familia;
        this.tipoAyuda = tipoAyuda;
        this.asignacion = asignacion;
        this.articuloAyudaId = asignacion.getArticulo().getId();
        this.cantidadValor = cantidadValor;
        this.unidad = unidad;
        this.fechaHoraEntrega = fechaHoraEntrega;
        this.registradaAt = registradaAt;
        this.usuarioCorreo = usuarioCorreo;
        this.satisfaceNecesidadCompleta = false;
        this.observacion = observacion;
    }

    public Long getId() {
        return id;
    }

    public Necesidad getTipoAyuda() {
        return tipoAyuda;
    }

    public String getCantidad() {
        return cantidad;
    }

    public BigDecimal getCantidadValor() {
        return cantidadValor;
    }

    public String getUnidad() {
        return unidad;
    }

    public LocalDate getFechaEntrega() {
        return fechaEntrega;
    }

    public Instant getFechaHoraEntrega() {
        return fechaHoraEntrega;
    }

    public Instant getRegistradaAt() {
        return registradaAt;
    }

    public String getUsuarioCorreo() {
        return usuarioCorreo;
    }

    public boolean isSatisfaceNecesidadCompleta() {
        return satisfaceNecesidadCompleta;
    }

    public String getObservacion() {
        return observacion;
    }

    public AsignacionAyuda getAsignacion() {
        return asignacion;
    }
}
