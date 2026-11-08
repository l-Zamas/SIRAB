package com.sirab.registro.duplicados;

public enum EstadoPosibleDuplicado {
    PENDIENTE_REVISION("Pendiente de revisión"),
    DUPLICADO_CONFIRMADO("Duplicado confirmado"),
    DESCARTADO("Coincidencia descartada"),
    SIN_COINCIDENCIA("Ya no coincide con los criterios");

    private final String etiqueta;

    EstadoPosibleDuplicado(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
