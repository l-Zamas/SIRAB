CREATE TABLE cuenta_usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    correo VARCHAR(254) NOT NULL,
    contrasena_hash VARCHAR(100) NOT NULL,
    rol VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cuenta_usuario_correo UNIQUE (correo),
    CONSTRAINT chk_cuenta_usuario_rol
        CHECK (rol IN ('PERSONAL_CAMPO', 'COORDINACION', 'ADMIN'))
);
