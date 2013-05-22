/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author jmoura
 */
public class TipoTropa extends BaseModel {

    private int movimento, upkeepMoney, upkeepFood, recruitCostMoney, recruitCostTime;
    private SortedMap<Terreno, Integer> ataqueTerreno = new TreeMap<Terreno, Integer>();
    private SortedMap<Terreno, Integer> defesaTerreno = new TreeMap<Terreno, Integer>();
    private SortedMap<Terreno, Integer> custoMovimento = new TreeMap<Terreno, Integer>();

    public int getMovimento() {
        return movimento;
    }

    public void setMovimento(int movimento) {
        this.movimento = movimento;
    }

    public int getUpkeepMoney() {
        return upkeepMoney;
    }

    public void setUpkeepMoney(int upkeepMoney) {
        this.upkeepMoney = upkeepMoney;
    }

    public int getUpkeepFood() {
        return upkeepFood;
    }

    public void setUpkeepFood(int upkeepFood) {
        this.upkeepFood = upkeepFood;
    }

    public int getRecruitCostMoney() {
        return recruitCostMoney;
    }

    public void setRecruitCostMoney(int recruitCostMoney) {
        this.recruitCostMoney = recruitCostMoney;
    }

    public int getRecruitCostTime() {
        return recruitCostTime;
    }

    public void setRecruitCostTime(int recruitCostTime) {
        this.recruitCostTime = recruitCostTime;
    }

    /**
     * @return the ataqueTerreno
     */
    public SortedMap<Terreno, Integer> getAtaqueTerreno() {
        return ataqueTerreno;
    }

    /**
     * @param ataqueTerreno the ataqueTerreno to set
     */
    public void setAtaqueTerreno(SortedMap<Terreno, Integer> ataqueTerreno) {
        this.ataqueTerreno = ataqueTerreno;
    }

    /**
     * @return the defesaTerreno
     */
    public SortedMap<Terreno, Integer> getDefesaTerreno() {
        return defesaTerreno;
    }

    /**
     * @param defesaTerreno the defesaTerreno to set
     */
    public void setDefesaTerreno(SortedMap<Terreno, Integer> defesaTerreno) {
        this.defesaTerreno = defesaTerreno;
    }

    /**
     * @return the custoMovimento
     */
    public SortedMap<Terreno, Integer> getMovimentoTerreno() {
        return custoMovimento;
    }

    /**
     * @param custoMovimento the custoMovimento to set
     */
    public void setMovimentoTerreno(SortedMap<Terreno, Integer> custoMovimento) {
        this.custoMovimento = custoMovimento;
    }

    public int getBonusTatica(int tatica) {
        //FIXME: adicionar ao banco de dados por tipo tropa.postergado apra nao focar mudanca no model agora.
        int tpTropa = getInt();
        int bonusTatica[][] = {{120, 100, 100, 100, 100, 80},
            {100, 100, 100, 120, 100, 80},
            {100, 120, 100, 80, 100, 100},
            {80, 100, 100, 100, 120, 100},
            {100, 80, 100, 100, 100, 120},
            {80, 100, 100, 100, 120, 100}};
        return (bonusTatica[tpTropa][tatica]);
    }

    private int getInt() {
        String tpTropa = this.getCodigo();
        int ret = 1;
        if (tpTropa.equalsIgnoreCase("CP")) {
            ret = 0;
        } else if (tpTropa.equalsIgnoreCase("CL")) {
            ret = 1;
        } else if (tpTropa.equalsIgnoreCase("IP")) {
            ret = 2;
        } else if (tpTropa.equalsIgnoreCase("IL")) {
            ret = 3;
        } else if (tpTropa.equalsIgnoreCase("AR")) {
            ret = 4;
        } else if (tpTropa.equalsIgnoreCase("ME")) {
            ret = 5;
        }
        return ret;
    }

    public boolean isFastMovement() {
        return this.hasHabilidade(";TTF;");
    }

    public boolean isBarcos() {
        return this.hasHabilidade(";TTN;");
    }

    public boolean isAuctionable() {
        return this.hasHabilidade(";LNA;");
    }

    public boolean isTransferable() {
        return this.hasHabilidade(";TNN;");
    }
}
