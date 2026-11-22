package com.sirab.registro.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.sirab.registro.service.ConsultaEstadoService;

@Controller
public class ConsultaEstadoController {

    private static final int MAX_FAILURES = 5;
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);
    private static final String GENERIC_ERROR = "No fue posible validar el código de consulta.";

    private final ConsultaEstadoService consultaEstadoService;
    private final Clock clock;
    private final Map<String, FailureState> failures = new ConcurrentHashMap<>();

    @Autowired
    public ConsultaEstadoController(ConsultaEstadoService consultaEstadoService) {
        this(consultaEstadoService, Clock.systemUTC());
    }

    ConsultaEstadoController(ConsultaEstadoService consultaEstadoService, Clock clock) {
        this.consultaEstadoService = consultaEstadoService;
        this.clock = clock;
    }

    @GetMapping("/familia/consulta")
    public String formulario(Model model, HttpServletResponse response) {
        noCache(response);
        if (!model.containsAttribute("consultaEstadoForm")) {
            model.addAttribute("consultaEstadoForm", new ConsultaEstadoForm());
        }
        return "familia/consulta-estado";
    }

    @PostMapping("/familia/consulta")
    public String consultar(
            @Valid @ModelAttribute("consultaEstadoForm") ConsultaEstadoForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model,
            HttpServletResponse response) {
        noCache(response);
        String ip = obtenerIp(request);
        Instant ahora = clock.instant();
        FailureState estado = failures.get(ip);
        if (estado != null && estado.bloqueadoHasta().isAfter(ahora)) {
            model.addAttribute("errorConsulta", GENERIC_ERROR);
            return "familia/consulta-estado";
        }
        if (estado != null && !estado.bloqueadoHasta().isAfter(ahora)) {
            failures.remove(ip, estado);
        }
        if (bindingResult.hasErrors() || form.getCodigo() == null
                || consultaEstadoService.consultar(form.getCodigo()).isEmpty()) {
            registrarFallo(ip, ahora);
            model.addAttribute("errorConsulta", GENERIC_ERROR);
            return "familia/consulta-estado";
        }
        failures.remove(ip);
        model.addAttribute("estadoPublico", consultaEstadoService.consultar(form.getCodigo()).orElseThrow());
        return "familia/consulta-estado";
    }

    private void registrarFallo(String ip, Instant ahora) {
        failures.compute(ip, (clave, actual) -> {
            int intentos = actual == null ? 1 : actual.intentos() + 1;
            return new FailureState(intentos, intentos >= MAX_FAILURES
                    ? ahora.plus(BLOCK_DURATION) : Instant.MIN);
        });
    }

    private String obtenerIp(HttpServletRequest request) {
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private void noCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
    }

    private record FailureState(int intentos, Instant bloqueadoHasta) {
    }
}
