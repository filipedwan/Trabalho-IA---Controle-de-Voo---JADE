/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caiaja.model;

import jade.content.Concept;
import java.io.Serializable;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fosa
 */
public class Aviao implements Serializable, Concept {

    final int PROPULSAO_NENHUMA = 0;
    final int PROPULSAO_PISTAO = 1;
    final int PROPULSAO_JATO = 2;
    final int PROPULSAO_TURBOHELICE = 3;
    final int PROPULSAO_ELETRICO = 4;

    private int pistaMinima;
    private String Prefixo;
    private int Propulsao; //Tipo de propulsao
    private int Motores; //Numero de motores da aeronave
    private int tamanhoTanque; //em litros
    private float aceleracaoMotor; //em litros
    int combustivel;
    private Thread th;

    public Aviao(String Prefix) {
        Motores = 1;
        Propulsao = PROPULSAO_PISTAO;
        Prefixo = Prefix;
        pistaMinima = 500;
        tamanhoTanque = 100;
        aceleracaoMotor = 0;
        combustivel = tamanhoTanque;
    }

    public int getPistaMinima() {
        return pistaMinima;
    }

    public void setPistaMinima(int pistaMinima) {
        this.pistaMinima = pistaMinima;
    }

    public String getPrefixo() {
        return Prefixo;
    }

    public void setPrefixo(String Prefixo) {
        this.Prefixo = Prefixo;
    }

    public int getPropulsao() {
        return Propulsao;
    }

    public void setPropulsao(int Propulsao) {
        this.Propulsao = Propulsao;
    }

    public int getMotores() {
        return Motores;
    }

    public void setMotores(int Motores) {
        this.Motores = Motores;
    }

    public int getTamanhoTanque() {
        return tamanhoTanque;
    }

    public void setTamanhoTanque(int tamanhoTanque) {
        this.tamanhoTanque = tamanhoTanque;
    }

    public float getAceleracaoMotor() {
        return aceleracaoMotor;
    }

    public void setAceleracaoMotor(float aceleracaoMotor) {
        if (aceleracaoMotor > 0) {
            if (th != null) {
                if (th.isAlive()) {
                    th.stop();
                }
            }
            th = new Thread(new GastaCommbustivel());

            th.start();

        } else {
            if (th != null) {
                th.stop();
            }
            th = null;
        }
        this.aceleracaoMotor = aceleracaoMotor;
    }

    public float getNilveCombustivel() {
        return (float) combustivel * 100 / tamanhoTanque;
    }

    public int getCombustivel() {
        return combustivel;
    }

    public void setCombustivel(int combustivel) {
        this.combustivel = combustivel;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.Prefixo);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Aviao other = (Aviao) obj;
        if (!Objects.equals(this.Prefixo, other.Prefixo)) {
            return false;
        }
        return true;
    }

    class GastaCommbustivel implements Runnable {

        public GastaCommbustivel() {
        }

        @Override
        public void run() {
            while (getCombustivel() > 0) {
                int milis = 1000;

                if (getAceleracaoMotor() > 0) {
                    setCombustivel(combustivel - getMotores());
                    milis -= (500 * getAceleracaoMotor());
                }
                if (getCombustivel() <= (getTamanhoTanque() * 0.1) && getCombustivel() >= (getTamanhoTanque() * 0.09)) {
                    System.err.println(getPrefixo() + ": 10% combutivel");
                }
//                System.err.println(getPrefixo() + ": acel=" + getAceleracaoMotor() + " comb=" + getCombustivel() + " milis=" + milis);
                try {
                    Thread.sleep(milis);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Aviao.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            System.err.println(getPrefixo() + ": Sem combutivel");

        }

    }
}
