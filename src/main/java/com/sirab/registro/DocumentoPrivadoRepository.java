package com.sirab.registro;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentoPrivadoRepository extends JpaRepository<DocumentoPrivado, Long> {

    Optional<DocumentoPrivado> findByClaveAlmacenamiento(String claveAlmacenamiento);

    Optional<DocumentoPrivado> findBySha256(String sha256);
}
