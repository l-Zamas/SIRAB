package com.sirab.registro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sirab.registro.Familia;
import com.sirab.registro.FamiliaRepository;

@ExtendWith(MockitoExtension.class)
class CodigoConsultaServiceTests {

    @Mock
    private FamiliaRepository familiaRepository;

    @Test
    void generaCodigosHexadecimalesDe128BitsYNoRepiteDosValores() {
        CodigoConsultaService service = new CodigoConsultaService(familiaRepository);

        String primero = service.generarCodigo();
        String segundo = service.generarCodigo();

        assertEquals(32, primero.length());
        assertTrue(primero.matches("[0-9a-f]{32}"));
        assertNotEquals(primero, segundo);
    }

    @Test
    void buscaPorElHashSinExponerElCodigoOriginalAlRepositorio() {
        CodigoConsultaService service = new CodigoConsultaService(familiaRepository);
        Familia familia = new Familia();
        when(familiaRepository.findByCodigoConsultaHash("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"))
                .thenReturn(Optional.of(familia));

        Optional<Familia> resultado = service.buscar("test");

        assertTrue(resultado.isPresent());
        verify(familiaRepository).findByCodigoConsultaHash(
                "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
    }
}
