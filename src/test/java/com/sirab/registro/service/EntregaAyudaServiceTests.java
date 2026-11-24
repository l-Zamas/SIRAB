package com.sirab.registro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sirab.registro.ArticuloAyuda;
import com.sirab.registro.AsignacionAyuda;
import com.sirab.registro.AsignacionAyudaRepository;
import com.sirab.registro.DecisionAyuda;
import com.sirab.registro.DocumentoPrivado;
import com.sirab.registro.EntregaAyuda;
import com.sirab.registro.EntregaAyudaRepository;
import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.Familia;
import com.sirab.registro.LoteInventario;
import com.sirab.registro.LoteInventarioRepository;
import com.sirab.registro.MovimientoInventario;
import com.sirab.registro.MovimientoInventarioRepository;
import com.sirab.registro.Necesidad;
import com.sirab.registro.OrigenAsignacionAyuda;
import com.sirab.registro.RevisionSanitariaExterna;
import com.sirab.registro.SolicitudAyudaMedica;
import com.sirab.registro.TipoDocumentoPrivado;
import com.sirab.registro.TipoMovimientoInventario;

@ExtendWith(MockitoExtension.class)
class EntregaAyudaServiceTests {

    @Mock
    private EntregaAyudaRepository entregaRepository;

    @Mock
    private AsignacionAyudaRepository asignacionRepository;

    @Mock
    private LoteInventarioRepository loteRepository;

    @Mock
    private MovimientoInventarioRepository movimientoRepository;

    @InjectMocks
    private EntregaAyudaService entregaAyudaService;

    private Familia familia;
    private ArticuloAyuda articuloAgua;
    private AsignacionAyuda asignacionAgua;
    private LoteInventario loteAgua;

