CREATE TABLE entrega_ayuda (
    id BIGINT NOT NULL AUTO_INCREMENT,
    familia_id BIGINT NOT NULL,
    tipo_ayuda VARCHAR(30) NOT NULL,
    cantidad VARCHAR(100) NOT NULL,
    fecha_entrega DATE NOT NULL,
    registrada_at TIMESTAMP NOT NULL,
    usuario_correo VARCHAR(254) NOT NULL,
    satisface_necesidad_completa BOOLEAN NOT NULL DEFAULT FALSE,
    observacion VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT fk_entrega_ayuda_familia
        FOREIGN KEY (familia_id) REFERENCES familia (id)
);
