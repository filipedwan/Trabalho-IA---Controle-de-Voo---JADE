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
 * Modelo de Pessoa, o qual todos os modelos trabalhadores irao herdar
 */
public class Pessoa implements Serializable, Concept {

    String Nome;
    Date Nascimento;

    public String getNome() {
        return Nome;
    }

    public void setNome(String Nome) {
        this.Nome = Nome;
    }

    public Date getNascimento() {
        return Nascimento;
    }

    public void setNascimento(Date Nascimento) {
        this.Nascimento = Nascimento;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + Objects.hashCode(this.Nome);
        hash = 37 * hash + Objects.hashCode(this.Nascimento);
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
        final Pessoa other = (Pessoa) obj;
        if (!Objects.equals(this.Nome, other.Nome)) {
            return false;
        }
        if (!Objects.equals(this.Nascimento, other.Nascimento)) {
            return false;
        }
        return true;
    }

}
