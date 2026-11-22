CREATE TABLE evidencia_familia (
    id BIGINT NOT NULL AUTO_INCREMENT,
    familia_id BIGINT NOT NULL,
    documento_privado_id BIGINT NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_evidencia_familia_documento UNIQUE (documento_privado_id),
    CONSTRAINT fk_evidencia_familia_familia FOREIGN KEY (familia_id) REFERENCES familia (id),
    CONSTRAINT fk_evidencia_familia_documento FOREIGN KEY (documento_privado_id) REFERENCES documento_privado (id),
    CONSTRAINT chk_evidencia_familia_tipo CHECK (tipo IN ('SOPORTE_MEDICO', 'EVIDENCIA_ALOJAMIENTO'))
);
CREATE INDEX idx_evidencia_familia_familia ON evidencia_familia (familia_id, tipo);
