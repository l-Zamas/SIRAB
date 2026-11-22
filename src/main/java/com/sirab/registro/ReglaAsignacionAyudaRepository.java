package com.sirab.registro;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReglaAsignacionAyudaRepository extends JpaRepository<ReglaAsignacionAyuda, Long> {

    List<ReglaAsignacionAyuda> findByArticuloIdOrderByVersionDescCriterioCodigoAsc(Long articuloId);

    List<ReglaAsignacionAyuda> findByArticuloIdAndVersionOrderByCriterioCodigoAsc(Long articuloId, int version);

    @Query("SELECT COALESCE(MAX(r.version), 0) FROM ReglaAsignacionAyuda r WHERE r.articulo.id = :articuloId")
    int findMaxVersionByArticuloId(@Param("articuloId") Long articuloId);

    @Query("""
            SELECT r FROM ReglaAsignacionAyuda r
            WHERE r.articulo.id = :articuloId
              AND r.vigenteDesde <= :fecha
              AND (r.vigenteHasta IS NULL OR r.vigenteHasta >= :fecha)
              AND r.version = (
                  SELECT MAX(r2.version) FROM ReglaAsignacionAyuda r2
                  WHERE r2.articulo.id = :articuloId
                    AND r2.vigenteDesde <= :fecha
                    AND (r2.vigenteHasta IS NULL OR r2.vigenteHasta >= :fecha)
              )
            ORDER BY r.criterioCodigo ASC
            """)
    List<ReglaAsignacionAyuda> findVigentesByArticuloIdAndFecha(
            @Param("articuloId") Long articuloId,
            @Param("fecha") LocalDate fecha);

    @Query("""
            SELECT r FROM ReglaAsignacionAyuda r
            JOIN FETCH r.articulo a
            WHERE a.activo = true
              AND r.vigenteDesde <= :fecha
              AND (r.vigenteHasta IS NULL OR r.vigenteHasta >= :fecha)
              AND r.version = (
                  SELECT MAX(r2.version) FROM ReglaAsignacionAyuda r2
                  WHERE r2.articulo.id = a.id
                    AND r2.vigenteDesde <= :fecha
                    AND (r2.vigenteHasta IS NULL OR r2.vigenteHasta >= :fecha)
              )
            ORDER BY a.codigo ASC, r.criterioCodigo ASC
            """)
    List<ReglaAsignacionAyuda> findAllVigentesArticulosActivos(@Param("fecha") LocalDate fecha);

    boolean existsByArticuloIdAndVersionAndCriterioCodigo(Long articuloId, int version, String criterioCodigo);

    @Modifying(clearAutomatically = true)
    @Query(
            value = """
                    UPDATE regla_asignacion_ayuda
                    SET vigente_hasta = :vigenteHasta
                    WHERE articulo_ayuda_id = :articuloId
                      AND version = :version
                      AND (vigente_hasta IS NULL OR vigente_hasta > :vigenteHasta)
                    """,
            nativeQuery = true)
    int acotarVigenciaHastaVersion(
            @Param("articuloId") Long articuloId,
            @Param("version") int version,
            @Param("vigenteHasta") LocalDate vigenteHasta);
}
