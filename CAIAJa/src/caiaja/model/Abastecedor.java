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
 * @author Laian
 * Modelo de Abastecedor, para o agente responsável por este manipalar
 */
public class Abastecedor extends Pessoa implements Serializable, Concept {
    private int Capacidade;

    public int getCapacidade() {
        return Capacidade;
    }

    public void setCapacidade(int Capacidade) {
        this.Capacidade = Capacidade;
    }    
}
