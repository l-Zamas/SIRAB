package com.sirab.registro;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FamiliaRepository extends JpaRepository<Familia, Long> {
    Optional<Familia> findFirstByNombreRegistrante(String nombreRegistrante);

    List<Familia> findByDuplicadoNombreNormalizadoIsNullAndIdGreaterThanOrderByIdAsc(
            Long id, Pageable pageable);

    List<Familia> findAllByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);

    @Query("""
            select f from Familia f
            where f.id <> :familiaId
              and ((:nombre is not null and :telefono is not null
                        and f.duplicadoNombreNormalizado = :nombre
                        and f.duplicadoTelefonoNormalizado = :telefono)
                or (:nombre is not null and :ubicacion is not null
                        and f.duplicadoNombreNormalizado = :nombre
                        and f.duplicadoUbicacionNormalizada = :ubicacion)
                or (:telefono is not null and :ubicacion is not null
                        and f.duplicadoTelefonoNormalizado = :telefono
                        and f.duplicadoUbicacionNormalizada = :ubicacion))
            """)
    List<Familia> buscarCandidatasDuplicado(
            @Param("familiaId") Long familiaId,
            @Param("nombre") String nombre,
            @Param("telefono") String telefono,
            @Param("ubicacion") String ubicacion);
}
