package com.sirab.registro.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sirab.registro.Familia;
import com.sirab.registro.FamiliaRepository;

@Service
public class CodigoConsultaService {

    private static final int CODE_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final FamiliaRepository familiaRepository;

    public CodigoConsultaService(FamiliaRepository familiaRepository) {
        this.familiaRepository = familiaRepository;
    }

    public String generarCodigo() {
        byte[] bytes = new byte[CODE_BYTES];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String generarCodigoUnico() {
        for (int intento = 0; intento < 3; intento++) {
            String codigo = generarCodigo();
            if (familiaRepository.findByCodigoConsultaHash(hash(codigo)).isEmpty()) {
                return codigo;
            }
        }
        throw new IllegalStateException("No se pudo generar un código de consulta único.");
    }

    public String hash(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código de consulta es obligatorio.");
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(codigo.trim().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("No se pudo preparar el código de consulta.", exception);
        }
    }

    public Optional<Familia> buscar(String codigo) {
        return familiaRepository.findByCodigoConsultaHash(hash(codigo));
    }
}
