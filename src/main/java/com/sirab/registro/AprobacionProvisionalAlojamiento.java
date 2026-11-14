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

@Entity
@Immutable
@Table(name = "aprobacion_provisional_alojamiento")
public class AprobacionProvisionalAlojamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_alojamiento_id", nullable = false)
    private SolicitudAlojamiento solicitud;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DecisionAyuda decision;

    @Column(name = "decidida_at", nullable = false)
    private Instant decididaAt;

    @Column(name = "decidida_por", nullable = false, length = 254)
    private String decididaPor;

    @Column(length = 1000)
    private String observacion;

    protected AprobacionProvisionalAlojamiento() {
    }

    public AprobacionProvisionalAlojamiento(
            SolicitudAlojamiento solicitud,
            DecisionAyuda decision,
            Instant decididaAt,
            String decididaPor,
            String observacion) {
        this.solicitud = solicitud;
        this.decision = decision;
        this.decididaAt = decididaAt;
        this.decididaPor = decididaPor;
        this.observacion = observacion;
    }

    public Long getId() {
        return id;
    }

    public SolicitudAlojamiento getSolicitud() {
        return solicitud;
    }

    public DecisionAyuda getDecision() {
        return decision;
    }

    public Instant getDecididaAt() {
        return decididaAt;
    }

    public String getDecididaPor() {
        return decididaPor;
    }

    public String getObservacion() {
        return observacion;
    }
}
