package com.sirab.registro.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import com.sirab.registro.*;
import com.sirab.seguridad.entity.RolUsuario;

@Service
public class SeguimientoNecesidadService {
    private final SeguimientoNecesidadRepository seguimientoRepository;
    private final HistorialNecesidadRepository historialRepository;
    private final FamiliaRepository familiaRepository;
    @Autowired(required = false)
    private EstadoSolicitudHistorialRepository estadoHistorialRepository;

    public SeguimientoNecesidadService(SeguimientoNecesidadRepository seguimientoRepository, HistorialNecesidadRepository historialRepository, FamiliaRepository familiaRepository) {
        this.seguimientoRepository = seguimientoRepository; this.historialRepository = historialRepository; this.familiaRepository = familiaRepository;
    }

    @Transactional
    public List<SeguimientoNecesidad> inicializar(Long familiaId, String correo, RolUsuario rol) {
        Familia familia = familiaRepository.findById(familiaId).orElseThrow(() -> new IllegalArgumentException("La solicitud no existe."));
        for (Necesidad necesidad : familia.getNecesidades()) {
            SeguimientoNecesidad seguimiento = seguimientoRepository.findByFamilia_IdAndNecesidad(familiaId, necesidad)
                    .orElseGet(() -> seguimientoRepository.save(new SeguimientoNecesidad(familia, necesidad,
                            EstadoSeguimientoNecesidad.DECLARADA, Instant.now())));
            seguimiento.setVigente(true);
            if (seguimiento.getId() != null) {
                seguimientoRepository.save(seguimiento);
            }
            if (seguimiento.getEstado() == EstadoSeguimientoNecesidad.DECLARADA) {
                registrar(seguimiento, null, EstadoSeguimientoNecesidad.DECLARADA, correo, rol, null, "Necesidad vigente inicializada.");
            }
        }
        return seguimientoRepository.findByFamilia_IdOrderByNecesidadAsc(familiaId);
    }

    @Transactional
    public void sincronizarVigencia(Long familiaId, Set<Necesidad> necesidadesVigentes, String correo, RolUsuario rol,
            String observacion) {
        List<SeguimientoNecesidad> seguimientos = seguimientoRepository.findByFamilia_IdOrderByNecesidadAsc(familiaId);
        for (SeguimientoNecesidad seguimiento : seguimientos) {
            boolean vigente = necesidadesVigentes.contains(seguimiento.getNecesidad());
            seguimiento.setVigente(vigente);
            seguimientoRepository.save(seguimiento);
            if (!vigente) {
                registrar(seguimiento, seguimiento.getEstado(), seguimiento.getEstado(), correo, rol, null,
                        observacion != null && !observacion.isBlank()
                                ? "Necesidad descartada durante la verificación: " + observacion
                                : "Necesidad descartada durante la verificación.");
            }
        }
    }

    @Transactional
    public SeguimientoNecesidad verificar(Long seguimientoId, String correo, RolUsuario rol, String observacion) {
        return cambiar(seguimientoId, EstadoSeguimientoNecesidad.VERIFICADA, correo, rol, null, observacion);
    }

    @Transactional
    public SeguimientoNecesidad marcarAtendida(Long seguimientoId, String correo, RolUsuario rol, String observacion) {
        SeguimientoNecesidad seguimiento = seguimientoRepository.findWithLockById(seguimientoId)
                .orElseThrow(() -> new IllegalArgumentException("El seguimiento no existe."));
        exigirVigente(seguimiento);
        exigirRol(seguimiento.getNecesidad(), rol);
        if (seguimiento.getEstado() == EstadoSeguimientoNecesidad.SATISFECHA) {
            return seguimiento;
        }
        validarSatisfaccionCompleta(seguimiento);
        EstadoSeguimientoNecesidad anterior = seguimiento.getEstado();
        Instant ahora = Instant.now();
        seguimiento.setEstado(EstadoSeguimientoNecesidad.SATISFECHA);
        seguimiento.setAtendidoPor(correo);
        seguimiento.setResponsableRol(rol);
        seguimiento.setObservacion(observacion);
        seguimiento.actualizarAt(ahora);
        seguimientoRepository.save(seguimiento);
        registrar(seguimiento, anterior, EstadoSeguimientoNecesidad.SATISFECHA, correo, rol, null, observacion);
        return seguimiento;
    }

