package com.sirab.registro;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SoporteMedicoRepository extends JpaRepository<SoporteMedico, Long> {

    List<SoporteMedico> findBySolicitud_IdOrderByCargadoAtDesc(Long solicitudId);

    boolean existsBySolicitud_Id(Long solicitudId);
}
