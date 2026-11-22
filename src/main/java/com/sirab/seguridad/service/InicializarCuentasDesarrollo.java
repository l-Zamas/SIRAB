package com.sirab.seguridad;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@Profile("dev & !prod")
public class InicializarCuentasDesarrollo implements ApplicationRunner {

    private final CuentaUsuarioRepository cuentaUsuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String contrasenaCampo;
    private final String contrasenaAdmin;

    public InicializarCuentasDesarrollo(
            CuentaUsuarioRepository cuentaUsuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${SIRAB_DEV_CAMPO_PASSWORD:}") String contrasenaCampo,
            @Value("${SIRAB_DEV_ADMIN_PASSWORD:}") String contrasenaAdmin) {
        this.cuentaUsuarioRepository = cuentaUsuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.contrasenaCampo = contrasenaCampo;
        this.contrasenaAdmin = contrasenaAdmin;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<CuentaPendiente> cuentasPendientes = new ArrayList<>();
        agregarSiNoExiste(cuentasPendientes,
                "campo@sirab.local", RolUsuario.PERSONAL_CAMPO,
                "SIRAB_DEV_CAMPO_PASSWORD", contrasenaCampo);
        agregarSiNoExiste(cuentasPendientes,
                "admin@sirab.local", RolUsuario.ADMIN,
                "SIRAB_DEV_ADMIN_PASSWORD", contrasenaAdmin);

        List<String> variablesFaltantes = cuentasPendientes.stream()
                .filter(cuenta -> !StringUtils.hasText(cuenta.contrasena()))
                .map(CuentaPendiente::variable)
                .toList();
        if (!variablesFaltantes.isEmpty()) {
            throw new IllegalStateException(
                    "No se inicializaron cuentas de desarrollo. Configure las variables externas: "
                            + String.join(", ", variablesFaltantes));
        }

        cuentasPendientes.forEach(cuenta -> cuentaUsuarioRepository.save(
                new CuentaUsuario(
                        cuenta.correo(),
                        passwordEncoder.encode(cuenta.contrasena()),
                        cuenta.rol())));
    }

    private void agregarSiNoExiste(
            List<CuentaPendiente> pendientes,
            String correo,
            RolUsuario rol,
            String variable,
            String contrasena) {
        if (cuentaUsuarioRepository.findByCorreo(correo).isEmpty()) {
            pendientes.add(new CuentaPendiente(correo, rol, variable, contrasena));
        }
    }

    private record CuentaPendiente(
            String correo,
            RolUsuario rol,
            String variable,
            String contrasena) {
    }
}
