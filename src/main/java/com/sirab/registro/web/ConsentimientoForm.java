package com.sirab.registro.web;

import jakarta.validation.constraints.AssertTrue;

public class ConsentimientoForm {

    @AssertTrue(message = "Debes aceptar el uso de los datos para continuar.")
    private boolean aceptado;

    public boolean isAceptado() {
        return aceptado;
    }

    public void setAceptado(boolean aceptado) {
        this.aceptado = aceptado;
    }
}
