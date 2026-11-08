CREATE TABLE posible_duplicado (
    id BIGINT NOT NULL AUTO_INCREMENT,
    familia_menor_id BIGINT NOT NULL,
    familia_mayor_id BIGINT NOT NULL,
    estado VARCHAR(30) NOT NULL,
    senales_coincidentes VARCHAR(100) NOT NULL,
    detectado_at TIMESTAMP NOT NULL,
    actualizado_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_posible_duplicado_familias
        UNIQUE (familia_menor_id, familia_mayor_id),
    CONSTRAINT chk_posible_duplicado_familias
        CHECK (familia_menor_id < familia_mayor_id),
    CONSTRAINT fk_posible_duplicado_familia_menor
        FOREIGN KEY (familia_menor_id) REFERENCES familia (id),
    CONSTRAINT fk_posible_duplicado_familia_mayor
        FOREIGN KEY (familia_mayor_id) REFERENCES familia (id)
);

CREATE INDEX idx_posible_duplicado_estado_detectado
    ON posible_duplicado (estado, detectado_at);

CREATE TABLE historial_posible_duplicado (
    id BIGINT NOT NULL AUTO_INCREMENT,
    posible_duplicado_id BIGINT NOT NULL,
    evento VARCHAR(40) NOT NULL,
    actor_correo VARCHAR(254),
    fecha_hora TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_historial_posible_duplicado
        FOREIGN KEY (posible_duplicado_id) REFERENCES posible_duplicado (id)
);

CREATE INDEX idx_historial_posible_duplicado_fecha
    ON historial_posible_duplicado (posible_duplicado_id, fecha_hora);
