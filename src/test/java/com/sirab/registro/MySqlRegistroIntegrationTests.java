package com.sirab.registro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.sirab.registro.service.RegistroFamiliarCommand;
import com.sirab.registro.service.RegistroFamiliarService;
import com.sirab.SirabApplication;
import com.sirab.registro.duplicados.DeteccionDuplicadosService;
import com.sirab.registro.duplicados.EstadoPosibleDuplicado;
import com.sirab.registro.duplicados.PosibleDuplicadoRepository;
import com.sirab.registro.duplicados.RevisionPosibleDuplicadoRepository;
import com.sirab.registro.duplicados.TipoEventoDuplicado;
import com.sirab.registro.SituacionFamiliar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfSystemProperty(named = "sirab.mysql.integration", matches = "true")
class MySqlRegistroIntegrationTests {

    @Autowired
    private RegistroFamiliarService registroFamiliarService;

    @Autowired
    private FamiliaRepository familiaRepository;

    @Autowired
    private PosibleDuplicadoRepository posibleDuplicadoRepository;

    @Autowired
    private RevisionPosibleDuplicadoRepository revisionRepository;

    @Autowired
    private DeteccionDuplicadosService deteccionDuplicadosService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void registraYLeeUnRegistroSinteticoEnLaBaseConfigurada() {
        String syntheticRegistrant = "INTEGRATION_TEST_" + UUID.randomUUID();
        RegistroFamiliarCommand command = new RegistroFamiliarCommand(
                syntheticRegistrant,
                "TEST_BARRIO",
                "TEST_DIRECCION",
                "TEST_REFERENCE",
                1,
                "SYNTHETIC_PHONE",
                Set.of(SituacionFamiliar.MENORES_DE_CINCO),
                Set.of(Necesidad.AGUA),
                true,
                Instant.now());

        registroFamiliarService.registrar(command);

        Familia saved = familiaRepository.findFirstByNombreRegistrante(syntheticRegistrant)
                .orElseThrow();
        assertEquals(1, saved.getNumeroIntegrantes());
        assertEquals("TEST_BARRIO", saved.getBarrio());
        assertEquals("TEST_DIRECCION", saved.getDireccion());
        assertEquals("TEST_REFERENCE", saved.getDireccionReferencia());
        assertEquals("SYNTHETIC_PHONE", saved.getTelefonoContacto());
        assertEquals(Set.of(SituacionFamiliar.MENORES_DE_CINCO), saved.getSituaciones());
        assertEquals(Set.of(Necesidad.AGUA), saved.getNecesidades());
        assertTrue(saved.getConsentimientoAt() != null);
        assertTrue(saved.getConsentimientoVersion() != null);
    }

    @Test
    @Transactional
    void persisteYResuelveCoincidenciaConHistorialEnMySql() {
        String syntheticRegistrant = "DUPLICATE_INTEGRATION_TEST_" + UUID.randomUUID();
        registrarSintetico(syntheticRegistrant, "3001234567", "TEST_BARRIO_A", "TEST_DIRECCION_A");
        registrarSintetico(syntheticRegistrant, "3001234567", "TEST_BARRIO_B", "TEST_DIRECCION_B");

        Familia primero = familiaRepository.findFirstByNombreRegistrante(syntheticRegistrant).orElseThrow();
        var duplicado = posibleDuplicadoRepository.findAllByFamiliaId(primero.getId()).stream()
                .findFirst()
                .orElseThrow();
        assertEquals(EstadoPosibleDuplicado.PENDIENTE_REVISION, duplicado.getEstado());
        assertEquals("NOMBRE,TELEFONO", duplicado.getSenalesCoincidentes());

        assertTrue(deteccionDuplicadosService.resolver(
                duplicado.getId(), EstadoPosibleDuplicado.DESCARTADO, "mysql-integration@sirab.local"));
        assertEquals(EstadoPosibleDuplicado.DESCARTADO,
                posibleDuplicadoRepository.findById(duplicado.getId()).orElseThrow().getEstado());
        assertEquals(TipoEventoDuplicado.DESCARTADO,
                revisionRepository.findByPosibleDuplicado_IdOrderByFechaHoraAscIdAsc(duplicado.getId())
                        .get(0).getEvento());
    }

