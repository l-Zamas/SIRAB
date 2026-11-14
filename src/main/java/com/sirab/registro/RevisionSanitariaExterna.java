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
        name = "revision_sanitaria_externa",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_revision_sanitaria_id_familia",
                columnNames = {"id", "familia_id"}))
public class RevisionSanitariaExterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_ayuda_medica_id", nullable = false)
    private SolicitudAyudaMedica solicitud;

    @Column(name = "familia_id", nullable = false)
    private Long familiaId;

    @Column(nullable = false, length = 200)
    private String institucion;

    @Column(name = "profesional_nombre", nullable = false, length = 200)
    private String profesionalNombre;

    @Column(name = "identificador_profesional", nullable = false, length = 100)
    private String identificadorProfesional;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DecisionAyuda decision;

    @Column(name = "decidida_at", nullable = false)
    private Instant decididaAt;

    @Column(name = "referencia_externa", length = 200)
    private String referenciaExterna;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_decision_id", nullable = false)
    private DocumentoPrivado documentoDecision;

    @Column(name = "registrada_at", nullable = false)
    private Instant registradaAt;

    @Column(name = "registrada_por", nullable = false, length = 254)
    private String registradaPor;

    @Column(length = 1000)
    private String observacion;

    protected RevisionSanitariaExterna() {
    }

    public RevisionSanitariaExterna(
            SolicitudAyudaMedica solicitud,
            String institucion,
            String profesionalNombre,
            String identificadorProfesional,
            DecisionAyuda decision,
            Instant decididaAt,
            String referenciaExterna,
            DocumentoPrivado documentoDecision,
            Instant registradaAt,
            String registradaPor,
            String observacion) {
        this.solicitud = solicitud;
        this.familiaId = solicitud.getFamilia().getId();
        this.institucion = institucion;
        this.profesionalNombre = profesionalNombre;
        this.identificadorProfesional = identificadorProfesional;
        this.decision = decision;
        this.decididaAt = decididaAt;
        this.referenciaExterna = referenciaExterna;
        this.documentoDecision = documentoDecision;
        this.registradaAt = registradaAt;
        this.registradaPor = registradaPor;
        this.observacion = observacion;
    }

    public Long getId() {
        return id;
    }

    public SolicitudAyudaMedica getSolicitud() {
        return solicitud;
    }

    public String getInstitucion() {
        return institucion;
    }

    public String getProfesionalNombre() {
        return profesionalNombre;
    }

    public String getIdentificadorProfesional() {
        return identificadorProfesional;
    }

    public DecisionAyuda getDecision() {
        return decision;
    }

    public Instant getDecididaAt() {
        return decididaAt;
    }

    public String getReferenciaExterna() {
        return referenciaExterna;
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
