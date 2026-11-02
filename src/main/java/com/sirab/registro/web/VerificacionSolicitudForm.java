package com.sirab.registro.web;

import java.util.EnumSet;
import java.util.Set;

import com.sirab.registro.Necesidad;
import com.sirab.registro.SituacionFamiliar;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VerificacionSolicitudForm {

    @NotBlank(message = "El nombre de quien registra es obligatorio.")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres.")
    private String nombreRegistrante;

    @NotNull(message = "Indica cuántas personas integran la familia.")
    @Min(value = 1, message = "Debe indicarse al menos una persona.")
    private Integer numeroIntegrantes;

    @Size(max = 100, message = "El teléfono no puede superar 100 caracteres.")
    private String telefonoContacto;

    @Size(max = 120, message = "El barrio no puede superar 120 caracteres.")
    private String barrio;

    @Size(max = 255, message = "La referencia no puede superar 255 caracteres.")
    private String direccionReferencia;

    private Set<SituacionFamiliar> situaciones = EnumSet.noneOf(SituacionFamiliar.class);
    private Set<Necesidad> necesidades = EnumSet.noneOf(Necesidad.class);

    @Size(max = 1000, message = "La observación no puede superar 1000 caracteres.")
    private String observacion;

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
        this.situaciones = situaciones == null || situaciones.isEmpty()
                ? EnumSet.noneOf(SituacionFamiliar.class)
                : EnumSet.copyOf(situaciones);
    }

    public Set<Necesidad> getNecesidades() {
        return Set.copyOf(necesidades);
    }

    public void setNecesidades(Set<Necesidad> necesidades) {
        this.necesidades = necesidades == null || necesidades.isEmpty()
                ? EnumSet.noneOf(Necesidad.class)
                : EnumSet.copyOf(necesidades);
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
