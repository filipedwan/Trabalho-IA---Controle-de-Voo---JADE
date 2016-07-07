/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja;
// macelo testando gitHub clone commit
// -----------------------------------

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.util.ExtendedProperties;
import jade.util.leap.Properties;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import java.util.ArrayList;
import java.util.List;
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

        List<String> Pilotos = new ArrayList<>();
        List<String> Controladores = new ArrayList<>();

        Pilotos.add("Fernando");
        Pilotos.add("Filipe");
        Pilotos.add("Marcelo");

        Controladores.add("Jose");
        /**
         * Inicializando Agentes
         */
        try {

            AgentController Aeroporto = ac.createNewAgent("AeroportoSBBV", "caiaja.agentes.AeroportoAgent", new String[]{"", "SBBV", "Atlas Brasil Catanhede", "2700"});
            Aeroporto.start();

            for (String piloto : Pilotos) {
                AgentController Piloto = ac.createNewAgent("p_" + piloto, "caiaja.agentes.PilotoAgent", new String[]{piloto});
                Piloto.start();
            }
            for (String Controlador : Controladores) {
                AgentController Controlador1 = ac.createNewAgent("c_" + Controlador, "caiaja.agentes.ControladorAgent", new String[]{Controlador});
                Controlador1.start();
            }

        } catch (StaleProxyException ex) {
            Logger.getLogger(CAIAJa.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
