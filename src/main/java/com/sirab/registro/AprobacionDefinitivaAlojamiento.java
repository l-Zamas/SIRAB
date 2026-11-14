package com.sirab.registro;

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
        name = "aprobacion_definitiva_alojamiento",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_aprobacion_definitiva_id_solicitud",
                columnNames = {"id", "solicitud_alojamiento_id"}))
public class AprobacionDefinitivaAlojamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_alojamiento_id", nullable = false)
    private SolicitudAlojamiento solicitud;

    @Column(nullable = false, length = 200)
    private String institucion;

    @Column(name = "referencia_externa", length = 200)
    private String referenciaExterna;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DecisionAyuda decision;

    @Column(name = "decidida_at", nullable = false)
    private Instant decididaAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_decision_id")
    private DocumentoPrivado documentoDecision;

    @Column(name = "registrada_at", nullable = false)
    private Instant registradaAt;

    @Column(name = "registrada_por", nullable = false, length = 254)
    private String registradaPor;

    @Column(length = 1000)
    private String observacion;

    protected AprobacionDefinitivaAlojamiento() {
    }

    public AprobacionDefinitivaAlojamiento(
            SolicitudAlojamiento solicitud,
            String institucion,
            String referenciaExterna,
            DecisionAyuda decision,
            Instant decididaAt,
            DocumentoPrivado documentoDecision,
            Instant registradaAt,
            String registradaPor,
            String observacion) {
        this.solicitud = solicitud;
        this.institucion = institucion;
        this.referenciaExterna = referenciaExterna;
        this.decision = decision;
        this.decididaAt = decididaAt;
        this.documentoDecision = documentoDecision;
        this.registradaAt = registradaAt;
        this.registradaPor = registradaPor;
        this.observacion = observacion;
    }

    public Long getId() {
        return id;
    }

    public SolicitudAlojamiento getSolicitud() {
        return solicitud;
    }

    public String getInstitucion() {
        return institucion;
    }

    public String getReferenciaExterna() {
        return referenciaExterna;
    }

    public DecisionAyuda getDecision() {
        return decision;
    }

    public Instant getDecididaAt() {
        return decididaAt;
    }

    public DocumentoPrivado getDocumentoDecision() {
        return documentoDecision;
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
