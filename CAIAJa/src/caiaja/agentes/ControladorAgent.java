/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.agentes;

import caiaja.model.Aeroporto;
import caiaja.model.Aviao;
import caiaja.model.Controlador;
import caiaja.ontologia.CAIAJaOntologia;
import caiaja.ontologia.predicados.ControladoPor;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
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

                addBehaviour(new RequisicoesDePropostas());
                addBehaviour(new VerificaOntologia(this, 2000));
            }
        }
    }

    protected void takeDown() {
        System.out.println("Controlador " + controlador.getNome() + " saindo de operação.");
    }

    private class VerificaOntologia extends TickerBehaviour {

        boolean done = false;

        public VerificaOntologia(Agent myAgent, long milis) {
            super(myAgent, milis);
        }

        @Override
        protected void onTick() {

            if (aeroporto_m != null) {
                System.out.println(controlador.getNome()+": Verificando Ontologia ");
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
                    e.printStackTrace();
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
            switch (estado) {
                case 0: {
                    System.out.println(controlador.getNome() + ": Estado 0");
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
                    System.out.println(controlador.getNome() + ": Estado 1");
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
                    System.out.println(controlador.getNome() + ": Estado 2");

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
                    System.out.println(controlador.getNome() + ": Estado 3");

                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
//                            try {
                            aeroporto = reply.getSender();
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
                    System.out.println(controlador.getNome() + ": Estado 4");
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
