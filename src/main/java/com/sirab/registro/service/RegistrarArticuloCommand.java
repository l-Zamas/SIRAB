package com.sirab.registro.service;

import com.sirab.registro.Necesidad;

public record RegistrarArticuloCommand(
        String codigo,
        String nombre,
        Necesidad tipoAyuda,
        String unidad,
        String usuarioCorreo) {
}
