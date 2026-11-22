package com.sirab.registro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sirab.registro.ArticuloAyuda;
import com.sirab.registro.ArticuloAyudaRepository;
import com.sirab.registro.Necesidad;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogoArticuloServiceTests {

    @Autowired
    private CatalogoArticuloService catalogoService;

    @Autowired
    private ArticuloAyudaRepository articuloRepository;

    @Test
    void registraArticuloValidoExitosamente() {
        RegistrarArticuloCommand command = new RegistrarArticuloCommand(
                "KIT-ALIM-TEST-1",
                "Kit de Alimentos Básico",
                Necesidad.ALIMENTOS,
                "KITS",
                "admin@sirab.local");

        ArticuloAyuda articulo = catalogoService.registrarArticulo(command);

        assertNotNull(articulo.getId());
        assertEquals("KIT-ALIM-TEST-1", articulo.getCodigo());
        assertEquals("Kit de Alimentos Básico", articulo.getNombre());
        assertEquals(Necesidad.ALIMENTOS, articulo.getTipoAyuda());
        assertEquals("KITS", articulo.getUnidad());
        assertTrue(articulo.isActivo());
        assertEquals("admin@sirab.local", articulo.getCreadoPor());
        assertNotNull(articulo.getCreadoAt());
    }

    @Test
    void rechazaArticuloConTipoAlojamiento() {
        RegistrarArticuloCommand command = new RegistrarArticuloCommand(
                "ALOJ-TEST",
                "Alojamiento temporal",
                Necesidad.ALOJAMIENTO,
                "DIAS",
                "admin@sirab.local");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> catalogoService.registrarArticulo(command));

        assertTrue(ex.getMessage().contains("ALOJAMIENTO no se gestiona como artículo de catálogo"));
    }

    @Test
    void rechazaCodigoDuplicado() {
        catalogoService.registrarArticulo(new RegistrarArticuloCommand(
                "AGUA-TEST-DUP",
                "Agua 5L",
                Necesidad.AGUA,
                "LITROS",
                "admin@sirab.local"));

        RegistrarArticuloCommand duplicado = new RegistrarArticuloCommand(
                "AGUA-TEST-DUP",
                "Agua 5L Otra",
                Necesidad.AGUA,
                "LITROS",
                "admin@sirab.local");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> catalogoService.registrarArticulo(duplicado));

        assertTrue(ex.getMessage().contains("Ya existe un artículo registrado con el código"));
    }

    @Test
    void rechazaCamposObligatoriosNulosOBancos() {
        assertThrows(IllegalArgumentException.class, () -> catalogoService.registrarArticulo(
                new RegistrarArticuloCommand("", "Nombre", Necesidad.AGUA, "LITROS", "coord@sirab.local")));

        assertThrows(IllegalArgumentException.class, () -> catalogoService.registrarArticulo(
                new RegistrarArticuloCommand("COD-1", "", Necesidad.AGUA, "LITROS", "coord@sirab.local")));

        assertThrows(IllegalArgumentException.class, () -> catalogoService.registrarArticulo(
                new RegistrarArticuloCommand("COD-1", "Nombre", null, "LITROS", "coord@sirab.local")));

        assertThrows(IllegalArgumentException.class, () -> catalogoService.registrarArticulo(
                new RegistrarArticuloCommand("COD-1", "Nombre", Necesidad.AGUA, "", "coord@sirab.local")));

        assertThrows(IllegalArgumentException.class, () -> catalogoService.registrarArticulo(
                new RegistrarArticuloCommand("COD-1", "Nombre", Necesidad.AGUA, "LITROS", "  ")));
    }

    @Test
    void rechazaCamposQueSuperanLongitudMaxima() {
        String codigoLargo = "A".repeat(61);
        String nombreLargo = "B".repeat(121);
        String unidadLarga = "C".repeat(41);

        assertThrows(IllegalArgumentException.class, () -> catalogoService.registrarArticulo(
                new RegistrarArticuloCommand(codigoLargo, "Nombre", Necesidad.AGUA, "LITROS", "coord@sirab.local")));

        assertThrows(IllegalArgumentException.class, () -> catalogoService.registrarArticulo(
                new RegistrarArticuloCommand("COD-LEN", nombreLargo, Necesidad.AGUA, "LITROS", "coord@sirab.local")));

        assertThrows(IllegalArgumentException.class, () -> catalogoService.registrarArticulo(
                new RegistrarArticuloCommand("COD-LEN2", "Nombre", Necesidad.AGUA, unidadLarga, "coord@sirab.local")));
    }

    @Test
    void activaYDesactivaArticulo() {
        ArticuloAyuda articulo = catalogoService.registrarArticulo(new RegistrarArticuloCommand(
                "MED-TEST-ACT",
                "Suero Oral",
                Necesidad.MEDICAMENTOS,
                "UNIDADES",
                "admin@sirab.local"));

        assertTrue(articulo.isActivo());

        ArticuloAyuda desactivado = catalogoService.desactivar(articulo.getId(), "admin@sirab.local");
        assertFalse(desactivado.isActivo());

        ArticuloAyuda reactivado = catalogoService.activar(articulo.getId(), "admin@sirab.local");
        assertTrue(reactivado.isActivo());
    }

    @Test
    void actualizaNombreArticulo() {
        ArticuloAyuda articulo = catalogoService.registrarArticulo(new RegistrarArticuloCommand(
                "KIT-ALIM-RENOM",
                "Kit Inicial",
                Necesidad.ALIMENTOS,
                "KITS",
                "admin@sirab.local"));

        ArticuloAyuda actualizado = catalogoService.actualizarNombre(
                articulo.getId(), "Kit Familiar Reforzado", "admin@sirab.local");

        assertEquals("Kit Familiar Reforzado", actualizado.getNombre());
    }

    @Test
    void listaArticulosPorFiltrosYEstado() {
        catalogoService.registrarArticulo(new RegistrarArticuloCommand(
                "ART-LISTA-AGUA-1", "Agua Activa", Necesidad.AGUA, "LITROS", "admin@sirab.local"));
        ArticuloAyuda inactivo = catalogoService.registrarArticulo(new RegistrarArticuloCommand(
                "ART-LISTA-AGUA-2", "Agua Inactiva", Necesidad.AGUA, "LITROS", "admin@sirab.local"));
        catalogoService.desactivar(inactivo.getId(), "admin@sirab.local");

        catalogoService.registrarArticulo(new RegistrarArticuloCommand(
                "ART-LISTA-MED-1", "Medicamento 1", Necesidad.MEDICAMENTOS, "CAJAS", "admin@sirab.local"));

        List<ArticuloAyuda> activos = catalogoService.listarActivos();
        assertTrue(activos.stream().anyMatch(a -> a.getCodigo().equals("ART-LISTA-AGUA-1")));
        assertFalse(activos.stream().anyMatch(a -> a.getCodigo().equals("ART-LISTA-AGUA-2")));

        List<ArticuloAyuda> aguasActivas = catalogoService.listarActivosPorTipo(Necesidad.AGUA);
        assertTrue(aguasActivas.stream().allMatch(a -> a.getTipoAyuda() == Necesidad.AGUA && a.isActivo()));
        assertTrue(aguasActivas.stream().anyMatch(a -> a.getCodigo().equals("ART-LISTA-AGUA-1")));

        List<ArticuloAyuda> todasAguas = catalogoService.listarPorTipo(Necesidad.AGUA);
        assertTrue(todasAguas.stream().anyMatch(a -> a.getCodigo().equals("ART-LISTA-AGUA-2")));
    }

    @Test
    void buscarPorCodigoYPorId() {
        ArticuloAyuda creado = catalogoService.registrarArticulo(new RegistrarArticuloCommand(
                "BUSQUEDA-COD", "Prueba Búsqueda", Necesidad.AGUA, "LITROS", "admin@sirab.local"));

        Optional<ArticuloAyuda> porId = catalogoService.obtenerPorId(creado.getId());
        assertTrue(porId.isPresent());
        assertEquals("BUSQUEDA-COD", porId.get().getCodigo());

        Optional<ArticuloAyuda> porCodigo = catalogoService.obtenerPorCodigo("BUSQUEDA-COD");
        assertTrue(porCodigo.isPresent());
        assertEquals(creado.getId(), porCodigo.get().getId());

        assertTrue(catalogoService.obtenerPorCodigo("NO-EXISTE").isEmpty());
        assertTrue(catalogoService.obtenerPorCodigo("").isEmpty());
    }
}
