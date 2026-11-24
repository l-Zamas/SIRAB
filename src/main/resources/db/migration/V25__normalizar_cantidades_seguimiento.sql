ALTER TABLE seguimiento_necesidad
    MODIFY COLUMN cantidad_solicitada INT NULL,
    MODIFY COLUMN cantidad_entregada INT NOT NULL DEFAULT 0;

ALTER TABLE historial_necesidad
    MODIFY COLUMN cantidad_entregada INT NULL;
