package com.sirab.registro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sirab.registro.AprobacionDefinitivaAlojamiento;
import com.sirab.registro.AprobacionProvisionalAlojamiento;
import com.sirab.registro.ArticuloAyuda;
import com.sirab.registro.ArticuloAyudaRepository;
import com.sirab.registro.AsignacionAyuda;
import com.sirab.registro.AsignacionAyudaRepository;
import com.sirab.registro.DecisionAyuda;
import com.sirab.registro.DocumentoPrivado;
import com.sirab.registro.DocumentoPrivadoRepository;
import com.sirab.registro.EntregaAyuda;
import com.sirab.registro.EntregaAyudaRepository;
import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.EvaluacionAsignacionAyuda;
import com.sirab.registro.EvaluacionAsignacionAyudaRepository;
import com.sirab.registro.Familia;
import com.sirab.registro.FamiliaRepository;
import com.sirab.registro.LoteInventario;
import com.sirab.registro.LoteInventarioRepository;
import com.sirab.registro.MovimientoInventario;
import com.sirab.registro.MovimientoInventarioRepository;
import com.sirab.registro.Necesidad;
import com.sirab.registro.OrigenAsignacionAyuda;
import com.sirab.registro.ReglaAsignacionAyuda;
import com.sirab.registro.ReglaAsignacionAyudaRepository;
import com.sirab.registro.ResultadoVerificacionAlojamiento;
import com.sirab.registro.SituacionFamiliar;
import com.sirab.registro.SolicitudAlojamiento;
import com.sirab.registro.TipoCriterioAsignacion;
import com.sirab.registro.TipoDocumentoPrivado;

@SpringBootTest
@ActiveProfiles("test")
class EntregaAyudaIntegrationTests {

    @Autowired
    private FamiliaRepository familiaRepository;

    @Autowired
    private ArticuloAyudaRepository articuloRepository;

    @Autowired
    private AsignacionAyudaRepository asignacionRepository;

    @Autowired
    private EntregaAyudaRepository entregaRepository;

    @Autowired
    private LoteInventarioRepository loteRepository;

    @Autowired
    private MovimientoInventarioRepository movimientoRepository;

    @Autowired
    private DocumentoPrivadoRepository documentoRepository;

    @Autowired
    private EvaluacionAsignacionAyudaRepository evaluacionRepository;

    @Autowired
    private ReglaAsignacionAyudaRepository reglaRepository;

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private EntregaAyudaService entregaAyudaService;

    @Autowired
    private AlojamientoService alojamientoService;

    @Autowired
    private RegistroFamiliarService registroFamiliarService;

    @AfterEach
    void tearDown() {
        movimientoRepository.deleteAll();
        entregaRepository.deleteAll();
        loteRepository.deleteAll();
        asignacionRepository.deleteAll();
        evaluacionRepository.deleteAll();
        reglaRepository.deleteAll();
    }

    private AsignacionAyuda crearAsignacionValida(Familia familia, ArticuloAyuda articulo, BigDecimal cantidad, String claveIdempotencia) {
        EvaluacionAsignacionAyuda eval = new EvaluacionAsignacionAyuda(
                familia, 1, "EVAL-IDEMP-" + claveIdempotencia, Instant.now(), "evaluador@sirab.local",
                familia.getNecesidades(), familia.getSituaciones());
        eval = evaluacionRepository.save(eval);

        ReglaAsignacionAyuda regla = reglaRepository
                .findByArticuloIdAndVersionOrderByCriterioCodigoAsc(articulo.getId(), 1)
                .stream()
                .findFirst()
                .orElseGet(() -> reglaRepository.save(new ReglaAsignacionAyuda(
                        articulo, 1, TipoCriterioAsignacion.BASE, null, cantidad, articulo.getUnidad(),
                        LocalDate.now().minusDays(1), null, Instant.now(), "admin@sirab.local")));

        AsignacionAyuda asignacion = new AsignacionAyuda(
                familia, articulo, eval, regla, null, OrigenAsignacionAyuda.REGLA,
                claveIdempotencia, cantidad, articulo.getUnidad(), Instant.now(), "coordinador@sirab.local");
        return asignacionRepository.save(asignacion);
    }

