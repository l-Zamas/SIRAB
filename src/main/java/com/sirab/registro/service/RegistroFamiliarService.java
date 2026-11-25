package com.sirab.registro.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sirab.registro.ActorTipoSolicitud;
import com.sirab.registro.AsignacionAyuda;
import com.sirab.registro.AsignacionAyudaRepository;
import com.sirab.registro.CambioDatoVerificacion;
import com.sirab.registro.ConfirmacionPrestacionAlojamientoRepository;
import com.sirab.registro.EntregaAyuda;
import com.sirab.registro.EntregaAyudaRepository;
import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.EstadoSolicitudHistorial;
import com.sirab.registro.EstadoSolicitudHistorialRepository;
import com.sirab.registro.Familia;
import com.sirab.registro.FamiliaRepository;
import com.sirab.registro.Necesidad;
import com.sirab.registro.ResultadoVerificacion;
import com.sirab.registro.SituacionFamiliar;
import com.sirab.registro.VerificacionSolicitud;
import com.sirab.registro.VerificacionSolicitudRepository;
import com.sirab.duplicados.CoincidenciaFamilia;
import com.sirab.duplicados.DeteccionDuplicadosService;
import com.sirab.duplicados.IndiceDuplicadosInitializer;

@Service
public class RegistroFamiliarService {

    private static final String CONSENTIMIENTO_VERSION = "penpot-consentimiento-v1";

    private final FamiliaRepository familiaRepository;
    private final EstadoSolicitudHistorialRepository historialRepository;
    private final VerificacionSolicitudRepository verificacionRepository;
    private final EntregaAyudaRepository entregaRepository;
    private final DeteccionDuplicadosService deteccionDuplicadosService;
    private final CodigoConsultaService codigoConsultaService;
    private final EvidenciaFamiliaService evidenciaFamiliaService;
    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;
    @Autowired(required = false)
    private SeguimientoNecesidadService seguimientoNecesidadService;

    @Autowired(required = false)
    private ConfirmacionPrestacionAlojamientoRepository confirmacionPrestacionAlojamientoRepository;

    @Autowired(required = false)
    private AsignacionAyudaRepository asignacionAyudaRepository;

    public RegistroFamiliarService(
            FamiliaRepository familiaRepository,
            EstadoSolicitudHistorialRepository historialRepository,
            VerificacionSolicitudRepository verificacionRepository,
            EntregaAyudaRepository entregaRepository,
            DeteccionDuplicadosService deteccionDuplicadosService,
            CodigoConsultaService codigoConsultaService,
            EvidenciaFamiliaService evidenciaFamiliaService) {
        this.familiaRepository = familiaRepository;
        this.historialRepository = historialRepository;
        this.verificacionRepository = verificacionRepository;
        this.entregaRepository = entregaRepository;
        this.deteccionDuplicadosService = deteccionDuplicadosService;
        this.codigoConsultaService = codigoConsultaService;
        this.evidenciaFamiliaService = evidenciaFamiliaService;
    }

    void setConfirmacionPrestacionAlojamientoRepository(
            ConfirmacionPrestacionAlojamientoRepository confirmacionPrestacionAlojamientoRepository) {
        this.confirmacionPrestacionAlojamientoRepository = confirmacionPrestacionAlojamientoRepository;
    }

    void setAsignacionAyudaRepository(AsignacionAyudaRepository asignacionAyudaRepository) {
        this.asignacionAyudaRepository = asignacionAyudaRepository;
    }

