CREATE TABLE articulo_ayuda (
    id BIGINT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(60) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    tipo_ayuda VARCHAR(30) NOT NULL,
    unidad VARCHAR(40) NOT NULL,
    activo BOOLEAN NOT NULL,
    creado_at TIMESTAMP(6) NOT NULL,
    creado_por VARCHAR(254) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_articulo_ayuda_codigo UNIQUE (codigo),
    CONSTRAINT uk_articulo_ayuda_id_tipo UNIQUE (id, tipo_ayuda),
    CONSTRAINT chk_articulo_ayuda_tipo
        CHECK (tipo_ayuda IN ('AGUA', 'ALIMENTOS', 'MEDICAMENTOS'))
);

CREATE TABLE regla_asignacion_ayuda (
    id BIGINT NOT NULL AUTO_INCREMENT,
    articulo_ayuda_id BIGINT NOT NULL,
    version INT NOT NULL,
    tipo_criterio VARCHAR(30) NOT NULL,
    situacion VARCHAR(40),
    criterio_codigo VARCHAR(50) NOT NULL,
    cantidad DECIMAL(18, 6) NOT NULL,
    unidad VARCHAR(40) NOT NULL,
    vigente_desde DATE NOT NULL,
    vigente_hasta DATE,
    creado_at TIMESTAMP(6) NOT NULL,
    creado_por VARCHAR(254) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_regla_asignacion_version_criterio
        UNIQUE (articulo_ayuda_id, version, criterio_codigo),
    CONSTRAINT fk_regla_asignacion_articulo
        FOREIGN KEY (articulo_ayuda_id) REFERENCES articulo_ayuda (id),
    CONSTRAINT chk_regla_asignacion_version CHECK (version > 0),
    CONSTRAINT chk_regla_asignacion_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_regla_asignacion_vigencia
        CHECK (vigente_hasta IS NULL OR vigente_hasta >= vigente_desde),
    CONSTRAINT chk_regla_asignacion_criterio
        CHECK (
            (tipo_criterio = 'BASE' AND situacion IS NULL AND criterio_codigo = 'BASE')
            OR
            (
                tipo_criterio = 'SITUACION_FAMILIAR'
                AND situacion IS NOT NULL
                AND criterio_codigo = situacion
                AND situacion IN (
                    'MENORES_DE_CINCO',
                    'ADULTOS_MAYORES',
                    'PERSONAS_CON_DISCAPACIDAD',
                    'MUJERES_GESTANTES'
                )
            )
        )
);

CREATE INDEX idx_regla_asignacion_articulo_vigencia
    ON regla_asignacion_ayuda (articulo_ayuda_id, vigente_desde, vigente_hasta);
