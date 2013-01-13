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
    private int comida = 0;
    private int tamanhoExercito = 0, tamanhoEsquadra = 0;
    private boolean movimentacaoEvasiva = false;
    private Personagem comandante;
    private Nacao nacao;
    private Local local;
    private SortedMap<String, Pelotao> tropas = new TreeMap();

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
    public int getPericiaComandante() {
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
}
