/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.combat;

import business.facade.BattleSimFacade;
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
    /** BORROWED, never edited. See {@link #getComandanteModel}. */
    private Personagem comandanteModel;
    /** Wiped out DURING the simulation. See {@link #isDisband}. */
    private boolean disband = false;
    private Local local;
    private Terreno terreno;
    private Nacao nacao;
    private SortedMap<String, Pelotao> platoons = new TreeMap();

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
        // A GARRISON IS AN ARMY WITHOUT A COMMANDER, and that is all it is. Reading the skill off
        // getComandante() threw for exactly those armies, and the catch set only the name - so the
        // field initializer stood and every garrison entered the simulator with a commander of
        // skill 10. Three things went wrong at once: BattleSimFacade overstated the army (the
        // skill is a quarter of its combat value), isGarrison() - which is comandante <= 0 -
        // answered FALSE for every garrison, and the spinner showed the player a 10 as though it
        // had been read from somewhere. Exercito's own accessors answer 0 and "" here, so there is
        // nothing to catch.
        this.comandante = exercito.getComandantePericia();
        this.comandanteNome = exercito.getComandanteNome();
        this.comandanteModel = exercito.getComandante();
        this.setNome(exercito.isGarrison()
                ? SettingsManager.getInstance().getBundleManager().getString("GUARNICAO")
                : exercito.getComandanteNome());
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
        // Clone army has to mean CLONE. These four were dropped, so a clone silently reverted to
        // the field defaults: an army the player had set to "Defend only" came back as
        // ATTACK_ARMY - reported as initiating combat and entering a layer the original does not
        // enter - a target-nation restriction became "attack everyone", and hand-entered attack
        // and defense bonuses reset to zero. Nothing announced any of it.
        this.combatLevel = exercito.getCombatLevel();
        this.targetNacao = exercito.getTargetNacao();
        this.bonusAttack = exercito.getAttackBonus();
        this.bonusDefense = exercito.getArmyDefenseBonus();
        this.comandante = exercito.getComandantePericia();
        this.comandanteNome = exercito.getComandanteNome();
        this.comandanteModel = exercito.getComandanteModel();
        this.setNome(exercito.getNome());
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

    /**
     * The real commander, BORROWED. Returning null here was dropping a combat bonus.
     *
     * Its only consumer is {@code ExercitoFacade.isHero}, which reads
     * {@code getComandanteModel().isHero()} inside a {@code catch (NullPointerException) return
     * false} - so a null here did not fail, it answered "not a hero", and
     * {@code BattleSimFacade.getPlatoonDefense} silently dropped the {@code ;TAH;} hero defence
     * bonus from every army in the simulator. A wrong number with no warning, which is the one
     * failure mode this whole rebuild is against.
     *
     * BORROWED, like {@code Nacao} and {@code TipoTropa} and unlike {@code Pelotao}: the simulator
     * never edits the commander object. What the player edits is {@code comandante}, the skill as
     * an int, which is a field of this class. Hero-ness is not editable and not his to change.
     */
    @Override
    public Personagem getComandanteModel() {
        return comandanteModel;
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

    /**
     * The troop types present, rebuilt on every call.
     *
     * It used to fill a {@code troops} field once and never look again, which was a trap sitting on
     * the one class the engine is going to consume: this simulator exists so the player can add,
     * remove and retype platoons, and every one of those left the cached list describing an army
     * that no longer exists. Nothing calls this yet, so it never fired - which is precisely why it
     * had to go now rather than after something depends on it. The list is a handful of entries
     * over a map this object owns; there was nothing here worth caching.
     */
    public Collection<TipoTropa> getTipoTropa() {
        final List<TipoTropa> ret = new ArrayList<>();
        for (Pelotao platoon : platoons.values()) {
            if (platoon.getTipoTropa() != null && !ret.contains(platoon.getTipoTropa())) {
                ret.add(platoon.getTipoTropa());
            }
        }
        return ret;
    }

    /**
     * By land strength, descending-capable, as {@code ExercitoControl.compareTo} does.
     *
     * NOT inherited. {@code BaseModel.compareTo} compares {@code getCodigo()} as a string, and the
     * Judge's engine sorts armies by this in two places it never names - {@code getExercitosSorted}
     * and the {@code TreeMap} keyed on the army in {@code CombatLand}'s casualty snapshot. Left
     * inherited, the engine would have resolved a battle in ALPHABETICAL order instead of strength
     * order, silently, and a blank army (whose codigo is null) would have taken the snapshot down
     * with an NPE. Neither shows up in any search for calls on the army.
     *
     * Guarded on type and on a missing hex, because this class has a constructor that takes neither
     * a codigo nor a Local - the Judge's cast to its own concrete type cannot be copied here.
     */
    @Override
    public int compareTo(Object other) {
        if (!(other instanceof ArmySim)) {
            return super.compareTo(other);
        }
        return strength() - ((ArmySim) other).strength();
    }

    /** {@code ExercitoControl.getForcaBasicaLand()}, through the facade the Judge itself uses. */
    private int strength() {
        return local == null ? 0 : new BattleSimFacade().getArmyAttackBaseLand(this, local);
    }

    @Override
    public boolean isGarrison() {
        return comandante <= 0;
    }

    /**
     * Has this army been wiped out DURING the simulation?
     *
     * The flag, not "has no platoons", which is what this used to answer. The two are different
     * claims and the difference is the unscouted enemy: the server sends it with a size band and no
     * platoons at all, so an army nobody has scouted would have reported itself destroyed before a
     * blow was struck. Same mistake as the old DESTROYED_EARLIER reason, one layer down.
     *
     * {@code ExercitoFacade.subTropaQt} sets it when the last troop dies, which is the Judge's own
     * contract for the field on {@code ExercitoControl}.
     */
    @Override
    public boolean isDisband() {
        return disband;
    }

    @Override
    public void setDisband(boolean disband) {
        this.disband = disband;
    }

    /**
     * The magic-defence exchange writes this five times per land combat, and it used to THROW.
     *
     * Worth stating because of how it hid: the method is declared on {@link IExercito}, so every
     * compile-time check said the contract was met, and only running the engine would have found
     * it. The Judge's is a plain int field ({@code ExercitoControl.setArmyDefenseBonus}), which is
     * what {@code bonusDefense} already is here - the stub was never needed, only unwritten.
     */
    @Override
    public void setArmyDefenseBonus(int bonus) {
        this.bonusDefense = bonus;
    }

    /**
     * Destroyed: marked, and emptied so every layer stops counting it.
     *
     * The Judge's version unregisters the army from the Partida, the Nacao and the Hexagono - server
     * object-graph surgery that has no meaning here. For a simulation "disbanded" means exactly
     * "takes no further part", and clearing the platoons is what says so in the vocabulary the rest
     * of this package already reads: {@code getQtTropasTotal} falls to zero and
     * {@code LayerParticipation} drops it from all three layers with NO_TROOPS.
     */
    @Override
    public void doDisband() {
        this.disband = true;
        this.platoons.clear();
    }

    /**
     * Same thing, and the "WithMsg" half is owed.
     *
     * {@code ExercitoFacade.isPrecisaDebandar} calls this, so it has to work, and the Judge's copy
     * adds a line to the army's result text. The simulator has no narrative stream yet - that is
     * T-802/T-803 - so it does the disbanding and silently owes the sentence. Named here rather
     * than left as a silent equivalence, because "with msg" is a promise this does not yet keep.
     */
    @Override
    public void doDisbandWithMsg() {
        doDisband();
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
