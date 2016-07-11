/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.agentes;

import caiaja.CAIAJa;
import caiaja.model.Abastecedor;
import caiaja.model.Aeroporto;
import caiaja.model.Aviao;
import caiaja.model.Bombeiro;
import caiaja.model.Controlador;
import caiaja.model.Piloto;
import caiaja.model.Pista;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fosa
 */
public class AeroportoAgent extends Agent {

    Aeroporto aeroporto_modelo;
    AID controlador_agente;
    AID bombeiro_agente;
    AID abastecedor_agente;

    public Aeroporto getAeroporto() {
        return aeroporto_modelo;
    }

    public void setAeroporto(Aeroporto aeroporto) {
        this.aeroporto_modelo = aeroporto;
    }

    protected void setup() {
        Object[] args = getArguments();

        aeroporto_modelo = new Aeroporto();

        if (args != null) {
            if (args.length > 0) {
                try {
                    String[] strargs = ((String) args[0]).split("&");
                    if (strargs.length > 0) {
                        aeroporto_modelo.setPrefixo(strargs[0]);
                    }
                    if (strargs.length > 1) {
                        aeroporto_modelo.setNome(strargs[1]);
                    }
                } catch (Exception e) {

                }
            }
            if (args.length > 1) {
                String strargs = ((String) args[1]);
                aeroporto_modelo.setPrefixo(strargs);
            }
            if (args.length > 2) {
                String strargs = ((String) args[2]);
                aeroporto_modelo.setNome(strargs);
            }
            if (args.length > 3) {
                try {
                    int intargs = Integer.parseInt((String) args[2]);
                    aeroporto_modelo.addPista(new Pista(intargs));
                } catch (Exception e) {

                }
            }
            System.out.println("Aeroporto " + aeroporto_modelo.getNome() + " operando");

//            String numero = JOptionPane.showInputDialog("Numero de Aeronaves em  " + aeroporto_modelo.getNome() + "?");
            String numero = "10";

            try {
                int naeronaves = Integer.parseInt(numero);
                for (int i = 0; i < naeronaves; i++) {

                    char s1 = (char) (Math.random() * 26 + 65);
                    char s2 = (char) (Math.random() * 26 + 65);
                    char s3 = (char) (Math.random() * 26 + 65);

                    Aviao av = new Aviao("PT-" + s1 + s2 + s3);
                    aeroporto_modelo.addAviao(av);
                }
            } catch (Exception e) {
                char s1 = (char) (Math.random() * 26 + 65);
                char s2 = (char) (Math.random() * 26 + 65);
                char s3 = (char) (Math.random() * 26 + 65);

                Aviao av = new Aviao("PT-" + s1 + s2 + s3);
                aeroporto_modelo.addAviao(av);
            }

            CAIAJa.registrarServico(this, "Aeroporto", aeroporto_modelo.getPrefixo());

            CAIAJa.addAeroporto(aeroporto_modelo);

//            addBehaviour(new RequisicoesDePropostas());
            addBehaviour(new RequisicoesDePropostasControladores());
            addBehaviour(new RequisicoesDePropostasPilotos());
            addBehaviour(new RequisicoesDePropostasBombeiros());

            addBehaviour(new PropostaControlar());
            addBehaviour(new PropostaPilotar());
            addBehaviour(new PropostaBombeiros());

            addBehaviour(new RequisicoesDePropostasAbastecedor());
            addBehaviour(new PropostaAbastecedor());

            addBehaviour(new RecebeAtualizacao());
            addBehaviour(new RecebeIncendioExtinto());
        }

    }

    @Override
    protected void takeDown() {
        System.out.println("Aeroporto " + aeroporto_modelo.getNome() + " saindo de operação.");

        CAIAJa.removeAeroporto(aeroporto_modelo);
    }

