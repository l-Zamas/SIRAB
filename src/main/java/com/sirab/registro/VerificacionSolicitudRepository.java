package com.sirab.registro;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificacionSolicitudRepository extends JpaRepository<VerificacionSolicitud, Long> {

    List<VerificacionSolicitud> findByFamilia_IdOrderByVerificadaAtDesc(Long familiaId);
}
