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
import caiaja.model.Controlador;
import caiaja.model.Incendio;
import caiaja.model.Piloto;
import caiaja.ontologia.CAIAJaOntologia;
import caiaja.ontologia.acoes.Decolar;
import caiaja.ontologia.acoes.Pousar;
import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import java.io.IOException;
import java.util.List;
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
                addBehaviour(new PilotoAgent.CondicaoDeVoo());

            }
        }
    }

    @Override
    public void takeDown() {
        if (emvoo) {

            int intencidade = aviao.getMotores() * (aviao.getCombustivel() + 1) * aviao.getTamanhoTanque() + 1;

            System.err.println("Acidente com " + aviao.getPrefixo() + ": Anunciando incendio (" + intencidade + ") em " + aeroportoModel.getNome());

            Incendio incendio_modelo = new Incendio(intencidade);

            ACLMessage avistaincendio = new ACLMessage(ACLMessage.PROPAGATE);

            avistaincendio.addReceiver(aeroportoAgent);
            avistaincendio.addReceiver(controladorAgent);
            try {
                avistaincendio.setContentObject(incendio_modelo);
            } catch (IOException ex) {
                Logger.getLogger(PilotoAgent.class.getName()).log(Level.SEVERE, null, ex);
            }
            this.send(avistaincendio);

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
                    aviao.setAceleracaoMotor(0.8f);
//                        System.out.println(piloto.getNome() + ": Em voo com " + aviao.getPrefixo());

                    if (aviao.getNilveCombustivel() < 0.3f) {
                        System.out.println(piloto.getNome() + " " + aviao.getPrefixo() + ": Preciso pousar combustível baixo (" + aviao.getNilveCombustivel() + ")");
                        myAgent.addBehaviour(new PilotoAgent.PropoePousar(myAgent));
                    } else {
                        System.out.println(piloto.getNome() + " " + aviao.getPrefixo() + ": Ainda tenho combustível (" + aviao.getNilveCombustivel() + ")");

                    }

                } else if (aeroportoModel != null) {

                    if (aviao.getNilveCombustivel() > 0.4f) {
                        myAgent.addBehaviour(new PilotoAgent.PropoeDecolar(myAgent));
                    } else {
                        //TODO Criar comportamento pra abastecer
                        /**
                         * chama abastecedor
                         */
                        System.err.println("Tanque não tem combustível suficiente para voo..." + aviao.getPrefixo());

                        List<AID> abastecedores = CAIAJa.getServico(myAgent, "Abastecedor");
                        myAgent.addBehaviour(new PilotoAgent.RequisicaoAbastecer(myAgent, abastecedores));
                    }
                }
            }

