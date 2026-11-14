CREATE TABLE lote_inventario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    articulo_ayuda_id BIGINT NOT NULL,
    tipo_ayuda VARCHAR(30) NOT NULL,
    codigo_lote VARCHAR(100),
    fecha_vencimiento DATE,
    unidad VARCHAR(40) NOT NULL,
    saldo_disponible DECIMAL(18, 6) NOT NULL,
    recibido_at TIMESTAMP(6) NOT NULL,
    origen VARCHAR(200) NOT NULL,
    registrado_por VARCHAR(254) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_lote_inventario_articulo
        FOREIGN KEY (articulo_ayuda_id, tipo_ayuda)
        REFERENCES articulo_ayuda (id, tipo_ayuda),
    CONSTRAINT uk_lote_inventario_codigo
        UNIQUE (articulo_ayuda_id, codigo_lote),
    CONSTRAINT chk_lote_inventario_saldo CHECK (saldo_disponible >= 0),
    CONSTRAINT chk_lote_inventario_tipo CHECK (
        tipo_ayuda IN ('AGUA', 'ALIMENTOS', 'MEDICAMENTOS')
        AND (
            tipo_ayuda <> 'MEDICAMENTOS'
            OR (codigo_lote IS NOT NULL AND fecha_vencimiento IS NOT NULL)
        )
    )
);

CREATE INDEX idx_lote_inventario_articulo_vencimiento
    ON lote_inventario (articulo_ayuda_id, fecha_vencimiento);

CREATE TABLE movimiento_inventario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lote_inventario_id BIGINT NOT NULL,
    entrega_ayuda_id BIGINT,
    tipo VARCHAR(30) NOT NULL,
    cantidad DECIMAL(18, 6) NOT NULL,
    registrado_at TIMESTAMP(6) NOT NULL,
    usuario_correo VARCHAR(254) NOT NULL,
    observacion VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT fk_movimiento_inventario_lote
        FOREIGN KEY (lote_inventario_id) REFERENCES lote_inventario (id),
    CONSTRAINT fk_movimiento_inventario_entrega
        FOREIGN KEY (entrega_ayuda_id) REFERENCES entrega_ayuda (id),
    CONSTRAINT chk_movimiento_inventario_tipo
        CHECK (tipo IN (
            'APERTURA',
            'INGRESO',
            'SALIDA_ENTREGA',
            'AJUSTE_POSITIVO',
            'AJUSTE_NEGATIVO'
        )),
    CONSTRAINT chk_movimiento_inventario_cantidad CHECK (cantidad > 0)
);

CREATE INDEX idx_movimiento_inventario_lote_fecha
    ON movimiento_inventario (lote_inventario_id, registrado_at, id);

CREATE INDEX idx_movimiento_inventario_entrega
    ON movimiento_inventario (entrega_ayuda_id);
