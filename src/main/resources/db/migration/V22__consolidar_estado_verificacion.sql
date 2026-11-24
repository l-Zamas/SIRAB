-- La verificación es una acción auditada; el estado general posterior es EN_ATENCION.
UPDATE familia
SET estado = 'EN_ATENCION'
WHERE estado = 'VERIFICADO';
