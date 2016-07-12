/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.model;

import jade.content.Concept;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 *
 * @author fosa
 * Modelo de Pista, para o agente responsável por este manipalar
 */
public class Piloto extends Pessoa implements Serializable, Concept {

    int horasDeVoo;
    Date pilotoDesde;

    public int getHorasDeVoo() {
        return horasDeVoo;
    }

    public void setHorasDeVoo(int horasDeVoo) {
        this.horasDeVoo = horasDeVoo;
    }

    public Date getPilotoDesde() {
        return pilotoDesde;
    }

    public void setPilotoDesde(Date pilotoDesde) {
        this.pilotoDesde = pilotoDesde;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + this.horasDeVoo;
        hash = 67 * hash + Objects.hashCode(this.pilotoDesde);
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
        final Piloto other = (Piloto) obj;
        if (this.horasDeVoo != other.horasDeVoo) {
            return false;
        }
        if (!Objects.equals(this.pilotoDesde, other.pilotoDesde)) {
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
