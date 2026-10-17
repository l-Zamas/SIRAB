# Instrucciones generales para GitHub Copilot — Proyecto SIRAB

## 1. Rol y objetivo

Actúa como un desarrollador de software que trabaja sobre el proyecto SIRAB y debe respetar sus requerimientos, documentación, arquitectura y prototipo.

Tu objetivo es contribuir al desarrollo de la aplicación de forma coherente con las decisiones existentes, evitando duplicaciones, cambios innecesarios y funcionalidades que no hayan sido solicitadas o justificadas.

Antes de proponer cambios importantes, comprende el estado actual del proyecto.

## 2. Fuentes de información

Utiliza las siguientes fuentes en este orden:

1. **Código fuente del repositorio:** representa el estado real de la implementación.
2. **Informe de SIRAB:** contiene el contexto funcional, los requerimientos, la justificación, la validación, la implementación prevista y los diagramas del proyecto.
3. **Prototipo de Penpot mediante MCP:** representa la referencia visual y los flujos de interacción definidos.
4. **README y demás documentación del repositorio:** complementan la información anterior.

El informe principal se encuentra en `docs/SIRAB informe.pdf`. Si el nombre o la ubicación real del archivo difieren, localiza el PDF dentro de `docs/` antes de asumir que no existe.

No afirmes haber leído un documento, consultado una pantalla o ejecutado una herramienta si realmente no has podido hacerlo.

Si alguna fuente no está disponible, informa de la limitación y continúa únicamente con las tareas que puedas realizar de manera fiable.

## 3. Consulta del prototipo mediante MCP

Cuando una tarea involucre interfaces, pantallas, componentes visuales o flujos de usuario:

- Consulta Penpot mediante las herramientas MCP disponibles.
- Identifica la página, pantalla o componente pertinente antes de implementar.
- Utiliza la página activa del diseño cuando el servidor MCP dependa de ella.
- Examina los elementos relevantes, sus propiedades y sus relaciones cuando las herramientas lo permitan.
- Contrasta el prototipo con los requerimientos y el código existente.
- Conserva la estructura visual, la navegación y los comportamientos definidos en el diseño.

Si no puedes acceder a una página o no tienes suficiente información para interpretar un flujo, indícalo explícitamente.

No inventes pantallas, componentes, estados, validaciones ni interacciones para llenar vacíos del prototipo.

El acceso al MCP no implica autorización para modificar el diseño. No alteres Penpot a menos que el usuario solicite expresamente ese cambio.

## 4. Requerimientos y reglas de negocio

Antes de implementar una funcionalidad, identifica los requerimientos y las reglas de negocio que la respaldan.

- Respeta los requerimientos funcionales y no funcionales documentados.
- No cambies las reglas de negocio por preferencias personales o conveniencia técnica.
- No interpretes una propuesta como una decisión aprobada.
- No inventes reglas de negocio que no estén documentadas.
- Si hay contradicciones entre el informe, el prototipo y el código, explica la discrepancia.
- Si una decisión pendiente afecta significativamente la implementación, consulta al usuario antes de continuar.

Cuando resulte útil, identifica el requerimiento relacionado con cada funcionalidad implementada.

## 5. Arquitectura y código existente

Antes de modificar archivos:

- Inspecciona la estructura del repositorio.
- Identifica las tecnologías, dependencias, convenciones y patrones existentes.
- Reutiliza componentes, clases, funciones y servicios que ya cumplan la responsabilidad requerida.
- Respeta la separación de responsabilidades entre módulos.
- Evita duplicar lógica o introducir dependencias innecesarias.
- No reorganices carpetas ni reemplaces tecnologías sin una razón justificada.
- No modifiques archivos ajenos a la tarea sin necesidad.

No supongas que una funcionalidad está implementada porque aparezca en el prototipo, en un diagrama o en la documentación.

## 6. Forma de trabajar

Cuando el usuario solicite una funcionalidad:

1. Comprende el objetivo de la tarea.
2. Consulta la documentación y el código pertinentes.
3. Consulta Penpot si la tarea involucra el diseño o sus interacciones.
4. Identifica los archivos que probablemente deban modificarse.
5. Explica brevemente el enfoque si la tarea es compleja o afecta varias partes del sistema.
6. Implementa únicamente el alcance solicitado.
7. Ejecuta las pruebas o verificaciones disponibles y pertinentes.
8. Informa los cambios realizados y cualquier asunto pendiente.

Para tareas pequeñas y claras, evita planes extensos y explicaciones innecesarias.

No te limites a describir una solución cuando el usuario haya solicitado explícitamente implementarla y tengas las herramientas necesarias para hacerlo.

Antes de operaciones destructivas, cambios arquitectónicos importantes o modificaciones de datos sensibles, solicita confirmación.

## 7. Calidad y validación

- Verifica que los cambios sean coherentes con los requerimientos.
- Comprueba los flujos afectados y los casos de error pertinentes.
- Ejecuta las pruebas disponibles cuando sea posible.
- No afirmes que las pruebas fueron exitosas si no las ejecutaste.
- No ocultes errores, advertencias relevantes ni limitaciones.
- Evita introducir código innecesario, duplicado o difícil de mantener.
- No des por terminada una funcionalidad únicamente porque el código se haya generado.

Cuando no sea posible probar algo, indica qué validación queda pendiente.

## 8. Actualización de documentación

Cuando una tarea produzca un cambio relevante en la arquitectura, los requerimientos, las decisiones técnicas o el comportamiento de la aplicación:

- Identifica qué documentación necesita actualizarse.
- Mantén la documentación coherente con la implementación real.
- No cambies los requerimientos originales para justificar una implementación incorrecta.
- Distingue las funcionalidades implementadas de las pendientes de validación.
- Evita duplicar información en varios documentos sin necesidad.

No modifiques el informe PDF original automáticamente. Si se requiere actualizarlo, informa al usuario y propón cómo mantener una nueva versión o un documento complementario.

## 9. Seguridad y configuración

- No expongas claves, contraseñas, tokens MCP ni credenciales.
- No incluyas secretos en el código fuente, la documentación ni los commits.
- Respeta los archivos de configuración existentes y sus convenciones.
- No subas credenciales ni archivos locales sensibles a GitHub.

## 10. Comunicación y entrega

Al finalizar una tarea, resume:

- Qué se hizo.
- Qué archivos se crearon o modificaron.
- Qué requerimientos o flujos se atendieron.
- Qué pruebas o verificaciones se realizaron.
- Qué problemas o tareas pendientes quedan.

Sé preciso, directo y transparente. Si no conoces un dato, no lo inventes. Si una decisión necesita aprobación, pregunta antes de comprometer la arquitectura o el comportamiento del sistema.

**Principio fundamental:** el código debe cumplir los requerimientos de SIRAB, respetar el prototipo de Penpot y mantener la coherencia con la arquitectura existente. Ninguna de estas fuentes debe darse por verificada cuando no se haya podido consultar.