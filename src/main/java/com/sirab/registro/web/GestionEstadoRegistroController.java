package com.sirab.registro.web;

import java.util.Optional;
import java.util.stream.Collectors;

import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.Necesidad;
import com.sirab.registro.SituacionFamiliar;
import com.sirab.registro.service.RegistrarEntregaCommand;
import com.sirab.registro.service.RegistroFamiliarService;
import com.sirab.registro.service.SolicitudVerificacionCommand;
import com.sirab.registro.service.RegistroFamiliarService.SolicitudDetalle;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class GestionEstadoRegistroController {

    private final RegistroFamiliarService registroFamiliarService;

    public GestionEstadoRegistroController(RegistroFamiliarService registroFamiliarService) {
        this.registroFamiliarService = registroFamiliarService;
    }

    @GetMapping("/campo/registros")
    public String listar(Model model, HttpServletResponse response) {
        noCache(response);
        model.addAttribute("registros", registroFamiliarService.listarSolicitudes());
        return "campo/registros";
    }

    @GetMapping("/campo/registros/{familiaId}")
    public String detalle(
            @PathVariable Long familiaId,
            Model model,
            HttpServletResponse response) {
        noCache(response);
        cargarDetalle(familiaId, model);
        return "campo/verificar";
    }

    @PostMapping("/campo/registros/{familiaId}/verificacion")
    public String verificar(
            @PathVariable Long familiaId,
            @Valid @ModelAttribute("verificacionForm") VerificacionSolicitudForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        noCache(response);
        if (bindingResult.hasErrors()) {
            cargarDetalle(familiaId, model);
            return "campo/verificar";
        }

        try {
            boolean encontrada = registroFamiliarService.verificar(
                    familiaId,
                    authentication.getName(),
                    new SolicitudVerificacionCommand(
                            form.getNombreRegistrante(),
                            form.getNumeroIntegrantes(),
                            form.getTelefonoContacto(),
                            form.getBarrio(),
                            form.getDireccion(),
                            form.getDireccionReferencia(),
                            form.getSituaciones(),
                            form.getNecesidades(),
                            form.getObservacion()));
            if (!encontrada) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("aviso", exception.getMessage());
            return "redirect:/campo/registros/" + familiaId;
        }

        redirectAttributes.addFlashAttribute("mensaje", "Verificación registrada.");
        return "redirect:/campo/registros/" + familiaId;
    }

    @PostMapping("/campo/registros/{familiaId}/entregas")
    public String registrarEntrega(
            @PathVariable Long familiaId,
            @Valid @ModelAttribute("entregaForm") EntregaAyudaForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        noCache(response);
        if (bindingResult.hasErrors()) {
            cargarDetalle(familiaId, model);
            return "campo/verificar";
        }

        try {
            boolean encontrada = registroFamiliarService.registrarEntrega(
                    familiaId,
                    authentication.getName(),
                    new RegistrarEntregaCommand(
                            form.getTipoAyuda(),
                            form.getCantidad(),
                            form.getFechaEntrega(),
                            form.isSatisfaceNecesidadCompleta(),
                            form.getObservacion()));
            if (!encontrada) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("aviso", exception.getMessage());
            return "redirect:/campo/registros/" + familiaId;
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("entrega.invalida", exception.getMessage());
            cargarDetalle(familiaId, model);
            return "campo/verificar";
        }

        redirectAttributes.addFlashAttribute("mensaje", "Entrega registrada.");
        return "redirect:/campo/registros/" + familiaId;
    }

    @PostMapping("/campo/registros/{familiaId}/atendido")
    public String marcarAtendido(
            @PathVariable Long familiaId,
            @RequestParam(required = false) String observacion,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            if (!registroFamiliarService.marcarAtendido(
                    familiaId, authentication.getName(), observacion)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("aviso", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("aviso", exception.getMessage());
        }
        return "redirect:/campo/registros/" + familiaId;
    }

    private void cargarDetalle(Long familiaId, Model model) {
        Optional<SolicitudDetalle> detalle = registroFamiliarService.obtenerSolicitud(familiaId);
        if (detalle.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        SolicitudDetalle solicitud = detalle.get();
        model.addAttribute("solicitud", solicitud);
        model.addAttribute("situacionesDisponibles", SituacionFamiliar.values());
        model.addAttribute("estadosDisponibles", EstadoRegistro.values());
        model.addAttribute("entregasDisponibles", Necesidad.values());
        model.addAttribute("necesidadesSatisfechas", solicitud.entregas().stream()
                .filter(RegistroFamiliarService.EntregaVista::satisfaceNecesidadCompleta)
                .map(RegistroFamiliarService.EntregaVista::tipoAyuda)
                .collect(Collectors.toUnmodifiableSet()));
        model.addAttribute("puedeMarcarAtendido", registroFamiliarService.puedeMarcarAtendido(familiaId));
        if (!model.containsAttribute("verificacionForm")) {
            VerificacionSolicitudForm form = new VerificacionSolicitudForm();
            form.setNombreRegistrante(solicitud.nombreRegistrante());
            form.setNumeroIntegrantes(solicitud.numeroIntegrantes());
            form.setTelefonoContacto(solicitud.telefonoContacto());
            form.setBarrio(solicitud.barrio());
            form.setDireccion(solicitud.direccion());
            form.setDireccionReferencia(solicitud.direccionReferencia());
            form.setSituaciones(solicitud.situaciones());
            form.setNecesidades(solicitud.necesidades());
            model.addAttribute("verificacionForm", form);
        }
        if (!model.containsAttribute("entregaForm")) {
            model.addAttribute("entregaForm", new EntregaAyudaForm());
        }
    }

    private void noCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
    }
}
