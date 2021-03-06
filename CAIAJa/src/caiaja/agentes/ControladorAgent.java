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
import caiaja.model.Incendio;
import caiaja.model.Pista;
import caiaja.ontologia.CAIAJaOntologia;
import caiaja.ontologia.acoes.Decolar;
import caiaja.ontologia.acoes.PlanoDeVoo;
import caiaja.ontologia.acoes.Pousar;
import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
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
 * O ControladorAgent é capaz de se cadastrar no DFService e procurar 
 * emprego em um aeroporto. Além disso, ele é responsável por analisar
 * as propostas dos pilotos de decolagem e pouso.
 * Outrosim, ele deve verificar os alertas de acidentes e se posicianar
 * quando estes forem extintos.
 */
public class ControladorAgent extends Agent {

    Controlador controlador_modelo;
    Aeroporto aeroporto_modelo;

    List<PlanoDeVoo> fila_pilotos_pousar;
    List<PlanoDeVoo> fila_pilotos_decolar;
    List<AID> fila_propor_reabastecimento;

    boolean pistaOcupada;
    boolean buscandoAeroporto;
    boolean ancidente;

    AID aeroporto;
    List<Aviao> Avioes;

    public ControladorAgent() {
        Avioes = new ArrayList<Aviao>();
        fila_pilotos_pousar = new ArrayList<PlanoDeVoo>();
        fila_pilotos_decolar = new ArrayList<PlanoDeVoo>();
        fila_propor_reabastecimento = new ArrayList<AID>();
    }