    @Transactional
    public SeguimientoNecesidad definirCantidadSolicitada(Long seguimientoId, BigDecimal cantidadSolicitada,
            String correo, RolUsuario rol) {
        Integer cantidadEntera = enteroPositivo(cantidadSolicitada, "La cantidad solicitada debe ser un entero positivo.");
        if (cantidadEntera == null) {
            throw new IllegalArgumentException("La cantidad solicitada debe ser un entero positivo.");
        }
        SeguimientoNecesidad seguimiento = seguimientoRepository.findWithLockById(seguimientoId)
                .orElseThrow(() -> new IllegalArgumentException("El seguimiento no existe."));
        exigirRol(seguimiento.getNecesidad(), rol);
        seguimiento.setCantidadSolicitada(cantidadEntera);
        return seguimientoRepository.save(seguimiento);
    }

    @Transactional
    public SeguimientoNecesidad actualizarCantidadEntregada(Long seguimientoId, BigDecimal cantidad,
            String correo, RolUsuario rol, String observacion) {
        validarEnteroPositivo(cantidad, "La cantidad entregada debe ser un entero positivo.");
        SeguimientoNecesidad seguimiento = seguimientoRepository.findWithLockById(seguimientoId)
                .orElseThrow(() -> new IllegalArgumentException("El seguimiento no existe."));
        exigirRol(seguimiento.getNecesidad(), rol);
        Integer solicitada = seguimiento.getCantidadSolicitada();
        int total = seguimiento.getCantidadEntregada() + enteroPositivo(cantidad, "La cantidad entregada debe ser un entero positivo.");
        if (solicitada != null && total > solicitada) {
            throw new IllegalArgumentException("La cantidad acumulada no puede superar la cantidad solicitada.");
        }
        EstadoSeguimientoNecesidad estado = solicitada != null && total >= solicitada
                ? EstadoSeguimientoNecesidad.SATISFECHA : EstadoSeguimientoNecesidad.PARCIALMENTE_ATENDIDA;
        EstadoSeguimientoNecesidad anterior = seguimiento.getEstado();
        seguimiento.setCantidadEntregada(total);
        seguimiento.setEstado(estado);
        seguimiento.setAtendidoPor(correo);
        seguimiento.setResponsableRol(rol);
        seguimiento.setObservacion(observacion);
        seguimiento.actualizarAt(Instant.now());
        seguimientoRepository.save(seguimiento);
        registrar(seguimiento, anterior, estado, correo, rol, cantidad, observacion);
        return seguimiento;
    }

    @Transactional
    public SeguimientoNecesidad actualizarEstadoAlojamiento(Long seguimientoId, String estado,
            String correo, RolUsuario rol, String observacion) {
        if (rol != RolUsuario.COORDINACION) throw new SecurityException("Solo COORDINACION puede gestionar alojamiento.");
        SeguimientoNecesidad seguimiento = seguimientoRepository.findWithLockById(seguimientoId)
                .orElseThrow(() -> new IllegalArgumentException("El seguimiento no existe."));
        if (seguimiento.getNecesidad() != Necesidad.ALOJAMIENTO) throw new IllegalArgumentException("La necesidad no es alojamiento.");
        EstadoSeguimientoNecesidad nuevo = switch (estado) {
            case "PENDIENTE" -> EstadoSeguimientoNecesidad.DECLARADA;
            case "ENTREGADO" -> EstadoSeguimientoNecesidad.EN_ATENCION;
            case "SATISFECHO" -> EstadoSeguimientoNecesidad.SATISFECHA;
            default -> throw new IllegalArgumentException("Estado de alojamiento no vÃ¡lido.");
        };
        EstadoSeguimientoNecesidad anterior = seguimiento.getEstado();
        seguimiento.setEstado(nuevo); seguimiento.setAtendidoPor(correo); seguimiento.setResponsableRol(rol);
        seguimiento.setObservacion(observacion); seguimiento.actualizarAt(Instant.now()); seguimientoRepository.save(seguimiento);
        registrar(seguimiento, anterior, nuevo, correo, rol, null, observacion);
        return seguimiento;
    }

