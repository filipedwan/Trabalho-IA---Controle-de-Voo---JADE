/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.agentes;

import caiaja.model.Aeroporto;
import caiaja.model.Controlador;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

/**
 *
 * @author fosa
 */
public class ControladorAgent extends Agent {

    Controlador controlador;

    protected void setup() {
        Object[] args = getArguments();

        controlador = new Controlador();
        controlador.setNome("Fulano de Tal");
        Aeroporto aero = new Aeroporto();
        aero.setPrefixo("SBBV");
        aero.setNome("Atlas Brasil Catanhede");
        if (args != null) {
            if (args.length > 0) {
                String[] strargs = ((String) args[0]).split("&");
                if (strargs.length > 0) {
                    controlador.setNome(strargs[0]);
                }
                if (strargs.length > 1) {
                    aero.setNome(strargs[1]);
                }
            }
        }

        controlador.setAeroporto(aero);

        System.out.println("Controlador " + controlador.getNome() + " operando em " + controlador.getAeroporto().getNome());

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Controlador");
        sd.setName(controlador.getNome());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("Controlador " + controlador.getNome() + " saindo de operação.");
    }

    private class Contato extends Behaviour {

        @Override
        public void action() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean done() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }
}
