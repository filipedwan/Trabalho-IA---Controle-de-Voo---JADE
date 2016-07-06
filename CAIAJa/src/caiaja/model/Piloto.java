/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.model;

import jade.content.Concept;
import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author fosa
 */
public class Piloto extends Pessoa implements Serializable, Concept {

    int horasDeVoo;
    Date pilotoDesde;

}
