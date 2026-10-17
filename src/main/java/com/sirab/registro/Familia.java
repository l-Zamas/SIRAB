package com.sirab.registro;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "familia")
public class Familia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_registrante", nullable = false, length = 120)
    private String nombreRegistrante;

    @Column(length = 120)
    private String barrio;

    @Column(name = "direccion_referencia", length = 255)
    private String direccionReferencia;

    @Column(name = "numero_integrantes", nullable = false)
    private Integer numeroIntegrantes;

    @Column(name = "telefono_contacto", length = 100)
    private String telefonoContacto;

    @Column(name = "consentimiento_at", nullable = false)
    private Instant consentimientoAt;

    @Column(name = "consentimiento_version", length = 40)
    private String consentimientoVersion;

    @ElementCollection
    @CollectionTable(
            name = "familia_necesidad",
            joinColumns = @JoinColumn(name = "familia_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"familia_id", "necesidad"}))
    @Column(name = "necesidad", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Set<Necesidad> necesidades = new HashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "familia_situacion",
            joinColumns = @JoinColumn(name = "familia_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"familia_id", "situacion"}))
    @Column(name = "situacion", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private Set<SituacionFamiliar> situaciones = new HashSet<>();

    public Familia() {
    }

    public Long getId() {
        return id;
    }

    public String getNombreRegistrante() {
        return nombreRegistrante;
    }

    public void setNombreRegistrante(String nombreRegistrante) {
        this.nombreRegistrante = nombreRegistrante;
    }

    public String getBarrio() {
        return barrio;
    }

    public void setBarrio(String barrio) {
        this.barrio = barrio;
    }

    public String getDireccionReferencia() {
        return direccionReferencia;
    }

    public void setDireccionReferencia(String direccionReferencia) {
        this.direccionReferencia = direccionReferencia;
    }

    public Integer getNumeroIntegrantes() {
        return numeroIntegrantes;
    }

    public void setNumeroIntegrantes(Integer numeroIntegrantes) {
        this.numeroIntegrantes = numeroIntegrantes;
    }

    public String getTelefonoContacto() {
        return telefonoContacto;
    }

    public void setTelefonoContacto(String telefonoContacto) {
        this.telefonoContacto = telefonoContacto;
    }

    public Instant getConsentimientoAt() {
        return consentimientoAt;
    }

    public void setConsentimientoAt(Instant consentimientoAt) {
        this.consentimientoAt = consentimientoAt;
    }

    public String getConsentimientoVersion() {
        return consentimientoVersion;
    }

    public void setConsentimientoVersion(String consentimientoVersion) {
        this.consentimientoVersion = consentimientoVersion;
    }

    public Set<Necesidad> getNecesidades() {
        return necesidades;
    }

    public void setNecesidades(Set<Necesidad> necesidades) {
        this.necesidades = new HashSet<>(necesidades);
    }

    public Set<SituacionFamiliar> getSituaciones() {
        return situaciones;
    }

    public void setSituaciones(Set<SituacionFamiliar> situaciones) {
        this.situaciones = new HashSet<>(situaciones);
    }
}
