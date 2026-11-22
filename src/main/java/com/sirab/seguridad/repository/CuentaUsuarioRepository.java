package com.sirab.seguridad.repository;

import com.sirab.seguridad.entity.CuentaUsuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaUsuarioRepository extends JpaRepository<CuentaUsuario, Long> {
    Optional<CuentaUsuario> findByCorreo(String correo);
}
