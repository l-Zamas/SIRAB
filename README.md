# SIRAB: Sistema Integral de Respuesta y Ayuda para Buenaventura

> **Emergencia por terremoto.** Plataforma web para organizar la atención de las familias afectadas: registrar qué necesitan, verificar la información, priorizar por urgencia y llevar el control de las ayudas entregadas.
---

## Tabla de contenido

1. [Qué hace SIRAB](#1-qué-hace-sirab)
2. [Stack tecnológico](#2-stack-tecnológico)
3. [Arquitectura](#3-arquitectura)
4. [Estructura de carpetas](#4-estructura-de-carpetas)
5. [Requisitos funcionales](#5-requisitos-funcionales)
6. [Convenciones de trabajo](#6-convenciones-de-trabajo)
7. [Seguridad y privacidad](#7-seguridad-y-privacidad)
8. [Reglas de commits](#8-seguridad-y-privacidad)


## 1. Qué hace SIRAB

SIRAB se organiza en cuatro pilares:

1. **Registro de necesidades:** las familias se registran e indican qué requieren (alimentos, agua, medicamentos o alojamiento).
2. **Verificación de la información:** el personal de campo confirma los datos contra el documento de identidad.
3. **Priorización por urgencia:** el sistema calcula automáticamente el nivel de urgencia de cada familia.
4. **Control de ayudas entregadas:** se registra qué recibió cada familia para evitar duplicidad o vacíos.


## 2. Stack tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 21 (LTS) |
| Framework | Spring Boot 3.x (Web, Data JPA, Validation, Security) |
| Vistas | Thymeleaf (renderizado en el servidor) con fragmentos reutilizables |
| Base de datos | MySQL |
| Migraciones | Flyway |
| Seguridad | Spring Security con roles (`PERSONAL_CAMPO`, `COORDINACION`, `ADMIN`) |
| Estilos | CSS propio basado en *design tokens* (variables CSS) exportados del prototipo |
| Modo sin conexión | Service Worker + IndexedDB (JavaScript mínimo, ver [sección 5](#5-arquitectura)) |
| Pruebas | JUnit 5, Mockito, Spring Boot Test, Testcontainers |
| Empaquetado | Maven |
| Integración continua | GitHub Actions |

## 3. Arquitectura

Aplicación monolítica modular, organizada **por funcionalidad** (no por capas técnicas). Cada módulo tiene sus propios controladores, servicios, entidades y repositorios.

```
Navegador ──► Controladores (web) ──► Servicios ──► Repositorios ──► PostgreSQL
                    │                     │
                    ▼                     ▼
            Plantillas Thymeleaf     Reglas de dominio
            (layouts + fragmentos)   (urgencia, duplicados)
```

Decisiones principales:

- **Un módulo por pilar:** `registro`, `verificacion`, `priorizacion`, `seguimiento`, `duplicados`, `reportes`.
- **Las reglas de negocio viven en servicios y en el dominio**, nunca en los controladores ni en las plantillas.
- **Almacenamiento de documentos detrás de una interfaz** (`AlmacenamientoDocumentos`), con implementación local en desarrollo y reemplazable por almacenamiento externo en producción.

## 4. Estructura de carpetas

```
SIRAB/
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/sirab/
    │   │       ├── config/
    │   │       ├── registro/
    │   │       │   └── web/
    │   │       └── SirabApplication.java
    │   └── resources/
    │       ├── templates/
    │       │   └── familia/
    │       ├── application.properties
    │       ├── application-dev.properties
    │       └── application-prod.properties
    │
    └── test/
        ├── java/
        │   └── com/sirab/
        │       └── SirabApplicationTests.java
        └── resources/
            ├── application-test.properties
            └── messages.properties

```

## 5. Requisitos funcionales

| Pilar | Código | Descripción | Módulo |
|---|---|---|---|
| Registro | RF-01 | Registrar familias con datos básicos (nombre, ubicación, número de integrantes, contacto) | `registro` |
| Registro | RF-02 | Registrar necesidades específicas por familia (alimentos, agua, medicamentos, alojamiento) | `registro` |
| Verificación | RF-03 | Confirmar y actualizar la veracidad de los datos registrados | `verificacion` |
| Verificación | RF-04 | Marcar el estado de un registro (pendiente, verificado, atendido) | `verificacion` |
| Verificación | RF-05 | Evitar duplicados de una misma familia en el sistema | `duplicados` |
| Priorización | RF-06 | Calcular un nivel de urgencia por familia según vulnerabilidad, tipo de necesidad y tiempo sin atención | `priorizacion` |
| Priorización | RF-07 | Ordenar y filtrar familias por nivel de urgencia | `priorizacion` / `reportes` |
| Seguimiento | RF-08 | Registrar las ayudas entregadas a cada familia (tipo, fecha, cantidad) | `seguimiento` |
| Seguimiento | RF-09 | Generar historial de atención por familia | `seguimiento` |
| Seguimiento | RF-10 | Generar reportes y consolidados para entidades coordinadoras (Alcaldía, UNGRD) | `reportes` |

### Estado del registro familiar

El flujo web actual recopila el nombre de quien registra, el total de integrantes, un teléfono de contacto, barrio y referencia de ubicación, situaciones familiares y necesidades; el consentimiento se registra antes de guardar. El número de integrantes no crea fichas individuales ni implica atributos por persona. El paso de documento informa que la carga no está disponible hasta contar con almacenamiento y acceso protegidos. La consulta interna, el seguimiento de estado, la detección de duplicados, el modo sin conexión y el cálculo automático de urgencia continúan fuera del alcance implementado. El texto del consentimiento requiere aprobación legal antes de operar con datos reales.

## 6. Convenciones de trabajo

- **Ramas:** `main` (estable), `develop` (integración) y `feature/<modulo>-<descripcion>`.
- **Commits:** mensajes en español, en imperativo y con prefijo (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`).
- **Código:** nombres de clases y métodos en español de dominio coherente con el prototipo (`Familia`, `Verificacion`, `AyudaEntregada`).
- **Plantillas:** un fragmento por componente visual y sin lógica de negocio.
- **Estilos:** solo variables de `tokens.css`, sin colores ni medidas sueltas.
- **Accesibilidad:** la urgencia y el estado se muestran siempre con **icono y texto**, nunca solo con color.

## 7. Seguridad y privacidad

SIRAB maneja datos personales sensibles de personas en situación de vulnerabilidad.

- **Consentimiento informado** antes de registrar datos (pantalla F02).
- **Roles y permisos:** cada actor accede solo a lo que necesita.
- **Contraseñas** cifradas con un algoritmo de *hash* adaptativo (por ejemplo, BCrypt).
- **Documentos de identidad:** almacenamiento restringido y acceso solo para personal autorizado.
- **Auditoría:** se registra quién consulta o modifica cada registro.
- **Protección de datos:** en Colombia aplica la Ley 1581 de 2012. Debe confirmarse el cumplimiento con asesoría antes de operar con datos reales.
- **Nunca** subir al repositorio contraseñas, llaves ni datos reales.


## 8. Reglas de commits

| Tipo | Descripción | Ejemplo |
|---|---|---|
| `feat` | Agrega una nueva funcionalidad. | `feat: agregar módulo de usuarios` |
| `fix` | Corrige un error en el código. | `fix: corregir validación del formulario` |
| `docs` | Crea o actualiza documentación. | `docs: actualizar README` |
| `style` | Realiza cambios de formato sin afectar la lógica. | `style: ajustar indentación` |
| `refactor` | Reestructura el código sin cambiar su comportamiento. | `refactor: simplificar lógica de autenticación` |
| `perf` | Mejora el rendimiento. | `perf: optimizar consultas SQL` |
| `test` | Agrega o modifica pruebas. | `test: agregar pruebas de registro` |
| `build` | Modifica dependencias o herramientas de compilación. | `build: actualizar dependencias de Maven` |
| `ci` | Modifica procesos de integración continua. | `ci: configurar ejecución de pruebas` |
| `chore` | Realiza tareas generales de mantenimiento. | `chore: actualizar archivo .gitignore` |
| `revert` | Revierte un commit anterior. | `revert: revertir cambios del módulo de usuarios` |
