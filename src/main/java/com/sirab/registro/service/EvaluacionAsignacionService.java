package com.sirab.registro.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sirab.registro.ArticuloAyuda;
import com.sirab.registro.ArticuloAyudaRepository;
import com.sirab.registro.AsignacionAyuda;
import com.sirab.registro.AsignacionAyudaRepository;
import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.EvaluacionAsignacionAyuda;
import com.sirab.registro.EvaluacionAsignacionAyudaRepository;
import com.sirab.registro.Familia;
import com.sirab.registro.FamiliaRepository;
import com.sirab.registro.Necesidad;
import com.sirab.registro.OrigenAsignacionAyuda;
import com.sirab.registro.ReglaAsignacionAyuda;
import com.sirab.registro.ReglaAsignacionAyudaRepository;
import com.sirab.registro.TipoCriterioAsignacion;

@Service
@Transactional
public class EvaluacionAsignacionService {

    private final EvaluacionAsignacionAyudaRepository evaluacionRepository;
    private final AsignacionAyudaRepository asignacionRepository;
    private final FamiliaRepository familiaRepository;
    private final ArticuloAyudaRepository articuloRepository;
    private final ReglaAsignacionAyudaRepository reglaRepository;

    public EvaluacionAsignacionService(
            EvaluacionAsignacionAyudaRepository evaluacionRepository,
            AsignacionAyudaRepository asignacionRepository,
            FamiliaRepository familiaRepository,
            ArticuloAyudaRepository articuloRepository,
            ReglaAsignacionAyudaRepository reglaRepository) {
        this.evaluacionRepository = evaluacionRepository;
        this.asignacionRepository = asignacionRepository;
        this.familiaRepository = familiaRepository;
        this.articuloRepository = articuloRepository;
        this.reglaRepository = reglaRepository;
    }

