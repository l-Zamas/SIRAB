package com.sirab.registro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sirab.registro.ArticuloAyuda;
import com.sirab.registro.ArticuloAyudaRepository;
import com.sirab.registro.Necesidad;
import com.sirab.registro.ReglaAsignacionAyuda;
import com.sirab.registro.ReglaAsignacionAyudaRepository;
import com.sirab.registro.SituacionFamiliar;
import com.sirab.registro.TipoCriterioAsignacion;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogoYReglasIntegrationTests {

    @Autowired
    private CatalogoArticuloService catalogoService;

    @Autowired
    private ReglaAsignacionService reglaService;

    @Autowired
    private ArticuloAyudaRepository articuloRepository;

    @Autowired
    private ReglaAsignacionAyudaRepository reglaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void flujoCompletoDeCicloDeVidaDeCatalogoYReglasVersionadas() {
        // 1. Crear un artículo de ayuda
        ArticuloAyuda articulo = catalogoService.registrarArticulo(new RegistrarArticuloCommand(
                "KIT-INTEGRACION-" + System.nanoTime(),
                "Kit Nutricional de Emergencia",
                Necesidad.ALIMENTOS,
                "KITS",
                "coord@sirab.local"));
        entityManager.flush();

        // 2. Publicar versión 1 con BASE y 2 situaciones familiares
        reglaService.publicarVersion(new PublicarVersionReglasCommand(
                articulo.getId(),
                LocalDate.of(2026, 1, 1),
                null,
                List.of(
                        CriterioReglaCommand.base(new BigDecimal("1.000000")),
                        CriterioReglaCommand.situacionFamiliar(
                                SituacionFamiliar.MENORES_DE_CINCO, new BigDecimal("1.000000")),
                        CriterioReglaCommand.situacionFamiliar(
                                SituacionFamiliar.MUJERES_GESTANTES, new BigDecimal("0.500000"))),
                "coord@sirab.local"));
        entityManager.flush();
        entityManager.clear();

        // Verificar v1 en BD
        List<ReglaAsignacionAyuda> v1Reglas = reglaRepository.findByArticuloIdAndVersionOrderByCriterioCodigoAsc(
                articulo.getId(), 1);
        assertEquals(3, v1Reglas.size());
        assertTrue(v1Reglas.stream().allMatch(r -> r.getVigenteHasta() == null));

        // 3. Publicar versión 2 desde el 1 de julio de 2026
        reglaService.publicarVersion(new PublicarVersionReglasCommand(
                articulo.getId(),
                LocalDate.of(2026, 7, 1),
                null,
                List.of(
                        CriterioReglaCommand.base(new BigDecimal("2.000000")),
                        CriterioReglaCommand.situacionFamiliar(
                                SituacionFamiliar.MENORES_DE_CINCO, new BigDecimal("1.500000"))),
                "coord@sirab.local"));
        entityManager.flush();
        entityManager.clear();

        // Verificar que la versión 1 ahora tiene vigenteHasta = 2026-06-30
        List<ReglaAsignacionAyuda> v1Cerrada = reglaRepository.findByArticuloIdAndVersionOrderByCriterioCodigoAsc(
                articulo.getId(), 1);
        assertEquals(3, v1Cerrada.size());
        assertTrue(v1Cerrada.stream().allMatch(r -> LocalDate.of(2026, 6, 30).equals(r.getVigenteHasta())));

        // Verificar que la versión 2 tiene vigenteHasta == null
        List<ReglaAsignacionAyuda> v2Abierta = reglaRepository.findByArticuloIdAndVersionOrderByCriterioCodigoAsc(
                articulo.getId(), 2);
        assertEquals(2, v2Abierta.size());
        assertTrue(v2Abierta.stream().allMatch(r -> r.getVigenteHasta() == null));

        // 4. Verificar consultas temporales
        // Fecha en enero: debe dar v1 (3 criterios)
        List<ReglaAsignacionAyuda> vigentesEnero = reglaService.consultarReglasVigentes(
                articulo.getId(), LocalDate.of(2026, 1, 15));
        assertEquals(3, vigentesEnero.size());
        assertEquals(1, vigentesEnero.get(0).getVersion());

        // Fecha en agosto: debe dar v2 (2 criterios)
        List<ReglaAsignacionAyuda> vigentesAgosto = reglaService.consultarReglasVigentes(
                articulo.getId(), LocalDate.of(2026, 8, 1));
        assertEquals(2, vigentesAgosto.size());
        assertEquals(2, vigentesAgosto.get(0).getVersion());

        // 5. Desactivar el artículo y verificar que no permite nuevas versiones
        catalogoService.desactivar(articulo.getId(), "coord@sirab.local");
        entityManager.flush();

        assertThrows(IllegalStateException.class, () -> reglaService.publicarVersion(new PublicarVersionReglasCommand(
                articulo.getId(),
                LocalDate.of(2026, 12, 1),
                null,
                List.of(CriterioReglaCommand.base(BigDecimal.ONE)),
                "coord@sirab.local")));
    }

    @Test
    void verificaRestriccionDeIntegridadUnicidadVersionCriterioEnBaseDeDatos() {
        ArticuloAyuda articulo = catalogoService.registrarArticulo(new RegistrarArticuloCommand(
                "AGUA-INTEG-ERR-" + System.nanoTime(),
                "Agua bidón",
                Necesidad.AGUA,
                "LITROS",
                "admin@sirab.local"));
        entityManager.flush();

        // Persistir una regla BASE
        ReglaAsignacionAyuda r1 = new ReglaAsignacionAyuda(
                articulo,
                1,
                TipoCriterioAsignacion.BASE,
                null,
                BigDecimal.TEN,
                "LITROS",
                LocalDate.of(2026, 1, 1),
                null,
                java.time.Instant.now(),
                "coord@sirab.local");
        entityManager.persist(r1);
        entityManager.flush();

        // Intentar persistir otra regla BASE en la misma versión directamente
        ReglaAsignacionAyuda r2 = new ReglaAsignacionAyuda(
                articulo,
                1,
                TipoCriterioAsignacion.BASE,
                null,
                BigDecimal.ONE,
                "LITROS",
                LocalDate.of(2026, 1, 1),
                null,
                java.time.Instant.now(),
                "coord@sirab.local");

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(r2);
            entityManager.flush();
        });
    }
}
