package com.sirab.reportes.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sirab.reportes.ReportesService;
import com.sirab.registro.FamiliaRepository;
import com.sirab.registro.service.SeguimientoNecesidadService;
import java.util.List;
import java.util.Set;
import com.sirab.registro.Necesidad;
import com.sirab.registro.EstadoRegistro;

@Controller
public class ReportesController {

    private final ReportesService reportesService;
    private final FamiliaRepository familiaRepository;
    private final SeguimientoNecesidadService seguimientoNecesidadService;

    public ReportesController(ReportesService reportesService, FamiliaRepository familiaRepository,
            SeguimientoNecesidadService seguimientoNecesidadService) {
        this.reportesService = reportesService;
        this.familiaRepository = familiaRepository;
        this.seguimientoNecesidadService = seguimientoNecesidadService;
    }

    @GetMapping("/coordinacion/reportes")
    public String reportes(
            @RequestParam(required = false) EstadoRegistro estado,
            @RequestParam(required = false) Necesidad necesidad,
            Model model) {
        var reporte = reportesService.generarReporte();
        var solicitudes = familiaRepository.findAll().stream()
                .filter(f -> estado == null || f.getEstado() == estado)
                .filter(f -> necesidad == null || f.getNecesidades().contains(necesidad))
                .map(f -> new SolicitudCoordinacion(
                        f.getId(), f.getNumeroSolicitudPublica(), f.getNombreRegistrante(), f.getEstado(),
                        Set.copyOf(f.getNecesidades())))
                .toList();
        model.addAttribute("reporte", reporte);
        model.addAttribute("estados", EstadoRegistro.values());
        model.addAttribute("necesidades", Necesidad.values());
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("necesidadSeleccionada", necesidad);
        model.addAttribute("solicitudes", solicitudes);
        model.addAttribute("seguimientoNecesidadService", seguimientoNecesidadService);
        return "coordinacion/reportes";
    }

    public record SolicitudCoordinacion(Long id, String numeroSolicitudPublica, String nombreRegistrante,
            EstadoRegistro estado, Set<Necesidad> necesidades) { }
}
