package com.sirab.registro;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface SeguimientoNecesidadRepository extends JpaRepository<SeguimientoNecesidad, Long> {
    List<SeguimientoNecesidad> findByFamilia_IdAndVigenteTrueOrderByNecesidadAsc(Long familiaId);
    List<SeguimientoNecesidad> findByFamilia_IdOrderByNecesidadAsc(Long familiaId);
    Optional<SeguimientoNecesidad> findByFamilia_IdAndNecesidad(Long familiaId, Necesidad necesidad);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SeguimientoNecesidad> findWithLockById(Long id);
}
