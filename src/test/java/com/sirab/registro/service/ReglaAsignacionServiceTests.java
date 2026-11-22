package com.sirab.registro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sirab.registro.ArticuloAyuda;
import com.sirab.registro.Necesidad;
import com.sirab.registro.ReglaAsignacionAyuda;
import com.sirab.registro.SituacionFamiliar;
import com.sirab.registro.TipoCriterioAsignacion;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReglaAsignacionServiceTests {

    @Autowired
    private ReglaAsignacionService reglaService;

    @Autowired
    private CatalogoArticuloService catalogoService;

    private ArticuloAyuda articuloAgua;

    @BeforeEach
    void setUp() {
        articuloAgua = catalogoService.registrarArticulo(new RegistrarArticuloCommand(
                "AGUA-REGLAS-TEST-" + System.nanoTime(),
                "Agua Potable 5L",
                Necesidad.AGUA,
                "LITROS",
                "coord@sirab.local"));
    }

    @Test
    void publicaVersionInicialConCriterioBaseYSituacionesFamiliares() {
        PublicarVersionReglasCommand command = new PublicarVersionReglasCommand(
                articuloAgua.getId(),
                LocalDate.of(2026, 1, 1),
                null,
                List.of(
                        CriterioReglaCommand.base(new BigDecimal("10.000000")),
                        CriterioReglaCommand.situacionFamiliar(
                                SituacionFamiliar.MENORES_DE_CINCO, new BigDecimal("5.000000")),
                        CriterioReglaCommand.situacionFamiliar(
                                SituacionFamiliar.ADULTOS_MAYORES, new BigDecimal("3.000000"))),
                "coord@sirab.local");

        List<ReglaAsignacionAyuda> reglas = reglaService.publicarVersion(command);

        assertEquals(3, reglas.size());
        for (ReglaAsignacionAyuda r : reglas) {
            assertEquals(1, r.getVersion());
            assertEquals(articuloAgua.getId(), r.getArticulo().getId());
            assertEquals("LITROS", r.getUnidad());
            assertEquals(LocalDate.of(2026, 1, 1), r.getVigenteDesde());
            assertNull(r.getVigenteHasta());
            assertEquals("coord@sirab.local", r.getCreadoPor());
            assertNotNull(r.getCreadoAt());
        }

        assertEquals(1, reglaService.obtenerUltimaVersion(articuloAgua.getId()));
    }

    @Test
    void publicaSegundaVersionYAcotaVigenciaDeVersionPrevia() {
        PublicarVersionReglasCommand v1 = new PublicarVersionReglasCommand(
                articuloAgua.getId(),
                LocalDate.of(2026, 1, 1),
                null,
                List.of(CriterioReglaCommand.base(new BigDecimal("10.000000"))),
                "coord@sirab.local");
        reglaService.publicarVersion(v1);

        PublicarVersionReglasCommand v2 = new PublicarVersionReglasCommand(
                articuloAgua.getId(),
                LocalDate.of(2026, 6, 1),
                null,
                List.of(
                        CriterioReglaCommand.base(new BigDecimal("15.000000")),
                        CriterioReglaCommand.situacionFamiliar(
                                SituacionFamiliar.MENORES_DE_CINCO, new BigDecimal("4.000000"))),
                "coord@sirab.local");
        List<ReglaAsignacionAyuda> reglasV2 = reglaService.publicarVersion(v2);

        assertEquals(2, reglasV2.get(0).getVersion());
        assertEquals(2, reglaService.obtenerUltimaVersion(articuloAgua.getId()));

        // Verificar que la versión 1 quedó acotada a 2026-05-31
        List<ReglaAsignacionAyuda> reglasV1 = reglaService.consultarVersion(articuloAgua.getId(), 1);
        assertEquals(1, reglasV1.size());
        assertEquals(LocalDate.of(2026, 5, 31), reglasV1.get(0).getVigenteHasta());

        // Consultar vigencia en fecha intermedia (2026-03-15) debe dar la v1
        List<ReglaAsignacionAyuda> vigentesMarzo =
                reglaService.consultarReglasVigentes(articuloAgua.getId(), LocalDate.of(2026, 3, 15));
        assertEquals(1, vigentesMarzo.size());
        assertEquals(1, vigentesMarzo.get(0).getVersion());
        assertEquals(new BigDecimal("10.000000"), vigentesMarzo.get(0).getCantidad());

        // Consultar vigencia a partir de 2026-06-01 debe dar la v2
        List<ReglaAsignacionAyuda> vigentesJunio =
                reglaService.consultarReglasVigentes(articuloAgua.getId(), LocalDate.of(2026, 6, 1));
        assertEquals(2, vigentesJunio.size());
        assertEquals(2, vigentesJunio.get(0).getVersion());
    }

    @Test
    void rechazaPublicacionSiArticuloNoExiste() {
        PublicarVersionReglasCommand command = new PublicarVersionReglasCommand(
                999999L,
                LocalDate.of(2026, 1, 1),
                null,
                List.of(CriterioReglaCommand.base(BigDecimal.TEN)),
                "coord@sirab.local");

        assertThrows(IllegalArgumentException.class, () -> reglaService.publicarVersion(command));
    }

    @Test
    void rechazaPublicacionSiArticuloEstaInactivo() {
        catalogoService.desactivar(articuloAgua.getId(), "admin@sirab.local");

        PublicarVersionReglasCommand command = new PublicarVersionReglasCommand(
                articuloAgua.getId(),
                LocalDate.of(2026, 1, 1),
                null,
                List.of(CriterioReglaCommand.base(BigDecimal.TEN)),
                "coord@sirab.local");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class, () -> reglaService.publicarVersion(command));
        assertTrue(ex.getMessage().contains("inactivo"));
    }

    @Test
    void rechazaVersionSinCriterioBase() {
        PublicarVersionReglasCommand command = new PublicarVersionReglasCommand(
                articuloAgua.getId(),
                LocalDate.of(2026, 1, 1),
                null,
                List.of(CriterioReglaCommand.situacionFamiliar(
                        SituacionFamiliar.MUJERES_GESTANTES, BigDecimal.valueOf(5))),
                "coord@sirab.local");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> reglaService.publicarVersion(command));
        assertTrue(ex.getMessage().contains("criterio BASE"));
    }

    @Test
    void rechazaCriteriosDuplicadosEnLaMismaVersion() {
        PublicarVersionReglasCommand command = new PublicarVersionReglasCommand(
                articuloAgua.getId(),
                LocalDate.of(2026, 1, 1),
                null,
                List.of(
                        CriterioReglaCommand.base(BigDecimal.TEN),
                        CriterioReglaCommand.base(BigDecimal.valueOf(12))),
                "coord@sirab.local");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> reglaService.publicarVersion(command));
        assertTrue(ex.getMessage().contains("Criterio duplicado"));
    }

    @Test
    void rechazaCriterioSituacionFamiliarDuplicada() {
        PublicarVersionReglasCommand command = new PublicarVersionReglasCommand(
                articuloAgua.getId(),
                LocalDate.of(2026, 1, 1),
                null,
                List.of(
                        CriterioReglaCommand.base(BigDecimal.TEN),
                        CriterioReglaCommand.situacionFamiliar(
                                SituacionFamiliar.MENORES_DE_CINCO, BigDecimal.valueOf(3)),
                        CriterioReglaCommand.situacionFamiliar(
                                SituacionFamiliar.MENORES_DE_CINCO, BigDecimal.valueOf(4))),
                "coord@sirab.local");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> reglaService.publicarVersion(command));
        assertTrue(ex.getMessage().contains("Criterio duplicado"));
    }

    @Test
    void rechazaCriterioBaseConSituacionFamiliar() {
        CriterioReglaCommand invalido = new CriterioReglaCommand(
                TipoCriterioAsignacion.BASE, SituacionFamiliar.ADULTOS_MAYORES, BigDecimal.ONE);

        PublicarVersionReglasCommand command = new PublicarVersionReglasCommand(
                articuloAgua.getId(),
                LocalDate.of(2026, 1, 1),
                null,
                List.of(invalido),
                "coord@sirab.local");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> reglaService.publicarVersion(command));
        assertTrue(ex.getMessage().contains("no debe incluir una situación familiar"));
    }

    @Test
    void rechazaCriterioSituacionFamiliarSinSituacion() {
        CriterioReglaCommand invalido = new CriterioReglaCommand(
                TipoCriterioAsignacion.SITUACION_FAMILIAR, null, BigDecimal.ONE);

        PublicarVersionReglasCommand command = new PublicarVersionReglasCommand(
                articuloAgua.getId(),
                LocalDate.of(2026, 1, 1),
                null,
                List.of(CriterioReglaCommand.base(BigDecimal.TEN), invalido),
                "coord@sirab.local");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> reglaService.publicarVersion(command));
        assertTrue(ex.getMessage().contains("debe especificar la situación familiar"));
    }

    @Test
    void rechazaCantidadCeroONegativa() {
        assertThrows(IllegalArgumentException.class, () -> reglaService.publicarVersion(
                new PublicarVersionReglasCommand(
                        articuloAgua.getId(),
                        LocalDate.of(2026, 1, 1),
                        null,
                        List.of(CriterioReglaCommand.base(BigDecimal.ZERO)),
                        "coord@sirab.local")));

        assertThrows(IllegalArgumentException.class, () -> reglaService.publicarVersion(
                new PublicarVersionReglasCommand(
                        articuloAgua.getId(),
                        LocalDate.of(2026, 1, 1),
                        null,
                        List.of(CriterioReglaCommand.base(new BigDecimal("-1.5"))),
                        "coord@sirab.local")));
    }

    @Test
    void rechazaVigenciaHastaAnteriorAVigenciaDesde() {
        PublicarVersionReglasCommand command = new PublicarVersionReglasCommand(
                articuloAgua.getId(),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 5, 31),
                List.of(CriterioReglaCommand.base(BigDecimal.TEN)),
                "coord@sirab.local");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> reglaService.publicarVersion(command));
        assertTrue(ex.getMessage().contains("fin de vigencia no puede ser anterior"));
    }

    @Test
    void rechazaNuevaVersionConFechaInicioAnteriorOIgualALaPrevia() {
        reglaService.publicarVersion(new PublicarVersionReglasCommand(
                articuloAgua.getId(),
                LocalDate.of(2026, 5, 1),
                null,
                List.of(CriterioReglaCommand.base(BigDecimal.TEN)),
                "coord@sirab.local"));

        // Misma fecha
        assertThrows(IllegalArgumentException.class, () -> reglaService.publicarVersion(
                new PublicarVersionReglasCommand(
                        articuloAgua.getId(),
                        LocalDate.of(2026, 5, 1),
                        null,
                        List.of(CriterioReglaCommand.base(BigDecimal.valueOf(12))),
                        "coord@sirab.local")));

        // Fecha anterior
        assertThrows(IllegalArgumentException.class, () -> reglaService.publicarVersion(
                new PublicarVersionReglasCommand(
                        articuloAgua.getId(),
                        LocalDate.of(2026, 4, 30),
                        null,
                        List.of(CriterioReglaCommand.base(BigDecimal.valueOf(12))),
                        "coord@sirab.local")));
    }

    @Test
    void consultaHistorialDeVersionesOrdenado() {
        reglaService.publicarVersion(new PublicarVersionReglasCommand(
                articuloAgua.getId(),
                LocalDate.of(2026, 1, 1),
                null,
                List.of(CriterioReglaCommand.base(BigDecimal.TEN)),
                "coord@sirab.local"));

        reglaService.publicarVersion(new PublicarVersionReglasCommand(
                articuloAgua.getId(),
                LocalDate.of(2026, 6, 1),
                null,
                List.of(
                        CriterioReglaCommand.base(BigDecimal.valueOf(12)),
                        CriterioReglaCommand.situacionFamiliar(
                                SituacionFamiliar.PERSONAS_CON_DISCAPACIDAD, BigDecimal.valueOf(3))),
                "coord@sirab.local"));

        List<ReglaAsignacionAyuda> historial = reglaService.consultarHistorial(articuloAgua.getId());
        assertEquals(3, historial.size());
        assertEquals(2, historial.get(0).getVersion());
        assertEquals(2, historial.get(1).getVersion());
        assertEquals(1, historial.get(2).getVersion());
    }

    @Test
    void consultaTodasLasReglasVigentesDeArticulosActivos() {
        ArticuloAyuda comida = catalogoService.registrarArticulo(new RegistrarArticuloCommand(
                "COMIDA-VIG-" + System.nanoTime(),
                "Kit Alimentos",
                Necesidad.ALIMENTOS,
                "KITS",
                "coord@sirab.local"));

        reglaService.publicarVersion(new PublicarVersionReglasCommand(
                articuloAgua.getId(),
                LocalDate.of(2026, 1, 1),
                null,
                List.of(CriterioReglaCommand.base(BigDecimal.valueOf(10))),
                "coord@sirab.local"));

        reglaService.publicarVersion(new PublicarVersionReglasCommand(
                comida.getId(),
                LocalDate.of(2026, 1, 1),
                null,
                List.of(CriterioReglaCommand.base(BigDecimal.valueOf(1))),
                "coord@sirab.local"));

        List<ReglaAsignacionAyuda> vigentes =
                reglaService.consultarTodasLasReglasVigentes(LocalDate.of(2026, 2, 1));

        assertTrue(vigentes.stream().anyMatch(r -> r.getArticulo().getId().equals(articuloAgua.getId())));
        assertTrue(vigentes.stream().anyMatch(r -> r.getArticulo().getId().equals(comida.getId())));
    }
}
