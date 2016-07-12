/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.model;

import jade.content.Concept;
import java.io.Serializable;

/**
 *
 * @author fosa
 * Modelo de Combustivel, para o agente responsável por este manipalar
 */
public class Combustivel implements Concept, Serializable {

    int quantidade;

    public Combustivel(int quantidade) {
        this.quantidade = quantidade;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

}