    private Familia crearFamiliaVerificada(String nombre, Set<Necesidad> necesidades, Set<SituacionFamiliar> situaciones) {
        Familia familia = new Familia();
        familia.setNombreRegistrante(nombre);
        familia.setDireccionReferencia("Frente a sector comunal");
        familia.setBarrio("Centro");
        familia.setDireccion("Calle Principal # 10");
        familia.setTelefonoContacto("3001234567");
        familia.setConsentimientoAt(Instant.now());
        familia.setConsentimientoVersion("penpot-consentimiento-v1");
        familia.setNumeroIntegrantes(4);
        familia.setSituaciones(situaciones != null ? situaciones : Set.of());
        familia.setNecesidades(necesidades != null ? necesidades : Set.of());
        familia.setEstado(EstadoRegistro.EN_ATENCION);
        return familiaRepository.save(familia);
    }

    @Test
    void flujoCompletoDeEntregaInventarioYTransicionAAtendido() {
        // 1. Crear Familia en estado VERIFICADO
        Familia familia = crearFamiliaVerificada("Familia Integración 1", Set.of(Necesidad.AGUA), Set.of(SituacionFamiliar.MENORES_DE_CINCO));

        // 2. Crear Artículo de Ayuda
        ArticuloAyuda articulo = new ArticuloAyuda(
                "TEST-AGUA-01", "Agua Potable 20L", Necesidad.AGUA, "Litros", true, Instant.now(), "admin@sirab.local");
        articulo = articuloRepository.save(articulo);

        // 3. Crear Asignación de 60 Litros
        AsignacionAyuda asignacion = crearAsignacionValida(
                familia, articulo, new BigDecimal("60.00"), "IDEMP-TEST-INT-01");

        // 4. Ingresar Lote de Inventario de 100 Litros
        RegistrarLoteCommand cmdLote = new RegistrarLoteCommand(
                articulo.getId(),
                "LOT-INT-AGUA-1",
                null,
                new BigDecimal("100.00"),
                "Bodega Central",
                "Ingreso para prueba");
        LoteInventario lote = inventarioService.ingresarLote(cmdLote, "bodeguero@sirab.local");

        // Al inicio, la familia NO puede marcarse como atendida
        assertFalse(registroFamiliarService.puedeMarcarAtendido(familia.getId()));
        final Long familiaId = familia.getId();
        assertThrows(IllegalStateException.class,
                () -> registroFamiliarService.marcarAtendido(familiaId, "campo@sirab.local", "Cierre prematuro"));

        // 5. Realizar entrega parcial de 20 Litros
        RegistrarEntregaAsignacionCommand cmdEntrega1 = new RegistrarEntregaAsignacionCommand(
                asignacion.getId(),
                new BigDecimal("20.00"),
                lote.getId(),
                Instant.now(),
                "Primera entrega parcial");
        EntregaAyuda entrega1 = entregaAyudaService.registrarEntrega(cmdEntrega1, "campo@sirab.local");

        assertNotNull(entrega1.getId());
        assertEquals(new BigDecimal("20.00"), entrega1.getCantidadValor());
        assertFalse(entrega1.isSatisfaceNecesidadCompleta());

        // Comprobar inventario descontado y movimiento registrado
        LoteInventario loteActualizado = loteRepository.findById(lote.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("80.00").compareTo(loteActualizado.getSaldoDisponible()));

        List<MovimientoInventario> movimientos = movimientoRepository.findByLote_IdOrderByRegistradoAtDescIdDesc(lote.getId());
        assertEquals(2, movimientos.size()); // 1 INGRESO + 1 SALIDA_ENTREGA

        // Aún no puede marcarse atendido
        assertFalse(registroFamiliarService.puedeMarcarAtendido(familia.getId()));

        // 6. Realizar segunda entrega de 40 Litros (completando los 60 Litros asignados)
        RegistrarEntregaAsignacionCommand cmdEntrega2 = new RegistrarEntregaAsignacionCommand(
                asignacion.getId(),
                new BigDecimal("40.00"),
                lote.getId(),
                Instant.now(),
                "Segunda entrega que completa asignación");
        EntregaAyuda entrega2 = entregaAyudaService.registrarEntrega(cmdEntrega2, "campo@sirab.local");

        assertTrue(entrega2.isSatisfaceNecesidadCompleta());

        LoteInventario loteFinal = loteRepository.findById(lote.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("40.00").compareTo(loteFinal.getSaldoDisponible()));

        // 7. Ahora la familia SÍ puede marcarse como atendida
        assertTrue(registroFamiliarService.puedeMarcarAtendido(familia.getId()));

        boolean marcado = registroFamiliarService.marcarAtendido(familia.getId(), "campo@sirab.local", "Atención completa comprobada");
        assertTrue(marcado);

        Familia familiaAtendida = familiaRepository.findById(familia.getId()).orElseThrow();
        assertEquals(EstadoRegistro.ATENDIDO, familiaAtendida.getEstado());
    }

