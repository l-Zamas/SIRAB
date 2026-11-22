package com.sirab.registro;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitudAyudaMedicaRepository extends JpaRepository<SolicitudAyudaMedica, Long> {

    List<SolicitudAyudaMedica> findByFamilia_IdOrderByRegistradaAtDesc(Long familiaId);

    Optional<SolicitudAyudaMedica> findFirstByFamilia_IdOrderByRegistradaAtDesc(Long familiaId);

    boolean existsByFamilia_IdAndEstadoRevision(Long familiaId, EstadoRevisionSanitaria estadoRevision);
}
