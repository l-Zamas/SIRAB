package com.sirab.registro.service;

import java.util.Set;

import com.sirab.registro.Necesidad;
import com.sirab.registro.SituacionFamiliar;

public record RegistroFamiliarCommand(
        String nombreRegistrante,
        String barrio,
        String direccion,
        String direccionReferencia,
        Integer numeroIntegrantes,
        String telefonoContacto,
        Set<SituacionFamiliar> situaciones,
        Set<Necesidad> necesidades,
        boolean consentimientoAceptado,
        java.time.Instant consentimientoAt,
        String evidenciaAlojamiento,
        String nombreEvidenciaAlojamiento,
        String tipoEvidenciaAlojamiento,
        String evidenciaMedicamentos,
        String nombreEvidenciaMedicamentos,
        String tipoEvidenciaMedicamentos) {

    public RegistroFamiliarCommand(
            String nombreRegistrante,
            String barrio,
            String direccion,
            String direccionReferencia,
            Integer numeroIntegrantes,
            String telefonoContacto,
            Set<SituacionFamiliar> situaciones,
            Set<Necesidad> necesidades,
            boolean consentimientoAceptado,
            java.time.Instant consentimientoAt) {
        this(nombreRegistrante, barrio, direccion, direccionReferencia, numeroIntegrantes,
                telefonoContacto, situaciones, necesidades, consentimientoAceptado, consentimientoAt,
                null, null, null, null, null, null);
    }
}
