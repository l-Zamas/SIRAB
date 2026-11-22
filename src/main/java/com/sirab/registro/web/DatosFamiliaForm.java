package com.sirab.registro.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class DatosFamiliaForm {

    @NotBlank(message = "Ingresa tus nombres y apellidos.")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres.")
    private String nombreRegistrante;

    @NotNull(message = "Indica cuántas personas integran la familia.")
    @Min(value = 1, message = "Debe indicarse al menos una persona.")
    private Integer numeroIntegrantes;

    @NotBlank(message = "Ingresa un teléfono de contacto.")
    @Pattern(regexp = "\\d{10}", message = "El teléfono debe contener exactamente 10 dígitos.")
    @Size(max = 10, message = "El teléfono debe contener exactamente 10 dígitos.")
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
}
