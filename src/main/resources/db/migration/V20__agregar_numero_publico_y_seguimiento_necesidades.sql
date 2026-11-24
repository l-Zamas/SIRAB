-- ============================================================
-- V20: Número público de solicitud + seguimiento de necesidades
-- ============================================================

-- 1. Número público de solicitud (consecutivo, inmutable, concurrente-safe)
--    Usamos una tabla de secuencia con bloqueo FOR UPDATE para MySQL.
CREATE TABLE secuencia_numero_solicitud (
    nombre VARCHAR(50) NOT NULL,
    valor  BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (nombre)
);

INSERT INTO secuencia_numero_solicitud (nombre, valor) VALUES ('solicitud', 0);

ALTER TABLE familia
    ADD COLUMN numero_solicitud_publica VARCHAR(32) NULL;

-- Asignar números a registros existentes preservando su identificador histórico.
UPDATE secuencia_numero_solicitud
SET valor = (SELECT COALESCE(MAX(id), 0) FROM familia)
WHERE nombre = 'solicitud';

UPDATE familia
SET numero_solicitud_publica = CONCAT('SIRAB-', LPAD(id, 6, '0'))
WHERE numero_solicitud_publica IS NULL;

ALTER TABLE familia
    MODIFY COLUMN numero_solicitud_publica VARCHAR(32) NULL;

CREATE UNIQUE INDEX uk_familia_numero_solicitud_publica
    ON familia (numero_solicitud_publica);

-- 2. Estado EN_ATENCION para solicitudes con actuaciones parciales
--    (la restricción CHECK existente en V3 solo lista PENDIENTE, VERIFICADO, ATENDIDO;
--     en MySQL 8 los CHECK se validan, así que hay que ampliarla)
-- H2 no acepta DROP CHECK con la sintaxis de MySQL; la columna ya no tiene
-- una restricción CHECK en las migraciones históricas, por lo que basta con
-- ampliar el modelo JPA para aceptar EN_ATENCION.

-- 3. Seguimiento individual de cada necesidad por familia
CREATE TABLE seguimiento_necesidad (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    familia_id          BIGINT       NOT NULL,
    necesidad           VARCHAR(30)  NOT NULL,
    estado              VARCHAR(30)  NOT NULL DEFAULT 'DECLARADA',
    cantidad_solicitada DECIMAL(18,6) NULL,
    cantidad_entregada  DECIMAL(18,6) NOT NULL DEFAULT 0,
    rol_responsable     VARCHAR(30)  NULL,
    atendido_por        VARCHAR(254) NULL,
    observacion         VARCHAR(1000) NULL,
    evidencia_ref       VARCHAR(500) NULL,
    creado_at           TIMESTAMP(6) NOT NULL,
    actualizado_at      TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_seguimiento_familia_nec UNIQUE (familia_id, necesidad),
    CONSTRAINT fk_seguimiento_familia FOREIGN KEY (familia_id) REFERENCES familia (id) ON DELETE CASCADE,
    CONSTRAINT chk_seguimiento_estado CHECK (estado IN (
        'DECLARADA', 'VERIFICADA', 'EN_ATENCION', 'PARCIALMENTE_ATENDIDA', 'SATISFECHA'
    ))
);

-- 4. Historial de actuaciones sobre cada necesidad (append-only)
CREATE TABLE historial_necesidad (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    seguimiento_id    BIGINT       NOT NULL,
    estado_anterior   VARCHAR(30)  NULL,
    estado_nuevo      VARCHAR(30)  NOT NULL,
    usuario_correo    VARCHAR(254) NOT NULL,
    rol_usuario       VARCHAR(30)  NOT NULL,
    cantidad_entregada DECIMAL(18,6) NULL,
    observacion       VARCHAR(1000) NULL,
    fecha_hora        TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_historial_necesidad_seg FOREIGN KEY (seguimiento_id) REFERENCES seguimiento_necesidad (id) ON DELETE CASCADE
);

-- 5. Inicializar seguimiento para familias existentes que ya tengan necesidades
INSERT INTO seguimiento_necesidad (familia_id, necesidad, estado, creado_at, actualizado_at)
SELECT fn.familia_id, fn.necesidad,
       CASE
           WHEN f.estado = 'ATENDIDO' THEN 'SATISFECHA'
           WHEN f.estado = 'VERIFICADO' THEN 'VERIFICADA'
           ELSE 'DECLARADA'
       END,
       NOW(6), NOW(6)
FROM familia_necesidad fn
JOIN familia f ON f.id = fn.familia_id;
