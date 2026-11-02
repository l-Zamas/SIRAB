package com.sirab.registro.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UbicacionForm {

    @NotBlank(message = "Ingresa el barrio.")
    @Size(max = 120, message = "El barrio no puede superar 120 caracteres.")
    private String barrio;

    @NotBlank(message = "Ingresa la dirección.")
    @Size(max = 255, message = "La dirección no puede superar 255 caracteres.")
    private String direccion;

    @Size(max = 255, message = "El punto de referencia no puede superar 255 caracteres.")
    private String direccionReferencia;

    public String getBarrio() {
        return barrio;
    }

    public void setBarrio(String barrio) {
        this.barrio = barrio;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getDireccionReferencia() {
        return direccionReferencia;
    }

    public void setDireccionReferencia(String direccionReferencia) {
        this.direccionReferencia = direccionReferencia;
    }
}
