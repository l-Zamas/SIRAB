package com.sirab.registro;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvidenciaFamiliaRepository extends JpaRepository<EvidenciaFamilia, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "documento")
    List<EvidenciaFamilia> findByFamilia_Id(Long familiaId);

    @Query("select e from EvidenciaFamilia e join fetch e.documento where e.id = :id and e.familia.id = :familiaId")
    Optional<EvidenciaFamilia> findByIdAndFamilia_Id(@Param("id") Long id, @Param("familiaId") Long familiaId);
}
