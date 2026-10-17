package com.sirab.registro.service;

import java.util.Set;

import com.sirab.registro.Necesidad;
import com.sirab.registro.SituacionFamiliar;

public record RegistroFamiliarCommand(
        String nombreRegistrante,
        String barrio,
        String direccionReferencia,
        Integer numeroIntegrantes,
        String telefonoContacto,
        Set<SituacionFamiliar> situaciones,
        Set<Necesidad> necesidades,
        boolean consentimientoAceptado,
        java.time.Instant consentimientoAt) {
}
