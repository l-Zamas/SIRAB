package com.sirab.registro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class Rf08MigracionHistoricaTests {

    @Test
    void conservaEntregasExistentesAlAplicarLasMigracionesNuevas() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(
                "jdbc:h2:mem:rf08_history_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("10"))
                .load()
                .migrate();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update(
                """
                INSERT INTO familia (
                    nombre_registrante,
                    direccion_referencia,
                    numero_integrantes,
                    consentimiento_at,
                    consentimiento_version
                ) VALUES (?, ?, ?, ?, ?)
                """,
                "FAMILIA_HISTORICA_RF08",
                "REFERENCIA_HISTORICA",
                1,
                Instant.parse("2026-01-01T00:00:00Z"),
                "test");
        Long familiaId = jdbc.queryForObject(
                "SELECT id FROM familia WHERE nombre_registrante = ?",
                Long.class,
                "FAMILIA_HISTORICA_RF08");

        jdbc.update(
                """
                INSERT INTO entrega_ayuda (
                    familia_id,
                    tipo_ayuda,
                    cantidad,
                    fecha_entrega,
                    registrada_at,
                    usuario_correo,
                    satisface_necesidad_completa,
                    observacion
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                familiaId,
                "AGUA",
                "20 L",
                java.sql.Date.valueOf("2026-02-01"),
                java.sql.Timestamp.from(Instant.parse("2026-02-01T18:00:00Z")),
                "campo@sirab.local",
                false,
                "Entrega histórica");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        Map<String, Object> entrega = jdbc.queryForMap(
                "SELECT * FROM entrega_ayuda WHERE familia_id = ?",
                familiaId);
        assertEquals("20 L", entrega.get("CANTIDAD"));
        assertEquals(java.sql.Date.valueOf("2026-02-01"), entrega.get("FECHA_ENTREGA"));
        assertEquals("campo@sirab.local", entrega.get("USUARIO_CORREO"));
        assertNull(entrega.get("ASIGNACION_AYUDA_ID"));
        assertNull(entrega.get("CANTIDAD_VALOR"));
        assertNull(entrega.get("UNIDAD"));
        assertNull(entrega.get("FECHA_HORA_ENTREGA"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM asignacion_ayuda", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM movimiento_inventario", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM lote_inventario", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM articulo_ayuda", Integer.class));

    }
}
