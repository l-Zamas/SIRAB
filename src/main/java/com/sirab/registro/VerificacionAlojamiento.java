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
@Table(name = "verificacion_alojamiento")
public class VerificacionAlojamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_alojamiento_id", nullable = false)
    private SolicitudAlojamiento solicitud;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ResultadoVerificacionAlojamiento resultado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_fotografia_id", nullable = false)
    private DocumentoPrivado fotografia;

    @Column(name = "verificada_at", nullable = false)
    private Instant verificadaAt;

    @Column(name = "verificada_por", nullable = false, length = 254)
    private String verificadaPor;

    @Column(length = 1000)
    private String observacion;

    protected VerificacionAlojamiento() {
    }

    public VerificacionAlojamiento(
            SolicitudAlojamiento solicitud,
            ResultadoVerificacionAlojamiento resultado,
            DocumentoPrivado fotografia,
            Instant verificadaAt,
            String verificadaPor,
            String observacion) {
        this.solicitud = solicitud;
        this.resultado = resultado;
        this.fotografia = fotografia;
        this.verificadaAt = verificadaAt;
        this.verificadaPor = verificadaPor;
        this.observacion = observacion;
    }

    public Long getId() {
        return id;
    }

    public SolicitudAlojamiento getSolicitud() {
        return solicitud;
    }

    public ResultadoVerificacionAlojamiento getResultado() {
        return resultado;
    }

    public DocumentoPrivado getFotografia() {
        return fotografia;
    }

    public Instant getVerificadaAt() {
        return verificadaAt;
    }

    public String getVerificadaPor() {
        return verificadaPor;
    }

    public String getObservacion() {
        return observacion;
    }
}
