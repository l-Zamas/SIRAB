# RF-07 — Ordenamiento y filtrado por urgencia

La priorización se integra al listado existente de solicitudes de `/campo/registros`, protegido por el rol `PERSONAL_CAMPO`.

La cola activa excluye solicitudes `ATENDIDO` y se ordena primero por nivel (CRÍTICA, ALTA, MEDIA, BAJA), después por puntuación descendente y, en caso de empate, por fecha de creación ascendente y registro ascendente para mantener un orden estable. El filtro `urgencia` acepta `TODAS`, `CRITICA`, `ALTA`, `MEDIA` o `BAJA`; al filtrar, se conserva ese valor en el formulario y en la URL.

El módulo no pagina actualmente las solicitudes, por lo que no se introduce paginación ni un parámetro de página. Las solicitudes atendidas permanecen disponibles en una sección histórica separada con acceso a su detalle e historial; no participan en el orden o filtro de la cola activa.

El listado presenta identificación, nombre, barrio, puntuación, nivel con indicador visual y texto, necesidades y situaciones familiares registradas. El filtro se aplica en el servidor a la misma lista ya ordenada.

## Medición de consultas

En MySQL 8.0.45, Hibernate Statistics registró 7 sentencias para una familia nueva en la cola y 25 para diez familias nuevas, con dos familias preexistentes en ambos casos. El incremento de 18 sentencias confirma dos consultas adicionales por cada familia agregada, asociadas a las colecciones `LAZY` que el servicio materializa para el listado. La consulta completa carga y ordena la cola en memoria; no hay paginación.

No se introdujo una estrategia de carga adicional sin un volumen operativo esperado que justifique el cambio. Antes de operar con colas grandes, medir con datos representativos; si el costo lo requiere, evaluar una carga conjunta de las colecciones para eliminar el crecimiento por familia y validar su impacto en duplicación de filas y memoria.
