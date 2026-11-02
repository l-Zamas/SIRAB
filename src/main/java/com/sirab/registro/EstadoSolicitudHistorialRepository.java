package com.sirab.registro;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoSolicitudHistorialRepository extends JpaRepository<EstadoSolicitudHistorial, Long> {

    List<EstadoSolicitudHistorial> findByFamilia_IdOrderByFechaHoraAsc(Long familiaId);
}
