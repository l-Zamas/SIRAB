package com.sirab.registro.service;

import java.time.LocalDate;
import java.util.List;

public record PublicarVersionReglasCommand(
        Long articuloId,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        List<CriterioReglaCommand> criterios,
        String usuarioCorreo) {

    public PublicarVersionReglasCommand {
        criterios = criterios != null ? List.copyOf(criterios) : List.of();
    }
}