    /**
     * Classe para responder aos requerimentos de controladores que precisem de
     * um aeroporto_modelo pra controlar, retonar sim ou não para a requisição
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

                if (aeroporto_modelo.getControlador() == null) {
                    System.out.println("Aeroporto " + aeroporto_modelo.getNome() + ": Preciso de um controlador");
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("Preciso de Cointrolador");
                } else {
                    System.out.println("Aeroporto " + aeroporto_modelo.getNome() + ": já tenho um controlador");
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
     * um aeroporto_modelo pra controlar, retonar sim ou não para a requisição
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

                if (aeroporto_modelo.getQuantidadeAvioes() > 0) {
                    System.out.println("Aeroporto " + aeroporto_modelo.getNome() + ": tenho Aeronaves");
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("Tenho aeronaves");
                    try {
                        reply.setContentObject(aeroporto_modelo);
                    } catch (IOException ex) {
                        Logger.getLogger(AeroportoAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    System.out.println("Aeroporto " + aeroporto_modelo.getNome() + ": não tenho aeronaves disponíveis");
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

                if (aeroporto_modelo.getControlador() == null) {
                    try {
                        aeroporto_modelo.setControlador((Controlador) msg.getContentObject());
                        controlador_agente = msg.getSender();

                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContentObject(aeroporto_modelo);
                        System.out.println("Aeroporto " + aeroporto_modelo.getNome() + ": controlado por " + msg.getSender().getName());
                    } catch (UnreadableException ex) {
                        System.out.println("Aeroporto " + aeroporto_modelo.getNome() + ": erro na msg");
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("error-msg");
                    } catch (IOException ex) {
                        Logger.getLogger(AeroportoAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    System.out.println("Aeroporto " + aeroporto_modelo.getNome() + ": arrumou um controlador neste tempo");
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

                Aviao aviao = aeroporto_modelo.retiraAviao(0);
                if (aviao != null) {
                    try {
                        Piloto pil = (Piloto) msg.getContentObject();

                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContentObject(aviao);
                        System.out.println("Aeroporto " + aeroporto_modelo.getNome() + ": aviao " + aviao.getPrefixo() + " para " + pil.getNome());

                    } catch (UnreadableException ex) {
                        System.out.println("Aeroporto " + aeroporto_modelo.getNome() + ": erro na msg");
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("error-msg");
                    } catch (IOException ex) {
                        Logger.getLogger(AeroportoAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    System.out.println("Aeroporto " + aeroporto_modelo.getNome() + ": pegaram o aviao neste tempo");
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
     * um aeroporto_modelo pra controlar, retonar sim ou não para a requisição
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

                if (aeroporto_modelo.getBombeiro() == null) {
                    System.out.println("Aeroporto " + aeroporto_modelo.getNome() + ": tenho vaga pra Bombeiro");
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("Tenho vaga");
                    try {
                        reply.setContentObject(aeroporto_modelo);
                    } catch (IOException ex) {
                        Logger.getLogger(AeroportoAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    System.out.println("Aeroporto " + aeroporto_modelo.getNome() + ": não tenho vagas pra Bombeiro diposnível");
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
                ACLMessage reply = msg.createReply();

                if (aeroporto_modelo.getBombeiro() == null) {
                    try {
                        Bombeiro bombeiro = (Bombeiro) msg.getContentObject();

                        aeroporto_modelo.setBombeiro(bombeiro);
                        bombeiro_agente = msg.getSender();

                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContentObject(aeroporto_modelo);
                        System.out.println("Aeroporto " + aeroporto_modelo.getNome() + ": Alista " + bombeiro.getNome() + " no Corpo de Bombeiros");
                    } catch (UnreadableException ex) {
                        System.out.println("Aeroporto " + aeroporto_modelo.getNome() + ": erro na msg");
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("error-msg");
                    } catch (IOException ex) {
                        Logger.getLogger(AeroportoAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    System.out.println("Aeroporto " + aeroporto_modelo.getNome() + ": a vaga já foi preenchida");
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
     * um aeroporto_modelo pra controlar, retonar sim ou não para a requisição
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

                if (aeroporto_modelo.getAbastecedor() == null) {
                    System.out.println("Aeroporto " + aeroporto_modelo.getNome() + ": tenho vaga");
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("Tenho vaga");
                    try {
                        reply.setContentObject(aeroporto_modelo);
                    } catch (IOException ex) {
                        Logger.getLogger(AeroportoAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    System.out.println("Aeroporto " + aeroporto_modelo.getNome() + ": não tenho vagas diposníveis");
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

//                Aviao av = aeroporto_modelo.retiraAviao(0);
//                if (av != null) {
                try {

                    Abastecedor abastecedor = (Abastecedor) msg.getContentObject();

                    //aeroporto.setBombeiro(bombeiro);
                    aeroporto_modelo.setAbastecedor(abastecedor);

                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContentObject(aeroporto_modelo);
                    System.out.println("Aeroporto " + aeroporto_modelo.getNome() + ": aviao para " + abastecedor.getNome());

                } catch (UnreadableException ex) {
                    System.out.println("Aeroporto " + aeroporto_modelo.getNome() + ": erro na msg");
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("error-msg");
                } catch (IOException ex) {
                    Logger.getLogger(AeroportoAgent.class.getName()).log(Level.SEVERE, null, ex);
                }
//                } else {
//                    System.out.println("Aeroporto " + aeroporto_modelo.getNome() + ": pegaram o aviao neste tempo");
//                    reply.setPerformative(ACLMessage.FAILURE);
//                    reply.setContent("not-available");
//                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private class RecebeAtualizacao extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("atualiza-controlador-aeroporto")
            );
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                try {
                    Object obj = msg.getContentObject();

                    if (obj.getClass() == Controlador.class) {
                        Controlador controaldor = (Controlador) obj;
                        aeroporto_modelo.setControlador(controaldor);
                    } else if (obj.getClass() == Bombeiro.class) {
                        Bombeiro bombeiro = (Bombeiro) obj;
                        aeroporto_modelo.setBombeiro(bombeiro);
                    } else if (obj.getClass() == Abastecedor.class) {
                        Abastecedor abastecedor = (Abastecedor) obj;
                        aeroporto_modelo.setAbastecedor(abastecedor);
                    }

                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContentObject(aeroporto_modelo);

                } catch (UnreadableException ex) {
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("error-msg");
                } catch (IOException ex) {
                    Logger.getLogger(AeroportoAgent.class.getName()).log(Level.SEVERE, null, ex);
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private class RecebeIncendioExtinto extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE),
                    MessageTemplate.MatchConversationId("incendio-extinto")
            );
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String title = msg.getContent();
                ACLMessage reply = new ACLMessage(ACLMessage.INFORM);

                reply.setConversationId("incendio-extinto");
                reply.addReceiver(controlador_agente);

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
            System.out.println("Aeroporto: " + aeroporto_modelo.getNome());
            if (aeroporto_modelo.getControlador() != null) {
                System.out.println("Controlador: " + aeroporto_modelo.getControlador().getNome());
            }
        }

    }

}
