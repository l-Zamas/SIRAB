package com.sirab.registro;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfirmacionPrestacionAlojamientoRepository extends JpaRepository<ConfirmacionPrestacionAlojamiento, Long> {

    List<ConfirmacionPrestacionAlojamiento> findBySolicitud_IdOrderByConfirmadoAtDesc(Long solicitudId);

    Optional<ConfirmacionPrestacionAlojamiento> findFirstBySolicitud_IdOrderByConfirmadoAtDesc(Long solicitudId);

    boolean existsBySolicitud_Familia_Id(Long familiaId);

    boolean existsBySolicitud_Id(Long solicitudId);
}
