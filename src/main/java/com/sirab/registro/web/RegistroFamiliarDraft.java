package com.sirab.registro.web;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import com.sirab.registro.Necesidad;
import com.sirab.registro.SituacionFamiliar;
import com.sirab.registro.service.RegistroFamiliarCommand;

public final class RegistroFamiliarDraft implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String nombreRegistrante;
    private Integer numeroIntegrantes;
    private String telefonoContacto;
    private String barrio;
    private String direccionReferencia;
    private Set<SituacionFamiliar> situaciones = EnumSet.noneOf(SituacionFamiliar.class);
    private Set<Necesidad> necesidades = EnumSet.noneOf(Necesidad.class);
    private Instant consentimientoAt;
    private int etapa;

    public String getNombreRegistrante() {
        return nombreRegistrante;
    }

    public void setNombreRegistrante(String nombreRegistrante) {
        this.nombreRegistrante = nombreRegistrante;
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

    public Set<SituacionFamiliar> getSituaciones() {
        return Set.copyOf(situaciones);
    }

    public void setSituaciones(Set<SituacionFamiliar> situaciones) {
        this.situaciones = situaciones.isEmpty()
                ? EnumSet.noneOf(SituacionFamiliar.class)
                : EnumSet.copyOf(situaciones);
    }

    public Set<Necesidad> getNecesidades() {
        return Set.copyOf(necesidades);
    }

    public void setNecesidades(Set<Necesidad> necesidades) {
        this.necesidades = necesidades.isEmpty()
                ? EnumSet.noneOf(Necesidad.class)
                : EnumSet.copyOf(necesidades);
    }

    public Instant getConsentimientoAt() {
        return consentimientoAt;
    }

    public void setConsentimientoAt(Instant consentimientoAt) {
        this.consentimientoAt = consentimientoAt;
    }

    public int getEtapa() {
        return etapa;
    }

    public void setEtapa(int etapa) {
        this.etapa = etapa;
    }

    RegistroFamiliarCommand toCommand() {
        return new RegistroFamiliarCommand(
                nombreRegistrante,
                barrio,
                direccionReferencia,
                numeroIntegrantes,
                telefonoContacto,
                situaciones,
                necesidades,
                consentimientoAt != null,
                consentimientoAt);
    }
}
