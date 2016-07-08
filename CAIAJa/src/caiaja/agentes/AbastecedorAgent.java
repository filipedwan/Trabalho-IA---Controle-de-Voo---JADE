/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.agentes;
import caiaja.model.Abastecedor;
import caiaja.model.Aviao;
import caiaja.model.Controlador;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

/**
 *
 * @author Laian
 */
public class AbastecedorAgent extends Agent{

    private Abastecedor abastecedor;
    private Controlador controlador;
    private boolean ocupado;
    
    @Override
    protected void setup() {
        super.setup(); //To change body of generated methods, choose Tools | Templates.
        
        Object[] args = getArguments();
        
        abastecedor = new Abastecedor();
        
        if (args != null) {
            if (args.length > 0) {
                abastecedor.setNome((String) args[0]);

                System.out.println("Abstecedor " + abastecedor.getNome() + " procurando avião sem combustível");                

                DFAgentDescription dfd = new DFAgentDescription();
                dfd.setName(getAID());
                ServiceDescription sd = new ServiceDescription();
                sd.setType("Abastecedor");
                sd.setName(abastecedor.getNome());
                dfd.addServices(sd);

                try {
                    DFService.register(this, dfd);
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }

                //addBehaviour(new BombeiroAgent.BuscarEmprego(this, 60000));

                //addBehaviour(new BombeiroAgent.RequisicoesDePropostas());
            }
        }        
        
    }
    
    private class Abastecer extends Behaviour{
        
        private Aviao aviao;
        private int TamanhoMangueira;        

        @Override
        public void action() {
            
        }

        @Override
        public boolean done() {
            return false;
        }

    }
}