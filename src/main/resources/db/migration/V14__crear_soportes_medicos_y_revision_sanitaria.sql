CREATE TABLE documento_privado (
    id BIGINT NOT NULL AUTO_INCREMENT,
    clave_almacenamiento VARCHAR(512) NOT NULL,
    nombre_original VARCHAR(255) NOT NULL,
    tipo_contenido VARCHAR(120) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    clasificacion VARCHAR(40) NOT NULL,
    cargado_at TIMESTAMP(6) NOT NULL,
    cargado_por VARCHAR(254) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_documento_privado_clave UNIQUE (clave_almacenamiento),
    CONSTRAINT chk_documento_privado_tamano CHECK (tamano_bytes > 0),
    CONSTRAINT chk_documento_privado_clasificacion
        CHECK (clasificacion IN (
            'SOPORTE_MEDICO',
            'DECISION_SANITARIA',
            'EVIDENCIA_ALOJAMIENTO',
            'DECISION_ENTIDAD',
            'CONFIRMACION_PRESTACION'
        ))
);

CREATE TABLE solicitud_ayuda_medica (
    id BIGINT NOT NULL AUTO_INCREMENT,
    familia_id BIGINT NOT NULL,
    estado_revision VARCHAR(40) NOT NULL,
    registrada_at TIMESTAMP(6) NOT NULL,
    registrada_por VARCHAR(254) NOT NULL,
    observacion VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT fk_solicitud_ayuda_medica_familia
        FOREIGN KEY (familia_id) REFERENCES familia (id),
    CONSTRAINT chk_solicitud_ayuda_medica_estado
        CHECK (estado_revision IN (
            'PENDIENTE_REVISION_SANITARIA',
            'APROBADA_EXTERNAMENTE',
            'RECHAZADA_EXTERNAMENTE'
        ))
);

CREATE INDEX idx_solicitud_ayuda_medica_familia
    ON solicitud_ayuda_medica (familia_id, registrada_at);

CREATE TABLE soporte_medico (
    id BIGINT NOT NULL AUTO_INCREMENT,
    solicitud_ayuda_medica_id BIGINT NOT NULL,
    documento_privado_id BIGINT NOT NULL,
    tipo_soporte VARCHAR(40) NOT NULL,
    cargado_at TIMESTAMP(6) NOT NULL,
    cargado_por VARCHAR(254) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_soporte_medico_documento UNIQUE (documento_privado_id),
    CONSTRAINT fk_soporte_medico_solicitud
        FOREIGN KEY (solicitud_ayuda_medica_id) REFERENCES solicitud_ayuda_medica (id),
    CONSTRAINT fk_soporte_medico_documento
        FOREIGN KEY (documento_privado_id) REFERENCES documento_privado (id)
);

CREATE TABLE revision_sanitaria_externa (
    id BIGINT NOT NULL AUTO_INCREMENT,
    solicitud_ayuda_medica_id BIGINT NOT NULL,
    institucion VARCHAR(200) NOT NULL,
    profesional_nombre VARCHAR(200) NOT NULL,
    identificador_profesional VARCHAR(100) NOT NULL,
    decision VARCHAR(20) NOT NULL,
    decidida_at TIMESTAMP(6) NOT NULL,
    referencia_externa VARCHAR(200),
    documento_decision_id BIGINT NOT NULL,
    registrada_at TIMESTAMP(6) NOT NULL,
    registrada_por VARCHAR(254) NOT NULL,
    observacion VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT fk_revision_sanitaria_solicitud
        FOREIGN KEY (solicitud_ayuda_medica_id) REFERENCES solicitud_ayuda_medica (id),
    CONSTRAINT fk_revision_sanitaria_documento
        FOREIGN KEY (documento_decision_id) REFERENCES documento_privado (id),
    CONSTRAINT chk_revision_sanitaria_decision
        CHECK (decision IN ('APROBADA', 'RECHAZADA'))
);

CREATE INDEX idx_revision_sanitaria_solicitud_fecha
    ON revision_sanitaria_externa (solicitud_ayuda_medica_id, decidida_at);

ALTER TABLE asignacion_ayuda
    ADD CONSTRAINT fk_asignacion_ayuda_revision_sanitaria
        FOREIGN KEY (revision_sanitaria_id) REFERENCES revision_sanitaria_externa (id);

ALTER TABLE asignacion_ayuda
    ADD CONSTRAINT chk_asignacion_ayuda_origen_referencia
        CHECK (
            (
                origen = 'REGLA'
                AND evaluacion_id IS NOT NULL
                AND regla_asignacion_id IS NOT NULL
                AND revision_sanitaria_id IS NULL
            )
            OR
            (
                origen = 'REVISION_SANITARIA'
                AND evaluacion_id IS NULL
                AND regla_asignacion_id IS NULL
                AND revision_sanitaria_id IS NOT NULL
            )
        );
