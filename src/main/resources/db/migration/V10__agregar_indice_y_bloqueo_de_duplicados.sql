ALTER TABLE familia
    ADD COLUMN duplicado_nombre_normalizado VARCHAR(255) NULL;

ALTER TABLE familia
    ADD COLUMN duplicado_telefono_normalizado VARCHAR(15) NULL;

ALTER TABLE familia
    ADD COLUMN duplicado_ubicacion_normalizada VARCHAR(512) NULL;

CREATE INDEX idx_familia_duplicado_nombre
    ON familia (duplicado_nombre_normalizado);

CREATE INDEX idx_familia_duplicado_telefono
    ON familia (duplicado_telefono_normalizado);

CREATE INDEX idx_familia_duplicado_ubicacion
    ON familia (duplicado_ubicacion_normalizada);

CREATE TABLE bloqueo_deteccion_duplicados (
    id TINYINT NOT NULL PRIMARY KEY
);

INSERT INTO bloqueo_deteccion_duplicados (id) VALUES (1);
