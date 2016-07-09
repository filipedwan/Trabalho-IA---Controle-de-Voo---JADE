/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja;
// macelo testando gitHub clone commit
// -----------------------------------

import jade.core.AID;
import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
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

        List<AgentController> Agentes = new ArrayList<>();

        List<String> Pilotos = new ArrayList<>();
        List<String> Controladores = new ArrayList<>();
        List<String> Bombeiros = new ArrayList<>();
        List<String> abastecedores = new ArrayList<>();

        Pilotos.add("Fernando");
        Pilotos.add("Filipe");
        Pilotos.add("Marcelo");

        Controladores.add("Jose");

        Bombeiros.add("Carlos");

        abastecedores.add("Ville");

        /**
         * Inicializando Agentes
         */
        try {

            AgentController Aeroporto = ac.createNewAgent("AeroportoSBBV", "caiaja.agentes.AeroportoAgent", new String[]{"", "SBBV", "Atlas Brasil Catanhete", "2700"});
            Aeroporto.start();

            for (String piloto : Pilotos) {
                AgentController Piloto = ac.createNewAgent("p_" + piloto, "caiaja.agentes.PilotoAgent", new String[]{piloto});
                Agentes.add(Piloto);
            }
            for (String Controlador : Controladores) {
                AgentController Controlador1 = ac.createNewAgent("c_" + Controlador, "caiaja.agentes.ControladorAgent", new String[]{Controlador});
                Agentes.add(Controlador1);
            }

            for (String bombeiro : Bombeiros) {
                AgentController Bombeiros1 = ac.createNewAgent("b_" + bombeiro, "caiaja.agentes.BombeiroAgent", new String[]{bombeiro});
                Agentes.add(Bombeiros1);
            }

            for (String abastacedor : abastecedores) {
                AgentController Bombeiros1 = ac.createNewAgent("a_" + abastacedor, "caiaja.agentes.AbastecedorAgent", new String[]{abastacedor});
                Agentes.add(Bombeiros1);
            }

        } catch (StaleProxyException ex) {
            Logger.getLogger(CAIAJa.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(CAIAJa.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (AgentController Agente : Agentes) {
            try {
                Agente.start();
            } catch (StaleProxyException ex) {
                Logger.getLogger(CAIAJa.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static List<AID> getServico(Agent agente, String DescricaoServico) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(DescricaoServico);
        template.addServices(sd);

        List<AID> servicos = new ArrayList<AID>();
        try {
            DFAgentDescription[] result = DFService.search(agente, template);
            for (int i = 0; i < result.length; ++i) {
                servicos.add(result[i].getName());
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        return servicos;
    }

    public static void registrarServico(Agent agente, String Servico, String Nome) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(agente.getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(Servico);
        sd.setName(Nome);
        dfd.addServices(sd);

        try {
            DFService.register(agente, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

}
