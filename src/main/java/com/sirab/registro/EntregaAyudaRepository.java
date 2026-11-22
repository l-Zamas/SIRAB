package com.sirab.registro;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EntregaAyudaRepository extends JpaRepository<EntregaAyuda, Long> {

    List<EntregaAyuda> findByFamilia_IdOrderByFechaEntregaDescIdDesc(Long familiaId);

    List<EntregaAyuda> findByFamilia_IdOrderByIdDesc(Long familiaId);

    List<EntregaAyuda> findByFamilia_IdAndFechaHoraEntregaIsNotNullOrderByFechaHoraEntregaDescIdDesc(Long familiaId);

    boolean existsByFamilia_IdAndTipoAyudaAndSatisfaceNecesidadCompletaTrue(
            Long familiaId,
            Necesidad tipoAyuda);

    List<EntregaAyuda> findByAsignacion_IdOrderByFechaHoraEntregaAscIdAsc(Long asignacionId);

    @Query("SELECT COALESCE(SUM(e.cantidadValor), 0) FROM EntregaAyuda e WHERE e.asignacion.id = :asignacionId")
    BigDecimal sumarCantidadEntregadaPorAsignacion(@Param("asignacionId") Long asignacionId);

    boolean existsByAsignacion_Id(Long asignacionId);
}
