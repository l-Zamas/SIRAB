package com.sirab.registro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sirab.registro.AprobacionDefinitivaAlojamiento;
import com.sirab.registro.AprobacionDefinitivaAlojamientoRepository;
import com.sirab.registro.AprobacionProvisionalAlojamiento;
import com.sirab.registro.AprobacionProvisionalAlojamientoRepository;
import com.sirab.registro.ConfirmacionPrestacionAlojamiento;
import com.sirab.registro.ConfirmacionPrestacionAlojamientoRepository;
import com.sirab.registro.DecisionAyuda;
import com.sirab.registro.DocumentoPrivado;
import com.sirab.registro.DocumentoPrivadoRepository;
import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.Familia;
import com.sirab.registro.FamiliaRepository;
import com.sirab.registro.Necesidad;
import com.sirab.registro.ResultadoVerificacionAlojamiento;
import com.sirab.registro.SolicitudAlojamiento;
import com.sirab.registro.SolicitudAlojamientoRepository;
import com.sirab.registro.TipoDocumentoPrivado;
import com.sirab.registro.VerificacionAlojamiento;
import com.sirab.registro.VerificacionAlojamientoRepository;

@ExtendWith(MockitoExtension.class)
class AlojamientoServiceTests {

    @Mock
    private FamiliaRepository familiaRepository;

    @Mock
    private DocumentoPrivadoRepository documentoRepository;

    @Mock
    private SolicitudAlojamientoRepository solicitudRepository;

    @Mock
    private VerificacionAlojamientoRepository verificacionRepository;

    @Mock
    private AprobacionProvisionalAlojamientoRepository provisionalRepository;

    @Mock
    private AprobacionDefinitivaAlojamientoRepository definitivaRepository;

    @Mock
    private ConfirmacionPrestacionAlojamientoRepository confirmacionRepository;

    @InjectMocks
    private AlojamientoService alojamientoService;

    private Familia familiaConAlojamiento;
    private Familia familiaSinAlojamiento;
    private DocumentoPrivado fotoDoc;
    private SolicitudAlojamiento solicitud;

    @BeforeEach
    void setUp() {
        familiaConAlojamiento = new Familia();
        familiaConAlojamiento.setNombreRegistrante("Familia Damnificada");
        familiaConAlojamiento.setDireccionReferencia("Zona afectada");
        familiaConAlojamiento.setEstado(EstadoRegistro.EN_ATENCION);
        familiaConAlojamiento.setNecesidades(Set.of(Necesidad.ALOJAMIENTO));

        familiaSinAlojamiento = new Familia();
        familiaSinAlojamiento.setNombreRegistrante("Familia Otra");
        familiaSinAlojamiento.setDireccionReferencia("Otra zona");
        familiaSinAlojamiento.setEstado(EstadoRegistro.EN_ATENCION);
        familiaSinAlojamiento.setNecesidades(Set.of(Necesidad.AGUA));

        fotoDoc = new DocumentoPrivado(
                "clave/foto1.jpg",
                "casa_afectada.jpg",
                "image/jpeg",
                1024,
                "sha256fototest",
                TipoDocumentoPrivado.EVIDENCIA_ALOJAMIENTO,
                Instant.now(),
                "verificador@sirab.local");

        solicitud = new SolicitudAlojamiento(familiaConAlojamiento, Instant.now(), "campo@sirab.local", "Vivienda colapsada");

        org.springframework.test.util.ReflectionTestUtils.setField(familiaConAlojamiento, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(familiaSinAlojamiento, "id", 2L);
        org.springframework.test.util.ReflectionTestUtils.setField(fotoDoc, "id", 100L);
        org.springframework.test.util.ReflectionTestUtils.setField(solicitud, "id", 10L);
    }

    @Test
    void registraSolicitudExitosamenteParaFamiliaConNecesidadAlojamiento() {
        when(familiaRepository.findById(1L)).thenReturn(Optional.of(familiaConAlojamiento));
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitudAlojamiento resultado = alojamientoService.registrarSolicitud(
                1L, "campo@sirab.local", "Vivienda destruida");

        assertNotNull(resultado);
        assertEquals(familiaConAlojamiento, resultado.getFamilia());
    }

    @Test
    void rechazaSolicitudSiFamiliaNoTieneNecesidadAlojamiento() {
        when(familiaRepository.findById(2L)).thenReturn(Optional.of(familiaSinAlojamiento));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> alojamientoService.registrarSolicitud(2L, "campo@sirab.local", "Prueba"));
        assertTrue(ex.getMessage().contains("necesidad de alojamiento"));
    }