    @Test
    void detectaRegistrosConcurrentesEntreDosInstanciasConMySql() throws Exception {
        String syntheticRegistrant = "CROSS_INSTANCE_TEST_" + UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);

        try (ConfigurableApplicationContext segundaInstancia = new SpringApplicationBuilder(SirabApplication.class)
                .profiles("dev")
                .properties("server.port=0")
                .run()) {
            RegistroFamiliarService servicioSegundaInstancia =
                    segundaInstancia.getBean(RegistroFamiliarService.class);
            Future<?> primerRegistro = executor.submit(() -> {
                if (!inicio.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("No se inició la primera operación concurrente.");
                }
                registrarSintetico(syntheticRegistrant, "3001234567", "TEST_BARRIO_A", "TEST_DIRECCION_A");
                return null;
            });
            Future<?> segundoRegistro = executor.submit(() -> {
                if (!inicio.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("No se inició la segunda operación concurrente.");
                }
                servicioSegundaInstancia.registrar(new RegistroFamiliarCommand(
                        syntheticRegistrant,
                        "TEST_BARRIO_B",
                        "TEST_DIRECCION_B",
                        "TEST_REFERENCE",
                        1,
                        "3001234567",
                        Set.of(SituacionFamiliar.MENORES_DE_CINCO),
                        Set.of(Necesidad.AGUA),
                        true,
                        Instant.now()));
                return null;
            });

            inicio.countDown();
            primerRegistro.get(45, TimeUnit.SECONDS);
            segundoRegistro.get(45, TimeUnit.SECONDS);

            var ids = jdbcTemplate.queryForList(
                    "SELECT id FROM familia WHERE nombre_registrante = ? ORDER BY id",
                    Long.class,
                    syntheticRegistrant);
            assertEquals(2, ids.size());
            assertEquals(1, posibleDuplicadoRepository.findAllByFamiliaId(ids.get(0)).size());
        } finally {
            inicio.countDown();
            executor.shutdownNow();
            limpiarRegistrosSinteticos(syntheticRegistrant);
        }
    }

    private void limpiarRegistrosSinteticos(String nombre) {
        String ids = "SELECT id FROM familia WHERE nombre_registrante = ?";
        jdbcTemplate.update("""
                DELETE FROM historial_posible_duplicado
                WHERE posible_duplicado_id IN (
                    SELECT id FROM posible_duplicado
                    WHERE familia_menor_id IN (%s) OR familia_mayor_id IN (%s))
                """.formatted(ids, ids), nombre, nombre);
        jdbcTemplate.update("""
                DELETE FROM posible_duplicado
                WHERE familia_menor_id IN (%s) OR familia_mayor_id IN (%s)
                """.formatted(ids, ids), nombre, nombre);
        jdbcTemplate.update("DELETE FROM historial_estado_solicitud WHERE familia_id IN (" + ids + ")", nombre);
        jdbcTemplate.update("DELETE FROM familia_necesidad WHERE familia_id IN (" + ids + ")", nombre);
        jdbcTemplate.update("DELETE FROM familia_situacion WHERE familia_id IN (" + ids + ")", nombre);
        jdbcTemplate.update("DELETE FROM familia WHERE nombre_registrante = ?", nombre);
    }

    private void registrarSintetico(String nombre, String telefono, String barrio, String direccion) {
        registroFamiliarService.registrar(new RegistroFamiliarCommand(
                nombre,
                barrio,
                direccion,
                "TEST_REFERENCE",
                1,
                telefono,
                Set.of(SituacionFamiliar.MENORES_DE_CINCO),
                Set.of(Necesidad.AGUA),
                true,
                Instant.now()));
    }
}
