package com.sirab.registro.duplicados;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;

@Repository
public class BloqueoDeteccionDuplicados {

    private final EntityManager entityManager;

    public BloqueoDeteccionDuplicados(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void bloquear() {
        entityManager.createNativeQuery(
                        "select id from bloqueo_deteccion_duplicados where id = 1 for update")
                .getSingleResult();
    }
}
