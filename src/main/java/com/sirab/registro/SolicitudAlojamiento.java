package com.sirab.registro;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "solicitud_alojamiento",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_solicitud_alojamiento_id_familia",
                columnNames = {"id", "familia_id"}))
public class SolicitudAlojamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "familia_id", nullable = false)
    private Familia familia;

    @Column(name = "registrada_at", nullable = false)
    private Instant registradaAt;

    @Column(name = "registrada_por", nullable = false, length = 254)
    private String registradaPor;

    @Column(length = 1000)
    private String observacion;

    protected SolicitudAlojamiento() {
    }

    public SolicitudAlojamiento(
            Familia familia,
            Instant registradaAt,
            String registradaPor,
            String observacion) {
        this.familia = familia;
        this.registradaAt = registradaAt;
        this.registradaPor = registradaPor;
        this.observacion = observacion;
    }

    public Long getId() {
        return id;
    }

    public Familia getFamilia() {
        return familia;
    }

    public Instant getRegistradaAt() {
        return registradaAt;
    }

    public String getRegistradaPor() {
        return registradaPor;
    }

    public String getObservacion() {
        return observacion;
    }
}