    protected void setup() {
        Object[] args = getArguments();

        controlador_modelo = new Controlador();
        aeroporto = null;
        buscandoAeroporto = false;
        ancidente = false;
        if (args != null) {
            if (args.length > 0) {
                controlador_modelo.setNome((String) args[0]);

                System.out.println("Controlador " + controlador_modelo.getNome() + " operando");

                // Register the codec for the SL0 language
                getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL0);

                getContentManager().registerOntology(CAIAJaOntologia.getInstance());

                CAIAJa.registrarServico(this, "Controlador", controlador_modelo.getNome());

                addBehaviour(new BuscarEmprego(this, 2000));

                addBehaviour(new Contato());
                addBehaviour(new RecebePilotoConsultaControlador());

                addBehaviour(new RecebeLiberacaoDecolar());
                addBehaviour(new TratarPropostaDecolar());
                addBehaviour(new RecebeSucessoDecolagem());

                addBehaviour(new TratarPropostaPousar());
                addBehaviour(new RecebeLiberacaoPousar());
                addBehaviour(new RecebeSucessoPouso());

                addBehaviour(new RecebeAlertaAcidente());
                addBehaviour(new RecebeIncendioExtinto());

                addBehaviour(new ProcessaFilaDePousoEDecolagem(this));
            }
        }
    }

    @Override
    protected void takeDown() {
        System.out.println("Controlador " + controlador_modelo.getNome() + " saindo de operação.");
    }

    int Decolar(Aviao aviao, Aeroporto aero) {
        if (aeroporto_modelo.equals(aero)) {
            return 1;
        }
        return 0;
    }

    boolean Controla(Controlador con, Aeroporto aero) {
        if (controlador_modelo.equals(con) && aeroporto_modelo.equals(aero)) {
            return true;
        }
        return false;
    }

    private class BuscarEmprego extends TickerBehaviour {

        public BuscarEmprego(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {

            if (aeroporto == null && !buscandoAeroporto) {
                buscandoAeroporto = true;
                List<AID> aeroportos = CAIAJa.getServico(myAgent, "Aeroporto");

                myAgent.addBehaviour(new PropoeControlar(aeroportos));
            } else {
                block();
            }
        }

    }

    private class PropoeControlar extends Behaviour {

        List<AID> aeroportos;
        AID Escolhido;
        int estado = 0;
        private MessageTemplate mt; // The template to receive replies
        private int repliesCnt = 0;

        public PropoeControlar(List<AID> aeroportos) {
            this.aeroportos = aeroportos;
        }

        @Override
        public int onEnd() {
            buscandoAeroporto = false;
            return super.onEnd();
        }

        @Override
        public void action() {
//            System.out.println(controlador_modelo.getNome() + ": PropoeControlar " + estado);
            switch (estado) {
                case 0: {
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                    for (AID aerosporto : aeroportos) {
                        System.out.println(controlador_modelo.getNome() + " --> " + aerosporto.getLocalName() + ": Posso controlar?");
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
                        if (repliesCnt >= aeroportos.size()) {
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
                        controlar.setContentObject(controlador_modelo);
                    } catch (IOException ex) {
                        Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    controlar.setConversationId("proposta-controlador");
                    controlar.setReplyWith("controlar" + System.currentTimeMillis());
                    myAgent.send(controlar);
                    System.out.println(controlador_modelo.getNome() + " --> " + Escolhido.getLocalName() + ": Aceito Controlar");

                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("proposta-controlador"),
                            MessageTemplate.MatchInReplyTo(controlar.getReplyWith()));
                    estado = 3;
                    break;
                }
                case 3: {
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            aeroporto = reply.getSender();
                            try {
                                aeroporto_modelo = (Aeroporto) reply.getContentObject();
                            } catch (UnreadableException ex) {
                                Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            System.out.println(controlador_modelo.getNome() + ": Operando no aeroporto " + aeroporto_modelo.getPrefixo());

                        } else {
                            System.out.println(controlador_modelo.getNome() + ": não pode controlar " + Escolhido.getLocalName() + " já conseguiu outro controlador");
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
            if (aeroporto_modelo != null) {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
//                MessageTemplate mt2 = MessageTemplate.MatchContent(aeroporto_modelo.getPrefixo());
//                MessageTemplate mt = MessageTemplate.and(mt1, mt2);

                ACLMessage msg = myAgent.receive(mt);
                if (msg != null) {
                    String title = msg.getContent();
                    ACLMessage reply = msg.createReply();

                    try {
                        if (msg.getContentObject().getClass() == Decolar.class) {
                            System.err.println("Class decolar");
                        } else if (msg.getContentObject().getClass() == Pousar.class) {
                            System.err.println("Class Pousar");
                        } else {
                            System.err.println("Outra Class " + msg.getContentObject().getClass());

                        }
                    } catch (UnreadableException ex) {
                        Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    System.out.println(controlador_modelo.getNome() + ": Sou seu controlador!");
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("Serei seu Controlador");
                    try {
                        reply.setContentObject(controlador_modelo);
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

                System.out.println(controlador_modelo.getNome() + ": Pode decolar");
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    /**
     * Classe para responder as propostas do piloto que quer Pousar
     */
    private class TratarPropostaPousar extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
            MessageTemplate mt2 = MessageTemplate.MatchConversationId("proposta-piloto-pousar");
            MessageTemplate mt = MessageTemplate.and(mt1, mt2);

            ACLMessage msg = myAgent.receive(mt);

            boolean pistaMinima = false;

            if (msg != null) {
                ACLMessage reply = msg.createReply();

                PlanoDeVoo planoDeVoo = null;
                try {
                    planoDeVoo = (PlanoDeVoo) msg.getContentObject();

                    if (aeroporto_modelo.getPistas() != null) {
                        for (Pista pista : aeroporto_modelo.getPistas()) {
                            System.out.println("pista: " + pista.getTamanho());
                            if (pista.getTamanho() > planoDeVoo.getAviao().getPistaMinima()) {
                                pistaMinima = true;
                            }
                        }
                    }

                } catch (UnreadableException ex) {
                    Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (!pistaMinima) {
                    System.out.println("Controlador " + controlador_modelo.getNome() + ":  " + planoDeVoo.getAviao() + " pedido rejeitado pista muito curta para pouso");

                    reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                } else if (planoDeVoo != null) {
                    if (ancidente) {
                        System.out.println("Controlador " + controlador_modelo.getNome() + ":  " + planoDeVoo.getPiloto().getNome() + " Aeroporto fechado, Acidente na pista");

                        reply.setContent("negado acidente");

                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    } else if (!fila_pilotos_pousar.isEmpty()) {

                        System.out.println("Controlador " + controlador_modelo.getNome() + ":  " + planoDeVoo.getPiloto().getNome() + " pedido rejeitado outra aeronave esta pousando");

                        reply.setContent("negado aguarde");

                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    } else if (!fila_pilotos_decolar.isEmpty()) {

                        System.out.println("Controlador " + controlador_modelo.getNome() + ":  " + planoDeVoo.getPiloto().getNome() + " aguarde outra aeronave aeronave decolando");

                        reply.setContent("aguarde");

                        reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    } else {
//                        fila_pilotos_pousar.enqueue(conteudo);
                        System.out.println("Controlador " + controlador_modelo.getNome() + ": Informa que a pista está livre para pouso "
                                + "Piloto " + msg.getSender().getLocalName() + " pode aproximar para pouso");

                        reply.setContent("Pode aproximar");

                        reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);

                    }
                }
                myAgent.send(reply);
            } else {
//                block();
            }
        }
    }

    /**
     * Classe para responder ao clearance do piloto que vai pousar
     */
    private class RecebeLiberacaoPousar extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("liberacao-piloto-pousar")
            );
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();

                PlanoDeVoo planoDeVoo = null;
                try {
                    planoDeVoo = (PlanoDeVoo) msg.getContentObject();

                    if (!pistaOcupada) {
                        pistaOcupada = true;
                        reply.setPerformative(ACLMessage.CONFIRM);
                        System.out.println(controlador_modelo.getNome() + ": Pode pousar " + planoDeVoo.getAviao().getPrefixo());
                    } else {
//                                    planoDeVoo.setReplyWith(planoDeVoo.getIdplano());

                        reply.setPerformative(ACLMessage.REQUEST);
                        System.out.println(controlador_modelo.getNome() + ": Aguarde");

                        fila_pilotos_pousar.add(planoDeVoo);
                    }

                } catch (UnreadableException ex) {
                    Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
                }

                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    /**
     * Classe para responder ao sucesso do piloto
     */
    private class RecebeSucessoPouso extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("sucesso-piloto-pouso")
            );
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();

                if (pistaOcupada) {
                    pistaOcupada = false;
                }

                reply.setConversationId("abastecer-piloto");
                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent("Precisa abastecer?");
                System.out.println(controlador_modelo.getNome() + ": precisa reabastecer " + msg.getSender().getLocalName() + "?");

                myAgent.send(reply);

                MessageTemplate mt1 = MessageTemplate.MatchReplyWith(reply.getReplyWith());
                MessageTemplate mt2 = MessageTemplate.MatchConversationId(reply.getConversationId());
                MessageTemplate mt0 = MessageTemplate.and(mt1, mt2);

                fila_propor_reabastecimento.add(msg.getSender());
//                addBehaviour(new RecebeRequisicaoAbastecer(msg.getSender(), mt0));
            } else {
                block();
            }
        }
    }

    /**
     * Classe para responder ao sucesso do piloto
     */
    private class PropoeReabastecimentoDepoisDoPouso extends CyclicBehaviour {

        @Override
        public void action() {

            if (fila_propor_reabastecimento.size() > 0) {
                AID piloto = fila_propor_reabastecimento.remove(0);

                ACLMessage propoeReabastecer = new ACLMessage(ACLMessage.PROPOSE);
                propoeReabastecer.setConversationId("propor-abastecer-piloto");
                propoeReabastecer.setContent("Precisa abastecer?");
                System.out.println(controlador_modelo.getNome() + ": precisa reabastecer " + piloto.getLocalName() + "?");

                myAgent.send(propoeReabastecer);

                MessageTemplate mt1 = MessageTemplate.MatchReplyWith(propoeReabastecer.getReplyWith());
                MessageTemplate mt2 = MessageTemplate.MatchConversationId(propoeReabastecer.getConversationId());
                MessageTemplate mt = MessageTemplate.and(mt1, mt2);

                ACLMessage msg = myAgent.receive(mt);

                int i = 0;
                while (msg == null && i < 100) {
                    msg = myAgent.receive(mt);
                    block(100);
                    i++;
                }

                if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {

                    ACLMessage informaAbastecedor = new ACLMessage(ACLMessage.INFORM);

                    System.out.println(controlador_modelo.getNome() + ": informa " + " reabastecer " + piloto.getLocalName() + "?");

                    myAgent.send(msg);
                }

            }
            block();
        }
    }

    /**
     * Classe para responder as propostas do piloto que quer decolar
     */
    private class TratarPropostaDecolar extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
            MessageTemplate mt2 = MessageTemplate.MatchConversationId("proposta-piloto-decolar");
            MessageTemplate mt = MessageTemplate.and(mt1, mt2);

            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                ACLMessage reply = msg.createReply();

                PlanoDeVoo planoDeVoo = null;
                try {
                    planoDeVoo = (PlanoDeVoo) msg.getContentObject();

                } catch (UnreadableException ex) {
                    Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (planoDeVoo.getAeroportoOrigem() == aeroporto_modelo) {
                    System.out.println("======================== Iguais ===============================");
                }
                if (planoDeVoo != null) {

                    if (ancidente) {
                        System.out.println("Controlador " + controlador_modelo.getNome() + ":  " + planoDeVoo.getPiloto().getNome() + " Aeroporto fechado, Acidente na pista");

                        reply.setContent("negado acidente");

                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    } else if (!fila_pilotos_pousar.isEmpty()) {

                        System.out.println("Controlador " + controlador_modelo.getNome() + ":  " + planoDeVoo.getPiloto().getNome() + " pedido rejeitado outra aeronave esta pousando");

                        reply.setContent("negado aguarde");

                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    } else if (!fila_pilotos_decolar.isEmpty()) {

                        System.out.println("Controlador " + controlador_modelo.getNome() + ":  " + planoDeVoo.getPiloto().getNome() + " aguarde outra aeronave estão decolando");

                        reply.setContent("aguarde");

                        reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    } else {
//                        fila_pilotos_decolar.enqueue(conteudo);
                        System.out.println("Controlador " + controlador_modelo.getNome() + ": "
                                + "Decolagem aceita posicione-se na pista"
                                + " piloto " + msg.getSender().getLocalName());

                        reply.setContent("Posicione para decolar");

                        reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);

                    }
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    /**
     * Classe para responder ao clearance do piloto que vai decolar
     */
    private class RecebeLiberacaoDecolar extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("liberacao-piloto-decolar")
            );
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();

                PlanoDeVoo planoDeVoo = null;
                try {
                    planoDeVoo = (PlanoDeVoo) msg.getContentObject();

                } catch (UnreadableException ex) {
                    Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
                }

                if (!pistaOcupada && !ancidente) {
                    pistaOcupada = true;
                    reply.setPerformative(ACLMessage.CONFIRM);
                    System.out.println(controlador_modelo.getNome() + ": Pode decolar " + planoDeVoo.getAviao());
                } else {
                    System.err.println("planoDeVoo id: " + planoDeVoo.getIdplano());
//                    planoDeVoo.setIdplano(reply.getInReplyTo());
                    reply.setPerformative(ACLMessage.REQUEST);
                    System.out.println(controlador_modelo.getNome() + ": Aguarde");
                    fila_pilotos_decolar.add(planoDeVoo);
                }

                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    /**
     * Classe para responder ao clearance do piloto que vai decolar
     */
    private class RecebeSucessoDecolagem extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("sucesso-piloto-decolar")
            );
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();

                if (pistaOcupada) {
                    pistaOcupada = false;
                    reply.setPerformative(ACLMessage.CONFIRM);
                    System.out.println(controlador_modelo.getNome() + ": Positivo " + msg.getSender().getLocalName());
                }

                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    /**
     * Classe para Tratar a fila de pousos e decolagens
     */
    private class ProcessaFilaDePousoEDecolagem extends CyclicBehaviour {

        public ProcessaFilaDePousoEDecolagem(Agent myAgent) {
            super(myAgent);
        }

        @Override
        public void action() {
//            System.err.println("Fila de Pousos e decolagens\n"
//                    + "ancidente(" + ancidente + ")\n"
//                    + "fila_pilotos_pousar.isEmpty(" + fila_pilotos_pousar.isEmpty() + ")\n"
//                    + "fila_pilotos_decolar.isEmpty(" + fila_pilotos_decolar.isEmpty() + ")");
            if (ancidente) {
                if (fila_pilotos_pousar.size() > 0) {
                    PlanoDeVoo pouso = fila_pilotos_pousar.remove(0);

                    System.out.println("Controlador " + controlador_modelo.getNome() + ": Aeroporto fechado por acidente!");

                    ACLMessage confirmaLiberacao = new ACLMessage(ACLMessage.DISCONFIRM);
                    confirmaLiberacao.addReceiver(pouso.getActor());
                    confirmaLiberacao.setConversationId("liberacao-piloto-pousar");
                    confirmaLiberacao.setInReplyTo(pouso.getIdplano());

                    myAgent.send(confirmaLiberacao);
                } else if (fila_pilotos_decolar.size() > 0) {
                    PlanoDeVoo pouso = fila_pilotos_pousar.remove(0);

                    System.out.println("Controlador " + controlador_modelo.getNome() + ": Aeroporto fechado por acidente!");

                    ACLMessage confirmaLiberacao = new ACLMessage(ACLMessage.DISCONFIRM);
                    confirmaLiberacao.addReceiver(pouso.getActor());
                    confirmaLiberacao.setConversationId("liberacao-piloto-decolar");
                    confirmaLiberacao.setInReplyTo(pouso.getIdplano());

                    myAgent.send(confirmaLiberacao);
                }
            } else if (!fila_pilotos_pousar.isEmpty() && !pistaOcupada) {
                pistaOcupada = true;
                PlanoDeVoo pouso = fila_pilotos_pousar.remove(0);
                System.out.println("Controlador " + controlador_modelo.getNome() + ": Chamar para pouso " + pouso.getAviao().getPrefixo());

                ACLMessage confirmaLiberacao = new ACLMessage(ACLMessage.CONFIRM);
                confirmaLiberacao.addReceiver(pouso.getActor());
                confirmaLiberacao.setConversationId("liberacao-piloto-pousar");
                confirmaLiberacao.setInReplyTo(pouso.getIdplano());
                confirmaLiberacao.setReplyWith("pouso" + System.currentTimeMillis());

                myAgent.send(confirmaLiberacao);

                MessageTemplate mt1 = MessageTemplate.MatchConversationId("confirma-liberacao-piloto");
                MessageTemplate mt2 = MessageTemplate.MatchInReplyTo(confirmaLiberacao.getReplyWith());
                MessageTemplate mt = MessageTemplate.and(mt1, mt2);

                ACLMessage reply = myAgent.receive(mt);

                int count = 0;
                while (reply == null && count < 10) {
                    block(100);
                    reply = myAgent.receive(mt);
                    count++;
                }
                if (reply != null) {
                    if (reply.getPerformative() == ACLMessage.INFORM) {
                        pistaOcupada = true;
                    }
                } else {
                    ACLMessage cancelarLiberacao = new ACLMessage(ACLMessage.CANCEL);
                    cancelarLiberacao.addReceiver(pouso.getActor());
                    cancelarLiberacao.setConversationId("liberacao-piloto-pousar");
                    cancelarLiberacao.setInReplyTo(pouso.getIdplano());
                    cancelarLiberacao.setReplyWith("pouso" + System.currentTimeMillis());

                    myAgent.send(cancelarLiberacao);
                }

            } else if (!fila_pilotos_decolar.isEmpty() && !pistaOcupada) {
                pistaOcupada = true;
                PlanoDeVoo decolagem = fila_pilotos_decolar.remove(0);
                System.out.println("Controlador " + controlador_modelo.getNome() + ": Chama " + decolagem.getAviao() + " para decolagem ");

                ACLMessage confirmaLiberacao = new ACLMessage(ACLMessage.CONFIRM);
                confirmaLiberacao.addReceiver(decolagem.getActor());
                confirmaLiberacao.setConversationId("liberacao-piloto-decolar");
                confirmaLiberacao.setInReplyTo(decolagem.getIdplano());
                confirmaLiberacao.setSender(getAID());
                
                myAgent.send(confirmaLiberacao);
            } else {
                block(2000);
            }

        }
    }

    /**
     * Classe para responder aos requerimentos de pilotos que precisem de um
     * saber quem é o controlador_modelo
     */
    private class RecebePilotoConsultaControlador extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF),
                    MessageTemplate.MatchConversationId("piloto-consulta-Controlador")
            );
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {

                ACLMessage reply = msg.createReply();

                Aeroporto aeroporto_consulta;
                try {
                    aeroporto_consulta = (Aeroporto) msg.getContentObject();

                    if (aeroporto_modelo.equals(aeroporto_consulta)) {
                        System.out.println(controlador_modelo.getNome() + ": Sou seu Controlador " + msg.getSender().getLocalName());
                        try {
                            reply.setContentObject(controlador_modelo);
                        } catch (IOException ex) {
                            Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        reply.setPerformative(ACLMessage.CONFIRM);
                    } else {
                        reply.setPerformative(ACLMessage.DISCONFIRM);

                    }

                } catch (UnreadableException ex) {
                    reply.setPerformative(ACLMessage.FAILURE);
                }

                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    /**
     * Classe para responder aos requerimentos de pilotos que precisem de um
     * saber quem é o controlador_modelo
     */
    private class RecebeAlertaAcidente extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                Object obj = null;

                try {
                    obj = msg.getContentObject();

                    if (obj.getClass() == Incendio.class) {

                        if (aeroporto_modelo.getBombeiro() == null) {
                            addBehaviour(new AtualizaDadosAeroporto());
                        }

                        if (aeroporto_modelo.getBombeiro() != null) {
                            ACLMessage alertaBombeiros = new ACLMessage(ACLMessage.INFORM);
                            Incendio incendio = (Incendio) obj;
                            System.out.println(controlador_modelo.getNome() + ": Alerta bombeiro " + aeroporto_modelo.getBombeiro().getNome());
                            try {
                                alertaBombeiros.setContentObject(incendio);
                            } catch (IOException ex) {
                                Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
                            }

                            List<AID> bombeiros = CAIAJa.getAgent(myAgent, aeroporto_modelo.getBombeiro().getNome(), "Bombeiro");

                            for (AID bombeiro : bombeiros) {
                                alertaBombeiros.addReceiver(bombeiro);
                            }

                            alertaBombeiros.setConversationId("Incendio");
                            myAgent.send(alertaBombeiros);

                            ancidente = true;
                        }
                    }

                } catch (UnreadableException ex) {
                }

            } else {
                block();
            }
        }
    }

    /**
     * Classe para responder aos requerimentos de pilotos que precisem de um
     * saber quem é o controlador_modelo
     */
    private class AtualizaDadosAeroporto extends OneShotBehaviour {

        public void action() {
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);

            try {
                msg.setConversationId("atualiza-controlador-aeroporto");
                msg.setContentObject(controlador_modelo);

                msg.setReplyWith("atualiza" + System.currentTimeMillis());
                msg.addReceiver(aeroporto);
                myAgent.send(msg);

                MessageTemplate mt1 = MessageTemplate.MatchConversationId("atualiza-controlador-aeroporto");
                MessageTemplate mt2 = MessageTemplate.MatchInReplyTo(msg.getReplyWith());
                MessageTemplate mt = MessageTemplate.and(mt1, mt2);

                ACLMessage reply = myAgent.receive(mt);

                while (reply == null) {
                    block();
                    reply = myAgent.receive(mt);
                }

                if (reply.getPerformative() == ACLMessage.INFORM) {
                    aeroporto_modelo = (Aeroporto) reply.getContentObject();
                }
            } catch (UnreadableException ex) {
                Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    private class RecebeIncendioExtinto extends CyclicBehaviour {

        public void action() {
            if (ancidente) {
                MessageTemplate mt = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchConversationId("incendio-extinto")
                );
                ACLMessage msg = myAgent.receive(mt);
                if (msg != null) {
                    ancidente = false;
                } else {
                    block();
                }
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

}
