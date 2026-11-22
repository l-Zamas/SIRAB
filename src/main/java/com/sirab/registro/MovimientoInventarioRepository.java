package com.sirab.registro;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findByLote_IdOrderByRegistradoAtDescIdDesc(Long loteId);

    List<MovimientoInventario> findByEntrega_Id(Long entregaId);
}
