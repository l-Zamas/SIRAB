package com.sirab.registro;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CambioDatoVerificacion {

    @Column(name = "campo", nullable = false, length = 50)
    private String campo;

    @Column(name = "valor_anterior", length = 2000)
    private String valorAnterior;

    @Column(name = "valor_nuevo", length = 2000)
    private String valorNuevo;

    protected CambioDatoVerificacion() {
    }

    public CambioDatoVerificacion(String campo, String valorAnterior, String valorNuevo) {
        this.campo = campo;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
    }

    public String getCampo() {
        return campo;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public String getValorNuevo() {
        return valorNuevo;
    }
}
