package com.sirab.registro.web;

public class SituacionForm {

    private boolean menoresDeCinco;
    private boolean adultosMayores;
    private boolean personasConDiscapacidad;
    private boolean mujeresGestantes;

    public boolean isMenoresDeCinco() {
        return menoresDeCinco;
    }

    public void setMenoresDeCinco(boolean menoresDeCinco) {
        this.menoresDeCinco = menoresDeCinco;
    }

    public boolean isAdultosMayores() {
        return adultosMayores;
    }

    public void setAdultosMayores(boolean adultosMayores) {
        this.adultosMayores = adultosMayores;
    }

    public boolean isPersonasConDiscapacidad() {
        return personasConDiscapacidad;
    }

    public void setPersonasConDiscapacidad(boolean personasConDiscapacidad) {
        this.personasConDiscapacidad = personasConDiscapacidad;
    }

    public boolean isMujeresGestantes() {
        return mujeresGestantes;
    }

    public void setMujeresGestantes(boolean mujeresGestantes) {
        this.mujeresGestantes = mujeresGestantes;
    }

    void clearValues() {
        menoresDeCinco = false;
        adultosMayores = false;
        personasConDiscapacidad = false;
        mujeresGestantes = false;
    }
}
