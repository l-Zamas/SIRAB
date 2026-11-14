ALTER TABLE evaluacion_asignacion_ayuda
    ADD CONSTRAINT uk_evaluacion_id_familia UNIQUE (id, familia_id);

ALTER TABLE regla_asignacion_ayuda
    ADD CONSTRAINT uk_regla_id_articulo UNIQUE (id, articulo_ayuda_id);

ALTER TABLE solicitud_ayuda_medica
    ADD CONSTRAINT uk_solicitud_medica_id_familia UNIQUE (id, familia_id);

ALTER TABLE revision_sanitaria_externa
    ADD COLUMN familia_id BIGINT NULL;

UPDATE revision_sanitaria_externa r
SET familia_id = (
    SELECT s.familia_id
    FROM solicitud_ayuda_medica s
    WHERE s.id = r.solicitud_ayuda_medica_id
);

ALTER TABLE revision_sanitaria_externa
    MODIFY COLUMN familia_id BIGINT NOT NULL;

ALTER TABLE revision_sanitaria_externa
    ADD CONSTRAINT uk_revision_sanitaria_id_familia UNIQUE (id, familia_id);

ALTER TABLE revision_sanitaria_externa
    ADD CONSTRAINT fk_revision_sanitaria_solicitud_familia
        FOREIGN KEY (solicitud_ayuda_medica_id, familia_id)
        REFERENCES solicitud_ayuda_medica (id, familia_id);

ALTER TABLE asignacion_ayuda
    ADD COLUMN tipo_ayuda VARCHAR(30) NULL;

UPDATE asignacion_ayuda a
SET tipo_ayuda = (
    SELECT art.tipo_ayuda
    FROM articulo_ayuda art
    WHERE art.id = a.articulo_ayuda_id
);

ALTER TABLE asignacion_ayuda
    MODIFY COLUMN tipo_ayuda VARCHAR(30) NOT NULL;

ALTER TABLE asignacion_ayuda
    ADD CONSTRAINT uk_asignacion_id_familia_articulo_tipo
        UNIQUE (id, familia_id, articulo_ayuda_id, tipo_ayuda);

ALTER TABLE asignacion_ayuda
    ADD CONSTRAINT fk_asignacion_articulo_tipo
        FOREIGN KEY (articulo_ayuda_id, tipo_ayuda)
        REFERENCES articulo_ayuda (id, tipo_ayuda);

ALTER TABLE asignacion_ayuda
    ADD CONSTRAINT fk_asignacion_evaluacion_familia
        FOREIGN KEY (evaluacion_id, familia_id)
        REFERENCES evaluacion_asignacion_ayuda (id, familia_id);

ALTER TABLE asignacion_ayuda
    ADD CONSTRAINT fk_asignacion_regla_articulo
        FOREIGN KEY (regla_asignacion_id, articulo_ayuda_id)
        REFERENCES regla_asignacion_ayuda (id, articulo_ayuda_id);

ALTER TABLE asignacion_ayuda
    ADD CONSTRAINT fk_asignacion_revision_familia
        FOREIGN KEY (revision_sanitaria_id, familia_id)
        REFERENCES revision_sanitaria_externa (id, familia_id);

ALTER TABLE asignacion_ayuda
    ADD CONSTRAINT chk_asignacion_revision_medicamento
        CHECK (origen <> 'REVISION_SANITARIA' OR tipo_ayuda = 'MEDICAMENTOS');

ALTER TABLE entrega_ayuda
    ADD COLUMN articulo_ayuda_id BIGINT NULL;

UPDATE entrega_ayuda e
SET articulo_ayuda_id = (
    SELECT a.articulo_ayuda_id
    FROM asignacion_ayuda a
    WHERE a.id = e.asignacion_ayuda_id
)
WHERE e.asignacion_ayuda_id IS NOT NULL;

ALTER TABLE entrega_ayuda
    ADD CONSTRAINT uk_entrega_id_tipo UNIQUE (id, tipo_ayuda);

ALTER TABLE entrega_ayuda
    ADD CONSTRAINT uk_entrega_id_articulo_tipo
        UNIQUE (id, articulo_ayuda_id, tipo_ayuda);

ALTER TABLE entrega_ayuda
    ADD CONSTRAINT fk_entrega_asignacion_contexto
        FOREIGN KEY (
            asignacion_ayuda_id,
            familia_id,
            articulo_ayuda_id,
            tipo_ayuda
        )
        REFERENCES asignacion_ayuda (
            id,
            familia_id,
            articulo_ayuda_id,
            tipo_ayuda
        );

