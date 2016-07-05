/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.model;

import java.io.Serializable;

/**
 *
 * @author fosa
 */
public class Controlador extends Pessoa implements Serializable{

    Aeroporto Aeroporto;

    public Controlador() {
        super();
    }

    public Aeroporto getAeroporto() {
        return Aeroporto;
    }

    public void setAeroporto(Aeroporto Aeroporto) {
        this.Aeroporto = Aeroporto;
    }

}
