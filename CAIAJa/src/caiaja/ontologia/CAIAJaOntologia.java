/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.ontologia;

import caiaja.model.Aviao;
import caiaja.model.Pista;
import jade.content.onto.BeanOntology;
import jade.domain.FIPANames.Ontology;

/**
 *
 * @author fosa
 */
public class CAIAJaOntologia extends BeanOntology {

    private static Ontology theInstance = (Ontology) new CAIAJaOntologia("");

    public static Ontology getInstance() {
        return theInstance;
    }

    public CAIAJaOntologia(String name) {
        super(name);

        try {
            add(Aviao.class);
            add(Pista.class);
//            add("com.acme.rocket.ontology");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
