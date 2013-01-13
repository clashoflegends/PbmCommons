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
 * @author jmoura
 */
public class ExercitoSim extends BaseModel implements IExercito {

    private int moral = 0;
    private int comandante = 0;
    private String comandanteNome;
    private int tatica = 2;
    private Local local;
    private Terreno terreno;
    private Nacao nacao;
    private SortedMap<String, Pelotao> tropas = new TreeMap();

    public ExercitoSim(Exercito exercito) {
        this.moral = exercito.getMoral();
        this.tropas = exercito.getPelotoes();
        this.local = exercito.getLocal();
        this.terreno = exercito.getLocal().getTerreno();
        this.nacao = exercito.getNacao();
        try {
            this.comandante = exercito.getComandante().getPericiaComandante();
            this.comandanteNome = exercito.getComandante().getNome();
        } catch (NullPointerException ex) {
        }
        //TODO: clone pelotoes para poder mudar valores sem afetar original
    }

    @Override
    public int getPericiaComandante() {
        return comandante;
    }

    public void setComandante(int comandante) {
        this.comandante = comandante;
    }

    @Override
    public int getMoral() {
        return moral;
    }

    public void setMoral(int moral) {
        this.moral = moral;
    }

    @Override
    public SortedMap<String, Pelotao> getPelotoes() {
        return tropas;
    }

    public void setTropas(SortedMap<String, Pelotao> tropas) {
        this.tropas = tropas;
    }

    public void addPelotoes(SortedMap<String, Pelotao> pelotoes) {
        this.tropas = pelotoes;
    }

    @Override
    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public int getTatica() {
        return tatica;
    }

    public void setTatica(int tatica) {
        this.tatica = tatica;
    }

    @Override
    public Nacao getNacao() {
        return nacao;
    }

    public void setNacao(Nacao nacao) {
        this.nacao = nacao;
    }

    public String getComandanteNome() {
        return comandanteNome;
    }

    public void setComandanteNome(String comandanteNome) {
        this.comandanteNome = comandanteNome;
    }

    public Terreno getTerreno() {
        return terreno;
    }

    public void setTerreno(Terreno terreno) {
        this.terreno = terreno;
    }
}