//            if (aviao.getCombustivel() == 0 && emvoo) {
//                doDelete();
//            }
        }

    }

    private class RequisicaoAbastecer extends Behaviour {

        List<AID> lista_abastecedores;
        AID Escolhido;
        Abastecedor abastecedorModel;
        int estado = 0;
        private MessageTemplate mt; // The template to receive replies
        private int repliesCnt = 0;

        public RequisicaoAbastecer(Agent a, List<AID> abastecedores) {
            super(a);
            lista_abastecedores = abastecedores;
        }

        @Override
        public void action() {
            //System.out.println(piloto.getNome() + ": Pilotar " + estado);
            switch (estado) {
                case 0: {
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                    for (AID abastecedor : lista_abastecedores) {
                        System.out.println(piloto.getNome() + " --> " + abastecedor.getLocalName());
                        cfp.addReceiver(abastecedor);
                    }
                    cfp.setConversationId("proposta-abastecedor");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());
                    cfp.setContent("Preciso de um abastecedor");
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("proposta-abastecedor"),
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
                                abastecedorModel = (Abastecedor) reply.getContentObject();
                            } catch (UnreadableException ex) {
                                Logger.getLogger(PilotoAgent.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                        repliesCnt++;
                        if (repliesCnt >= lista_abastecedores.size()) {
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
                    msg.setConversationId("proposta-abastecedor");
                    msg.setReplyWith("abastecer" + System.currentTimeMillis());
                    myAgent.send(msg);
                    System.out.println(piloto.getNome() + " --> " + Escolhido.getLocalName() + ": Aceito Proposta");

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
                            try {
                                aviao = ((Aviao) reply.getContentObject());
                                System.out.println(piloto.getNome() + ": Aviao " + aviao.getPrefixo() + " sendo abastecido");
                                aviao.setAceleracaoMotor(0.00f);
                            } catch (UnreadableException ex) {
                                Logger.getLogger(PilotoAgent.class.getName()).log(Level.SEVERE, null, ex);
                            }

                        } else {
                            System.out.println(aviao.getPrefixo() + ": não pode ser abastecido " + Escolhido.getLocalName() + " já está ocupado");
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
                        System.out.println(piloto.getNome() + " --> " + aeroporto.getLocalName() + ": quero uma aeronave pra pilotar");
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

    /**
     * Class para Criar o comportamento de pouso
     */
    private class PropoePousar extends Behaviour {

        int estado = 0;
        private MessageTemplate mt;
        int estado_final = 10;
        Pousar pousar;

        public PropoePousar(Agent myAgent) {
            super(myAgent);
            emAcao = true;
        }

        @Override
        public void action() {
//            System.out.println(piloto.getNome() + ": Pousar " + estado);
            switch (estado) {
                case 0: {
                    ACLMessage proposta = new ACLMessage(ACLMessage.PROPOSE);
                    if (controladorAgent != null) {
                        System.out.println(piloto.getNome() + ": Solicitando permissão para pousar " + controladorModel.getNome());
                        proposta.addReceiver(controladorAgent);

                        aviao.setAceleracaoMotor(0);

                        proposta.setConversationId("proposta-piloto-pousar");
                        proposta.setReplyWith("cfp" + System.currentTimeMillis());

                        pousar = new Pousar(aviao, piloto, aeroportoModel, myAgent.getAID(), proposta.getReplyWith());

                        try {
                            proposta.setContentObject(pousar);
                        } catch (IOException ex) {
                            Logger.getLogger(PilotoAgent.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        myAgent.send(proposta);

                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("proposta-piloto-pousar"),
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

                            msg = new ACLMessage(ACLMessage.REQUEST);

                            msg.setConversationId("liberacao-piloto-pousar");
                            msg.setReplyWith("confirm" + System.currentTimeMillis());

                            msg.addReceiver(reply.getSender());

                            System.out.println(piloto.getNome() + ": Aguardando liberação para pousar " + aviao.getPrefixo());
                            estado = 2;
                            try {
                                msg.setContentObject(pousar);
                            } catch (IOException ex) {
                                Logger.getLogger(ControladorAgent.class.getName()).log(Level.SEVERE, null, ex);
                            }

                            mt = MessageTemplate.and(
                                    MessageTemplate.MatchConversationId(msg.getConversationId()),
                                    MessageTemplate.MatchInReplyTo(msg.getReplyWith())
                            );

                            myAgent.send(msg);

                        } else {
                            if (aviao.getCombustivel() == 0) {
                                myAgent.doDelete();
                            }
                            emAcao = false;
                            estado = estado_final;
                        }
                    } else {
                        block();
                    }
                    break;
                }
                case 2: {
                    /**
                     * Pouso aprovado, aguardando clearance para pouso
                     */
                    ACLMessage reply = myAgent.receive(mt);
                    System.err.println("Aguardando pouso");
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.CONFIRM) {
                            aeroportoAgent = reply.getSender();
                            System.out.println(piloto.getNome() + ": pousando " + aviao.getPrefixo());
                            aviao.setAceleracaoMotor(1f);
                            estado = 3;
                        } else if (reply.getPerformative() == ACLMessage.CANCEL) {
                            System.out.println(piloto.getNome() + ": Pouso cancelado pelo controlador " + controladorModel.getNome() + "");
                            emAcao = false;
                            estado = estado_final;
                        } else {
                            System.out.println(piloto.getNome() + ": Aguardando confirmação do controlador para pouso" + controladorModel.getNome() + "");
                        }
                    } else {
                        block();
                    }
                    break;
                }
                case 3: {
                    block(2000);

                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

                    msg.setConversationId("sucesso-piloto-pouso");
                    msg.setReplyWith("pouso" + System.currentTimeMillis());

                    msg.addReceiver(controladorAgent);

                    System.out.println(piloto.getNome() + ": informando pouso com sucesso " + aviao.getPrefixo());

                    myAgent.send(msg);

                    aviao.setAceleracaoMotor(0.3f);

                    emvoo = false;
                    emAcao = false;

                    //msg.setReplyWith("cfp" + System.currentTimeMillis());
                    //MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                    MessageTemplate mt1 = MessageTemplate.MatchInReplyTo(msg.getReplyWith());
                    MessageTemplate mt2 = MessageTemplate.MatchConversationId("abastecer-piloto");
                    mt = MessageTemplate.and(mt1, mt2);
                    estado = 4;
                    break;
                }
                case 4: {
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        ACLMessage reply = msg.createReply();
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            msg.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                            msg.setConversationId("abastecer-piloto");
                            msg.addReceiver(reply.getSender());
                            send(msg);
                        }
                        estado = 5;
                    }
                    break;
                }
                case 5: {
                    break;
                }
            }
        }

        @Override
        public boolean done() {
            if (estado == 5) {
                return true;
            }
            if (estado == estado_final) {
                return true;
            }
            return false;
        }

    }

    /**
     * Class para Criar o comportamento de Decolagem
     */
    private class PropoeDecolar extends Behaviour {

        int estado = 0;
        private MessageTemplate mt;
        int estado_final = 10;
        Decolar decolar;

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
                        System.out.println(piloto.getNome() + ": Solicitando permissão para decolar " + controladorModel.getNome());
                        proposta.addReceiver(controladorAgent);

                        aviao.setAceleracaoMotor(0);

                        proposta.setConversationId("proposta-piloto-decolar");
                        proposta.setReplyWith("cfp" + System.currentTimeMillis());

                        decolar = new Decolar(aviao, piloto, aeroportoModel, myAgent.getAID(), proposta.getReplyWith());

                        try {
                            proposta.setContentObject(decolar);
                        } catch (IOException ex) {
                            Logger.getLogger(PilotoAgent.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        myAgent.send(proposta);

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
                                    msg.setContentObject(decolar);
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
                            emAcao = false;
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
                            System.out.println(piloto.getNome() + ": Decolando " + aviao.getPrefixo());
                            aviao.setAceleracaoMotor(1f);
                            estado = 4;
                        } else if (reply.getPerformative() == ACLMessage.CANCEL) {
                            System.out.println(piloto.getNome() + ": Decolagem cancelada pelo controlador " + controladorModel.getNome() + "");
                            emAcao = false;
                            estado = estado_final;
                        } else {
                            System.out.println(piloto.getNome() + ": Aguardando confirmação do controlador " + controladorModel.getNome() + "");
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
                        block(100);
                    }
                    break;
                }
                case 4: {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(PilotoAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

                    msg.setConversationId("sucesso-piloto-decolar");
                    msg.setReplyWith("decolar" + System.currentTimeMillis());

                    msg.addReceiver(controladorAgent);

                    System.out.println(piloto.getNome() + ": informando decolagem com sucesso " + aviao.getPrefixo());

                    myAgent.send(msg);

                    emvoo = true;
                    emAcao = false;
                    estado = 5;

                    break;
                }
                case 5: {
                    break;
                }
            }
        }

        @Override
        public boolean done() {
            if (estado == 5) {
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

    /**
     * Classe para responder aos requerimentos de controladores que precisem de
     * um aeroporto pra controlar, retonar sim ou não para a requisição
     */
    private class CondicaoDeVoo extends CyclicBehaviour {

        public void action() {
            if (aviao != null) {
                if (emvoo && !emAcao && aviao.getCombustivel() == 0) {
                    doDelete();
                }
            } else {
                block();
            }
        }
    }

}
