package com.sirab.registro.web;

import java.time.LocalDate;

import com.sirab.registro.Necesidad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class EntregaAyudaForm {

    @NotNull(message = "Selecciona la necesidad atendida.")
    private Necesidad tipoAyuda;

    @NotBlank(message = "Indica la cantidad entregada.")
    @Size(max = 100, message = "La cantidad no puede superar 100 caracteres.")
    private String cantidad;

    @NotNull(message = "Indica la fecha de entrega.")
    private LocalDate fechaEntrega = LocalDate.now();

    private boolean satisfaceNecesidadCompleta;

    @Size(max = 1000, message = "La observación no puede superar 1000 caracteres.")
    private String observacion;

    public Necesidad getTipoAyuda() {
        return tipoAyuda;
    }

    public void setTipoAyuda(Necesidad tipoAyuda) {
        this.tipoAyuda = tipoAyuda;
    }

    public String getCantidad() {
        return cantidad;
    }

    public void setCantidad(String cantidad) {
        this.cantidad = cantidad;
    }

    public LocalDate getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDate fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public boolean isSatisfaceNecesidadCompleta() {
        return satisfaceNecesidadCompleta;
    }

    public void setSatisfaceNecesidadCompleta(boolean satisfaceNecesidadCompleta) {
        this.satisfaceNecesidadCompleta = satisfaceNecesidadCompleta;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
