package com.sirab.registro;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticuloAyudaRepository extends JpaRepository<ArticuloAyuda, Long> {

    Optional<ArticuloAyuda> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<ArticuloAyuda> findByActivoTrueOrderByCodigoAsc();

    List<ArticuloAyuda> findAllByOrderByCodigoAsc();

    List<ArticuloAyuda> findByTipoAyudaOrderByCodigoAsc(Necesidad tipoAyuda);

    List<ArticuloAyuda> findByTipoAyudaAndActivoTrueOrderByCodigoAsc(Necesidad tipoAyuda);
}
