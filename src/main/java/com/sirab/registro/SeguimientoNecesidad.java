package com.sirab.registro;

import java.math.BigDecimal;
import java.time.Instant;

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
import com.sirab.seguridad.entity.RolUsuario;

@Entity
@Table(name = "seguimiento_necesidad", uniqueConstraints = @UniqueConstraint(name = "uk_seguimiento_familia_nec", columnNames = {"familia_id", "necesidad"}))
public class SeguimientoNecesidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "familia_id", nullable = false)
    private Familia familia;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Necesidad necesidad;
    @Column(nullable = false)
    private boolean vigente = true;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private EstadoSeguimientoNecesidad estado = EstadoSeguimientoNecesidad.DECLARADA;
    @Column(name = "cantidad_solicitada")
    private Integer cantidadSolicitada;
    @Column(name = "cantidad_entregada", nullable = false)
    private Integer cantidadEntregada = 0;
    @Enumerated(EnumType.STRING)
    @Column(name = "rol_responsable", length = 30)
    private RolUsuario responsableRol;
    @Column(name = "atendido_por", length = 254)
    private String atendidoPor;
    @Column(length = 1000)
    private String observacion;
    @Column(name = "evidencia_ref", length = 500)
    private String evidenciaRef;
    @Column(name = "creado_at", nullable = false)
    private Instant creadoAt;
    @Column(name = "actualizado_at", nullable = false)
    private Instant actualizadoAt;

    protected SeguimientoNecesidad() { }
    public SeguimientoNecesidad(Familia familia, Necesidad necesidad, EstadoSeguimientoNecesidad estado, Instant ahora) {
        this.familia = familia; this.necesidad = necesidad; this.estado = estado; this.creadoAt = ahora; this.actualizadoAt = ahora;
    }
    public Long getId() { return id; }
    public Familia getFamilia() { return familia; }
    public Necesidad getNecesidad() { return necesidad; }
    public boolean isVigente() { return vigente; }
    public void setVigente(boolean vigente) { this.vigente = vigente; }
    public EstadoSeguimientoNecesidad getEstado() { return estado; }
    public void setEstado(EstadoSeguimientoNecesidad estado) { this.estado = estado; }
    public Integer getCantidadSolicitada() { return cantidadSolicitada; }
    public void setCantidadSolicitada(Integer value) { this.cantidadSolicitada = value; }
    public Integer getCantidadEntregada() { return cantidadEntregada; }
    public void setCantidadEntregada(Integer value) { this.cantidadEntregada = value; }
    public RolUsuario getResponsableRol() { return responsableRol; }
    public void setResponsableRol(RolUsuario value) { this.responsableRol = value; }
    public String getAtendidoPor() { return atendidoPor; }
    public void setAtendidoPor(String value) { this.atendidoPor = value; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String value) { this.observacion = value; }
    public String getEvidenciaRef() { return evidenciaRef; }
    public void setEvidenciaRef(String value) { this.evidenciaRef = value; }
    public Instant getCreadoAt() { return creadoAt; }
    public Instant getActualizadoAt() { return actualizadoAt; }
    public void actualizarAt(Instant value) { this.actualizadoAt = value; }
}
