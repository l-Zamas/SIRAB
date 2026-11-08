package com.sirab.registro.duplicados;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sirab.registro.Familia;

class CoincidenciaFamiliaTests {

    @Test
    void detectaDosSenalesTrasNormalizarNombreTelefonoYUbicacion() {
        Familia primera = familia("María Pérez", "300 123 4567", "Centro", "Calle 1 # 2-3");
        Familia segunda = familia("MARIA PEREZ", "+57 (300) 123-4567", "centro", "calle 1 2 3");

        CoincidenciaFamilia coincidencia = CoincidenciaFamilia.comparar(primera, segunda);

        assertTrue(coincidencia.esPosibleDuplicado());
        assertEquals(Set.of(
                CoincidenciaFamilia.SenalCoincidencia.NOMBRE,
                CoincidenciaFamilia.SenalCoincidencia.TELEFONO,
                CoincidenciaFamilia.SenalCoincidencia.UBICACION), Set.copyOf(coincidencia.senales()));
    }

    @Test
    void unaSolaSenalNoSeConsideraPosibleDuplicado() {
        Familia primera = familia("María Pérez", "300 123 4567", "Centro", "Calle 1 # 2-3");
        Familia segunda = familia("María Pérez", "310 765 4321", "Otro barrio", "Otra dirección");

        assertFalse(CoincidenciaFamilia.comparar(primera, segunda).esPosibleDuplicado());
    }

    @Test
    void ignoraTelefonoIncompletoYUbicacionIncompleta() {
        Familia primera = familia("María Pérez", "123", "Centro", null);
        Familia segunda = familia("María Pérez", "123", "Centro", null);

        CoincidenciaFamilia coincidencia = CoincidenciaFamilia.comparar(primera, segunda);

        assertEquals(Set.of(CoincidenciaFamilia.SenalCoincidencia.NOMBRE),
                Set.copyOf(coincidencia.senales()));
        assertFalse(coincidencia.esPosibleDuplicado());
    }

    @Test
    void noEliminaElPrefijo57DeNumerosDeNueveDigitos() {
        Familia primera = familia("Ana Pérez", "571234567", "Centro", "Calle 1");
        Familia segunda = familia("Ana Perez", "1234567", "Otro", "Calle 9");

        CoincidenciaFamilia coincidencia = CoincidenciaFamilia.comparar(primera, segunda);

        assertEquals(Set.of(CoincidenciaFamilia.SenalCoincidencia.NOMBRE),
                Set.copyOf(coincidencia.senales()));
        assertFalse(coincidencia.esPosibleDuplicado());
        assertEquals("3001234567", CoincidenciaFamilia.normalizarTelefono("+57 300 123 4567"));
        assertEquals("571234567", CoincidenciaFamilia.normalizarTelefono("571234567"));
    }

    @Test
    void soloRetiraElPrefijo57CuandoSeIndicaFormatoInternacional() {
        assertEquals("3001234567", CoincidenciaFamilia.normalizarTelefono("+57 300 123 4567"));
        assertEquals("3001234567", CoincidenciaFamilia.normalizarTelefono("0057 300 123 4567"));
        assertEquals("573001234567", CoincidenciaFamilia.normalizarTelefono("573001234567"));
        assertEquals("1573001234567", CoincidenciaFamilia.normalizarTelefono("+1 573001234567"));
    }

    @Test
    void normalizaNombresUnicodeYConservaLaEnieComoLetraDistinta() {
        Familia conEnie = familia("Peña", "3001234567", "Centro", "Calle 1");
        Familia conN = familia("Pena", "3117654321", "Norte", "Calle 9");
        Familia conEnieMayuscula = familia("PEÑA", "3221234567", "Sur", "Calle 8");
        Familia nombreChino = familia("张三", "3001112233", "Centro", "Calle 1");
        Familia nombreChinoIgual = familia("张三", "3111112233", "Norte", "Calle 9");
        Familia nombreGriego = familia("Γιώργος", "3002223344", "Centro", "Calle 1");
        Familia nombreGriegoIgual = familia("Γιώργος", "3112223344", "Norte", "Calle 9");

        assertFalse(CoincidenciaFamilia.comparar(conEnie, conN).esPosibleDuplicado());
        assertEquals(Set.of(CoincidenciaFamilia.SenalCoincidencia.NOMBRE),
                Set.copyOf(CoincidenciaFamilia.comparar(conEnie, conEnieMayuscula).senales()));
        assertEquals(Set.of(CoincidenciaFamilia.SenalCoincidencia.NOMBRE),
                Set.copyOf(CoincidenciaFamilia.comparar(nombreChino, nombreChinoIgual).senales()));
        assertEquals(Set.of(CoincidenciaFamilia.SenalCoincidencia.NOMBRE),
                Set.copyOf(CoincidenciaFamilia.comparar(nombreGriego, nombreGriegoIgual).senales()));
    }

    private Familia familia(String nombre, String telefono, String barrio, String direccion) {
        Familia familia = new Familia();
        familia.setNombreRegistrante(nombre);
        familia.setTelefonoContacto(telefono);
        familia.setBarrio(barrio);
        familia.setDireccion(direccion);
        return familia;
    }
}
