package com.sirab;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Set;

import com.sirab.registro.Familia;
import com.sirab.registro.FamiliaRepository;
import com.sirab.registro.Necesidad;
import com.sirab.registro.SituacionFamiliar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SirabApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FamiliaRepository familiaRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void homeAndConsentRoutesArePublicAndNotCached() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("familia/bienvenida"))
                .andExpect(content().string(containsString("Sistema Integral de Respuesta")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Cache-Control", containsString("no-store")));

        mockMvc.perform(get("/familia/registro/consentimiento"))
                .andExpect(status().isOk())
                .andExpect(view().name("familia/consentimiento"));

        mockMvc.perform(get("/css/registro.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"));
    }

    @Test
    @Transactional
    void wizardPersistsSyntheticFamilyAndRelatedSelectionsOnlyAtConfirmation() throws Exception {
        long familyCountBefore = familiaRepository.count();
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/familia/registro/consentimiento")
                        .session(session)
                        .with(csrf())
                        .param("aceptado", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/familia/registro/datos"));
        mockMvc.perform(get("/familia/registro/datos").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("familia/datos-familia"));

        mockMvc.perform(post("/familia/registro/datos")
                        .session(session)
                        .with(csrf())
                        .param("nombreRegistrante", "REGISTRO_SINTETICO")
                        .param("numeroIntegrantes", "2")
                        .param("telefonoContacto", "TELEFONO_SINTETICO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/familia/registro/ubicacion"));
        mockMvc.perform(get("/familia/registro/ubicacion").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("familia/ubicacion"));

        mockMvc.perform(post("/familia/registro/ubicacion")
                        .session(session)
                        .with(csrf())
                        .param("barrio", "BARRIO_SINTETICO")
                        .param("direccionReferencia", "REFERENCIA_SINTETICA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/familia/registro/situacion"));
        mockMvc.perform(get("/familia/registro/situacion").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("familia/situacion"));

        mockMvc.perform(post("/familia/registro/situacion")
                        .session(session)
                        .with(csrf())
                        .param("menoresDeCinco", "true")
                        .param("personasConDiscapacidad", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/familia/registro/necesidades"));
        mockMvc.perform(get("/familia/registro/necesidades").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("familia/necesidades"));

        mockMvc.perform(post("/familia/registro/necesidades")
                        .session(session)
                        .with(csrf())
                        .param("necesidades", "AGUA", "ALIMENTOS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/familia/registro/documento"));

        mockMvc.perform(get("/familia/registro/documento").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("familia/documento-diferido"))
                .andExpect(content().string(containsString("no está habilitada")));

        mockMvc.perform(post("/familia/registro/documento").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/familia/registro/resumen"));

        mockMvc.perform(get("/familia/registro/resumen").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("REGISTRO_SINTETICO")))
                .andExpect(content().string(containsString("REFERENCIA_SINTETICA")));
        assertEquals(familyCountBefore, familiaRepository.count());

        mockMvc.perform(post("/familia/registro/confirmar").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/familia/registro/confirmacion"));

        mockMvc.perform(get("/familia/registro/confirmacion"))
                .andExpect(status().isOk())
                .andExpect(view().name("familia/confirmacion"))
                .andExpect(content().string(containsString("Registro recibido")))
                .andExpect(content().string(not(containsString("REGISTRO_SINTETICO"))));

        Familia saved = familiaRepository.findFirstByNombreRegistrante("REGISTRO_SINTETICO").orElseThrow();
        assertEquals(2, saved.getNumeroIntegrantes());
        assertEquals("BARRIO_SINTETICO", saved.getBarrio());
        assertEquals("REFERENCIA_SINTETICA", saved.getDireccionReferencia());
        assertEquals("TELEFONO_SINTETICO", saved.getTelefonoContacto());
        assertEquals(Set.of(SituacionFamiliar.MENORES_DE_CINCO,
                SituacionFamiliar.PERSONAS_CON_DISCAPACIDAD), saved.getSituaciones());
        assertEquals(Set.of(Necesidad.AGUA, Necesidad.ALIMENTOS), saved.getNecesidades());
        assertTrue(saved.getConsentimientoAt() != null);
        assertTrue(saved.getConsentimientoVersion() != null);
        assertTrue(session.getAttribute("registroFamiliarDraft") == null);
    }

    @Test
    void rejectsMissingConsentAndInvalidDataWithoutEchoingPersonalValuesOrPersisting() throws Exception {
        long familyCountBefore = familiaRepository.count();
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/familia/registro/consentimiento")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Debes aceptar el uso")))
                .andExpect(content().string(not(containsString("REGISTRO_SINTETICO"))));

        mockMvc.perform(post("/familia/registro/datos")
                        .session(session)
                        .with(csrf())
                        .param("nombreRegistrante", "DATO_PERSONAL_SINTETICO")
                        .param("numeroIntegrantes", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/familia/registro/consentimiento"));

        mockMvc.perform(post("/familia/registro/consentimiento")
                        .session(session)
                        .with(csrf())
                        .param("aceptado", "true"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/familia/registro/datos")
                        .session(session)
                        .with(csrf())
                        .param("nombreRegistrante", "DATO_PERSONAL_SINTETICO")
                        .param("numeroIntegrantes", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Debe indicarse al menos una persona.")))
                .andExpect(content().string(not(containsString("DATO_PERSONAL_SINTETICO"))));

        assertEquals(familyCountBefore, familiaRepository.count());
    }

    @Test
    void registrationStateAndInternalRoutesAreNotPublic() throws Exception {
        mockMvc.perform(get("/familia/estado"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/familia/registro/123"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/interno/no-configurado"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/campo/no-configurado"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/campo/no-configurado")
                        .with(user("personal").roles("PERSONAL_CAMPO")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/coordinacion/no-configurado")
                        .with(user("personal").roles("PERSONAL_CAMPO")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/coordinacion/no-configurado")
                        .with(user("coordinacion").roles("COORDINACION")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/admin/no-configurado")
                        .with(user("coordinacion").roles("COORDINACION")))
                .andExpect(status().isForbidden());
    }
}
