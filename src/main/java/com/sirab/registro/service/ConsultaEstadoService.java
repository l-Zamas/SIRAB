package com.sirab.registro.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.Familia;

@Service
public class ConsultaEstadoService {

    private final CodigoConsultaService codigoConsultaService;

    public ConsultaEstadoService(CodigoConsultaService codigoConsultaService) {
        this.codigoConsultaService = codigoConsultaService;
    }

    @Transactional(readOnly = true)
    public Optional<EstadoPublico> consultar(String codigo) {
        return codigoConsultaService.buscar(codigo)
                .map(familia -> new EstadoPublico(familia.getEstado(), familia.getFechaCreacion()));
    }

    public record EstadoPublico(EstadoRegistro estado, Instant fechaActualizacion) {
    }
}
