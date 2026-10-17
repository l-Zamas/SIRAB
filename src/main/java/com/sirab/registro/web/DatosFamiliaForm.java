package com.sirab.registro.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DatosFamiliaForm {

    @NotBlank(message = "Ingresa el nombre de quien registra.")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres.")
    private String nombreRegistrante;

    @NotNull(message = "Indica cuántas personas integran la familia.")
    @Min(value = 1, message = "Debe indicarse al menos una persona.")
    private Integer numeroIntegrantes;

    @Size(max = 100, message = "El teléfono no puede superar 100 caracteres.")
    private String telefonoContacto;

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

    void clearPersonalValues() {
        nombreRegistrante = null;
        telefonoContacto = null;
        numeroIntegrantes = null;
    }
}