ALTER TABLE entrega_ayuda
    ADD CONSTRAINT chk_entrega_ayuda_formato
        CHECK (
            (
                cantidad IS NOT NULL
                AND fecha_entrega IS NOT NULL
                AND cantidad_valor IS NULL
                AND unidad IS NULL
                AND fecha_hora_entrega IS NULL
                AND asignacion_ayuda_id IS NULL
                AND articulo_ayuda_id IS NULL
            )
            OR
            (
                cantidad_valor IS NOT NULL
                AND cantidad_valor > 0
                AND unidad IS NOT NULL
                AND fecha_hora_entrega IS NOT NULL
                AND asignacion_ayuda_id IS NOT NULL
                AND articulo_ayuda_id IS NOT NULL
            )
        );

ALTER TABLE lote_inventario
    ADD CONSTRAINT uk_lote_id_articulo_tipo
        UNIQUE (id, articulo_ayuda_id, tipo_ayuda);

ALTER TABLE movimiento_inventario
    ADD COLUMN articulo_ayuda_id BIGINT NULL;

ALTER TABLE movimiento_inventario
    ADD COLUMN tipo_ayuda VARCHAR(30) NULL;

UPDATE movimiento_inventario m
SET articulo_ayuda_id = (
        SELECT l.articulo_ayuda_id
        FROM lote_inventario l
        WHERE l.id = m.lote_inventario_id
    ),
    tipo_ayuda = (
        SELECT l.tipo_ayuda
        FROM lote_inventario l
        WHERE l.id = m.lote_inventario_id
    );

ALTER TABLE movimiento_inventario
    MODIFY COLUMN articulo_ayuda_id BIGINT NOT NULL;

ALTER TABLE movimiento_inventario
    MODIFY COLUMN tipo_ayuda VARCHAR(30) NOT NULL;

ALTER TABLE movimiento_inventario
    ADD CONSTRAINT fk_movimiento_lote_articulo_tipo
        FOREIGN KEY (lote_inventario_id, articulo_ayuda_id, tipo_ayuda)
        REFERENCES lote_inventario (id, articulo_ayuda_id, tipo_ayuda);

ALTER TABLE movimiento_inventario
    ADD CONSTRAINT fk_movimiento_entrega_articulo_tipo
        FOREIGN KEY (entrega_ayuda_id, articulo_ayuda_id, tipo_ayuda)
        REFERENCES entrega_ayuda (id, articulo_ayuda_id, tipo_ayuda);

ALTER TABLE solicitud_alojamiento
    ADD CONSTRAINT uk_solicitud_alojamiento_id_familia
        UNIQUE (id, familia_id);

ALTER TABLE aprobacion_definitiva_alojamiento
    ADD CONSTRAINT uk_aprobacion_definitiva_id_solicitud
        UNIQUE (id, solicitud_alojamiento_id);

ALTER TABLE confirmacion_prestacion_alojamiento
    ADD CONSTRAINT fk_confirmacion_aprobacion_solicitud
        FOREIGN KEY (aprobacion_definitiva_id, solicitud_alojamiento_id)
        REFERENCES aprobacion_definitiva_alojamiento (id, solicitud_alojamiento_id);

CREATE VIEW vw_reconciliacion_inventario AS
SELECT
    l.id AS lote_inventario_id,
    l.saldo_disponible,
    COALESCE(SUM(
        CASE
            WHEN m.tipo IN ('APERTURA', 'INGRESO', 'AJUSTE_POSITIVO')
                THEN m.cantidad
            WHEN m.tipo IN ('SALIDA_ENTREGA', 'AJUSTE_NEGATIVO')
                THEN -m.cantidad
            ELSE 0
        END
    ), 0) AS saldo_segun_movimientos,
    l.saldo_disponible - COALESCE(SUM(
        CASE
            WHEN m.tipo IN ('APERTURA', 'INGRESO', 'AJUSTE_POSITIVO')
                THEN m.cantidad
            WHEN m.tipo IN ('SALIDA_ENTREGA', 'AJUSTE_NEGATIVO')
                THEN -m.cantidad
            ELSE 0
        END
    ), 0) AS diferencia
FROM lote_inventario l
LEFT JOIN movimiento_inventario m ON m.lote_inventario_id = l.id
GROUP BY l.id, l.saldo_disponible;