    public ResultadoEvaluacion evaluarNecesidades(EvaluarNecesidadesCommand command) {
        Objects.requireNonNull(command, "El comando de evaluación no puede ser nulo.");
        exigirActor(command.usuarioCorreo());

        if (command.familiaId() == null) {
            throw new IllegalArgumentException("El ID de la familia es obligatorio.");
        }

        String claveIdempotencia = normalizarClaveIdempotencia(command.claveIdempotencia());

        // 1. Verificación previa de idempotencia (Fast path)
        Optional<EvaluacionAsignacionAyuda> existenteOpt =
                evaluacionRepository.findByClaveIdempotencia(claveIdempotencia);
        if (existenteOpt.isPresent()) {
            EvaluacionAsignacionAyuda existente = existenteOpt.get();
            if (!existente.getFamilia().getId().equals(command.familiaId())) {
                throw new IllegalStateException(
                        "La clave de idempotencia ya fue utilizada para otra familia.");
            }
            List<AsignacionAyuda> asignacionesExistentes =
                    asignacionRepository.findByEvaluacionIdOrderByIdAsc(existente.getId());
            return new ResultadoEvaluacion(
                    existente,
                    asignacionesExistentes,
                    existente.getNecesidades(),
                    Set.of(),
                    true);
        }

        // 2. Bloqueo pesimista sobre la familia para serializar concurrentes
        Familia familia = familiaRepository.findByIdConBloqueo(command.familiaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la familia con ID: " + command.familiaId()));

        // 3. Re-verificación bajo bloqueo
        Optional<EvaluacionAsignacionAyuda> recheck =
                evaluacionRepository.findByClaveIdempotencia(claveIdempotencia);
        if (recheck.isPresent()) {
            EvaluacionAsignacionAyuda existente = recheck.get();
            List<AsignacionAyuda> asignacionesExistentes =
                    asignacionRepository.findByEvaluacionIdOrderByIdAsc(existente.getId());
            return new ResultadoEvaluacion(
                    existente,
                    asignacionesExistentes,
                    existente.getNecesidades(),
                    Set.of(),
                    true);
        }

        // 4. Validar estado de la familia
        if (familia.getEstado() != EstadoRegistro.EN_ATENCION) {
            throw new IllegalStateException(
                    "Solo se pueden evaluar solicitudes en estado EN_ATENCION. Estado actual: "
                            + familia.getEstado());
        }

        Set<Necesidad> necesidades = familia.getNecesidades();
        if (necesidades.isEmpty()) {
            throw new IllegalStateException("La familia no tiene necesidades registradas para evaluar.");
        }

        LocalDate fechaEvaluacion = command.fechaEvaluacion() != null
                ? command.fechaEvaluacion()
                : LocalDate.now();

        int maxRevision = evaluacionRepository.findMaxRevisionByFamiliaId(familia.getId());
        int nuevaRevision = maxRevision + 1;
        Instant ahora = Instant.now();
        String usuario = command.usuarioCorreo().trim();

        // 5. Crear el snapshot inmutable de evaluación
        EvaluacionAsignacionAyuda evaluacion = new EvaluacionAsignacionAyuda(
                familia,
                nuevaRevision,
                claveIdempotencia,
                ahora,
                usuario,
                necesidades,
                familia.getSituaciones());

        evaluacion = evaluacionRepository.save(evaluacion);

        // 6. Evaluar reglas vigentes y generar asignaciones
        List<AsignacionAyuda> asignaciones = new ArrayList<>();
        Set<Necesidad> necesidadesSinReglas = new HashSet<>();

        for (Necesidad necesidad : necesidades) {
            // El alojamiento se atiende en el proceso administrativo independiente (V15)
            if (necesidad == Necesidad.ALOJAMIENTO) {
                continue;
            }

            List<ArticuloAyuda> articulos =
                    articuloRepository.findByTipoAyudaAndActivoTrueOrderByCodigoAsc(necesidad);
            if (articulos.isEmpty()) {
                necesidadesSinReglas.add(necesidad);
                continue;
            }

            boolean seAsignoParaNecesidad = false;

            for (ArticuloAyuda articulo : articulos) {
                List<ReglaAsignacionAyuda> reglasVigentes =
                        reglaRepository.findVigentesByArticuloIdAndFecha(articulo.getId(), fechaEvaluacion);

                for (ReglaAsignacionAyuda regla : reglasVigentes) {
                    boolean aplica = false;
                    if (regla.getTipoCriterio() == TipoCriterioAsignacion.BASE) {
                        aplica = true;
                    } else if (regla.getTipoCriterio() == TipoCriterioAsignacion.SITUACION_FAMILIAR) {
                        aplica = familia.getSituaciones().contains(regla.getSituacion());
                    }

                    if (aplica) {
                        String claveAsignacion = generarClaveIdempotenciaAsignacion(
                                claveIdempotencia, regla.getId());

                        AsignacionAyuda asignacion = new AsignacionAyuda(
                                familia,
                                articulo,
                                evaluacion,
                                regla,
                                null,
                                OrigenAsignacionAyuda.REGLA,
                                claveAsignacion,
                                regla.getCantidad(),
                                regla.getUnidad(),
                                ahora,
                                usuario);

                        asignaciones.add(asignacion);
                        seAsignoParaNecesidad = true;
                    }
                }
            }

            if (!seAsignoParaNecesidad) {
                necesidadesSinReglas.add(necesidad);
            }
        }

        if (!asignaciones.isEmpty()) {
            asignaciones = asignacionRepository.saveAll(asignaciones);
        }

        return new ResultadoEvaluacion(
                evaluacion,
                asignaciones,
                necesidades,
                necesidadesSinReglas,
                false);
    }

    @Transactional(readOnly = true)
    public Optional<EvaluacionAsignacionAyuda> obtenerEvaluacionPorId(Long evaluacionId) {
        if (evaluacionId == null) {
            return Optional.empty();
        }
        return evaluacionRepository.findById(evaluacionId);
    }

    @Transactional(readOnly = true)
    public Optional<EvaluacionAsignacionAyuda> obtenerEvaluacionPorClaveIdempotencia(String clave) {
        if (clave == null || clave.isBlank()) {
            return Optional.empty();
        }
        return evaluacionRepository.findByClaveIdempotencia(clave.trim());
    }

    @Transactional(readOnly = true)
    public List<EvaluacionAsignacionAyuda> listarEvaluacionesFamilia(Long familiaId) {
        if (familiaId == null) {
            throw new IllegalArgumentException("El ID de la familia es obligatorio.");
        }
        return evaluacionRepository.findByFamiliaIdOrderByRevisionDesc(familiaId);
    }

    @Transactional(readOnly = true)
    public List<AsignacionAyuda> listarAsignacionesEvaluacion(Long evaluacionId) {
        if (evaluacionId == null) {
            throw new IllegalArgumentException("El ID de la evaluación es obligatorio.");
        }
        return asignacionRepository.findByEvaluacionIdOrderByIdAsc(evaluacionId);
    }

    @Transactional(readOnly = true)
    public List<AsignacionAyuda> listarAsignacionesFamilia(Long familiaId) {
        if (familiaId == null) {
            throw new IllegalArgumentException("El ID de la familia es obligatorio.");
        }
        return asignacionRepository.findByFamiliaIdOrderByAsignadaAtDescIdAsc(familiaId);
    }

    private String generarClaveIdempotenciaAsignacion(String claveEvaluacion, Long reglaId) {
        String base = "asig:" + claveEvaluacion + ":" + reglaId;
        if (base.length() > 120) {
            return base.substring(0, 120);
        }
        return base;
    }

    private String normalizarClaveIdempotencia(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("La clave de idempotencia es obligatoria.");
        }
        String normalizada = clave.trim();
        if (normalizada.length() > 100) {
            throw new IllegalArgumentException(
                    "La clave de idempotencia no puede superar 100 caracteres.");
        }
        return normalizada;
    }

    private void exigirActor(String usuarioCorreo) {
        if (usuarioCorreo == null || usuarioCorreo.isBlank()) {
            throw new IllegalArgumentException("No se pudo identificar al usuario autenticado.");
        }
    }
}
