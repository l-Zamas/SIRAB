package com.sirab.registro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;

import com.sirab.registro.Familia;
import com.sirab.registro.FamiliaRepository;
import com.sirab.registro.Necesidad;
import com.sirab.registro.SituacionFamiliar;
import com.sirab.registro.EstadoRegistro;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistroFamiliarServiceTests {

    @Mock
    private FamiliaRepository familiaRepository;

    @InjectMocks
    private RegistroFamiliarService service;

    @Test
    void guardaLosDatosConfirmadosLaSituacionLasNecesidadesYElConsentimiento() {
        Instant consentTime = Instant.parse("2026-01-01T12:00:00Z");
        RegistroFamiliarCommand command = new RegistroFamiliarCommand(
                "REGISTRO_SINTETICO",
                "BARRIO_SINTETICO",
                "REFERENCIA_SINTETICA",
                3,
                "TELEFONO_SINTETICO",
                Set.of(SituacionFamiliar.MENORES_DE_CINCO, SituacionFamiliar.ADULTOS_MAYORES),
                Set.of(Necesidad.AGUA, Necesidad.ALIMENTOS),
                true,
                consentTime);
        when(familiaRepository.save(any(Familia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.registrar(command);

        ArgumentCaptor<Familia> captor = ArgumentCaptor.forClass(Familia.class);
        verify(familiaRepository).save(captor.capture());
        Familia familia = captor.getValue();
        assertEquals(command.nombreRegistrante(), familia.getNombreRegistrante());
        assertEquals(command.barrio(), familia.getBarrio());
        assertEquals(command.direccionReferencia(), familia.getDireccionReferencia());
        assertEquals(command.numeroIntegrantes(), familia.getNumeroIntegrantes());
        assertEquals(command.telefonoContacto(), familia.getTelefonoContacto());
        assertEquals(command.situaciones(), familia.getSituaciones());
        assertEquals(command.necesidades(), familia.getNecesidades());
        assertEquals(consentTime, familia.getConsentimientoAt());
        assertNotNull(familia.getConsentimientoVersion());
    }

    @Test
    void rechazaElRegistroSinConsentimiento() {
        RegistroFamiliarCommand command = new RegistroFamiliarCommand(
                "REGISTRO_SINTETICO",
                null,
                null,
                1,
                null,
                Set.of(),
                Set.of(),
                false,
                null);

        assertThrows(IllegalArgumentException.class, () -> service.registrar(command));

        verifyNoInteractions(familiaRepository);
    }

    @Test
    void actualizaEstadoDeRegistroExistente() {
        Familia familia = new Familia();
        when(familiaRepository.findById(1L)).thenReturn(java.util.Optional.of(familia));

        service.actualizarEstado(1L, EstadoRegistro.VERIFICADO);

        assertEquals(EstadoRegistro.VERIFICADO, familia.getEstado());
    }
}
