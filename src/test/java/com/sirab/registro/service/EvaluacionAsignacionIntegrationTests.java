package com.sirab.registro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sirab.registro.ArticuloAyuda;
import com.sirab.registro.ArticuloAyudaRepository;
import com.sirab.registro.AsignacionAyudaRepository;
import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.EvaluacionAsignacionAyudaRepository;
import com.sirab.registro.Familia;
import com.sirab.registro.FamiliaRepository;
import com.sirab.registro.Necesidad;
import com.sirab.registro.ReglaAsignacionAyudaRepository;
import com.sirab.registro.SituacionFamiliar;

@SpringBootTest
@ActiveProfiles("test")
class EvaluacionAsignacionIntegrationTests {

    @Autowired
    private EvaluacionAsignacionService evaluacionService;

    @Autowired
    private CatalogoArticuloService catalogoService;

    @Autowired
    private ReglaAsignacionService reglaService;

    @Autowired
    private FamiliaRepository familiaRepository;

    @Autowired
    private EvaluacionAsignacionAyudaRepository evaluacionRepository;

    @Autowired
    private AsignacionAyudaRepository asignacionRepository;

    @Autowired
    private ReglaAsignacionAyudaRepository reglaRepository;

    @Autowired
    private ArticuloAyudaRepository articuloRepository;

    private Long familiaId;
    private Long articuloAguaId;

    @BeforeEach
    void setUp() {
        Familia familia = new Familia();
        familia.setNombreRegistrante("FAMILIA CONCURRENTE TEST");
        familia.setDireccionReferencia("Referencia Concurrencia");
        familia.setNumeroIntegrantes(3);
        familia.setTelefonoContacto("3009998877");
        familia.setConsentimientoAt(Instant.now());
        familia.setConsentimientoVersion("v1");
        familia.setEstado(EstadoRegistro.VERIFICADO);
        familia.setNecesidades(Set.of(Necesidad.AGUA));
        familia.setSituaciones(Set.of(SituacionFamiliar.MENORES_DE_CINCO));
        familia = familiaRepository.save(familia);
        familiaId = familia.getId();

        ArticuloAyuda agua = catalogoService.registrarArticulo(new RegistrarArticuloCommand(
                "AGUA-CONC-" + System.nanoTime(),
                "Agua 5L Concurrencia",
                Necesidad.AGUA,
                "LITROS",
                "admin@sirab.local"));
        articuloAguaId = agua.getId();

        reglaService.publicarVersion(new PublicarVersionReglasCommand(
                agua.getId(),
                LocalDate.of(2026, 1, 1),
                null,
                List.of(
                        CriterioReglaCommand.base(new BigDecimal("10.000000")),
                        CriterioReglaCommand.situacionFamiliar(
                                SituacionFamiliar.MENORES_DE_CINCO, new BigDecimal("5.000000"))),
                "coord@sirab.local"));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (familiaId != null) {
            List<com.sirab.registro.EvaluacionAsignacionAyuda> evals =
                    evaluacionRepository.findByFamiliaIdOrderByRevisionDesc(familiaId);
            for (var e : evals) {
                asignacionRepository.deleteAll(asignacionRepository.findByEvaluacionIdOrderByIdAsc(e.getId()));
            }
            evaluacionRepository.deleteAll(evals);
            familiaRepository.deleteById(familiaId);
        }
        if (articuloAguaId != null) {
            reglaRepository.deleteAll(
                    reglaRepository.findByArticuloIdOrderByVersionDescCriterioCodigoAsc(articuloAguaId));
            articuloRepository.deleteById(articuloAguaId);
        }
    }

    @Test
    void soportaReintentosConcurrentesConMismaClaveIdempotenciaSinDuplicar() throws Exception {
        int numHilos = 4;
        ExecutorService executor = Executors.newFixedThreadPool(numHilos);
        CountDownLatch latch = new CountDownLatch(1);
        String claveIdempotencia = "conc-misma-clave-" + System.nanoTime();

        List<Future<ResultadoEvaluacion>> futures = new ArrayList<>();
        for (int i = 0; i < numHilos; i++) {
            futures.add(executor.submit(() -> {
                latch.await();
                return evaluacionService.evaluarNecesidades(new EvaluarNecesidadesCommand(
                        familiaId,
                        claveIdempotencia,
                        LocalDate.of(2026, 3, 1),
                        "coord@sirab.local"));
            }));
        }

        latch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        List<ResultadoEvaluacion> resultados = new ArrayList<>();
        for (Future<ResultadoEvaluacion> f : futures) {
            resultados.add(f.get());
        }

        // Todos los hilos deben retornar la misma evaluación
        Long evaluacionIdEsperada = resultados.get(0).evaluacion().getId();
        assertNotNull(evaluacionIdEsperada);

        for (ResultadoEvaluacion r : resultados) {
            assertEquals(evaluacionIdEsperada, r.evaluacion().getId());
            assertEquals(2, r.asignaciones().size());
        }

        // En la base de datos debe existir exactamente 1 evaluación y 2 asignaciones
        assertEquals(1, evaluacionRepository.findByFamiliaIdOrderByRevisionDesc(familiaId).size());
        assertEquals(2, asignacionRepository.findByFamiliaIdOrderByAsignadaAtDescIdAsc(familiaId).size());
    }

    @Test
    void serializaEvaluacionesConcurrentesConClavesDistintasGenerandoRevisionesSecuenciales() throws Exception {
        int numHilos = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numHilos);
        CountDownLatch latch = new CountDownLatch(1);

        List<Future<ResultadoEvaluacion>> futures = new ArrayList<>();
        for (int i = 0; i < numHilos; i++) {
            final String clave = "conc-clave-distinta-" + i + "-" + System.nanoTime();
            futures.add(executor.submit(() -> {
                latch.await();
                return evaluacionService.evaluarNecesidades(new EvaluarNecesidadesCommand(
                        familiaId,
                        clave,
                        LocalDate.of(2026, 3, 1),
                        "coord@sirab.local"));
            }));
        }

        latch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        List<ResultadoEvaluacion> resultados = new ArrayList<>();
        for (Future<ResultadoEvaluacion> f : futures) {
            resultados.add(f.get());
        }

        // Se deben haber generado revisiones distintas (1 y 2)
        List<Integer> revisiones = resultados.stream()
                .map(r -> r.evaluacion().getRevision())
                .sorted()
                .toList();

        assertEquals(List.of(1, 2), revisiones);
        assertEquals(2, evaluacionRepository.findByFamiliaIdOrderByRevisionDesc(familiaId).size());
    }
}
