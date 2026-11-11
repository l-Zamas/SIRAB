package com.sirab.priorizacion;

public enum NivelUrgencia {
    CRITICA(0, "Crítica", "critica"),
    ALTA(1, "Alta", "alta"),
    MEDIA(2, "Media", "media"),
    BAJA(3, "Baja", "baja");

    private final int orden;
    private final String etiqueta;
    private final String claseCss;

    NivelUrgencia(int orden, String etiqueta, String claseCss) {
        this.orden = orden;
        this.etiqueta = etiqueta;
        this.claseCss = claseCss;
    }

    public int getOrden() {
        return orden;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getClaseCss() {
        return claseCss;
    }

    public static NivelUrgencia clasificar(int puntuacionTotal) {
        if (puntuacionTotal < 0 || puntuacionTotal > 40) {
            throw new IllegalArgumentException("La puntuación de urgencia debe estar entre 0 y 40.");
        }
        if (puntuacionTotal >= 30) {
            return CRITICA;
        }
        if (puntuacionTotal >= 20) {
            return ALTA;
        }
        if (puntuacionTotal >= 10) {
            return MEDIA;
        }
        return BAJA;
    }
}
