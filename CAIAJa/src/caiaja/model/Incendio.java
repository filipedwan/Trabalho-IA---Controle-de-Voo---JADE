package caiaja.model;

import jade.content.Concept;
import java.io.Serializable;

/**
 *
 * @author Fosa
 */
public class Incendio implements Serializable, Concept {

    int intencidade;

    public Incendio(int intencidade) {
        this.intencidade = intencidade;
    }

    public int combateIncendio() {
        intencidade--;
        return intencidade;
    }

    public int getIntencidade() {
        return intencidade;
    }

    public void setIntencidade(int intencidade) {
        this.intencidade = intencidade;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + this.intencidade;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Incendio other = (Incendio) obj;
        if (this.intencidade != other.intencidade) {
            return false;
        }
        return true;
    }

}
