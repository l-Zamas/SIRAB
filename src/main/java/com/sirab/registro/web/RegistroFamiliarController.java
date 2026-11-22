package com.sirab.registro.web;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sirab.registro.Necesidad;
import com.sirab.registro.SituacionFamiliar;
import com.sirab.registro.service.RegistroFamiliarService;

@Controller
public class RegistroFamiliarController {

    private static final String BORRADOR = "registroFamiliarDraft";
    private static final String CODIGO_CONSULTA = "codigoConsulta";
    private static final int ETAPA_CONSENTIMIENTO = 2;
    private static final int ETAPA_DATOS = 3;
    private static final int ETAPA_UBICACION = 4;
    private static final int ETAPA_SITUACION = 5;
    private static final int ETAPA_NECESIDADES = 6;
    private static final int ETAPA_DOCUMENTO = 7;

    private final RegistroFamiliarService registroFamiliarService;
    private final com.sirab.registro.service.EvidenciaFamiliaService evidenciaFamiliaService;

    public RegistroFamiliarController(RegistroFamiliarService registroFamiliarService,
            com.sirab.registro.service.EvidenciaFamiliaService evidenciaFamiliaService) {
        this.registroFamiliarService = registroFamiliarService;
        this.evidenciaFamiliaService = evidenciaFamiliaService;
    }

    @GetMapping("/familia/registro")
    public String bienvenida(HttpServletResponse response) {
        noCache(response);
        return "familia/bienvenida";
    }

    @GetMapping("/familia/registro/consentimiento")
    public String consentimiento(HttpSession session, Model model, HttpServletResponse response) {
        noCache(response);
        RegistroFamiliarDraft draft = draft(session);
        ConsentimientoForm form = new ConsentimientoForm();
        form.setAceptado(draft.getConsentimientoAt() != null);
        model.addAttribute("consentimientoForm", form);
        return "familia/consentimiento";
    }

