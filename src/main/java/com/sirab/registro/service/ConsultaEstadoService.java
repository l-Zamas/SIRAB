package com.sirab.registro.service;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sirab.registro.EstadoRegistro;
import com.sirab.registro.Familia;
import com.sirab.registro.Necesidad;

@Service
public class ConsultaEstadoService {

    private final CodigoConsultaService codigoConsultaService;

    public ConsultaEstadoService(CodigoConsultaService codigoConsultaService) {
        this.codigoConsultaService = codigoConsultaService;
    }

    @Transactional(readOnly = true)
    public Optional<EstadoPublico> consultar(String codigo) {
        return codigoConsultaService.buscar(codigo)
                .map(familia -> new EstadoPublico(familia.getEstado(), familia.getFechaCreacion(),
                        familia.getNombreRegistrante(), familia.getDireccion(), familia.getBarrio(),
                        Set.copyOf(familia.getNecesidades())));
    }

    public record EstadoPublico(EstadoRegistro estado, Instant fechaActualizacion, String nombreRegistrante,
            String direccion, String barrio, java.util.Set<Necesidad> necesidades) {
    }
}
