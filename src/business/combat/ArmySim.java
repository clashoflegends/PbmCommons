/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.combat;

import business.facade.ExercitoFacade;

import baseLib.BaseModel;
import business.interfaces.IExercito;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
    /**
     * Combat intent, the Judge's {@code ExercitoControl.combateNivel}. Editable, because without it
     * the simulator cannot tell "defend in place" from "storm the city".
     *
     * Defaults to {@link CombatLevel#ATTACK_ARMY}: an army loaded into the simulator is there to be
     * fought over, so defaulting to defend-only would silently answer a different question than the
     * one the player asked. It stops short of the city, which is the half that needs an explicit
     * order.
     */
    private CombatLevel combatLevel = CombatLevel.ATTACK_ARMY;
    /**
     * Whom to attack, mirroring {@code ExercitoControl.getCombateNacaoNumero()}: null means every
     * enemy, anything else singles out one nation.
     */
    private Nacao targetNacao = null;
    /**
     * The server's own description of how big this army is, when that is all the player was told.
     *
     * At visibility level 1 an enemy arrives with a name, a nation and a SIZE BAND
     * ({@code tamanhoExercito} / {@code tamanhoEsquadra}) and NO platoons at all, so the troop count
     * reads zero. The band is the only thing the player actually knows about its strength, and
     * dropping it - which this class used to do - left him with a blank where his real information
     * should be.
     *
     * Captured as the finished STRING rather than the two numbers, because turning them into a
     * description means picking between the army, fleet and garrison wordings, and
     * {@code ExercitoFacade.getDescricaoTamanho} already does that. Calling it once at load reuses
     * the shared branching instead of copying it, and it needs the real {@code Exercito}, which only
     * exists here.
     */
    private String sizeBand = "";
    private String comandanteNome;
    private Local local;
    private Terreno terreno;
    private Nacao nacao;
    private SortedMap<String, Pelotao> platoons = new TreeMap();
    private List<TipoTropa> troops = new ArrayList<>();

    public ArmySim(String name, Terreno terrain, Nacao nation) {
        this.setNome(name);
        this.comandanteNome = name;
        this.terreno = terrain;
        this.nacao = nation;
        //FIXME: needs to receive Local for the Battle to be resolved. Deal with this later.
    }

    public ArmySim(Exercito exercito) {
        this.moral = exercito.getMoral();
        doClonePelotoes(exercito.getPelotoes());
        this.local = exercito.getLocal();
        this.terreno = exercito.getLocal().getTerreno();
        this.tatica = exercito.getTatica();
        this.setCodigo(exercito.getCodigo());
        this.nacao = exercito.getNacao();
        this.sizeBand = new ExercitoFacade().getDescricaoTamanho(exercito);
        try {
            this.comandante = exercito.getComandante().getPericiaComandante();
            this.comandanteNome = exercito.getComandante().getNome();
            this.setNome(exercito.getComandante().getNome());
        } catch (NullPointerException ex) {
            this.setNome(SettingsManager.getInstance().getBundleManager().getString("GUARNICAO"));
        }
    }

    public ArmySim(ArmySim exercito) {
        this.moral = exercito.getMoral();
        doClonePelotoes(exercito.getPelotoes());
        this.local = exercito.getLocal();
        this.terreno = exercito.getTerreno();
        this.tatica = exercito.getTatica();
        this.setCodigo(exercito.getCodigo());
        this.nacao = exercito.getNacao();
        this.sizeBand = exercito.getSizeBand();
        try {
            this.comandante = exercito.getComandantePericia();
            this.comandanteNome = exercito.getNome();
            this.setNome(exercito.getNome());
        } catch (NullPointerException ex) {
            this.setNome(SettingsManager.getInstance().getBundleManager().getString("GUARNICAO"));
        }
    }

    /**
     * Gives this simulated army its OWN {@link Pelotao} objects.
     *
     * The simulator lets the player retype a platoon's quantity, training, weapon, armour and troop
     * type. Before this existed both copy constructors did {@code platoons.putAll(source)}, which
     * copies the MAP and shares the Pelotao instances - so editing a platoon in the simulator edited
     * the real army loaded from the EGF, for the rest of the session, everywhere in the Counselor.
     *
     * {@link Pelotao#clone()} is a shallow copy and that is exactly right here: the four editable
     * fields are primitives and come across by value, while {@code TipoTropa} stays a SHARED
     * reference on purpose. TipoTropa is the scenario's read-only troop catalogue; the simulator
     * points platoons at different entries but never edits an entry, and copying it would waste
     * memory and break identity comparisons against the catalogue.
     *
     * @param source the platoons to copy, left untouched
     */
    private void doClonePelotoes(SortedMap<String, Pelotao> source) {
        for (Map.Entry<String, Pelotao> entry : source.entrySet()) {
            this.platoons.put(entry.getKey(), entry.getValue().clone());
        }
    }

    /** How big the player was told this army is, or empty when he can count it himself. */
    public String getSizeBand() {
        return sizeBand == null ? "" : sizeBand;
    }

    public void setSizeBand(String sizeBand) {
        this.sizeBand = sizeBand;
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

    /** Combat intent. Never null. */
    public CombatLevel getCombatLevel() {
        return combatLevel;
    }

    public void setCombatLevel(CombatLevel combatLevel) {
        this.combatLevel = combatLevel == null ? CombatLevel.DEFEND_ONLY : combatLevel;
    }

    /** The single nation this army will attack, or null for every enemy. */
    public Nacao getTargetNacao() {
        return targetNacao;
    }

    public void setTargetNacao(Nacao targetNacao) {
        this.targetNacao = targetNacao;
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
