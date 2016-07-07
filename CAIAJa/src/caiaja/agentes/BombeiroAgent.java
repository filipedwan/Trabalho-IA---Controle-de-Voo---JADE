/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.agentes;

import caiaja.CAIAJa;
import caiaja.model.Aeroporto;
import caiaja.model.Bombeiro;
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
import jade.lang.acl.UnreadableException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ufrr
 */
public class BombeiroAgent extends Agent {

    private Bombeiro bombeiro;
    private Aeroporto aeroporto_base;
    private AID aeroporto;
    private boolean ativo;

    protected void setup() {
        Object[] args = getArguments();

        bombeiro = new Bombeiro();
        aeroporto = null;
        aeroporto_base = null;
        ativo = false;

        if (args != null) {
            if (args.length > 0) {
                bombeiro.setNome((String) args[0]);

                System.out.println("Bombeiro " + bombeiro.getNome() + " aguardando trabalho");

                CAIAJa.registrarServico(this, "Bombeiro", bombeiro.getNome());
                
                addBehaviour(new BombeiroAgent.BuscarEmprego(this, 5000));

                addBehaviour(new BombeiroAgent.AguardaAlertaDeIncendio());
            }
        }
    }

    private class BuscarEmprego extends TickerBehaviour {

        public BuscarEmprego(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            if (aeroporto_base == null) {
                myAgent.addBehaviour(new BombeiroAgent.PropoeTrabalhar(CAIAJa.getServico(myAgent, "Aeroporto")));
            } else {
                System.out.println("Bombeiro " + bombeiro.getNome() + ": trabalhando em " + aeroporto_base.getNome());

                block(1000);
            }
        }

    }

    private class PropoeTrabalhar extends Behaviour {

        List<AID> aerosportos;
        AID Escolhido;
        int estado = 0;
        private MessageTemplate mt; // The template to receive replies
        private int repliesCnt = 0;

        public PropoeTrabalhar(List<AID> aerosportos) {
            this.aerosportos = aerosportos;
        }

        @Override
        public void action() {
            System.out.println("Bombeiro " + bombeiro.getNome() + ": Trablho bombeiro " + estado);
            switch (estado) {
                case 0: {
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                    for (AID aerosporto : aerosportos) {
                        System.out.println("Bombeiro " + bombeiro.getNome() + " --> " + aerosporto.getName());
                        cfp.addReceiver(aerosporto);
                    }
                    cfp.setConversationId("proposta-bombeiro");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());
                    cfp.setContent("Precisa de Bombeiro?");
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("proposta-bombeiro"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    estado = 1;
                    break;
                }
                case 1: {
                    ACLMessage reply = myAgent.receive(mt);

                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            Escolhido = reply.getSender();
                            try {
                                aeroporto_base = (Aeroporto) reply.getContentObject();
                            } catch (UnreadableException ex) {
                                Logger.getLogger(PilotoAgent.class.getName()).log(Level.SEVERE, null, ex);
                            }
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
                    ACLMessage msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    msg.addReceiver(Escolhido);
//                    controlar.setContent("Aceito controlar");
                    try {
                        msg.setContentObject(bombeiro);
                    } catch (IOException ex) {
                        Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    msg.setConversationId("proposta-bombeiro");
                    msg.setReplyWith("trabalhar" + System.currentTimeMillis());
                    myAgent.send(msg);
                    System.out.println("Bombeiro " + bombeiro.getNome() + " --> " + Escolhido.getName() + ": Aceito Trabalhar");

                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("proposta-bombeiro"),
                            MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                    estado = 3;
                    break;
                }
                case 3: {
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            aeroporto = reply.getSender();
                            System.out.println("Bombeiro " + bombeiro.getNome() + ": trabalhando para o  " + aeroporto.getLocalName());

                        } else {
                            System.out.println("Bombeiro " + bombeiro.getNome() + ": não foi contratado por " + Escolhido.getName() + " já conseguiu outro bombeiro");
                        }

                        estado = 4;
                    } else {
                        block();
                    }
                    break;
                }
                case 4: {
                    break;
                }
            }
        }

        @Override
        public boolean done() {
            if (estado == 4) {
                return true;
            }
            if (estado > 1 && Escolhido == null) {
                return true;
            }
            return false;
        }

    }

    /**
     * Classe para responder aos requerimentos de controladores que precisem de
     * um aeroporto pra controlar, retonar sim ou não para a requisição
     */
    private class AguardaAlertaDeIncendio extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            MessageTemplate mt2 = MessageTemplate.MatchContent("Incendio");
            MessageTemplate mt = MessageTemplate.and(mt1, mt2);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {

                // CFP Message received. Process it
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();
                
                reply.setContent("Ainda não sei apagar fogo :(");

                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

}
