package caiaja.ontologia.predicados;

import caiaja.model.Controlador;
import caiaja.model.Pessoa;
import jade.content.Predicate;

/**
 *
 * @author fosa
 */
public class ControladorE implements Predicate {

    private Pessoa _pessoa;
    private Controlador _controlador;

    public void setPessoa(Pessoa pessoa) {
        _pessoa = pessoa;
    }

    public Pessoa getPessoa() {
        return _pessoa;
    }

    public void setControlador(Controlador controlador) {
        _controlador = controlador;
    }

    public Controlador getControlador() {
        return _controlador;
    }

}
