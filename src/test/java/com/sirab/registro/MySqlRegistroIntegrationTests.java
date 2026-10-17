package com.sirab.registro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.sirab.registro.service.RegistroFamiliarCommand;
import com.sirab.registro.service.RegistroFamiliarService;
import com.sirab.registro.SituacionFamiliar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfSystemProperty(named = "sirab.mysql.integration", matches = "true")
class MySqlRegistroIntegrationTests {

    @Autowired
    private RegistroFamiliarService registroFamiliarService;

    @Autowired
    private FamiliaRepository familiaRepository;

    @Test
    @Transactional
    void registraYLeeUnRegistroSinteticoEnLaBaseConfigurada() {
        String syntheticRegistrant = "INTEGRATION_TEST_" + UUID.randomUUID();
        RegistroFamiliarCommand command = new RegistroFamiliarCommand(
                syntheticRegistrant,
                "TEST_BARRIO",
                "TEST_REFERENCE",
                1,
                "SYNTHETIC_PHONE",
                Set.of(SituacionFamiliar.MENORES_DE_CINCO),
                Set.of(Necesidad.AGUA),
                true,
                Instant.now());

        registroFamiliarService.registrar(command);

        Familia saved = familiaRepository.findFirstByNombreRegistrante(syntheticRegistrant)
                .orElseThrow();
        assertEquals(1, saved.getNumeroIntegrantes());
        assertEquals("TEST_BARRIO", saved.getBarrio());
        assertEquals("TEST_REFERENCE", saved.getDireccionReferencia());
        assertEquals("SYNTHETIC_PHONE", saved.getTelefonoContacto());
        assertEquals(Set.of(SituacionFamiliar.MENORES_DE_CINCO), saved.getSituaciones());
        assertEquals(Set.of(Necesidad.AGUA), saved.getNecesidades());
        assertTrue(saved.getConsentimientoAt() != null);
        assertTrue(saved.getConsentimientoVersion() != null);
    }
}
