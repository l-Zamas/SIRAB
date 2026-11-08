package com.sirab.registro.duplicados;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RevisionPosibleDuplicadoRepository extends JpaRepository<RevisionPosibleDuplicado, Long> {

    List<RevisionPosibleDuplicado> findByPosibleDuplicado_IdOrderByFechaHoraAscIdAsc(Long posibleDuplicadoId);

    @Query("""
            select r from RevisionPosibleDuplicado r
            where r.posibleDuplicado.id in :posibleDuplicadoIds
            order by r.posibleDuplicado.id, r.fechaHora, r.id
            """)
    List<RevisionPosibleDuplicado> findAllByPosibleDuplicadoIds(
            @Param("posibleDuplicadoIds") List<Long> posibleDuplicadoIds);
}
