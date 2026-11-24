package com.sirab.registro;

public enum EstadoRegistro {
    PENDIENTE("Pendiente"),
    EN_ATENCION("En atención"),
    ATENDIDO("Atendido");

    private final String etiqueta;

    EstadoRegistro(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
