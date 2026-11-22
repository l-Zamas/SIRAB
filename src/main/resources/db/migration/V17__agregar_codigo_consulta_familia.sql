ALTER TABLE familia
    ADD COLUMN codigo_consulta_hash VARCHAR(64) NULL;

CREATE UNIQUE INDEX uk_familia_codigo_consulta_hash
    ON familia (codigo_consulta_hash);
