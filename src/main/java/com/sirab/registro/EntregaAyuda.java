package com.sirab.registro;

import java.time.Instant;
import java.time.LocalDate;

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
@Table(name = "entrega_ayuda")
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

    @Column(nullable = false, length = 100)
    private String cantidad;

    @Column(name = "fecha_entrega", nullable = false)
    private LocalDate fechaEntrega;

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

    public Long getId() {
        return id;
    }

    public Necesidad getTipoAyuda() {
        return tipoAyuda;
    }

    public String getCantidad() {
        return cantidad;
    }

    public LocalDate getFechaEntrega() {
        return fechaEntrega;
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
}
