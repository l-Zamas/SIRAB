package com.sirab.registro.duplicados;

import java.time.Instant;
import java.util.Objects;

import com.sirab.registro.Familia;

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

@Entity
@Table(
        name = "posible_duplicado",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_posible_duplicado_familias",
                columnNames = {"familia_menor_id", "familia_mayor_id"}))
public class PosibleDuplicado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "familia_menor_id", nullable = false)
    private Familia familiaMenor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "familia_mayor_id", nullable = false)
    private Familia familiaMayor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoPosibleDuplicado estado;

    @Column(name = "senales_coincidentes", nullable = false, length = 100)
    private String senalesCoincidentes;

    @Column(name = "detectado_at", nullable = false)
    private Instant detectadoAt;

    @Column(name = "actualizado_at", nullable = false)
    private Instant actualizadoAt;

    protected PosibleDuplicado() {
    }

    public PosibleDuplicado(
            Familia primera,
            Familia segunda,
            CoincidenciaFamilia coincidencia,
            Instant detectadoAt) {
        if (primera.getId() == null || segunda.getId() == null
                || Objects.equals(primera.getId(), segunda.getId())) {
            throw new IllegalArgumentException("La coincidencia requiere dos familias persistidas distintas.");
        }
        this.familiaMenor = primera.getId() < segunda.getId() ? primera : segunda;
        this.familiaMayor = primera.getId() < segunda.getId() ? segunda : primera;
        this.estado = EstadoPosibleDuplicado.PENDIENTE_REVISION;
        this.senalesCoincidentes = serializar(coincidencia);
        this.detectadoAt = detectadoAt;
        this.actualizadoAt = detectadoAt;
    }

    public void actualizarCoincidencia(CoincidenciaFamilia coincidencia, Instant ahora) {
        this.senalesCoincidentes = serializar(coincidencia);
        this.actualizadoAt = ahora;
    }

    public void reabrir(CoincidenciaFamilia coincidencia, Instant ahora) {
        actualizarCoincidencia(coincidencia, ahora);
        this.estado = EstadoPosibleDuplicado.PENDIENTE_REVISION;
    }

    public void resolver(EstadoPosibleDuplicado resultado, Instant ahora) {
        if (estado != EstadoPosibleDuplicado.PENDIENTE_REVISION
                || (resultado != EstadoPosibleDuplicado.DUPLICADO_CONFIRMADO
                        && resultado != EstadoPosibleDuplicado.DESCARTADO)) {
            throw new IllegalStateException("La coincidencia ya fue resuelta o el resultado no es válido.");
        }
        this.estado = resultado;
        this.actualizadoAt = ahora;
    }

    public void retirar(Instant ahora) {
        if (estado == EstadoPosibleDuplicado.PENDIENTE_REVISION
                || estado == EstadoPosibleDuplicado.DUPLICADO_CONFIRMADO) {
            this.estado = EstadoPosibleDuplicado.SIN_COINCIDENCIA;
            this.actualizadoAt = ahora;
        }
    }

    private String serializar(CoincidenciaFamilia coincidencia) {
        return coincidencia.senales().stream()
                .map(Enum::name)
                .sorted()
                .reduce((primera, segunda) -> primera + "," + segunda)
                .orElseThrow(() -> new IllegalArgumentException("La coincidencia debe incluir señales."));
    }

    public Long getId() {
        return id;
    }

    public Familia getFamiliaMenor() {
        return familiaMenor;
    }

    public Familia getFamiliaMayor() {
        return familiaMayor;
    }

    public EstadoPosibleDuplicado getEstado() {
        return estado;
    }

    public String getSenalesCoincidentes() {
        return senalesCoincidentes;
    }

    public Instant getDetectadoAt() {
        return detectadoAt;
    }

    public Instant getActualizadoAt() {
        return actualizadoAt;
    }
}
