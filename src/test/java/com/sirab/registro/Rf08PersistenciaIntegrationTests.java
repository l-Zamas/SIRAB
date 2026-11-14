package com.sirab.registro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Rf08PersistenciaIntegrationTests {

    @Autowired
    private EntityManager entityManager;

    @Test
    void persisteElRegistroHistoricoSinExigirLosCamposNuevos() {
        Familia familia = crearFamilia("HISTORICO_RF08");
        EntregaAyuda entrega = new EntregaAyuda(
                familia,
                Necesidad.AGUA,
                "20 L",
                LocalDate.of(2026, 2, 1),
                Instant.parse("2026-02-01T18:00:00Z"),
                "campo@sirab.local",
                false,
                "Registro legado");

        entityManager.persist(familia);
        entityManager.flush();
        entityManager.persist(entrega);
        entityManager.flush();
        Long entregaId = entrega.getId();
        entityManager.clear();

        EntregaAyuda persistida = entityManager.find(EntregaAyuda.class, entregaId);
        assertEquals("20 L", persistida.getCantidad());
        assertEquals(LocalDate.of(2026, 2, 1), persistida.getFechaEntrega());
        assertNull(persistida.getAsignacion());
        assertNull(persistida.getCantidadValor());
        assertNull(persistida.getUnidad());
        assertNull(persistida.getFechaHoraEntrega());
    }

    @Test
    void persisteAsignacionesInventarioSoportesYFasesDeAlojamiento() {
        Familia familia = crearFamilia("MODELO_RF08");
        entityManager.persist(familia);
        entityManager.flush();

        Instant ahora = Instant.parse("2026-03-01T15:00:00Z");
        ArticuloAyuda agua = new ArticuloAyuda(
                "RF08_AGUA_TEST", "Artículo de agua de prueba",
                Necesidad.AGUA, "UNIDAD_TEST", true, ahora, "coord@sirab.local");
        ArticuloAyuda medicamento = new ArticuloAyuda(
                "RF08_MED_TEST", "Artículo médico de prueba",
                Necesidad.MEDICAMENTOS, "UNIDAD_TEST", true, ahora, "coord@sirab.local");
        entityManager.persist(agua);
        entityManager.persist(medicamento);
        entityManager.flush();

        ReglaAsignacionAyuda regla = new ReglaAsignacionAyuda(
                agua,
                1,
                TipoCriterioAsignacion.SITUACION_FAMILIAR,
                SituacionFamiliar.MENORES_DE_CINCO,
                new BigDecimal("2.500000"),
                "UNIDAD_TEST",
                LocalDate.of(2026, 1, 1),
                null,
                ahora,
                "coord@sirab.local");
        entityManager.persist(regla);

        EvaluacionAsignacionAyuda evaluacion = new EvaluacionAsignacionAyuda(
                familia,
                1,
                "eval-rf08-modelo",
                ahora,
                "coord@sirab.local",
                Set.of(Necesidad.AGUA),
                Set.of(SituacionFamiliar.MENORES_DE_CINCO));
        entityManager.persist(evaluacion);
        entityManager.flush();

        AsignacionAyuda asignacionAgua = new AsignacionAyuda(
                familia,
                agua,
                evaluacion,
                regla,
                null,
                OrigenAsignacionAyuda.REGLA,
                "asig-rf08-agua-modelo",
                new BigDecimal("2.500000"),
                "UNIDAD_TEST",
                ahora,
                "coord@sirab.local");
        entityManager.persist(asignacionAgua);
        entityManager.flush();

        EntregaAyuda entrega = new EntregaAyuda(
                familia,
                Necesidad.AGUA,
                asignacionAgua,
                new BigDecimal("2.000000"),
                "UNIDAD_TEST",
                ahora,
                ahora,
                "campo@sirab.local",
                "Entrega parcial de prueba");
        entityManager.persist(entrega);

        LoteInventario loteUno = new LoteInventario(
                agua, null, null, "UNIDAD_TEST", new BigDecimal("3.000000"),
                ahora, "Apertura de prueba", "coord@sirab.local");
        LoteInventario loteDos = new LoteInventario(
                agua, null, null, "UNIDAD_TEST", new BigDecimal("4.000000"),
                ahora, "Apertura de prueba", "coord@sirab.local");
        entityManager.persist(loteUno);
        entityManager.persist(loteDos);
        entityManager.flush();
        entityManager.persist(new MovimientoInventario(
                loteUno, null, TipoMovimientoInventario.APERTURA,
                new BigDecimal("3.000000"), ahora, "coord@sirab.local", "Conteo inicial de prueba"));
        entityManager.persist(new MovimientoInventario(
                loteDos, null, TipoMovimientoInventario.APERTURA,
                new BigDecimal("4.000000"), ahora, "coord@sirab.local", "Conteo inicial de prueba"));
        entityManager.persist(new MovimientoInventario(
                loteUno, entrega, TipoMovimientoInventario.SALIDA_ENTREGA,
                new BigDecimal("1.000000"), ahora, "campo@sirab.local", "Primera porción"));
        entityManager.persist(new MovimientoInventario(
                loteDos, entrega, TipoMovimientoInventario.SALIDA_ENTREGA,
                new BigDecimal("1.000000"), ahora, "campo@sirab.local", "Segunda porción"));
        loteUno.actualizarSaldoDisponible(new BigDecimal("2.000000"));
        loteDos.actualizarSaldoDisponible(new BigDecimal("3.000000"));

        DocumentoPrivado soporte = crearDocumento(
                "soporte-medico", TipoDocumentoPrivado.SOPORTE_MEDICO, ahora);
        DocumentoPrivado decisionMedica = crearDocumento(
                "decision-medica", TipoDocumentoPrivado.DECISION_SANITARIA, ahora);
        DocumentoPrivado fotografia = crearDocumento(
                "fotografia-vivienda", TipoDocumentoPrivado.EVIDENCIA_ALOJAMIENTO, ahora);
        DocumentoPrivado decisionEntidad = crearDocumento(
                "decision-entidad", TipoDocumentoPrivado.DECISION_ENTIDAD, ahora);
        DocumentoPrivado evidenciaPrestacion = crearDocumento(
                "prestacion-alojamiento", TipoDocumentoPrivado.CONFIRMACION_PRESTACION, ahora);
        entityManager.persist(soporte);
        entityManager.persist(decisionMedica);
        entityManager.persist(fotografia);
        entityManager.persist(decisionEntidad);
        entityManager.persist(evidenciaPrestacion);
        entityManager.flush();

        SolicitudAyudaMedica solicitudMedica = new SolicitudAyudaMedica(
                familia, ahora, "campo@sirab.local", "Solicitud de prueba");
        entityManager.persist(solicitudMedica);
        entityManager.flush();
        assertEquals(
                EstadoRevisionSanitaria.PENDIENTE_REVISION_SANITARIA,
                solicitudMedica.getEstadoRevision());
        entityManager.persist(new SoporteMedico(
                solicitudMedica,
                soporte,
                TipoSoporteMedico.FORMULA_MEDICA,
                ahora,
                "campo@sirab.local"));
        RevisionSanitariaExterna revision = new RevisionSanitariaExterna(
                solicitudMedica,
                "Institución de prueba",
                "Profesional de prueba",
                "ID-PROFESIONAL-TEST",
                DecisionAyuda.APROBADA,
                ahora,
                "REFERENCIA-TEST",
                decisionMedica,
                ahora,
                "campo@sirab.local",
                "Decisión externa de prueba");
        entityManager.persist(revision);
        solicitudMedica.setEstadoRevision(EstadoRevisionSanitaria.APROBADA_EXTERNAMENTE);
        entityManager.flush();
        AsignacionAyuda asignacionMedica = new AsignacionAyuda(
                familia,
                medicamento,
                null,
                null,
                revision,
                OrigenAsignacionAyuda.REVISION_SANITARIA,
                "asig-rf08-med-modelo",
                new BigDecimal("1.000000"),
                "UNIDAD_TEST",
                ahora,
                "campo@sirab.local");
        entityManager.persist(asignacionMedica);
        entityManager.flush();

        EntregaAyuda entregaMedicamento = new EntregaAyuda(
                familia,
                Necesidad.MEDICAMENTOS,
                asignacionMedica,
                new BigDecimal("3.000000"),
                "UNIDAD_TEST",
                ahora,
                ahora,
                "campo@sirab.local",
                "Entrega médica desde varios lotes");
        entityManager.persist(entregaMedicamento);
        entityManager.flush();

        LoteInventario loteMedicamentoUno = new LoteInventario(
                medicamento,
                "LOTE-MED-TEST-1",
                LocalDate.of(2028, 1, 1),
                "UNIDAD_TEST",
                new BigDecimal("4.000000"),
                ahora,
                "Recepción de prueba",
                "coord@sirab.local");
        LoteInventario loteMedicamentoDos = new LoteInventario(
                medicamento,
                "LOTE-MED-TEST-2",
                LocalDate.of(2028, 6, 1),
                "UNIDAD_TEST",
                new BigDecimal("5.000000"),
                ahora,
                "Recepción de prueba",
                "coord@sirab.local");
        entityManager.persist(loteMedicamentoUno);
        entityManager.persist(loteMedicamentoDos);
        entityManager.flush();
        entityManager.persist(new MovimientoInventario(
                loteMedicamentoUno,
                null,
                TipoMovimientoInventario.APERTURA,
                new BigDecimal("4.000000"),
                ahora,
                "coord@sirab.local",
                "Apertura de lote de prueba"));
        entityManager.persist(new MovimientoInventario(
                loteMedicamentoDos,
                null,
                TipoMovimientoInventario.APERTURA,
                new BigDecimal("5.000000"),
                ahora,
                "coord@sirab.local",
                "Apertura de lote de prueba"));
        entityManager.persist(new MovimientoInventario(
                loteMedicamentoUno,
                entregaMedicamento,
                TipoMovimientoInventario.SALIDA_ENTREGA,
                new BigDecimal("1.000000"),
                ahora,
                "campo@sirab.local",
                "Salida de primer lote"));
        entityManager.persist(new MovimientoInventario(
                loteMedicamentoDos,
                entregaMedicamento,
                TipoMovimientoInventario.SALIDA_ENTREGA,
                new BigDecimal("2.000000"),
                ahora,
                "campo@sirab.local",
                "Salida de segundo lote"));
        loteMedicamentoUno.actualizarSaldoDisponible(new BigDecimal("3.000000"));
        loteMedicamentoDos.actualizarSaldoDisponible(new BigDecimal("3.000000"));

        SolicitudAlojamiento solicitudAlojamiento = new SolicitudAlojamiento(
                familia, ahora, "campo@sirab.local", "Solicitud de alojamiento de prueba");
        entityManager.persist(solicitudAlojamiento);
        entityManager.flush();
        entityManager.persist(new VerificacionAlojamiento(
                solicitudAlojamiento,
                ResultadoVerificacionAlojamiento.VERIFICADA,
                fotografia,
                ahora,
                "campo@sirab.local",
                "Verificación de prueba"));
        entityManager.persist(new AprobacionProvisionalAlojamiento(
                solicitudAlojamiento,
                DecisionAyuda.APROBADA,
                ahora,
                "coord@sirab.local",
                "Aprobación provisional de prueba"));
        AprobacionDefinitivaAlojamiento aprobacionDefinitiva =
                new AprobacionDefinitivaAlojamiento(
                        solicitudAlojamiento,
                        "Entidad de prueba",
                        "REFERENCIA-ENTIDAD-TEST",
                        DecisionAyuda.APROBADA,
                        ahora,
                        decisionEntidad,
                        ahora,
                        "coord@sirab.local",
                        "Aprobación externa registrada");
        entityManager.persist(aprobacionDefinitiva);
        entityManager.flush();
        entityManager.persist(new ConfirmacionPrestacionAlojamiento(
                solicitudAlojamiento,
                aprobacionDefinitiva,
                ahora,
                ahora,
                "campo@sirab.local",
                evidenciaPrestacion,
                "Prestación efectiva de prueba"));
        entityManager.flush();
        assertEquals(
                0L,
                entityManager.createNativeQuery(
                                "select count(*) from vw_reconciliacion_inventario where diferencia <> 0")
                        .getSingleResult());

        Long evaluacionId = evaluacion.getId();
        Long entregaId = entrega.getId();
        Long entregaMedicamentoId = entregaMedicamento.getId();
        Long asignacionMedicaId = asignacionMedica.getId();
        Long solicitudMedicaId = solicitudMedica.getId();
        Long aprobacionId = aprobacionDefinitiva.getId();
        entityManager.clear();

        EvaluacionAsignacionAyuda evaluacionPersistida =
                entityManager.find(EvaluacionAsignacionAyuda.class, evaluacionId);
        assertEquals(Set.of(Necesidad.AGUA), evaluacionPersistida.getNecesidades());
        assertEquals(Set.of(SituacionFamiliar.MENORES_DE_CINCO), evaluacionPersistida.getSituaciones());

        EntregaAyuda entregaPersistida = entityManager.find(EntregaAyuda.class, entregaId);
        assertEquals(new BigDecimal("2.000000"), entregaPersistida.getCantidadValor());
        assertEquals(asignacionAgua.getId(), entregaPersistida.getAsignacion().getId());

        AsignacionAyuda asignacionMedicaPersistida =
                entityManager.find(AsignacionAyuda.class, asignacionMedicaId);
        assertEquals(
                DecisionAyuda.APROBADA,
                asignacionMedicaPersistida.getRevisionSanitaria().getDecision());
        assertEquals(
                EstadoRevisionSanitaria.APROBADA_EXTERNAMENTE,
                entityManager.find(SolicitudAyudaMedica.class, solicitudMedicaId).getEstadoRevision());

        EntregaAyuda entregaMedicamentoPersistida =
                entityManager.find(EntregaAyuda.class, entregaMedicamentoId);
        assertEquals(new BigDecimal("3.000000"), entregaMedicamentoPersistida.getCantidadValor());
        assertEquals(
                2L,
                entityManager.createQuery(
                                """
                                select count(m) from MovimientoInventario m
                                where m.entrega.id = :entregaId and m.tipo = :tipo
                                """,
                                Long.class)
                        .setParameter("entregaId", entregaMedicamentoId)
                        .setParameter("tipo", TipoMovimientoInventario.SALIDA_ENTREGA)
                        .getSingleResult());

        AprobacionDefinitivaAlojamiento aprobacionPersistida =
                entityManager.find(AprobacionDefinitivaAlojamiento.class, aprobacionId);
        assertNotNull(aprobacionPersistida.getDocumentoDecision());
        assertEquals(
                1L,
                entityManager.createQuery(
                                "select count(c) from ConfirmacionPrestacionAlojamiento c "
                                        + "where c.aprobacionDefinitiva.id = :aprobacionId",
                                Long.class)
                        .setParameter("aprobacionId", aprobacionId)
                        .getSingleResult());
    }

    @Test
    void rechazaEntregaTipadaSinTodosLosCamposRequeridos() {
        Familia familia = crearFamilia("ENTREGA_INCOMPLETA_RF08");
        entityManager.persist(familia);
        entityManager.flush();
        AsignacionAyuda asignacion = crearAsignacion(familia, "entrega-incompleta");

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(new EntregaAyuda(
                    familia,
                    Necesidad.AGUA,
                    asignacion,
                    BigDecimal.ONE,
                    "UNIDAD_TEST",
                    null,
                    Instant.parse("2026-03-01T15:00:00Z"),
                    "campo@sirab.local",
                    "Debe fallar por fecha ausente"));
            entityManager.flush();
        });
    }

    @Test
    void rechazaAsignacionQueMezclaFamiliaYEvaluacion() {
        Familia familiaEvaluada = crearFamilia("FAMILIA_EVALUADA_RF08");
        Familia otraFamilia = crearFamilia("OTRA_FAMILIA_RF08");
        entityManager.persist(familiaEvaluada);
        entityManager.persist(otraFamilia);
        entityManager.flush();

        ArticuloAyuda agua = crearArticuloAgua();
        entityManager.persist(agua);
        entityManager.flush();
        EvaluacionAsignacionAyuda evaluacion = crearEvaluacion(familiaEvaluada, "evaluacion-cruzada");
        ReglaAsignacionAyuda regla = crearRegla(agua);

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(new AsignacionAyuda(
                    otraFamilia,
                    agua,
                    evaluacion,
                    regla,
                    null,
                    OrigenAsignacionAyuda.REGLA,
                    "asignacion-cruzada",
                    BigDecimal.ONE,
                    "UNIDAD_TEST",
                    Instant.parse("2026-03-01T15:00:00Z"),
                    "coord@sirab.local"));
            entityManager.flush();
        });
    }

    @Test
    void rechazaEntregaVinculadaAAsignacionDeOtraFamilia() {
        Familia familiaAsignada = crearFamilia("FAMILIA_ASIGNADA_RF08");
        Familia otraFamilia = crearFamilia("OTRA_FAMILIA_ENTREGA_RF08");
        entityManager.persist(familiaAsignada);
        entityManager.persist(otraFamilia);
        entityManager.flush();
        AsignacionAyuda asignacion = crearAsignacion(familiaAsignada, "entrega-otra-familia");

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(new EntregaAyuda(
                    otraFamilia,
                    Necesidad.AGUA,
                    asignacion,
                    BigDecimal.ONE,
                    "UNIDAD_TEST",
                    Instant.parse("2026-03-01T15:00:00Z"),
                    Instant.parse("2026-03-01T15:00:00Z"),
                    "campo@sirab.local",
                    "Debe fallar por familia distinta"));
            entityManager.flush();
        });
    }

    @Test
    void rechazaMovimientoQueCombinaEntregaYLoteDeArticulosDistintos() {
        Familia familia = crearFamilia("MOVIMIENTO_CRUZADO_RF08");
        entityManager.persist(familia);
        entityManager.flush();
        AsignacionAyuda asignacion = crearAsignacion(familia, "movimiento-cruzado");
        Instant ahora = Instant.parse("2026-03-01T15:00:00Z");
        EntregaAyuda entrega = new EntregaAyuda(
                familia,
                Necesidad.AGUA,
                asignacion,
                BigDecimal.ONE,
                "UNIDAD_TEST",
                ahora,
                ahora,
                "campo@sirab.local",
                "Entrega de prueba");
        entityManager.persist(entrega);
        entityManager.flush();

        ArticuloAyuda otroArticulo = crearArticuloAgua();
        entityManager.persist(otroArticulo);
        entityManager.flush();
        LoteInventario lote = new LoteInventario(
                otroArticulo,
                null,
                null,
                "UNIDAD_TEST",
                BigDecimal.ONE,
                ahora,
                "Inventario de prueba",
                "coord@sirab.local");
        entityManager.persist(lote);
        entityManager.flush();
        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(new MovimientoInventario(
                    lote,
                    entrega,
                    TipoMovimientoInventario.SALIDA_ENTREGA,
                    BigDecimal.ONE,
                    ahora,
                    "campo@sirab.local",
                    "Debe fallar por artículo distinto"));
            entityManager.flush();
        });
    }

    @Test
    void rechazaConfirmacionConAprobacionDeOtraSolicitud() {
        Familia familia = crearFamilia("ALOJAMIENTO_RF08");
        entityManager.persist(familia);
        entityManager.flush();
        SolicitudAlojamiento solicitudConfirmada = new SolicitudAlojamiento(
                familia, Instant.now(), "campo@sirab.local", "Solicitud confirmada");
        SolicitudAlojamiento solicitudDeAprobacion = new SolicitudAlojamiento(
                familia, Instant.now(), "campo@sirab.local", "Solicitud aprobada");
        entityManager.persist(solicitudConfirmada);
        entityManager.persist(solicitudDeAprobacion);
        entityManager.flush();
        AprobacionDefinitivaAlojamiento aprobacion = new AprobacionDefinitivaAlojamiento(
                solicitudDeAprobacion,
                "Entidad de prueba",
                null,
                DecisionAyuda.APROBADA,
                Instant.now(),
                null,
                Instant.now(),
                "coord@sirab.local",
                "Aprobación de otra solicitud");
        entityManager.persist(aprobacion);
        entityManager.flush();

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(new ConfirmacionPrestacionAlojamiento(
                    solicitudConfirmada,
                    aprobacion,
                    Instant.now(),
                    Instant.now(),
                    "campo@sirab.local",
                    null,
                    "Debe fallar por solicitud distinta"));
            entityManager.flush();
        });
    }

    private AsignacionAyuda crearAsignacion(Familia familia, String sufijo) {
        ArticuloAyuda agua = crearArticuloAgua();
        entityManager.persist(agua);
        entityManager.flush();
        EvaluacionAsignacionAyuda evaluacion = crearEvaluacion(familia, "evaluacion-" + sufijo);
        ReglaAsignacionAyuda regla = crearRegla(agua);
        AsignacionAyuda asignacion = new AsignacionAyuda(
                familia,
                agua,
                evaluacion,
                regla,
                null,
                OrigenAsignacionAyuda.REGLA,
                "asignacion-" + sufijo,
                BigDecimal.ONE,
                "UNIDAD_TEST",
                Instant.parse("2026-03-01T15:00:00Z"),
                "coord@sirab.local");
        entityManager.persist(asignacion);
        entityManager.flush();
        return asignacion;
    }

    private ArticuloAyuda crearArticuloAgua() {
        return new ArticuloAyuda(
                "AGUA_" + java.util.UUID.randomUUID(),
                "Artículo de agua",
                Necesidad.AGUA,
                "UNIDAD_TEST",
                true,
                Instant.parse("2026-03-01T15:00:00Z"),
                "coord@sirab.local");
    }

    private EvaluacionAsignacionAyuda crearEvaluacion(Familia familia, String clave) {
        EvaluacionAsignacionAyuda evaluacion = new EvaluacionAsignacionAyuda(
                familia,
                1,
                clave,
                Instant.parse("2026-03-01T15:00:00Z"),
                "coord@sirab.local",
                Set.of(Necesidad.AGUA),
                Set.of(SituacionFamiliar.MENORES_DE_CINCO));
        entityManager.persist(evaluacion);
        entityManager.flush();
        return evaluacion;
    }

    private ReglaAsignacionAyuda crearRegla(ArticuloAyuda agua) {
        ReglaAsignacionAyuda regla = new ReglaAsignacionAyuda(
                agua,
                1,
                TipoCriterioAsignacion.SITUACION_FAMILIAR,
                SituacionFamiliar.MENORES_DE_CINCO,
                BigDecimal.ONE,
                "UNIDAD_TEST",
                LocalDate.of(2026, 1, 1),
                null,
                Instant.parse("2026-03-01T15:00:00Z"),
                "coord@sirab.local");
        entityManager.persist(regla);
        entityManager.flush();
        return regla;
    }

    private Familia crearFamilia(String nombre) {
        Familia familia = new Familia();
        familia.setNombreRegistrante(nombre);
        familia.setDireccionReferencia("REFERENCIA_TEST");
        familia.setNumeroIntegrantes(1);
        familia.setConsentimientoAt(Instant.parse("2026-01-01T00:00:00Z"));
        familia.setNecesidades(Set.of(Necesidad.AGUA, Necesidad.MEDICAMENTOS, Necesidad.ALOJAMIENTO));
        familia.setSituaciones(Set.of(SituacionFamiliar.MENORES_DE_CINCO));
        return familia;
    }

    private DocumentoPrivado crearDocumento(
            String clave,
            TipoDocumentoPrivado clasificacion,
            Instant creadoAt) {
        return new DocumentoPrivado(
                clave,
                clave + ".pdf",
                "application/pdf",
                100,
                "a".repeat(64),
                clasificacion,
                creadoAt,
                "campo@sirab.local");
    }
}
