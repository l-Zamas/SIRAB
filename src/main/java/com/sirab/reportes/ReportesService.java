package com.sirab.reportes;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sirab.registro.EntregaAyuda;
import com.sirab.registro.EntregaAyudaRepository;
import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.Familia;
import com.sirab.registro.FamiliaRepository;
import com.sirab.registro.Necesidad;

@Service
public class ReportesService {

    private final FamiliaRepository familiaRepository;
    private final EntregaAyudaRepository entregaRepository;

    public ReportesService(FamiliaRepository familiaRepository, EntregaAyudaRepository entregaRepository) {
        this.familiaRepository = familiaRepository;
        this.entregaRepository = entregaRepository;
    }

    @Transactional(readOnly = true)
    public ReporteCoordinacion generarReporte() {
        List<Familia> familias = familiaRepository.findAll();
        Map<EstadoRegistro, Long> estados = new EnumMap<>(EstadoRegistro.class);
        Arrays.stream(EstadoRegistro.values()).forEach(estado -> estados.put(estado, 0L));
        familias.forEach(familia -> estados.compute(familia.getEstado(), (estado, total) -> total + 1));

        Map<Necesidad, Long> necesidades = new EnumMap<>(Necesidad.class);
        Arrays.stream(Necesidad.values()).forEach(necesidad -> necesidades.put(necesidad, 0L));
        familias.forEach(familia -> familia.getNecesidades().forEach(necesidad ->
                necesidades.compute(necesidad, (tipo, total) -> total + 1)));

        Map<String, Long> barrios = familias.stream()
                .collect(Collectors.groupingBy(
                        familia -> normalizarBarrio(familia.getBarrio()),
                        LinkedHashMap::new,
                        Collectors.counting()));

        List<EntregaAyuda> entregas = entregaRepository.findAll();
        Map<Necesidad, Long> entregasHistoricas = entregas.stream()
                .filter(entrega -> entrega.getFechaHoraEntrega() == null)
                .collect(Collectors.groupingBy(EntregaAyuda::getTipoAyuda,
                        () -> new EnumMap<>(Necesidad.class), Collectors.counting()));
        Map<ClaveEntrega, ConsolidadoEntrega> entregasEfectivas = entregas.stream()
                .filter(entrega -> entrega.getFechaHoraEntrega() != null && entrega.getCantidadValor() != null)
                .collect(Collectors.groupingBy(
                        entrega -> new ClaveEntrega(entrega.getTipoAyuda(), entrega.getUnidad()),
                        Collectors.collectingAndThen(Collectors.toList(), lista -> new ConsolidadoEntrega(
                                lista.get(0).getTipoAyuda(), lista.get(0).getUnidad(), lista.size(),
                                lista.stream().map(EntregaAyuda::getCantidadValor).reduce(BigDecimal.ZERO, BigDecimal::add)))));

        return new ReporteCoordinacion(
                familias.size(), estados, necesidades, barrios,
                entregasEfectivas.values().stream().sorted(Comparator.comparing(e -> e.tipoAyuda().name())).toList(),
                entregasHistoricas);
    }

    private String normalizarBarrio(String barrio) {
        return barrio == null || barrio.isBlank() ? "Sin barrio informado" : barrio.trim();
    }

    private record ClaveEntrega(Necesidad tipoAyuda, String unidad) {
    }

    public record ConsolidadoEntrega(
            Necesidad tipoAyuda,
            String unidad,
            long numeroEntregas,
            BigDecimal cantidadTotal) {
    }

    public record ReporteCoordinacion(
            int totalFamilias,
            Map<EstadoRegistro, Long> familiasPorEstado,
            Map<Necesidad, Long> familiasPorNecesidad,
            Map<String, Long> familiasPorBarrio,
            List<ConsolidadoEntrega> entregasEfectivas,
            Map<Necesidad, Long> entregasHistoricas) {
        public ReporteCoordinacion {
            familiasPorEstado = Map.copyOf(familiasPorEstado);
            familiasPorNecesidad = Map.copyOf(familiasPorNecesidad);
            familiasPorBarrio = Map.copyOf(familiasPorBarrio);
            entregasEfectivas = List.copyOf(entregasEfectivas);
            entregasHistoricas = Map.copyOf(entregasHistoricas);
        }
    }
}
