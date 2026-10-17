package com.sirab.registro;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FamiliaRepository extends JpaRepository<Familia, Long> {
    Optional<Familia> findFirstByNombreRegistrante(String nombreRegistrante);
}
