package com.sirab.registro.duplicados;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sirab.registro.Familia;
import com.sirab.registro.FamiliaRepository;

@Service
public class DeteccionDuplicadosService {

    private static final String ACTOR_SISTEMA = "SISTEMA";

    private final FamiliaRepository familiaRepository;
    private final PosibleDuplicadoRepository posibleDuplicadoRepository;
    private final RevisionPosibleDuplicadoRepository revisionRepository;
    private final BloqueoDeteccionDuplicados bloqueo;

    public DeteccionDuplicadosService(
            FamiliaRepository familiaRepository,
            PosibleDuplicadoRepository posibleDuplicadoRepository,
            RevisionPosibleDuplicadoRepository revisionRepository,
            BloqueoDeteccionDuplicados bloqueo) {
        this.familiaRepository = familiaRepository;
        this.posibleDuplicadoRepository = posibleDuplicadoRepository;
        this.revisionRepository = revisionRepository;
        this.bloqueo = bloqueo;
    }

    @Transactional
    public void iniciarOperacion() {
        bloqueo.bloquear();
    }

    @Transactional
    public void detectarPara(Familia familia, boolean datosComparacionModificados) {
        if (familia.getId() == null) {
            return;
        }
        bloqueo.bloquear();
        String nombreNormalizado = familia.getDuplicadoNombreNormalizado();
        List<Familia> candidatas = familiaRepository.buscarCandidatasDuplicado(
                familia.getId(),
                nombreNormalizado == null || nombreNormalizado.isEmpty()
                        ? null
                        : nombreNormalizado,
                familia.getDuplicadoTelefonoNormalizado(),
                familia.getDuplicadoUbicacionNormalizada());
        Map<Long, PosibleDuplicado> existentes = new HashMap<>();
        for (PosibleDuplicado duplicado : posibleDuplicadoRepository.findAllByFamiliaId(familia.getId())) {
            existentes.put(otraFamiliaId(duplicado, familia.getId()), duplicado);
        }

        for (Familia candidata : candidatas) {
            actualizarCoincidencia(familia, candidata, datosComparacionModificados,
                    existentes.remove(candidata.getId()));
        }
        for (Map.Entry<Long, PosibleDuplicado> existente : existentes.entrySet()) {
            actualizarCoincidencia(familia, null, datosComparacionModificados, existente.getValue());
        }
    }

    @Transactional(readOnly = true)
    public Page<PosibleDuplicadoVista> listar(Pageable pageable) {
        Page<PosibleDuplicado> duplicados =
                posibleDuplicadoRepository.findAllByOrderByDetectadoAtDesc(pageable);
        if (duplicados.isEmpty()) {
            return duplicados.map(duplicado -> vista(duplicado, List.of()));
        }
        Map<Long, List<RevisionVista>> historialPorDuplicado = revisionRepository
                .findAllByPosibleDuplicadoIds(
                        duplicados.getContent().stream().map(PosibleDuplicado::getId).toList())
                .stream()
                .map(revision -> Map.entry(
                        revision.getPosibleDuplicado().getId(),
                        new RevisionVista(revision.getEvento(), revision.getActorCorreo(), revision.getFechaHora())))
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        return duplicados.map(duplicado -> vista(
                duplicado, historialPorDuplicado.getOrDefault(duplicado.getId(), List.of())));
    }

    @Transactional
    public boolean resolver(Long posibleDuplicadoId, EstadoPosibleDuplicado resultado, String actorCorreo) {
        bloqueo.bloquear();
        if (actorCorreo == null || actorCorreo.isBlank()) {
            throw new IllegalArgumentException("No se pudo identificar al usuario autenticado.");
        }
        if (resultado != EstadoPosibleDuplicado.DUPLICADO_CONFIRMADO
                && resultado != EstadoPosibleDuplicado.DESCARTADO) {
            throw new IllegalArgumentException("El resultado de revisión no es válido.");
        }
        Optional<PosibleDuplicado> posible = posibleDuplicadoRepository.findById(posibleDuplicadoId);
        if (posible.isEmpty()) {
            return false;
        }
        PosibleDuplicado duplicado = posible.get();
        Instant ahora = Instant.now();
        duplicado.resolver(resultado, ahora);
        posibleDuplicadoRepository.save(duplicado);
        revisionRepository.save(new RevisionPosibleDuplicado(
                duplicado,
                resultado == EstadoPosibleDuplicado.DUPLICADO_CONFIRMADO
                        ? TipoEventoDuplicado.CONFIRMADO
                        : TipoEventoDuplicado.DESCARTADO,
                actorCorreo,
                ahora));
        return true;
    }

