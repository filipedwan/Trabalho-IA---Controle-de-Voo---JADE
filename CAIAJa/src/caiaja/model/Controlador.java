/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.model;

import jade.content.Concept;
import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author fosa
 * Modelo de Controlador, para o agente responsável por este manipalar
 */
public class Controlador extends Pessoa implements Serializable, Concept {

    Aeroporto Aeroporto;

    public Controlador() {
        super();
    }

    public Aeroporto getAeroporto() {
        return Aeroporto;
    }

    public void setAeroporto(Aeroporto Aeroporto) {
        this.Aeroporto = Aeroporto;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + Objects.hashCode(this.Aeroporto);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Controlador other = (Controlador) obj;
        if (!Objects.equals(this.Aeroporto, other.Aeroporto)) {
            return false;
        }
        if (!Objects.equals(this.Nome, other.Nome)) {
            return false;
        }
        if (!Objects.equals(this.Nascimento, other.Nascimento)) {
            return false;
        }
        return true;
    }

}
