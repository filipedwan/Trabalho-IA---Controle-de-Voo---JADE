/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.model;

import jade.content.Concept;
import jade.util.leap.ArrayList;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author fosa
 */
public class Aeroporto implements Serializable, Concept {

    String Nome;
    String Prefixo;
    List<Pista> Pistas;
    List<Patio> Patio;
    EstacaoMeteorologica estacaoMeteorologica;
    Controlador controlador;
    List<Aviao> avioes;

    public Aeroporto() {
        Pistas = new java.util.ArrayList<>();
        controlador = null;
        estacaoMeteorologica = null;
        avioes = new java.util.ArrayList<>();
    }

    void addAviao(Aviao aviao) {
        avioes.add(aviao);
    }

    Aviao pegaAviao(int i) {
        if (i <= avioes.size()) {
            return avioes.get(i);
        }
        return null;
    }

    public String getNome() {
        return Nome;
    }

    public void setNome(String Nome) {
        this.Nome = Nome;
    }

    public String getPrefixo() {
        return Prefixo;
    }

    public void setPrefixo(String Prefixo) {
        this.Prefixo = Prefixo;
    }

    public List<Pista> getPistas() {
        return Pistas;
    }

    public void setPistas(List<Pista> Pistas) {
        this.Pistas = Pistas;
    }

    public EstacaoMeteorologica getEstacaoMeteorologica() {
        return estacaoMeteorologica;
    }

    public void setEstacaoMeteorologica(EstacaoMeteorologica estacaoMeteorologica) {
        this.estacaoMeteorologica = estacaoMeteorologica;
    }

    public List<Patio> getPatio() {
        return Patio;
    }

    public void setPatio(List<Patio> Patio) {
        this.Patio = Patio;
    }

    public void addPista(Pista Pista) {
        this.Pistas.add(Pista);
    }

    public Controlador getControlador() {
        return controlador;
    }

    public void setControlador(Controlador controlador) {
        this.controlador = controlador;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.Nome);
        hash = 67 * hash + Objects.hashCode(this.Prefixo);
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
        final Aeroporto other = (Aeroporto) obj;
        if (!Objects.equals(this.Nome, other.Nome)) {
            return false;
        }
        if (!Objects.equals(this.Prefixo, other.Prefixo)) {
            return false;
        }
        return true;
    }

}
