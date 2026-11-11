package com.sirab.config;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Component;

@Component("fechaHoraBogota")
public class FechaHoraBogota {

    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Bogota");

    public ZonedDateTime convertir(Instant instante) {
        return instante.atZone(ZONA_HORARIA);
    }
}