    @Transactional
    public String registrar(RegistroFamiliarCommand command) {
        if (!command.consentimientoAceptado()) {
            throw new IllegalArgumentException("Se requiere consentimiento para registrar los datos.");
        }
        Set<Necesidad> necesidades = command.necesidades() == null ? Set.of() : command.necesidades();
        if (necesidades.contains(Necesidad.ALOJAMIENTO)
                && (command.evidenciaAlojamiento() == null || command.evidenciaAlojamiento().isBlank())) {
            throw new IllegalArgumentException("La evidencia de alojamiento es obligatoria.");
        }
        if (necesidades.contains(Necesidad.MEDICAMENTOS)
                && (command.evidenciaMedicamentos() == null || command.evidenciaMedicamentos().isBlank())) {
            throw new IllegalArgumentException("La evidencia de medicamentos es obligatoria.");
        }
        deteccionDuplicadosService.iniciarOperacion();

        Familia familia = new Familia();
        familia.setNombreRegistrante(command.nombreRegistrante());
        familia.setBarrio(command.barrio());
        familia.setDireccion(command.direccion());
        familia.setDireccionReferencia(command.direccionReferencia());
        familia.setNumeroIntegrantes(command.numeroIntegrantes());
        familia.setTelefonoContacto(command.telefonoContacto());
        familia.setSituaciones(command.situaciones() == null ? Set.of() : command.situaciones());
        familia.setNecesidades(necesidades);
        familia.setConsentimientoAt(command.consentimientoAt() == null ? Instant.now() : command.consentimientoAt());
        familia.setConsentimientoVersion(CONSENTIMIENTO_VERSION);
        String codigoConsulta = codigoConsultaService.generarCodigoUnico();
        familia.setCodigoConsultaHash(codigoConsultaService.hash(codigoConsulta));
        familia.setNumeroSolicitudPublica(siguienteNumeroSolicitud());
        IndiceDuplicadosInitializer.actualizarIndice(familia);
        Familia guardada = familiaRepository.save(familia);
        historialRepository.save(new EstadoSolicitudHistorial(
                guardada,
                null,
                EstadoRegistro.PENDIENTE,
                ActorTipoSolicitud.SISTEMA,
                null,
                Instant.now(),
                "Solicitud registrada por el flujo ciudadano."));
        deteccionDuplicadosService.detectarPara(guardada, false);
        if (seguimientoNecesidadService != null) {
            seguimientoNecesidadService.inicializar(guardada.getId(), "sistema@sirab.local", com.sirab.seguridad.entity.RolUsuario.PERSONAL_CAMPO);
        }
        if (command.evidenciaAlojamiento() != null) {
            evidenciaFamiliaService.asociar(guardada, command.evidenciaAlojamiento(), command.nombreEvidenciaAlojamiento(), command.tipoEvidenciaAlojamiento(), com.sirab.registro.TipoDocumentoPrivado.EVIDENCIA_ALOJAMIENTO);
        }
        if (command.evidenciaMedicamentos() != null) {
            evidenciaFamiliaService.asociar(guardada, command.evidenciaMedicamentos(), command.nombreEvidenciaMedicamentos(), command.tipoEvidenciaMedicamentos(), com.sirab.registro.TipoDocumentoPrivado.SOPORTE_MEDICO);
        }
        return codigoConsulta;
    }

