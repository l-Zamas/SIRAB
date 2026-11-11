package com.sirab.priorizacion;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sirab.registro.Necesidad;
import com.sirab.registro.SituacionFamiliar;
import com.sirab.registro.service.RegistroFamiliarService.SolicitudResumen;

@Component
public class CalculadorUrgencia {

    private static final Map<Necesidad, Integer> PUNTOS_NECESIDAD = Map.of(
            Necesidad.ALOJAMIENTO, 8,
            Necesidad.MEDICAMENTOS, 5,
            Necesidad.AGUA, 3,
            Necesidad.ALIMENTOS, 2);

    private static final Map<SituacionFamiliar, Integer> PUNTOS_SITUACION = Map.of(
            SituacionFamiliar.MENORES_DE_CINCO, 5,
            SituacionFamiliar.MUJERES_GESTANTES, 5,
            SituacionFamiliar.PERSONAS_CON_DISCAPACIDAD, 3,
            SituacionFamiliar.ADULTOS_MAYORES, 3);

    public PuntuacionUrgencia calcular(SolicitudResumen solicitud, Instant ahora) {
        Objects.requireNonNull(solicitud, "La solicitud es obligatoria.");
        Objects.requireNonNull(ahora, "La fecha de cálculo es obligatoria.");
        long diasTranscurridos = Math.max(
                0, Duration.between(solicitud.fechaCreacion(), ahora).toDays());
        int puntosNecesidades = sumar(solicitud.necesidades(), PUNTOS_NECESIDAD);
        int puntosSituacion = sumar(solicitud.situaciones(), PUNTOS_SITUACION);
        int puntosTiempo = puntuarTiempo(diasTranscurridos);
        int total = puntosNecesidades + puntosSituacion + puntosTiempo;
        return new PuntuacionUrgencia(
                puntosNecesidades,
                puntosSituacion,
                puntosTiempo,
                total,
                NivelUrgencia.clasificar(total));
    }

    private <T> int sumar(Set<T> valores, Map<T, Integer> puntuaciones) {
        return valores.stream()
                .mapToInt(valor -> puntuaciones.getOrDefault(valor, 0))
                .sum();
    }

    private int puntuarTiempo(long diasTranscurridos) {
        if (diasTranscurridos <= 2) {
            return 0;
        }
        if (diasTranscurridos <= 5) {
            return 2;
        }
        if (diasTranscurridos <= 10) {
            return 4;
        }
        return 6;
    }
}
