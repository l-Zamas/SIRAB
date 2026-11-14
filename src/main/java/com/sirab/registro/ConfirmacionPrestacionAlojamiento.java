package com.sirab.registro;

import java.time.Instant;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Immutable
@Table(name = "confirmacion_prestacion_alojamiento")
public class ConfirmacionPrestacionAlojamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_alojamiento_id", nullable = false)
    private SolicitudAlojamiento solicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aprobacion_definitiva_id", nullable = false)
    private AprobacionDefinitivaAlojamiento aprobacionDefinitiva;

    @Column(name = "proporcionado_at", nullable = false)
    private Instant proporcionadoAt;

    @Column(name = "confirmado_at", nullable = false)
    private Instant confirmadoAt;

    @Column(name = "confirmado_por", nullable = false, length = 254)
    private String confirmadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_evidencia_id")
    private DocumentoPrivado documentoEvidencia;

    @Column(length = 1000)
    private String observacion;

    protected ConfirmacionPrestacionAlojamiento() {
    }

    public ConfirmacionPrestacionAlojamiento(
            SolicitudAlojamiento solicitud,
            AprobacionDefinitivaAlojamiento aprobacionDefinitiva,
            Instant proporcionadoAt,
            Instant confirmadoAt,
            String confirmadoPor,
            DocumentoPrivado documentoEvidencia,
            String observacion) {
        this.solicitud = solicitud;
        this.aprobacionDefinitiva = aprobacionDefinitiva;
        this.proporcionadoAt = proporcionadoAt;
        this.confirmadoAt = confirmadoAt;
        this.confirmadoPor = confirmadoPor;
        this.documentoEvidencia = documentoEvidencia;
        this.observacion = observacion;
    }

    public Long getId() {
        return id;
    }

    public SolicitudAlojamiento getSolicitud() {
        return solicitud;
    }

    public AprobacionDefinitivaAlojamiento getAprobacionDefinitiva() {
        return aprobacionDefinitiva;
    }

    public Instant getProporcionadoAt() {
        return proporcionadoAt;
    }

    public Instant getConfirmadoAt() {
        return confirmadoAt;
    }

    public String getConfirmadoPor() {
        return confirmadoPor;
    }

    public DocumentoPrivado getDocumentoEvidencia() {
        return documentoEvidencia;
    }

    public String getObservacion() {
        return observacion;
    }
}
