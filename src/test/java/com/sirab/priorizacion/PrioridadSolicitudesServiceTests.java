package com.sirab.priorizacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.Necesidad;
import com.sirab.registro.SituacionFamiliar;
import com.sirab.registro.service.RegistroFamiliarService;
import com.sirab.registro.service.RegistroFamiliarService.SolicitudResumen;

class PrioridadSolicitudesServiceTests {

    private static final Instant AHORA = Instant.parse("2026-06-20T12:00:00Z");
    private final PrioridadSolicitudesService service =
            new PrioridadSolicitudesService(
                    Mockito.mock(RegistroFamiliarService.class), new CalculadorUrgencia());

    @Test
    void ordenaPorNivelYPuntuacionYDesempataPorFechaMasAntigua() {
        List<SolicitudResumen> solicitudes = List.of(
                solicitud(1, AHORA.minusSeconds(86_400), Set.of(Necesidad.ALOJAMIENTO),
                        Set.of(SituacionFamiliar.ADULTOS_MAYORES), EstadoRegistro.PENDIENTE),
                solicitud(2, AHORA.minusSeconds(11 * 86_400L), Set.of(Necesidad.ALOJAMIENTO),
                        Set.of(SituacionFamiliar.ADULTOS_MAYORES), EstadoRegistro.PENDIENTE),
                solicitud(3, AHORA.minusSeconds(2 * 86_400L), Set.of(Necesidad.AGUA),
                        Set.of(), EstadoRegistro.PENDIENTE),
                solicitud(4, AHORA.minusSeconds(86_400), Set.of(Necesidad.AGUA),
                        Set.of(), EstadoRegistro.PENDIENTE));

        var resultado = service.priorizar(solicitudes, Optional.empty(), AHORA);

        assertIterableEquals(List.of(2L, 1L, 3L, 4L),
                resultado.activas().stream().map(item -> item.solicitud().id()).toList());
    }

    @Test
    void filtraPorCadaNivelSinPerderElOrdenDePrioridad() {
        List<SolicitudResumen> solicitudes = solicitudesDeCadaNivel();

        for (NivelUrgencia nivel : NivelUrgencia.values()) {
            var resultado = service.priorizar(solicitudes, Optional.of(nivel), AHORA);

            assertEquals(1, resultado.activas().size());
            assertEquals(nivel, resultado.activas().getFirst().urgencia().nivel());
        }

        assertEquals(4, service.priorizar(solicitudes, Optional.empty(), AHORA).activas().size());
    }

    @Test
    void combinaElFiltroConElOrdenDescendenteDePuntuacion() {
        List<SolicitudResumen> solicitudes = List.of(
                solicitud(1, AHORA.minusSeconds(86_400), Set.of(Necesidad.ALOJAMIENTO),
                        Set.of(SituacionFamiliar.MENORES_DE_CINCO), EstadoRegistro.PENDIENTE),
                solicitud(2, AHORA.minusSeconds(86_400), Set.of(Necesidad.ALOJAMIENTO),
                        Set.of(SituacionFamiliar.ADULTOS_MAYORES), EstadoRegistro.PENDIENTE),
                solicitud(3, AHORA.minusSeconds(86_400), Set.of(Necesidad.MEDICAMENTOS),
                        Set.of(SituacionFamiliar.MENORES_DE_CINCO), EstadoRegistro.PENDIENTE));

        var resultado = service.priorizar(
                solicitudes, Optional.of(NivelUrgencia.MEDIA), AHORA);

        assertIterableEquals(List.of(1L, 2L, 3L),
                resultado.activas().stream().map(item -> item.solicitud().id()).toList());
    }

    @Test
    void mantieneLasSolicitudesAtendidasEnElHistorialFueraDeLaCola() {
        List<SolicitudResumen> solicitudes = List.of(
                solicitud(1, AHORA.minusSeconds(86_400), Set.of(Necesidad.ALOJAMIENTO),
                        Set.of(), EstadoRegistro.ATENDIDO),
                solicitud(2, AHORA.minusSeconds(2 * 86_400L), Set.of(Necesidad.ALIMENTOS),
                        Set.of(), EstadoRegistro.PENDIENTE));

        var resultado = service.priorizar(solicitudes, Optional.empty(), AHORA);

        assertIterableEquals(List.of(2L),
                resultado.activas().stream().map(item -> item.solicitud().id()).toList());
        assertIterableEquals(List.of(1L),
                resultado.atendidas().stream().map(item -> item.solicitud().id()).toList());
    }

    @Test
    void unaMismaSolicitudAumentaSuUrgenciaAlAvanzarElTiempo() {
        SolicitudResumen solicitud = solicitud(
                1,
                AHORA.minusSeconds(2 * 86_400L),
                Set.of(Necesidad.ALOJAMIENTO),
                Set.of(),
                EstadoRegistro.PENDIENTE);

        var antes = service.priorizar(List.of(solicitud), Optional.empty(), AHORA).activas().getFirst();
        var despues = service.priorizar(
                List.of(solicitud), Optional.empty(), AHORA.plusSeconds(86_400)).activas().getFirst();

        assertEquals(8, antes.urgencia().puntuacionTotal());
        assertEquals(NivelUrgencia.BAJA, antes.urgencia().nivel());
        assertEquals(10, despues.urgencia().puntuacionTotal());
        assertEquals(NivelUrgencia.MEDIA, despues.urgencia().nivel());
    }

    private List<SolicitudResumen> solicitudesDeCadaNivel() {
        return List.of(
                solicitud(1, AHORA.minusSeconds(86_400), Set.of(Necesidad.ALOJAMIENTO,
                        Necesidad.MEDICAMENTOS, Necesidad.AGUA, Necesidad.ALIMENTOS),
                        EnumSet.allOf(SituacionFamiliar.class), EstadoRegistro.PENDIENTE),
                solicitud(2, AHORA.minusSeconds(7 * 86_400L),
                        Set.of(Necesidad.ALOJAMIENTO, Necesidad.MEDICAMENTOS),
                        Set.of(SituacionFamiliar.ADULTOS_MAYORES,
                                SituacionFamiliar.PERSONAS_CON_DISCAPACIDAD), EstadoRegistro.PENDIENTE),
                solicitud(3, AHORA.minusSeconds(86_400), Set.of(Necesidad.ALOJAMIENTO),
                        Set.of(SituacionFamiliar.MENORES_DE_CINCO), EstadoRegistro.PENDIENTE),
                solicitud(4, AHORA.minusSeconds(86_400), Set.of(Necesidad.ALIMENTOS),
                        Set.of(), EstadoRegistro.PENDIENTE));
    }

    private SolicitudResumen solicitud(
            long id,
            Instant fechaCreacion,
            Set<Necesidad> necesidades,
            Set<SituacionFamiliar> situaciones,
            EstadoRegistro estado) {
        return new SolicitudResumen(
                id,
                fechaCreacion,
                "Familia " + id,
                "Barrio",
                1,
                necesidades,
                situaciones,
                estado);
    }
}
