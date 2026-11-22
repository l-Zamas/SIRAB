package com.sirab.registro;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

@Repository
public interface LoteInventarioRepository extends JpaRepository<LoteInventario, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM LoteInventario l WHERE l.id = :id")
    Optional<LoteInventario> findByIdConBloqueo(@Param("id") Long id);

    List<LoteInventario> findByArticulo_IdOrderByRecibidoAtDesc(Long articuloId);

    List<LoteInventario> findByArticulo_IdAndSaldoDisponibleGreaterThanOrderByFechaVencimientoAscRecibidoAtAsc(
            Long articuloId, BigDecimal saldoMinimo);

    Optional<LoteInventario> findByArticulo_IdAndCodigoLote(Long articuloId, String codigoLote);

    @Query("SELECT COALESCE(SUM(l.saldoDisponible), 0) FROM LoteInventario l WHERE l.articulo.id = :articuloId")
    BigDecimal sumarSaldoDisponiblePorArticulo(@Param("articuloId") Long articuloId);

    List<LoteInventario> findByTipoAyudaOrderByRecibidoAtDesc(Necesidad tipoAyuda);
}
