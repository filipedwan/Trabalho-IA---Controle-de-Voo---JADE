package caiaja.ontologia.predicados;

import caiaja.model.Aeroporto;
import caiaja.model.Controlador;
import jade.content.Predicate;

/**
 *
 * @author fosa
 */
public class ControladoPor implements Predicate {

    private Controlador _controlador;
    private Aeroporto _aeroporto;

    public void setControlador(Controlador controlador) {
        _controlador = controlador;
    }

    public Controlador getControlador() {
        return _controlador;
    }

    public void setAeroporto(Aeroporto aeroporto) {
        _aeroporto = aeroporto;
    }

    public Aeroporto getAeroporto() {
        return _aeroporto;
    }

}
