/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.ontologia;

import caiaja.model.Aeroporto;
import caiaja.model.Aviao;
import caiaja.model.Controlador;
import caiaja.model.Piloto;
import caiaja.ontologia.acoes.Decolar;
import caiaja.ontologia.acoes.Pousar;
import caiaja.ontologia.predicados.Pilota;
import jade.content.onto.BasicOntology;
import jade.content.onto.BeanOntology;
import jade.content.onto.Ontology;
import jade.content.schema.AgentActionSchema;
import jade.content.schema.ConceptSchema;
import jade.content.schema.PredicateSchema;
import jade.content.schema.PrimitiveSchema;

/**
 *
 * @author fosa
 */
public class CAIAJaOntologia extends BeanOntology {

    public static final String NAME = "piloto-ontology";
    private static Ontology theInstance = (Ontology) new CAIAJaOntologia();

    // VOCABULARY
    // Concepts
    public static final String PESSOA = "PESSOA";
    public static final String PESSOA_NAME = "nome";

    public static final String PILOTO = "PILOTO";
    public static final String PILOTO_NAME = "nome";

    public static final String AVIAO = "AVIAO";
    public static final String AVIAO_PISTAMIN = "pista-minima";
    public static final String AVIAO_PREFIXO = "prefixo";

    public static final String CONTROLADOR = "CONTROLADOR";
    public static final String CONTROLADOR_NAME = "nome";

    public static final String AEROPORTO = "AEROPORTO";
    public static final String AEROPORTO_PREFIXO = "prefixo";

    // Actions
    public static final String DECOLAR = "DECOLAR";
    public static final String DECOLAR_AVIAO = "aviao";
    public static final String DECOLAR_DO_AEROPORTO = "aeroporto";

    public static final String POUSAR = "POUSAR";
    public static final String POUSAR_AVIAO = "aviao";
    public static final String POUSAR_NO_AEROPORTO = "aeroporto";

    // Predicates    
    public static final String PILOTA = "PILOTA";
    public static final String PILOTA_PILOTO = "piloto";
    public static final String PILOTA_AVIAO = "aviao";

    public static final String CONTROLA = "CONTROLA";
    public static final String CONTROLA_CONTROLADOR = "controlador";
    public static final String CONTROLA_AEROPORTO = "aeroporto";

    public static final String CONTROLADO_POR = "CONTROLADOR-POR";
    public static final String CONTROLADO_POR_AEROPORTO = "aeroporto";
    public static final String CONTROLADO_POR_CONTROLADOR = "controlador";

    public static final String CONTROLADOR_E = "CONTROLADOR_E";
    public static final String CONTROLADOR_E_PESSOA = "pessoa";
    public static final String CONTROLADOR_E_CONTROLADOR = "controlador";

    public static Ontology getInstance() {
        return theInstance;
    }

    public CAIAJaOntologia() {
        super(NAME, BasicOntology.getInstance());

        try {
            //Conceitos
            add(new ConceptSchema(PESSOA), Piloto.class);
            add(new ConceptSchema(PILOTO), Piloto.class);
            add(new ConceptSchema(CONTROLADOR), Controlador.class);
            add(new ConceptSchema(AVIAO), Aviao.class);
            add(new ConceptSchema(AEROPORTO), Aeroporto.class);

            //Acoes
            add(new AgentActionSchema(DECOLAR), Decolar.class);
            add(new AgentActionSchema(POUSAR), Pousar.class);

            //Predicados
            add(new PredicateSchema(PILOTA), Pilota.class);
            add(new PredicateSchema(CONTROLA), Pilota.class);
            add(new PredicateSchema(CONTROLADOR_E), Pilota.class);

            //Conceitos
            ConceptSchema cs = (ConceptSchema) getSchema(PESSOA);
            cs.add(PESSOA_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING));

            cs = (ConceptSchema) getSchema(PILOTO);
            cs.add(PILOTO_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING));

            cs = (ConceptSchema) getSchema(CONTROLADOR);
            cs.add(CONTROLADOR_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING));

            cs = (ConceptSchema) getSchema(AVIAO);
            cs.add(AVIAO_PREFIXO, (PrimitiveSchema) getSchema(BasicOntology.STRING));

            cs = (ConceptSchema) getSchema(AEROPORTO);
            cs.add(AEROPORTO_PREFIXO, (PrimitiveSchema) getSchema(BasicOntology.STRING));

            //Predicados            
            PredicateSchema ps = (PredicateSchema) getSchema(PILOTA);
            ps.add(PILOTA_PILOTO, (ConceptSchema) getSchema(PILOTO));
            ps.add(PILOTA_AVIAO, (ConceptSchema) getSchema(AVIAO));

            ps = (PredicateSchema) getSchema(CONTROLA);
            ps.add(CONTROLA_AEROPORTO, (ConceptSchema) getSchema(AEROPORTO));
            ps.add(CONTROLA_CONTROLADOR, (ConceptSchema) getSchema(CONTROLADOR));

            ps = (PredicateSchema) getSchema(CONTROLADOR_E);
            ps.add(CONTROLADOR_E_CONTROLADOR, (ConceptSchema) getSchema(CONTROLADOR));
            ps.add(CONTROLADOR_E_PESSOA, (ConceptSchema) getSchema(PESSOA));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
