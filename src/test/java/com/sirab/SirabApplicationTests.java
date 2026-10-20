package com.sirab;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import java.time.Instant;

import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.Familia;
import com.sirab.registro.FamiliaRepository;
import com.sirab.registro.Necesidad;
import com.sirab.registro.SituacionFamiliar;
import com.sirab.seguridad.CuentaUsuario;
import com.sirab.seguridad.CuentaUsuarioRepository;
import com.sirab.seguridad.InicializarCuentasDesarrollo;
import com.sirab.seguridad.RolUsuario;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpSession;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SirabApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FamiliaRepository familiaRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CuentaUsuarioRepository cuentaUsuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplicationContext applicationContext;

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

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @Transactional
    void persistedAccountCanLoginWithHashedPasswordAndLogout() throws Exception {
        String correo = "login-" + java.util.UUID.randomUUID() + "@sirab.local";
        String password = java.util.UUID.randomUUID().toString();
        String hash = passwordEncoder.encode(password);
        cuentaUsuarioRepository.saveAndFlush(
                new CuentaUsuario(correo, hash, RolUsuario.PERSONAL_CAMPO));
        assertFalse(hash.equals(password));
        assertTrue(passwordEncoder.matches(password, hash));

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/login")
                        .session(session)
                        .param("username", correo)
                        .param("password", password))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/login")
                        .session(session)
                        .with(csrf())
                        .param("username", correo)
                        .param("password", java.util.UUID.randomUUID().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Correo o contraseña incorrectos.")))
                .andExpect(content().string(not(containsString(correo))));

        org.springframework.test.web.servlet.MvcResult loginResult = mockMvc.perform(post("/login")
                        .session(session)
                        .with(csrf())
                        .param("username", correo)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/campo/registros"))
                .andReturn();
        MockHttpSession authenticatedSession =
                (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotEquals(session.getId(), authenticatedSession.getId());

        mockMvc.perform(get("/campo/registros").session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(view().name("campo/registros"));
        mockMvc.perform(get("/admin/no-configurado").session(authenticatedSession))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/logout").session(authenticatedSession).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
        mockMvc.perform(get("/campo/registros").session(authenticatedSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @Transactional
    void persistedAdminAccountRetainsAdministrativeAccess() throws Exception {
        String correo = "admin-" + java.util.UUID.randomUUID() + "@sirab.local";
        String password = java.util.UUID.randomUUID().toString();
        cuentaUsuarioRepository.saveAndFlush(new CuentaUsuario(
                correo, passwordEncoder.encode(password), RolUsuario.ADMIN));
        MockHttpSession session = new MockHttpSession();

        org.springframework.test.web.servlet.MvcResult loginResult = mockMvc.perform(post("/login")
                        .session(session)
                        .with(csrf())
                        .param("username", correo)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession authenticatedSession =
                (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/admin/no-configurado").session(authenticatedSession))
                .andExpect(status().isNotFound());
    }

    @Test
    void developmentAccountInitializerIsNotLoadedOutsideDevProfile() {
        assertTrue(applicationContext.getBeansOfType(InicializarCuentasDesarrollo.class).isEmpty());
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
        assertEquals(EstadoRegistro.PENDIENTE, saved.getEstado());
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
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/familia/registro/123"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/interno/no-configurado"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/campo/no-configurado"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/campo/no-configurado")
                        .with(user("personal").roles("PERSONAL_CAMPO")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/campo/registros"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/campo/registros")
                        .with(user("personal").roles("PERSONAL_CAMPO")))
                .andExpect(status().isOk())
                .andExpect(view().name("campo/registros"));

        mockMvc.perform(post("/campo/registros/1/estado")
                        .with(csrf())
                        .param("estado", "ATENDIDO"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/campo/registros/1/estado")
                        .with(user("coordinacion").roles("COORDINACION"))
                        .with(csrf())
                        .param("estado", "ATENDIDO"))
                .andExpect(status().isForbidden());

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

    @Test
    @Transactional
    void fieldStaffCanChangeAnyRegistrationStateAndOtherRolesCannot() throws Exception {
        Familia familia = new Familia();
        familia.setNombreRegistrante("REGISTRO_ESTADO_SINTETICO");
        familia.setDireccionReferencia("REFERENCIA_ESTADO_SINTETICA");
        familia.setNumeroIntegrantes(1);
        familia.setConsentimientoAt(Instant.now());
        familia = familiaRepository.saveAndFlush(familia);

        mockMvc.perform(post("/campo/registros/{id}/estado", familia.getId())
                        .with(user("coordinacion").roles("COORDINACION"))
                        .with(csrf())
                        .param("estado", "ATENDIDO"))
                .andExpect(status().isForbidden());
        entityManager.flush();
        entityManager.clear();
        assertEquals(EstadoRegistro.PENDIENTE,
                familiaRepository.findById(familia.getId()).orElseThrow().getEstado());

        mockMvc.perform(post("/campo/registros/{id}/estado", familia.getId())
                        .with(user("personal").roles("PERSONAL_CAMPO"))
                        .with(csrf())
                        .param("estado", "ATENDIDO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/campo/registros"));
        entityManager.flush();
        entityManager.clear();
        assertEquals(EstadoRegistro.ATENDIDO,
                familiaRepository.findById(familia.getId()).orElseThrow().getEstado());

        mockMvc.perform(post("/campo/registros/{id}/estado", familia.getId())
                        .with(user("personal").roles("PERSONAL_CAMPO"))
                        .with(csrf())
                        .param("estado", "VERIFICADO"))
                .andExpect(status().is3xxRedirection());
        entityManager.flush();
        entityManager.clear();
        assertEquals(EstadoRegistro.VERIFICADO,
                familiaRepository.findById(familia.getId()).orElseThrow().getEstado());

        mockMvc.perform(post("/campo/registros/{id}/estado", familia.getId())
                        .with(user("personal").roles("PERSONAL_CAMPO"))
                        .with(csrf())
                        .param("estado", "ESTADO_INVALIDO"))
                .andExpect(status().isBadRequest());
        entityManager.flush();
        entityManager.clear();
        assertEquals(EstadoRegistro.VERIFICADO,
                familiaRepository.findById(familia.getId()).orElseThrow().getEstado());

        mockMvc.perform(post("/campo/registros/{id}/estado", familia.getId())
                        .with(user("personal").roles("PERSONAL_CAMPO"))
                        .with(csrf())
                        .param("estado", "PENDIENTE"))
                .andExpect(status().is3xxRedirection());
        entityManager.flush();
        entityManager.clear();
        assertEquals(EstadoRegistro.PENDIENTE,
                familiaRepository.findById(familia.getId()).orElseThrow().getEstado());

        mockMvc.perform(post("/campo/registros/{id}/estado", Long.MAX_VALUE)
                        .with(user("personal").roles("PERSONAL_CAMPO"))
                        .with(csrf())
                        .param("estado", "ATENDIDO"))
                .andExpect(status().isNotFound());
    }
}
