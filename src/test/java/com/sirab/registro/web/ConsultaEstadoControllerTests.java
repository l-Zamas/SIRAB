package com.sirab.registro.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.sirab.registro.service.ConsultaEstadoService;

@ExtendWith(MockitoExtension.class)
class ConsultaEstadoControllerTests {

    @Mock
    private ConsultaEstadoService consultaEstadoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ConsultaEstadoController(
                consultaEstadoService, Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC))).build();
    }

    @Test
    void codigoInvalidoDevuelveVistaSinDistinguirExistencia() throws Exception {
        when(consultaEstadoService.consultar("invalido")).thenReturn(java.util.Optional.empty());

        mockMvc.perform(post("/familia/consulta").param("codigo", "invalido"))
                .andExpect(status().isOk())
                .andExpect(view().name("familia/consulta-estado"));
    }

    @Test
    void quintoFalloBloqueaLosIntentosPosteriores() throws Exception {
        when(consultaEstadoService.consultar("invalido")).thenReturn(java.util.Optional.empty());
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/familia/consulta").param("codigo", "invalido"));
        }
        mockMvc.perform(post("/familia/consulta").param("codigo", "invalido"))
                .andExpect(status().isOk());
    }
}
