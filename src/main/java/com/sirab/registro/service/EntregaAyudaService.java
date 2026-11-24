package com.sirab.registro.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sirab.registro.AsignacionAyuda;
import com.sirab.registro.AsignacionAyudaRepository;
import com.sirab.registro.DecisionAyuda;
import com.sirab.registro.EntregaAyuda;
import com.sirab.registro.EntregaAyudaRepository;
import com.sirab.registro.ActorTipoSolicitud;
import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.EstadoSolicitudHistorial;
import com.sirab.registro.EstadoSolicitudHistorialRepository;
import com.sirab.registro.Familia;
import com.sirab.registro.LoteInventario;
import com.sirab.registro.LoteInventarioRepository;
import com.sirab.registro.MovimientoInventario;
import com.sirab.registro.MovimientoInventarioRepository;
import com.sirab.registro.Necesidad;
import com.sirab.registro.OrigenAsignacionAyuda;
import com.sirab.registro.TipoMovimientoInventario;

@Service
public class EntregaAyudaService {

    private final EntregaAyudaRepository entregaRepository;
    private final AsignacionAyudaRepository asignacionRepository;
    private final LoteInventarioRepository loteRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private EstadoSolicitudHistorialRepository historialRepository;

    public EntregaAyudaService(
            EntregaAyudaRepository entregaRepository,
            AsignacionAyudaRepository asignacionRepository,
            LoteInventarioRepository loteRepository,
            MovimientoInventarioRepository movimientoRepository) {
        this.entregaRepository = entregaRepository;
        this.asignacionRepository = asignacionRepository;
        this.loteRepository = loteRepository;
        this.movimientoRepository = movimientoRepository;
    }

