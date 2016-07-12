package caiaja.agentes;

import caiaja.model.EstacaoMeteorologica;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 *
 * @author fosa
 * Agent responsável por enviar mensagens sobre as condições 
 * de voo, baseado no clima atual.
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

        addBehaviour(new RespondeConsulta());
    }

    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("Estacao se desligando.");

    }

    /**
     * Comportamentos do Agente
     */
    private class RespondeConsulta extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {

                String title = msg.getContent();
                System.out.println("EM recebeu: " + title);
                ACLMessage reply = msg.createReply();

                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent("Condição do tempo Boa");
                myAgent.send(reply);
            } else {
                block();
            }
        }
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
        }
    }
}
