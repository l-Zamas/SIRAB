package com.sirab.registro;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HistorialNecesidadRepository extends JpaRepository<HistorialNecesidad, Long> {
    @Query("select h from HistorialNecesidad h join fetch h.seguimiento s where s.familia.id = :familiaId order by h.fechaHora desc, h.id desc")
    List<HistorialNecesidad> findBySeguimiento_Familia_IdOrderByFechaHoraDesc(@Param("familiaId") Long familiaId);
    List<HistorialNecesidad> findBySeguimiento_IdOrderByFechaHoraAsc(Long seguimientoId);
}
