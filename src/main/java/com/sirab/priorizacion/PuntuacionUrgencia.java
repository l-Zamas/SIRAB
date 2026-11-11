package com.sirab.priorizacion;

public record PuntuacionUrgencia(
        int puntuacionNecesidades,
        int puntuacionSituacionFamiliar,
        int puntuacionTiempo,
        int puntuacionTotal,
        NivelUrgencia nivel) {
}
