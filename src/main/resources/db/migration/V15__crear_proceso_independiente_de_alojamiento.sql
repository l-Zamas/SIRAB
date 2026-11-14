CREATE TABLE solicitud_alojamiento (
    id BIGINT NOT NULL AUTO_INCREMENT,
    familia_id BIGINT NOT NULL,
    registrada_at TIMESTAMP(6) NOT NULL,
    registrada_por VARCHAR(254) NOT NULL,
    observacion VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT fk_solicitud_alojamiento_familia
        FOREIGN KEY (familia_id) REFERENCES familia (id)
);

CREATE INDEX idx_solicitud_alojamiento_familia
    ON solicitud_alojamiento (familia_id, registrada_at);

CREATE TABLE verificacion_alojamiento (
    id BIGINT NOT NULL AUTO_INCREMENT,
    solicitud_alojamiento_id BIGINT NOT NULL,
    resultado VARCHAR(30) NOT NULL,
    documento_fotografia_id BIGINT NOT NULL,
    verificada_at TIMESTAMP(6) NOT NULL,
    verificada_por VARCHAR(254) NOT NULL,
    observacion VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT fk_verificacion_alojamiento_solicitud
        FOREIGN KEY (solicitud_alojamiento_id) REFERENCES solicitud_alojamiento (id),
    CONSTRAINT fk_verificacion_alojamiento_fotografia
        FOREIGN KEY (documento_fotografia_id) REFERENCES documento_privado (id),
    CONSTRAINT chk_verificacion_alojamiento_resultado
        CHECK (resultado IN ('VERIFICADA', 'NO_VERIFICADA'))
);

CREATE TABLE aprobacion_provisional_alojamiento (
    id BIGINT NOT NULL AUTO_INCREMENT,
    solicitud_alojamiento_id BIGINT NOT NULL,
    decision VARCHAR(20) NOT NULL,
    decidida_at TIMESTAMP(6) NOT NULL,
    decidida_por VARCHAR(254) NOT NULL,
    observacion VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT fk_aprobacion_provisional_solicitud
        FOREIGN KEY (solicitud_alojamiento_id) REFERENCES solicitud_alojamiento (id),
    CONSTRAINT chk_aprobacion_provisional_decision
        CHECK (decision IN ('APROBADA', 'RECHAZADA'))
);

CREATE TABLE aprobacion_definitiva_alojamiento (
    id BIGINT NOT NULL AUTO_INCREMENT,
    solicitud_alojamiento_id BIGINT NOT NULL,
    institucion VARCHAR(200) NOT NULL,
    referencia_externa VARCHAR(200),
    decision VARCHAR(20) NOT NULL,
    decidida_at TIMESTAMP(6) NOT NULL,
    documento_decision_id BIGINT,
    registrada_at TIMESTAMP(6) NOT NULL,
    registrada_por VARCHAR(254) NOT NULL,
    observacion VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT fk_aprobacion_definitiva_solicitud
        FOREIGN KEY (solicitud_alojamiento_id) REFERENCES solicitud_alojamiento (id),
    CONSTRAINT fk_aprobacion_definitiva_documento
        FOREIGN KEY (documento_decision_id) REFERENCES documento_privado (id),
    CONSTRAINT chk_aprobacion_definitiva_decision
        CHECK (decision IN ('APROBADA', 'RECHAZADA'))
);

CREATE TABLE confirmacion_prestacion_alojamiento (
    id BIGINT NOT NULL AUTO_INCREMENT,
    solicitud_alojamiento_id BIGINT NOT NULL,
    aprobacion_definitiva_id BIGINT NOT NULL,
    proporcionado_at TIMESTAMP(6) NOT NULL,
    confirmado_at TIMESTAMP(6) NOT NULL,
    confirmado_por VARCHAR(254) NOT NULL,
    documento_evidencia_id BIGINT,
    observacion VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT fk_confirmacion_alojamiento_solicitud
        FOREIGN KEY (solicitud_alojamiento_id) REFERENCES solicitud_alojamiento (id),
    CONSTRAINT fk_confirmacion_alojamiento_aprobacion
        FOREIGN KEY (aprobacion_definitiva_id)
        REFERENCES aprobacion_definitiva_alojamiento (id),
    CONSTRAINT fk_confirmacion_alojamiento_documento
        FOREIGN KEY (documento_evidencia_id) REFERENCES documento_privado (id)
);
