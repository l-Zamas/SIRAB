package com.sirab.registro.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistrarLoteCommand(
        Long articuloId,
        String codigoLote,
        LocalDate fechaVencimiento,
        BigDecimal saldoInicial,
        String origen,
        String observacion) {

    public RegistrarLoteCommand {
        if (articuloId == null) {
            throw new IllegalArgumentException("El artículo de ayuda es obligatorio.");
        }
        if (saldoInicial == null || saldoInicial.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El saldo inicial debe ser mayor a cero.");
        }
        if (origen == null || origen.isBlank()) {
            throw new IllegalArgumentException("El origen del lote es obligatorio.");
        }
    }
}
