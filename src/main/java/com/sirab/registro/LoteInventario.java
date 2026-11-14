package com.sirab.registro;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
        name = "lote_inventario",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_lote_inventario_codigo",
                    columnNames = {"articulo_ayuda_id", "codigo_lote"}),
            @UniqueConstraint(
                    name = "uk_lote_id_articulo_tipo",
                    columnNames = {"id", "articulo_ayuda_id", "tipo_ayuda"})
        })
public class LoteInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "articulo_ayuda_id", nullable = false)
    private ArticuloAyuda articulo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ayuda", nullable = false, length = 30)
    private Necesidad tipoAyuda;

    @Column(name = "codigo_lote", length = 100)
    private String codigoLote;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(nullable = false, length = 40)
    private String unidad;

    @Column(name = "saldo_disponible", nullable = false, precision = 18, scale = 6)
    private BigDecimal saldoDisponible;

    @Column(name = "recibido_at", nullable = false)
    private Instant recibidoAt;

    @Column(nullable = false, length = 200)
    private String origen;

    @Column(name = "registrado_por", nullable = false, length = 254)
    private String registradoPor;

    @Version
    @Column(nullable = false)
    private long version;

    protected LoteInventario() {
    }

    public LoteInventario(
            ArticuloAyuda articulo,
            String codigoLote,
            LocalDate fechaVencimiento,
            String unidad,
            BigDecimal saldoDisponible,
            Instant recibidoAt,
            String origen,
            String registradoPor) {
        this.articulo = articulo;
        this.tipoAyuda = articulo.getTipoAyuda();
        this.codigoLote = codigoLote;
        this.fechaVencimiento = fechaVencimiento;
        this.unidad = unidad;
        this.saldoDisponible = saldoDisponible;
        this.recibidoAt = recibidoAt;
        this.origen = origen;
        this.registradoPor = registradoPor;
    }

    public Long getId() {
        return id;
    }

    public ArticuloAyuda getArticulo() {
        return articulo;
    }

    public Necesidad getTipoAyuda() {
        return tipoAyuda;
    }

    public String getCodigoLote() {
        return codigoLote;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public String getUnidad() {
        return unidad;
    }

    public BigDecimal getSaldoDisponible() {
        return saldoDisponible;
    }

    public void actualizarSaldoDisponible(BigDecimal saldoDisponible) {
        this.saldoDisponible = saldoDisponible;
    }

    public Instant getRecibidoAt() {
        return recibidoAt;
    }

    public String getOrigen() {
        return origen;
    }

    public String getRegistradoPor() {
        return registradoPor;
    }

    public long getVersion() {
        return version;
    }
}
