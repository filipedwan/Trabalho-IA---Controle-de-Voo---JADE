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
        if (args != null) {
            if (args.length > 0) {
                //executar classe privada aguardaPedidoDeAbastecimento
                AguardaPedidoDeAbastecimento aguarda;
                aguarda = new AguardaPedidoDeAbastecimento();
            }
        }
    }
    
    private class AguardaPedidoDeAbastecimento extends CyclicBehaviour{
        
        @Override
        public void action() {
            
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
    }
}