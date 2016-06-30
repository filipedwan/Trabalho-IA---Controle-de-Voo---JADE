/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.agentes;

import caiaja.model.Aeroporto;
import caiaja.model.Controlador;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author fosa
 */
public class ControladorAgent extends Agent {

    Controlador controlador;
    List<AID> estacoesMeteorologicas;

    protected void setup() {
        Object[] args = getArguments();

        estacoesMeteorologicas = new ArrayList<AID>();

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

        addBehaviour(new ConsultarClima(this, 5000));
    }

    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("Controlador " + controlador.getNome() + " saindo de operação.");
    }

    private class ConsultaClima extends Behaviour {

        @Override
        public void action() {
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

            for (AID estacao : estacoesMeteorologicas) {
                System.out.println(getName() + ": Sai do meio que eu to passando " + estacao.getName());
                cfp.addReceiver(estacao);
            }

            cfp.setConversationId("vo-passar");
            cfp.setReplyWith("cfp" + System.currentTimeMillis());
            cfp.setContent("Como está o tempo?");
            myAgent.send(cfp);

        }

        @Override
        public boolean done() {
            if (estacoesMeteorologicas.size() < 0) {
                return true;
            }
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            return true;
        }

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

    /**
     * Tarefas Executadas por este Agente
     */
    public class ConsultarClima extends TickerBehaviour {

        public ConsultarClima(Agent a, long period) {
            super(a, period);
        }

        public void init(int porta) {
        }

        @Override
        protected void onTick() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("EstacaoMeteorologica");
            template.addServices(sd);

            estacoesMeteorologicas.clear();
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                for (int i = 0; i < result.length; ++i) {
                    System.out.println("Estacao: " + result[i].getName());
                    estacoesMeteorologicas.add(result[i].getName());
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }

            myAgent.addBehaviour(new ConsultaClima());
        }
    }
}
