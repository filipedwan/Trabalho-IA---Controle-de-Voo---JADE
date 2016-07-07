/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.agentes;

import caiaja.model.Aeroporto;
import caiaja.model.Aviao;
import caiaja.model.Controlador;
import caiaja.model.Piloto;
import caiaja.ontologia.CAIAJaOntologia;
import caiaja.ontologia.acoes.Decolar;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ufrr
 */
public class PilotoAgent extends Agent {

    private Piloto piloto;
    private Aviao aviao;
    private Aeroporto aeroporto_atual;
    private Controlador Controlador_atual;
    private AID aeroporto;
    private AID Controlador;
    private boolean emvoo;

    protected void setup() {
        Object[] args = getArguments();

        piloto = new Piloto();
        aviao = null;
        aeroporto = null;
        emvoo = false;
        aeroporto_atual = null;

        if (args != null) {
            if (args.length > 0) {
                piloto.setNome((String) args[0]);

                System.out.println("Piloto " + piloto.getNome() + " procurando avião");

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

                getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL0);
                getContentManager().registerOntology(CAIAJaOntologia.getInstance());

                addBehaviour(new PilotoAgent.BuscarAtividade(this, 5000));

                addBehaviour(new PilotoAgent.RequisicoesDePropostas());
            }
        }
    }

    private class BuscarAtividade extends TickerBehaviour {

        public BuscarAtividade(Agent a, long period) {
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

                myAgent.addBehaviour(new PilotoAgent.PropoePilotar(aerosportos));
            } else {
                List<AID> Controladores = new ArrayList<AID>();
                if (Controlador == null) {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("Controlador");
                    template.addServices(sd);

                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        for (int i = 0; i < result.length; ++i) {
                            Controladores.add(result[i].getName());
                        }
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }
                } else {
                    Controladores.add(Controlador);
                }

                if (emvoo) {
                    System.out.println(piloto.getNome() + ": Em voo com " + aviao.getPrefixo());
                    
                    
                    
                    //propõe pouso depois de 1 minuto pilotando
                    SequentialBehaviour propoePousar = new SequentialBehaviour(myAgent) {
                        @Override
                        public int onEnd() {
                            myAgent.doDelete();
                            return 0;
                        }
                        
                    };
                    
                    long time = (long) (60000 + Math.random() * 60000);
                    propoePousar.addSubBehaviour(new WakerBehaviour(myAgent, time) {
                        
                        @Override
                        protected void onWake() {
                            ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
                            msg.setContent("Pousar");
                            msg.addReceiver(Controlador);
                            msg.setConversationId("proposta-pouso");
                            System.out.println("Piloto "+piloto.getNome()+ " Enviando proposta de pouso");
                            send(msg);
                        }
                        
                    });
                    
                    addBehaviour(propoePousar);
                
                } else {
                    if (aeroporto_atual != null) {
                        myAgent.addBehaviour(new PilotoAgent.PropoeDecolar(Controladores));
                    }
                }
            }

        }

    }
    
    private class PropoePousar extends SequentialBehaviour {

        public PropoePousar(Agent a) {
            super(a);
        }

        @Override
        public int onEnd() {
            return 0;
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
            System.out.println(piloto.getNome() + ": Pilotar " + estado);
            switch (estado) {
                case 0: {
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                    for (AID aerosporto : aerosportos) {
                        System.out.println(piloto.getNome() + " --> " + aerosporto.getName());
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
                    ACLMessage reply = myAgent.receive(mt);

                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            Escolhido = reply.getSender();
                            try {
                                aeroporto_atual = (Aeroporto) reply.getContentObject();
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
                        msg.setContentObject(piloto);
                    } catch (IOException ex) {
                        Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    msg.setConversationId("proposta-piloto");
                    msg.setReplyWith("pilotar" + System.currentTimeMillis());
                    myAgent.send(msg);
                    System.out.println(piloto.getNome() + " --> " + Escolhido.getName() + ": Aceito Pilotar");

                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("proposta-piloto"),
                            MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                    estado = 3;
                    break;
                }
                case 3: {
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            aeroporto = reply.getSender();
                            try {
                                aviao = ((Aviao) reply.getContentObject());
                                System.out.println(piloto.getNome() + ": Pilotando " + aviao.getPrefixo());
                            } catch (UnreadableException ex) {
                                Logger.getLogger(PilotoAgent.class.getName()).log(Level.SEVERE, null, ex);
                            }

                        } else {
                            System.out.println(piloto.getNome() + ": não pode pilotar " + Escolhido.getName() + " já conseguiu outro piloto");
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

    private class acaoDecolar extends OneShotBehaviour {

        // Local variables
        Behaviour queryBehaviour = null;
        Behaviour requestBehaviour = null;

        // Constructor
        public acaoDecolar(Agent myAgent) {
            super(myAgent);
        }

        @Override
        public void action() {
//            try {
            Decolar dec = new Decolar();
            dec.setAviao(((PilotoAgent) myAgent).aviao);
            dec.setAeroporto(((PilotoAgent) myAgent).aeroporto_atual);

            Ontology o = myAgent.getContentManager().lookupOntology(CAIAJaOntologia.NAME);
                // Create an ACL message to query the engager agent if the above fact is true or false
//                ACLMessage queryMsg = new ACLMessage(ACLMessage.QUERY_IF);
//                queryMsg.addReceiver(((PilotoAgent) myAgent).Controlador);
//                queryMsg.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
//                queryMsg.setOntology(CAIAJaOntologia.NAME);
            // Write the works for predicate in the :content slot of the message

//                try {
//                    myAgent.getContentManager().fillContent(queryMsg, dec);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
                // Create and add a behaviour to query the engager agent whether
            // person p already works for company c following a FIPAQeury protocol
//                queryBehaviour = new CheckAlreadyWorkingBehaviour(myAgent, queryMsg);
//                addSubBehaviour(queryBehaviour);
//            } catch (IOException ioe) {
//                System.err.println("I/O error: " + ioe.getMessage());
//            }
        }

    }

    private class PropoeDecolar extends Behaviour {

        List<AID> controladores;
        AID ControladorEscolhido;
        Controlador Controlador_model;
        int estado = 0;
        private MessageTemplate mt; // The template to receive replies
        private int repliesCnt = 0;
        int estado_final = 10;

        public PropoeDecolar(List<AID> _controladores) {
            this.controladores = _controladores;
        }

        @Override
        public void action() {
            System.out.println(piloto.getNome() + ": Decolar " + estado);
            switch (estado) {
                case 0: {
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    if (Controlador == null) {
                        for (AID controlador : controladores) {
                            System.out.println(piloto.getNome() + " --> " + controlador.getName());
                            cfp.addReceiver(controlador);
                        }
                    } else {
                        cfp.addReceiver(Controlador);
                    }

                    cfp.setConversationId("proposta-piloto-decolar");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());
                    cfp.setContent(aeroporto_atual.getPrefixo());

                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("proposta-piloto-decolar"),
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
                                Logger.getLogger(PilotoAgent.class.getName()).log(Level.SEVERE, null, ex);
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
                        msg.setContentObject(piloto);
                    } catch (IOException ex) {
                        Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    msg.setConversationId("proposta-piloto-decolar");
                    msg.setReplyWith("pilotar" + System.currentTimeMillis());
                    myAgent.send(msg);
                    System.out.println(piloto.getNome() + " --> " + Controlador_atual.getNome() + ": Pronto pra decolar");

                    mt = MessageTemplate.and(
                            MessageTemplate.MatchConversationId("proposta-piloto-decolar"),
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
                            System.out.println(piloto.getNome() + ": decolando " + aviao.getPrefixo());
                            emvoo = true;
                        } else {
                            System.out.println(piloto.getNome() + ": não pode decolar " + Controlador_atual.getNome() + " Decolagem abortada");
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