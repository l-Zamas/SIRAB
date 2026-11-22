package com.sirab.registro.service;

import java.math.BigDecimal;

import com.sirab.registro.SituacionFamiliar;
import com.sirab.registro.TipoCriterioAsignacion;

public record CriterioReglaCommand(
        TipoCriterioAsignacion tipoCriterio,
        SituacionFamiliar situacion,
        BigDecimal cantidad) {

    public static CriterioReglaCommand base(BigDecimal cantidad) {
        return new CriterioReglaCommand(TipoCriterioAsignacion.BASE, null, cantidad);
    }

    public static CriterioReglaCommand situacionFamiliar(SituacionFamiliar situacion, BigDecimal cantidad) {
        return new CriterioReglaCommand(TipoCriterioAsignacion.SITUACION_FAMILIAR, situacion, cantidad);
    }
}
