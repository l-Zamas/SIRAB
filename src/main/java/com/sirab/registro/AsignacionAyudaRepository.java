package com.sirab.registro;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

@Repository
public interface AsignacionAyudaRepository extends JpaRepository<AsignacionAyuda, Long> {

    Optional<AsignacionAyuda> findByClaveIdempotencia(String claveIdempotencia);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AsignacionAyuda a WHERE a.id = :id")
    Optional<AsignacionAyuda> findByIdConBloqueo(@Param("id") Long id);

    boolean existsByClaveIdempotencia(String claveIdempotencia);

    List<AsignacionAyuda> findByEvaluacionIdOrderByIdAsc(Long evaluacionId);

    List<AsignacionAyuda> findByFamiliaIdOrderByAsignadaAtDescIdAsc(Long familiaId);

    List<AsignacionAyuda> findByFamiliaIdAndArticuloId(Long familiaId, Long articuloId);

    boolean existsByEvaluacionIdAndReglaId(Long evaluacionId, Long reglaId);

    @Query("SELECT a FROM AsignacionAyuda a WHERE a.evaluacion.id = :evaluacionId ORDER BY a.articulo.codigo ASC, a.id ASC")
    List<AsignacionAyuda> findByEvaluacionIdDetallada(@Param("evaluacionId") Long evaluacionId);
}
