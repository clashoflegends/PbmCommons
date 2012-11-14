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
public class HabilidadeNacao extends BaseModel{
    private int vlHabilidadeNacao = 0;
    private int custoHabilidadeNacao = 0;

    /**
     * @return the vlHabilidadeNacao
     */
    public int getVlHabilidadeNacao() {
        return vlHabilidadeNacao;
    }

    /**
     * @param vlHabilidadeNacao the vlHabilidadeNacao to set
     */
    public void setVlHabilidadeNacao(int vlHabilidadeNacao) {
        this.vlHabilidadeNacao = vlHabilidadeNacao;
    }

    /**
     * @return the custoHabilidadeNacao
     */
    public int getCustoHabilidadeNacao() {
        return custoHabilidadeNacao;
    }

    /**
     * @param custoHabilidadeNacao the custoHabilidadeNacao to set
     */
    public void setCustoHabilidadeNacao(int custoHabilidadeNacao) {
        this.custoHabilidadeNacao = custoHabilidadeNacao;
    }

}
