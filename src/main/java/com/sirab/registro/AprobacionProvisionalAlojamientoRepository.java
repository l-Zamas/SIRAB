package com.sirab.registro;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AprobacionProvisionalAlojamientoRepository extends JpaRepository<AprobacionProvisionalAlojamiento, Long> {

    List<AprobacionProvisionalAlojamiento> findBySolicitud_IdOrderByDecididaAtDesc(Long solicitudId);

    Optional<AprobacionProvisionalAlojamiento> findFirstBySolicitud_IdOrderByDecididaAtDesc(Long solicitudId);
}