    @Transactional(readOnly = true)
    public List<SolicitudResumen> listarSolicitudes() {
        return familiaRepository.findAll().stream()
                .map(this::resumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<SolicitudDetalle> obtenerSolicitud(Long familiaId) {
        return familiaRepository.findById(familiaId).map(familia -> {
            List<EstadoHistorialVista> estados = historialRepository
                    .findByFamilia_IdOrderByFechaHoraAsc(familiaId).stream()
                    .map(this::vistaEstado)
                    .toList();
            List<VerificacionVista> verificaciones = verificacionRepository
                    .findByFamilia_IdOrderByVerificadaAtDesc(familiaId).stream()
                    .map(this::vistaVerificacion)
                    .toList();
            List<EntregaVista> entregas = entregaRepository
                    .findByFamilia_IdOrderByFechaEntregaDescIdDesc(familiaId).stream()
                    .map(this::vistaEntrega)
                    .toList();
            return new SolicitudDetalle(
                    familia.getId(),
                    familia.getFechaCreacion(),
                    familia.getNombreRegistrante(),
                    familia.getNumeroIntegrantes(),
                    familia.getTelefonoContacto(),
                    familia.getBarrio(),
                    familia.getDireccion(),
                    familia.getDireccionReferencia(),
                    familia.getNumeroSolicitudPublica(),
                    familia.getSituaciones(),
                    familia.getNecesidades(),
                    familia.getEstado(),
                    familia.getConsentimientoAt(),
                    estados,
                    verificaciones,
                    entregas);
        });
    }

    @Transactional
    public boolean verificar(Long familiaId, String usuarioCorreo, SolicitudVerificacionCommand command) {
        return verificar(familiaId, usuarioCorreo, com.sirab.seguridad.entity.RolUsuario.PERSONAL_CAMPO, command);
    }

    @Transactional
    public boolean verificar(Long familiaId, String usuarioCorreo, com.sirab.seguridad.entity.RolUsuario rol,
            SolicitudVerificacionCommand command) {
        if (rol != com.sirab.seguridad.entity.RolUsuario.PERSONAL_CAMPO) {
            throw new SecurityException("Solo PERSONAL_CAMPO puede verificar solicitudes.");
        }
        deteccionDuplicadosService.iniciarOperacion();
        Familia familia = familiaRepository.findById(familiaId).orElse(null);
        if (familia == null) {
            return false;
        }
        exigirEstado(familia, EstadoRegistro.PENDIENTE);
        exigirActor(usuarioCorreo);

        String nombreRegistrante = command.nombreRegistrante().trim();
        String telefonoContacto = normalizarOpcional(command.telefonoContacto());
        String barrio = normalizarOpcional(command.barrio());
        String direccion = normalizarOpcional(command.direccion());
        String direccionReferencia = normalizarOpcional(command.direccionReferencia());
        boolean datosComparacionModificados = CoincidenciaFamilia.cambiaronDatosComparacion(
                familia, nombreRegistrante, telefonoContacto, barrio, direccion);
        Set<SituacionFamiliar> situaciones = command.situaciones() == null ? Set.of() : command.situaciones();
        Set<Necesidad> necesidades = command.necesidades() == null ? Set.of() : command.necesidades();
        List<CambioDatoVerificacion> cambios = new ArrayList<>();
        actualizarCampo(cambios, "Nombre de quien registra", familia.getNombreRegistrante(), nombreRegistrante);
        actualizarCampo(cambios, "Número de integrantes", familia.getNumeroIntegrantes(), command.numeroIntegrantes());
        actualizarCampo(cambios, "Teléfono de contacto", familia.getTelefonoContacto(), telefonoContacto);
        actualizarCampo(cambios, "Barrio", familia.getBarrio(), barrio);
        actualizarCampo(cambios, "Dirección", familia.getDireccion(), direccion);
        actualizarCampo(cambios, "Punto de referencia", familia.getDireccionReferencia(), direccionReferencia);
        actualizarCampo(cambios, "Situaciones familiares",
                valores(familia.getSituaciones()), valores(situaciones));
        actualizarCampo(cambios, "Necesidades", valores(familia.getNecesidades()), valores(necesidades));

        familia.setNombreRegistrante(nombreRegistrante);
        familia.setNumeroIntegrantes(command.numeroIntegrantes());
        familia.setTelefonoContacto(telefonoContacto);
        familia.setBarrio(barrio);
        familia.setDireccion(direccion);
        familia.setDireccionReferencia(direccionReferencia);
        familia.setSituaciones(situaciones);
        familia.setNecesidades(necesidades);
        familia.setEstado(EstadoRegistro.EN_ATENCION);
        IndiceDuplicadosInitializer.actualizarIndice(familia);
        familiaRepository.save(familia);
        deteccionDuplicadosService.detectarPara(familia, datosComparacionModificados);
        if (seguimientoNecesidadService != null) {
            seguimientoNecesidadService.inicializar(familiaId, usuarioCorreo, com.sirab.seguridad.entity.RolUsuario.PERSONAL_CAMPO);
            seguimientoNecesidadService.sincronizarVigencia(familiaId, necesidades, usuarioCorreo,
                    com.sirab.seguridad.entity.RolUsuario.PERSONAL_CAMPO, command.observacion());
        }

        Instant ahora = Instant.now();
        String observacion = normalizarObservacion(command.observacion());
        ResultadoVerificacion resultado = cambios.isEmpty()
                ? ResultadoVerificacion.CONFIRMADA
                : ResultadoVerificacion.CORREGIDA;
        verificacionRepository.save(new VerificacionSolicitud(
                familia, usuarioCorreo, ahora, resultado, observacion, cambios));
        historialRepository.save(new EstadoSolicitudHistorial(
                familia,
                EstadoRegistro.PENDIENTE,
                EstadoRegistro.EN_ATENCION,
                ActorTipoSolicitud.USUARIO,
                usuarioCorreo,
                ahora,
                observacion));
        return true;
    }

    /**
     * Legacy free-text deliveries are retained only for source compatibility with
     * historical callers; new effective deliveries must use EntregaAyudaService.
     */
    @Deprecated
    public boolean registrarEntrega(Long familiaId, String usuarioCorreo, RegistrarEntregaCommand command) {
        throw new IllegalStateException(
                "Las entregas efectivas deben registrarse mediante una asignación y un lote de inventario.");
    }

    @Transactional(readOnly = true)
    public boolean puedeMarcarAtendido(Long familiaId) {
        return familiaRepository.findById(familiaId)
                .map(familia -> familia.getEstado() == EstadoRegistro.EN_ATENCION)
                .orElse(false);
    }

    @Transactional
    public boolean marcarAtendido(Long familiaId, String usuarioCorreo, String observacion) {
        Familia familia = familiaRepository.findById(familiaId).orElse(null);
        if (familia == null) {
            return false;
        }
        exigirActor(usuarioCorreo);
        EstadoRegistro anterior = familia.getEstado();
        if (anterior == EstadoRegistro.ATENDIDO) {
            throw new IllegalStateException("La solicitud ya está cerrada y no puede cerrarse nuevamente.");
        }
        if (anterior != EstadoRegistro.EN_ATENCION) {
            throw new IllegalStateException("La solicitud debe estar en atención antes de cerrarse.");
        }
        Instant ahora = Instant.now();
        EstadoRegistro siguiente = EstadoRegistro.ATENDIDO;
        familia.setEstado(siguiente);
        familiaRepository.save(familia);
        historialRepository.save(new EstadoSolicitudHistorial(
                familia,
                anterior,
                siguiente,
                ActorTipoSolicitud.USUARIO,
                usuarioCorreo,
                ahora,
                siguiente == EstadoRegistro.ATENDIDO ? "Solicitud marcada como atendida." : "Solicitud reabierta."));
        return true;
    }

    private EstadoRegistro estadoAnteriorNoFinalizado(Long familiaId) {
        return historialRepository.findByFamilia_IdOrderByFechaHoraAsc(familiaId).stream()
                .map(EstadoSolicitudHistorial::getEstadoAnterior)
                .filter(estado -> estado != null && estado != EstadoRegistro.ATENDIDO)
                .reduce((primero, ultimo) -> ultimo)
                .orElse(EstadoRegistro.EN_ATENCION);
    }

    private boolean necesidadesSatisfechas(Familia familia, Long familiaId) {
        if (seguimientoNecesidadService != null) {
            List<com.sirab.registro.SeguimientoNecesidad> seguimientos = seguimientoNecesidadService.listar(familiaId);
            if (!seguimientos.isEmpty()) {
                return seguimientoNecesidadService.todasSatisfechas(familiaId);
            }
        }
        Set<Necesidad> necesidades = familia.getNecesidades();
        if (necesidades.isEmpty()) {
            return false;
        }
        return necesidades.stream().allMatch(necesidad -> esNecesidadSatisfecha(familiaId, necesidad));
    }

    private boolean seguimientosCompatiblesConCierre(Long familiaId) {
        return true;
    }

    private boolean esNecesidadSatisfecha(Long familiaId, Necesidad necesidad) {
        if (entregaRepository.existsByFamilia_IdAndTipoAyudaAndSatisfaceNecesidadCompletaTrue(familiaId, necesidad)) {
            return true;
        }

        if (necesidad == Necesidad.ALOJAMIENTO) {
            return confirmacionPrestacionAlojamientoRepository != null
                    && confirmacionPrestacionAlojamientoRepository.existsBySolicitud_Familia_Id(familiaId);
        }

        if (asignacionAyudaRepository != null) {
            List<AsignacionAyuda> asignaciones = asignacionAyudaRepository
                    .findByFamiliaIdOrderByAsignadaAtDescIdAsc(familiaId)
                    .stream()
                    .filter(a -> a.getTipoAyuda() == necesidad)
                    .toList();
            if (!asignaciones.isEmpty()) {
                return asignaciones.stream().allMatch(a -> {
                    BigDecimal entregado = entregaRepository.sumarCantidadEntregadaPorAsignacion(a.getId());
                    return entregado != null && entregado.compareTo(a.getCantidad()) >= 0;
                });
            }
        }

        return false;
    }

    private String siguienteNumeroSolicitud() {
        if (jdbcTemplate == null) {
            return null;
        }
        Long actual = jdbcTemplate.queryForObject(
                "SELECT valor FROM secuencia_numero_solicitud WHERE nombre = 'solicitud' FOR UPDATE",
                Long.class);
        if (actual == null) {
            throw new IllegalStateException("No se pudo generar el número público de solicitud.");
        }
        long siguiente = actual + 1;
        int actualizada = jdbcTemplate.update(
                "UPDATE secuencia_numero_solicitud SET valor = ? WHERE nombre = 'solicitud'",
                siguiente);
        if (actualizada != 1) {
            throw new IllegalStateException("No se pudo generar el número público de solicitud.");
        }
        return "SIRAB-" + String.format("%06d", siguiente);
    }

    private SolicitudResumen resumen(Familia familia) {
        return new SolicitudResumen(
                familia.getId(),
                familia.getNumeroSolicitudPublica(),
                familia.getFechaCreacion(),
                familia.getNombreRegistrante(),
                familia.getBarrio(),
                familia.getNumeroIntegrantes(),
                familia.getNecesidades(),
                familia.getSituaciones(),
                familia.getEstado());
    }

    private EstadoHistorialVista vistaEstado(EstadoSolicitudHistorial historial) {
        return new EstadoHistorialVista(
                historial.getEstadoAnterior(),
                historial.getEstadoNuevo(),
                historial.getActorTipo(),
                historial.getUsuarioCorreo(),
                historial.getFechaHora(),
                historial.getObservacion());
    }

    private VerificacionVista vistaVerificacion(VerificacionSolicitud verificacion) {
        return new VerificacionVista(
                verificacion.getVerificadaAt(),
                verificacion.getUsuarioCorreo(),
                verificacion.getResultado(),
                verificacion.getObservacion(),
                verificacion.getCambios());
    }

    private EntregaVista vistaEntrega(EntregaAyuda entrega) {
        return new EntregaVista(
                entrega.getTipoAyuda(),
                entrega.getCantidad(),
                entrega.getCantidadValor(),
                entrega.getUnidad(),
                entrega.getFechaEntrega(),
                entrega.getFechaHoraEntrega(),
                entrega.getRegistradaAt(),
                entrega.getUsuarioCorreo(),
                entrega.isSatisfaceNecesidadCompleta(),
                entrega.getObservacion(),
                entrega.getFechaHoraEntrega() != null);
    }

    private void exigirEstado(Familia familia, EstadoRegistro esperado) {
        if (familia.getEstado() != esperado) {
            throw new IllegalStateException("La solicitud ya no está en el estado requerido para esta operación.");
        }
    }

    private void exigirActor(String usuarioCorreo) {
        if (usuarioCorreo == null || usuarioCorreo.isBlank()) {
            throw new IllegalArgumentException("No se pudo identificar al usuario autenticado.");
        }
    }

    private void actualizarCampo(List<CambioDatoVerificacion> cambios, String campo, Object anterior, Object nuevo) {
        if (!Objects.equals(anterior, nuevo)) {
            cambios.add(new CambioDatoVerificacion(campo, texto(anterior), texto(nuevo)));
        }
    }

    private String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private String valores(Set<? extends Enum<?>> valores) {
        return valores.stream()
                .map(Enum::name)
                .sorted(Comparator.naturalOrder())
                .reduce((primero, segundo) -> primero + ", " + segundo)
                .orElse("");
    }

    private String normalizarOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private String normalizarObservacion(String valor) {
        String observacion = normalizarOpcional(valor);
        if (observacion != null && observacion.length() > 1000) {
            throw new IllegalArgumentException("La observación no puede superar 1000 caracteres.");
        }
        return observacion;
    }

    public record SolicitudResumen(
            Long id,
            String numeroSolicitudPublica,
            Instant fechaCreacion,
            String nombreRegistrante,
            String barrio,
            Integer numeroIntegrantes,
            Set<Necesidad> necesidades,
            Set<SituacionFamiliar> situaciones,
            EstadoRegistro estado) {
        public SolicitudResumen {
            necesidades = Set.copyOf(necesidades);
            situaciones = Set.copyOf(situaciones);
        }
    }

    public record SolicitudDetalle(
            Long id,
            Instant fechaCreacion,
            String nombreRegistrante,
            Integer numeroIntegrantes,
            String telefonoContacto,
            String barrio,
            String direccion,
            String direccionReferencia,
            String numeroSolicitudPublica,
            Set<SituacionFamiliar> situaciones,
            Set<Necesidad> necesidades,
            EstadoRegistro estado,
            Instant consentimientoAt,
            List<EstadoHistorialVista> historialEstados,
            List<VerificacionVista> verificaciones,
            List<EntregaVista> entregas) {
        public SolicitudDetalle {
            situaciones = Set.copyOf(situaciones);
            necesidades = Set.copyOf(necesidades);
            historialEstados = List.copyOf(historialEstados);
            verificaciones = List.copyOf(verificaciones);
            entregas = List.copyOf(entregas);
        }
    }

    public record EstadoHistorialVista(
            EstadoRegistro estadoAnterior,
            EstadoRegistro estadoNuevo,
            ActorTipoSolicitud actorTipo,
            String usuarioCorreo,
            Instant fechaHora,
            String observacion) {
    }

    public record VerificacionVista(
            Instant verificadaAt,
            String usuarioCorreo,
            ResultadoVerificacion resultado,
            String observacion,
            List<CambioDatoVerificacion> cambios) {
        public VerificacionVista {
            cambios = List.copyOf(cambios);
        }
    }

    public record EntregaVista(
            Necesidad tipoAyuda,
            String cantidad,
            BigDecimal cantidadValor,
            String unidad,
            java.time.LocalDate fechaEntrega,
            Instant fechaHoraEntrega,
            Instant registradaAt,
            String usuarioCorreo,
            boolean satisfaceNecesidadCompleta,
            String observacion,
            boolean efectiva) {
    }
}
