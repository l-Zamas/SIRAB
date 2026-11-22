package com.sirab.reportes.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.sirab.reportes.ReportesService;

@Controller
public class ReportesController {

    private final ReportesService reportesService;

    public ReportesController(ReportesService reportesService) {
        this.reportesService = reportesService;
    }

    @GetMapping("/coordinacion/reportes")
    public String reportes(Model model) {
        var reporte = reportesService.generarReporte();
        model.addAttribute("reporte", reporte);
        model.addAttribute("estados", com.sirab.registro.EstadoRegistro.values());
        model.addAttribute("necesidades", com.sirab.registro.Necesidad.values());
        return "coordinacion/reportes";
    }
}
