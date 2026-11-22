package com.sirab.registro.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sirab.registro.DocumentoPrivado;
import com.sirab.registro.DocumentoPrivadoRepository;
import com.sirab.registro.EvidenciaFamilia;
import com.sirab.registro.EvidenciaFamiliaRepository;
import com.sirab.registro.Familia;
import com.sirab.registro.TipoDocumentoPrivado;

@Service
public class EvidenciaFamiliaService {
    public record EvidenciaVista(Long id, TipoDocumentoPrivado tipo, String nombre, String tipoContenido) { }
    private static final long MAX_BYTES = 5 * 1024 * 1024;
    private final Path root;
    private final DocumentoPrivadoRepository documentoRepository;
    private final EvidenciaFamiliaRepository evidenciaRepository;

    public EvidenciaFamiliaService(
            @Value("${sirab.documentos.path:data/documentos}") String path,
            DocumentoPrivadoRepository documentoRepository,
            EvidenciaFamiliaRepository evidenciaRepository) {
        this.root = Path.of(path).toAbsolutePath().normalize();
        this.documentoRepository = documentoRepository;
        this.evidenciaRepository = evidenciaRepository;
    }

    public String guardarTemporal(MultipartFile file, TipoDocumentoPrivado tipo) {
        validar(file, tipo);
        try {
            Files.createDirectories(root.resolve("pendientes"));
            String extension = extension(file.getOriginalFilename());
            String key = "pendientes/" + UUID.randomUUID() + extension;
            Files.copy(file.getInputStream(), root.resolve(key), StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (IOException exception) {
            throw new IllegalArgumentException("No fue posible guardar la evidencia.", exception);
        }
    }

    @Transactional
    public void asociar(Familia familia, String key, String originalName, String contentType, TipoDocumentoPrivado tipo) {
        if (key == null || !key.startsWith("pendientes/")) return;
        Path source = root.resolve(key).normalize();
        if (!source.startsWith(root) || !Files.isRegularFile(source)) throw new IllegalArgumentException("La evidencia no está disponible.");
        Path target = null;
        try {
            String finalKey = tipo.name().toLowerCase(Locale.ROOT) + "/" + UUID.randomUUID() + extension(originalName);
            target = root.resolve(finalKey).normalize();
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            byte[] bytes = Files.readAllBytes(target);
            DocumentoPrivado documento = documentoRepository.save(new DocumentoPrivado(
                    finalKey, sanitizeName(originalName), contentType, bytes.length, sha256(bytes), tipo, Instant.now(), "ciudadania"));
            evidenciaRepository.saveAndFlush(new EvidenciaFamilia(familia, documento, tipo));
            registrarLimpiezaEnRollback(target);
        } catch (Exception exception) {
            eliminarSiCreado(target);
            throw new IllegalArgumentException("No fue posible asociar la evidencia.", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<EvidenciaVista> listar(Long familiaId) {
        return evidenciaRepository.findByFamilia_Id(familiaId).stream()
                .map(e -> new EvidenciaVista(e.getId(), e.getTipo(), e.getDocumento().getNombreOriginal(),
                        e.getDocumento().getTipoContenido()))
                .toList();
    }

    public DocumentoPrivado obtenerDocumento(Long familiaId, Long evidenciaId) {
        return evidenciaRepository.findByIdAndFamilia_Id(evidenciaId, familiaId)
                .map(EvidenciaFamilia::getDocumento)
                .orElseThrow(() -> new IllegalArgumentException("La evidencia no existe para esta familia."));
    }

    public byte[] leer(Long familiaId, Long evidenciaId) {
        EvidenciaFamilia evidencia = evidenciaRepository.findByIdAndFamilia_Id(evidenciaId, familiaId)
                .orElseThrow(() -> new IllegalArgumentException("La evidencia no existe para esta familia."));
        try {
            Path path = root.resolve(evidencia.getDocumento().getClaveAlmacenamiento()).normalize();
            if (!path.startsWith(root) || !Files.isRegularFile(path)) {
                throw new IllegalArgumentException("El archivo de evidencia no está disponible.");
            }
            return Files.readAllBytes(path);
        } catch (IOException exception) {
            throw new IllegalArgumentException("No fue posible leer la evidencia.", exception);
        }
    }

    private void registrarLimpiezaEnRollback(Path target) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eliminarSiCreado(target);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    eliminarSiCreado(target);
                }
            }
        });
    }

    private void eliminarSiCreado(Path target) {
        if (target != null) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // La operación original ya falló; no ocultar su causa.
            }
        }
    }

    private void validar(MultipartFile file, TipoDocumentoPrivado tipo) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) throw new IllegalArgumentException("Adjunta un archivo válido de máximo 5 MB.");
        String mime = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        boolean imagen = mime.equals("image/jpeg") || mime.equals("image/png") || mime.equals("image/webp");
        boolean permitido = tipo == TipoDocumentoPrivado.EVIDENCIA_ALOJAMIENTO ? imagen : imagen || mime.equals("application/pdf");
        if (!permitido) throw new IllegalArgumentException("El tipo de archivo no está permitido.");
    }

    private String extension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot).replaceAll("[^A-Za-z0-9.]", "").toLowerCase(Locale.ROOT);
    }
    private String sanitizeName(String name) { return name == null ? "archivo" : Path.of(name).getFileName().toString(); }
    private String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
}
