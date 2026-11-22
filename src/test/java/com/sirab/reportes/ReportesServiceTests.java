package com.sirab.reportes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sirab.registro.EntregaAyuda;
import com.sirab.registro.EntregaAyudaRepository;
import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.Familia;
import com.sirab.registro.FamiliaRepository;
import com.sirab.registro.Necesidad;

@ExtendWith(MockitoExtension.class)
class ReportesServiceTests {

    @Mock
    private FamiliaRepository familiaRepository;

    @Mock
    private EntregaAyudaRepository entregaRepository;

    @InjectMocks
    private ReportesService reportesService;

    @Test
    void consolidaFamiliasEstadosNecesidadesYBarriosSinDuplicarFamilias() {
        Familia agua = familia(EstadoRegistro.VERIFICADO, "Centro", Set.of(Necesidad.AGUA, Necesidad.ALIMENTOS));
        Familia sinBarrio = familia(EstadoRegistro.PENDIENTE, " ", Set.of(Necesidad.AGUA));
        when(familiaRepository.findAll()).thenReturn(List.of(agua, sinBarrio));
        when(entregaRepository.findAll()).thenReturn(List.of());

        ReportesService.ReporteCoordinacion reporte = reportesService.generarReporte();

        assertEquals(2, reporte.totalFamilias());
        assertEquals(1L, reporte.familiasPorEstado().get(EstadoRegistro.VERIFICADO));
        assertEquals(2L, reporte.familiasPorNecesidad().get(Necesidad.AGUA));
        assertEquals(1L, reporte.familiasPorNecesidad().get(Necesidad.ALIMENTOS));
        assertEquals(1L, reporte.familiasPorBarrio().get("Sin barrio informado"));
    }

    @Test
    void noSumaRegistrosHistoricosYSeparaUnidadesEfectivas() {
        Familia familia = familia(EstadoRegistro.ATENDIDO, "Centro", Set.of(Necesidad.AGUA));
        EntregaAyuda efectiva = new EntregaAyuda(familia, Necesidad.AGUA, null,
                new BigDecimal("10"), "Litros", java.time.Instant.now(), java.time.Instant.now(), "campo", "ok");
        EntregaAyuda efectivaOtraUnidad = new EntregaAyuda(familia, Necesidad.AGUA, null,
                new BigDecimal("2"), "Botellas", java.time.Instant.now(), java.time.Instant.now(), "campo", "ok");
        EntregaAyuda historica = new EntregaAyuda(familia, Necesidad.AGUA, "20 L",
                java.time.LocalDate.now(), java.time.Instant.now(), "campo", false, "legado");
        when(familiaRepository.findAll()).thenReturn(List.of(familia));
        when(entregaRepository.findAll()).thenReturn(List.of(efectiva, efectivaOtraUnidad, historica));

        ReportesService.ReporteCoordinacion reporte = reportesService.generarReporte();

        assertEquals(2, reporte.entregasEfectivas().size());
        assertEquals(1L, reporte.entregasHistoricas().get(Necesidad.AGUA));
        assertEquals(1L, reporte.entregasEfectivas().stream()
                .filter(entrega -> entrega.unidad().equals("Litros"))
                .findFirst().orElseThrow().numeroEntregas());
    }

    private Familia familia(EstadoRegistro estado, String barrio, Set<Necesidad> necesidades) {
        Familia familia = new Familia();
        familia.setEstado(estado);
        familia.setNombreRegistrante("Familia");
        familia.setBarrio(barrio);
        familia.setNumeroIntegrantes(1);
        familia.setNecesidades(necesidades);
        return familia;
    }
}
