package caiaja.agentes;

import caiaja.model.Abastecedor;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

/**
 *
 * @author Laian
 */
public class AbastecedorAgent extends Agent{
    
    private Abastecedor abastecedor;
    private boolean Ocupado;
    
    @Override
    protected void setup(){
        
        Object[] args = getArguments();
        
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action(){
                ACLMessage msg = myAgent.receive();
                if(msg != null){
                    String content = msg.getContent();
                    if(content.equalsIgnoreCase("Abasteca")){
                        //Receber msg o controlador
                        //sobre qual aviao necessita abastecer
                    }
                } else {
                    block();
                }
            }
        });
    }
}