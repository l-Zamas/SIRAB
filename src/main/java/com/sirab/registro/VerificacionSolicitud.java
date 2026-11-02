package com.sirab.registro;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
@Table(name = "verificacion_solicitud")
public class VerificacionSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "familia_id", nullable = false)
    private Familia familia;

    @Column(name = "usuario_correo", nullable = false, length = 254)
    private String usuarioCorreo;

    @Column(name = "verificada_at", nullable = false)
    private Instant verificadaAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, length = 20)
    private ResultadoVerificacion resultado;

    @Column(length = 1000)
    private String observacion;

    @ElementCollection
    @CollectionTable(
            name = "cambio_dato_verificacion",
            joinColumns = @JoinColumn(name = "verificacion_id"))
    private List<CambioDatoVerificacion> cambios = new ArrayList<>();

    protected VerificacionSolicitud() {
    }

    public VerificacionSolicitud(
            Familia familia,
            String usuarioCorreo,
            Instant verificadaAt,
            ResultadoVerificacion resultado,
            String observacion,
            List<CambioDatoVerificacion> cambios) {
        this.familia = familia;
        this.usuarioCorreo = usuarioCorreo;
        this.verificadaAt = verificadaAt;
        this.resultado = resultado;
        this.observacion = observacion;
        this.cambios = new ArrayList<>(cambios);
    }

    public Long getId() {
        return id;
    }

    public String getUsuarioCorreo() {
        return usuarioCorreo;
    }

    public Instant getVerificadaAt() {
        return verificadaAt;
    }

    public ResultadoVerificacion getResultado() {
        return resultado;
    }

    public String getObservacion() {
        return observacion;
    }

    public List<CambioDatoVerificacion> getCambios() {
        return List.copyOf(cambios);
    }
}
