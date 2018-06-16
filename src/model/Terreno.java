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
public class Terreno extends BaseModel {

    private boolean agua, ancoravel;
    private int producaoValor[];
    private int movimentoValor[] = {1, 2, 3, 4, 5}; //Infantry, +roads, Cavalry,+roads, navy

    public int[] getProducaoValor() {
        return producaoValor;
    }

    public void setProducaoValor(int[] producaoValor) {
        this.producaoValor = producaoValor;
    }

    public boolean isAgua() {
        return agua;
    }

    public void setAgua(boolean agua) {
        this.agua = agua;
    }

    public boolean isAncoravel() {
        return ancoravel;
    }

    public void setAncoravel(boolean ancoravel) {
        this.ancoravel = ancoravel;
    }

    /**
     * @return the movimentoValor
     */
    public int[] getMovimentoValor() {
        return movimentoValor;
    }

    /**
     * @param movimentoValor the movimentoValor to set
     */
    public void setMovimentoValor(int[] movimentoValor) {
        this.movimentoValor = movimentoValor;
    }

    public boolean isMontanha() {
        return getCodigo().equals("M");
    }

    public boolean isColina() {
        return getCodigo().equals("H");
    }

    public boolean isFloresta() {
        return getCodigo().equals("F");
    }

    public boolean isPantano() {
        return getCodigo().equals("S");
    }

    public boolean isPlanicie() {
        return getCodigo().equals("P");
    }

    public boolean isDeserto() {
        return getCodigo().equals("D");
    }
}
