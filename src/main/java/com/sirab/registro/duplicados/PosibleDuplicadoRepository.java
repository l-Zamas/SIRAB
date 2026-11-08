package com.sirab.registro.duplicados;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PosibleDuplicadoRepository extends JpaRepository<PosibleDuplicado, Long> {

    Optional<PosibleDuplicado> findByFamiliaMenor_IdAndFamiliaMayor_Id(Long familiaMenorId, Long familiaMayorId);

    @EntityGraph(attributePaths = {"familiaMenor", "familiaMayor"})
    @Query("""
            select p from PosibleDuplicado p
            where p.familiaMenor.id = :familiaId or p.familiaMayor.id = :familiaId
            """)
    java.util.List<PosibleDuplicado> findAllByFamiliaId(@Param("familiaId") Long familiaId);

    @EntityGraph(attributePaths = {"familiaMenor", "familiaMayor"})
    Page<PosibleDuplicado> findAllByOrderByDetectadoAtDesc(Pageable pageable);
}