    @Transactional
    public List<SeguimientoNecesidad> actualizarAtenciones(List<AtencionNecesidadCommand> comandos, String correo, RolUsuario rol) {
        if (comandos == null || comandos.isEmpty()) {
            throw new IllegalArgumentException("Debe registrar al menos una atención.");
        }
        List<SeguimientoNecesidad> actualizadas = new ArrayList<>();
        HashSet<Long> ids = new HashSet<>();
        for (AtencionNecesidadCommand comando : comandos) {
            if (comando == null || comando.seguimientoId() == null || !ids.add(comando.seguimientoId())) {
                throw new IllegalArgumentException("La necesidad seleccionada no es válida o está duplicada.");
            }
            if (comando.cantidad() == null && !comando.completa()) {
                throw new IllegalArgumentException("La cantidad atendida es obligatoria.");
            }
            if (comando.cantidad() != null) {
                actualizadas.add(actualizarAtencion(comando.seguimientoId(), comando.cantidad(), comando.completa(),
                        correo, rol, comando.evidencia(), comando.observacion()));
            } else {
                actualizadas.add(marcarAtendida(comando.seguimientoId(), correo, rol, comando.observacion()));
            }
        }
        return actualizadas;
    }

    public record AtencionNecesidadCommand(Long seguimientoId, BigDecimal cantidad, boolean completa,
            String evidencia, String observacion) { }

    @Transactional
    public SeguimientoNecesidad actualizarAtencion(Long seguimientoId, BigDecimal cantidad, boolean completa, String correo, RolUsuario rol, String evidencia, String observacion) {
        validarEnteroPositivo(cantidad, "La cantidad atendida debe ser un entero positivo.");
        SeguimientoNecesidad seguimiento = seguimientoRepository.findWithLockById(seguimientoId).orElseThrow(() -> new IllegalArgumentException("El seguimiento no existe."));
        exigirVigente(seguimiento);
        exigirRol(seguimiento.getNecesidad(), rol);
        int total = seguimiento.getCantidadEntregada() + enteroPositivo(cantidad, "La cantidad atendida debe ser un entero positivo.");
        Integer solicitada = seguimiento.getCantidadSolicitada();
        if (solicitada != null && total > solicitada) {
            throw new IllegalArgumentException("La cantidad acumulada no puede superar la cantidad solicitada.");
        }
        EstadoSeguimientoNecesidad estado;
        if (completa || (solicitada != null && total >= solicitada)) {
            estado = EstadoSeguimientoNecesidad.SATISFECHA;
        } else if (total > 0) {
            estado = EstadoSeguimientoNecesidad.PARCIALMENTE_ATENDIDA;
        } else {
            estado = EstadoSeguimientoNecesidad.EN_ATENCION;
        }
        EstadoSeguimientoNecesidad anterior = seguimiento.getEstado();
        seguimiento.setCantidadEntregada(total); seguimiento.setEstado(estado); seguimiento.setAtendidoPor(correo); seguimiento.setResponsableRol(rol); seguimiento.setEvidenciaRef(evidencia); seguimiento.setObservacion(observacion); seguimiento.actualizarAt(Instant.now());
        seguimientoRepository.save(seguimiento); registrar(seguimiento, anterior, estado, correo, rol, cantidad, observacion);
        return seguimiento;
    }

    @Transactional(readOnly = true)
    public List<SeguimientoNecesidad> listar(Long familiaId) { return seguimientoRepository.findByFamilia_IdAndVigenteTrueOrderByNecesidadAsc(familiaId); }

    @Transactional(readOnly = true)
    public List<SeguimientoNecesidad> listarHistorial(Long familiaId) { return seguimientoRepository.findByFamilia_IdOrderByNecesidadAsc(familiaId); }
    @Transactional(readOnly = true)
    public List<HistorialNecesidad> historial(Long familiaId) { return historialRepository.findBySeguimiento_Familia_IdOrderByFechaHoraAsc(familiaId); }
    @Transactional(readOnly = true)
    public boolean todasSatisfechas(Long familiaId) { List<SeguimientoNecesidad> lista = listar(familiaId); return !lista.isEmpty() && lista.stream().allMatch(this::necesidadSatisfecha); }

