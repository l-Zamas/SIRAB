package com.sirab.seguridad.service;

import com.sirab.seguridad.entity.CuentaUsuario;
import com.sirab.seguridad.entity.RolUsuario;
import com.sirab.seguridad.repository.CuentaUsuarioRepository;
import com.sirab.seguridad.service.InicializarCuentasDesarrollo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class InicializarCuentasDesarrolloTests {

    private final CuentaUsuarioRepository repository = org.mockito.Mockito.mock(CuentaUsuarioRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void createsDevelopmentAccountsWithHashedPasswordsAndDoesNotOverwriteThem() {
        Map<String, CuentaUsuario> cuentas = new HashMap<>();
        when(repository.findByCorreo(any())).thenAnswer(invocation ->
                Optional.ofNullable(cuentas.get(invocation.getArgument(0))));
        doAnswer(invocation -> {
            CuentaUsuario cuenta = invocation.getArgument(0);
            cuentas.put(cuenta.getCorreo(), cuenta);
            return cuenta;
        }).when(repository).save(any(CuentaUsuario.class));

        String campoPassword = UUID.randomUUID().toString();
        String coordinacionPassword = UUID.randomUUID().toString();
        InicializarCuentasDesarrollo initializer = new InicializarCuentasDesarrollo(
                repository, passwordEncoder, campoPassword, coordinacionPassword);
        initializer.run(null);

        CuentaUsuario campo = cuentas.get("campo@sirab.local");
        CuentaUsuario coordinacion = cuentas.get("coordinacion@sirab.local");
        assertEquals(RolUsuario.PERSONAL_CAMPO, campo.getRol());
        assertEquals(RolUsuario.COORDINACION, coordinacion.getRol());
        assertFalse(campo.getContrasenaHash().equals(campoPassword));
        assertFalse(coordinacion.getContrasenaHash().equals(coordinacionPassword));
        assertTrue(passwordEncoder.matches(campoPassword, campo.getContrasenaHash()));
        assertTrue(passwordEncoder.matches(coordinacionPassword, coordinacion.getContrasenaHash()));

        String changedPassword = UUID.randomUUID().toString();
        String changedPasswordHash = passwordEncoder.encode(changedPassword);
        cuentas.put(campo.getCorreo(), new CuentaUsuario(
                campo.getCorreo(), changedPasswordHash, campo.getRol()));
        initializer.run(null);

        assertEquals(changedPasswordHash, cuentas.get(campo.getCorreo()).getContrasenaHash());
        verify(repository, org.mockito.Mockito.times(2)).save(any(CuentaUsuario.class));
    }

    @Test
    void doesNotCreateAccountsWhenRequiredDevelopmentCredentialsAreMissing() {
        when(repository.findByCorreo(any())).thenReturn(Optional.empty());
        InicializarCuentasDesarrollo initializer = new InicializarCuentasDesarrollo(
                repository, passwordEncoder, "", "");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> initializer.run(null));

        assertTrue(exception.getMessage().contains("SIRAB_DEV_CAMPO_PASSWORD"));
        assertTrue(exception.getMessage().contains("SIRAB_DEV_COORDINACION_PASSWORD"));
        verify(repository, never()).save(any(CuentaUsuario.class));
    }

    @Test
    void doesNotRequireOrReplaceCredentialsForAccountsThatAlreadyExist() {
        String existingPassword = UUID.randomUUID().toString();
        CuentaUsuario campo = new CuentaUsuario(
                "campo@sirab.local", passwordEncoder.encode(existingPassword), RolUsuario.PERSONAL_CAMPO);
        CuentaUsuario coordinacion = new CuentaUsuario(
                "coordinacion@sirab.local", passwordEncoder.encode(existingPassword), RolUsuario.COORDINACION);
        when(repository.findByCorreo("campo@sirab.local")).thenReturn(Optional.of(campo));
        when(repository.findByCorreo("coordinacion@sirab.local")).thenReturn(Optional.of(coordinacion));

        new InicializarCuentasDesarrollo(repository, passwordEncoder, "", "").run(null);

        verify(repository, never()).save(any(CuentaUsuario.class));
        assertTrue(passwordEncoder.matches(existingPassword, campo.getContrasenaHash()));
        assertTrue(passwordEncoder.matches(existingPassword, coordinacion.getContrasenaHash()));
    }

    @Test
    void initializerIsExcludedFromProductionProfile() {
        assertEquals("dev & !prod", InicializarCuentasDesarrollo.class
                .getAnnotation(Profile.class).value()[0]);
        assertInitializerExcluded("prod");
        assertInitializerExcluded("dev", "prod");
    }

    private void assertInitializerExcluded(String... profiles) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles(profiles);
            context.register(InicializarCuentasDesarrollo.class);
            context.refresh();
            assertFalse(context.containsBean("inicializarCuentasDesarrollo"));
        }
    }
}
