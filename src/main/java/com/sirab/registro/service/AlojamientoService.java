package com.sirab.registro.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.sirab.registro.TipoDocumentoPrivado;
import com.sirab.registro.SolicitudAlojamientoRepository;
import com.sirab.registro.VerificacionAlojamiento;
import com.sirab.registro.VerificacionAlojamientoRepository;

@Service
public class AlojamientoService {

    private final FamiliaRepository familiaRepository;
    private final DocumentoPrivadoRepository documentoRepository;
    private final SolicitudAlojamientoRepository solicitudRepository;
    private final VerificacionAlojamientoRepository verificacionRepository;
    private final AprobacionProvisionalAlojamientoRepository aprobacionProvisionalRepository;
    private final AprobacionDefinitivaAlojamientoRepository aprobacionDefinitivaRepository;
    private final ConfirmacionPrestacionAlojamientoRepository confirmacionPrestacionRepository;

    public AlojamientoService(
            FamiliaRepository familiaRepository,
            DocumentoPrivadoRepository documentoRepository,
            SolicitudAlojamientoRepository solicitudRepository,
            VerificacionAlojamientoRepository verificacionRepository,
            AprobacionProvisionalAlojamientoRepository aprobacionProvisionalRepository,
            AprobacionDefinitivaAlojamientoRepository aprobacionDefinitivaRepository,
            ConfirmacionPrestacionAlojamientoRepository confirmacionPrestacionRepository) {
        this.familiaRepository = familiaRepository;
        this.documentoRepository = documentoRepository;
        this.solicitudRepository = solicitudRepository;
        this.verificacionRepository = verificacionRepository;
        this.aprobacionProvisionalRepository = aprobacionProvisionalRepository;
        this.aprobacionDefinitivaRepository = aprobacionDefinitivaRepository;
        this.confirmacionPrestacionRepository = confirmacionPrestacionRepository;
    }

    @Transactional
    public SolicitudAlojamiento registrarSolicitud(Long familiaId, String usuarioCorreo, String observacion) {
        if (usuarioCorreo == null || usuarioCorreo.isBlank()) {
            throw new IllegalArgumentException("El usuario que registra la solicitud de alojamiento es obligatorio.");
        }

        Familia familia = familiaRepository.findById(familiaId)
                .orElseThrow(() -> new IllegalArgumentException("La familia especificada no existe."));

        if (!familia.getNecesidades().contains(Necesidad.ALOJAMIENTO)) {
            throw new IllegalStateException("La familia no tiene registrada la necesidad de alojamiento.");
        }

        if (familia.getEstado() != EstadoRegistro.PENDIENTE && familia.getEstado() != EstadoRegistro.VERIFICADO) {
            throw new IllegalStateException("No se puede registrar solicitud de alojamiento para una familia en estado " + familia.getEstado());
        }

        SolicitudAlojamiento solicitud = new SolicitudAlojamiento(
                familia,
                Instant.now(),
                usuarioCorreo.trim(),
                observacion != null ? observacion.trim() : null);

        return solicitudRepository.save(solicitud);
    }

    @Transactional
    public VerificacionAlojamiento verificarAlojamiento(
            Long solicitudId,
            ResultadoVerificacionAlojamiento resultado,
            Long fotografiaDocumentoId,
            String usuarioCorreo,
            String observacion) {
        if (usuarioCorreo == null || usuarioCorreo.isBlank()) {
            throw new IllegalArgumentException("El usuario que verifica el alojamiento es obligatorio.");
        }
        if (resultado == null) {
            throw new IllegalArgumentException("El resultado de verificación técnica es obligatorio.");
        }
        if (fotografiaDocumentoId == null) {
            throw new IllegalArgumentException("El soporte fotográfico es obligatorio para verificar el alojamiento.");
        }

        SolicitudAlojamiento solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("La solicitud de alojamiento no existe."));

        DocumentoPrivado fotografia = documentoRepository.findById(fotografiaDocumentoId)
                .orElseThrow(() -> new IllegalArgumentException("El documento fotográfico especificado no existe."));
        exigirTipoDocumento(fotografia, TipoDocumentoPrivado.EVIDENCIA_ALOJAMIENTO,
                "La verificación requiere un documento clasificado como EVIDENCIA_ALOJAMIENTO.");

        VerificacionAlojamiento verificacion = new VerificacionAlojamiento(
                solicitud,
                resultado,
                fotografia,
                Instant.now(),
                usuarioCorreo.trim(),
                observacion != null ? observacion.trim() : null);

