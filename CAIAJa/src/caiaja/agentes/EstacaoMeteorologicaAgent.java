/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.agentes;

import caiaja.model.Aeroporto;
import caiaja.model.Controlador;
import caiaja.model.EstacaoMeteorologica;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import java.util.ArrayList;

/**
 *
 * @author fosa
 */
public class EstacaoMeteorologicaAgent extends Agent {

    EstacaoMeteorologica estacao;

    protected void setup() {
        Object[] args = getArguments();

        estacao = new EstacaoMeteorologica();

        if (args != null) {
            if (args.length > 0) {
                String[] strargs = ((String) args[0]).split("&");
            }
        }

//        estacao.setAeroporto(aero);
        System.out.println("Estacao se Em Funcionamento. ");

        DFAgentDescription dfd = new DFAgentDescription();

        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();

        sd.setType("EstacaoMeteorologica");
        sd.setName("EstacaoMeteorologica");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

//        addBehaviour(new ConsultarClima(this, 5000));
    }

    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("Estacao se desligando.");

    }

    /**
     * Tarefas Executadas por este Agente
     */
    public static class ConsultarClima extends TickerBehaviour {

        public ConsultarClima(Agent a, long period) {
            super(a, period);
        }

        public void init(int porta) {
        }

        @Override
        protected void onTick() {
//            DFAgentDescription template = new DFAgentDescription();
//            ServiceDescription sd = new ServiceDescription();
//            sd.setType("EstacaoMeteorologica");
//            template.addServices(sd);
//            try {
//                DFAgentDescription[] result = DFService.search(myAgent, template);
////                    System.out.println("Procurando outros veiculos:");
////                    OutrosVeiculos = new AID[result.length];
//                ArrayList<AID> OutrosVeiculos = new ArrayList<AID>();
//                for (int i = 0; i < result.length; ++i) {
//                    System.out.println("int i = " + i);
////                    if (!result[i].getName().getName().equals(getName())) {
////                        OutrosVeiculos.add(result[i].getName());
////                        OutrosVeiculos[i] = result[i].getName();
////                            System.out.println("Encontrado: " + result[i].getName());
////                    }
//                }
//            } catch (FIPAException fe) {
//                fe.printStackTrace();
//            }
        }
    }
}
