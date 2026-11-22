package com.sirab.registro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sirab.registro.ArticuloAyuda;
import com.sirab.registro.AsignacionAyuda;
import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.EvaluacionAsignacionAyuda;
import com.sirab.registro.Familia;
import com.sirab.registro.FamiliaRepository;
import com.sirab.registro.Necesidad;
import com.sirab.registro.OrigenAsignacionAyuda;
import com.sirab.registro.SituacionFamiliar;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EvaluacionAsignacionServiceTests {

    @Autowired
    private EvaluacionAsignacionService evaluacionService;

    @Autowired
    private CatalogoArticuloService catalogoService;

    @Autowired
    private ReglaAsignacionService reglaService;

    @Autowired
    private FamiliaRepository familiaRepository;

    private Familia familiaVerificada;
    private ArticuloAyuda articuloAgua;
    private ArticuloAyuda articuloAlimento;

    @BeforeEach
    void setUp() {
        // Crear familia en estado VERIFICADO
        familiaVerificada = new Familia();
        familiaVerificada.setNombreRegistrante("FAMILIA EVALUACION TEST");
        familiaVerificada.setNumeroIntegrantes(4);
        familiaVerificada.setTelefonoContacto("3001234567");
        familiaVerificada.setConsentimientoAt(Instant.parse("2026-03-01T10:00:00Z"));
        familiaVerificada.setConsentimientoVersion("penpot-consentimiento-v1");
        familiaVerificada.setBarrio("Centro");
        familiaVerificada.setDireccion("Calle 1 # 2-3");
        familiaVerificada.setDireccionReferencia("Casa blanca");
        familiaVerificada.setEstado(EstadoRegistro.VERIFICADO);
        familiaVerificada.setNecesidades(Set.of(Necesidad.AGUA, Necesidad.ALIMENTOS));
        familiaVerificada.setSituaciones(Set.of(SituacionFamiliar.MENORES_DE_CINCO));
        familiaVerificada = familiaRepository.save(familiaVerificada);

        // Crear artículos en catálogo
        articuloAgua = catalogoService.registrarArticulo(new RegistrarArticuloCommand(
                "AGUA-EVAL-" + System.nanoTime(),
                "Agua Bidón 5L",
                Necesidad.AGUA,
                "LITROS",
                "admin@sirab.local"));

        articuloAlimento = catalogoService.registrarArticulo(new RegistrarArticuloCommand(
                "ALIM-EVAL-" + System.nanoTime(),
                "Kit Alimentos",
                Necesidad.ALIMENTOS,
                "KITS",
                "admin@sirab.local"));

        // Publicar reglas vigentes para ambos artículos desde 2026-01-01
        reglaService.publicarVersion(new PublicarVersionReglasCommand(
                articuloAgua.getId(),
                LocalDate.of(2026, 1, 1),
                null,
                List.of(
                        CriterioReglaCommand.base(new BigDecimal("10.000000")),
                        CriterioReglaCommand.situacionFamiliar(
                                SituacionFamiliar.MENORES_DE_CINCO, new BigDecimal("5.000000")),
                        CriterioReglaCommand.situacionFamiliar(
                                SituacionFamiliar.ADULTOS_MAYORES, new BigDecimal("3.000000"))),
                "coord@sirab.local"));

        reglaService.publicarVersion(new PublicarVersionReglasCommand(
                articuloAlimento.getId(),
                LocalDate.of(2026, 1, 1),
                null,
                List.of(CriterioReglaCommand.base(new BigDecimal("2.000000"))),
                "coord@sirab.local"));
    }

    @Test
    void evaluaCorrectamenteNecesidadesYAplicaReglasVigentes() {
        EvaluarNecesidadesCommand command = new EvaluarNecesidadesCommand(
                familiaVerificada.getId(),
                "eval-idemp-1-" + System.nanoTime(),
                LocalDate.of(2026, 3, 1),
                "coord@sirab.local");

        ResultadoEvaluacion resultado = evaluacionService.evaluarNecesidades(command);

        assertNotNull(resultado);
        assertFalse(resultado.reintentoIdempotente());
        EvaluacionAsignacionAyuda evaluacion = resultado.evaluacion();
        assertEquals(1, evaluacion.getRevision());
        assertEquals(familiaVerificada.getId(), evaluacion.getFamilia().getId());
        assertEquals("coord@sirab.local", evaluacion.getUsuarioCorreo());

        // Snapshot de necesidades y situaciones
        assertEquals(Set.of(Necesidad.AGUA, Necesidad.ALIMENTOS), evaluacion.getNecesidades());
        assertEquals(Set.of(SituacionFamiliar.MENORES_DE_CINCO), evaluacion.getSituaciones());

        // Asignaciones generadas:
        // Para AGUA: BASE (10 L) + MENORES_DE_CINCO (5 L) = 2 asignaciones (ADULTOS_MAYORES no aplica)
        // Para ALIMENTOS: BASE (2 KITS) = 1 asignación
        List<AsignacionAyuda> asignaciones = resultado.asignaciones();
        List<AsignacionAyuda> asignacionesAgua = asignaciones.stream()
                .filter(a -> a.getArticulo().getId().equals(articuloAgua.getId()))
                .toList();
        assertEquals(2, asignacionesAgua.size());

        List<AsignacionAyuda> asignacionesAlimento = asignaciones.stream()
                .filter(a -> a.getArticulo().getId().equals(articuloAlimento.getId()))
                .toList();
        assertEquals(1, asignacionesAlimento.size());

        for (AsignacionAyuda a : asignaciones) {
            assertEquals(familiaVerificada.getId(), a.getFamilia().getId());
            assertEquals(evaluacion.getId(), a.getEvaluacion().getId());
            assertNotNull(a.getRegla());
            assertNull(a.getRevisionSanitaria());
            assertEquals(OrigenAsignacionAyuda.REGLA, a.getOrigen());
            assertEquals("coord@sirab.local", a.getUsuarioCorreo());
            assertNotNull(a.getClaveIdempotencia());
            assertTrue(a.getCantidad().compareTo(BigDecimal.ZERO) > 0);
        }

        assertTrue(resultado.necesidadesSinReglas().isEmpty());
    }

    @Test
    void garantizaIdempotenciaAnteReintentosDeMismaClave() {
        String clave = "eval-reintento-" + System.nanoTime();
        EvaluarNecesidadesCommand command = new EvaluarNecesidadesCommand(
                familiaVerificada.getId(),
                clave,
                LocalDate.of(2026, 3, 1),
                "coord@sirab.local");

        ResultadoEvaluacion primerIntento = evaluacionService.evaluarNecesidades(command);
        assertFalse(primerIntento.reintentoIdempotente());
        assertFalse(primerIntento.asignaciones().isEmpty());

        // Segundo intento con la misma clave
        ResultadoEvaluacion segundoIntento = evaluacionService.evaluarNecesidades(command);
        assertTrue(segundoIntento.reintentoIdempotente());
        assertEquals(primerIntento.evaluacion().getId(), segundoIntento.evaluacion().getId());
        assertEquals(primerIntento.asignaciones().size(), segundoIntento.asignaciones().size());

        // Verificar que no se duplicaron registros en base de datos
        List<EvaluacionAsignacionAyuda> evaluaciones =
                evaluacionService.listarEvaluacionesFamilia(familiaVerificada.getId());
        assertEquals(1, evaluaciones.size());

        List<AsignacionAyuda> asignaciones =
                evaluacionService.listarAsignacionesFamilia(familiaVerificada.getId());
        assertEquals(primerIntento.asignaciones().size(), asignaciones.size());
    }

    @Test
    void rechazaMismaClaveIdempotenciaParaFamiliaDiferente() {
        String claveCompartida = "clave-compartida-" + System.nanoTime();
        evaluacionService.evaluarNecesidades(new EvaluarNecesidadesCommand(
                familiaVerificada.getId(),
                claveCompartida,
                LocalDate.of(2026, 3, 1),
                "coord@sirab.local"));

        // Otra familia
        Familia otraFamilia = new Familia();
        otraFamilia.setNombreRegistrante("OTRA FAMILIA TEST");
        otraFamilia.setDireccionReferencia("Referencia Otra Familia");
        otraFamilia.setNumeroIntegrantes(2);
        otraFamilia.setTelefonoContacto("3119876543");
        otraFamilia.setConsentimientoAt(Instant.now());
        otraFamilia.setConsentimientoVersion("v1");
        otraFamilia.setEstado(EstadoRegistro.VERIFICADO);
        otraFamilia.setNecesidades(Set.of(Necesidad.AGUA));
        otraFamilia = familiaRepository.save(otraFamilia);

        EvaluarNecesidadesCommand intentoCruzado = new EvaluarNecesidadesCommand(
                otraFamilia.getId(),
                claveCompartida,
                LocalDate.of(2026, 3, 1),
                "coord@sirab.local");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> evaluacionService.evaluarNecesidades(intentoCruzado));
        assertTrue(ex.getMessage().contains("ya fue utilizada para otra familia"));
    }

    @Test
    void conservaSnapshotHistoricoIndependienteDeCambiosPosteriores() {
        EvaluarNecesidadesCommand command = new EvaluarNecesidadesCommand(
                familiaVerificada.getId(),
                "eval-snap-" + System.nanoTime(),
                LocalDate.of(2026, 3, 1),
                "coord@sirab.local");

        ResultadoEvaluacion resultado = evaluacionService.evaluarNecesidades(command);
        EvaluacionAsignacionAyuda ev = resultado.evaluacion();

        // Modificar necesidades y situaciones de la familia
        familiaVerificada.setNecesidades(Set.of(Necesidad.MEDICAMENTOS));
        familiaVerificada.setSituaciones(Set.of(SituacionFamiliar.ADULTOS_MAYORES));
        familiaRepository.save(familiaVerificada);

        // La evaluación previa conserva su snapshot original
        EvaluacionAsignacionAyuda persistida =
                evaluacionService.obtenerEvaluacionPorId(ev.getId()).orElseThrow();
        assertEquals(Set.of(Necesidad.AGUA, Necesidad.ALIMENTOS), persistida.getNecesidades());
        assertEquals(Set.of(SituacionFamiliar.MENORES_DE_CINCO), persistida.getSituaciones());
    }

    @Test
    void generaSegundaRevisionSinModificarEvaluacionAnterior() {
        ResultadoEvaluacion r1 = evaluacionService.evaluarNecesidades(new EvaluarNecesidadesCommand(
                familiaVerificada.getId(),
                "eval-r1-" + System.nanoTime(),
                LocalDate.of(2026, 3, 1),
                "coord@sirab.local"));
        assertEquals(1, r1.evaluacion().getRevision());

        // Segunda evaluación para la misma familia con nueva clave
        ResultadoEvaluacion r2 = evaluacionService.evaluarNecesidades(new EvaluarNecesidadesCommand(
                familiaVerificada.getId(),
                "eval-r2-" + System.nanoTime(),
                LocalDate.of(2026, 4, 1),
                "coord@sirab.local"));
        assertEquals(2, r2.evaluacion().getRevision());

        List<EvaluacionAsignacionAyuda> evaluaciones =
                evaluacionService.listarEvaluacionesFamilia(familiaVerificada.getId());
        assertEquals(2, evaluaciones.size());
        assertEquals(2, evaluaciones.get(0).getRevision());
        assertEquals(1, evaluaciones.get(1).getRevision());
    }

    @Test
    void noGeneraAsignacionesParaReglasNoVigentesEnLaFecha() {
        // Consultar con fecha anterior al inicio de vigencia (2025-12-01)
        EvaluarNecesidadesCommand command = new EvaluarNecesidadesCommand(
                familiaVerificada.getId(),
                "eval-no-vigente-" + System.nanoTime(),
                LocalDate.of(2025, 12, 1),
                "coord@sirab.local");

        ResultadoEvaluacion resultado = evaluacionService.evaluarNecesidades(command);

        // No hay reglas vigentes para esa fecha
        assertTrue(resultado.asignaciones().isEmpty());
        assertEquals(Set.of(Necesidad.AGUA, Necesidad.ALIMENTOS), resultado.necesidadesSinReglas());
    }

    @Test
    void reportaNecesidadesSinReglasCuandoNoHayArticulosOValoresAplicables() {
        // Familia con necesidad MEDICAMENTOS (para la cual no creamos reglas en setUp)
        familiaVerificada.setNecesidades(Set.of(Necesidad.AGUA, Necesidad.MEDICAMENTOS));
        familiaRepository.save(familiaVerificada);

        ResultadoEvaluacion resultado = evaluacionService.evaluarNecesidades(new EvaluarNecesidadesCommand(
                familiaVerificada.getId(),
                "eval-sin-reglas-" + System.nanoTime(),
                LocalDate.of(2026, 3, 1),
                "coord@sirab.local"));

        assertTrue(resultado.necesidadesSinReglas().contains(Necesidad.MEDICAMENTOS));
        assertFalse(resultado.necesidadesSinReglas().contains(Necesidad.AGUA));
    }

    @Test
    void ignoraAlojamientoEnAsignacionesDeArticulosPorTenerProcesoIndependiente() {
        familiaVerificada.setNecesidades(Set.of(Necesidad.AGUA, Necesidad.ALOJAMIENTO));
        familiaRepository.save(familiaVerificada);

        ResultadoEvaluacion resultado = evaluacionService.evaluarNecesidades(new EvaluarNecesidadesCommand(
                familiaVerificada.getId(),
                "eval-alojamiento-" + System.nanoTime(),
                LocalDate.of(2026, 3, 1),
                "coord@sirab.local"));

        // Se incluye en el snapshot de evaluación
        assertTrue(resultado.evaluacion().getNecesidades().contains(Necesidad.ALOJAMIENTO));
        // No genera asignaciones para alojamiento
        assertTrue(resultado.asignaciones().stream().noneMatch(a -> a.getTipoAyuda() == Necesidad.ALOJAMIENTO));
        // No se reporta como "necesidad sin reglas" porque no es gestionable por catálogo
        assertFalse(resultado.necesidadesSinReglas().contains(Necesidad.ALOJAMIENTO));
    }

    @Test
    void rechazaEvaluacionDeFamiliaEnEstadoPendiente() {
        familiaVerificada.setEstado(EstadoRegistro.PENDIENTE);
        familiaRepository.save(familiaVerificada);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> evaluacionService.evaluarNecesidades(new EvaluarNecesidadesCommand(
                        familiaVerificada.getId(),
                        "eval-pendiente-" + System.nanoTime(),
                        LocalDate.of(2026, 3, 1),
                        "coord@sirab.local")));

        assertTrue(ex.getMessage().contains("VERIFICADO"));
    }

    @Test
    void rechazaEvaluacionDeFamiliaEnEstadoAtendido() {
        familiaVerificada.setEstado(EstadoRegistro.ATENDIDO);
        familiaRepository.save(familiaVerificada);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> evaluacionService.evaluarNecesidades(new EvaluarNecesidadesCommand(
                        familiaVerificada.getId(),
                        "eval-atendido-" + System.nanoTime(),
                        LocalDate.of(2026, 3, 1),
                        "coord@sirab.local")));

        assertTrue(ex.getMessage().contains("VERIFICADO"));
    }

    @Test
    void rechazaEvaluacionSiFamiliaNoTieneNecesidades() {
        familiaVerificada.setNecesidades(Set.of());
        familiaRepository.save(familiaVerificada);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> evaluacionService.evaluarNecesidades(new EvaluarNecesidadesCommand(
                        familiaVerificada.getId(),
                        "eval-vacia-" + System.nanoTime(),
                        LocalDate.of(2026, 3, 1),
                        "coord@sirab.local")));

        assertTrue(ex.getMessage().contains("no tiene necesidades registradas"));
    }

    @Test
    void rechazaParametrosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> evaluacionService.evaluarNecesidades(
                new EvaluarNecesidadesCommand(null, "clave", LocalDate.now(), "coord@sirab.local")));

        assertThrows(IllegalArgumentException.class, () -> evaluacionService.evaluarNecesidades(
                new EvaluarNecesidadesCommand(familiaVerificada.getId(), "", LocalDate.now(), "coord@sirab.local")));

        assertThrows(IllegalArgumentException.class, () -> evaluacionService.evaluarNecesidades(
                new EvaluarNecesidadesCommand(familiaVerificada.getId(), "clave", LocalDate.now(), "")));
    }
}