    @Transactional
    public EntregaAyuda registrarEntrega(RegistrarEntregaAsignacionCommand command, String usuarioCorreo) {
        if (usuarioCorreo == null || usuarioCorreo.isBlank()) {
            throw new IllegalArgumentException("El usuario que realiza la entrega es obligatorio.");
        }

        AsignacionAyuda asignacion = asignacionRepository.findByIdConBloqueo(command.asignacionId())
                .orElseThrow(() -> new IllegalArgumentException("La asignación especificada no existe."));

        Familia familia = asignacion.getFamilia();
        if (familia.getEstado() != EstadoRegistro.EN_ATENCION) {
            throw new IllegalStateException("Solo se pueden registrar entregas para familias en estado EN_ATENCION.");
        }

        BigDecimal totalEntregado = entregaRepository.sumarCantidadEntregadaPorAsignacion(asignacion.getId());
        BigDecimal saldoPendiente = asignacion.getCantidad().subtract(totalEntregado);

        if (command.cantidad().compareTo(saldoPendiente) > 0) {
            throw new IllegalArgumentException(String.format(
                    "La cantidad solicitada (%s %s) supera el saldo pendiente (%s %s) de la asignación.",
                    command.cantidad(), asignacion.getUnidad(), saldoPendiente, asignacion.getUnidad()));
        }

        Long loteId = command.loteId();
        if (loteId == null) {
            List<LoteInventario> lotesDisponibles = loteRepository
                    .findByArticulo_IdAndSaldoDisponibleGreaterThanOrderByFechaVencimientoAscRecibidoAtAsc(
                            asignacion.getArticulo().getId(), BigDecimal.ZERO);
            if (lotesDisponibles.isEmpty()) {
                throw new IllegalStateException("No hay inventario disponible para el artículo " + asignacion.getArticulo().getNombre());
            }
            loteId = lotesDisponibles.get(0).getId();
        }

        LoteInventario lote = loteRepository.findByIdConBloqueo(loteId)
                .orElseThrow(() -> new IllegalArgumentException("El lote especificado no existe."));

        if (!java.util.Objects.equals(lote.getArticulo().getId(), asignacion.getArticulo().getId())) {
            throw new IllegalArgumentException("El lote seleccionado no corresponde al artículo asignado.");
        }

        if (lote.getSaldoDisponible().compareTo(command.cantidad()) < 0) {
            throw new IllegalStateException(String.format(
                    "El lote no dispone de existencias suficientes (disponible: %s, requerido: %s).",
                    lote.getSaldoDisponible(), command.cantidad()));
        }

        if (lote.getFechaVencimiento() != null && lote.getFechaVencimiento().isBefore(LocalDate.now())) {
            throw new IllegalStateException("No se pueden despachar entregas desde un lote vencido.");
        }

        if (asignacion.getArticulo().getTipoAyuda() == Necesidad.MEDICAMENTOS) {
            if (asignacion.getOrigen() == OrigenAsignacionAyuda.REVISION_SANITARIA) {
                if (asignacion.getRevisionSanitaria() == null
                        || asignacion.getRevisionSanitaria().getDecision() != DecisionAyuda.APROBADA) {
                    throw new IllegalStateException("La entrega de medicamentos requiere una revisión sanitaria externa aprobada.");
                }
            }
        }

        BigDecimal nuevoTotalEntregado = totalEntregado.add(command.cantidad());
        boolean asignacionCumplida = nuevoTotalEntregado.compareTo(asignacion.getCantidad()) >= 0;

        boolean satisfaceNecesidad = false;
        if (asignacionCumplida) {
            List<AsignacionAyuda> otrasAsignacionesMismoTipo = asignacionRepository
                    .findByFamiliaIdOrderByAsignadaAtDescIdAsc(familia.getId())
                    .stream()
                    .filter(a -> a.getTipoAyuda() == asignacion.getTipoAyuda() && !a.getId().equals(asignacion.getId()))
                    .toList();

            boolean todasLasOtrasCumplidas = otrasAsignacionesMismoTipo.stream().allMatch(a -> {
                BigDecimal entregado = entregaRepository.sumarCantidadEntregadaPorAsignacion(a.getId());
                return entregado.compareTo(a.getCantidad()) >= 0;
            });

            satisfaceNecesidad = todasLasOtrasCumplidas;
        }

        Instant fechaHora = command.fechaHoraEntrega() != null ? command.fechaHoraEntrega() : Instant.now();
        EntregaAyuda entrega = new EntregaAyuda(
                familia,
                asignacion.getTipoAyuda(),
                asignacion,
                command.cantidad(),
                asignacion.getUnidad(),
                fechaHora,
                Instant.now(),
                usuarioCorreo.trim(),
                satisfaceNecesidad,
                command.observacion() != null ? command.observacion().trim() : null);

        entrega = entregaRepository.save(entrega);

        lote.actualizarSaldoDisponible(lote.getSaldoDisponible().subtract(command.cantidad()));
        loteRepository.save(lote);

        MovimientoInventario movimiento = new MovimientoInventario(
                lote,
                entrega,
                TipoMovimientoInventario.SALIDA_ENTREGA,
                command.cantidad(),
                Instant.now(),
                usuarioCorreo.trim(),
                command.observacion() != null ? command.observacion().trim() : "Salida por entrega de asignación #" + asignacion.getId());

        movimientoRepository.save(movimiento);

        return entrega;
    }

    @Transactional(readOnly = true)
    public BigDecimal obtenerCantidadEntregada(Long asignacionId) {
        return entregaRepository.sumarCantidadEntregadaPorAsignacion(asignacionId);
    }

    @Transactional(readOnly = true)
    public BigDecimal obtenerSaldoPendiente(Long asignacionId) {
        AsignacionAyuda asignacion = asignacionRepository.findById(asignacionId)
                .orElseThrow(() -> new IllegalArgumentException("La asignación especificada no existe."));
        BigDecimal entregado = entregaRepository.sumarCantidadEntregadaPorAsignacion(asignacionId);
        return asignacion.getCantidad().subtract(entregado);
    }

    @Transactional(readOnly = true)
    public List<EntregaAyuda> listarEntregasPorAsignacion(Long asignacionId) {
        return entregaRepository.findByAsignacion_IdOrderByFechaHoraEntregaAscIdAsc(asignacionId);
    }

    @Transactional(readOnly = true)
    public List<EntregaAyuda> listarEntregasPorFamilia(Long familiaId) {
        return entregaRepository.findByFamilia_IdAndFechaHoraEntregaIsNotNullOrderByFechaHoraEntregaDescIdDesc(familiaId);
    }
}
