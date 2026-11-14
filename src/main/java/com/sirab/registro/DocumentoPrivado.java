package com.sirab.registro;

import java.time.Instant;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Immutable
@Table(
        name = "documento_privado",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_documento_privado_clave",
                columnNames = "clave_almacenamiento"))
public class DocumentoPrivado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clave_almacenamiento", nullable = false, length = 512)
    private String claveAlmacenamiento;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "tipo_contenido", nullable = false, length = 120)
    private String tipoContenido;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoDocumentoPrivado clasificacion;

    @Column(name = "cargado_at", nullable = false)
    private Instant cargadoAt;

    @Column(name = "cargado_por", nullable = false, length = 254)
    private String cargadoPor;

    protected DocumentoPrivado() {
    }

    public DocumentoPrivado(
            String claveAlmacenamiento,
            String nombreOriginal,
            String tipoContenido,
            long tamanoBytes,
            String sha256,
            TipoDocumentoPrivado clasificacion,
            Instant cargadoAt,
            String cargadoPor) {
        this.claveAlmacenamiento = claveAlmacenamiento;
        this.nombreOriginal = nombreOriginal;
        this.tipoContenido = tipoContenido;
        this.tamanoBytes = tamanoBytes;
        this.sha256 = sha256;
        this.clasificacion = clasificacion;
        this.cargadoAt = cargadoAt;
        this.cargadoPor = cargadoPor;
    }

    public Long getId() {
        return id;
    }

    public String getClaveAlmacenamiento() {
        return claveAlmacenamiento;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public String getTipoContenido() {
        return tipoContenido;
    }

    public long getTamanoBytes() {
        return tamanoBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public TipoDocumentoPrivado getClasificacion() {
        return clasificacion;
    }

    public Instant getCargadoAt() {
        return cargadoAt;
    }

    public String getCargadoPor() {
        return cargadoPor;
    }
}