    @PostMapping("/familia/registro/consentimiento")
    public String aceptarConsentimiento(
            @Valid @ModelAttribute("consentimientoForm") ConsentimientoForm form,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            HttpServletResponse response) {
        noCache(response);
        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    BindingResult.MODEL_KEY_PREFIX + "consentimientoForm",
                    RegistroFormErrors.withoutRejectedValues(form, bindingResult));
            return "familia/consentimiento";
        }

        RegistroFamiliarDraft draft = draft(session);
        if (draft.getConsentimientoAt() == null) {
            draft.setConsentimientoAt(Instant.now());
        }
        avanzarEtapa(draft, ETAPA_CONSENTIMIENTO);
        session.setAttribute(BORRADOR, draft);
        return "redirect:/familia/registro/datos";
    }

    @GetMapping("/familia/registro/datos")
    public String datos(HttpSession session, Model model, HttpServletResponse response) {
        noCache(response);
        RegistroFamiliarDraft draft = draft(session);
        if (draft.getEtapa() < ETAPA_CONSENTIMIENTO) {
            return "redirect:/familia/registro/consentimiento";
        }

        DatosFamiliaForm form = new DatosFamiliaForm();
        form.setNombreRegistrante(draft.getNombreRegistrante());
        form.setNumeroIntegrantes(draft.getNumeroIntegrantes());
        form.setTelefonoContacto(draft.getTelefonoContacto());
        model.addAttribute("datosFamiliaForm", form);
        return "familia/datos-familia";
    }

    @PostMapping("/familia/registro/datos")
    public String guardarDatos(
            @Valid @ModelAttribute("datosFamiliaForm") DatosFamiliaForm form,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            HttpServletResponse response) {
        noCache(response);
        RegistroFamiliarDraft draft = draft(session);
        if (draft.getEtapa() < ETAPA_CONSENTIMIENTO) {
            return "redirect:/familia/registro/consentimiento";
        }
        if (bindingResult.hasErrors()) {
            DatosFamiliaForm safeForm = new DatosFamiliaForm();
            if (!bindingResult.hasFieldErrors("nombreRegistrante")) {
                safeForm.setNombreRegistrante(form.getNombreRegistrante());
            }
            if (!bindingResult.hasFieldErrors("numeroIntegrantes")) {
                safeForm.setNumeroIntegrantes(form.getNumeroIntegrantes());
            }
            if (!bindingResult.hasFieldErrors("telefonoContacto")) {
                safeForm.setTelefonoContacto(form.getTelefonoContacto());
            }
            model.addAttribute("datosFamiliaForm", safeForm);
            model.addAttribute(
                    BindingResult.MODEL_KEY_PREFIX + "datosFamiliaForm",
                    RegistroFormErrors.withoutRejectedValues(safeForm, bindingResult));
            return "familia/datos-familia";
        }

        draft.setNombreRegistrante(form.getNombreRegistrante().trim());
        draft.setNumeroIntegrantes(form.getNumeroIntegrantes());
        draft.setTelefonoContacto(form.getTelefonoContacto().trim());
        avanzarEtapa(draft, ETAPA_DATOS);
        session.setAttribute(BORRADOR, draft);
        return "redirect:/familia/registro/ubicacion";
    }

    @GetMapping("/familia/registro/ubicacion")
    public String ubicacion(HttpSession session, Model model, HttpServletResponse response) {
        noCache(response);
        RegistroFamiliarDraft draft = draft(session);
        if (draft.getEtapa() < ETAPA_DATOS) {
            return "redirect:/familia/registro/datos";
        }

        UbicacionForm form = new UbicacionForm();
        form.setBarrio(draft.getBarrio());
        form.setDireccion(draft.getDireccion());
        form.setDireccionReferencia(draft.getDireccionReferencia());
        model.addAttribute("ubicacionForm", form);
        return "familia/ubicacion";
    }

    @PostMapping("/familia/registro/ubicacion")
    public String guardarUbicacion(
            @Valid @ModelAttribute("ubicacionForm") UbicacionForm form,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            HttpServletResponse response) {
        noCache(response);
        RegistroFamiliarDraft draft = draft(session);
        if (draft.getEtapa() < ETAPA_DATOS) {
            return "redirect:/familia/registro/datos";
        }
        if (bindingResult.hasErrors()) {
            UbicacionForm safeForm = new UbicacionForm();
            if (!bindingResult.hasFieldErrors("barrio")) {
                safeForm.setBarrio(form.getBarrio());
            }
            if (!bindingResult.hasFieldErrors("direccion")) {
                safeForm.setDireccion(form.getDireccion());
            }
            if (!bindingResult.hasFieldErrors("direccionReferencia")) {
                safeForm.setDireccionReferencia(form.getDireccionReferencia());
            }
            model.addAttribute("ubicacionForm", safeForm);
            model.addAttribute(
                    BindingResult.MODEL_KEY_PREFIX + "ubicacionForm",
                    RegistroFormErrors.withoutRejectedValues(safeForm, bindingResult));
            return "familia/ubicacion";
        }

        draft.setBarrio(form.getBarrio().trim());
        draft.setDireccion(form.getDireccion().trim());
        draft.setDireccionReferencia(normalizeOptional(form.getDireccionReferencia()));
        avanzarEtapa(draft, ETAPA_UBICACION);
        session.setAttribute(BORRADOR, draft);
        return "redirect:/familia/registro/situacion";
    }

    @GetMapping("/familia/registro/situacion")
    public String situacion(HttpSession session, Model model, HttpServletResponse response) {
        noCache(response);
        RegistroFamiliarDraft draft = draft(session);
        if (draft.getEtapa() < ETAPA_UBICACION) {
            return "redirect:/familia/registro/ubicacion";
        }

        SituacionForm form = new SituacionForm();
        Set<SituacionFamiliar> situaciones = draft.getSituaciones();
        form.setMenoresDeCinco(situaciones.contains(SituacionFamiliar.MENORES_DE_CINCO));
        form.setAdultosMayores(situaciones.contains(SituacionFamiliar.ADULTOS_MAYORES));
        form.setPersonasConDiscapacidad(situaciones.contains(SituacionFamiliar.PERSONAS_CON_DISCAPACIDAD));
        form.setMujeresGestantes(situaciones.contains(SituacionFamiliar.MUJERES_GESTANTES));
        model.addAttribute("situacionForm", form);
        return "familia/situacion";
    }

    @PostMapping("/familia/registro/situacion")
    public String guardarSituacion(
            @ModelAttribute("situacionForm") SituacionForm form,
            HttpSession session,
            HttpServletResponse response) {
        noCache(response);
        RegistroFamiliarDraft draft = draft(session);
        if (draft.getEtapa() < ETAPA_UBICACION) {
            return "redirect:/familia/registro/ubicacion";
        }

        EnumSet<SituacionFamiliar> situaciones = EnumSet.noneOf(SituacionFamiliar.class);
        if (form.isMenoresDeCinco()) situaciones.add(SituacionFamiliar.MENORES_DE_CINCO);
        if (form.isAdultosMayores()) situaciones.add(SituacionFamiliar.ADULTOS_MAYORES);
        if (form.isPersonasConDiscapacidad()) situaciones.add(SituacionFamiliar.PERSONAS_CON_DISCAPACIDAD);
        if (form.isMujeresGestantes()) situaciones.add(SituacionFamiliar.MUJERES_GESTANTES);
        draft.setSituaciones(situaciones);
        avanzarEtapa(draft, ETAPA_SITUACION);
        session.setAttribute(BORRADOR, draft);
        return "redirect:/familia/registro/necesidades";
    }

    @GetMapping("/familia/registro/necesidades")
    public String necesidades(HttpSession session, Model model, HttpServletResponse response) {
        noCache(response);
        RegistroFamiliarDraft draft = draft(session);
        if (draft.getEtapa() < ETAPA_SITUACION) {
            return "redirect:/familia/registro/situacion";
        }

        NecesidadesForm form = new NecesidadesForm();
        form.setNecesidades(draft.getNecesidades());
        model.addAttribute("necesidadesForm", form);
        model.addAttribute("necesidadesDisponibles", Necesidad.values());
        return "familia/necesidades";
    }

    @PostMapping("/familia/registro/necesidades")
    public String guardarNecesidades(
            @ModelAttribute("necesidadesForm") NecesidadesForm form,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            HttpServletResponse response) {
        noCache(response);
        RegistroFamiliarDraft draft = draft(session);
        if (draft.getEtapa() < ETAPA_SITUACION) {
            return "redirect:/familia/registro/situacion";
        }
        if (bindingResult.hasErrors()) {
            form.setNecesidades(Set.of());
            model.addAttribute(
                    BindingResult.MODEL_KEY_PREFIX + "necesidadesForm",
                    RegistroFormErrors.withoutRejectedValues(form, bindingResult));
            model.addAttribute("necesidadesDisponibles", Necesidad.values());
            return "familia/necesidades";
        }

        draft.setNecesidades(form.getNecesidades());
        avanzarEtapa(draft, ETAPA_NECESIDADES);
        session.setAttribute(BORRADOR, draft);
        return "redirect:/familia/registro/documento";
    }

    @GetMapping("/familia/registro/documento")
    public String documento(HttpSession session, Model model, HttpServletResponse response) {
        noCache(response);
        RegistroFamiliarDraft draft = draft(session);
        if (draft.getEtapa() < ETAPA_NECESIDADES) {
            return "redirect:/familia/registro/necesidades";
        }
        model.addAttribute("registroFamiliarDraft", draft);
        return "familia/documento-diferido";
    }

    @PostMapping("/familia/registro/documento")
    public String continuarSinDocumento(
            @RequestParam(name = "evidenciaAlojamiento", required = false) org.springframework.web.multipart.MultipartFile evidenciaAlojamiento,
            @RequestParam(name = "evidenciaMedicamentos", required = false) MultipartFile evidenciaMedicamentos,
            HttpSession session, Model model, HttpServletResponse response) {
        noCache(response);
        RegistroFamiliarDraft draft = draft(session);
        if (draft.getEtapa() < ETAPA_NECESIDADES) {
            return "redirect:/familia/registro/necesidades";
        }
        String error = null;
        if (draft.getNecesidades().contains(Necesidad.ALOJAMIENTO)
                && (evidenciaAlojamiento == null || evidenciaAlojamiento.isEmpty())) {
            error = "La evidencia de alojamiento es obligatoria.";
        } else if (draft.getNecesidades().contains(Necesidad.MEDICAMENTOS)
                && (evidenciaMedicamentos == null || evidenciaMedicamentos.isEmpty())) {
            error = "La evidencia de medicamentos es obligatoria.";
        }
        try {
        if (error == null && draft.getNecesidades().contains(Necesidad.ALOJAMIENTO)) {
            draft.setEvidenciaAlojamiento(evidenciaFamiliaService.guardarTemporal(evidenciaAlojamiento, com.sirab.registro.TipoDocumentoPrivado.EVIDENCIA_ALOJAMIENTO));
            draft.setNombreEvidenciaAlojamiento(evidenciaAlojamiento.getOriginalFilename());
            draft.setTipoEvidenciaAlojamiento(evidenciaAlojamiento.getContentType());
        }
        if (error == null && draft.getNecesidades().contains(Necesidad.MEDICAMENTOS)) {
            draft.setEvidenciaMedicamentos(evidenciaFamiliaService.guardarTemporal(evidenciaMedicamentos, com.sirab.registro.TipoDocumentoPrivado.SOPORTE_MEDICO));
            draft.setNombreEvidenciaMedicamentos(evidenciaMedicamentos.getOriginalFilename());
            draft.setTipoEvidenciaMedicamentos(evidenciaMedicamentos.getContentType());
        }
        } catch (IllegalArgumentException exception) {
            error = exception.getMessage();
        }
        if (error != null) {
            model.addAttribute("registroFamiliarDraft", draft);
            model.addAttribute("errorEvidencia", error);
            return "familia/documento-diferido";
        }
        avanzarEtapa(draft, ETAPA_DOCUMENTO);
        session.setAttribute(BORRADOR, draft);
        return "redirect:/familia/registro/resumen";
    }

    @GetMapping("/familia/registro/resumen")
    public String resumen(HttpSession session, Model model, HttpServletResponse response) {
        noCache(response);
        RegistroFamiliarDraft draft = draft(session);
        if (draft.getEtapa() < ETAPA_DOCUMENTO) {
            return "redirect:/familia/registro/documento";
        }

        model.addAttribute("registro", draft);
        model.addAttribute("situacionesDisponibles", SituacionFamiliar.values());
        model.addAttribute("necesidadesDisponibles", Necesidad.values());
        return "familia/resumen";
    }

    @PostMapping("/familia/registro/confirmar")
    public String confirmar(HttpSession session, HttpServletResponse response) {
        noCache(response);
        RegistroFamiliarDraft draft = draft(session);
        if (draft.getEtapa() < ETAPA_DOCUMENTO) {
            return "redirect:/familia/registro/documento";
        }
        if (draft.getNecesidades().contains(Necesidad.ALOJAMIENTO)
                && (draft.getEvidenciaAlojamiento() == null || draft.getEvidenciaAlojamiento().isBlank())) {
            return "redirect:/familia/registro/documento";
        }
        if (draft.getNecesidades().contains(Necesidad.MEDICAMENTOS)
                && (draft.getEvidenciaMedicamentos() == null || draft.getEvidenciaMedicamentos().isBlank())) {
            return "redirect:/familia/registro/documento";
        }
        String codigoConsulta = registroFamiliarService.registrar(draft.toCommand());
        session.setAttribute(CODIGO_CONSULTA, codigoConsulta);
        session.removeAttribute(BORRADOR);
        return "redirect:/familia/registro/confirmacion";
    }

    @GetMapping("/familia/registro/confirmacion")
    public String confirmacion(HttpSession session, Model model, HttpServletResponse response) {
        noCache(response);
        model.addAttribute("codigoConsulta", session.getAttribute(CODIGO_CONSULTA));
        session.removeAttribute(CODIGO_CONSULTA);
        return "familia/confirmacion";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String errorCargaGrande(HttpSession session, Model model, HttpServletResponse response) {
        noCache(response);
        model.addAttribute("registroFamiliarDraft", draft(session));
        model.addAttribute("errorEvidencia", "El archivo supera el límite permitido de 5 MB.");
        return "familia/documento-diferido";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String errorEvidencia(IllegalArgumentException exception, HttpSession session, Model model, HttpServletResponse response) {
        noCache(response);
        model.addAttribute("registroFamiliarDraft", draft(session));
        model.addAttribute("errorEvidencia", exception.getMessage());
        return "familia/documento-diferido";
    }

    @ExceptionHandler(DataAccessException.class)
    public String errorPersistencia(HttpServletResponse response, Model model) {
        noCache(response);
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        model.addAttribute("mensaje", "No fue posible guardar el registro. Intenta nuevamente más tarde.");
        return "familia/error-registro";
    }

    private RegistroFamiliarDraft draft(HttpSession session) {
        Object current = session.getAttribute(BORRADOR);
        if (current instanceof RegistroFamiliarDraft draft) {
            return draft;
        }
        RegistroFamiliarDraft draft = new RegistroFamiliarDraft();
        session.setAttribute(BORRADOR, draft);
        return draft;
    }

    private void avanzarEtapa(RegistroFamiliarDraft draft, int etapa) {
        draft.setEtapa(Math.max(draft.getEtapa(), etapa));
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void noCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
    }
}
