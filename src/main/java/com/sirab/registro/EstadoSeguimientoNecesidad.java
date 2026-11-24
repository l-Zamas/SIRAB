package com.sirab.registro;

public enum EstadoSeguimientoNecesidad {
    DECLARADA("Declarada"),
    VERIFICADA("Verificada"),
    EN_ATENCION("En atención"),
    PARCIALMENTE_ATENDIDA("Parcialmente atendida"),
    SATISFECHA("Satisfecha");

    private final String etiqueta;

    EstadoSeguimientoNecesidad(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
}
