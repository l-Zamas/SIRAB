package com.sirab.registro.service;

import java.math.BigDecimal;
import java.time.Instant;

public record RegistrarEntregaAsignacionCommand(
        Long asignacionId,
        BigDecimal cantidad,
        Long loteId,
        Instant fechaHoraEntrega,
        String observacion) {

    public RegistrarEntregaAsignacionCommand {
        if (asignacionId == null) {
            throw new IllegalArgumentException("La asignación de ayuda es obligatoria.");
        }
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad entregada debe ser mayor a cero.");
        }
    }
}
