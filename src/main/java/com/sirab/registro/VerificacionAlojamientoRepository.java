package com.sirab.registro;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificacionAlojamientoRepository extends JpaRepository<VerificacionAlojamiento, Long> {

    List<VerificacionAlojamiento> findBySolicitud_IdOrderByVerificadaAtDesc(Long solicitudId);

    Optional<VerificacionAlojamiento> findFirstBySolicitud_IdOrderByVerificadaAtDesc(Long solicitudId);
}
