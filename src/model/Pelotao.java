/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;

/**
 *
 * @author gurgel
 */
public class Pelotao extends BaseModel {
//Tropas 	Treino 	Arma 	Armadura 	Qtd 	Tipo de Tropa
//Hoplitas 	100 	60 	60 	600 	Infantaria Pesada

    private int treino = 0, modAtaque = 0, modDefesa = 0, qtd = 0;
    private TipoTropa tipoTropa;
//    private Raca raca;

    public int getTreino() {
        return treino;
    }

    public void setTreino(int treino) {
        this.treino = treino;
    }

    public int getModAtaque() {
        return modAtaque;
    }

    public void setModAtaque(int arma) {
        this.modAtaque = arma;
    }

    public void sumModAtaque(int arma) {
        this.modAtaque = Math.min(100, arma + this.modAtaque);
    }

    public int getModDefesa() {
        return modDefesa;
    }

    public void setModDefesa(int armadura) {
        this.modDefesa = armadura;
    }

    public void sumModDefesa(int armadura) {
        this.modDefesa = Math.min(100, armadura + this.modDefesa);
    }

    public int getQtd() {
        return qtd;
    }

    public void setQtd(int qtd) {
        this.qtd = qtd;
    }

    public void addQtd(int qtd) {
        this.qtd += qtd;
    }

    public TipoTropa getTipoTropa() {
        return tipoTropa;
    }

    public void setTipoTropa(TipoTropa tipoTropa) {
        this.tipoTropa = tipoTropa;
    }

    @Override
    public String getNome() {
        return this.getTipoTropa().getNome();
    }

    @Override
    public String getCodigo() {
        return getTipoTropa().getCodigo();
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", this.getTipoTropa().getNome(), this.getQtd());
    }
}
