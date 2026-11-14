CREATE TABLE evaluacion_asignacion_ayuda (
    id BIGINT NOT NULL AUTO_INCREMENT,
    familia_id BIGINT NOT NULL,
    revision INT NOT NULL,
    clave_idempotencia VARCHAR(100) NOT NULL,
    evaluada_at TIMESTAMP(6) NOT NULL,
    usuario_correo VARCHAR(254) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_evaluacion_asignacion_idempotencia UNIQUE (clave_idempotencia),
    CONSTRAINT uk_evaluacion_asignacion_revision UNIQUE (familia_id, revision),
    CONSTRAINT fk_evaluacion_asignacion_familia
        FOREIGN KEY (familia_id) REFERENCES familia (id),
    CONSTRAINT chk_evaluacion_asignacion_revision CHECK (revision > 0)
);

CREATE TABLE evaluacion_asignacion_necesidad (
    evaluacion_id BIGINT NOT NULL,
    necesidad VARCHAR(30) NOT NULL,
    PRIMARY KEY (evaluacion_id, necesidad),
    CONSTRAINT fk_evaluacion_asignacion_necesidad
        FOREIGN KEY (evaluacion_id) REFERENCES evaluacion_asignacion_ayuda (id)
);

CREATE TABLE evaluacion_asignacion_situacion (
    evaluacion_id BIGINT NOT NULL,
    situacion VARCHAR(40) NOT NULL,
    PRIMARY KEY (evaluacion_id, situacion),
    CONSTRAINT fk_evaluacion_asignacion_situacion
        FOREIGN KEY (evaluacion_id) REFERENCES evaluacion_asignacion_ayuda (id)
);

CREATE TABLE asignacion_ayuda (
    id BIGINT NOT NULL AUTO_INCREMENT,
    familia_id BIGINT NOT NULL,
    articulo_ayuda_id BIGINT NOT NULL,
    evaluacion_id BIGINT,
    regla_asignacion_id BIGINT,
    revision_sanitaria_id BIGINT,
    origen VARCHAR(30) NOT NULL,
    clave_idempotencia VARCHAR(120) NOT NULL,
    cantidad DECIMAL(18, 6) NOT NULL,
    unidad VARCHAR(40) NOT NULL,
    asignada_at TIMESTAMP(6) NOT NULL,
    usuario_correo VARCHAR(254) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_asignacion_ayuda_idempotencia UNIQUE (clave_idempotencia),
    CONSTRAINT uk_asignacion_evaluacion_regla
        UNIQUE (evaluacion_id, regla_asignacion_id),
    CONSTRAINT fk_asignacion_ayuda_familia
        FOREIGN KEY (familia_id) REFERENCES familia (id),
    CONSTRAINT fk_asignacion_ayuda_articulo
        FOREIGN KEY (articulo_ayuda_id) REFERENCES articulo_ayuda (id),
    CONSTRAINT fk_asignacion_ayuda_evaluacion
        FOREIGN KEY (evaluacion_id) REFERENCES evaluacion_asignacion_ayuda (id),
    CONSTRAINT fk_asignacion_ayuda_regla
        FOREIGN KEY (regla_asignacion_id) REFERENCES regla_asignacion_ayuda (id),
    CONSTRAINT chk_asignacion_ayuda_origen
        CHECK (origen IN ('REGLA', 'REVISION_SANITARIA')),
    CONSTRAINT chk_asignacion_ayuda_cantidad CHECK (cantidad > 0)
);

CREATE INDEX idx_asignacion_ayuda_familia
    ON asignacion_ayuda (familia_id, asignada_at);

ALTER TABLE entrega_ayuda MODIFY COLUMN cantidad VARCHAR(100) NULL;
ALTER TABLE entrega_ayuda MODIFY COLUMN fecha_entrega DATE NULL;

ALTER TABLE entrega_ayuda
    ADD COLUMN asignacion_ayuda_id BIGINT NULL;

ALTER TABLE entrega_ayuda
    ADD COLUMN cantidad_valor DECIMAL(18, 6) NULL;

ALTER TABLE entrega_ayuda
    ADD COLUMN unidad VARCHAR(40) NULL;

ALTER TABLE entrega_ayuda
    ADD COLUMN fecha_hora_entrega TIMESTAMP(6) NULL;

ALTER TABLE entrega_ayuda
    ADD CONSTRAINT fk_entrega_ayuda_asignacion
        FOREIGN KEY (asignacion_ayuda_id) REFERENCES asignacion_ayuda (id);

ALTER TABLE entrega_ayuda
    ADD CONSTRAINT chk_entrega_ayuda_cantidad_valor
        CHECK (cantidad_valor IS NULL OR cantidad_valor > 0);

CREATE INDEX idx_entrega_ayuda_asignacion
    ON entrega_ayuda (asignacion_ayuda_id, fecha_hora_entrega);
