package com.sirab.registro.duplicados;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.sirab.registro.Familia;

public record CoincidenciaFamilia(List<SenalCoincidencia> senales) {

    public CoincidenciaFamilia {
        senales = List.copyOf(senales);
    }

    public boolean esPosibleDuplicado() {
        return senales.size() >= 2;
    }

    public static CoincidenciaFamilia comparar(Familia primera, Familia segunda) {
        List<SenalCoincidencia> senales = new ArrayList<>();
        if (coincide(normalizarTexto(primera.getNombreRegistrante()),
                normalizarTexto(segunda.getNombreRegistrante()))) {
            senales.add(SenalCoincidencia.NOMBRE);
        }

        String telefonoPrimero = normalizarTelefono(primera.getTelefonoContacto());
        String telefonoSegundo = normalizarTelefono(segunda.getTelefonoContacto());
        if (telefonoPrimero != null && telefonoPrimero.equals(telefonoSegundo)) {
            senales.add(SenalCoincidencia.TELEFONO);
        }

        String ubicacionPrimera = normalizarUbicacion(primera);
        String ubicacionSegunda = normalizarUbicacion(segunda);
        if (ubicacionPrimera != null && ubicacionPrimera.equals(ubicacionSegunda)) {
            senales.add(SenalCoincidencia.UBICACION);
        }

        return new CoincidenciaFamilia(senales);
    }

    public static boolean cambiaronDatosComparacion(
            Familia antes,
            String nombreRegistrante,
            String telefonoContacto,
            String barrio,
            String direccion) {
        return !normalizarTexto(antes.getNombreRegistrante())
                        .equals(normalizarTexto(nombreRegistrante))
                || !Objects.equals(normalizarTelefono(antes.getTelefonoContacto()),
                        normalizarTelefono(telefonoContacto))
                || !Objects.equals(normalizarUbicacion(antes),
                        normalizarUbicacion(barrio, direccion));
    }

    static String normalizarTexto(String valor) {
        if (valor == null || valor.isBlank()) {
            return "";
        }
        String descompuesto = Normalizer.normalize(valor.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replace("n\u0303", "ñ");
        StringBuilder normalizado = new StringBuilder(descompuesto.length());
        boolean separadorPendiente = false;
        Character.UnicodeScript ultimaLetra = null;
        for (int indice = 0; indice < descompuesto.length();) {
            int puntoCodigo = descompuesto.codePointAt(indice);
            indice += Character.charCount(puntoCodigo);
            if (Character.isLetterOrDigit(puntoCodigo)) {
                if (separadorPendiente && !normalizado.isEmpty()) {
                    normalizado.append(' ');
                }
                normalizado.appendCodePoint(puntoCodigo);
                ultimaLetra = Character.UnicodeScript.of(puntoCodigo);
                separadorPendiente = false;
            } else if (esMarcaCombinante(puntoCodigo) && ultimaLetra != null) {
                if (ultimaLetra != Character.UnicodeScript.LATIN) {
                    normalizado.appendCodePoint(puntoCodigo);
                }
            } else {
                separadorPendiente = true;
                ultimaLetra = null;
            }
        }
        return Normalizer.normalize(normalizado, Normalizer.Form.NFC);
    }

    static String normalizarTelefono(String valor) {
        if (valor == null) {
            return null;
        }
        StringBuilder soloDigitos = new StringBuilder();
        valor.codePoints()
                .filter(Character::isDigit)
                .map(puntoCodigo -> Character.digit(puntoCodigo, 10))
                .filter(digito -> digito >= 0)
                .forEach(soloDigitos::append);
        String digitos = soloDigitos.toString();
        String recortado = valor.stripLeading();
        int largoPrefijoInternacional = recortado.startsWith("+") && digitos.startsWith("57")
                ? 2
                : recortado.startsWith("00") && digitos.startsWith("0057") ? 4 : 0;
        if (largoPrefijoInternacional > 0
                && digitos.length() >= largoPrefijoInternacional + 7
                && digitos.length() <= largoPrefijoInternacional + 15) {
            digitos = digitos.substring(largoPrefijoInternacional);
        }
        return digitos.length() >= 7 && digitos.length() <= 15 ? digitos : null;
    }

    static String normalizarUbicacion(Familia familia) {
        return normalizarUbicacion(familia.getBarrio(), familia.getDireccion());
    }

    static String normalizarUbicacion(String valorBarrio, String valorDireccion) {
        String barrio = normalizarTexto(valorBarrio);
        String direccion = normalizarTexto(valorDireccion);
        if (barrio.isEmpty() || direccion.isEmpty()) {
            return null;
        }
        return barrio + "|" + direccion;
    }

    private static boolean esMarcaCombinante(int puntoCodigo) {
        int tipo = Character.getType(puntoCodigo);
        return tipo == Character.NON_SPACING_MARK
                || tipo == Character.COMBINING_SPACING_MARK
                || tipo == Character.ENCLOSING_MARK;
    }

    private static boolean coincide(String primero, String segundo) {
        return !primero.isEmpty() && primero.equals(segundo);
    }

    public enum SenalCoincidencia {
        NOMBRE,
        TELEFONO,
        UBICACION
    }
}
