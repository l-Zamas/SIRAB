package com.sirab.registro.service;

import java.util.List;
import java.util.Set;

import com.sirab.registro.AsignacionAyuda;
import com.sirab.registro.EvaluacionAsignacionAyuda;
import com.sirab.registro.Necesidad;

public record ResultadoEvaluacion(
        EvaluacionAsignacionAyuda evaluacion,
        List<AsignacionAyuda> asignaciones,
        Set<Necesidad> necesidadesEvaluadas,
        Set<Necesidad> necesidadesSinReglas,
        boolean reintentoIdempotente) {

    public ResultadoEvaluacion {
        asignaciones = asignaciones != null ? List.copyOf(asignaciones) : List.of();
        necesidadesEvaluadas = necesidadesEvaluadas != null ? Set.copyOf(necesidadesEvaluadas) : Set.of();
        necesidadesSinReglas = necesidadesSinReglas != null ? Set.copyOf(necesidadesSinReglas) : Set.of();
    }
}