    @Test
    void flujoAlojamientoRequiereTodasLasFasesParaHabilitarAtendido() {
        // Familia con necesidad ALOJAMIENTO
        Familia familia = crearFamiliaVerificada("Familia Alojamiento Int", Set.of(Necesidad.ALOJAMIENTO), Set.of(SituacionFamiliar.ADULTOS_MAYORES));

        // Sin flujo, no puede marcarse atendido
        assertFalse(registroFamiliarService.puedeMarcarAtendido(familia.getId()));

        // Fase 1: Solicitud
        SolicitudAlojamiento sol = alojamientoService.registrarSolicitud(
                familia.getId(), "campo@sirab.local", "Pérdida de vivienda");
        assertFalse(registroFamiliarService.puedeMarcarAtendido(familia.getId()));

        // Guardar documento fotográfico para la verificación
        DocumentoPrivado foto = new DocumentoPrivado(
                "fotos/vivienda_1.jpg", "foto1.jpg", "image/jpeg", 2048, "hashsha256fotounica1",
                TipoDocumentoPrivado.EVIDENCIA_ALOJAMIENTO, Instant.now(), "perito@sirab.local");
        foto = documentoRepository.save(foto);

        // Fase 2: Verificación técnica
        alojamientoService.verificarAlojamiento(
                sol.getId(), ResultadoVerificacionAlojamiento.VERIFICADA, foto.getId(), "perito@sirab.local", "Daño irreparable");
        assertFalse(registroFamiliarService.puedeMarcarAtendido(familia.getId()));

        // Fase 3: Aprobación provisional
        AprobacionProvisionalAlojamiento prov = alojamientoService.aprobarProvisionalmente(
                sol.getId(), DecisionAyuda.APROBADA, "coordinacion@sirab.local", "Cumple criterios provisionales");
        assertFalse(registroFamiliarService.puedeMarcarAtendido(familia.getId()));

        // Fase 4: Aprobación definitiva
        DocumentoPrivado decision = new DocumentoPrivado(
                "decision/resolucion-999.pdf", "resolucion-999.pdf", "application/pdf", 1024, "hashdecision999",
                TipoDocumentoPrivado.DECISION_ENTIDAD, Instant.now(), "ungrd@sirab.local");
        decision = documentoRepository.save(decision);
        AprobacionDefinitivaAlojamiento def = alojamientoService.registrarAprobacionDefinitiva(
                sol.getId(), "UNGRD", "RESOLUCION-999", DecisionAyuda.APROBADA, decision.getId(), "ungrd@sirab.local", "Aprobación oficial");
        assertFalse(registroFamiliarService.puedeMarcarAtendido(familia.getId()));

        // Fase 5: Confirmación de prestación efectiva
        DocumentoPrivado evidenciaPrestacion = new DocumentoPrivado(
                "prestacion/albergue-999.pdf", "albergue-999.pdf", "application/pdf", 1024, "hashevidencia999",
                TipoDocumentoPrivado.CONFIRMACION_PRESTACION, Instant.now(), "enlace@sirab.local");
        evidenciaPrestacion = documentoRepository.save(evidenciaPrestacion);
        alojamientoService.confirmarPrestacion(
                sol.getId(), def.getId(), Instant.now(), evidenciaPrestacion.getId(), "enlace@sirab.local", "Entrega formal de albergue temporal");

        // Tras la confirmación, SÍ puede marcarse como atendido
        assertTrue(registroFamiliarService.puedeMarcarAtendido(familia.getId()));

        registroFamiliarService.marcarAtendido(familia.getId(), "campo@sirab.local", "Cierre con alojamiento confirmado");
        assertEquals(EstadoRegistro.ATENDIDO, familiaRepository.findById(familia.getId()).orElseThrow().getEstado());
    }

