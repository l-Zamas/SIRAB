package com.sirab.registro.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import com.sirab.registro.AsignacionAyudaRepository;
import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.EstadoSeguimientoNecesidad;
import com.sirab.registro.Necesidad;
import com.sirab.registro.SituacionFamiliar;
import com.sirab.priorizacion.NivelUrgencia;
import com.sirab.priorizacion.PrioridadSolicitudesService;
import com.sirab.duplicados.DeteccionDuplicadosService;
import com.sirab.duplicados.EstadoPosibleDuplicado;
import com.sirab.registro.service.AlojamientoService;
import com.sirab.registro.service.EntregaAyudaService;
import com.sirab.registro.service.InventarioService;
import com.sirab.registro.service.RegistrarEntregaAsignacionCommand;
import com.sirab.registro.service.RegistroFamiliarService;
import com.sirab.registro.service.EvidenciaFamiliaService;
import com.sirab.registro.DocumentoPrivado;
import com.sirab.registro.service.SolicitudVerificacionCommand;
import com.sirab.registro.service.RegistroFamiliarService.SolicitudDetalle;
import com.sirab.registro.service.SeguimientoNecesidadService;
import com.sirab.seguridad.entity.RolUsuario;

import org.springframework.data.domain.PageRequest;
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
    private final DeteccionDuplicadosService deteccionDuplicadosService;
    private final PrioridadSolicitudesService prioridadSolicitudesService;
    private final EvidenciaFamiliaService evidenciaFamiliaService;
    private final SeguimientoNecesidadService seguimientoNecesidadService;

    @Autowired(required = false)
    private AsignacionAyudaRepository asignacionAyudaRepository;

    @Autowired(required = false)
    private EntregaAyudaService entregaAyudaService;

    @Autowired(required = false)
    private AlojamientoService alojamientoService;

    @Autowired(required = false)
    private InventarioService inventarioService;

    public GestionEstadoRegistroController(
            RegistroFamiliarService registroFamiliarService,
            DeteccionDuplicadosService deteccionDuplicadosService,
            PrioridadSolicitudesService prioridadSolicitudesService,
            EvidenciaFamiliaService evidenciaFamiliaService,
            SeguimientoNecesidadService seguimientoNecesidadService) {
        this.registroFamiliarService = registroFamiliarService;
        this.deteccionDuplicadosService = deteccionDuplicadosService;
        this.prioridadSolicitudesService = prioridadSolicitudesService;
        this.evidenciaFamiliaService = evidenciaFamiliaService;
        this.seguimientoNecesidadService = seguimientoNecesidadService;
    }

    @GetMapping("/campo/registros")
    public String listar(
            @RequestParam(name = "urgencia", defaultValue = "TODAS") String filtroUrgencia,
            Model model,
            HttpServletResponse response) {
        noCache(response);
        Optional<NivelUrgencia> nivel = parsearFiltroUrgencia(filtroUrgencia);
        var solicitudes = prioridadSolicitudesService.listar(nivel);
        model.addAttribute("registros", solicitudes.activas());
        model.addAttribute("atendidos", solicitudes.atendidas());
        model.addAttribute("nivelesUrgencia", NivelUrgencia.values());
        model.addAttribute("urgenciaSeleccionada", nivel.map(Enum::name).orElse("TODAS"));
        return "campo/registros";
    }

    @GetMapping("/campo/duplicados")
    public String listarPosiblesDuplicados(
            @RequestParam(defaultValue = "0") int pagina,
            Model model,
            HttpServletResponse response) {
        noCache(response);
        int paginaSegura = Math.max(0, pagina);
        var posiblesDuplicados = deteccionDuplicadosService.listar(
                PageRequest.of(paginaSegura, 20));
        model.addAttribute("posiblesDuplicados", posiblesDuplicados.getContent());
        model.addAttribute("pagina", posiblesDuplicados);
        model.addAttribute("estadosDuplicado", EstadoPosibleDuplicado.values());
        return "campo/duplicados";
    }

    @PostMapping("/campo/duplicados/{id}/confirmar")
    public String confirmarDuplicado(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        return resolverDuplicado(
                id, EstadoPosibleDuplicado.DUPLICADO_CONFIRMADO, authentication, redirectAttributes);
    }

    @PostMapping("/campo/duplicados/{id}/descartar")
    public String descartarDuplicado(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        return resolverDuplicado(
                id, EstadoPosibleDuplicado.DESCARTADO, authentication, redirectAttributes);
    }

    @GetMapping("/coordinacion/solicitudes/{familiaId}")
    public String detalleCoordinacion(@PathVariable Long familiaId, Model model, HttpServletResponse response) {
        noCache(response);
        cargarDetalle(familiaId, model);
        model.addAttribute("vistaCoordinacion", true);
        return "campo/verificar";
    }

    @PostMapping("/coordinacion/solicitudes/{familiaId}/necesidades/{seguimientoId}/atender")
    public String atenderNecesidadCoordinacion(
                @PathVariable Long familiaId,
            @PathVariable Long seguimientoId,
            @RequestParam(required = false) BigDecimal cantidad,
            @RequestParam(required = false) BigDecimal cantidadSolicitada,
            @RequestParam(required = false) String estadoAlojamiento,
            @RequestParam(defaultValue = "false") boolean completa,
            @RequestParam(required = false) String observacion,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            if (cantidadSolicitada != null) {
                seguimientoNecesidadService.definirCantidadSolicitada(seguimientoId, cantidadSolicitada,
                        authentication.getName(), RolUsuario.COORDINACION);
            }
            if (estadoAlojamiento != null) {
                seguimientoNecesidadService.actualizarEstadoAlojamiento(seguimientoId, estadoAlojamiento,
                        authentication.getName(), RolUsuario.COORDINACION, observacion);
            } else if (cantidad != null) {
                seguimientoNecesidadService.actualizarCantidadEntregada(seguimientoId, cantidad,
                        authentication.getName(), RolUsuario.COORDINACION, observacion);
            }
            redirectAttributes.addFlashAttribute("mensaje", "Atención de necesidad registrada.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("aviso", exception.getMessage());
        }
        return "redirect:/coordinacion/solicitudes/" + familiaId;
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

    @GetMapping("/campo/registros/{familiaId}/evidencias/{evidenciaId}")
    public org.springframework.http.ResponseEntity<byte[]> evidencia(
            @PathVariable Long familiaId, @PathVariable Long evidenciaId) {
        try {
            DocumentoPrivado documento = evidenciaFamiliaService.obtenerDocumento(familiaId, evidenciaId);
            return org.springframework.http.ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(documento.getTipoContenido()))
                    .header("Content-Disposition", "inline; filename=\"" + documento.getNombreOriginal() + "\"")
                    .body(evidenciaFamiliaService.leer(familiaId, evidenciaId));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La evidencia no está disponible.", exception);
        }
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
            RolUsuario rol = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_PERSONAL_CAMPO"))
                            ? RolUsuario.PERSONAL_CAMPO : null;
            boolean encontrada = registroFamiliarService.verificar(
                    familiaId,
                    authentication.getName(),
                    rol,
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

    @PostMapping("/campo/registros/{familiaId}/necesidades/{seguimientoId}/verificar")
    public String verificarNecesidad(@PathVariable Long familiaId, @PathVariable Long seguimientoId,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            seguimientoNecesidadService.verificar(seguimientoId, authentication.getName(), RolUsuario.PERSONAL_CAMPO, "Verificación de campo");
            redirectAttributes.addFlashAttribute("mensaje", "Necesidad verificada.");
        } catch (RuntimeException exception) { redirectAttributes.addFlashAttribute("aviso", exception.getMessage()); }
        return "redirect:/campo/registros/" + familiaId;
    }

    @PostMapping("/campo/registros/{familiaId}/necesidades/atender")
    public String atenderNecesidades(@PathVariable Long familiaId,
            @RequestParam(required = false) List<Long> seguimientoId,
            @RequestParam(required = false) List<BigDecimal> cantidad,
            @RequestParam(required = false) List<Boolean> completa,
            @RequestParam(required = false) List<String> observacion,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            RolUsuario rol = RolUsuario.PERSONAL_CAMPO;
            List<SeguimientoNecesidadService.AtencionNecesidadCommand> comandos = new java.util.ArrayList<>();
            for (int i = 0; seguimientoId != null && i < seguimientoId.size(); i++) {
                comandos.add(new SeguimientoNecesidadService.AtencionNecesidadCommand(
                        seguimientoId.get(i), valor(cantidad, i), valorBooleano(completa, i),
                        null, valor(observacion, i)));
            }
            seguimientoNecesidadService.actualizarAtenciones(comandos, authentication.getName(), rol);
            redirectAttributes.addFlashAttribute("mensaje", "Atenciones registradas correctamente.");
        } catch (RuntimeException exception) { redirectAttributes.addFlashAttribute("aviso", exception.getMessage()); }
        return "redirect:/campo/registros/" + familiaId;
    }

    @PostMapping("/campo/registros/{familiaId}/necesidades/{seguimientoId}/atender")
    public String atenderNecesidad(@PathVariable Long familiaId, @PathVariable Long seguimientoId,
            @RequestParam(required = false) BigDecimal cantidad,
            @RequestParam(required = false) BigDecimal cantidadSolicitada,
            @RequestParam(defaultValue = "false") boolean completa,
            @RequestParam(required = false) String observacion, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            RolUsuario rol = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_COORDINACION"))
                    ? RolUsuario.COORDINACION : RolUsuario.PERSONAL_CAMPO;
            if (cantidadSolicitada != null) {
                seguimientoNecesidadService.definirCantidadSolicitada(seguimientoId, cantidadSolicitada,
                        authentication.getName(), rol);
            }
            if (completa || cantidad == null) {
                seguimientoNecesidadService.marcarAtendida(seguimientoId, authentication.getName(), rol, observacion);
            } else {
                seguimientoNecesidadService.actualizarAtencion(seguimientoId, cantidad, false,
                        authentication.getName(), rol, null, observacion);
            }
            redirectAttributes.addFlashAttribute("mensaje", "Necesidad atendida correctamente.");
        } catch (RuntimeException exception) { redirectAttributes.addFlashAttribute("aviso", exception.getMessage()); }
        return "redirect:/campo/registros/" + familiaId;
    }

    private static <T> T valor(List<T> valores, int indice) {
        return valores != null && indice < valores.size() ? valores.get(indice) : null;
    }

    private static boolean valorBooleano(List<Boolean> valores, int indice) {
        return Boolean.TRUE.equals(valor(valores, indice));
    }

    @PostMapping("/campo/registros/{familiaId}/entregas")
    public String rechazarEntregaLegacy(
            @PathVariable Long familiaId,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("aviso",
                "Las entregas efectivas deben registrarse mediante una asignación y un lote de inventario.");
        return "redirect:/campo/registros/" + familiaId;
    }

    @PostMapping("/campo/registros/{familiaId}/asignaciones/{asignacionId}/entrega")
    public String registrarEntregaAsignacion(
            @PathVariable Long familiaId,
            @PathVariable Long asignacionId,
            @RequestParam BigDecimal cantidad,
            @RequestParam(required = false) Long loteId,
            @RequestParam(required = false) String observacion,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (entregaAyudaService == null || asignacionAyudaRepository == null
                || asignacionAyudaRepository.findById(asignacionId)
                        .map(asignacion -> !java.util.Objects.equals(asignacion.getFamilia().getId(), familiaId))
                        .orElse(true)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        try {
            entregaAyudaService.registrarEntrega(
                    new RegistrarEntregaAsignacionCommand(
                            asignacionId,
                            cantidad,
                            loteId,
                            Instant.now(),
                            observacion),
                    authentication.getName());
            redirectAttributes.addFlashAttribute("mensaje", "Entrega por asignación registrada correctamente.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("aviso", exception.getMessage());
        }
        return "redirect:/campo/registros/" + familiaId;
    }

    @PostMapping("/coordinacion/solicitudes/{familiaId}/atendido")
    public String marcarAtendidoCoordinacion(
            @PathVariable Long familiaId,
            @RequestParam(required = false) String observacion,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        return procesarCierre(familiaId, authentication, observacion, redirectAttributes,
                "/coordinacion/solicitudes/");
    }

    @PostMapping("/campo/registros/{familiaId}/atendido")
    public String marcarAtendido(
            @PathVariable Long familiaId,
            @RequestParam(required = false) String observacion,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        return procesarCierre(familiaId, authentication, observacion, redirectAttributes,
                "/campo/registros/");
    }

    private String procesarCierre(Long familiaId, Authentication authentication, String observacion,
            RedirectAttributes redirectAttributes, String rutaBase) {
        try {
            if (!registroFamiliarService.marcarAtendido(familiaId, authentication.getName(), observacion)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        } catch (IllegalStateException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("aviso", exception.getMessage());
        }
        return "redirect:" + rutaBase + familiaId;
    }

    private void cargarDetalle(Long familiaId, Model model) {
        Optional<SolicitudDetalle> detalle = registroFamiliarService.obtenerSolicitud(familiaId);
        if (detalle.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        SolicitudDetalle solicitud = detalle.get();
        model.addAttribute("solicitud", solicitud);
        List<com.sirab.registro.SeguimientoNecesidad> seguimientos = seguimientoNecesidadService.listar(familiaId);
        model.addAttribute("seguimientosNecesidad", seguimientos);
        model.addAttribute("hayNecesidadesAtendibles", seguimientos.stream().anyMatch(seg ->
                seg.isVigente()
                        && seg.getEstado() != EstadoSeguimientoNecesidad.SATISFECHA
                        && seg.getNecesidad() != Necesidad.ALOJAMIENTO));
        model.addAttribute("historialNecesidad", seguimientoNecesidadService.historial(familiaId));
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
        if (asignacionAyudaRepository != null) {
            var asignaciones = asignacionAyudaRepository.findByFamiliaIdOrderByAsignadaAtDescIdAsc(familiaId);
            model.addAttribute("asignaciones", asignaciones);
            Map<Long, java.util.List<com.sirab.registro.LoteInventario>> lotesPorArticulo = new HashMap<>();
            if (inventarioService != null) {
                asignaciones.forEach(asignacion -> lotesPorArticulo.put(
                        asignacion.getArticulo().getId(),
                        inventarioService.obtenerLotesDisponiblesParaEntrega(asignacion.getArticulo().getId())));
            }
            model.addAttribute("lotesPorArticulo", lotesPorArticulo);
        }
        model.addAttribute("evidencias", evidenciaFamiliaService.listar(familiaId));
        if (entregaAyudaService != null) {
            model.addAttribute("entregasRealizadas",
                    entregaAyudaService.listarEntregasPorFamilia(familiaId));
        }
        if (alojamientoService != null) {
            model.addAttribute("alojamientoSatisfecho",
                    alojamientoService.estaAlojamientoSatisfecho(familiaId));
            model.addAttribute("solicitudesAlojamiento",
                    alojamientoService.listarSolicitudesPorFamilia(familiaId));
        }
    }

    private String resolverDuplicado(
            Long id,
            EstadoPosibleDuplicado resultado,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            if (!deteccionDuplicadosService.resolver(id, resultado, authentication.getName())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("aviso", exception.getMessage());
            return "redirect:/campo/duplicados";
        }
        redirectAttributes.addFlashAttribute(
                "mensaje",
                resultado == EstadoPosibleDuplicado.DUPLICADO_CONFIRMADO
                        ? "La relación se marcó como duplicado confirmado."
                        : "La coincidencia se descartó.");
        return "redirect:/campo/duplicados";
    }

    private void noCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
    }

    private Optional<NivelUrgencia> parsearFiltroUrgencia(String filtroUrgencia) {
        String filtro = filtroUrgencia.trim().toUpperCase(Locale.ROOT);
        if ("TODAS".equals(filtro)) {
            return Optional.empty();
        }
        try {
            return Optional.of(NivelUrgencia.valueOf(filtro));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nivel de urgencia no es válido.");
        }
    }
}
