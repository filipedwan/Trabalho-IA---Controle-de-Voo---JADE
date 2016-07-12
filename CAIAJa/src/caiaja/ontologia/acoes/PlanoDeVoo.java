/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.ontologia.acoes;

import caiaja.model.Aeroporto;
import caiaja.model.Aviao;
import caiaja.model.Piloto;
import jade.content.Concept;
import jade.core.AID;
import java.io.Serializable;

/**
 *
 * @author fosa
 * Plano de voo com o itinerário do piloto
 */
public class PlanoDeVoo implements Concept, Serializable {

    private Aviao _aviao;
    private Piloto _piloto;
    private Aeroporto _aeroportoOrigem;
    private Aeroporto _aeroportoDestino;
    private AID _actor;
    private String _idplano;

    public PlanoDeVoo(Aviao _aviao, Piloto _piloto, Aeroporto _aeroportoOrigem, Aeroporto _aeroportoDestino, AID _actor, String _idplano) {
        this._aviao = _aviao;
        this._piloto = _piloto;
        this._aeroportoOrigem = _aeroportoOrigem;
        this._aeroportoDestino = _aeroportoDestino;
        this._actor = _actor;
        this._idplano = _idplano;

        _aviao.setAceleracaoMotor(0);
    }

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

    public Aeroporto getAeroportoOrigem() {
        return _aeroportoOrigem;
    }

    public void setAeroportoOrigem(Aeroporto _aeroportoOrigem) {
        this._aeroportoOrigem = _aeroportoOrigem;
    }

    public Aeroporto getAeroportoDestino() {
        return _aeroportoDestino;
    }

    public void setAeroportoDestino(Aeroporto _aeroportoDestino) {
        this._aeroportoDestino = _aeroportoDestino;
    }

    public AID getActor() {
        return _actor;
    }

    public void setActor(AID _actor) {
        this._actor = _actor;
    }

    public String getIdplano() {
        return _idplano;
    }

    public void setIdplano(String _idplano) {
        this._idplano = _idplano;
    }

}
