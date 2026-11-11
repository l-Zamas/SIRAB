package com.sirab.priorizacion;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.service.RegistroFamiliarService;
import com.sirab.registro.service.RegistroFamiliarService.SolicitudResumen;

@Service
public class PrioridadSolicitudesService {

    private static final Comparator<SolicitudPriorizada> ORDEN_PRIORIDAD =
            Comparator.comparingInt((SolicitudPriorizada solicitud) ->
                            solicitud.urgencia().nivel().getOrden())
                    .thenComparing(
                            Comparator.comparingInt(
                                    (SolicitudPriorizada solicitud) ->
                                            solicitud.urgencia().puntuacionTotal())
                                    .reversed())
                    .thenComparing(solicitud -> solicitud.solicitud().fechaCreacion())
                    .thenComparing(
                            solicitud -> solicitud.solicitud().id(),
                            Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<SolicitudPriorizada> ORDEN_HISTORICO =
            Comparator.comparing((SolicitudPriorizada solicitud) ->
                            solicitud.solicitud().fechaCreacion())
                    .thenComparing(
                            solicitud -> solicitud.solicitud().id(),
                            Comparator.nullsLast(Comparator.naturalOrder()));

    private final RegistroFamiliarService registroFamiliarService;
    private final CalculadorUrgencia calculadorUrgencia;

    public PrioridadSolicitudesService(
            RegistroFamiliarService registroFamiliarService,
            CalculadorUrgencia calculadorUrgencia) {
        this.registroFamiliarService = registroFamiliarService;
        this.calculadorUrgencia = calculadorUrgencia;
    }

    @Transactional(readOnly = true)
    public ResultadoPriorizacion listar(Optional<NivelUrgencia> filtro) {
        return priorizar(registroFamiliarService.listarSolicitudes(), filtro, Instant.now());
    }

    ResultadoPriorizacion priorizar(
            List<SolicitudResumen> solicitudes,
            Optional<NivelUrgencia> filtro,
            Instant ahora) {
        Objects.requireNonNull(filtro, "El filtro de urgencia es obligatorio.");
        List<SolicitudPriorizada> calculadas = solicitudes.stream()
                .map(solicitud -> new SolicitudPriorizada(
                        solicitud, calculadorUrgencia.calcular(solicitud, ahora)))
                .toList();

        List<SolicitudPriorizada> activas = calculadas.stream()
                .filter(solicitud -> solicitud.solicitud().estado() != EstadoRegistro.ATENDIDO)
                .filter(solicitud -> filtro
                        .map(nivel -> solicitud.urgencia().nivel() == nivel)
                        .orElse(true))
                .sorted(ORDEN_PRIORIDAD)
                .toList();
        List<SolicitudPriorizada> atendidas = calculadas.stream()
                .filter(solicitud -> solicitud.solicitud().estado() == EstadoRegistro.ATENDIDO)
                .sorted(ORDEN_HISTORICO)
                .toList();
        return new ResultadoPriorizacion(activas, atendidas);
    }

    public record SolicitudPriorizada(
            SolicitudResumen solicitud,
            PuntuacionUrgencia urgencia) {
    }

    public record ResultadoPriorizacion(
            List<SolicitudPriorizada> activas,
            List<SolicitudPriorizada> atendidas) {
        public ResultadoPriorizacion {
            activas = List.copyOf(activas);
            atendidas = List.copyOf(atendidas);
        }
    }
}
