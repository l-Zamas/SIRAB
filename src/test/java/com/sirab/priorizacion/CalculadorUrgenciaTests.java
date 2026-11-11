package com.sirab.priorizacion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.Necesidad;
import com.sirab.registro.SituacionFamiliar;
import com.sirab.registro.service.RegistroFamiliarService.SolicitudResumen;

class CalculadorUrgenciaTests {

    private static final Instant CREACION = Instant.parse("2026-01-01T12:00:00Z");
    private static final Instant AHORA = Instant.parse("2026-01-01T12:00:00Z");

    private final CalculadorUrgencia calculador = new CalculadorUrgencia();

    @Test
    void asignaLaPuntuacionEstablecidaACadaNecesidad() {
        Map<Necesidad, Integer> puntuaciones = Map.of(
                Necesidad.ALOJAMIENTO, 8,
                Necesidad.MEDICAMENTOS, 5,
                Necesidad.AGUA, 3,
                Necesidad.ALIMENTOS, 2);

        puntuaciones.forEach((necesidad, puntos) ->
                assertEquals(puntos, calcular(Set.of(necesidad), Set.of(), AHORA).puntuacionNecesidades()));
        assertEquals(18, calcular(EnumSet.allOf(Necesidad.class), Set.of(), AHORA)
                .puntuacionNecesidades());
    }

    @Test
    void asignaCadaCondicionUnaSolaVezYPermiteSumarCondicionesDiferentes() {
        Map<SituacionFamiliar, Integer> puntuaciones = Map.of(
                SituacionFamiliar.MENORES_DE_CINCO, 5,
                SituacionFamiliar.MUJERES_GESTANTES, 5,
                SituacionFamiliar.PERSONAS_CON_DISCAPACIDAD, 3,
                SituacionFamiliar.ADULTOS_MAYORES, 3);

        puntuaciones.forEach((situacion, puntos) ->
                assertEquals(puntos, calcular(Set.of(), Set.of(situacion), AHORA)
                        .puntuacionSituacionFamiliar()));
        assertEquals(16, calcular(Set.of(), EnumSet.allOf(SituacionFamiliar.class), AHORA)
                .puntuacionSituacionFamiliar());
    }

    @Test
    void noMultiplicaUnaCondicionPorElNumeroDeIntegrantes() {
        SolicitudResumen familiaConVariosIntegrantes = new SolicitudResumen(
                1L,
                CREACION,
                "Familia de prueba",
                "Barrio de prueba",
                8,
                Set.of(),
                Set.of(SituacionFamiliar.MENORES_DE_CINCO),
                EstadoRegistro.PENDIENTE);

        assertEquals(5, calculador.calcular(familiaConVariosIntegrantes, AHORA)
                .puntuacionSituacionFamiliar());
    }

    @Test
    void alcanzaElMaximoDeCuarentaPuntos() {
        PuntuacionUrgencia urgencia = calcular(
                EnumSet.allOf(Necesidad.class),
                EnumSet.allOf(SituacionFamiliar.class),
                CREACION.plusSeconds(11 * 86_400L));

        assertEquals(18, urgencia.puntuacionNecesidades());
        assertEquals(16, urgencia.puntuacionSituacionFamiliar());
        assertEquals(6, urgencia.puntuacionTiempo());
        assertEquals(40, urgencia.puntuacionTotal());
        assertEquals(NivelUrgencia.CRITICA, urgencia.nivel());
    }

    @Test
    void calculaLosIntervalosConDiasCompletosDesdeLaFechaYHoraDeCreacion() {
        assertEquals(0, calcular(Set.of(), Set.of(), CREACION).puntuacionTiempo());
        assertEquals(0, calcular(Set.of(), Set.of(), CREACION.plusSeconds(2 * 86_400L))
                .puntuacionTiempo());
        assertEquals(2, calcular(Set.of(), Set.of(), CREACION.plusSeconds(3 * 86_400L))
                .puntuacionTiempo());
        assertEquals(2, calcular(Set.of(), Set.of(), CREACION.plusSeconds(5 * 86_400L))
                .puntuacionTiempo());
        assertEquals(4, calcular(Set.of(), Set.of(), CREACION.plusSeconds(6 * 86_400L))
                .puntuacionTiempo());
        assertEquals(4, calcular(Set.of(), Set.of(), CREACION.plusSeconds(10 * 86_400L))
                .puntuacionTiempo());
        assertEquals(6, calcular(Set.of(), Set.of(), CREACION.plusSeconds(11 * 86_400L))
                .puntuacionTiempo());
    }

