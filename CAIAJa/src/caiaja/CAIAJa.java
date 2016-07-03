/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja;

import caiaja.model.Aeroporto;
import caiaja.model.Pista;
import jade.Boot;
import static jade.Boot.DEFAULT_FILENAME;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.util.ExtendedProperties;
import jade.util.leap.Properties;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fosa
 */
public class CAIAJa {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        AgentContainer ac = null;

        ProfileImpl p = null;
        Properties props = new ExtendedProperties();
        p = new ProfileImpl(props);
        props.setProperty(Profile.GUI, "true");

        jade.core.Runtime.instance().setCloseVM(true);
        if (p.getBooleanProperty(Profile.MAIN, true)) {
            ac = jade.core.Runtime.instance().createMainContainer(p);
        } else {
            jade.core.Runtime.instance().createAgentContainer(p);
        }

//        Aeroporto aero = new Aeroporto();
//        aero.setNome("Atlas Brasil Catanhede");
//        aero.setPrefixo("SBBV");
//        aero.addPista(new Pista(2700));
        /**
         * Inicializando Agentes
         */
        try {

            AgentController Aeroporto = ac.createNewAgent("AeroportoSBBV", "caiaja.agentes.AeroportoAgent", new String[]{"", "SBBV", "Atlas Brasil Catanhede", "2700"});
            Aeroporto.start();

            AgentController Controlador1 = ac.createNewAgent("Controlador1", "caiaja.agentes.ControladorAgent", new String[]{"cmd Amiltom"});
            Controlador1.start();

            AgentController Estacao = ac.createNewAgent("Estacao", "caiaja.agentes.EstacaoMeteorologicaAgent", new Object[0]);
            Estacao.start();

        } catch (StaleProxyException ex) {
            Logger.getLogger(CAIAJa.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
