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
 */
public class Decolar implements Concept, Serializable {

    private Aviao _aviao;
    private Piloto _piloto;
    private Aeroporto _aeroporto;
    private AID _actor;
    private String _replyWith;

    public Decolar(Aviao _aviao, Piloto _piloto, Aeroporto _aeroporto, AID _actor, String _replyWith) {
        this._aviao = _aviao;
        this._piloto = _piloto;
        this._aeroporto = _aeroporto;
        this._actor = _actor;
        this._replyWith = _replyWith;

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

    public Aeroporto getAeroporto() {
        return _aeroporto;
    }

    public void setAeroporto(Aeroporto aeroporto) {
        this._aeroporto = aeroporto;
    }

    public AID getActor() {
        return _actor;
    }

    public void setActor(AID _actor) {
        this._actor = _actor;
    }

    public String getReplyWith() {
        return _replyWith;
    }

    public void setReplyWith(String _replyWith) {
        this._replyWith = _replyWith;
    }

}
