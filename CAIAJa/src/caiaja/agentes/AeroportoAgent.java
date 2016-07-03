/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.agentes;

import caiaja.model.Aeroporto;
import caiaja.model.Pista;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 *
 * @author fosa
 */
public class AeroportoAgent extends Agent {

    Aeroporto aeroporto;

    public Aeroporto getAeroporto() {
        return aeroporto;
    }

    public void setAeroporto(Aeroporto aeroporto) {
        this.aeroporto = aeroporto;
    }

    protected void setup() {
        Object[] args = getArguments();

        aeroporto = new Aeroporto();

        if (args != null) {
            if (args.length > 0) {
                try {
                    String[] strargs = ((String) args[0]).split("&");
                    if (strargs.length > 0) {
                        aeroporto.setPrefixo(strargs[0]);
                    }
                    if (strargs.length > 1) {
                        aeroporto.setNome(strargs[1]);
                    }
                } catch (Exception e) {

                }
            }
            if (args.length > 1) {
                String strargs = ((String) args[1]);
                aeroporto.setPrefixo(strargs);
            }
            if (args.length > 2) {
                String strargs = ((String) args[2]);
                aeroporto.setNome(strargs);
            }
            if (args.length > 3) {
                try {
                    int intargs = Integer.parseInt((String) args[2]);
                    aeroporto.addPista(new Pista(intargs));
                } catch (Exception e) {

                }
            }
            System.out.println("Aeroporto " + aeroporto.getNome() + " operando");

            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("Aeroporto");
            sd.setName(aeroporto.getPrefixo());
            dfd.addServices(sd);

            try {
                DFService.register(this, dfd);
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }

            addBehaviour(new RequisicoesDePropostas());
            addBehaviour(new PropostaControlar());
        }

    }

    protected void takeDown() {
        System.out.println("Controlador " + aeroporto.getNome() + " saindo de operação.");
    }

    /**
     * Classe para responder aos requerimentos de controladores que precisem de
     * um aeroporto pra controlar, retonar sim ou não para a requisição
     */
    private class RequisicoesDePropostas extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                if (aeroporto.getControlador() == null) {
                    System.out.println(getName() + ": Preciso de um controlador");
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("Preciso de Cointrolador");
                } else {
                    System.out.println(getName() + ": já tenho um controlador");
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("ja tenho um controlador");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private class PropostaControlar extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                if (aeroporto.getControlador() == null) {
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println(getName() + ": controlado por " + msg.getSender().getName());
                } else {
                    System.out.println(getName()+": arrumou um controlador neste tempo");
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

}
