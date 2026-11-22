package com.sirab.registro.service;

import java.time.LocalDate;

public record EvaluarNecesidadesCommand(
        Long familiaId,
        String claveIdempotencia,
        LocalDate fechaEvaluacion,
        String usuarioCorreo) {
}
