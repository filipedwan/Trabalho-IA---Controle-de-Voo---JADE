/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.ontologia.acoes;

import caiaja.model.Aviao;
import caiaja.model.Piloto;
import jade.content.Concept;
import jade.core.AID;

/**
 *
 * @author fosa
 */
public class Decolar implements Concept {

    private Aviao _aviao;
    private Piloto _piloto;
    private AID _actor;

    public void setPiloto(Piloto piloto) {
        _piloto = piloto;
    }

    public Piloto getPiloto() {
        return _piloto;
    }

    public void setAviao(Aviao company) {
        _aviao = company;
    }

    public Aviao getAviao() {
        return _aviao;
    }

}
