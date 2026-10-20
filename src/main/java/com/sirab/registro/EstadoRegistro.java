package com.sirab.registro;

public enum EstadoRegistro {
    PENDIENTE("Pendiente"),
    VERIFICADO("Verificado"),
    ATENDIDO("Atendido");

    private final String etiqueta;

    EstadoRegistro(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