    @Test
    void noAvanzaUnIntervaloPorUnCambioDeDiaLocalAntesDeCumplirLaDuracion() {
        Instant creacionCercaDeMedianoche = Instant.parse("2026-01-01T23:59:00Z");
        SolicitudResumen solicitud = solicitud(Set.of(), Set.of(), creacionCercaDeMedianoche);

        assertEquals(0, calculador.calcular(
                solicitud, creacionCercaDeMedianoche.plusSeconds(2 * 86_400L + 60)).puntuacionTiempo());
        assertEquals(2, calculador.calcular(
                solicitud, creacionCercaDeMedianoche.plusSeconds(3 * 86_400L)).puntuacionTiempo());
    }

    @Test
    void losUmbralesDeDiasCompletosNoDependenDeLaZonaDeLaJvm() {
        TimeZone zonaOriginal = TimeZone.getDefault();
        try {
            SolicitudResumen solicitud = solicitud(Set.of(), Set.of(), CREACION);
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));

            assertEquals(0, calculador.calcular(
                    solicitud, CREACION.plusSeconds(2 * 86_400L)).puntuacionTiempo());
            assertEquals(2, calculador.calcular(
                    solicitud, CREACION.plusSeconds(3 * 86_400L)).puntuacionTiempo());
            assertEquals(2, calculador.calcular(
                    solicitud, CREACION.plusSeconds(5 * 86_400L)).puntuacionTiempo());
            assertEquals(4, calculador.calcular(
                    solicitud, CREACION.plusSeconds(6 * 86_400L)).puntuacionTiempo());
            assertEquals(4, calculador.calcular(
                    solicitud, CREACION.plusSeconds(10 * 86_400L)).puntuacionTiempo());
            assertEquals(6, calculador.calcular(
                    solicitud, CREACION.plusSeconds(11 * 86_400L)).puntuacionTiempo());
        } finally {
            TimeZone.setDefault(zonaOriginal);
        }
    }

    @Test
    void recalculaElTiempoSinCambiarLaSolicitud() {
        SolicitudResumen solicitud = solicitud(Set.of(), Set.of(), CREACION);

        assertEquals(0, calculador.calcular(solicitud, CREACION.plusSeconds(2 * 86_400L))
                .puntuacionTiempo());
        assertEquals(6, calculador.calcular(solicitud, CREACION.plusSeconds(11 * 86_400L))
                .puntuacionTiempo());
    }

    @Test
    void clasificaLosLimitesDeLosCuatroNiveles() {
        assertEquals(NivelUrgencia.BAJA, NivelUrgencia.clasificar(0));
        assertEquals(NivelUrgencia.BAJA, NivelUrgencia.clasificar(9));
        assertEquals(NivelUrgencia.MEDIA, NivelUrgencia.clasificar(10));
        assertEquals(NivelUrgencia.MEDIA, NivelUrgencia.clasificar(19));
        assertEquals(NivelUrgencia.ALTA, NivelUrgencia.clasificar(20));
        assertEquals(NivelUrgencia.ALTA, NivelUrgencia.clasificar(29));
        assertEquals(NivelUrgencia.CRITICA, NivelUrgencia.clasificar(30));
        assertEquals(NivelUrgencia.CRITICA, NivelUrgencia.clasificar(40));
    }

    @Test
    void actualizaLaPuntuacionAlCambiarNecesidadesOSituaciones() {
        SolicitudResumen solicitudOriginal = solicitud(
                Set.of(Necesidad.ALOJAMIENTO), Set.of(SituacionFamiliar.MENORES_DE_CINCO), CREACION);
        SolicitudResumen necesidadesCorregidas = solicitud(
                Set.of(Necesidad.AGUA), Set.of(SituacionFamiliar.MENORES_DE_CINCO), CREACION);
        SolicitudResumen situacionCorregida = solicitud(
                Set.of(Necesidad.ALOJAMIENTO), Set.of(SituacionFamiliar.ADULTOS_MAYORES), CREACION);

        assertEquals(13, calculador.calcular(solicitudOriginal, AHORA).puntuacionTotal());
        assertEquals(8, calculador.calcular(necesidadesCorregidas, AHORA).puntuacionTotal());
        assertEquals(11, calculador.calcular(situacionCorregida, AHORA).puntuacionTotal());
    }

    private PuntuacionUrgencia calcular(
            Set<Necesidad> necesidades,
            Set<SituacionFamiliar> situaciones,
            Instant ahora) {
        return calculador.calcular(solicitud(necesidades, situaciones, CREACION), ahora);
    }

    private SolicitudResumen solicitud(
            Set<Necesidad> necesidades,
            Set<SituacionFamiliar> situaciones,
            Instant fechaCreacion) {
        return new SolicitudResumen(
                1L,
                fechaCreacion,
                "Familia de prueba",
                "Barrio de prueba",
                1,
                necesidades,
                situaciones,
                EstadoRegistro.PENDIENTE);
    }
}
