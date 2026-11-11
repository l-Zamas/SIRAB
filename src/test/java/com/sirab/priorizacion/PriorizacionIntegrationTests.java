package com.sirab.priorizacion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.Familia;
import com.sirab.registro.FamiliaRepository;
import com.sirab.registro.Necesidad;
import com.sirab.registro.SituacionFamiliar;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class PriorizacionIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FamiliaRepository familiaRepository;

    @Test
    void listaOrdenaFiltraYConservaLasSolicitudesAtendidasEnElHistorial() throws Exception {
        Instant ahora = Instant.now();
        crearFamilia("FAMILIA_CRITICA", EnumSet.allOf(Necesidad.class),
                EnumSet.allOf(SituacionFamiliar.class), EstadoRegistro.PENDIENTE, ahora.minusSeconds(86_400));
        crearFamilia("FAMILIA_ALTA", Set.of(Necesidad.ALOJAMIENTO, Necesidad.MEDICAMENTOS),
                Set.of(SituacionFamiliar.ADULTOS_MAYORES, SituacionFamiliar.PERSONAS_CON_DISCAPACIDAD),
                EstadoRegistro.VERIFICADO, ahora.minusSeconds(7 * 86_400L));
        crearFamilia("FAMILIA_MEDIA", Set.of(Necesidad.ALOJAMIENTO),
                Set.of(SituacionFamiliar.MENORES_DE_CINCO), EstadoRegistro.PENDIENTE,
                ahora.minusSeconds(86_400));
        crearFamilia("FAMILIA_BAJA", Set.of(Necesidad.ALIMENTOS), Set.of(),
                EstadoRegistro.PENDIENTE, ahora.minusSeconds(86_400));
        crearFamilia("FAMILIA_ATENDIDA", EnumSet.allOf(Necesidad.class),
                EnumSet.allOf(SituacionFamiliar.class), EstadoRegistro.ATENDIDO, ahora.minusSeconds(86_400));

        String html = mockMvc.perform(get("/campo/registros").with(user("campo").roles("PERSONAL_CAMPO")))
                .andExpect(status().isOk())
                .andExpect(view().name("campo/registros"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Alojamiento")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Menores de 5 años")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        int critica = html.indexOf("FAMILIA_CRITICA");
        int alta = html.indexOf("FAMILIA_ALTA");
        int media = html.indexOf("FAMILIA_MEDIA");
        int baja = html.indexOf("FAMILIA_BAJA");
        int atendidas = html.indexOf("Solicitudes atendidas");
        assertTrue(critica < alta && alta < media && media < baja);
        assertTrue(atendidas >= 0);
        assertTrue(html.indexOf("FAMILIA_ATENDIDA") > atendidas);

        for (String nivel : new String[] {"CRITICA", "ALTA", "MEDIA", "BAJA"}) {
            String nombre = "FAMILIA_" + nivel;
            String filtrado = mockMvc.perform(get("/campo/registros")
                            .param("urgencia", nivel)
                            .with(user("campo").roles("PERSONAL_CAMPO")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString(nombre)))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            assertTrue(filtrado.contains("name=\"urgencia\""));
            int opcion = filtrado.indexOf("value=\"" + nivel + "\"");
            int inicioOpcion = filtrado.lastIndexOf("<option", opcion);
            int finOpcion = filtrado.indexOf('>', opcion);
            assertTrue(filtrado.substring(inicioOpcion, finOpcion).contains("selected"));
            String colaActiva = filtrado.substring(0, filtrado.indexOf("Solicitudes atendidas"));
            assertFalse(colaActiva.contains("FAMILIA_" + (nivel.equals("CRITICA") ? "BAJA" : "CRITICA")));
        }

        mockMvc.perform(get("/campo/registros").param("urgencia", "NO_VALIDO")
                        .with(user("campo").roles("PERSONAL_CAMPO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recalculaLaUrgenciaTrasCorregirNecesidadesYSituacionFamiliar() throws Exception {
        Familia familia = crearFamilia(
                "FAMILIA_CORREGIDA",
                Set.of(Necesidad.ALOJAMIENTO),
                Set.of(SituacionFamiliar.MENORES_DE_CINCO),
                EstadoRegistro.PENDIENTE,
                Instant.now().minusSeconds(86_400));
        var usuarioCampo = user("campo").roles("PERSONAL_CAMPO");

        mockMvc.perform(get("/campo/registros").with(usuarioCampo))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("13 puntos")));

        mockMvc.perform(post("/campo/registros/{id}/verificacion", familia.getId())
                        .with(usuarioCampo)
                        .with(csrf())
                        .param("nombreRegistrante", "FAMILIA_CORREGIDA")
                        .param("numeroIntegrantes", "1")
                        .param("barrio", "Barrio")
                        .param("direccion", "Dirección de prueba")
                        .param("direccionReferencia", "Referencia de prueba"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/campo/registros/" + familia.getId()));

        mockMvc.perform(get("/campo/registros").with(usuarioCampo))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("0 puntos")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Baja")));
    }

    @Test
    void protegeElListadoConElRolDePersonalDeCampo() throws Exception {
        mockMvc.perform(get("/campo/registros"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/campo/registros").with(user("coordinacion").roles("COORDINACION")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/campo/registros").with(user("campo").roles("PERSONAL_CAMPO")))
                .andExpect(status().isOk());
    }

    @Test
    void presentaLaFechaDeCreacionEnLaZonaHorariaDeBogota() throws Exception {
        Instant instante = Instant.parse("2026-03-08T04:30:00Z");
        Familia familia = crearFamilia(
                "FAMILIA_FECHA_BOGOTA",
                Set.of(),
                Set.of(),
                EstadoRegistro.PENDIENTE,
                instante);

        mockMvc.perform(get("/campo/registros/{id}", familia.getId())
                        .with(user("campo").roles("PERSONAL_CAMPO")))
                .andExpect(status().isOk())
                .andExpect(view().name("campo/verificar"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("07/03/2026 23:30")));
    }

    private Familia crearFamilia(
            String nombre,
            Set<Necesidad> necesidades,
            Set<SituacionFamiliar> situaciones,
            EstadoRegistro estado,
            Instant fechaCreacion) {
        Familia familia = new Familia();
        familia.setNombreRegistrante(nombre);
        familia.setNumeroIntegrantes(2);
        familia.setBarrio("Barrio de prueba");
        familia.setDireccion("Dirección de prueba");
        familia.setDireccionReferencia("Referencia de prueba");
        familia.setNecesidades(necesidades);
        familia.setSituaciones(situaciones);
        familia.setEstado(estado);
        familia.setConsentimientoAt(fechaCreacion);
        familia.setConsentimientoVersion("prueba");
        ReflectionTestUtils.setField(familia, "fechaCreacion", fechaCreacion);
        return familiaRepository.saveAndFlush(familia);
    }
}
