package com.sirab.registro.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import com.sirab.registro.DocumentoPrivadoRepository;
import com.sirab.registro.EvidenciaFamiliaRepository;
import com.sirab.registro.Familia;
import com.sirab.registro.FamiliaRepository;
import com.sirab.registro.Necesidad;
import com.sirab.registro.TipoDocumentoPrivado;
import com.sirab.registro.web.RegistroFamiliarDraft;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class EvidenciaFamiliaIntegrationTests {

    private static Path storage;

    @DynamicPropertySource
    static void configureStorage(DynamicPropertyRegistry registry) throws Exception {
        storage = Files.createTempDirectory("sirab-evidencias-");
        registry.add("sirab.documentos.path", () -> storage.toString());
    }

    @Autowired MockMvc mockMvc;
    @Autowired EvidenciaFamiliaService evidenciaService;
    @Autowired EvidenciaFamiliaRepository evidenciaRepository;
    @Autowired DocumentoPrivadoRepository documentoRepository;
    @Autowired FamiliaRepository familiaRepository;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired RegistroFamiliarService registroFamiliarService;

    @BeforeEach
    void limpiarBase() throws Exception {
        evidenciaRepository.deleteAll();
        if (Files.exists(storage)) {
            try (var paths = Files.walk(storage)) {
                paths.sorted(java.util.Comparator.reverseOrder())
                        .filter(path -> !path.equals(storage))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (java.io.IOException exception) {
                                throw new java.io.UncheckedIOException(exception);
                            }
                        });
            }
        }
    }

    @Test
    void rechazaAlojamientoMedicamentosYAmbasNecesidadesSinEvidenciaEnBackend() {
        long familiasAntes = familiaRepository.count();
        assertThrows(IllegalArgumentException.class, () -> registrar(SetFactory.necesidades(Necesidad.ALOJAMIENTO)));
        assertThrows(IllegalArgumentException.class, () -> registrar(SetFactory.necesidades(Necesidad.MEDICAMENTOS)));
        assertThrows(IllegalArgumentException.class, () -> registrar(SetFactory.necesidades(Necesidad.ALOJAMIENTO, Necesidad.MEDICAMENTOS)));
        assertTrue(familiaRepository.count() == familiasAntes);
    }

    @Test
    void rechazaCargaVaciaMimeNoPermitidoYTamanoExcesivoYConservaElBorrador() throws Exception {
        MockHttpSession session = draftSession(Necesidad.MEDICAMENTOS);

        mockMvc.perform(multipart("/familia/registro/documento")
                        .file(new MockMultipartFile("evidenciaMedicamentos", "vacia.pdf", "application/pdf", new byte[0]))
                        .session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("familia/documento-diferido"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("obligatoria")));
        assertDraftStillPresent(session);

        mockMvc.perform(multipart("/familia/registro/documento")
                        .file(new MockMultipartFile("evidenciaMedicamentos", "script.exe", "application/x-msdownload", new byte[] {1}))
                        .session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("tipo de archivo no está permitido")));
        assertDraftStillPresent(session);

        mockMvc.perform(multipart("/familia/registro/documento")
                        .file(new MockMultipartFile("evidenciaMedicamentos", "grande.pdf", "application/pdf", new byte[5 * 1024 * 1024 + 1]))
                        .session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("máximo 5 MB")));
        assertDraftStillPresent(session);

        mockMvc.perform(multipart("/familia/registro/documento")
                        .file(new MockMultipartFile("evidenciaMedicamentos", "formula.pdf", "application/pdf", new byte[] {2, 3, 4}))
                        .session(session).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertTrue(((RegistroFamiliarDraft) session.getAttribute("registroFamiliarDraft")).getEvidenciaMedicamentos() != null);
    }

    @Test
    void rechazaArchivoTemporalInexistente() throws Exception {
        Familia familia = familia(Necesidad.MEDICAMENTOS);
        assertThrows(IllegalArgumentException.class, () -> evidenciaService.asociar(
                familia, "pendientes/no-existe.pdf", "formula.pdf", "application/pdf", TipoDocumentoPrivado.SOPORTE_MEDICO));
    }

    @Test
    void recuperaEvidenciaSoloParaPersonalCampoYSoloDeLaFamiliaCorrecta() throws Exception {
        Familia primera = familia(Necesidad.MEDICAMENTOS);
        Familia segunda = familia(Necesidad.MEDICAMENTOS);
        String key = evidenciaService.guardarTemporal(
                new MockMultipartFile("evidenciaMedicamentos", "formula.pdf", "application/pdf", new byte[] {9, 8, 7}),
                TipoDocumentoPrivado.SOPORTE_MEDICO);
        evidenciaService.asociar(primera, key, "formula.pdf", "application/pdf", TipoDocumentoPrivado.SOPORTE_MEDICO);
        Long evidenciaId = evidenciaRepository.findByFamilia_Id(primera.getId()).get(0).getId();

        mockMvc.perform(get("/campo/registros/{familiaId}/evidencias/{evidenciaId}", primera.getId(), evidenciaId)
                        .with(user("campo").roles("PERSONAL_CAMPO")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(new byte[] {9, 8, 7}));

        mockMvc.perform(get("/campo/registros/{familiaId}/evidencias/{evidenciaId}", segunda.getId(), evidenciaId)
                        .with(user("campo").roles("PERSONAL_CAMPO")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/campo/registros/{familiaId}/evidencias/{evidenciaId}", primera.getId(), evidenciaId))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void eliminaArchivoYAsociacionesCuandoLaTransaccionHaceRollbackDiferido() throws Exception {
        Familia familia = familia(Necesidad.MEDICAMENTOS);
        String key = evidenciaService.guardarTemporal(
                new MockMultipartFile("evidenciaMedicamentos", "formula.pdf", "application/pdf", new byte[] {5, 4, 3}),
                TipoDocumentoPrivado.SOPORTE_MEDICO);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            evidenciaService.asociar(familia, key, "formula.pdf", "application/pdf", TipoDocumentoPrivado.SOPORTE_MEDICO);
            status.setRollbackOnly();
        });

        assertTrue(evidenciaRepository.findByFamilia_Id(familia.getId()).isEmpty());
        assertTrue(documentoRepository.findByClaveAlmacenamiento("soporte_medico/" + key.substring(key.lastIndexOf('/') + 1))
                .isEmpty());
        try (var paths = Files.walk(storage)) {
            assertTrue(paths.noneMatch(path -> Files.isRegularFile(path)
                    && path.toString().contains("soporte_medico")));
        }
    }

    private Familia familia(Necesidad... necesidades) {
        Familia familia = new Familia();
        familia.setNombreRegistrante("Familia " + UUID.randomUUID());
        familia.setNumeroIntegrantes(2);
        familia.setDireccionReferencia("");
        familia.setNecesidades(EnumSet.copyOf(List.of(necesidades)));
        familia.setConsentimientoAt(Instant.now());
        familia.setConsentimientoVersion("test");
        return familiaRepository.saveAndFlush(familia);
    }

    private String registrar(java.util.Set<Necesidad> necesidades) {
        return registroFamiliarService.registrar(new RegistroFamiliarCommand(
                "Familia sin evidencia", "Barrio", "Dirección", null, 2, "3000000000",
                java.util.Set.of(), necesidades, true, Instant.now(), null, null, null, null, null, null));
    }

    private MockHttpSession draftSession(Necesidad necesidad) {
        RegistroFamiliarDraft draft = new RegistroFamiliarDraft();
        draft.setEtapa(6);
        draft.setNombreRegistrante("Familia diligenciada");
        draft.setNumeroIntegrantes(2);
        draft.setNecesidades(SetFactory.necesidades(necesidad));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("registroFamiliarDraft", draft);
        return session;
    }

    private void assertDraftStillPresent(MockHttpSession session) {
        RegistroFamiliarDraft draft = (RegistroFamiliarDraft) session.getAttribute("registroFamiliarDraft");
        assertTrue(draft != null && draft.getNombreRegistrante().equals("Familia diligenciada"));
    }

    private static final class SetFactory {
        static java.util.Set<Necesidad> necesidades(Necesidad... values) {
            return EnumSet.copyOf(List.of(values));
        }
    }
}
