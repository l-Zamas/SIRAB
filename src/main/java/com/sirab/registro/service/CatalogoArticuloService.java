package com.sirab.registro.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sirab.registro.ArticuloAyuda;
import com.sirab.registro.ArticuloAyudaRepository;
import com.sirab.registro.Necesidad;

@Service
@Transactional
public class CatalogoArticuloService {

    private final ArticuloAyudaRepository articuloRepository;

    public CatalogoArticuloService(ArticuloAyudaRepository articuloRepository) {
        this.articuloRepository = articuloRepository;
    }

    public ArticuloAyuda registrarArticulo(RegistrarArticuloCommand command) {
        Objects.requireNonNull(command, "El comando de registro de artículo no puede ser nulo.");
        exigirActor(command.usuarioCorreo());

        String codigo = normalizarTexto(command.codigo(), "El código del artículo es obligatorio.");
        if (codigo.length() > 60) {
            throw new IllegalArgumentException("El código del artículo no puede superar 60 caracteres.");
        }
        if (articuloRepository.existsByCodigo(codigo)) {
            throw new IllegalArgumentException("Ya existe un artículo registrado con el código: " + codigo);
        }

        String nombre = normalizarTexto(command.nombre(), "El nombre del artículo es obligatorio.");
        if (nombre.length() > 120) {
            throw new IllegalArgumentException("El nombre del artículo no puede superar 120 caracteres.");
        }

        if (command.tipoAyuda() == null) {
            throw new IllegalArgumentException("El tipo de ayuda del artículo es obligatorio.");
        }
        if (command.tipoAyuda() == Necesidad.ALOJAMIENTO) {
            throw new IllegalArgumentException(
                    "El tipo de ayuda ALOJAMIENTO no se gestiona como artículo de catálogo. Corresponde al flujo independiente de alojamiento.");
        }

        String unidad = normalizarTexto(command.unidad(), "La unidad de medida es obligatoria.");
        if (unidad.length() > 40) {
            throw new IllegalArgumentException("La unidad de medida no puede superar 40 caracteres.");
        }

        ArticuloAyuda articulo = new ArticuloAyuda(
                codigo,
                nombre,
                command.tipoAyuda(),
                unidad,
                true,
                Instant.now(),
                command.usuarioCorreo().trim());

        return articuloRepository.save(articulo);
    }

    public ArticuloAyuda activar(Long articuloId, String usuarioCorreo) {
        exigirActor(usuarioCorreo);
        ArticuloAyuda articulo = obtenerArticulo(articuloId);
        articulo.activar();
        return articuloRepository.save(articulo);
    }

    public ArticuloAyuda desactivar(Long articuloId, String usuarioCorreo) {
        exigirActor(usuarioCorreo);
        ArticuloAyuda articulo = obtenerArticulo(articuloId);
        articulo.desactivar();
        return articuloRepository.save(articulo);
    }

    public ArticuloAyuda actualizarNombre(Long articuloId, String nuevoNombre, String usuarioCorreo) {
        exigirActor(usuarioCorreo);
        ArticuloAyuda articulo = obtenerArticulo(articuloId);
        String nombre = normalizarTexto(nuevoNombre, "El nombre del artículo es obligatorio.");
        if (nombre.length() > 120) {
            throw new IllegalArgumentException("El nombre del artículo no puede superar 120 caracteres.");
        }
        articulo.actualizarNombre(nombre);
        return articuloRepository.save(articulo);
    }

    @Transactional(readOnly = true)
    public Optional<ArticuloAyuda> obtenerPorId(Long id) {
        return articuloRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ArticuloAyuda> obtenerPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        return articuloRepository.findByCodigo(codigo.trim());
    }

    @Transactional(readOnly = true)
    public List<ArticuloAyuda> listarTodos() {
        return articuloRepository.findAllByOrderByCodigoAsc();
    }

    @Transactional(readOnly = true)
    public List<ArticuloAyuda> listarActivos() {
        return articuloRepository.findByActivoTrueOrderByCodigoAsc();
    }

    @Transactional(readOnly = true)
    public List<ArticuloAyuda> listarPorTipo(Necesidad tipoAyuda) {
        if (tipoAyuda == null) {
            return List.of();
        }
        return articuloRepository.findByTipoAyudaOrderByCodigoAsc(tipoAyuda);
    }

    @Transactional(readOnly = true)
    public List<ArticuloAyuda> listarActivosPorTipo(Necesidad tipoAyuda) {
        if (tipoAyuda == null) {
            return List.of();
        }
        return articuloRepository.findByTipoAyudaAndActivoTrueOrderByCodigoAsc(tipoAyuda);
    }

    private ArticuloAyuda obtenerArticulo(Long articuloId) {
        if (articuloId == null) {
            throw new IllegalArgumentException("El ID del artículo es obligatorio.");
        }
        return articuloRepository.findById(articuloId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el artículo con ID: " + articuloId));
    }

    private void exigirActor(String usuarioCorreo) {
        if (usuarioCorreo == null || usuarioCorreo.isBlank()) {
            throw new IllegalArgumentException("No se pudo identificar al usuario autenticado.");
        }
    }

    private String normalizarTexto(String valor, String mensajeError) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensajeError);
        }
        return valor.trim();
    }
}
