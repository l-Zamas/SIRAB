package com.sirab.registro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sirab.registro.ArticuloAyuda;
import com.sirab.registro.ArticuloAyudaRepository;
import com.sirab.registro.LoteInventario;
import com.sirab.registro.LoteInventarioRepository;
import com.sirab.registro.MovimientoInventario;
import com.sirab.registro.MovimientoInventarioRepository;
import com.sirab.registro.Necesidad;
import com.sirab.registro.TipoMovimientoInventario;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTests {

    @Mock
    private LoteInventarioRepository loteRepository;

    @Mock
    private MovimientoInventarioRepository movimientoRepository;

    @Mock
    private ArticuloAyudaRepository articuloRepository;

    @InjectMocks
    private InventarioService inventarioService;

    private ArticuloAyuda articuloAgua;
    private ArticuloAyuda articuloMedicamento;

    @BeforeEach
    void setUp() {
        articuloAgua = new ArticuloAyuda("AGUA-01", "Bidón de Agua", Necesidad.AGUA, "Litros", true, java.time.Instant.now(), "admin@sirab.local");
        articuloMedicamento = new ArticuloAyuda("MED-01", "Amoxicilina", Necesidad.MEDICAMENTOS, "Cajas", true, java.time.Instant.now(), "admin@sirab.local");
        org.springframework.test.util.ReflectionTestUtils.setField(articuloAgua, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(articuloMedicamento, "id", 2L);
    }

    @Test
    void registraLoteExitosamenteYGeneraMovimientoDeIngreso() {
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(articuloAgua));
        when(loteRepository.save(any(LoteInventario.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrarLoteCommand command = new RegistrarLoteCommand(
                1L,
                "LOTE-AGUA-001",
                null,
                new BigDecimal("500.00"),
                "Donación Cruz Roja",
                "Recepción inicial");

        LoteInventario lote = inventarioService.ingresarLote(command, "operador@sirab.local");

        assertNotNull(lote);
        assertEquals(articuloAgua, lote.getArticulo());
        assertEquals("LOTE-AGUA-001", lote.getCodigoLote());
        assertEquals(new BigDecimal("500.00"), lote.getSaldoDisponible());
        assertEquals("operador@sirab.local", lote.getRegistradoPor());

        ArgumentCaptor<MovimientoInventario> movCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoRepository).save(movCaptor.capture());
        MovimientoInventario mov = movCaptor.getValue();
        assertEquals(TipoMovimientoInventario.INGRESO, mov.getTipo());
        assertEquals(new BigDecimal("500.00"), mov.getCantidad());
        assertEquals("operador@sirab.local", mov.getUsuarioCorreo());
    }

    @Test
    void rechazaLoteSiArticuloEstaInactivo() {
        articuloAgua.desactivar();
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(articuloAgua));

        RegistrarLoteCommand command = new RegistrarLoteCommand(
                1L,
                "LOTE-001",
                null,
                new BigDecimal("100.00"),
                "Proveedor",
                null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> inventarioService.ingresarLote(command, "operador@sirab.local"));
        assertTrue(ex.getMessage().contains("inactivo"));
    }

    @Test
    void exigeCodigoYFechaVencimientoFuturaParaMedicamentos() {
        when(articuloRepository.findById(2L)).thenReturn(Optional.of(articuloMedicamento));

        // Sin código de lote
        RegistrarLoteCommand sinCodigo = new RegistrarLoteCommand(
                2L,
                "",
                LocalDate.now().plusMonths(6),
                new BigDecimal("100.00"),
                "Farmacia Central",
                null);

        assertThrows(IllegalArgumentException.class,
                () -> inventarioService.ingresarLote(sinCodigo, "operador@sirab.local"));

        // Sin fecha de vencimiento
        RegistrarLoteCommand sinFecha = new RegistrarLoteCommand(
                2L,
                "MED-LOT-1",
                null,
                new BigDecimal("100.00"),
                "Farmacia Central",
                null);

        assertThrows(IllegalArgumentException.class,
                () -> inventarioService.ingresarLote(sinFecha, "operador@sirab.local"));

        // Fecha de vencimiento en el pasado
        RegistrarLoteCommand vencido = new RegistrarLoteCommand(
                2L,
                "MED-LOT-1",
                LocalDate.now().minusDays(1),
                new BigDecimal("100.00"),
                "Farmacia Central",
                null);

        assertThrows(IllegalArgumentException.class,
                () -> inventarioService.ingresarLote(vencido, "operador@sirab.local"));
    }

    @Test
    void rechazaCodigoLoteDuplicadoParaMismoArticulo() {
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(articuloAgua));
        LoteInventario loteExistente = new LoteInventario(
                articuloAgua, "LOTE-DUP", null, "Litros", BigDecimal.TEN, java.time.Instant.now(), "Origen", "user");
        when(loteRepository.findByArticulo_IdAndCodigoLote(any(), any())).thenReturn(Optional.of(loteExistente));

        RegistrarLoteCommand command = new RegistrarLoteCommand(
                1L, "LOTE-DUP", null, new BigDecimal("100.00"), "Donación", null);

        assertThrows(IllegalStateException.class,
                () -> inventarioService.ingresarLote(command, "operador@sirab.local"));
    }

    @Test
    void ajustePositivoIncrementaSaldoYRegistraMovimiento() {
        LoteInventario lote = new LoteInventario(
                articuloAgua, "LOTE-1", null, "Litros", new BigDecimal("100.00"), java.time.Instant.now(), "Origen", "user");
        when(loteRepository.findByIdConBloqueo(10L)).thenReturn(Optional.of(lote));
        when(loteRepository.save(any(LoteInventario.class))).thenAnswer(inv -> inv.getArgument(0));

        LoteInventario actualizado = inventarioService.ajustarInventario(
                10L, new BigDecimal("25.00"), TipoMovimientoInventario.AJUSTE_POSITIVO, "auditor@sirab.local", "Conteo físico sobrante");

        assertEquals(new BigDecimal("125.00"), actualizado.getSaldoDisponible());

        ArgumentCaptor<MovimientoInventario> captor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoRepository).save(captor.capture());
        assertEquals(TipoMovimientoInventario.AJUSTE_POSITIVO, captor.getValue().getTipo());
        assertEquals(new BigDecimal("25.00"), captor.getValue().getCantidad());
    }

    @Test
    void ajusteNegativoDecrementaSaldoYRechazaSiSaldoInsuficiente() {
        LoteInventario lote = new LoteInventario(
                articuloAgua, "LOTE-1", null, "Litros", new BigDecimal("50.00"), java.time.Instant.now(), "Origen", "user");
        when(loteRepository.findByIdConBloqueo(10L)).thenReturn(Optional.of(lote));

        // Ajuste mayor a saldo disponible -> Rechazado
        assertThrows(IllegalStateException.class, () -> inventarioService.ajustarInventario(
                10L, new BigDecimal("55.00"), TipoMovimientoInventario.AJUSTE_NEGATIVO, "auditor@sirab.local", "Merma"));

        // Ajuste válido
        when(loteRepository.save(any(LoteInventario.class))).thenAnswer(inv -> inv.getArgument(0));
        LoteInventario actualizado = inventarioService.ajustarInventario(
                10L, new BigDecimal("20.00"), TipoMovimientoInventario.AJUSTE_NEGATIVO, "auditor@sirab.local", "Merma");

        assertEquals(new BigDecimal("30.00"), actualizado.getSaldoDisponible());
    }
}
