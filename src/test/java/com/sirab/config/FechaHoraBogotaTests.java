package com.sirab.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

class FechaHoraBogotaTests {

    private final FechaHoraBogota fechaHoraBogota = new FechaHoraBogota();

    @Test
    void convierteInstantesAUtcMenosCincoSinDependerDeLaZonaDeLaJvm() {
        TimeZone zonaOriginal = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));

            var fechaBogota = fechaHoraBogota.convertir(Instant.parse("2026-03-08T04:30:00Z"));

            assertEquals(ZoneId.of("America/Bogota"), fechaBogota.getZone());
            assertEquals(LocalDateTime.parse("2026-03-07T23:30:00"), fechaBogota.toLocalDateTime());
            assertEquals(-5 * 60 * 60, fechaBogota.getOffset().getTotalSeconds());
        } finally {
            TimeZone.setDefault(zonaOriginal);
        }
    }
}
