
package caiaja.ontologia.predicados;

import caiaja.model.Aviao;
import caiaja.model.Piloto;
import jade.content.Predicate;

/**
 *
 * @author fosa
 * Um Piloto pilota um avião
 */
public class Pilota implements Predicate {

    private Piloto _piloto;
    private Aviao _aviao;

    public void setPiloto(Piloto piloto) {
        _piloto = piloto;
    }

    public Piloto getPiloto() {
        return _piloto;
    }

    public void setAviao(Aviao aviao) {
        _aviao = aviao;
    }

    public Aviao getAviao() {
        return _aviao;
    }

}
