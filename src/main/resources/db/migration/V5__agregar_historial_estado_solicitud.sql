ALTER TABLE familia
    ADD COLUMN fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE historial_estado_solicitud (
    id BIGINT NOT NULL AUTO_INCREMENT,
    familia_id BIGINT NOT NULL,
    estado_anterior VARCHAR(20),
    estado_nuevo VARCHAR(20) NOT NULL,
    actor_tipo VARCHAR(20) NOT NULL,
    usuario_correo VARCHAR(254),
    fecha_hora TIMESTAMP NOT NULL,
    observacion VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT fk_historial_estado_familia
        FOREIGN KEY (familia_id) REFERENCES familia (id)
);

INSERT INTO historial_estado_solicitud (
    familia_id, estado_anterior, estado_nuevo, actor_tipo, fecha_hora, observacion
)
SELECT id, NULL, estado, 'SISTEMA', fecha_creacion, 'Estado vigente al activar el historial.'
FROM familia;
