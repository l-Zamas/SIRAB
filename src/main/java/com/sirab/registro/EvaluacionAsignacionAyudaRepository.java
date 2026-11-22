package com.sirab.registro;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluacionAsignacionAyudaRepository extends JpaRepository<EvaluacionAsignacionAyuda, Long> {

    Optional<EvaluacionAsignacionAyuda> findByClaveIdempotencia(String claveIdempotencia);

    boolean existsByClaveIdempotencia(String claveIdempotencia);

    Optional<EvaluacionAsignacionAyuda> findByFamiliaIdAndRevision(Long familiaId, int revision);

    List<EvaluacionAsignacionAyuda> findByFamiliaIdOrderByRevisionDesc(Long familiaId);

    Optional<EvaluacionAsignacionAyuda> findFirstByFamiliaIdOrderByRevisionDesc(Long familiaId);

    @Query("SELECT COALESCE(MAX(e.revision), 0) FROM EvaluacionAsignacionAyuda e WHERE e.familia.id = :familiaId")
    int findMaxRevisionByFamiliaId(@Param("familiaId") Long familiaId);
}
