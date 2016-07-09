/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.agentes;

import caiaja.model.Abastecedor;
import caiaja.model.Aeroporto;
import caiaja.model.Aviao;
import caiaja.model.Bombeiro;
import caiaja.model.Controlador;
import caiaja.model.Piloto;
import caiaja.model.Pista;
import jade.content.ContentElementList;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Done;
import jade.content.onto.basic.TrueProposition;
import jade.core.Agent;
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
import jade.proto.SimpleAchieveREResponder;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

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

//            String numero = JOptionPane.showInputDialog("Numero de Aeronaves em  " + aeroporto.getNome() + "?");
            String numero = "3";

            try {
                int naeronaves = Integer.parseInt(numero);
                for (int i = 0; i < naeronaves; i++) {

                    char s1 = (char) (Math.random() * 26 + 65);
                    char s2 = (char) (Math.random() * 26 + 65);
                    char s3 = (char) (Math.random() * 26 + 65);

                    Aviao av = new Aviao("PT-" + s1 + s2 + s3);
                    aeroporto.addAviao(av);
                }
            } catch (Exception e) {
                char s1 = (char) (Math.random() * 26 + 65);
                char s2 = (char) (Math.random() * 26 + 65);
                char s3 = (char) (Math.random() * 26 + 65);

                Aviao av = new Aviao("PT-" + s1 + s2 + s3);
                aeroporto.addAviao(av);
            }

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

//            addBehaviour(new RequisicoesDePropostas());
            addBehaviour(new RequisicoesDePropostasControladores());
            addBehaviour(new RequisicoesDePropostasPilotos());
            addBehaviour(new RequisicoesDePropostasBombeiros());
            addBehaviour(new PropostaControlar());
            addBehaviour(new PropostaPilotar());
            addBehaviour(new PropostaBombeiros());

            addBehaviour(new RequisicoesDePropostasAbastecedor());
            addBehaviour(new PropostaAbastecedor());
