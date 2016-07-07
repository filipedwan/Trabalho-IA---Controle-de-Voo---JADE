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
 */
public class EstacaoMeteorologica implements Serializable, Concept {
    
    private float taxaNeblina; //--------------------------------------------------------
    private float taxaFumaca;  // porcentagens de 0 a 100
    private float taxaAves;    // obs: se pelo menos um animal na pista, nao pode decolar
    private float taxaAnimais; //--------------------------------------------------------

    public float getTaxaNeblina() {
        return taxaNeblina;
    }

    public void setTaxaNeblina(float taxaNeblina) {
        this.taxaNeblina = taxaNeblina;
    }

    public float getTaxaFumaca() {
        return taxaFumaca;
    }

    public void setTaxaFumaca(float taxaFumaca) {
        this.taxaFumaca = taxaFumaca;
    }

    public float getTaxaAves() {
        return taxaAves;
    }

    public void setTaxaAves(float taxaAves) {
        this.taxaAves = taxaAves;
    }

    public float getTaxaAnimais() {
        return taxaAnimais;
    }

    public void setTaxaAnimais(float taxaAnimais) {
        this.taxaAnimais = taxaAnimais;
    }
}
