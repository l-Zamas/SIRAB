package com.sirab.registro;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "historial_necesidad")
public class HistorialNecesidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "seguimiento_id", nullable = false) private SeguimientoNecesidad seguimiento;
    @Enumerated(EnumType.STRING) @Column(name = "estado_anterior", length = 30) private EstadoSeguimientoNecesidad estadoAnterior;
    @Enumerated(EnumType.STRING) @Column(name = "estado_nuevo", nullable = false, length = 30) private EstadoSeguimientoNecesidad estadoNuevo;
    @Column(name = "usuario_correo", nullable = false, length = 254) private String usuarioCorreo;
    @Enumerated(EnumType.STRING) @Column(name = "rol_usuario", nullable = false, length = 30) private com.sirab.seguridad.entity.RolUsuario rolUsuario;
    @Column(name = "cantidad_entregada") private Integer cantidadEntregada;
    @Column(length = 1000) private String observacion;
    @Column(name = "fecha_hora", nullable = false) private Instant fechaHora;
    protected HistorialNecesidad() { }
    public HistorialNecesidad(SeguimientoNecesidad seguimiento, EstadoSeguimientoNecesidad anterior, EstadoSeguimientoNecesidad nuevo, String correo, com.sirab.seguridad.entity.RolUsuario rol, BigDecimal cantidad, String observacion, Instant fechaHora) {
        this.seguimiento = seguimiento; this.estadoAnterior = anterior; this.estadoNuevo = nuevo; this.usuarioCorreo = correo; this.rolUsuario = rol;
        this.cantidadEntregada = cantidad == null ? null : cantidad.intValueExact();
        this.observacion = observacion; this.fechaHora = fechaHora;
    }
    public Long getId() { return id; }
    public SeguimientoNecesidad getSeguimiento() { return seguimiento; }
    public EstadoSeguimientoNecesidad getEstadoAnterior() { return estadoAnterior; }
    public EstadoSeguimientoNecesidad getEstadoNuevo() { return estadoNuevo; }
    public String getUsuarioCorreo() { return usuarioCorreo; }
    public com.sirab.seguridad.entity.RolUsuario getRolUsuario() { return rolUsuario; }
    public Integer getCantidadEntregada() { return cantidadEntregada; }
    public String getObservacion() { return observacion; }
    public Instant getFechaHora() { return fechaHora; }
}
