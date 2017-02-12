/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;
import business.interfaces.IExercito;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import persistenceCommons.SettingsManager;

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
    private SortedMap<String, Pelotao> platoons = new TreeMap();
    private List<TipoTropa> troops = new ArrayList<TipoTropa>();

    public ExercitoSim(Exercito exercito) {
        this.moral = exercito.getMoral();
        this.platoons = exercito.getPelotoes();
        this.local = exercito.getLocal();
        this.terreno = exercito.getLocal().getTerreno();
        this.nacao = exercito.getNacao();
        try {
            this.comandante = exercito.getComandante().getPericiaComandante();
            this.comandanteNome = exercito.getComandante().getNome();
            this.setNome(exercito.getComandante().getNome());
        } catch (NullPointerException ex) {
            this.setNome(SettingsManager.getInstance().getBundleManager().getString("GUARNICAO"));
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
        return platoons;
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

    @Override
    public Terreno getTerreno() {
        return terreno;
    }

    public void setTerreno(Terreno terreno) {
        this.terreno = terreno;
    }

    public Collection<TipoTropa> getTipoTropa() {
        if (this.troops.isEmpty() && this.platoons.size() > 0) {
            for (Pelotao platoon : platoons.values()) {
                this.troops.add(platoon.getTipoTropa());
            }
        }
        return this.troops;
    }
}