        return verificacionRepository.save(verificacion);
    }

    @Transactional
    public AprobacionProvisionalAlojamiento aprobarProvisionalmente(
            Long solicitudId,
            DecisionAyuda decision,
            String usuarioCorreo,
            String observacion) {
        if (usuarioCorreo == null || usuarioCorreo.isBlank()) {
            throw new IllegalArgumentException("El usuario que toma la decisión provisional es obligatorio.");
        }
        if (decision == null) {
            throw new IllegalArgumentException("La decisión provisional es obligatoria.");
        }

        SolicitudAlojamiento solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("La solicitud de alojamiento no existe."));

        VerificacionAlojamiento ultimaVerificacion = verificacionRepository
                .findFirstBySolicitud_IdOrderByVerificadaAtDesc(solicitudId)
                .orElseThrow(() -> new IllegalStateException("No se puede emitir aprobación provisional sin verificación previa del alojamiento."));

        if (decision == DecisionAyuda.APROBADA && ultimaVerificacion.getResultado() != ResultadoVerificacionAlojamiento.VERIFICADA) {
            throw new IllegalStateException("No se puede aprobar provisionalmente un alojamiento que no fue técnicamente VERIFICADO.");
        }

        AprobacionProvisionalAlojamiento aprobacion = new AprobacionProvisionalAlojamiento(
                solicitud,
                decision,
                Instant.now(),
                usuarioCorreo.trim(),
                observacion != null ? observacion.trim() : null);

        return aprobacionProvisionalRepository.save(aprobacion);
    }

    @Transactional
    public AprobacionDefinitivaAlojamiento registrarAprobacionDefinitiva(
            Long solicitudId,
            String institucion,
            String referenciaExterna,
            DecisionAyuda decision,
            Long documentoDecisionId,
            String usuarioCorreo,
            String observacion) {
        if (usuarioCorreo == null || usuarioCorreo.isBlank()) {
            throw new IllegalArgumentException("El usuario que registra la aprobación definitiva es obligatorio.");
        }
        if (institucion == null || institucion.isBlank()) {
            throw new IllegalArgumentException("La institución competente que emite la decisión definitiva es obligatoria.");
        }
        if (decision == null) {
            throw new IllegalArgumentException("La decisión definitiva es obligatoria.");
        }

        SolicitudAlojamiento solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("La solicitud de alojamiento no existe."));

        AprobacionProvisionalAlojamiento ultimaProvisional = aprobacionProvisionalRepository
                .findFirstBySolicitud_IdOrderByDecididaAtDesc(solicitudId)
                .orElseThrow(() -> new IllegalStateException("No se puede emitir aprobación definitiva sin aprobación provisional previa."));

        if (decision == DecisionAyuda.APROBADA && ultimaProvisional.getDecision() != DecisionAyuda.APROBADA) {
            throw new IllegalStateException("No se puede otorgar aprobación definitiva si la aprobación provisional no fue APROBADA.");
        }

        DocumentoPrivado documentoDecision = null;
        if (decision == DecisionAyuda.APROBADA && documentoDecisionId == null) {
            throw new IllegalArgumentException("La aprobación definitiva requiere un documento de decisión institucional.");
        }
        if (documentoDecisionId != null) {
            documentoDecision = documentoRepository.findById(documentoDecisionId)
                    .orElseThrow(() -> new IllegalArgumentException("El documento de soporte de la decisión no existe."));
            exigirTipoDocumento(documentoDecision, TipoDocumentoPrivado.DECISION_ENTIDAD,
                    "La aprobación definitiva requiere un documento clasificado como DECISION_ENTIDAD.");
        }

        Instant ahora = Instant.now();
        AprobacionDefinitivaAlojamiento definitiva = new AprobacionDefinitivaAlojamiento(
                solicitud,
                institucion.trim(),
                referenciaExterna != null ? referenciaExterna.trim() : null,
                decision,
                ahora,
                documentoDecision,
                ahora,
                usuarioCorreo.trim(),
                observacion != null ? observacion.trim() : null);

        return aprobacionDefinitivaRepository.save(definitiva);
    }

    @Transactional
    public ConfirmacionPrestacionAlojamiento confirmarPrestacion(
            Long solicitudId,
            Long aprobacionDefinitivaId,
            Instant proporcionadoAt,
            Long documentoEvidenciaId,
            String confirmadoPor,
            String observacion) {
        if (confirmadoPor == null || confirmadoPor.isBlank()) {
            throw new IllegalArgumentException("El usuario o funcionario que confirma la prestación es obligatorio.");
        }
        if (proporcionadoAt == null) {
            throw new IllegalArgumentException("La fecha u hora en que se proporcionó efectivamente el alojamiento es obligatoria.");
        }

        SolicitudAlojamiento solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("La solicitud de alojamiento no existe."));

        AprobacionDefinitivaAlojamiento definitiva = aprobacionDefinitivaRepository.findById(aprobacionDefinitivaId)
                .orElseThrow(() -> new IllegalArgumentException("La aprobación definitiva especificada no existe."));

        if (!java.util.Objects.equals(definitiva.getSolicitud().getId(), solicitud.getId())) {
            throw new IllegalArgumentException("La aprobación definitiva no corresponde a la solicitud indicada.");
        }

        if (definitiva.getDecision() != DecisionAyuda.APROBADA) {
            throw new IllegalStateException("No se puede confirmar la prestación de un alojamiento con decisión definitiva RECHAZADA.");
        }

        if (documentoEvidenciaId == null) {
            throw new IllegalArgumentException("La confirmación de prestación requiere evidencia documental.");
        }
        DocumentoPrivado docEvidencia = documentoRepository.findById(documentoEvidenciaId)
                .orElseThrow(() -> new IllegalArgumentException("El documento de evidencia de prestación no existe."));
        exigirTipoDocumento(docEvidencia, TipoDocumentoPrivado.CONFIRMACION_PRESTACION,
                "La confirmación requiere un documento clasificado como CONFIRMACION_PRESTACION.");

        ConfirmacionPrestacionAlojamiento confirmacion = new ConfirmacionPrestacionAlojamiento(
                solicitud,
                definitiva,
                proporcionadoAt,
                Instant.now(),
                confirmadoPor.trim(),
                docEvidencia,
                observacion != null ? observacion.trim() : null);

        return confirmacionPrestacionRepository.save(confirmacion);
    }

    private void exigirTipoDocumento(
            DocumentoPrivado documento,
            TipoDocumentoPrivado tipoEsperado,
            String mensaje) {
        if (documento.getClasificacion() != tipoEsperado) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    @Transactional(readOnly = true)
    public boolean estaAlojamientoSatisfecho(Long familiaId) {
        return confirmacionPrestacionRepository.existsBySolicitud_Familia_Id(familiaId);
    }

    @Transactional(readOnly = true)
    public List<SolicitudAlojamiento> listarSolicitudesPorFamilia(Long familiaId) {
        return solicitudRepository.findByFamilia_IdOrderByRegistradaAtDesc(familiaId);
    }
}