    @BeforeEach
    void setUp() {
        familia = new Familia();
        familia.setNombreRegistrante("Familia Pérez");
        familia.setDireccionReferencia("Frente a escuela");
        familia.setEstado(EstadoRegistro.EN_ATENCION);

        articuloAgua = new ArticuloAyuda("AGUA-01", "Bidón Agua", Necesidad.AGUA, "Litros", true, Instant.now(), "admin@sirab.local");

        asignacionAgua = new AsignacionAyuda(
                familia,
                articuloAgua,
                null,
                null,
                null,
                OrigenAsignacionAyuda.REGLA,
                "IDEMP-ASIG-001",
                new BigDecimal("100.00"),
                "Litros",
                Instant.now(),
                "coordinador@sirab.local");

        loteAgua = new LoteInventario(
                articuloAgua,
                "LOTE-A1",
                null,
                "Litros",
                new BigDecimal("200.00"),
                Instant.now(),
                "Cruz Roja",
                "bodega@sirab.local");

        org.springframework.test.util.ReflectionTestUtils.setField(familia, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(articuloAgua, "id", 10L);
        org.springframework.test.util.ReflectionTestUtils.setField(asignacionAgua, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(loteAgua, "id", 5L);
    }

    @Test
    void registraEntregaExitosaDescuentaLoteYGeneraMovimientoSalida() {
        when(asignacionRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(asignacionAgua));
        when(entregaRepository.sumarCantidadEntregadaPorAsignacion(any())).thenReturn(BigDecimal.ZERO);
        when(loteRepository.findByIdConBloqueo(5L)).thenReturn(Optional.of(loteAgua));
        when(entregaRepository.save(any(EntregaAyuda.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrarEntregaAsignacionCommand command = new RegistrarEntregaAsignacionCommand(
                1L,
                new BigDecimal("40.00"),
                5L,
                Instant.now(),
                "Primera entrega parcial");

        EntregaAyuda entrega = entregaAyudaService.registrarEntrega(command, "campo@sirab.local");

        assertNotNull(entrega);
        assertEquals(new BigDecimal("40.00"), entrega.getCantidadValor());
        assertEquals("Litros", entrega.getUnidad());
        assertFalse(entrega.isSatisfaceNecesidadCompleta());
        assertEquals("campo@sirab.local", entrega.getUsuarioCorreo());

        // El saldo del lote debe haberse descontado de 200 a 160
        assertEquals(new BigDecimal("160.00"), loteAgua.getSaldoDisponible());
        verify(loteRepository).save(loteAgua);

        // Movimiento de inventario generado
        ArgumentCaptor<MovimientoInventario> movCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoRepository).save(movCaptor.capture());
        MovimientoInventario mov = movCaptor.getValue();
        assertEquals(TipoMovimientoInventario.SALIDA_ENTREGA, mov.getTipo());
        assertEquals(new BigDecimal("40.00"), mov.getCantidad());
        assertEquals("campo@sirab.local", mov.getUsuarioCorreo());
    }

    @Test
    void rechazaEntregaSiFamiliaNoEstaEnEstadoVerificado() {
        familia.setEstado(EstadoRegistro.PENDIENTE);
        when(asignacionRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(asignacionAgua));

        RegistrarEntregaAsignacionCommand command = new RegistrarEntregaAsignacionCommand(
                1L, new BigDecimal("20.00"), null, null, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> entregaAyudaService.registrarEntrega(command, "campo@sirab.local"));
        assertTrue(ex.getMessage().contains("EN_ATENCION"));
    }

    @Test
    void rechazaEntregaSiCantidadSuperaSaldoPendienteDeAsignacion() {
        when(asignacionRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(asignacionAgua));
        // Ya se entregaron 80 de los 100 asignados
        when(entregaRepository.sumarCantidadEntregadaPorAsignacion(any())).thenReturn(new BigDecimal("80.00"));

        // Se intentan entregar 30 (80 + 30 = 110 > 100)
        RegistrarEntregaAsignacionCommand command = new RegistrarEntregaAsignacionCommand(
                1L, new BigDecimal("30.00"), null, null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> entregaAyudaService.registrarEntrega(command, "campo@sirab.local"));
        assertTrue(ex.getMessage().contains("supera el saldo pendiente"));
    }

    @Test
    void rechazaEntregaSiLoteNoTieneExistenciasSuficientes() {
        when(asignacionRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(asignacionAgua));
        when(entregaRepository.sumarCantidadEntregadaPorAsignacion(any())).thenReturn(BigDecimal.ZERO);

        // Lote con solo 15 disponibles
        loteAgua.actualizarSaldoDisponible(new BigDecimal("15.00"));
        when(loteRepository.findByIdConBloqueo(5L)).thenReturn(Optional.of(loteAgua));

        RegistrarEntregaAsignacionCommand command = new RegistrarEntregaAsignacionCommand(
                1L, new BigDecimal("50.00"), 5L, null, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> entregaAyudaService.registrarEntrega(command, "campo@sirab.local"));
        assertTrue(ex.getMessage().contains("existencias suficientes"));
    }

    @Test
    void rechazaEntregaSiLoteEstaVencido() {
        when(asignacionRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(asignacionAgua));
        when(entregaRepository.sumarCantidadEntregadaPorAsignacion(any())).thenReturn(BigDecimal.ZERO);

        LoteInventario loteVencido = new LoteInventario(
                articuloAgua, "LOT-VENC", LocalDate.now().minusDays(1), "Litros", new BigDecimal("100.00"), Instant.now(), "Donación", "user");
        when(loteRepository.findByIdConBloqueo(99L)).thenReturn(Optional.of(loteVencido));

        RegistrarEntregaAsignacionCommand command = new RegistrarEntregaAsignacionCommand(
                1L, new BigDecimal("10.00"), 99L, null, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> entregaAyudaService.registrarEntrega(command, "campo@sirab.local"));
        assertTrue(ex.getMessage().contains("vencido"));
    }

    @Test
    void validaRevisionSanitariaAprobadaParaMedicamentos() {
        ArticuloAyuda articuloMed = new ArticuloAyuda("MED-01", "Analgesico", Necesidad.MEDICAMENTOS, "Unidades", true, Instant.now(), "admin@sirab.local");
        SolicitudAyudaMedica solMed = new SolicitudAyudaMedica(familia, Instant.now(), "doc@sirab.local", "Dolor");
        DocumentoPrivado docPriv = new DocumentoPrivado("k1", "rec.pdf", "application/pdf", 100, "sha", TipoDocumentoPrivado.SOPORTE_MEDICO, Instant.now(), "doc");

        RevisionSanitariaExterna revRechazada = new RevisionSanitariaExterna(
                solMed, "Hospital", "Dr. Ruiz", "RM-123", DecisionAyuda.RECHAZADA, Instant.now(), "REF-1", docPriv, Instant.now(), "doc", "No procede");

        AsignacionAyuda asignacionMed = new AsignacionAyuda(
                familia,
                articuloMed,
                null,
                null,
                revRechazada,
                OrigenAsignacionAyuda.REVISION_SANITARIA,
                "IDEMP-MED-01",
                new BigDecimal("10.00"),
                "Unidades",
                Instant.now(),
                "doc@sirab.local");

        LoteInventario loteMed = new LoteInventario(
                articuloMed, "LOT-MED", LocalDate.now().plusMonths(3), "Unidades", new BigDecimal("50.00"), Instant.now(), "MinSalud", "bodega");

        org.springframework.test.util.ReflectionTestUtils.setField(articuloMed, "id", 20L);
        org.springframework.test.util.ReflectionTestUtils.setField(asignacionMed, "id", 2L);
        org.springframework.test.util.ReflectionTestUtils.setField(loteMed, "id", 10L);

        when(asignacionRepository.findByIdConBloqueo(2L)).thenReturn(Optional.of(asignacionMed));
        when(entregaRepository.sumarCantidadEntregadaPorAsignacion(any())).thenReturn(BigDecimal.ZERO);
        when(loteRepository.findByIdConBloqueo(10L)).thenReturn(Optional.of(loteMed));

        RegistrarEntregaAsignacionCommand command = new RegistrarEntregaAsignacionCommand(
                2L, new BigDecimal("10.00"), 10L, null, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> entregaAyudaService.registrarEntrega(command, "campo@sirab.local"));
        assertTrue(ex.getMessage().contains("revisión sanitaria externa aprobada"));
    }

    @Test
    void marcaSatisfaceNecesidadCompletaCuandoSeCompletanTodasLasAsignacionesDelTipo() {
        when(asignacionRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(asignacionAgua));
        when(entregaRepository.sumarCantidadEntregadaPorAsignacion(1L)).thenReturn(new BigDecimal("50.00"));
        when(loteRepository.findByIdConBloqueo(5L)).thenReturn(Optional.of(loteAgua));
        when(asignacionRepository.findByFamiliaIdOrderByAsignadaAtDescIdAsc(any())).thenReturn(List.of(asignacionAgua));
        when(entregaRepository.save(any(EntregaAyuda.class))).thenAnswer(inv -> inv.getArgument(0));

        // Entregamos los 50 restantes para completar los 100
        RegistrarEntregaAsignacionCommand command = new RegistrarEntregaAsignacionCommand(
                1L, new BigDecimal("50.00"), 5L, null, "Cierre de asignación de agua");

        EntregaAyuda entrega = entregaAyudaService.registrarEntrega(command, "campo@sirab.local");

        assertTrue(entrega.isSatisfaceNecesidadCompleta());
    }
}

