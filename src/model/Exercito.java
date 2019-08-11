/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;
import business.interfaces.IExercito;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author gurgel
 */
public class Exercito extends BaseModel implements IExercito {

    private int moral = 0;
    private int comida = 0, tatica = 2;
    private int tamanhoExercito = 0, tamanhoEsquadra = 0;
    private boolean movimentacaoEvasiva = false;
    private Personagem comandante;
    private Nacao nacao;
    private Local local;
    private SortedMap<String, Pelotao> tropas = new TreeMap();

    @Override
    public int getTatica() {
        return tatica;
    }

    public void setTatica(int tatica) {
        this.tatica = tatica;
    }

    public void addPelotoes(SortedMap<String, Pelotao> pelotoes) {
        this.tropas = pelotoes;
    }

    public String getComandanteIdentificacao() {
        return this.comandante.getCodigo();
    }

    public Personagem getComandante() {
        return comandante;
    }

    @Override
    public Personagem getComandanteModel() {
        return getComandante();
    }

    @Override
    public Nacao getNacao() {
        return this.nacao;
    }

    @Override
    public SortedMap<String, Pelotao> getPelotoes() {
        return this.tropas;
    }

    public int getTropasTotal() {
        int ret = 0;
        for (Pelotao pelotao : tropas.values()) {
            ret += pelotao.getQtd();
        }
        return ret;
    }

    public void setComandante(Personagem personagem) {
        if (this.comandante != null) {
            this.comandante.setExercito(null);
        }
        try {
            personagem.setExercito(this);
        } catch (NullPointerException e) {
        }
        this.comandante = personagem;
    }

    @Override
    public int getMoral() {
        return moral;
    }

    public void setMoral(int moral) {
        this.moral = moral;
    }

    public int getComida() {
        return comida;
    }

    public void setComida(int comida) {
        this.comida = comida;
    }

    public boolean isMovimentacaoEvasiva() {
        return movimentacaoEvasiva;
    }

    public void setMovimentacaoEvasiva(boolean movimentacaoEvasiva) {
        this.movimentacaoEvasiva = movimentacaoEvasiva;
    }

    public void setNacao(Nacao nacao) {
        this.nacao = nacao;
    }

    @Override
    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
        local.addExercito(this);
    }

    /**
     * @return the tamanhoExercito
     */
    public int getTamanhoExercito() {
        return tamanhoExercito;
    }

    /**
     * @param tamanhoExercito the tamanhoExercito to set
     */
    public void setTamanhoExercito(int tamanhoExercito) {
        this.tamanhoExercito = tamanhoExercito;
    }

    /**
     * @return the tamanhoEsquadra
     */
    public int getTamanhoEsquadra() {
        return tamanhoEsquadra;
    }

    /**
     * @param tamanhoEsquadra the tamanhoEsquadra to set
     */
    public void setTamanhoEsquadra(int tamanhoEsquadra) {
        this.tamanhoEsquadra = tamanhoEsquadra;
    }

    @Override
    public String toString() {
        return this.getNome() + "-" + this.getLocal().getCoordenadas() + "-" + getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    @Override
    public int getComandantePericia() {
        try {
            return getComandante().getPericiaComandante();
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    @Override
    public Terreno getTerreno() {
        return getLocal().getTerreno();
    }

    @Override
    public int getOrdensQt() {
        return 0;
    }

    @Override
    public String getTpActor() {
        return "E";
    }

    @Override
    public int getAttackBonus() {
        return 0;
    }

    @Override
    public int getArmyDefenseBonus() {
        return 0;
    }

    @Override
    public void doDisband() {
        //nothing to do for now. We don't disband in Counselor so far.
    }

    @Override
    public boolean isGarrison() {
        return getComandante() == null;
    }

    @Override
    public String getComandanteNome() {
        try {
            return getComandante().getNome();
        } catch (NullPointerException ex) {
            return "";
        }
    }

    @Override
    public boolean isDisband() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setArmyDefenseBonus(int bonus) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void doDisbandWithMsg() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setDisband(boolean disband) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setCombatDamageClear() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isGameHasResourceManagement() {
        //FIXME: How to know if the scenario has resource management?
        return true;
    }
}