    @Test
    void verificacionAlojamientoRequiereSoporteFotografico() {
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(documentoRepository.findById(100L)).thenReturn(Optional.of(fotoDoc));
        when(verificacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VerificacionAlojamiento verif = alojamientoService.verificarAlojamiento(
                10L, ResultadoVerificacionAlojamiento.VERIFICADA, 100L, "verificador@sirab.local", "Pérdida total");

        assertNotNull(verif);
        assertEquals(ResultadoVerificacionAlojamiento.VERIFICADA, verif.getResultado());
        assertEquals(fotoDoc, verif.getFotografia());

        // Si falta fotografía
        assertThrows(IllegalArgumentException.class,
                () -> alojamientoService.verificarAlojamiento(
                        10L, ResultadoVerificacionAlojamiento.VERIFICADA, null, "verificador@sirab.local", null));
    }

    @Test
    void aprobacionProvisionalRechazaSiVerificacionNoFuePositiva() {
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        VerificacionAlojamiento verifNoVerificada = new VerificacionAlojamiento(
                solicitud, ResultadoVerificacionAlojamiento.NO_VERIFICADA, fotoDoc, Instant.now(), "verificador@sirab.local", "No hay daños estructurales");
        when(verificacionRepository.findFirstBySolicitud_IdOrderByVerificadaAtDesc(10L))
                .thenReturn(Optional.of(verifNoVerificada));

        // Intento de emitir APROBADA cuando la verificación fue NO_VERIFICADA
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> alojamientoService.aprobarProvisionalmente(
                        10L, DecisionAyuda.APROBADA, "coordinacion@sirab.local", "Aprobando provisional"));
        assertTrue(ex.getMessage().contains("VERIFICADO"));
    }

    @Test
    void aprobacionDefinitivaExigeAprobacionProvisionalAprobadaPrevia() {
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        AprobacionProvisionalAlojamiento provisionalRechazada = new AprobacionProvisionalAlojamiento(
                solicitud, DecisionAyuda.RECHAZADA, Instant.now(), "coordinacion@sirab.local", "No cumple cupos");
        when(provisionalRepository.findFirstBySolicitud_IdOrderByDecididaAtDesc(10L))
                .thenReturn(Optional.of(provisionalRechazada));

        // Intento de dar definitiva APROBADA cuando la provisional fue RECHAZADA
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> alojamientoService.registrarAprobacionDefinitiva(
                        10L, "UNGRD", "ACTA-2026", DecisionAyuda.APROBADA, null, "ungrd@sirab.local", "Aprobación externa"));
        assertTrue(ex.getMessage().contains("provisional no fue APROBADA"));
    }

    @Test
    void confirmacionPrestacionRequiereDefinitivaAprobada() {
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        AprobacionDefinitivaAlojamiento defRechazada = new AprobacionDefinitivaAlojamiento(
                solicitud, "Alcaldía", "REF-001", DecisionAyuda.RECHAZADA, Instant.now(), null, Instant.now(), "user", "Negado");
        when(definitivaRepository.findById(50L)).thenReturn(Optional.of(defRechazada));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> alojamientoService.confirmarPrestacion(
                        10L, 50L, Instant.now(), null, "enlace@sirab.local", "Entrega de llaves"));
        assertTrue(ex.getMessage().contains("RECHAZADA"));
    }

    @Test
    void satisfaccionDeAlojamientoReflejaEstadoRealSegunConfirmacion() {
        when(confirmacionRepository.existsBySolicitud_Familia_Id(1L)).thenReturn(false);
        assertFalse(alojamientoService.estaAlojamientoSatisfecho(1L));

        when(confirmacionRepository.existsBySolicitud_Familia_Id(1L)).thenReturn(true);
        assertTrue(alojamientoService.estaAlojamientoSatisfecho(1L));
    }
}
