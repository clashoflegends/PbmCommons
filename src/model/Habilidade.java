/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;

/**
 *
 * @author jmoura
 */
public class Habilidade extends BaseModel {

    private int valor, cost = 0;
    private String tipo; //ENUM('SCENARIO','TROOP','CITY','NATION')

    /**
     * @return the valor
     */
    public int getValor() {
        return valor;
    }

    /**
     * @param valor the valor to set
     */
    public void setValor(int valor) {
        this.valor = valor;
    }

    /**
     * @return the tipo
     */
    public String getTipo() {
        return tipo;
    }

    /**
     * @param tipo the tipo to set
     */
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public boolean isPackage() {
        return tipo.equalsIgnoreCase("PACKAGE");
    }

    public boolean isVolatile() {
        return this.hasHabilidade(";VOL;");
    }

    public boolean isNone() {
        return this.getCodigo().equals(";-;");
    }

    public boolean isFilter() {
        return tipo.equalsIgnoreCase("FILTER");
    }

    public boolean isPowerNation() {
        return tipo.equalsIgnoreCase("NATION");
    }

    public boolean isChance() {
        return tipo.equalsIgnoreCase("GAME") && getCodigo().contains(";C");
    }

    public boolean isAtribute() {
        return tipo.equalsIgnoreCase("SCENARIO");
    }

    public boolean isHidden() {
        return isPackage() || isFilter();
    }

    @Override
    public String toString() {
        return this.getComboDisplay();
    }
}
