ALTER TABLE familia RENAME COLUMN nombre TO nombre_registrante;
ALTER TABLE familia RENAME COLUMN ubicacion TO direccion_referencia;
ALTER TABLE familia RENAME COLUMN contacto TO telefono_contacto;

ALTER TABLE familia ADD COLUMN barrio VARCHAR(120) NULL;
ALTER TABLE familia ADD COLUMN consentimiento_version VARCHAR(40) NULL;

CREATE TABLE familia_situacion (
    familia_id BIGINT NOT NULL,
    situacion VARCHAR(40) NOT NULL,
    PRIMARY KEY (familia_id, situacion),
    CONSTRAINT fk_familia_situacion_familia
        FOREIGN KEY (familia_id) REFERENCES familia (id)
);
