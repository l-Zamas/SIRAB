package com.sirab.registro.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sirab.registro.ArticuloAyuda;
import com.sirab.registro.ArticuloAyudaRepository;
import com.sirab.registro.ReglaAsignacionAyuda;
import com.sirab.registro.ReglaAsignacionAyudaRepository;
import com.sirab.registro.TipoCriterioAsignacion;

@Service
@Transactional
public class ReglaAsignacionService {

    private final ReglaAsignacionAyudaRepository reglaRepository;
    private final ArticuloAyudaRepository articuloRepository;

    public ReglaAsignacionService(
            ReglaAsignacionAyudaRepository reglaRepository,
            ArticuloAyudaRepository articuloRepository) {
        this.reglaRepository = reglaRepository;
        this.articuloRepository = articuloRepository;
    }

    public List<ReglaAsignacionAyuda> publicarVersion(PublicarVersionReglasCommand command) {
        Objects.requireNonNull(command, "El comando de publicación de reglas no puede ser nulo.");
        exigirActor(command.usuarioCorreo());

        if (command.articuloId() == null) {
            throw new IllegalArgumentException("El ID del artículo es obligatorio.");
        }
        ArticuloAyuda articulo = articuloRepository.findById(command.articuloId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el artículo con ID: " + command.articuloId()));

        if (!articulo.isActivo()) {
            throw new IllegalStateException(
                    "No se pueden registrar reglas para el artículo inactivo: " + articulo.getCodigo());
        }

        if (command.vigenteDesde() == null) {
            throw new IllegalArgumentException("La fecha de inicio de vigencia es obligatoria.");
        }
        if (command.vigenteHasta() != null && command.vigenteHasta().isBefore(command.vigenteDesde())) {
            throw new IllegalArgumentException("La fecha de fin de vigencia no puede ser anterior a la fecha de inicio.");
        }

        if (command.criterios() == null || command.criterios().isEmpty()) {
            throw new IllegalArgumentException("Debe especificarse al menos un criterio para la versión de reglas.");
        }

        validarCriterios(command.criterios());

        int maxVersion = reglaRepository.findMaxVersionByArticuloId(articulo.getId());
        int nuevaVersion = maxVersion + 1;

        if (maxVersion > 0) {
            List<ReglaAsignacionAyuda> reglasAnteriores =
                    reglaRepository.findByArticuloIdAndVersionOrderByCriterioCodigoAsc(articulo.getId(), maxVersion);
            if (!reglasAnteriores.isEmpty()) {
                LocalDate previaVigenteDesde = reglasAnteriores.get(0).getVigenteDesde();
                if (!command.vigenteDesde().isAfter(previaVigenteDesde)) {
                    throw new IllegalArgumentException(
                            "La nueva versión debe iniciar después de la fecha de inicio de la versión anterior ("
                                    + previaVigenteDesde + ").");
                }
                LocalDate nuevoFinPrevia = command.vigenteDesde().minusDays(1);
                reglaRepository.acotarVigenciaHastaVersion(articulo.getId(), maxVersion, nuevoFinPrevia);
            }
        }

        Instant ahora = Instant.now();
        String usuario = command.usuarioCorreo().trim();
        List<ReglaAsignacionAyuda> nuevasReglas = new ArrayList<>();

        for (CriterioReglaCommand criterio : command.criterios()) {
            ReglaAsignacionAyuda regla = new ReglaAsignacionAyuda(
                    articulo,
                    nuevaVersion,
                    criterio.tipoCriterio(),
                    criterio.situacion(),
                    criterio.cantidad(),
                    articulo.getUnidad(),
                    command.vigenteDesde(),
                    command.vigenteHasta(),
                    ahora,
                    usuario);
            nuevasReglas.add(regla);
        }

        return reglaRepository.saveAll(nuevasReglas);
    }

    @Transactional(readOnly = true)
    public List<ReglaAsignacionAyuda> consultarReglasVigentes(Long articuloId, LocalDate fecha) {
        if (articuloId == null) {
            throw new IllegalArgumentException("El ID del artículo es obligatorio.");
        }
        LocalDate fechaConsulta = fecha != null ? fecha : LocalDate.now();
        return reglaRepository.findVigentesByArticuloIdAndFecha(articuloId, fechaConsulta);
    }

    @Transactional(readOnly = true)
    public List<ReglaAsignacionAyuda> consultarTodasLasReglasVigentes(LocalDate fecha) {
        LocalDate fechaConsulta = fecha != null ? fecha : LocalDate.now();
        return reglaRepository.findAllVigentesArticulosActivos(fechaConsulta);
    }

    @Transactional(readOnly = true)
    public List<ReglaAsignacionAyuda> consultarVersion(Long articuloId, int version) {
        if (articuloId == null) {
            throw new IllegalArgumentException("El ID del artículo es obligatorio.");
        }
        if (version <= 0) {
            throw new IllegalArgumentException("La versión debe ser mayor a cero.");
        }
        return reglaRepository.findByArticuloIdAndVersionOrderByCriterioCodigoAsc(articuloId, version);
    }

    @Transactional(readOnly = true)
    public List<ReglaAsignacionAyuda> consultarHistorial(Long articuloId) {
        if (articuloId == null) {
            throw new IllegalArgumentException("El ID del artículo es obligatorio.");
        }
        return reglaRepository.findByArticuloIdOrderByVersionDescCriterioCodigoAsc(articuloId);
    }

    @Transactional(readOnly = true)
    public int obtenerUltimaVersion(Long articuloId) {
        if (articuloId == null) {
            throw new IllegalArgumentException("El ID del artículo es obligatorio.");
        }
        return reglaRepository.findMaxVersionByArticuloId(articuloId);
    }

    private void validarCriterios(List<CriterioReglaCommand> criterios) {
        boolean tieneBase = false;
        Set<String> codigosVistos = new HashSet<>();

        for (CriterioReglaCommand c : criterios) {
            if (c == null) {
                throw new IllegalArgumentException("El criterio no puede ser nulo.");
            }
            if (c.tipoCriterio() == null) {
                throw new IllegalArgumentException("El tipo de criterio es obligatorio.");
            }
            if (c.cantidad() == null || c.cantidad().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
            }

            String codigoCriterio;
            if (c.tipoCriterio() == TipoCriterioAsignacion.BASE) {
                if (c.situacion() != null) {
                    throw new IllegalArgumentException("El criterio BASE no debe incluir una situación familiar.");
                }
                codigoCriterio = "BASE";
                tieneBase = true;
            } else if (c.tipoCriterio() == TipoCriterioAsignacion.SITUACION_FAMILIAR) {
                if (c.situacion() == null) {
                    throw new IllegalArgumentException(
                            "El criterio de tipo SITUACION_FAMILIAR debe especificar la situación familiar.");
                }
                codigoCriterio = c.situacion().name();
            } else {
                throw new IllegalArgumentException("Tipo de criterio no soportado: " + c.tipoCriterio());
            }

            if (!codigosVistos.add(codigoCriterio)) {
                throw new IllegalArgumentException("Criterio duplicado en la misma versión: " + codigoCriterio);
            }
        }

        if (!tieneBase) {
            throw new IllegalArgumentException("La versión de reglas debe incluir obligatoriamente el criterio BASE.");
        }
    }

    private void exigirActor(String usuarioCorreo) {
        if (usuarioCorreo == null || usuarioCorreo.isBlank()) {
            throw new IllegalArgumentException("No se pudo identificar al usuario autenticado.");
        }
    }
}
