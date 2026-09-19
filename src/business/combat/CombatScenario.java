package business.combat;

import business.facade.BattleSimFacade;
import business.facade.ExercitoFacade;
import business.interfaces.IExercito;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import model.Cidade;
import model.Jogador;
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
        /**
         * Anything the player did not read from his own EGF: an ally he has not merged, or an enemy.
         *
         * Covers both a real platoon list seen from outside and the placeholder pair the server
         * sends for an army the player has not scouted. Deliberately ONE value for both: what the
         * EGF carries about a foreign army is the player's intelligence problem, which is the game,
         * not a data-quality problem for the tool to editorialise about. See {@code ScenarioLoader}.
         */
        ESTIMATED,
        /** The player typed it. */
        MANUAL
    }

    private final List<ArmySim> armies = new ArrayList<>();
    private final Map<ArmySim, Provenance> armyProvenance = new IdentityHashMap<>();
    private final Map<Pelotao, Provenance> platoonProvenance = new IdentityHashMap<>();
    private final Map<ArmySim, Map<ArmySim, Boolean>> hostilityEdits = new IdentityHashMap<>();
    private final HostilityDeriver deriver = new HostilityDeriver();
    private final BattleSimFacade battleSimFacade = new BattleSimFacade();
    private final ExercitoFacade exercitoFacade = new ExercitoFacade();

    private Partida partida;
    private Jogador observer;
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
            // CLONED, not borrowed: loyalty, size and fortification are all editable here, and the
            // same sharing bug that let a retyped platoon reach the loaded world would let a
            // retyped city do it. Cidade.clone() is shallow and that is the contract.
            this.cidade = local.getCidade() == null ? null : local.getCidade().clone();
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
     * Gives an ownerless city an owner, because every city has one and the shared combat code
     * assumes it.
     *
     * 27 of the 207 cities in a live GoT12c EGF arrive with no {@code nacao} and a size above zero:
     * the player simply cannot see who holds them. {@code getCityDefenseCombat} reads the owner for
     * its four nation powers and {@code log.error}s when there is none - a fair question on the
     * server, a false alarm on the client.
     *
     * The fix is to supply the missing INPUT, not to route around the shared method. Recomposing
     * the minimum data the Judge's code needs is the whole path to sharing it; branching away from
     * it on the client would have been a second implementation, which is the thing this design is
     * trying to avoid. The caller decides what to stand in - the Barbarians nation, today - because
     * finding it is Counselor knowledge.
     *
     * Post-MVP this is one more cell for the same gap-filling pass as the relationship matrix: an
     * assumed owner is a guess, and the player should be able to correct it.
     */
    public void setCityOwnerIfUnknown(Nacao fallback) {
        if (fallback != null && cidade != null && cidade.getNacao() == null) {
            cidade.setNacao(fallback);
        }
    }

    /**
     * The city's loyalty, 0 to 100. The player may state a different one.
     *
     * Loyalty is not a flavour field here: it multiplies the whole defense, and a city at zero
     * loyalty defends at DOUBLE rather than at nothing - see {@code BattleSimFacade.getCityDefense}.
     * That is the sort of thing a player is most likely to have backwards.
     */
    public void setCityLealdade(int lealdade) {
        if (cidade != null) {
            cidade.setLealdade(clamp(lealdade, 0, 100));
        }
    }

    /** The city's size, 0 (ruins) to 5 (metropolis). */
    public void setCityTamanho(int tamanho) {
        if (cidade != null) {
            cidade.setTamanho(clamp(tamanho, 0, 5));
        }
    }

    /** The city's fortification, 0 (none) to 5 (fortress). */
    public void setCityFortificacao(int fortificacao) {
        if (cidade != null) {
            cidade.setFortificacao(clamp(fortificacao, 0, 5));
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * What the attackers are actually up against in round 1, and what comes back at them.
     *
     * <b>{@code getCityDefenseCombat}, deliberately not {@code getCityDefense}.</b> The two differ
     * by {@code defenseBonus} and four nation powers - {@code ;PFD;} when fortified, {@code ;PCD;}
     * in mountains, {@code ;NWD;} in forest, {@code ;NWS;} in swamp - and the Judge's
     * {@code CombateTmpbm} uses the combat one via {@code CidadeControl.getDefesaPlusBonusCombate}.
     * The old BattleSim displayed the BASE figure, so it has always shown a number the battle does
     * not use, wrong in the attacker's favour whenever the defender holds any of those powers.
     *
     * This single number is the city's whole contribution: the Judge distributes it back at the
     * attackers in proportion to troop count. A city is passive - it is attacked, it damages the
     * attackers, and it takes the result.
     */
    public int getCityDefense() {
        final Cidade active = getCidadeAtiva();
        return active == null ? 0 : battleSimFacade.getCityDefenseCombat(active);
    }

    /**
     * What round 0 attacks: the fortification, separately from the city's own defense.
     *
     * The city layer is TWO rounds, not one. Round 0 is siege engines against the fortification and
     * it happens FIRST, so a fortification it reduces lowers the defense that round 1 then computes
     * - the city equivalent of the army layer's first-strike round. Only fought when at least one
     * attacker carries siege engines.
     */
    public int getCityFortificationDefense() {
        final Cidade active = getCidadeAtiva();
        return active == null ? 0 : battleSimFacade.getCityFortficationDefense(active);
    }

    /**
     * Will there be a round 0 at all?
     *
     * True when any army that will actually assault the city carries siege engines. Asking the
     * whole roster would be wrong: an army that is not attacking the city does not bring its
     * engines to the walls.
     */
    public boolean isSiegeExpected() {
        if (getCidadeAtiva() == null) {
            return false;
        }
        final Map<ArmySim, LayerParticipation> participation = getParticipation();
        for (ArmySim army : armies) {
            final LayerParticipation one = participation.get(army);
            if (one != null && one.isIn(CombatLayer.CITY) && exercitoFacade.isSiege(army)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The player at the keyboard. His own nations carry complete relationship rows, and that is the
     * only thing this is used for.
     */
    public Jogador getObserver() {
        return observer;
    }

    public void setObserver(Jogador observer) {
        this.observer = observer;
    }

    /**
     * The player's own answer for one pair, overriding whatever was derived.
     *
     * The matrix is the law for a simulation, so the player has to be able to state a different
     * diplomatic situation and see what falls out of it: "suppose he declares on me this turn". The
     * edit is symmetric, because hostility is.
     */
    public void setHostile(ArmySim one, ArmySim other, boolean hostile) {
        if (one == null || other == null || one == other) {
            return;
        }
        putEdit(one, other, hostile);
        putEdit(other, one, hostile);
    }

    private void putEdit(ArmySim from, ArmySim to, boolean hostile) {
        Map<ArmySim, Boolean> row = hostilityEdits.get(from);
        if (row == null) {
            row = new IdentityHashMap<>();
            hostilityEdits.put(from, row);
        }
        row.put(to, hostile);
    }

    /** Has the player overruled the derivation for this pair? */
    public boolean isEdited(ArmySim one, ArmySim other) {
        final Map<ArmySim, Boolean> row = hostilityEdits.get(one);
        return row != null && row.containsKey(other);
    }

    public int getEditedCount() {
        int ret = 0;
        for (Map<ArmySim, Boolean> row : hostilityEdits.values()) {
            ret += row.size();
        }
        return ret / 2;
    }

    /** Back to what the EGF and the game type say. The matrix dialog's "Reset to assumed". */
    public void clearHostilityEdits() {
        hostilityEdits.clear();
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
     * The hostility matrix: derived from the EGF and the game type, then the player's edits on top.
     *
     * <b>Rebuilt on every call, and the edits are what persist.</b> Caching the matrix itself would
     * need an invalidation rule for every input that can move - a new army, a removed one, an army
     * retyped to a different nation - and callers would forget one. A stale matrix is the kind of
     * wrong that looks right. Keeping the player's overrides as the durable state and re-deriving
     * around them gets persistence without the cache. The cost is a pairwise pass over at most a few
     * dozen armies.
     */
    public HostilityMatrix getMatrix() {
        final HostilityMatrix ret = deriver.derive(partida, armies, observer);
        for (ArmySim one : armies) {
            final Map<ArmySim, Boolean> row = hostilityEdits.get(one);
            if (row == null) {
                continue;
            }
            for (Map.Entry<ArmySim, Boolean> entry : row.entrySet()) {
                if (Boolean.TRUE.equals(entry.getValue())) {
                    ret.setHostile(one, entry.getKey(), HostilityMatrix.Origin.PLAYER_EDITED);
                } else {
                    ret.setNotHostile(one, entry.getKey());
                }
            }
        }
        return ret;
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
                    && deriver.isHostileToCity(partida, army, active.getNacao(), observer));
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
