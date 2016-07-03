/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.model;

import jade.util.leap.ArrayList;
import java.util.List;

/**
 *
 * @author fosa
 */
public class Aeroporto {

    String Nome;
    String Prefixo;
    List<Pista> Pistas;
    List<Patio> Patio;
    EstacaoMeteorologica estacaoMeteorologica;
    Controlador controlador;

    public Aeroporto() {
        setPistas(new java.util.ArrayList<>());
        setControlador(null);
        setEstacaoMeteorologica(null);
    }

    public String getNome() {
        return Nome;
    }

    public void setNome(String Nome) {
        this.Nome = Nome;
    }

    public String getPrefixo() {
        return Prefixo;
    }

    public void setPrefixo(String Prefixo) {
        this.Prefixo = Prefixo;
    }

    public List<Pista> getPistas() {
        return Pistas;
    }

    public void setPistas(List<Pista> Pistas) {
        this.Pistas = Pistas;
    }

    public EstacaoMeteorologica getEstacaoMeteorologica() {
        return estacaoMeteorologica;
    }

    public void setEstacaoMeteorologica(EstacaoMeteorologica estacaoMeteorologica) {
        this.estacaoMeteorologica = estacaoMeteorologica;
    }

    public List<Patio> getPatio() {
        return Patio;
    }

    public void setPatio(List<Patio> Patio) {
        this.Patio = Patio;
    }

    public void addPista(Pista Pista) {
        this.Pistas.add(Pista);
    }

    public Controlador getControlador() {
        return controlador;
    }

    public void setControlador(Controlador controlador) {
        this.controlador = controlador;
    }

}
