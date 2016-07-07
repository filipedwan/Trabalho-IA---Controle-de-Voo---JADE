/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.agentes;

import caiaja.model.Aeroporto;
import caiaja.model.Aviao;
import caiaja.model.Bombeiro;
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
public class AgenteBombeiro extends Agent {
    
    private Bombeiro bombeiro;
    private Aviao aviao;
    private Aeroporto aeroporto_atual;
    private Controlador Controlador_atual;
    private AID aeroporto;
    private AID Controlador;
    private boolean ativo;

    protected void setup() {
        Object[] args = getArguments();

        bombeiro = new Bombeiro();
        aviao = null;
        aeroporto = null;
        aeroporto_atual = null;
        ativo = false;

        if (args != null) {
            if (args.length > 0) {
                bombeiro.setNome((String) args[0]);

                System.out.println("Bombeiro " + bombeiro.getNome() + " procurando incêndio");
                //System.out.println("Bombeiro " + bombeiro.getNome() + " procurando aviao pegando fogo");

                DFAgentDescription dfd = new DFAgentDescription();
                dfd.setName(getAID());
                ServiceDescription sd = new ServiceDescription();
                sd.setType("Bombeiro");
                sd.setName(bombeiro.getNome());
                dfd.addServices(sd);

                try {
                    DFService.register(this, dfd);
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }

                addBehaviour(new AgenteBombeiro.BuscarEmprego(this, 60000));

                addBehaviour(new AgenteBombeiro.RequisicoesDePropostas());
            }
        }
    }

    private class BuscarEmprego extends TickerBehaviour {

        public BuscarEmprego(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {

            if (aviao == null) {
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

                myAgent.addBehaviour(new AgenteBombeiro.PropoeTrabalhar(aerosportos));
                if (aeroporto_atual != null) {
                        myAgent.addBehaviour(new AgenteBombeiro.PropoeApagarFogo(aerosportos));
                    }
            } 
//            else {
//
//                List<AID> Controladores = new ArrayList<AID>();
//                if (Controlador == null) {
//                    DFAgentDescription template = new DFAgentDescription();
//                    ServiceDescription sd = new ServiceDescription();
//                    sd.setType("Controlador");
//                    template.addServices(sd);
//
//                    try {
//                        DFAgentDescription[] result = DFService.search(myAgent, template);
//                        for (int i = 0; i < result.length; ++i) {
//                            Controladores.add(result[i].getName());
//                        }
//                    } catch (FIPAException fe) {
//                        fe.printStackTrace();
//                    }
//                } else {
//                    Controladores.add(Controlador);
//                }
//
//                if (ativo) {
//                    System.out.println(bombeiro.getNome() + " apagando fogo do aviao: " + aviao.getPrefixo());
//                } else {
//                    if (aeroporto_atual != null) {
//                        myAgent.addBehaviour(new AgenteBombeiro.PropoeApagarFogo(Controladores));
//                    }
//                }
//            }

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
            System.out.println(bombeiro.getNome() + ": Apagar Fogo " + estado);
            switch (estado) {
                case 0: {
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                    for (AID aerosporto : aerosportos) {
                        System.out.println(bombeiro.getNome() + " --> " + aerosporto.getName());
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
                                aeroporto_atual = (Aeroporto) reply.getContentObject();
                            } catch (UnreadableException ex) {
                                Logger.getLogger(PilotoAgente.class.getName()).log(Level.SEVERE, null, ex);
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
                    System.out.println(bombeiro.getNome() + " --> " + Escolhido.getName() + ": Aceito Trabalhar");

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
                            System.out.println(bombeiro.getNome() + ": trabalhando para o  " + aeroporto.getLocalName());
                            
                            //try {
                            //    aviao = ((Aviao) reply.getContentObject());
                            //    System.out.println(bombeiro.getNome() + ": trabalhando para o  " + aeroporto_atual.getNome());
                            //} catch (UnreadableException ex) {
                            //    Logger.getLogger(PilotoAgente.class.getName()).log(Level.SEVERE, null, ex);
                            //}

                        } else {
                            System.out.println(bombeiro.getNome() + ": não foi contratado por " + Escolhido.getName() + " já conseguiu outro bombeiro");
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

    private class PropoeApagarFogo extends Behaviour {

        List<AID> controladores;
        AID ControladorEscolhido;
        Controlador Controlador_model;
        int estado = 0;
        private MessageTemplate mt; // The template to receive replies
        private int repliesCnt = 0;
        int estado_final = 10;

        public PropoeApagarFogo(List<AID> _controladores) {
            this.controladores = _controladores;
        }

        @Override
        public void action() {
            System.out.println(bombeiro.getNome() + ": Apagando Fogo " + estado);
            switch (estado) {
                case 0: {
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    if (Controlador == null) {
                        for (AID controlador : controladores) {
                            System.out.println(bombeiro.getNome() + " --> " + controlador.getName());
                            cfp.addReceiver(controlador);
                        }
                    } else {
                        cfp.addReceiver(Controlador);
                    }

                    cfp.setConversationId("proposta-bombeiro-fogo");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());
                    cfp.setContent(aeroporto_atual.getPrefixo());

                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("proposta-bombeiro-fogo"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));

                    estado = 1;
                    break;
                }
                case 1: {
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            ControladorEscolhido = reply.getSender();
                            try {
                                Controlador_model = (Controlador) reply.getContentObject();
                            } catch (UnreadableException ex) {
                                Logger.getLogger(PilotoAgente.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            estado = 2;
                        } else {
                            estado = estado_final;
                        }
                    } else {
                        block();
                    }
                    break;
                }
                case 2: {
                    Controlador = ControladorEscolhido;
                    Controlador_atual = Controlador_model;;
                    ACLMessage msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    msg.addReceiver(ControladorEscolhido);
//                    controlar.setContent("Aceito controlar");
                    try {
                        msg.setContentObject(bombeiro);
                    } catch (IOException ex) {
                        Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    msg.setConversationId("proposta-bombeiro-fogo");
                    msg.setReplyWith("apagar fogo" + System.currentTimeMillis());
                    myAgent.send(msg);
                    System.out.println(bombeiro.getNome() + " --> " + Controlador_atual.getNome() + ": Fogo Apagado");

                    mt = MessageTemplate.and(
                            MessageTemplate.MatchConversationId("proposta-bombeiro-fogo"),
                            MessageTemplate.MatchInReplyTo(msg.getReplyWith())
                    );
                    estado = 3;
                    break;
                }
                case 3: {
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            aeroporto = reply.getSender();
                            System.out.println(bombeiro.getNome() + ": apagando fogo do avião " + aviao.getPrefixo());
                            ativo = true;
                        } else {
                            System.out.println(bombeiro.getNome() + ": permissão negada para apagar fogo " + Controlador_atual.getNome() + " abortada");
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
            if (estado == estado_final) {
                return true;
            }
            if (estado > 1 && ControladorEscolhido == null) {
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
