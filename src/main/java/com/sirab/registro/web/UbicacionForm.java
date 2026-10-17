package com.sirab.registro.web;

import jakarta.validation.constraints.Size;

public class UbicacionForm {

    @Size(max = 120, message = "El barrio no puede superar 120 caracteres.")
    private String barrio;

    @Size(max = 255, message = "La dirección o referencia no puede superar 255 caracteres.")
    private String direccionReferencia;

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

    void clearValues() {
        barrio = null;
        direccionReferencia = null;
    }
}
