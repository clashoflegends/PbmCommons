package business.combat;

import business.facade.ExercitoFacade;
import business.interfaces.IExercito;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import model.Cidade;
import model.Local;
import model.Nacao;
import model.Partida;
import model.Pelotao;
import model.Terreno;

/**
 * Everything one BattleSim window owns: the armies, who fights whom, the ground, the city, and how
 * trustworthy each number is.
 *
 * <h3>Why this exists</h3>
 *
 * All of this state used to live scattered across the controller as loose fields and GUI widgets -
 * the army list, the selected row, the terrain combo, three city sliders. That made it impossible to
 * say what a "scenario" was, to have two of them open at once, or to test any of it without a
 * window. It is one object now, it holds no Swing, and it is the thing an engine will eventually be
 * handed.
 *
 * <h3>Ownership</h3>
 *
 * The scenario OWNS its {@link ArmySim}s and their {@link Pelotao}s, and BORROWS everything else
 * ({@code TipoTropa}, {@code Nacao}, {@code Terreno}, {@code Local}, {@code Cidade}) as read-only
 * references. The full table and the reasoning are in {@code BattleSimFacade}'s javadoc; it is
 * enforced by {@code ArmySimOwnershipTest}.
 *
 * <h3>Provenance lives here, not on the model</h3>
 *
 * Per-platoon provenance is held in an identity-keyed side map rather than as a field on
 * {@link Pelotao}, because {@code Pelotao} is a {@code model.*} class serialized into the EGF and
 * this is pure UI scratch state. A field there would be an EGF compatibility change with twenty years
 * of saved games behind it, in exchange for nothing. Identity keying also avoids the
 * reference-keyed-map hazard that bites EGF-deserialized {@code TreeMap}s.
 */
public class CombatScenario {

    /** How much to trust a number. */
    public enum Provenance {
        /** Read from the player's own EGF, exact. */
        EXACT,
        /** The EGF's best estimate of something the player cannot see precisely. */
        ESTIMATED,
        /** The player typed it. */
        MANUAL
    }

    private final List<ArmySim> armies = new ArrayList<>();
    private final Map<ArmySim, Provenance> armyProvenance = new IdentityHashMap<>();
    private final Map<Pelotao, Provenance> platoonProvenance = new IdentityHashMap<>();
    private final Set<Nacao> loadedNacoes = new LinkedHashSet<>();
    private final HostilityDeriver deriver = new HostilityDeriver();
    private final ExercitoFacade exercitoFacade = new ExercitoFacade();

    private Partida partida;
    private Local local;
    private Terreno terreno;
    private Cidade cidade;
    private boolean cityParticipates;

    public CombatScenario() {
    }

    /**
     * @param partida the game, for its type flags
     * @param local   the hex this scenario is about, or null for a free-standing what-if
     */
    public CombatScenario(Partida partida, Local local) {
        this.partida = partida;
        setLocal(local);
    }

    /**
     * Points the scenario at a hex, taking its terrain and city.
     *
     * City participation defaults to ON when the hex holds one and OFF otherwise, rather than being
     * always-on with a dummy city standing in. A dummy has no owner, which is how the old code ended
     * up special-casing a null nation inside the damage maths.
     */
    public final void setLocal(Local local) {
        this.local = local;
        if (local != null) {
            this.terreno = local.getTerreno();
            this.cidade = local.getCidade();
        }
        this.cityParticipates = this.cidade != null;
    }

    public Partida getPartida() {
        return partida;
    }

    public void setPartida(Partida partida) {
        this.partida = partida;
    }

    public Local getLocal() {
        return local;
    }

    public Terreno getTerreno() {
        return terreno;
    }

    /** What-if: fight the same armies on different ground. */
    public void setTerreno(Terreno terreno) {
        this.terreno = terreno;
    }

    public Cidade getCidade() {
        return cidade;
    }

    public void setCidade(Cidade cidade) {
        this.cidade = cidade;
    }

    /** Is the city part of this fight? Always false when there is no city. */
    public boolean isCityParticipates() {
        return cityParticipates && cidade != null;
    }

    public void setCityParticipates(boolean cityParticipates) {
        this.cityParticipates = cityParticipates;
    }

    /** The city as a combat participant, or null when it takes no part. */
    public Cidade getCidadeAtiva() {
        return isCityParticipates() ? cidade : null;
    }

