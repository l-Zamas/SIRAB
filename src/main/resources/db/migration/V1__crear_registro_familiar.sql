CREATE TABLE familia (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL,
    ubicacion VARCHAR(255) NOT NULL,
    numero_integrantes INT NOT NULL,
    contacto VARCHAR(100),
    consentimiento_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_familia_numero_integrantes CHECK (numero_integrantes >= 1)
);

CREATE TABLE familia_necesidad (
    familia_id BIGINT NOT NULL,
    necesidad VARCHAR(30) NOT NULL,
    PRIMARY KEY (familia_id, necesidad),
    CONSTRAINT fk_familia_necesidad_familia
        FOREIGN KEY (familia_id) REFERENCES familia (id)
);
