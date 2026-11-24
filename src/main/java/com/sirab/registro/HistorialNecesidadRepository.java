package com.sirab.registro;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialNecesidadRepository extends JpaRepository<HistorialNecesidad, Long> {
    List<HistorialNecesidad> findBySeguimiento_Familia_IdOrderByFechaHoraAsc(Long familiaId);
    List<HistorialNecesidad> findBySeguimiento_IdOrderByFechaHoraAsc(Long seguimientoId);
}
