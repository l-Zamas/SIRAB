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
        name = "soporte_medico",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_soporte_medico_documento",
                columnNames = "documento_privado_id"))
public class SoporteMedico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_ayuda_medica_id", nullable = false)
    private SolicitudAyudaMedica solicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_privado_id", nullable = false)
    private DocumentoPrivado documento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_soporte", nullable = false, length = 40)
    private TipoSoporteMedico tipoSoporte;

    @Column(name = "cargado_at", nullable = false)
    private Instant cargadoAt;

    @Column(name = "cargado_por", nullable = false, length = 254)
    private String cargadoPor;

    protected SoporteMedico() {
    }

    public SoporteMedico(
            SolicitudAyudaMedica solicitud,
            DocumentoPrivado documento,
            TipoSoporteMedico tipoSoporte,
            Instant cargadoAt,
            String cargadoPor) {
        this.solicitud = solicitud;
        this.documento = documento;
        this.tipoSoporte = tipoSoporte;
        this.cargadoAt = cargadoAt;
        this.cargadoPor = cargadoPor;
    }

    public Long getId() {
        return id;
    }

    public SolicitudAyudaMedica getSolicitud() {
        return solicitud;
    }

    public DocumentoPrivado getDocumento() {
        return documento;
    }

    public TipoSoporteMedico getTipoSoporte() {
        return tipoSoporte;
    }

    public Instant getCargadoAt() {
        return cargadoAt;
    }

    public String getCargadoPor() {
        return cargadoPor;
    }
}
