/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.combat;

import baseLib.BaseModel;
import business.interfaces.IExercito;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Exercito;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Personagem;
import model.Terreno;
import model.TipoTropa;
import persistenceCommons.SettingsManager;

/**
 *
 * @author jmoura
 */
public class ArmySim extends BaseModel implements IExercito {

    private int moral = 10;
    private int comandante = 10;
    private int tatica = 0;
    private int bonusAttack = 0, bonusDefense = 0;
    private String comandanteNome;
    private Local local;
    private Terreno terreno;
    private Nacao nacao;
    private SortedMap<String, Pelotao> platoons = new TreeMap();
    private List<TipoTropa> troops = new ArrayList<TipoTropa>();

    public ArmySim(String name, Terreno terrain, Nacao nation) {
        this.setNome(name);
        this.comandanteNome = name;
        this.terreno = terrain;
        this.nacao = nation;
        //FIXME: needs to receive Local for the Battle to be resolved. Deal with this later.
    }

    public ArmySim(Exercito exercito) {
        this.moral = exercito.getMoral();
        this.platoons.putAll(exercito.getPelotoes());
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

    public ArmySim(ArmySim exercito) {
        this.moral = exercito.getMoral();
        this.platoons.putAll(exercito.getPelotoes());
        this.local = exercito.getLocal();
        this.terreno = exercito.getTerreno();
        this.nacao = exercito.getNacao();
        try {
            this.comandante = exercito.getComandantePericia();
            this.comandanteNome = exercito.getNome();
            this.setNome(exercito.getNome());
        } catch (NullPointerException ex) {
            this.setNome(SettingsManager.getInstance().getBundleManager().getString("GUARNICAO"));
        }
        //TODO: clone pelotoes para poder mudar valores sem afetar original
    }

    @Override
    public int getComandantePericia() {
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

    @Override
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

    @Override
    public Personagem getComandanteModel() {
        //FIXME: someday, do commander here.
        return null;
    }

    @Override
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

    @Override
    public int getAttackBonus() {
        return bonusAttack;
    }

    public void setBonusAttack(int bonusAttack) {
        this.bonusAttack = bonusAttack;
    }

    @Override
    public int getArmyDefenseBonus() {
        return bonusDefense;
    }

    public void setBonusDefense(int bonusDefense) {
        this.bonusDefense = bonusDefense;
    }

    public Collection<TipoTropa> getTipoTropa() {
        if (this.troops.isEmpty() && this.platoons.size() > 0) {
            for (Pelotao platoon : platoons.values()) {
                this.troops.add(platoon.getTipoTropa());
            }
        }
        return this.troops;
    }

    @Override
    public void doDisband() {
        //destroy army to remove it from combat
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isGarrison() {
        return comandante <= 0;
    }

    @Override
    public boolean isDisband() {
        return getPelotoes().isEmpty();
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
