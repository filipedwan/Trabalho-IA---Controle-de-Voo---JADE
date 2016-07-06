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
 */
public class Aviao implements Serializable, Concept {

    final int PROPULSAO_NENHUMA = 0;
    final int PROPULSAO_PISTAO = 1;
    final int PROPULSAO_JATO = 2;
    final int PROPULSAO_TURBOHELICE = 3;
    final int PROPULSAO_ELETRICO = 4;

    private int pistaMinima;
    private String Prefixo;
    private int Propulsao; //Tipo de propulsao
    private int Motores; //Numero de motores da aeronave

    public Aviao(String Prefix) {
        Motores = 1;
        Propulsao = PROPULSAO_PISTAO;
        Prefixo = Prefix;
        pistaMinima = 500;
    }

    public int getPistaMinima() {
        return pistaMinima;
    }

    public void setPistaMinima(int pistaMinima) {
        this.pistaMinima = pistaMinima;
    }

    public String getPrefixo() {
        return Prefixo;
    }

    public void setPrefixo(String Prefixo) {
        this.Prefixo = Prefixo;
    }

    public int getPropulsao() {
        return Propulsao;
    }

    public void setPropulsao(int Propulsao) {
        this.Propulsao = Propulsao;
    }

    public int getMotores() {
        return Motores;
    }

    public void setMotores(int Motores) {
        this.Motores = Motores;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.Prefixo);
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
        final Aviao other = (Aviao) obj;
        if (!Objects.equals(this.Prefixo, other.Prefixo)) {
            return false;
        }
        return true;
    }

}
