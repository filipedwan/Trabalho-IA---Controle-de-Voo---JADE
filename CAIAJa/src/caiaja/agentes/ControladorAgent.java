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
import caiaja.ontologia.predicados.Controla;
import caiaja.ontologia.predicados.ControladoPor;
import jade.content.ContentElementList;
import jade.content.Predicate;
import jade.content.abs.AbsPredicate;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.lang.sl.SLVocabulary;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Done;
import jade.content.onto.basic.TrueProposition;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SimpleAchieveREResponder;
import jade.util.leap.Iterator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fosa
 */
public class ControladorAgent extends Agent {

    Controlador controlador;
    Aeroporto aeroporto_m;

    AID aeroporto;
    List<Aviao> Avioes;

    public ControladorAgent() {
        Avioes = new ArrayList<Aviao>();
    }

    protected void setup() {
        Object[] args = getArguments();

        controlador = new Controlador();
        aeroporto = null;
        if (args != null) {
            if (args.length > 0) {
                controlador.setNome((String) args[0]);

                System.out.println("Controlador " + controlador.getNome() + " operando");

                // Register the codec for the SL0 language
                getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL0);

                getContentManager().registerOntology(CAIAJaOntologia.getInstance());

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

                addBehaviour(new BuscarEmprego(this, 2000));

                addBehaviour(new RecebePropostaDecolar());
                addBehaviour(new VerificaOntologia(this, 5000));
                addBehaviour(new Contato());

                // Create and add the behaviour for handling QUERIES using the employment-ontology
                addBehaviour(new TratarControladorConsultasBehaviour(this));

                // Create and add the behaviour for handling REQUESTS using the employment-ontology
                MessageTemplate mt = MessageTemplate.and(
                        MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                        MessageTemplate.MatchOntology(CAIAJaOntologia.NAME));
                TratarControladorConsultasBehaviour b = new TratarControladorConsultasBehaviour(this);
                TrataRequisicoesDecolar c = new TrataRequisicoesDecolar(this);

                addBehaviour(b);
                addBehaviour(c);
                
                //Verificando propostas de pouso dos pilotos
                tratarPropostasDePouso();
                
            }
        }
    }

    protected void takeDown() {
        System.out.println("Controlador " + controlador.getNome() + " saindo de operação.");
    }

    int Decolar(Aviao aviao, Aeroporto aero) {
        if (aeroporto_m.equals(aero)) {
            return 1;
        }
        return 0;
    }

    boolean Controla(Controlador con, Aeroporto aero) {
        if (controlador.equals(con) && aeroporto_m.equals(aero)) {
            return true;
        }
        return false;
    }

    private void tratarPropostasDePouso() {
        
        SequentialBehaviour tratarPropostasPouso = new SequentialBehaviour(this) {
            
            @Override
            public int onEnd() {
                return 0;
            }
            
        };
        
        tratarPropostasPouso.addSubBehaviour(new TickerBehaviour(this, 2000) {
                    @Override
                    protected void onTick() {
                        MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                        MessageTemplate mt2 = MessageTemplate.MatchConversationId("proposta-pouso");
                        MessageTemplate mt = MessageTemplate.and(mt1, mt2);
                        
                        ACLMessage msg = myAgent.receive(mt);
                        
                        if (msg != null) {
                            System.out.println("Controlador: " +getName()+ "informa que a pista está pronta para pouso "
                                    + "Piloto "+msg.getSender().getLocalName()+" pode pousar");
                            ACLMessage reply = msg.createReply();
                            reply.setContent("Pode pousar");
                            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                            reply.setConversationId("pouso-autorizado");
                            myAgent.send(reply);
                            stop();
                        } else {
                            block();
                        }
                    }
                });
        
        // TODO: Fazer uma proposta para o abastercer o avião que pousou
        
        addBehaviour(tratarPropostasPouso);
    }

    class TratarControladorConsultasBehaviour extends SimpleAchieveREResponder {

        /**
         *
         * @param myAgent The agent owning this behaviour
         */
        public TratarControladorConsultasBehaviour(Agent myAgent) {
            super(myAgent, MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_QUERY),
                    MessageTemplate.MatchOntology(CAIAJaOntologia.NAME)));
        }

        /**
         * This method is called when a QUERY-IF or QUERY-REF message is
         * received.
         *
         * @param msg The received query message
         * @return The ACL message to be sent back as reply
         * @see jade.proto.FipaQueryResponderBehaviour
         */
        @Override
        public ACLMessage prepareResponse(ACLMessage msg) {

            ACLMessage reply = msg.createReply();

            // The QUERY message could be a QUERY-REF. In this case reply 
            // with NOT_UNDERSTOOD
            if (msg.getPerformative() != ACLMessage.QUERY_IF) {
                reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                String content = "(" + msg.toString() + ")";
                reply.setContent(content);
                return (reply);
            }

            try {
                // Get the predicate for which the truth is queried	
                Predicate pred = (Predicate) myAgent.getContentManager().extractContent(msg);
                if (!(pred instanceof ControladoPor)) {
                    // If the predicate for which the truth is queried is not WORKS_FOR
                    // reply with NOT_UNDERSTOOD
                    reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                    String content = "(" + msg.toString() + ")";
                    reply.setContent(content);
                    return (reply);
                }

                // Reply 
                reply.setPerformative(ACLMessage.INFORM);
                Controla contr = (Controla) pred;
                Controlador con = contr.getControlador();
                Aeroporto aero = contr.getAeroporto();
                if (((ControladorAgent) myAgent).Controla(con, aero)) {
                    reply.setContent(msg.getContent());
                } else {
                    // Create an object representing the fact that the WORKS_FOR 
                    // predicate is NOT true.
                    Ontology o = getContentManager().lookupOntology(CAIAJaOntologia.NAME);
                    AbsPredicate not = new AbsPredicate(SLVocabulary.NOT);
                    not.set(SLVocabulary.NOT_WHAT, o.fromObject(contr));
                    myAgent.getContentManager().fillContent(reply, not);
                }
            } catch (Codec.CodecException fe) {
                System.err.println(myAgent.getLocalName() + " Fill/extract content unsucceeded. Reason:" + fe.getMessage());
            } catch (OntologyException oe) {
                System.err.println(myAgent.getLocalName() + " getRoleName() unsucceeded. Reason:" + oe.getMessage());
            }

            return (reply);

        }

    }

    /**
     * This behaviour handles a single engagement action that has been requested
     * following the FIPA-Request protocol
     */
    class TrataRequisicoesDecolar extends SimpleAchieveREResponder {

        /**
         * Constructor for the <code>HandleEngageBehaviour</code> class.
         *
         * @param myAgent The agent owning this behaviour
         * @param requestMsg The ACL message by means of which the engagement
         * action has been requested
         */
        public TrataRequisicoesDecolar(Agent myAgent) {
            super(myAgent, MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST));
        }

        /**
         * This method implements the
         * <code>FipaRequestResponderBehaviour.Factory</code> interface. It will
         * be called within a <code>FipaRequestResponderBehaviour</code> when an
         * engagement action is requested to instantiate a new
         * <code>HandleEngageBehaviour</code> handling the requested action
         *
         * @param msg The ACL message by means of which the engagement action
         * has been requested
         */
        @Override
        public ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) {
            // Prepare a dummy ACLMessage used to create the content of all reply messages
            ACLMessage msg = request.createReply();

            try {
                // Get the requested action
                Action a = (Action) myAgent.getContentManager().extractContent(request);
                Decolar dec = (Decolar) a.getAction();
                Piloto pil = dec.getPiloto();
                Aviao av = dec.getAviao();
                Aeroporto aero = dec.getAeroporto();

                // Perform the engagement action
                int result = ((ControladorAgent) myAgent).Decolar(av, aero);

                // Reply according to the result
                if (result > 0) {
                    // OK --> INFORM action done
                    Done d = new Done();
                    d.setAction(a);
                    myAgent.getContentManager().fillContent(msg, d);
                    msg.setPerformative(ACLMessage.INFORM);
                } else {
                    // NOT OK --> FAILURE
                    ContentElementList l = new ContentElementList();
                    l.add(a);
//                    l.add(new EngagementError());
                    myAgent.getContentManager().fillContent(msg, l);
                    msg.setPerformative(ACLMessage.FAILURE);
                }

            } catch (Exception fe) {
                System.out.println(myAgent.getName() + ": Error ao tratar decolar");
                System.out.println(fe.getMessage());
            }

            // System.out.println(msg);
            return msg;
        }

        @Override
        public ACLMessage prepareResponse(ACLMessage request) {
            // Prepare a dummy ACLMessage used to create the content of all reply messages
            ACLMessage temp = request.createReply();

            try {
                // Get the requested action. 
                Action a = (Action) getContentManager().extractContent(request);
                Decolar dec = (Decolar) a.getAction();
                Piloto pil = dec.getPiloto();
                Aviao av = dec.getAviao();
                Aeroporto aero = dec.getAeroporto();

                ContentElementList l = new ContentElementList();
                l.add(a);
                l.add(new TrueProposition());
                getContentManager().fillContent(temp, l);
                temp.setPerformative(ACLMessage.AGREE);

            } catch (Exception fe) {
                fe.printStackTrace();
                System.out.println(getName() + ": Error na acao decolar.");
                System.out.println(fe.getMessage());
            }

            return temp;
        }
    }

    private class VerificaOntologia extends TickerBehaviour {

        boolean done = false;

        public VerificaOntologia(Agent myAgent, long milis) {
            super(myAgent, milis);
        }

        @Override
        protected void onTick() {

            if (aeroporto_m != null) {
                System.out.println(controlador.getNome() + ": Verificando Ontologia ");
                ControladoPor ctrl = new ControladoPor();
                ctrl.setAeroporto(aeroporto_m);
                ctrl.setControlador(controlador);

                Ontology o = myAgent.getContentManager().lookupOntology(CAIAJaOntologia.NAME);
                // Create an ACL message to query the engager agent if the above fact is true or false
                ACLMessage queryMsg = new ACLMessage(ACLMessage.QUERY_IF);
                queryMsg.addReceiver(((ControladorAgent) myAgent).aeroporto);
                queryMsg.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
                queryMsg.setOntology(CAIAJaOntologia.NAME);
                // Write the works for predicate in the :content slot of the message

                try {
                    myAgent.getContentManager().fillContent(queryMsg, ctrl);
                } catch (Exception e) {
//                    e.printStackTrace();
                }
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

                myAgent.addBehaviour(new PropoeControlar(aerosportos));
            }
        }

    }

    private class PropoeControlar extends Behaviour {

        List<AID> aerosportos;
        AID Escolhido;
        int estado = 0;
        private MessageTemplate mt; // The template to receive replies
        private int repliesCnt = 0;

        public PropoeControlar(List<AID> aerosportos) {
            this.aerosportos = aerosportos;
        }

        @Override
        public void action() {
            System.out.println(controlador.getNome() + ": PropoeControlar " + estado);
            switch (estado) {
                case 0: {
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                    for (AID aerosporto : aerosportos) {
                        System.out.println(controlador.getNome() + " --> " + aerosporto.getName());
                        cfp.addReceiver(aerosporto);
                    }
                    cfp.setConversationId("proposta-controlador");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());
                    cfp.setContent("Precisa de Controlador?");
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("proposta-controlador"),
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
                    ACLMessage controlar = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    controlar.addReceiver(Escolhido);
//                    controlar.setContent("Aceito controlar");
                    try {
                        controlar.setContentObject(controlador);
                    } catch (IOException ex) {
                        Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    controlar.setConversationId("proposta-controlador");
                    controlar.setReplyWith("controlar" + System.currentTimeMillis());
                    myAgent.send(controlar);
                    System.out.println(controlador.getNome() + " --> " + Escolhido.getName() + ": Aceito Controlar");

                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("proposta-controlador"),
                            MessageTemplate.MatchInReplyTo(controlar.getReplyWith()));
                    estado = 3;
                    break;
                }
                case 3: {
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
//                            try {
                            aeroporto = reply.getSender();
                            try {
                                aeroporto_m = (Aeroporto) reply.getContentObject();
                            } catch (UnreadableException ex) {
                                Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            System.out.println(controlador.getNome() + ": Controlando " + Escolhido.getName());

                        } else {
                            System.out.println(controlador.getNome() + ": não pode controlar " + Escolhido.getName() + " já conseguiu outro controlador");
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

    private class ConsultaClima extends Behaviour {

        @Override
        public void action() {
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            cfp.setConversationId("vo-passar");
            cfp.setReplyWith("cfp" + System.currentTimeMillis());
            cfp.setContent("Como está o tempo?");
            myAgent.send(cfp);

        }

        @Override
        public boolean done() {
//            if (estacoesMeteorologicas.size() < 0) {
//                return true;
//            }
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            return true;
        }

    }

    /**
     * COmportamento do agente Consultar o piloto
     */
    private class ConsultaPiloto extends Behaviour {

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
     * Comportamento do agente Consultar a situação do clime na regiao
     */
    /**
     * Comportamento do agente Em Caso de incendio uma requisicao de combate a
     * incendio deverá ser enviado a equipe de Bombeiros que estiver de
     * prontidão
     */
    private class RequerimentoDeCombateAIncendio extends Behaviour {

        @Override
        public void action() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean done() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    private class Contato extends CyclicBehaviour {

        @Override
        public void action() {
            if (aeroporto_m != null) {
                MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP), MessageTemplate.MatchContent(aeroporto_m.getPrefixo()));

                ACLMessage msg = myAgent.receive(mt);
                if (msg != null) {
                    String title = msg.getContent();
                    ACLMessage reply = msg.createReply();

                    System.out.println(controlador.getNome() + ": Sou seu controlador!");
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("Serei seu Controlador");
                    try {
                        reply.setContentObject(controlador);
                    } catch (IOException ex) {
                        Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    myAgent.send(reply);
                }
            } else {
                block();
            }

        }

    }

    /**
     * Classe para responder aos requerimentos de controladores que precisem de
     * um aeroporto pra controlar, retonar sim ou não para a requisição
     */
    private class RecebePropostaDecolar extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                    MessageTemplate.MatchConversationId("proposta-piloto-decolar")
            );
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {

                // CFP Message received. Process it
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                reply.setPerformative(ACLMessage.INFORM);

                System.out.println(controlador.getNome() + ": Pode decolar");
                myAgent.send(reply);
            } else {
                block();
            }
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

//            estacoesMeteorologicas.clear();
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                for (int i = 0; i < result.length; ++i) {
                    System.out.println("Estacao: " + result[i].getName());
//                    estacoesMeteorologicas.add(result[i].getName());
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }

            myAgent.addBehaviour(new ConsultaClima());
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
