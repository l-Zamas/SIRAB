package com.sirab.registro.service;

import java.time.LocalDate;

import com.sirab.registro.Necesidad;

public record RegistrarEntregaCommand(
        Necesidad tipoAyuda,
        String cantidad,
        LocalDate fechaEntrega,
        boolean satisfaceNecesidadCompleta,
        String observacion) {
}
