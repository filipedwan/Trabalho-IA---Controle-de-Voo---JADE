/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.agentes;

import caiaja.model.Aeroporto;
import jade.core.Agent;

/**
 *
 * @author fosa
 */
public class AeroportoAgent extends Agent {
    Aeroporto aeroporto;
    
    protected void setup() {
        Object[] args = getArguments();

        Aeroporto aero = new Aeroporto();
        aero.setPrefixo("SBBV");
        aero.setNome("Atlas Brasil Catanhede");
        if (args != null) {
            if (args.length > 0) {
                String[] strargs = ((String) args[0]).split("&");
                if (strargs.length > 0) {
                    aeroporto.setPrefixo(strargs[0]);
                }
                if (strargs.length > 1) {
                    aeroporto.setNome(strargs[1]);
                }
            }
        }

    }

    protected void takeDown() {
        System.out.println("Controlador " + aeroporto.getNome() + " saindo de operação.");
    }

}
