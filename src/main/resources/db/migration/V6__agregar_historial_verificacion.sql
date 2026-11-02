CREATE TABLE verificacion_solicitud (
    id BIGINT NOT NULL AUTO_INCREMENT,
    familia_id BIGINT NOT NULL,
    usuario_correo VARCHAR(254) NOT NULL,
    verificada_at TIMESTAMP NOT NULL,
    resultado VARCHAR(20) NOT NULL,
    observacion VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT fk_verificacion_familia
        FOREIGN KEY (familia_id) REFERENCES familia (id)
);

CREATE TABLE cambio_dato_verificacion (
    verificacion_id BIGINT NOT NULL,
    campo VARCHAR(50) NOT NULL,
    valor_anterior VARCHAR(2000),
    valor_nuevo VARCHAR(2000),
    CONSTRAINT fk_cambio_dato_verificacion
        FOREIGN KEY (verificacion_id) REFERENCES verificacion_solicitud (id)
);
