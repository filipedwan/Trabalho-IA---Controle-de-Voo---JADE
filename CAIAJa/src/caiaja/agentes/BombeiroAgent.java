/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.agentes;

import caiaja.CAIAJa;
import caiaja.model.Aeroporto;
import caiaja.model.Bombeiro;
import caiaja.model.Incendio;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
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

    private Bombeiro bombeiro_modelo;
    private Aeroporto aeroporto_modelo;
    private List<Incendio> lista_incendio_modelo;
    private AID aeroporto_agente;
    private boolean ativo;
    Thread combateIncendio;

    protected void setup() {
        Object[] args = getArguments();

        bombeiro_modelo = new Bombeiro();
        aeroporto_agente = null;
        aeroporto_modelo = null;
        ativo = false;
        lista_incendio_modelo = new ArrayList<>();

        if (args != null) {
            if (args.length > 0) {
                bombeiro_modelo.setNome((String) args[0]);

                System.out.println("Bombeiro " + bombeiro_modelo.getNome() + " aguardando trabalho");

                CAIAJa.registrarServico(this, "Bombeiro", bombeiro_modelo.getNome());

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
            if (aeroporto_modelo == null) {
                myAgent.addBehaviour(new BombeiroAgent.PropoeTrabalhar(CAIAJa.getServico(myAgent, "Aeroporto")));
            } else {
                System.out.println("Bombeiro " + bombeiro_modelo.getNome() + ": trabalhando em " + aeroporto_modelo.getNome());

//                System.out.println("Bombeiro " + bombeiro_modelo.getNome() + ": Ativando incendio em " + aeroporto_modelo.getNome());
//                Incendio incendio_modelo = new Incendio(5);
//
//                lista_incendio_modelo.add(incendio_modelo);
//                if (!ativo) {
//
//                    if (combateIncendio == null) {
//                        combateIncendio = new Thread(new CombateIncendio((BombeiroAgent) myAgent));
//                    } else if (!combateIncendio.isAlive()) {
//                        combateIncendio.stop();
//                        combateIncendio = new Thread(new CombateIncendio((BombeiroAgent) myAgent));
//                    }
//                    combateIncendio.start();
//
//                }
                block(1000);
            }
        }

    }

    private class PropoeTrabalhar extends Behaviour {

        List<AID> aeroportos;
        AID Escolhido;
        int estado = 0;
        private MessageTemplate mt; // The template to receive replies
        private int repliesCnt = 0;

        public PropoeTrabalhar(List<AID> aeroportos) {
            this.aeroportos = aeroportos;
        }

        @Override
        public void action() {
            System.out.println("Bombeiro " + bombeiro_modelo.getNome() + ": Trabalho bombeiro " + estado);
            switch (estado) {
                case 0: {
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                    for (AID aeroporto : aeroportos) {
                        System.out.println("Bombeiro " + bombeiro_modelo.getNome() + " --> " + aeroporto.getName());
                        cfp.addReceiver(aeroporto);
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
                                aeroporto_modelo = (Aeroporto) reply.getContentObject();
                            } catch (UnreadableException ex) {
                                Logger.getLogger(PilotoAgent.class.getName()).log(Level.SEVERE, null, ex);
                            }
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
                    ACLMessage msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    msg.addReceiver(Escolhido);
//                    controlar.setContent("Aceito controlar");
                    try {
                        msg.setContentObject(bombeiro_modelo);
                    } catch (IOException ex) {
                        Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    msg.setConversationId("proposta-bombeiro");
                    msg.setReplyWith("trabalhar" + System.currentTimeMillis());
                    myAgent.send(msg);
                    System.out.println("Bombeiro " + bombeiro_modelo.getNome() + " --> " + Escolhido.getName() + ": Aceito Trabalhar");

                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("proposta-bombeiro"),
                            MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                    estado = 3;
                    break;
                }
                case 3: {
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            aeroporto_agente = reply.getSender();
                            System.out.println("Bombeiro " + bombeiro_modelo.getNome() + ": trabalhando para o  " + aeroporto_agente.getLocalName());

                        } else {
                            System.out.println("Bombeiro " + bombeiro_modelo.getNome() + ": não foi contratado por " + Escolhido.getName() + " já conseguiu outro bombeiro");
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
     * Classe para anunciar fim do incendio
     */
    private class AnunciaIncendioExtinto extends OneShotBehaviour {

        public void action() {

            ACLMessage anuncio = new ACLMessage(ACLMessage.PROPAGATE);

            anuncio.setConversationId("incendio-extinto");
            anuncio.setContent("Incendio Extinto");

            System.err.println("bombeiro " + bombeiro_modelo.getNome() + ": Incendio extinto");
            anuncio.addReceiver(aeroporto_agente);

            myAgent.send(anuncio);
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

                ACLMessage reply = msg.createReply();

                try {
                    Incendio incendio_modelo = (Incendio) msg.getContentObject();

                    lista_incendio_modelo.add(incendio_modelo);
                    if (ativo) {
                        reply.setContent("Estou atendendo a outra ocorencia");
                        System.err.println("bombeiro " + bombeiro_modelo.getNome() + ": já estou combatendo o incendio");
                    } else {
                        if (combateIncendio == null) {
                            combateIncendio = new Thread(new CombateIncendio((BombeiroAgent) myAgent));
                        } else if (!combateIncendio.isAlive()) {
                            combateIncendio.stop();
                            combateIncendio = new Thread(new CombateIncendio((BombeiroAgent) myAgent));
                        }
                        combateIncendio.start();
                        reply.setContent("Estou pronto a caminho");
                        System.err.println("bombeiro " + bombeiro_modelo.getNome() + ": já estou a caminho do incendio");
                    }

                } catch (UnreadableException ex) {

                    reply.setContent("nao encontrei");
                }

                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private class CombateIncendio implements Runnable {

        BombeiroAgent myAgent;

        public CombateIncendio(BombeiroAgent bombeiro_agente) {
            this.myAgent = bombeiro_agente;
        }

        @Override
        public void run() {
            myAgent.ativo = true;
            while (myAgent.lista_incendio_modelo.size() > 0) {
                Incendio incendio_modelo = myAgent.lista_incendio_modelo.get(0);
                myAgent.lista_incendio_modelo.remove(0);

                while (incendio_modelo.combateIncendio() > 0) {
                    System.err.println("Combatendo o incendio (" + myAgent.lista_incendio_modelo.size() + ")");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(BombeiroAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                System.err.println("Apaguei um incendio");
            }
            myAgent.ativo = false;
            myAgent.addBehaviour(new AnunciaIncendioExtinto());
        }

    }

}
