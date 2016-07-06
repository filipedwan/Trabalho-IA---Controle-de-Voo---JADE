/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.agentes;

import caiaja.model.Aviao;
import caiaja.model.Controlador;
import caiaja.model.Piloto;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ufrr
 */
public class PilotoAgente extends Agent {

    private Piloto piloto;
    private Aviao aviao;
    private AID aeroporto;

    protected void setup() {
        Object[] args = getArguments();
        
        piloto = new Piloto();
        aviao = null;
        aeroporto = null;
        
        if (args != null) {
            if (args.length > 0) {
                piloto.setNome((String) args[0]);

                System.out.println("Controlador " + piloto.getNome() + " procurando avião");

                DFAgentDescription dfd = new DFAgentDescription();
                dfd.setName(getAID());
                ServiceDescription sd = new ServiceDescription();
                sd.setType("Piloto");
                sd.setName(piloto.getNome());
                dfd.addServices(sd);

                try {
                    DFService.register(this, dfd);
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }

                addBehaviour(new PilotoAgente.BuscarEmprego(this, 2000));

                addBehaviour(new PilotoAgente.RequisicoesDePropostas());
            }
        }
    }
    
    
   private class BuscarEmprego extends TickerBehaviour {

        public BuscarEmprego(Agent a, long period) {
            super(a, period);
        }

       @Override
       protected void onTick() {

           if (aeroporto == null) {
               DFAgentDescription template = new DFAgentDescription();
               ServiceDescription sd = new ServiceDescription();
               sd.setType("Aeroporto");
               template.addServices(sd);

               List<AID> aerosportos = new ArrayList<AID>();
               try {
                   DFAgentDescription[] result = DFService.search(myAgent, template);
                   for (int i = 0; i < result.length; ++i) {
                       aerosportos.add(result[i].getName());
                   }
               } catch (FIPAException fe) {
                   fe.printStackTrace();
               }

               myAgent.addBehaviour(new PilotoAgente.PropoePilotar(aerosportos));
           } else {
               block();
           }

    }
   
   }
   
    private class PropoePilotar extends Behaviour {

        List<AID> aerosportos;
        AID Escolhido;
        int estado = 0;
        private MessageTemplate mt; // The template to receive replies
        private int repliesCnt = 0;

        public PropoePilotar(List<AID> aerosportos) {
            this.aerosportos = aerosportos;
        }

        @Override
        public void action() {
            switch (estado) {
                case 0: {
                    System.out.println(getName() + ": Estado 0");
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                    for (AID aerosporto : aerosportos) {
                        System.out.println(getName() + " --> " + aerosporto.getName());
                        cfp.addReceiver(aerosporto);
                    }
                    cfp.setConversationId("proposta-piloto");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());
                    cfp.setContent("Precisa de Piloto?");
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("proposta-piloto"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    estado = 1;
                    break;
                }
                case 1: {
                    System.out.println(getName() + ": Estado 1");
                    ACLMessage reply = myAgent.receive(mt);

                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            Escolhido = reply.getSender();
                        }

                        repliesCnt++;
                        if (repliesCnt >= aerosportos.size()) {
                            estado = 2;
                        }
                    } else {
                        block();
                    }
                    break;
                }
                case 2: {
                    System.out.println(getName() + ": Estado 2");

                    ACLMessage msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    msg.addReceiver(Escolhido);
//                    controlar.setContent("Aceito controlar");
                    try {
                        msg.setContentObject(piloto);
                    } catch (IOException ex) {
                        Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    msg.setConversationId("proposta-piloto");
                    msg.setReplyWith("pilotar" + System.currentTimeMillis());
                    myAgent.send(msg);
                    System.out.println(getName() + " --> " + Escolhido.getName() + ": Aceito Pilotar");

                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("proposta-piloto"),
                            MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                    estado = 3;
                    break;
                }
                case 3: {
                    System.out.println(getName() + ": Estado 3");

                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                                aeroporto = reply.getSender();
                                System.out.println(getName() + ": Pilotando " + Escolhido.getName());
                                
                        } else {
                            System.out.println(getName() + ": não pode pilotar " + Escolhido.getName() + " já conseguiu outro piloto");
                        }

                        estado = 4;
                    } else {
                        block();
                    }
                    break;
                }
                case 4: {
                    System.out.println(getName() + ": Estado 4");
                    break;
                }
            }
        }

        @Override
        public boolean done() {
            if (estado == 4) {
                return true;
            }
            return false;
        }

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

                myAgent.send(reply);
            } else {
                block();
            }
        }
    }    

}

