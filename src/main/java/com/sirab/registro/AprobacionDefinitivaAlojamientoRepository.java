package com.sirab.registro;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AprobacionDefinitivaAlojamientoRepository extends JpaRepository<AprobacionDefinitivaAlojamiento, Long> {

    List<AprobacionDefinitivaAlojamiento> findBySolicitud_IdOrderByDecididaAtDesc(Long solicitudId);

    Optional<AprobacionDefinitivaAlojamiento> findFirstBySolicitud_IdOrderByDecididaAtDesc(Long solicitudId);
}
