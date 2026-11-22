package com.sirab.registro;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RevisionSanitariaExternaRepository extends JpaRepository<RevisionSanitariaExterna, Long> {

    List<RevisionSanitariaExterna> findBySolicitud_IdOrderByDecididaAtDesc(Long solicitudId);

    Optional<RevisionSanitariaExterna> findFirstBySolicitud_IdOrderByDecididaAtDesc(Long solicitudId);

    List<RevisionSanitariaExterna> findByFamiliaIdOrderByDecididaAtDesc(Long familiaId);
}
