package com.sirab.registro.web;

import jakarta.validation.constraints.NotBlank;

public class ConsultaEstadoForm {

    @NotBlank(message = "Ingresa tu código de consulta.")
    private String codigo;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}