    private boolean necesidadSatisfecha(SeguimientoNecesidad seguimiento) {
        if (seguimiento.getNecesidad() == Necesidad.ALOJAMIENTO) {
            return seguimiento.getEstado() == EstadoSeguimientoNecesidad.SATISFECHA;
        }
        Integer solicitada = seguimiento.getCantidadSolicitada();
        Integer entregada = seguimiento.getCantidadEntregada();
        return seguimiento.getEstado() == EstadoSeguimientoNecesidad.SATISFECHA
                && entregada != null && entregada > 0
                && (solicitada == null || solicitada <= 0 || entregada >= solicitada);
    }

    private SeguimientoNecesidad cambiar(Long id, EstadoSeguimientoNecesidad nuevo, String correo, RolUsuario rol, BigDecimal cantidad, String observacion) {
        SeguimientoNecesidad seguimiento = seguimientoRepository.findWithLockById(id).orElseThrow(() -> new IllegalArgumentException("El seguimiento no existe."));
        if (rol != RolUsuario.PERSONAL_CAMPO) throw new SecurityException("Solo PERSONAL_CAMPO puede verificar necesidades.");
        EstadoSeguimientoNecesidad anterior = seguimiento.getEstado(); seguimiento.setEstado(nuevo); seguimiento.setResponsableRol(rol); seguimiento.setAtendidoPor(correo); seguimiento.setObservacion(observacion); seguimiento.actualizarAt(Instant.now()); seguimientoRepository.save(seguimiento); registrar(seguimiento, anterior, nuevo, correo, rol, cantidad, observacion); return seguimiento;
    }
    private void registrar(SeguimientoNecesidad s, EstadoSeguimientoNecesidad anterior, EstadoSeguimientoNecesidad nuevo, String correo, RolUsuario rol, BigDecimal cantidad, String observacion) { historialRepository.save(new HistorialNecesidad(s, anterior, nuevo, correo, rol, cantidad, observacion, Instant.now())); }

    private void actualizarEstadoSolicitudSiCorresponde(Familia familia, String correo, RolUsuario rol, String observacion) {
        if (estadoHistorialRepository == null || familia.getEstado() == EstadoRegistro.ATENDIDO
                || !todasSatisfechas(familia.getId())) {
            return;
        }
        EstadoRegistro anterior = familia.getEstado();
        if (anterior != EstadoRegistro.EN_ATENCION) {
            return;
        }
        familia.setEstado(EstadoRegistro.ATENDIDO);
        familiaRepository.save(familia);
        estadoHistorialRepository.save(new EstadoSolicitudHistorial(familia, anterior, EstadoRegistro.ATENDIDO,
                ActorTipoSolicitud.USUARIO, correo, Instant.now(),
                observacion != null && !observacion.isBlank() ? observacion : "Todas las necesidades fueron satisfechas."));
    }

    private void exigirVigente(SeguimientoNecesidad seguimiento) {
        if (!seguimiento.isVigente()) {
            throw new IllegalStateException("La necesidad fue descartada durante la verificación y no admite nuevas atenciones.");
        }
    }

    private void validarSatisfaccionCompleta(SeguimientoNecesidad seguimiento) {
        if (seguimiento.getNecesidad() == Necesidad.ALOJAMIENTO) {
            return;
        }
        Integer solicitada = seguimiento.getCantidadSolicitada();
        Integer entregada = seguimiento.getCantidadEntregada();
        if (solicitada == null || entregada == null || entregada < solicitada) {
            throw new IllegalStateException("La necesidad no puede marcarse como satisfecha antes de completar la cantidad solicitada.");
        }
    }

    private Integer enteroPositivo(BigDecimal cantidad, String mensaje) {
        if (cantidad == null || cantidad.signum() <= 0 || cantidad.stripTrailingZeros().scale() > 0
                || cantidad.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new IllegalArgumentException(mensaje);
        }
        return cantidad.intValueExact();
    }

    private void validarEnteroPositivo(BigDecimal cantidad, String mensaje) {
        enteroPositivo(cantidad, mensaje);
    }

    private void exigirRol(Necesidad necesidad, RolUsuario rol) { if (rol == null) throw new SecurityException("El rol es obligatorio."); if (necesidad == Necesidad.ALOJAMIENTO && rol != RolUsuario.COORDINACION) throw new SecurityException("ALOJAMIENTO requiere COORDINACION."); if (necesidad != Necesidad.ALOJAMIENTO && rol != RolUsuario.PERSONAL_CAMPO) throw new SecurityException("Esta necesidad requiere PERSONAL_CAMPO."); }
}



