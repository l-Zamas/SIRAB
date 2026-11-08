package com.sirab.registro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import com.sirab.registro.EntregaAyuda;
import com.sirab.registro.EntregaAyudaRepository;
import com.sirab.registro.EstadoSolicitudHistorial;
import com.sirab.registro.EstadoSolicitudHistorialRepository;
import com.sirab.registro.Familia;
import com.sirab.registro.FamiliaRepository;
import com.sirab.registro.Necesidad;
import com.sirab.registro.SituacionFamiliar;
import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.VerificacionSolicitud;
import com.sirab.registro.VerificacionSolicitudRepository;
import com.sirab.registro.duplicados.DeteccionDuplicadosService;

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

    @Mock
    private EstadoSolicitudHistorialRepository historialRepository;

    @Mock
    private VerificacionSolicitudRepository verificacionRepository;

    @Mock
    private EntregaAyudaRepository entregaRepository;

    @Mock
    private DeteccionDuplicadosService deteccionDuplicadosService;

    @InjectMocks
    private RegistroFamiliarService service;

    @Test
    void guardaLosDatosConfirmadosLaSituacionLasNecesidadesYElConsentimiento() {
        Instant consentTime = Instant.parse("2026-01-01T12:00:00Z");
        RegistroFamiliarCommand command = new RegistroFamiliarCommand(
                "REGISTRO_SINTETICO",
                "BARRIO_SINTETICO",
                "DIRECCION_SINTETICA",
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
        assertEquals(command.direccion(), familia.getDireccion());
        assertEquals(command.direccionReferencia(), familia.getDireccionReferencia());
        assertEquals(command.numeroIntegrantes(), familia.getNumeroIntegrantes());
        assertEquals(command.telefonoContacto(), familia.getTelefonoContacto());
        assertEquals(command.situaciones(), familia.getSituaciones());
        assertEquals(command.necesidades(), familia.getNecesidades());
        assertEquals(consentTime, familia.getConsentimientoAt());
        assertNotNull(familia.getConsentimientoVersion());
        verify(historialRepository).save(any(EstadoSolicitudHistorial.class));
    }

    @Test
    void rechazaElRegistroSinConsentimiento() {
        RegistroFamiliarCommand command = new RegistroFamiliarCommand(
                "REGISTRO_SINTETICO",
                null,
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
    void verificaYAuditaLasCorreccionesAntesDeCambiarEstado() {
        Familia familia = new Familia();
        familia.setNombreRegistrante("Nombre original");
        familia.setNumeroIntegrantes(2);
        familia.setSituaciones(Set.of(SituacionFamiliar.MENORES_DE_CINCO));
        familia.setNecesidades(Set.of(Necesidad.AGUA));
        when(familiaRepository.findById(1L)).thenReturn(java.util.Optional.of(familia));

        service.verificar(1L, "campo@sirab.local", new SolicitudVerificacionCommand(
                "Nombre corregido",
                3,
                "3000000000",
                "Barrio",
                "Dirección",
                "Referencia",
                Set.of(SituacionFamiliar.ADULTOS_MAYORES),
                Set.of(Necesidad.AGUA, Necesidad.ALOJAMIENTO),
                "Verificación presencial"));

        assertEquals(EstadoRegistro.VERIFICADO, familia.getEstado());
        ArgumentCaptor<VerificacionSolicitud> verificacionCaptor =
                ArgumentCaptor.forClass(VerificacionSolicitud.class);
        verify(verificacionRepository).save(verificacionCaptor.capture());
        VerificacionSolicitud verificacion = verificacionCaptor.getValue();
        assertEquals("campo@sirab.local", verificacion.getUsuarioCorreo());
        assertEquals("Verificación presencial", verificacion.getObservacion());
        assertEquals(8, verificacion.getCambios().size());
        assertEquals("Nombre de quien registra", verificacion.getCambios().get(0).getCampo());
        ArgumentCaptor<EstadoSolicitudHistorial> historialCaptor =
                ArgumentCaptor.forClass(EstadoSolicitudHistorial.class);
        verify(historialRepository).save(historialCaptor.capture());
        assertEquals(EstadoRegistro.PENDIENTE, historialCaptor.getValue().getEstadoAnterior());
        assertEquals(EstadoRegistro.VERIFICADO, historialCaptor.getValue().getEstadoNuevo());
    }

    @Test
    void registraEntregaParcialSinMarcarLaNecesidadComoCompleta() {
        Familia familia = new Familia();
        familia.setEstado(EstadoRegistro.VERIFICADO);
        familia.setNecesidades(Set.of(Necesidad.AGUA));
        when(familiaRepository.findById(1L)).thenReturn(java.util.Optional.of(familia));

        service.registrarEntrega(1L, "campo@sirab.local", new RegistrarEntregaCommand(
                Necesidad.AGUA, "20 L", LocalDate.of(2026, 2, 1), false, "Entrega parcial"));

        ArgumentCaptor<EntregaAyuda> entregaCaptor = ArgumentCaptor.forClass(EntregaAyuda.class);
        verify(entregaRepository).save(entregaCaptor.capture());
        assertEquals(false, entregaCaptor.getValue().isSatisfaceNecesidadCompleta());
        assertEquals("campo@sirab.local", entregaCaptor.getValue().getUsuarioCorreo());
    }

    @Test
    void impideMarcarAtendidoMientrasUnaNecesidadNoTengaEntregaCompleta() {
        Familia familia = new Familia();
        familia.setEstado(EstadoRegistro.VERIFICADO);
        familia.setNecesidades(Set.of(Necesidad.AGUA, Necesidad.ALIMENTOS));
        when(familiaRepository.findById(1L)).thenReturn(java.util.Optional.of(familia));
        when(entregaRepository.existsByFamilia_IdAndTipoAyudaAndSatisfaceNecesidadCompletaTrue(
                eq(1L), any(Necesidad.class))).thenAnswer(invocation ->
                        invocation.getArgument(1, Necesidad.class) == Necesidad.AGUA);

        assertThrows(IllegalStateException.class,
                () -> service.marcarAtendido(1L, "campo@sirab.local", "Cierre"));

        assertEquals(EstadoRegistro.VERIFICADO, familia.getEstado());
        verifyNoInteractions(historialRepository);
    }

    @Test
    void marcaAtendidoCuandoTodasLasNecesidadesTienenEntregaCompleta() {
        Familia familia = new Familia();
        familia.setEstado(EstadoRegistro.VERIFICADO);
        familia.setNecesidades(Set.of(Necesidad.AGUA, Necesidad.ALOJAMIENTO));
        when(familiaRepository.findById(1L)).thenReturn(java.util.Optional.of(familia));
        when(entregaRepository.existsByFamilia_IdAndTipoAyudaAndSatisfaceNecesidadCompletaTrue(
                eq(1L), any())).thenReturn(true);

        service.marcarAtendido(1L, "campo@sirab.local", "Atención completa");

        assertEquals(EstadoRegistro.ATENDIDO, familia.getEstado());
        ArgumentCaptor<EstadoSolicitudHistorial> historialCaptor =
                ArgumentCaptor.forClass(EstadoSolicitudHistorial.class);
        verify(historialRepository).save(historialCaptor.capture());
        assertEquals(EstadoRegistro.VERIFICADO, historialCaptor.getValue().getEstadoAnterior());
        assertEquals(EstadoRegistro.ATENDIDO, historialCaptor.getValue().getEstadoNuevo());
        assertEquals("campo@sirab.local", historialCaptor.getValue().getUsuarioCorreo());
    }
}
