/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.model;

/**
 *
 * @author fosa
 */
public class Aeroporto {

    String Nome;
    String Prefixo;
    Pista Pista;
    EstacaoMeteorologica estacaoMeteorologica;

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

    public Pista getPista() {
        return Pista;
    }

    public void setPista(Pista Pista) {
        this.Pista = Pista;
    }

    public EstacaoMeteorologica getEstacaoMeteorologica() {
        return estacaoMeteorologica;
    }

    public void setEstacaoMeteorologica(EstacaoMeteorologica estacaoMeteorologica) {
        this.estacaoMeteorologica = estacaoMeteorologica;
    }

}
