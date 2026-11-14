package com.sirab.registro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

class Rf08MigracionMySqlAisladaTests {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}:]+)(?::([^}]*))?}");

    @Test
    @EnabledIfSystemProperty(named = "sirab.mysql.integration", matches = "true")
    void migraDesdeV10YConservaEntregasHistoricasEnEsquemaTemporal() throws Exception {
        Properties properties = loadDatabaseProperties();
        String baseUrl = required(properties, "spring.datasource.url");
        String username = required(properties, "spring.datasource.username");
        String password = required(properties, "spring.datasource.password");
        String schema = "sirab_rf08_" + UUID.randomUUID().toString().replace("-", "");
        String authority = authority(baseUrl);
        String options = options(baseUrl);
        String serverUrl = "jdbc:mysql://" + authority + "/" + options;
        String isolatedUrl = "jdbc:mysql://" + authority + "/" + schema + options;
        boolean created = false;

        try {
            try (Connection connection = DriverManager.getConnection(serverUrl, username, password);
                    Statement statement = connection.createStatement()) {
                statement.execute(
                        "CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                created = true;
            }

            Flyway.configure()
                    .dataSource(isolatedUrl, username, password)
                    .locations("classpath:db/migration")
                    .target(MigrationVersion.fromVersion("10"))
                    .load()
                    .migrate();

            long familyId;
            try (Connection connection = DriverManager.getConnection(isolatedUrl, username, password);
                    PreparedStatement statement = connection.prepareStatement("""
                            INSERT INTO familia (
                                nombre_registrante,
                                direccion_referencia,
                                numero_integrantes,
                                consentimiento_at,
                                consentimiento_version
                            ) VALUES (?, ?, ?, ?, ?)
                            """, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, "FAMILIA_HISTORICA_RF08");
                statement.setString(2, "REFERENCIA_HISTORICA");
                statement.setInt(3, 1);
                statement.setTimestamp(4, java.sql.Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")));
                statement.setString(5, "test");
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    familyId = keys.getLong(1);
                }

                try (PreparedStatement entrega = connection.prepareStatement("""
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
                        """)) {
                    entrega.setLong(1, familyId);
                    entrega.setString(2, "AGUA");
                    entrega.setString(3, "20 L");
                    entrega.setDate(4, java.sql.Date.valueOf("2026-02-01"));
                    entrega.setTimestamp(5, java.sql.Timestamp.from(Instant.parse("2026-02-01T18:00:00Z")));
                    entrega.setString(6, "campo@sirab.local");
                    entrega.setBoolean(7, false);
                    entrega.setString(8, "Entrega histórica");
                    entrega.executeUpdate();
                }
            }

            Flyway.configure()
                    .dataSource(isolatedUrl, username, password)
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            try (Connection connection = DriverManager.getConnection(isolatedUrl, username, password);
                    Statement statement = connection.createStatement();
                    ResultSet entrega = statement.executeQuery("""
                            SELECT cantidad, fecha_entrega, asignacion_ayuda_id,
                                   cantidad_valor, unidad, fecha_hora_entrega
                            FROM entrega_ayuda
                            WHERE familia_id = %d
                            """.formatted(familyId))) {
                entrega.next();
                assertEquals("20 L", entrega.getString("cantidad"));
                assertEquals(java.sql.Date.valueOf("2026-02-01"), entrega.getDate("fecha_entrega"));
                assertNull(entrega.getObject("asignacion_ayuda_id"));
                assertNull(entrega.getObject("cantidad_valor"));
                assertNull(entrega.getString("unidad"));
                assertNull(entrega.getTimestamp("fecha_hora_entrega"));
            }
        } finally {
            if (created) {
                try (Connection connection = DriverManager.getConnection(serverUrl, username, password);
                        Statement statement = connection.createStatement()) {
                    statement.execute("DROP DATABASE `" + schema + "`");
                    try (PreparedStatement verification = connection.prepareStatement(
                                    "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?")) {
                        verification.setString(1, schema);
                        try (ResultSet result = verification.executeQuery()) {
                            result.next();
                            assertEquals(0, result.getInt(1));
                        }
                    }
                }
            }
        }
    }

    private Properties loadDatabaseProperties() throws Exception {
        Properties properties = new Properties();
        load(properties, "/application.properties");
        load(properties, "/application-local.properties");
        return properties;
    }

    private void load(Properties properties, String resource) throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(resource)) {
            if (stream != null) {
                properties.load(stream);
            }
        }
    }

    private String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value != null) {
            Matcher matcher = PLACEHOLDER.matcher(value);
            StringBuffer resolved = new StringBuffer();
            while (matcher.find()) {
                String replacement = System.getProperty(matcher.group(1));
                if (replacement == null) {
                    replacement = System.getenv(matcher.group(1));
                }
                if (replacement == null) {
                    replacement = matcher.group(2);
                }
                if (replacement == null) {
                    throw new IllegalStateException(
                            "Falta configurar " + matcher.group(1) + " para la prueba MySQL aislada.");
                }
                matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(resolved);
            value = resolved.toString();
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta configurar " + key + " para la prueba MySQL aislada.");
        }
        return value;
    }

    private String authority(String url) {
        int authorityStart = url.indexOf("://") + 3;
        int pathStart = url.indexOf('/', authorityStart);
        int queryStart = url.indexOf('?', authorityStart);
        int authorityEnd = pathStart < 0 ? (queryStart < 0 ? url.length() : queryStart) : pathStart;
        return url.substring(authorityStart, authorityEnd);
    }

    private String options(String url) {
        int queryStart = url.indexOf('?');
        return queryStart < 0 ? "" : url.substring(queryStart);
    }
}
