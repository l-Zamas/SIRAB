package com.sirab.registro.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sirab.registro.Familia;
import com.sirab.registro.FamiliaRepository;

@Service
public class RegistroFamiliarService {

    private static final String CONSENTIMIENTO_VERSION = "penpot-consentimiento-v1";

    private final FamiliaRepository familiaRepository;

    public RegistroFamiliarService(FamiliaRepository familiaRepository) {
        this.familiaRepository = familiaRepository;
    }

    @Transactional
    public void registrar(RegistroFamiliarCommand command) {
        if (!command.consentimientoAceptado()) {
            throw new IllegalArgumentException("Se requiere consentimiento para registrar los datos.");
        }

        Familia familia = new Familia();
        familia.setNombreRegistrante(command.nombreRegistrante());
        familia.setBarrio(command.barrio());
        familia.setDireccionReferencia(command.direccionReferencia());
        familia.setNumeroIntegrantes(command.numeroIntegrantes());
        familia.setTelefonoContacto(command.telefonoContacto());
        familia.setSituaciones(command.situaciones() == null ? java.util.Set.of() : command.situaciones());
        familia.setNecesidades(command.necesidades() == null ? java.util.Set.of() : command.necesidades());
        familia.setConsentimientoAt(command.consentimientoAt() == null ? Instant.now() : command.consentimientoAt());
        familia.setConsentimientoVersion(CONSENTIMIENTO_VERSION);
        familiaRepository.save(familia);
    }
}
