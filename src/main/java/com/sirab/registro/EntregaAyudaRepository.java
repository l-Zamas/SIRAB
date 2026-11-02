package com.sirab.registro;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EntregaAyudaRepository extends JpaRepository<EntregaAyuda, Long> {

    List<EntregaAyuda> findByFamilia_IdOrderByFechaEntregaDescIdDesc(Long familiaId);

    boolean existsByFamilia_IdAndTipoAyudaAndSatisfaceNecesidadCompletaTrue(
            Long familiaId,
            Necesidad tipoAyuda);
}