    /**
     * Nations whose relationship rows may be trusted: the observer's own, plus any ally whose EGF has
     * been merged. Known rows scale with EGFs loaded, not with one point of view.
     */
    public void addLoadedNacao(Nacao nacao) {
        if (nacao != null) {
            loadedNacoes.add(nacao);
        }
    }

    public Collection<Nacao> getLoadedNacoes() {
        return Collections.unmodifiableSet(loadedNacoes);
    }

    public List<ArmySim> getArmies() {
        return Collections.unmodifiableList(armies);
    }

    /**
     * Adds an army and records how trustworthy its numbers are.
     *
     * @param provenance {@link Provenance#EXACT} for the player's own armies, {@link Provenance#ESTIMATED}
     *                   for what he can only see from outside
     */
    public void addArmy(ArmySim army, Provenance provenance) {
        if (army == null) {
            return;
        }
        armies.add(army);
        armyProvenance.put(army, provenance == null ? Provenance.ESTIMATED : provenance);
        for (Pelotao pelotao : army.getPelotoes().values()) {
            platoonProvenance.put(pelotao, provenance == null ? Provenance.ESTIMATED : provenance);
        }
    }

    public void remArmy(ArmySim army) {
        armies.remove(army);
        armyProvenance.remove(army);
        for (Pelotao pelotao : army.getPelotoes().values()) {
            platoonProvenance.remove(pelotao);
        }
    }

    public Provenance getProvenance(ArmySim army) {
        final Provenance ret = armyProvenance.get(army);
        return ret == null ? Provenance.MANUAL : ret;
    }

    /** An untracked platoon is one the player added himself, so its numbers are his. */
    public Provenance getProvenance(Pelotao pelotao) {
        final Provenance ret = platoonProvenance.get(pelotao);
        return ret == null ? Provenance.MANUAL : ret;
    }

    /** Call when the player edits a value: what he typed is his, whatever it was before. */
    public void setEdited(Pelotao pelotao) {
        if (pelotao != null) {
            platoonProvenance.put(pelotao, Provenance.MANUAL);
        }
    }

    /**
     * The hostility matrix, rebuilt on every call.
     *
     * NOT cached, on purpose. The inputs change from several directions - a new army, a removed one,
     * an edited nation, a newly merged ally EGF - and only some of those pass through this class, so
     * any cache would need an invalidation rule that callers could forget. A stale matrix is the kind
     * of wrong that looks right. The cost is a pairwise pass over at most a few dozen armies, which
     * is nothing next to being quietly wrong about who is fighting whom.
     */
    public HostilityMatrix getMatrix() {
        return deriver.derive(partida, armies, loadedNacoes);
    }

    /**
     * Where every army fights, and why each sits out what it sits out.
     *
     * Roster-level because naval and land participation are decided by PAIRING: holding ships does
     * not put an army at sea unless an enemy also holds ships. Derived on demand rather than stored,
     * because the Judge re-evaluates between layers.
     */
    public Map<ArmySim, LayerParticipation> getParticipation() {
        final Cidade active = getCidadeAtiva();
        final Map<ArmySim, Boolean> hostileToCity = new IdentityHashMap<>();
        for (ArmySim army : armies) {
            hostileToCity.put(army, active != null
                    && deriver.isHostileToCity(partida, army, active.getNacao(), loadedNacoes));
        }
        return LayerParticipation.forRoster(armies, terreno, active, getMatrix(), hostileToCity);
    }

    /** Where this one army fights. Prefer {@link #getParticipation()} when asking about several. */
    public LayerParticipation getParticipation(ArmySim army) {
        return getParticipation().get(army);
    }

    /** Which armies take part in a given layer. */
    public List<ArmySim> getArmies(CombatLayer layer) {
        final List<ArmySim> ret = new ArrayList<>();
        final Map<ArmySim, LayerParticipation> all = getParticipation();
        for (ArmySim army : armies) {
            if (all.get(army).isIn(layer)) {
                ret.add(army);
            }
        }
        return ret;
    }

    /** Is there anything to resolve at all? */
    public boolean hasCombat() {
        return getMatrix().hasCombat();
    }

    /** How many hostile pairs were guessed rather than read or derived. Feeds the disclosure line. */
    public int getAssumedCount() {
        return getMatrix().getAssumedPairs().size();
    }

    /** Total troops across every army, for the roster header. */
    public int getQtTropasTotal() {
        int ret = 0;
        for (IExercito army : armies) {
            ret += exercitoFacade.getQtTropasTotal(army);
        }
        return ret;
    }
}
