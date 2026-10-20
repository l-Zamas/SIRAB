package com.sirab.registro.web;

import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.service.RegistroFamiliarService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class GestionEstadoRegistroController {

    private final RegistroFamiliarService registroFamiliarService;

    public GestionEstadoRegistroController(RegistroFamiliarService registroFamiliarService) {
        this.registroFamiliarService = registroFamiliarService;
    }

    @GetMapping("/campo/registros")
    public String listar(Model model) {
        model.addAttribute("registros", registroFamiliarService.listarRegistros());
        model.addAttribute("estadosDisponibles", EstadoRegistro.values());
        return "campo/registros";
    }

    @PostMapping("/campo/registros/{familiaId}/estado")
    public String actualizarEstado(
            @PathVariable Long familiaId,
            @RequestParam EstadoRegistro estado) {
        if (registroFamiliarService.actualizarEstado(familiaId, estado).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/campo/registros";
    }
}
