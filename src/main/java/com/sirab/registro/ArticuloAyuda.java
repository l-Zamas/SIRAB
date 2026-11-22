package com.sirab.registro;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "articulo_ayuda",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_articulo_ayuda_codigo",
                    columnNames = "codigo"),
            @UniqueConstraint(
                    name = "uk_articulo_ayuda_id_tipo",
                    columnNames = {"id", "tipo_ayuda"})
        })
public class ArticuloAyuda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ayuda", nullable = false, length = 30)
    private Necesidad tipoAyuda;

    @Column(nullable = false, length = 40)
    private String unidad;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "creado_at", nullable = false)
    private Instant creadoAt;

    @Column(name = "creado_por", nullable = false, length = 254)
    private String creadoPor;

    protected ArticuloAyuda() {
    }

    public ArticuloAyuda(
            String codigo,
            String nombre,
            Necesidad tipoAyuda,
            String unidad,
            boolean activo,
            Instant creadoAt,
            String creadoPor) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipoAyuda = tipoAyuda;
        this.unidad = unidad;
        this.activo = activo;
        this.creadoAt = creadoAt;
        this.creadoPor = creadoPor;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public Necesidad getTipoAyuda() {
        return tipoAyuda;
    }

    public String getUnidad() {
        return unidad;
    }

    public boolean isActivo() {
        return activo;
    }

    public Instant getCreadoAt() {
        return creadoAt;
    }

    public String getCreadoPor() {
        return creadoPor;
    }

    public void desactivar() {
        this.activo = false;
    }

    public void activar() {
        this.activo = true;
    }

    public void actualizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del artículo es obligatorio.");
        }
        this.nombre = nombre.trim();
    }
}
