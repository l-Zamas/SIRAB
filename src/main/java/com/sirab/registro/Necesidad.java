package com.sirab.registro;

public enum Necesidad {
    ALIMENTOS("Alimentos"),
    AGUA("Agua"),
    MEDICAMENTOS("Medicamentos"),
    ALOJAMIENTO("Alojamiento");

    private final String etiqueta;

    Necesidad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
