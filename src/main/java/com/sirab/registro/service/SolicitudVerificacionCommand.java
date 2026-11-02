package com.sirab.registro.service;

import java.util.Set;

import com.sirab.registro.Necesidad;
import com.sirab.registro.SituacionFamiliar;

public record SolicitudVerificacionCommand(
        String nombreRegistrante,
        Integer numeroIntegrantes,
        String telefonoContacto,
        String barrio,
        String direccion,
        String direccionReferencia,
        Set<SituacionFamiliar> situaciones,
        Set<Necesidad> necesidades,
        String observacion) {
}
