/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.model;

/**
 *
 * @author fosa
 */
public class Aviao {
    
    final int PROPULSAO_NENHUMA = 0;
    final int PROPULSAO_PISTAO = 1;
    final int PROPULSAO_JATO = 2;
    final int PROPULSAO_TURBOHELICE = 3;
    final int PROPULSAO_ELETRICO = 4;
    
    int pistaMinima;
    String Prefixo;
    int Propulsao; //Tipo de propulsao
    int Motores; //Numero de motores da aeronave
}
