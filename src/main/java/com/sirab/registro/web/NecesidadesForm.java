package com.sirab.registro.web;

import java.util.Set;

import com.sirab.registro.Necesidad;

public class NecesidadesForm {

    private Set<Necesidad> necesidades = Set.of();

    public Set<Necesidad> getNecesidades() {
        return necesidades;
    }

    public void setNecesidades(Set<Necesidad> necesidades) {
        this.necesidades = necesidades == null ? Set.of() : Set.copyOf(necesidades);
    }
}
