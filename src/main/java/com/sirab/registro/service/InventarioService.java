package com.sirab.registro.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sirab.registro.ArticuloAyuda;
import com.sirab.registro.ArticuloAyudaRepository;
import com.sirab.registro.LoteInventario;
import com.sirab.registro.LoteInventarioRepository;
import com.sirab.registro.MovimientoInventario;
import com.sirab.registro.MovimientoInventarioRepository;
import com.sirab.registro.Necesidad;
import com.sirab.registro.TipoMovimientoInventario;

@Service
public class InventarioService {

    private final LoteInventarioRepository loteRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final ArticuloAyudaRepository articuloRepository;

    public InventarioService(
            LoteInventarioRepository loteRepository,
            MovimientoInventarioRepository movimientoRepository,
            ArticuloAyudaRepository articuloRepository) {
        this.loteRepository = loteRepository;
        this.movimientoRepository = movimientoRepository;
        this.articuloRepository = articuloRepository;
    }

    @Transactional
    public LoteInventario ingresarLote(RegistrarLoteCommand command, String usuarioCorreo) {
        if (usuarioCorreo == null || usuarioCorreo.isBlank()) {
            throw new IllegalArgumentException("El usuario que registra el lote es obligatorio.");
        }

        ArticuloAyuda articulo = articuloRepository.findById(command.articuloId())
                .orElseThrow(() -> new IllegalArgumentException("El artículo de ayuda especificado no existe."));

        if (!articulo.isActivo()) {
            throw new IllegalStateException("No se pueden registrar lotes para un artículo inactivo.");
        }

        String codigoLote = command.codigoLote() != null ? command.codigoLote().trim() : null;
        if (articulo.getTipoAyuda() == Necesidad.MEDICAMENTOS) {
            if (codigoLote == null || codigoLote.isBlank()) {
                throw new IllegalArgumentException("El código de lote es obligatorio para medicamentos.");
            }
            if (command.fechaVencimiento() == null) {
                throw new IllegalArgumentException("La fecha de vencimiento es obligatoria para medicamentos.");
            }
            if (command.fechaVencimiento().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("No se pueden registrar lotes vencidos para medicamentos.");
            }
        }

        if (codigoLote != null && !codigoLote.isBlank()) {
            Optional<LoteInventario> existente = loteRepository.findByArticulo_IdAndCodigoLote(articulo.getId(), codigoLote);
            if (existente.isPresent()) {
                throw new IllegalStateException("Ya existe un lote con el código especificado para este artículo.");
            }
        }

        Instant ahora = Instant.now();
        LoteInventario lote = new LoteInventario(
                articulo,
                codigoLote,
                command.fechaVencimiento(),
                articulo.getUnidad(),
                command.saldoInicial(),
                ahora,
                command.origen().trim(),
                usuarioCorreo.trim());

        lote = loteRepository.save(lote);

        MovimientoInventario movimiento = new MovimientoInventario(
                lote,
                null,
                TipoMovimientoInventario.INGRESO,
                command.saldoInicial(),
                ahora,
                usuarioCorreo.trim(),
                command.observacion() != null ? command.observacion().trim() : "Ingreso inicial de lote");

        movimientoRepository.save(movimiento);

        return lote;
    }

    @Transactional
    public LoteInventario ajustarInventario(
            Long loteId,
            BigDecimal cantidadAjuste,
            TipoMovimientoInventario tipoAjuste,
            String usuarioCorreo,
            String observacion) {
        if (loteId == null) {
            throw new IllegalArgumentException("El ID del lote es obligatorio.");
        }
        if (cantidadAjuste == null || cantidadAjuste.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad de ajuste debe ser mayor a cero.");
        }
        if (tipoAjuste != TipoMovimientoInventario.AJUSTE_POSITIVO && tipoAjuste != TipoMovimientoInventario.AJUSTE_NEGATIVO) {
            throw new IllegalArgumentException("El tipo de movimiento para ajustes debe ser AJUSTE_POSITIVO o AJUSTE_NEGATIVO.");
        }
        if (usuarioCorreo == null || usuarioCorreo.isBlank()) {
            throw new IllegalArgumentException("El usuario que realiza el ajuste es obligatorio.");
        }

        LoteInventario lote = loteRepository.findByIdConBloqueo(loteId)
                .orElseThrow(() -> new IllegalArgumentException("El lote especificado no existe."));

        BigDecimal nuevoSaldo;
        if (tipoAjuste == TipoMovimientoInventario.AJUSTE_POSITIVO) {
            nuevoSaldo = lote.getSaldoDisponible().add(cantidadAjuste);
        } else {
            if (lote.getSaldoDisponible().compareTo(cantidadAjuste) < 0) {
                throw new IllegalStateException("Saldo insuficiente en el lote para aplicar el ajuste negativo.");
            }
            nuevoSaldo = lote.getSaldoDisponible().subtract(cantidadAjuste);
        }

        lote.actualizarSaldoDisponible(nuevoSaldo);
        lote = loteRepository.save(lote);

        Instant ahora = Instant.now();
        MovimientoInventario movimiento = new MovimientoInventario(
                lote,
                null,
                tipoAjuste,
                cantidadAjuste,
                ahora,
                usuarioCorreo.trim(),
                observacion != null ? observacion.trim() : null);

        movimientoRepository.save(movimiento);

        return lote;
    }

    @Transactional(readOnly = true)
    public BigDecimal obtenerSaldoDisponiblePorArticulo(Long articuloId) {
        return loteRepository.sumarSaldoDisponiblePorArticulo(articuloId);
    }

    @Transactional(readOnly = true)
    public List<LoteInventario> obtenerLotesDisponiblesParaEntrega(Long articuloId) {
        return loteRepository.findByArticulo_IdAndSaldoDisponibleGreaterThanOrderByFechaVencimientoAscRecibidoAtAsc(
                articuloId, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public Optional<LoteInventario> obtenerLotePorId(Long loteId) {
        return loteRepository.findById(loteId);
    }

    @Transactional(readOnly = true)
    public List<LoteInventario> listarLotesPorArticulo(Long articuloId) {
        return loteRepository.findByArticulo_IdOrderByRecibidoAtDesc(articuloId);
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventario> listarMovimientosPorLote(Long loteId) {
        return movimientoRepository.findByLote_IdOrderByRegistradoAtDescIdDesc(loteId);
    }
}
