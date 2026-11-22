package com.sirab.registro;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitudAlojamientoRepository extends JpaRepository<SolicitudAlojamiento, Long> {

    List<SolicitudAlojamiento> findByFamilia_IdOrderByRegistradaAtDesc(Long familiaId);

    Optional<SolicitudAlojamiento> findFirstByFamilia_IdOrderByRegistradaAtDesc(Long familiaId);

    boolean existsByFamilia_Id(Long familiaId);
}
