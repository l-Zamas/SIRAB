package com.sirab.seguridad.service;

import com.sirab.seguridad.entity.CuentaUsuario;
import com.sirab.seguridad.repository.CuentaUsuarioRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CuentaUsuarioDetailsService implements UserDetailsService {

    private final CuentaUsuarioRepository cuentaUsuarioRepository;

    public CuentaUsuarioDetailsService(CuentaUsuarioRepository cuentaUsuarioRepository) {
        this.cuentaUsuarioRepository = cuentaUsuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        CuentaUsuario cuenta = cuentaUsuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales no válidas."));
        return User.withUsername(cuenta.getCorreo())
                .password(cuenta.getContrasenaHash())
                .roles(cuenta.getRol().name())
                .build();
    }
}
