package com.sirab.registro;

public enum SituacionFamiliar {
    MENORES_DE_CINCO("Menores de 5 años"),
    ADULTOS_MAYORES("Adultos mayores"),
    PERSONAS_CON_DISCAPACIDAD("Personas con discapacidad"),
    MUJERES_GESTANTES("Mujeres gestantes");

    private final String etiqueta;

    SituacionFamiliar(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
