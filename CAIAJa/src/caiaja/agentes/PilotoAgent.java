/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.agentes;

import caiaja.CAIAJa;
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
    private Aeroporto aeroportoModel;
    private Controlador controladorModel;
    private AID aeroportoAgent;
    private AID controladorAgent;
    private boolean emvoo;
    private boolean emAcao;

    protected void setup() {
        Object[] args = getArguments();

        piloto = new Piloto();
        aviao = null;
        aeroportoAgent = null;
        emvoo = false;
        emAcao = false;
        aeroportoModel = null;

        if (args != null) {
            if (args.length > 0) {
                piloto.setNome((String) args[0]);

                System.out.println("Piloto " + piloto.getNome() + " procurando avião");

                CAIAJa.registrarServico(this, "Piloto", piloto.getNome());

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

            /**
             * Verifica atitudde a ser tomada
             */
            if (!emAcao) {
                if (aviao == null) {
                    /**
                     * Se não tem avião busca um aeroporto com aviões
                     */
                    List<AID> aerosportos = CAIAJa.getServico(myAgent, "Aeroporto");

                    myAgent.addBehaviour(new PilotoAgent.PropoePilotar(aerosportos));

                } else if (controladorAgent == null) {
                    /**
                     * Se não tem controlador busca um controlador
                     */

                    myAgent.addBehaviour(new ConsultarControlador(myAgent));

                } else if (emvoo) {
                    aviao.setAceleracaoMotor(0.3f);
                    System.out.println(piloto.getNome() + ": Em voo com " + aviao.getPrefixo());

                    //propõe pouso depois de 1 minuto pilotando
                    propoePousar();

                } else if (aeroportoModel != null) {

                    if (aviao.getNilveCombustivel() > 0.9f) {
                        myAgent.addBehaviour(new PilotoAgent.PropoeDecolar(myAgent));
                    } else {
                        //TODO Criar comportamento pra abastecer
                        /**
                         * chama abastecedor
                         */
                        System.err.println("Taquen não está cheio" + aviao.getPrefixo());

                    }
                }
            }
        }

        /*
         Método que com InnerClasses que fazem o Caso de Uso de pouso do piloto:
         */
        public void propoePousar() {
            SequentialBehaviour propoePousar = new SequentialBehaviour(myAgent) {
                @Override
                public int onEnd() {
                    emvoo = false;
                    myAgent.doDelete();

                    return 0;
                }

            };

            long time = (long) (60000 + Math.random() * 60000);
            //propoePousar.addSubBehaviour(new WakerBehaviour(myAgent, time) {
            propoePousar.addSubBehaviour(new WakerBehaviour(myAgent, 20) {
                @Override
                protected void onWake() {
                    ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
                    msg.setContent("Pousar");
                    msg.addReceiver(controladorAgent);
                    msg.setConversationId("proposta-pouso");
                    System.out.println("Piloto " + piloto.getNome() + " Enviando proposta de pouso");
                    send(msg);
                    stop();
                }

            });

            propoePousar.addSubBehaviour(new TickerBehaviour(myAgent, 200) {
                @Override
                protected void onTick() {
                    //System.err.println("\nCHEGOU AQUI\n");
                    MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    MessageTemplate mt2 = MessageTemplate.MatchConversationId("pouso-autorizado");
                    MessageTemplate mt = MessageTemplate.and(mt1, mt2);

                    ACLMessage msg = myAgent.receive(mt);

                    if (msg != null) {
                        if (msg.getConversationId().equalsIgnoreCase("pouso-autorizado")) {
                            System.out.println("Piloto " + piloto.getNome() + " preparando para pouso.");
                            stop();
                        }
                    } else {
                        block();
                    }
                }
            });

            propoePousar.addSubBehaviour(new OneShotBehaviour(myAgent) {
                @Override
                public void action() {
                    //aviao x pousado com sucesso
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.setConversationId("pouso-sucesso");
                    System.out.println("Piloto " + piloto.getNome() + " informa que o pouso da aeronave " + aviao.getPrefixo() + " foi realizado com sucesso");
                    myAgent.send(msg);
                    emvoo = false;
                }
            });

            // TODO: reiniciar atributos do Piloto
            // TODO: colocar avião no Aeroporto e remover avião do Piloto
            propoePousar.addSubBehaviour(this);

            addBehaviour(propoePousar);
        }

    }

    private class PropoePilotar extends Behaviour {

        List<AID> lista_aeroportos;
        AID Escolhido;
        int estado = 0;
        private MessageTemplate mt; // The template to receive replies
        private int repliesCnt = 0;

        public PropoePilotar(List<AID> aerosportos) {
            this.lista_aeroportos = aerosportos;
        }

        @Override
        public void action() {
            //System.out.println(piloto.getNome() + ": Pilotar " + estado);
            switch (estado) {
                case 0: {
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                    for (AID aeroporto : lista_aeroportos) {
                        System.out.println(piloto.getNome() + " --> " + aeroporto.getLocalName());
                        cfp.addReceiver(aeroporto);
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
                                aeroportoModel = (Aeroporto) reply.getContentObject();
                            } catch (UnreadableException ex) {
                                Logger.getLogger(PilotoAgent.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                        repliesCnt++;
                        if (repliesCnt >= lista_aeroportos.size()) {
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
                    System.out.println(piloto.getNome() + " --> " + Escolhido.getLocalName() + ": Aceito Pilotar");

                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("proposta-piloto"),
                            MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                    estado = 3;
                    break;
                }
                case 3: {
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            aeroportoAgent = reply.getSender();
                            try {
                                aviao = ((Aviao) reply.getContentObject());
                                System.out.println(piloto.getNome() + ": Pilotando " + aviao.getPrefixo() + " motores ligados");
                                aviao.setAceleracaoMotor(0.01f);
                            } catch (UnreadableException ex) {
                                Logger.getLogger(PilotoAgent.class.getName()).log(Level.SEVERE, null, ex);
                            }

                        } else {
                            System.out.println(piloto.getNome() + ": não pode pilotar " + Escolhido.getLocalName() + " já conseguiu outro piloto");
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
            Decolar dec = new Decolar(aviao, piloto, aeroportoModel, myAgent.getAID());
            dec.setAviao(((PilotoAgent) myAgent).aviao);
            dec.setAeroporto(((PilotoAgent) myAgent).aeroportoModel);

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

        int estado = 0;
        private MessageTemplate mt; // The template to receive replies
        private int repliesCnt = 0;
        int estado_final = 10;

        public PropoeDecolar(Agent myAgent) {
            super(myAgent);
            emAcao = true;
        }

        @Override
        public void action() {
//            System.out.println(piloto.getNome() + ": Decolar " + estado);
            switch (estado) {
                case 0: {
                    ACLMessage proposta = new ACLMessage(ACLMessage.PROPOSE);
                    if (controladorAgent != null) {
                        System.out.println(piloto.getNome() + " --> " + controladorModel.getNome());
                        proposta.addReceiver(controladorAgent);

                        aviao.setAceleracaoMotor(0);

                        Decolar decolar = new Decolar(aviao, piloto, aeroportoModel, myAgent.getAID());

                        proposta.setConversationId("proposta-piloto-decolar");
                        proposta.setReplyWith("cfp" + System.currentTimeMillis());
                        try {
                            proposta.setContentObject(decolar);
                        } catch (IOException ex) {
                            Logger.getLogger(PilotoAgent.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        myAgent.send(proposta);
                        // Prepare the template to get proposals
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("proposta-piloto-decolar"),
                                MessageTemplate.MatchInReplyTo(proposta.getReplyWith()));
                        estado = 1;
                    } else {
                        estado = estado_final;
                    }
                    break;
                }
                case 1: {
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {

                            ACLMessage msg;

                            if (aviao.getNilveCombustivel() > 0.5f) {
                                msg = new ACLMessage(ACLMessage.REQUEST);

                                msg.setConversationId("liberacao-piloto-decolar");
                                msg.setReplyWith("confirm" + System.currentTimeMillis());

                                msg.addReceiver(reply.getSender());

                                System.out.println(piloto.getNome() + ": Aguardando liberação para decolar " + aviao.getPrefixo());
                                estado = 2;
                                try {
                                    msg.setContentObject(piloto);
                                } catch (IOException ex) {
                                    Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            } else {
                                msg = new ACLMessage(ACLMessage.CANCEL);

                                msg.setConversationId("cancela-piloto-decolar");
                                msg.setReplyWith("cancel" + System.currentTimeMillis());

                                System.out.println(piloto.getNome() + ": cancelando decolagem " + aviao.getPrefixo());
                                estado = 3;
                            }

                            mt = MessageTemplate.and(
                                    MessageTemplate.MatchConversationId(msg.getConversationId()),
                                    MessageTemplate.MatchInReplyTo(msg.getReplyWith())
                            );

                            myAgent.send(msg);

                        } else {
                            estado = estado_final;
                        }
                    } else {
                        block();
                    }
                    break;
                }
                case 2: {
                    /**
                     * Decolagem aprovada, aguardando clearance para decolagem
                     */
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.CONFIRM) {
                            aeroportoAgent = reply.getSender();
                            System.out.println(piloto.getNome() + ": decolando " + aviao.getPrefixo());
                            aviao.setAceleracaoMotor(1f);
                            emvoo = true;
                            emAcao = false;
                            estado = estado_final;
                        } else if (reply.getPerformative() == ACLMessage.DISCONFIRM) {
                            System.out.println(piloto.getNome() + ": decolagem cancelada pelo controlado " + controladorModel.getNome() + "");
                            emAcao = false;
                            estado = estado_final;
                        }
                    } else {
                        block();
                    }
                    break;
                }
                case 3: {
                    /**
                     * Decolagem cancelada, aguarda confirmação do controlador
                     */
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.CONFIRM) {
                            System.out.println(piloto.getNome() + ": retornando ao aeroporto " + aviao.getPrefixo());
                            estado = estado_final;
                            emAcao = false;
                        } else {

                        }
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
            return false;
        }

    }

    private class ConsultarControlador extends Behaviour {

        List<AID> Controladores_lista;
        int estado = 0;
        private MessageTemplate mt;
        private int repliesCnt = 0;
        int estado_final = 10;

        public ConsultarControlador(Agent myAgent) {
            super(myAgent);
        }

        @Override
        public void action() {
//            System.out.println(piloto.getNome() + ": Consulta Controlador " + estado);
            switch (estado) {
                case 0: {
                    Controladores_lista = CAIAJa.getServico(myAgent, "Controlador");

                    ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);

                    try {
                        //                    msg.setContent(aeroportoModel.getPrefixo());
                        msg.setContentObject(aeroportoModel);

                        //msg.setReplyWith("ccp" + System.currentTimeMillis());
                        msg.setReplyWith("qi" + System.currentTimeMillis());

                        msg.setConversationId("piloto-consulta-controlador");
                        System.out.println("Piloto " + piloto.getNome() + " consultando controlador");

                        for (AID Contr : Controladores_lista) {
                            msg.addReceiver(Contr);
                        }

                        MessageTemplate mt1 = MessageTemplate.MatchConversationId(msg.getConversationId());
                        MessageTemplate mt2 = MessageTemplate.MatchInReplyTo(msg.getReplyWith());
                        mt = MessageTemplate.and(mt1, mt2);

                        send(msg);

                    } catch (IOException ex) {
                        Logger.getLogger(PilotoAgent.class.getName()).log(Level.SEVERE, null, ex);

                        estado = estado_final;
                    }

                    estado = 1;
                    break;
                }
                case 1: {
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.CONFIRM) {
                            try {
                                controladorModel = (Controlador) reply.getContentObject();
                                controladorAgent = reply.getSender();

                            } catch (UnreadableException ex) {
                                Logger.getLogger(PilotoAgent.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            estado = 2;
                        }
                        repliesCnt++;
                        if (repliesCnt >= Controladores_lista.size()) {
                            estado = estado_final;
                        }
                    } else {
                        block();
                    }
                    break;
                }
                case 2: {
                    break;
                }
            }
        }

        @Override
        public boolean done() {
            if (estado == 2) {
                return true;
            }
            if (estado == estado_final) {
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