//            addBehaviour(new ImprimeNome(this, 2000));
        }

    }

    protected void takeDown() {
        System.out.println("Aeroporto " + aeroporto.getNome() + " saindo de operação.");
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
                    System.out.println("Aeroporto " + aeroporto.getNome() + ": Preciso de um controlador");
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("Preciso de Cointrolador");
                } else {
                    System.out.println("Aeroporto " + aeroporto.getNome() + ": já tenho um controlador");
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("ja tenho um controlador");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    /**
     * Classe para responder aos requerimentos de controladores que precisem de
     * um aeroporto pra controlar, retonar sim ou não para a requisição
     */
    private class RequisicoesDePropostasControladores extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.CFP),
                    MessageTemplate.MatchConversationId("proposta-controlador")
            );

            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                if (aeroporto.getControlador() == null) {
                    System.out.println("Aeroporto " + aeroporto.getNome() + ": Preciso de um controlador");
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("Preciso de Cointrolador");
                } else {
                    System.out.println("Aeroporto " + aeroporto.getNome() + ": já tenho um controlador");
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("ja tenho um controlador");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    /**
     * Classe para responder aos requerimentos de controladores que precisem de
     * um aeroporto pra controlar, retonar sim ou não para a requisição
     */
    private class RequisicoesDePropostasPilotos extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.CFP),
                    MessageTemplate.MatchConversationId("proposta-piloto")
            );

            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                if (aeroporto.getQuantidadeAvioes() > 0) {
                    System.out.println("Aeroporto " + aeroporto.getNome() + ": tenho Aeronaves");
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("Tenho aeronaves");
                    try {
                        reply.setContentObject(aeroporto);
                    } catch (IOException ex) {
                        Logger.getLogger(AeroportoAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    System.out.println("Aeroporto " + aeroporto.getNome() + ": não tenho aeronaves disponíveis");
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("sem avioes");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private class PropostaControlar extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                    MessageTemplate.MatchConversationId("proposta-controlador")
            );
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                if (aeroporto.getControlador() == null) {
                    try {
                        aeroporto.setControlador((Controlador) msg.getContentObject());

                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContentObject(aeroporto);
                        System.out.println("Aeroporto " + aeroporto.getNome() + ": controlado por " + msg.getSender().getName());
                    } catch (UnreadableException ex) {
                        System.out.println("Aeroporto " + aeroporto.getNome() + ": erro na msg");
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("error-msg");
                    } catch (IOException ex) {
                        Logger.getLogger(AeroportoAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    System.out.println("Aeroporto " + aeroporto.getNome() + ": arrumou um controlador neste tempo");
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private class PropostaPilotar extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                    MessageTemplate.MatchConversationId("proposta-piloto")
            );
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                Aviao av = aeroporto.retiraAviao(0);
                if (av != null) {
                    try {
                        Piloto pil = (Piloto) msg.getContentObject();

                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContentObject(av);
                        System.out.println("Aeroporto " + aeroporto.getNome() + ": aviao para " + pil.getNome());

                    } catch (UnreadableException ex) {
                        System.out.println("Aeroporto " + aeroporto.getNome() + ": erro na msg");
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("error-msg");
                    } catch (IOException ex) {
                        Logger.getLogger(AeroportoAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    System.out.println("Aeroporto " + aeroporto.getNome() + ": pegaram o aviao neste tempo");
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    /**
     * Classe para responder aos requerimentos de controladores que precisem de
     * um aeroporto pra controlar, retonar sim ou não para a requisição
     */
    private class RequisicoesDePropostasBombeiros extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.CFP),
                    MessageTemplate.MatchConversationId("proposta-bombeiro")
            );

            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                if (aeroporto.getQuantidadeAvioes() > 0) {
                    System.out.println("Aeroporto " + aeroporto.getNome() + ": tenho vaga");
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("Tenho vaga");
                    try {
                        reply.setContentObject(aeroporto);
                    } catch (IOException ex) {
                        Logger.getLogger(AeroportoAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    System.out.println("Aeroporto " + aeroporto.getNome() + ": não tenho vagas diposníveis");
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Sem vagas");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private class PropostaBombeiros extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                    MessageTemplate.MatchConversationId("proposta-bombeiro")
            );
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

//                Aviao av = aeroporto.retiraAviao(0);
//                if (av != null) {
                try {
                    Bombeiro bombeiro = (Bombeiro) msg.getContentObject();

                    aeroporto.setBombeiro(bombeiro);

                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContentObject(aeroporto);
                    System.out.println("Aeroporto " + aeroporto.getNome() + ": aviao para " + bombeiro.getNome());

                } catch (UnreadableException ex) {
                    System.out.println("Aeroporto " + aeroporto.getNome() + ": erro na msg");
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("error-msg");
                } catch (IOException ex) {
                    Logger.getLogger(AeroportoAgent.class.getName()).log(Level.SEVERE, null, ex);
                }
//                } else {
//                    System.out.println("Aeroporto " + aeroporto.getNome() + ": pegaram o aviao neste tempo");
//                    reply.setPerformative(ACLMessage.FAILURE);
//                    reply.setContent("not-available");
//                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    /**
     * Classe para responder aos requerimentos de controladores que precisem de
     * um aeroporto pra controlar, retonar sim ou não para a requisição
     */
    private class RequisicoesDePropostasAbastecedor extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.CFP),
                    MessageTemplate.MatchConversationId("proposta-abastecedor")
            );

            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                if (aeroporto.getQuantidadeAvioes() > 0) {
                    System.out.println("Aeroporto " + aeroporto.getNome() + ": tenho vaga");
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("Tenho vaga");
                    try {
                        reply.setContentObject(aeroporto);
                    } catch (IOException ex) {
                        Logger.getLogger(AeroportoAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    System.out.println("Aeroporto " + aeroporto.getNome() + ": não tenho vagas diposníveis");
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Sem vagas");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private class PropostaAbastecedor extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                    MessageTemplate.MatchConversationId("proposta-abastecedor")
            );
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

//                Aviao av = aeroporto.retiraAviao(0);
//                if (av != null) {
                try {

                    Abastecedor abastecedor = (Abastecedor) msg.getContentObject();

                    //aeroporto.setBombeiro(bombeiro);
                    aeroporto.setAbastecedor(abastecedor);

                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContentObject(aeroporto);
                    System.out.println("Aeroporto " + aeroporto.getNome() + ": aviao para " + abastecedor.getNome());

                } catch (UnreadableException ex) {
                    System.out.println("Aeroporto " + aeroporto.getNome() + ": erro na msg");
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("error-msg");
                } catch (IOException ex) {
                    Logger.getLogger(AeroportoAgent.class.getName()).log(Level.SEVERE, null, ex);
                }
//                } else {
//                    System.out.println("Aeroporto " + aeroporto.getNome() + ": pegaram o aviao neste tempo");
//                    reply.setPerformative(ACLMessage.FAILURE);
//                    reply.setContent("not-available");
//                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private class ImprimeNome extends TickerBehaviour {

        public ImprimeNome(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            System.out.println("Aeroporto: " + aeroporto.getNome());
            if (aeroporto.getControlador() != null) {
                System.out.println("Controlador: " + aeroporto.getControlador().getNome());
            }
        }

    }

}
