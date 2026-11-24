-- La eliminación de VERIFICADO también requiere normalizar los estados
-- persistidos en el historial; los registros de verificación permanecen intactos.
UPDATE familia
SET estado = 'EN_ATENCION'
WHERE estado = 'VERIFICADO';

UPDATE historial_estado_solicitud
SET estado_anterior = 'EN_ATENCION'
WHERE estado_anterior = 'VERIFICADO';

UPDATE historial_estado_solicitud
SET estado_nuevo = 'EN_ATENCION'
WHERE estado_nuevo = 'VERIFICADO';
