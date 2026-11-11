package com.sirab.registro.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sirab.registro.ActorTipoSolicitud;
import com.sirab.registro.CambioDatoVerificacion;
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
import com.sirab.registro.duplicados.CoincidenciaFamilia;
import com.sirab.registro.duplicados.DeteccionDuplicadosService;
import com.sirab.registro.duplicados.IndiceDuplicadosInitializer;

@Service
public class RegistroFamiliarService {

    private static final String CONSENTIMIENTO_VERSION = "penpot-consentimiento-v1";

    private final FamiliaRepository familiaRepository;
    private final EstadoSolicitudHistorialRepository historialRepository;
    private final VerificacionSolicitudRepository verificacionRepository;
    private final EntregaAyudaRepository entregaRepository;
    private final DeteccionDuplicadosService deteccionDuplicadosService;

    public RegistroFamiliarService(
            FamiliaRepository familiaRepository,
            EstadoSolicitudHistorialRepository historialRepository,
            VerificacionSolicitudRepository verificacionRepository,
            EntregaAyudaRepository entregaRepository,
            DeteccionDuplicadosService deteccionDuplicadosService) {
        this.familiaRepository = familiaRepository;
        this.historialRepository = historialRepository;
        this.verificacionRepository = verificacionRepository;
        this.entregaRepository = entregaRepository;
        this.deteccionDuplicadosService = deteccionDuplicadosService;
    }

    @Transactional
    public void registrar(RegistroFamiliarCommand command) {
        if (!command.consentimientoAceptado()) {
            throw new IllegalArgumentException("Se requiere consentimiento para registrar los datos.");
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
        familia.setNecesidades(command.necesidades() == null ? Set.of() : command.necesidades());
        familia.setConsentimientoAt(command.consentimientoAt() == null ? Instant.now() : command.consentimientoAt());
        familia.setConsentimientoVersion(CONSENTIMIENTO_VERSION);
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
        familia.setEstado(EstadoRegistro.VERIFICADO);
        IndiceDuplicadosInitializer.actualizarIndice(familia);
        familiaRepository.save(familia);
        deteccionDuplicadosService.detectarPara(familia, datosComparacionModificados);

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
                EstadoRegistro.VERIFICADO,
                ActorTipoSolicitud.USUARIO,
                usuarioCorreo,
                ahora,
                observacion));
        return true;
    }

    @Transactional
    public boolean registrarEntrega(Long familiaId, String usuarioCorreo, RegistrarEntregaCommand command) {
        Familia familia = familiaRepository.findById(familiaId).orElse(null);
        if (familia == null) {
            return false;
        }
        exigirEstado(familia, EstadoRegistro.VERIFICADO);
        exigirActor(usuarioCorreo);
        if (command.tipoAyuda() == null || !familia.getNecesidades().contains(command.tipoAyuda())) {
            throw new IllegalStateException("La necesidad seleccionada no está registrada en esta solicitud.");
        }
        if (command.cantidad() == null || command.cantidad().isBlank()) {
            throw new IllegalArgumentException("La cantidad entregada es obligatoria.");
        }
        if (command.fechaEntrega() == null) {
            throw new IllegalArgumentException("La fecha de entrega es obligatoria.");
        }

        entregaRepository.save(new EntregaAyuda(
                familia,
                command.tipoAyuda(),
                command.cantidad().trim(),
                command.fechaEntrega(),
                Instant.now(),
                usuarioCorreo,
                command.satisfaceNecesidadCompleta(),
                normalizarObservacion(command.observacion())));
        return true;
    }

    @Transactional(readOnly = true)
    public boolean puedeMarcarAtendido(Long familiaId) {
        return familiaRepository.findById(familiaId)
                .filter(familia -> familia.getEstado() == EstadoRegistro.VERIFICADO)
                .map(familia -> necesidadesSatisfechas(familia, familiaId))
                .orElse(false);
    }

    @Transactional
    public boolean marcarAtendido(Long familiaId, String usuarioCorreo, String observacion) {
        Familia familia = familiaRepository.findById(familiaId).orElse(null);
        if (familia == null) {
            return false;
        }
        exigirEstado(familia, EstadoRegistro.VERIFICADO);
        exigirActor(usuarioCorreo);
        if (!necesidadesSatisfechas(familia, familiaId)) {
            throw new IllegalStateException(
                    "No se puede marcar como atendida: falta registrar la satisfacción completa de cada necesidad.");
        }

        Instant ahora = Instant.now();
        String observacionCierre = normalizarObservacion(observacion);
        familia.setEstado(EstadoRegistro.ATENDIDO);
        familiaRepository.save(familia);
        historialRepository.save(new EstadoSolicitudHistorial(
                familia,
                EstadoRegistro.VERIFICADO,
                EstadoRegistro.ATENDIDO,
                ActorTipoSolicitud.USUARIO,
                usuarioCorreo,
                ahora,
                observacionCierre));
        return true;
    }

    private boolean necesidadesSatisfechas(Familia familia, Long familiaId) {
        Set<Necesidad> necesidades = familia.getNecesidades();
        return !necesidades.isEmpty()
                && necesidades.stream().allMatch(necesidad ->
                        entregaRepository.existsByFamilia_IdAndTipoAyudaAndSatisfaceNecesidadCompletaTrue(
                                familiaId, necesidad));
    }

    private SolicitudResumen resumen(Familia familia) {
        return new SolicitudResumen(
                familia.getId(),
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
                entrega.getFechaEntrega(),
                entrega.getRegistradaAt(),
                entrega.getUsuarioCorreo(),
                entrega.isSatisfaceNecesidadCompleta(),
                entrega.getObservacion());
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
            java.time.LocalDate fechaEntrega,
            Instant registradaAt,
            String usuarioCorreo,
            boolean satisfaceNecesidadCompleta,
            String observacion) {
    }
}
