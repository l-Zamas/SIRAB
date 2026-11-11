# RF-06 — Gestión automática de urgencia

## Alcance implementado

El sistema calcula la puntuación y el nivel de urgencia desde los datos vigentes de cada solicitud. La puntuación no se guarda en la base de datos: se deriva al consultar las solicitudes, por lo que las correcciones de necesidades o situación familiar se reflejan en la siguiente consulta y el componente temporal aumenta sin tareas manuales.

## Puntuación

| Factor | Dato registrado | Puntos |
|---|---|---:|
| Necesidades | Alojamiento | 8 |
| Necesidades | Medicamentos | 5 |
| Necesidades | Agua | 3 |
| Necesidades | Alimentos | 2 |
| Situación familiar | Menores de 5 años | 5 |
| Situación familiar | Mujeres gestantes | 5 |
| Situación familiar | Personas con discapacidad | 3 |
| Situación familiar | Adultos mayores | 3 |
| Tiempo desde `fecha_creacion` | 0–2 días completos | 0 |
| Tiempo desde `fecha_creacion` | 3–5 días completos | 2 |
| Tiempo desde `fecha_creacion` | 6–10 días completos | 4 |
| Tiempo desde `fecha_creacion` | Más de 10 días completos | 6 |

La puntuación de necesidades suma las necesidades distintas registradas (máximo 18). Cada condición familiar se cuenta una sola vez, conforme a la representación existente como conjunto; no se infieren cantidades de integrantes. El máximo familiar es 16 y el máximo total es 40.

El tiempo se calcula como duración transcurrida entre los `Instant` de creación y consulta, en días completos. No depende de la zona horaria local ni de que el personal modifique la solicitud.

| Total | Nivel |
|---:|---|
| 0–9 | BAJA |
| 10–19 | MEDIA |
| 20–29 | ALTA |
| 30–40 | CRÍTICA |

Las solicitudes `ATENDIDO` mantienen sus datos e historial y se excluyen de la cola activa. No se agrega una columna ni migración: la puntuación es información calculada, no un dato persistente. El personal no tiene una operación para cambiarla manualmente.