    private void actualizarCoincidencia(
            Familia primera,
            Familia segunda,
            boolean datosComparacionModificados,
            PosibleDuplicado existente) {
        CoincidenciaFamilia coincidencia = segunda == null
                ? new CoincidenciaFamilia(List.of())
                : CoincidenciaFamilia.comparar(primera, segunda);
        Instant ahora = Instant.now();

        if (!coincidencia.esPosibleDuplicado()) {
            if (existente != null
                    && (existente.getEstado() == EstadoPosibleDuplicado.PENDIENTE_REVISION
                            || existente.getEstado() == EstadoPosibleDuplicado.DUPLICADO_CONFIRMADO)) {
                existente.retirar(ahora);
                posibleDuplicadoRepository.save(existente);
                revisionRepository.save(new RevisionPosibleDuplicado(
                        existente, TipoEventoDuplicado.RETIRADO_POR_CAMBIO_DE_DATOS, ACTOR_SISTEMA, ahora));
            }
            return;
        }

        if (existente == null) {
            posibleDuplicadoRepository.save(new PosibleDuplicado(primera, segunda, coincidencia, ahora));
            return;
        }

        PosibleDuplicado duplicado = existente;
        if ((duplicado.getEstado() == EstadoPosibleDuplicado.DESCARTADO
                || duplicado.getEstado() == EstadoPosibleDuplicado.SIN_COINCIDENCIA
                || duplicado.getEstado() == EstadoPosibleDuplicado.DUPLICADO_CONFIRMADO)
                && datosComparacionModificados) {
            duplicado.reabrir(coincidencia, ahora);
            posibleDuplicadoRepository.save(duplicado);
            revisionRepository.save(new RevisionPosibleDuplicado(
                    duplicado, TipoEventoDuplicado.REABIERTO_POR_CAMBIO_DE_DATOS, ACTOR_SISTEMA, ahora));
        } else if (duplicado.getEstado() == EstadoPosibleDuplicado.PENDIENTE_REVISION) {
            duplicado.actualizarCoincidencia(coincidencia, ahora);
            posibleDuplicadoRepository.save(duplicado);
        }
    }

    private Long otraFamiliaId(PosibleDuplicado duplicado, Long familiaId) {
        return duplicado.getFamiliaMenor().getId().equals(familiaId)
                ? duplicado.getFamiliaMayor().getId()
                : duplicado.getFamiliaMenor().getId();
    }

    private PosibleDuplicadoVista vista(PosibleDuplicado duplicado, List<RevisionVista> historial) {
        Familia menor = duplicado.getFamiliaMenor();
        Familia mayor = duplicado.getFamiliaMayor();
        List<CoincidenciaFamilia.SenalCoincidencia> senales = Arrays.stream(
                        duplicado.getSenalesCoincidentes().split(","))
                .map(CoincidenciaFamilia.SenalCoincidencia::valueOf)
                .toList();
        return new PosibleDuplicadoVista(
                duplicado.getId(),
                duplicado.getEstado(),
                duplicado.getDetectadoAt(),
                duplicado.getActualizadoAt(),
                senales,
                familiaVista(menor),
                familiaVista(mayor),
                historial);
    }

    private FamiliaVista familiaVista(Familia familia) {
        return new FamiliaVista(
                familia.getId(),
                familia.getNombreRegistrante(),
                familia.getTelefonoContacto(),
                familia.getBarrio(),
                familia.getDireccion(),
                familia.getEstado().getEtiqueta());
    }

    public record PosibleDuplicadoVista(
            Long id,
            EstadoPosibleDuplicado estado,
            Instant detectadoAt,
            Instant actualizadoAt,
            List<CoincidenciaFamilia.SenalCoincidencia> senales,
            FamiliaVista familiaMenor,
            FamiliaVista familiaMayor,
            List<RevisionVista> historial) {
        public PosibleDuplicadoVista {
            senales = List.copyOf(senales);
            historial = List.copyOf(historial);
        }
    }

    public record FamiliaVista(
            Long id,
            String nombreRegistrante,
            String telefonoContacto,
            String barrio,
            String direccion,
            String estado) {
    }

    public record RevisionVista(TipoEventoDuplicado evento, String actorCorreo, Instant fechaHora) {
    }
}
