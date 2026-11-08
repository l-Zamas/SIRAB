package com.sirab.registro.duplicados;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.FamiliaRepository;
import com.sirab.registro.Necesidad;
import com.sirab.registro.SituacionFamiliar;
import com.sirab.registro.service.RegistroFamiliarCommand;
import com.sirab.registro.service.RegistroFamiliarService;
import com.sirab.registro.service.SolicitudVerificacionCommand;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class DeteccionDuplicadosIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistroFamiliarService registroFamiliarService;

    @Autowired
    private DeteccionDuplicadosService deteccionDuplicadosService;

    @Autowired
    private IndiceDuplicadosInitializer indiceDuplicadosInitializer;

    @Autowired
    private FamiliaRepository familiaRepository;

    @Autowired
    private PosibleDuplicadoRepository posibleDuplicadoRepository;

    @Autowired
    private RevisionPosibleDuplicadoRepository revisionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BloqueoDeteccionDuplicados bloqueo;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @Transactional
    void detectaSinBloquearRegistrosYRestringeLaRevisionAlPersonalDeCampo() throws Exception {
        registroFamiliarService.registrar(registro(
                "María Pérez", "+57 300 123 4567", "Centro", "Calle 1 # 2-3"));
        registroFamiliarService.registrar(registro(
                "MARIA PEREZ", "300-123-4567", "Otro barrio", "Otra dirección"));

        assertEquals(2, familiaRepository.count());
        List<PosibleDuplicado> posibles = posibleDuplicadoRepository.findAll();
        assertEquals(1, posibles.size());
        PosibleDuplicado posible = posibles.get(0);
        assertEquals(EstadoPosibleDuplicado.PENDIENTE_REVISION, posible.getEstado());

        mockMvc.perform(get("/campo/duplicados").with(user("ciudadano").roles("CIUDADANO")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/campo/duplicados/{id}/confirmar", posible.getId())
                        .with(user("ciudadano").roles("CIUDADANO"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/campo/duplicados").with(user("personal").roles("PERSONAL_CAMPO")))
                .andExpect(status().isOk())
                .andExpect(view().name("campo/duplicados"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("María Pérez")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("300-123-4567")));

        mockMvc.perform(post("/campo/duplicados/{id}/descartar", posible.getId())
                        .with(user("personal").roles("PERSONAL_CAMPO"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertEquals(EstadoPosibleDuplicado.DESCARTADO,
                posibleDuplicadoRepository.findById(posible.getId()).orElseThrow().getEstado());
        assertEquals(1, revisionRepository.findByPosibleDuplicado_IdOrderByFechaHoraAscIdAsc(posible.getId()).size());
        assertEquals("personal",
                revisionRepository.findByPosibleDuplicado_IdOrderByFechaHoraAscIdAsc(posible.getId())
                        .get(0).getActorCorreo());
        DeteccionDuplicadosService.PosibleDuplicadoVista vista = deteccionDuplicadosService
                .listar(PageRequest.of(0, 20)).getContent().stream()
                .filter(duplicado -> duplicado.id().equals(posible.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, vista.historial().size());
        assertEquals(2, familiaRepository.count());
    }

    @Test
    @Transactional
    void confirmarNoFusionaNiCambiaElEstadoDeLasSolicitudes() throws Exception {
        registroFamiliarService.registrar(registro(
                "Familia Confirmada", "3001234567", "Centro", "Calle 1"));
        registroFamiliarService.registrar(registro(
                "Familia Confirmada", "3001234567", "Centro", "Calle 1"));
        PosibleDuplicado posible = posibleDuplicadoRepository.findAll().get(0);

        mockMvc.perform(post("/campo/duplicados/{id}/confirmar", posible.getId())
                        .with(user("personal").roles("PERSONAL_CAMPO"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertEquals(EstadoPosibleDuplicado.DUPLICADO_CONFIRMADO,
                posibleDuplicadoRepository.findById(posible.getId()).orElseThrow().getEstado());
        assertEquals(2, familiaRepository.count());
        assertTrue(familiaRepository.findAll().stream()
                .allMatch(familia -> familia.getEstado() == EstadoRegistro.PENDIENTE));
        assertEquals(TipoEventoDuplicado.CONFIRMADO,
                revisionRepository.findByPosibleDuplicado_IdOrderByFechaHoraAscIdAsc(posible.getId())
                        .get(0).getEvento());
    }

    @Test
    @Transactional
    void reevaluaCambiosDeDatosSinCompararLaFamiliaConsigoMisma() {
        registroFamiliarService.registrar(registro(
                "Familia Compartida", "3001234567", "Centro", "Calle 1"));
        registroFamiliarService.registrar(registro(
                "Familia Compartida", "3001234567", "Centro", "Calle 1"));
        List<com.sirab.registro.Familia> familias = familiaRepository.findAll();
        com.sirab.registro.Familia primera = familias.get(0);
        com.sirab.registro.Familia segunda = familias.get(1);
        Long posibleId = posibleDuplicadoRepository.findAll().get(0).getId();

        registroFamiliarService.verificar(
                primera.getId(), "personal@sirab.local",
                verificacion("Familia Compartida", "3117654321", "Sur", "Carrera 8"));

        assertEquals(EstadoPosibleDuplicado.SIN_COINCIDENCIA,
                posibleDuplicadoRepository.findById(posibleId).orElseThrow().getEstado());
        assertEquals(EstadoRegistro.VERIFICADO,
                familiaRepository.findById(primera.getId()).orElseThrow().getEstado());

        registroFamiliarService.verificar(
                segunda.getId(), "personal@sirab.local",
                verificacion("Familia Compartida", "311 765 4321", "Norte", "Diagonal 4"));

        PosibleDuplicado reabierto = posibleDuplicadoRepository.findById(posibleId).orElseThrow();
        assertEquals(EstadoPosibleDuplicado.PENDIENTE_REVISION, reabierto.getEstado());
        assertEquals(List.of(
                        TipoEventoDuplicado.RETIRADO_POR_CAMBIO_DE_DATOS,
                        TipoEventoDuplicado.REABIERTO_POR_CAMBIO_DE_DATOS),
                revisionRepository.findByPosibleDuplicado_IdOrderByFechaHoraAscIdAsc(posibleId).stream()
                        .map(RevisionPosibleDuplicado::getEvento)
                        .toList());
        assertEquals(1, posibleDuplicadoRepository.findAll().size());
        assertTrue(reabierto.getFamiliaMenor().getId() != reabierto.getFamiliaMayor().getId());
    }

    @Test
    @Transactional
    void reabreUnaConfirmacionCuandoCambianLosDatosYConservaLasDecisiones() throws Exception {
        registroFamiliarService.registrar(registro(
                "Familia Confirmada", "3001234567", "Centro", "Calle 1"));
        registroFamiliarService.registrar(registro(
                "Familia Confirmada", "3001234567", "Centro", "Calle 1"));
        PosibleDuplicado posible = posibleDuplicadoRepository.findAll().get(0);

        mockMvc.perform(post("/campo/duplicados/{id}/confirmar", posible.getId())
                        .with(user("personal").roles("PERSONAL_CAMPO"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        registroFamiliarService.verificar(
                posible.getFamiliaMenor().getId(), "personal@sirab.local",
                verificacion("Familia Confirmada", "3001234567", "Sur", "Carrera 8"));

        assertEquals(EstadoPosibleDuplicado.PENDIENTE_REVISION,
                posibleDuplicadoRepository.findById(posible.getId()).orElseThrow().getEstado());
        assertEquals(List.of(
                        TipoEventoDuplicado.CONFIRMADO,
                        TipoEventoDuplicado.REABIERTO_POR_CAMBIO_DE_DATOS),
                revisionRepository.findByPosibleDuplicado_IdOrderByFechaHoraAscIdAsc(posible.getId()).stream()
                        .map(RevisionPosibleDuplicado::getEvento)
                        .toList());
    }

    @Test
    @Transactional
    void reconciliaFamiliasHistoricasIdempotentementeDuranteElArranque() {
        registroFamiliarService.registrar(registro(
                "Familia Histórica", "3001234567", "Centro", "Calle 1"));
        registroFamiliarService.registrar(registro(
                "Familia Histórica", "3001234567", "Norte", "Carrera 8"));
        List<com.sirab.registro.Familia> familias = familiaRepository.findAll();
        Long primero = familias.get(0).getId();
        Long segundo = familias.get(1).getId();
        Long posibleId = posibleDuplicadoRepository.findAllByFamiliaId(primero).get(0).getId();

        jdbcTemplate.update("DELETE FROM historial_posible_duplicado WHERE posible_duplicado_id = ?", posibleId);
        jdbcTemplate.update("DELETE FROM posible_duplicado WHERE id = ?", posibleId);
        jdbcTemplate.update("""
                UPDATE familia SET duplicado_nombre_normalizado = NULL,
                    duplicado_telefono_normalizado = NULL, duplicado_ubicacion_normalizada = NULL
                WHERE id IN (?, ?)
                """, primero, segundo);
        entityManager.clear();

        indiceDuplicadosInitializer.run(new DefaultApplicationArguments(new String[0]));
        indiceDuplicadosInitializer.run(new DefaultApplicationArguments(new String[0]));

        assertEquals("familia historica",
                familiaRepository.findById(primero).orElseThrow().getDuplicadoNombreNormalizado());
        assertEquals(1, posibleDuplicadoRepository.findAllByFamiliaId(primero).size());
        assertEquals(0, revisionRepository.findAllByPosibleDuplicadoIds(
                posibleDuplicadoRepository.findAllByFamiliaId(primero).stream()
                        .map(PosibleDuplicado::getId).toList()).size());
    }

    @Test
    @Transactional
    void conservaTodoElHistorialEnCiclosDeConfirmacionRetiroYReapertura() {
        registroFamiliarService.registrar(registro(
                "Familia Ciclos", "3001234567", "Centro", "Calle 1"));
        registroFamiliarService.registrar(registro(
                "Familia Ciclos", "3001234567", "Norte", "Carrera 8"));
        PosibleDuplicado posible = posibleDuplicadoRepository.findAll().get(0);
        Long idMenor = posible.getFamiliaMenor().getId();

        assertTrue(deteccionDuplicadosService.resolver(
                posible.getId(), EstadoPosibleDuplicado.DUPLICADO_CONFIRMADO, "revisor@sirab.local"));
        cambiarDatosComparacion(idMenor, "Familia Ciclos", "3117654321");
        assertEquals(EstadoPosibleDuplicado.SIN_COINCIDENCIA,
                posibleDuplicadoRepository.findById(posible.getId()).orElseThrow().getEstado());

        cambiarDatosComparacion(idMenor, "Familia Ciclos", "3001234567");
        assertTrue(deteccionDuplicadosService.resolver(
                posible.getId(), EstadoPosibleDuplicado.DUPLICADO_CONFIRMADO, "revisor@sirab.local"));
        cambiarDatosComparacion(idMenor, "Familia Actualizada", "3001234567");
        assertEquals(EstadoPosibleDuplicado.SIN_COINCIDENCIA,
                posibleDuplicadoRepository.findById(posible.getId()).orElseThrow().getEstado());

        cambiarDatosComparacion(idMenor, "Familia Ciclos", "3001234567");
        assertTrue(deteccionDuplicadosService.resolver(
                posible.getId(), EstadoPosibleDuplicado.DESCARTADO, "revisor@sirab.local"));
        cambiarDatosComparacion(idMenor, "Familia Actualizada", "3001234567");
        cambiarDatosComparacion(idMenor, "Familia Ciclos", "3001234567");

        assertEquals(EstadoPosibleDuplicado.PENDIENTE_REVISION,
                posibleDuplicadoRepository.findById(posible.getId()).orElseThrow().getEstado());
        assertEquals(List.of(
                        TipoEventoDuplicado.CONFIRMADO,
                        TipoEventoDuplicado.RETIRADO_POR_CAMBIO_DE_DATOS,
                        TipoEventoDuplicado.REABIERTO_POR_CAMBIO_DE_DATOS,
                        TipoEventoDuplicado.CONFIRMADO,
                        TipoEventoDuplicado.RETIRADO_POR_CAMBIO_DE_DATOS,
                        TipoEventoDuplicado.REABIERTO_POR_CAMBIO_DE_DATOS,
                        TipoEventoDuplicado.DESCARTADO,
                        TipoEventoDuplicado.REABIERTO_POR_CAMBIO_DE_DATOS),
                revisionRepository.findByPosibleDuplicado_IdOrderByFechaHoraAscIdAsc(posible.getId()).stream()
                        .map(RevisionPosibleDuplicado::getEvento)
                        .toList());
    }

    @Test
    @Transactional
    void paginaElListadoDeCoincidencias() throws Exception {
        for (int indice = 0; indice < 21; indice++) {
            String nombre = "Familia Página " + indice;
            String telefono = "3001234" + String.format("%03d", indice);
            registroFamiliarService.registrar(registro(nombre, telefono, "Centro", "Calle 1"));
            registroFamiliarService.registrar(registro(nombre, telefono, "Norte", "Carrera 8"));
        }

        var primeraPagina = deteccionDuplicadosService.listar(PageRequest.of(0, 20));
        var segundaPagina = deteccionDuplicadosService.listar(PageRequest.of(1, 20));

        assertEquals(21, primeraPagina.getTotalElements());
        assertEquals(20, primeraPagina.getContent().size());
        assertEquals(1, segundaPagina.getContent().size());
        mockMvc.perform(get("/campo/duplicados")
                        .param("pagina", "0")
                        .with(user("personal").roles("PERSONAL_CAMPO")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Siguiente")));
    }

    private void cambiarDatosComparacion(Long familiaId, String nombre, String telefono) {
        com.sirab.registro.Familia familia = familiaRepository.findById(familiaId).orElseThrow();
        familia.setNombreRegistrante(nombre);
        familia.setTelefonoContacto(telefono);
        IndiceDuplicadosInitializer.actualizarIndice(familia);
        familiaRepository.saveAndFlush(familia);
        deteccionDuplicadosService.detectarPara(familia, true);
    }

    @Test
    void detectaCoincidenciasCuandoDosRegistrosSeCreanConcurrentemente() throws Exception {
        String nombre = "CONCURRENCIA_" + UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch bloqueoAdquirido = new CountDownLatch(1);
        CountDownLatch liberarBloqueo = new CountDownLatch(1);
        CountDownLatch registrosListos = new CountDownLatch(2);
        CountDownLatch iniciarRegistros = new CountDownLatch(1);
        Future<?> retenedor = executor.submit(() -> {
            org.springframework.transaction.support.TransactionTemplate transaccion =
                    new org.springframework.transaction.support.TransactionTemplate(transactionManager);
            transaccion.execute(estado -> {
                bloqueo.bloquear();
                bloqueoAdquirido.countDown();
                try {
                    if (!liberarBloqueo.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Tiempo de espera agotado para liberar el bloqueo de prueba.");
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("La prueba de concurrencia fue interrumpida.", error);
                }
                return null;
            });
        });

        try {
            assertTrue(bloqueoAdquirido.await(5, TimeUnit.SECONDS));
            Future<?> primerRegistro = executor.submit(() -> {
                registrosListos.countDown();
                iniciarRegistros.await();
                registroFamiliarService.registrar(registro(nombre, "3001234567", "Centro", "Calle 1"));
                return null;
            });
            Future<?> segundoRegistro = executor.submit(() -> {
                registrosListos.countDown();
                iniciarRegistros.await();
                registroFamiliarService.registrar(registro(nombre, "3001234567", "Norte", "Carrera 8"));
                return null;
            });
            assertTrue(registrosListos.await(5, TimeUnit.SECONDS));
            iniciarRegistros.countDown();
            Thread.sleep(200);
            assertEquals(false, primerRegistro.isDone());
            assertEquals(false, segundoRegistro.isDone());
            liberarBloqueo.countDown();
            retenedor.get(10, TimeUnit.SECONDS);
            primerRegistro.get(20, TimeUnit.SECONDS);
            segundoRegistro.get(20, TimeUnit.SECONDS);

            List<com.sirab.registro.Familia> familias = familiaRepository.findAll().stream()
                    .filter(familia -> nombre.equals(familia.getNombreRegistrante()))
                    .toList();
            assertEquals(2, familias.size());
            assertEquals(1, posibleDuplicadoRepository.findAllByFamiliaId(familias.get(0).getId()).size());
        } finally {
            liberarBloqueo.countDown();
            executor.shutdownNow();
            limpiarRegistrosConcurrentes(nombre);
        }
    }

    private void limpiarRegistrosConcurrentes(String nombre) {
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

    private RegistroFamiliarCommand registro(
            String nombre, String telefono, String barrio, String direccion) {
        return new RegistroFamiliarCommand(
                nombre,
                barrio,
                direccion,
                "Referencia del " + direccion,
                2,
                telefono,
                Set.of(SituacionFamiliar.MENORES_DE_CINCO),
                Set.of(Necesidad.AGUA),
                true,
                Instant.now());
    }

    private SolicitudVerificacionCommand verificacion(
            String nombre, String telefono, String barrio, String direccion) {
        return new SolicitudVerificacionCommand(
                nombre,
                2,
                telefono,
                barrio,
                direccion,
                "Referencia del " + direccion,
                Set.of(SituacionFamiliar.MENORES_DE_CINCO),
                Set.of(Necesidad.AGUA),
                null);
    }
}
