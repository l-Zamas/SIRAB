package com.sirab.registro;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.Immutable;

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

@Entity
@Immutable
@Table(name = "movimiento_inventario")
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_inventario_id", nullable = false)
    private LoteInventario lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrega_ayuda_id")
    private EntregaAyuda entrega;

    @Column(name = "articulo_ayuda_id", nullable = false)
    private Long articuloAyudaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ayuda", nullable = false, length = 30)
    private Necesidad tipoAyuda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoMovimientoInventario tipo;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal cantidad;

    @Column(name = "registrado_at", nullable = false)
    private Instant registradoAt;

    @Column(name = "usuario_correo", nullable = false, length = 254)
    private String usuarioCorreo;

    @Column(length = 1000)
    private String observacion;

    protected MovimientoInventario() {
    }

    public MovimientoInventario(
            LoteInventario lote,
            EntregaAyuda entrega,
            TipoMovimientoInventario tipo,
            BigDecimal cantidad,
            Instant registradoAt,
            String usuarioCorreo,
            String observacion) {
        this.lote = lote;
        this.entrega = entrega;
        this.articuloAyudaId = lote.getArticulo().getId();
        this.tipoAyuda = lote.getTipoAyuda();
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.registradoAt = registradoAt;
        this.usuarioCorreo = usuarioCorreo;
        this.observacion = observacion;
    }

    public Long getId() {
        return id;
    }

    public LoteInventario getLote() {
        return lote;
    }

    public EntregaAyuda getEntrega() {
        return entrega;
    }

    public TipoMovimientoInventario getTipo() {
        return tipo;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public Instant getRegistradoAt() {
        return registradoAt;
    }

    public String getUsuarioCorreo() {
        return usuarioCorreo;
    }

    public String getObservacion() {
        return observacion;
    }
}
