package caiaja.agentes;

import caiaja.CAIAJa;
import caiaja.model.Abastecedor;
import caiaja.model.Aeroporto;
import caiaja.model.Aviao;
import caiaja.model.Combustivel;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Laian
 * O AbastecedirAgent é capaz de se cadastrar no DFService procurar 
 * emprego em um Aeroporto e atender aos pedidos de abastecimentos
 * solicitados quando estes estiverem inativos.
 */
public class AbastecedorAgent extends Agent {

    private Abastecedor abastecedorModel;
    private boolean ativo;
    private AID aeroportoAgent;
    private Aeroporto aeroportoModel;
    private boolean pronunciamento;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        abastecedorModel = new Abastecedor();
        ativo = false;
        pronunciamento = false;
        aeroportoAgent = null;
        aeroportoModel = null;
        if (args != null) {
            if (args.length > 0) {
                abastecedorModel.setNome((String) args[0]);

                System.out.println("Abastecedor " + abastecedorModel.getNome() + " procurar trabalho..");

                CAIAJa.registrarServico(this, "Abastecedor", abastecedorModel.getNome());

                addBehaviour(new AbastecedorAgent.BuscaAtividade(this, 5000));
                addBehaviour(new RecebePedidosDeAbastecimento(this));
            }
        }
    }

    private class BuscaAtividade extends TickerBehaviour {

        public BuscaAtividade(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            if (aeroportoModel == null) {
                //if (aeroportoAgent == null) {
                List<AID> aeroportos = CAIAJa.getServico(myAgent, "Aeroporto");
                addBehaviour(new AbastecedorAgent.PropoeTrabalhar(aeroportos));
            } else {

                if (!pronunciamento) {
                    System.out.println("Abastecedor " + abastecedorModel.getNome() + ": trabalhando em " + aeroportoModel.getNome());
                    pronunciamento = true;
                }
                //TODO: Atuação do agente Abastecedor
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
            System.out.println("Abastecedor " + abastecedorModel.getNome() + ": Trabalho Abastecedor " + estado);
            switch (estado) {
                case 0: {
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                    for (AID aeroporto : aeroportos) {
                        System.out.println("Abastecedor " + abastecedorModel.getNome() + " --> " + aeroporto.getLocalName() + ": Quero Trabalhar");
                        cfp.addReceiver(aeroporto);
                    }
                    cfp.setConversationId("proposta-abastecedor");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());
                    cfp.setContent("Precisa de Abastecedor?");
                    myAgent.send(cfp);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("proposta-abastecedor"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    estado = 1;
                    break;
                }
                case 1: {
                    ACLMessage reply = myAgent.receive(mt);

                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            Escolhido = reply.getSender();
                            try {
                                aeroportoModel = (Aeroporto) reply.getContentObject();
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
                    try {
                        msg.setContentObject(abastecedorModel);
                    } catch (IOException ex) {
                        System.err.println("Erro de IO - Não foi possível Serializar o abastecedorModel");
                    }
                    msg.setConversationId("proposta-abastecedor");
                    msg.setReplyWith("trabalhar" + System.currentTimeMillis());
                    myAgent.send(msg);
                    System.out.println("Abastecedor " + abastecedorModel.getNome() + " --> " + Escolhido.getLocalName() + ": Aceito Trabalhar");

                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("proposta-abastecedor"),
                            MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                    estado = 3;
                    break;
                }
                case 3: {
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            aeroportoAgent = reply.getSender();
                            System.out.println("Abastecedor " + abastecedorModel.getNome() + ": trabalhando para o  " + aeroportoAgent.getLocalName());

                        } else {
                            System.out.println("Abastecedor " + abastecedorModel.getNome() + ": não foi contratado por " + Escolhido.getLocalName() + " já conseguiu outro abastecedor");
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
    private class RecebePedidosDeAbastecimento extends CyclicBehaviour {

        public RecebePedidosDeAbastecimento(Agent a) {
            super(a);
        }

        @Override
        public void action() {
//            MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            MessageTemplate mt = MessageTemplate.MatchConversationId("proposta-piloto-abastecedor");
//            MessageTemplate mt = MessageTemplate.and(mt1, mt2);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                if (msg.getPerformative() == ACLMessage.CFP) {
                    Object obj = null;
                    try {
                        obj = msg.getContentObject();

                        if (obj.getClass() == Aviao.class) {

                            Aviao aviao = (Aviao) obj;

                            reply.setPerformative(ACLMessage.PROPOSE);
                            reply.setContentObject(abastecedorModel);

                            System.out.println(abastecedorModel.getNome() + ": Posso lhe abastecer " + aviao.getPrefixo());

                        }

                    } catch (UnreadableException ex) {
                        reply.setPerformative(ACLMessage.REFUSE);

                    } catch (IOException ex) {
                        Logger.getLogger(AbastecedorAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }

                } else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    Object obj = null;
                    try {
                        obj = msg.getContentObject();

                        if (obj.getClass() == Aviao.class) {

                            Aviao aviao = (Aviao) obj;

                            Combustivel com = new Combustivel(aviao.getTamanhoTanque() - aviao.getCombustivel());

                            reply.setPerformative(ACLMessage.INFORM);
                            reply.setContentObject(com);

                            System.out.println(abastecedorModel.getNome() + ": Enviando combustível " + aviao.getPrefixo());

                        }

                    } catch (UnreadableException ex) {
                        reply.setPerformative(ACLMessage.REFUSE);

                    } catch (IOException ex) {
                        Logger.getLogger(AbastecedorAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }
}