    @Test
    void concurrenciaEnMismaAsignacionImpideSobreentrega() throws Exception {
        Familia familia = crearFamiliaVerificada("Fam Misma Asignación", Set.of(Necesidad.AGUA), Set.of());
        ArticuloAyuda articulo = articuloRepository.save(new ArticuloAyuda(
                "AGUA-SAME-ASIG", "Agua misma asignación", Necesidad.AGUA, "Litros", true, Instant.now(), "admin@sirab.local"));
        AsignacionAyuda asignacion = crearAsignacionValida(familia, articulo, new BigDecimal("50.00"), "IDEMP-SAME-ASIG");
        LoteInventario lote = inventarioService.ingresarLote(new RegistrarLoteCommand(
                articulo.getId(), "LOT-SAME-ASIG", null, new BigDecimal("100.00"), "Donante", "Prueba"), "bodega@sirab.local");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger fallos = new AtomicInteger();
        for (int i = 0; i < 2; i++) {
            final String usuario = "operador" + i + "@sirab.local";
            executor.submit(() -> {
                try {
                    startLatch.await();
                    entregaAyudaService.registrarEntrega(new RegistrarEntregaAsignacionCommand(
                            asignacion.getId(), new BigDecimal("40.00"), lote.getId(), Instant.now(), "Concurrente"), usuario);
                    exitos.incrementAndGet();
                } catch (Exception ex) {
                    fallos.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        assertEquals(1, exitos.get());
        assertEquals(1, fallos.get());
        assertEquals(0, new BigDecimal("40.00").compareTo(
                entregaRepository.sumarCantidadEntregadaPorAsignacion(asignacion.getId())));
    }

    @Test
    void concurrenciaEnDespachoDeLoteImpideSaldosNegativos() throws Exception {
        Familia f1 = crearFamiliaVerificada("Fam Concurrente 1", Set.of(Necesidad.AGUA), Set.of());
        Familia f2 = crearFamiliaVerificada("Fam Concurrente 2", Set.of(Necesidad.AGUA), Set.of());

        ArticuloAyuda articulo = new ArticuloAyuda(
                "AGUA-CONC-01", "Agua Concurrente", Necesidad.AGUA, "Litros", true, Instant.now(), "admin@sirab.local");
        articulo = articuloRepository.save(articulo);

        AsignacionAyuda asig1 = crearAsignacionValida(f1, articulo, new BigDecimal("40.00"), "IDEMP-C1");
        AsignacionAyuda asig2 = crearAsignacionValida(f2, articulo, new BigDecimal("40.00"), "IDEMP-C2");

        // Lote con solo 50 Litros (no alcanza para los dos despachos de 40: 40 + 40 = 80 > 50)
        RegistrarLoteCommand cmdLote = new RegistrarLoteCommand(
                articulo.getId(), "LOT-CONC-50", null, new BigDecimal("50.00"), "Donante", "Lote límite");
        LoteInventario lote = inventarioService.ingresarLote(cmdLote, "bodega@sirab.local");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);

        AtomicInteger exitos = new AtomicInteger(0);
        AtomicInteger fallos = new AtomicInteger(0);

        final Long loteId = lote.getId();
        final Long asig1Id = asig1.getId();
        final Long asig2Id = asig2.getId();

        executor.submit(() -> {
            try {
                startLatch.await();
                entregaAyudaService.registrarEntrega(
                        new RegistrarEntregaAsignacionCommand(asig1Id, new BigDecimal("40.00"), loteId, Instant.now(), "Hilo 1"),
                        "operador1@sirab.local");
                exitos.incrementAndGet();
            } catch (Exception e) {
                fallos.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                entregaAyudaService.registrarEntrega(
                        new RegistrarEntregaAsignacionCommand(asig2Id, new BigDecimal("40.00"), loteId, Instant.now(), "Hilo 2"),
                        "operador2@sirab.local");
                exitos.incrementAndGet();
            } catch (Exception e) {
                fallos.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        // Exactamente uno debe tener éxito y uno debe fallar por saldo insuficiente
        assertEquals(1, exitos.get());
        assertEquals(1, fallos.get());

        // El saldo del lote debe quedar exactamente en 10 Litros (50 - 40 = 10), NUNCA negativo!
        LoteInventario loteFinal = loteRepository.findById(loteId).orElseThrow();
        assertEquals(0, new BigDecimal("10.00").compareTo(loteFinal.getSaldoDisponible()));
    }
}
