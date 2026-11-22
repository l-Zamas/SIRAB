package com.sirab.registro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.Familia;
import com.sirab.registro.Necesidad;

@ExtendWith(MockitoExtension.class)
class ConsultaEstadoServiceTests {

    @Mock
    private CodigoConsultaService codigoConsultaService;

    @Test
    void construyeUnResultadoPublicoInmutableConVariasNecesidades() {
        Familia familia = familiaConNecesidades(Set.of(Necesidad.AGUA, Necesidad.ALIMENTOS));
        when(codigoConsultaService.buscar("codigo-valido")).thenReturn(Optional.of(familia));

        ConsultaEstadoService.EstadoPublico resultado = new ConsultaEstadoService(codigoConsultaService)
                .consultar("codigo-valido").orElseThrow();

        assertEquals(Set.of(Necesidad.AGUA, Necesidad.ALIMENTOS), resultado.necesidades());
        assertThrows(UnsupportedOperationException.class,
                () -> resultado.necesidades().clear());
    }

    @Test
    void construyeUnResultadoPublicoConjuntoCuandoNoHayNecesidades() {
        Familia familia = familiaConNecesidades(Set.of());
        when(codigoConsultaService.buscar("codigo-sin-necesidades")).thenReturn(Optional.of(familia));

        ConsultaEstadoService.EstadoPublico resultado = new ConsultaEstadoService(codigoConsultaService)
                .consultar("codigo-sin-necesidades").orElseThrow();

        assertTrue(resultado.necesidades().isEmpty());
    }

    private Familia familiaConNecesidades(Set<Necesidad> necesidades) {
        Familia familia = new Familia();
        familia.setEstado(EstadoRegistro.PENDIENTE);
        familia.setNombreRegistrante("Familia de prueba");
        familia.setDireccion("Calle 1");
        familia.setBarrio("Centro");
        familia.setNecesidades(necesidades);
        return familia;
    }
}
