package business.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import business.interfaces.IExercito;
import model.Pelotao;

/**
 * What a simulated battle did to each platoon: how many were left, and how many died.
 *
 * <h3>Keyed to the platoons the player is looking at</h3>
 *
 * The resolution runs on COPIES (see {@link LandCombatResolver}), so the numbers have to be handed
 * back against the originals or the window could not show them. The map is identity-keyed on the
 * scenario's own {@link Pelotao} objects, which is also what stops two platoons of the same troop
 * type in different armies from colliding.
 *
 * <h3>Absent is not zero</h3>
 *
 * A platoon that never took part - a ship in a land battle, an army with no enemy - has NO entry
 * here, and {@link #has} is how the table tells that from a platoon that took part and survived
 * intact. The columns show "--" for the first and a number for the second, which are different
 * statements and must not be collapsed.
 */
public class CombatResult {

    /**
     * How one army came out of the battle. The Judge's own three-way split, from the report it
     * writes at the end of a combat: took no part, won, or was destroyed.
     *
     * {@link #DID_NOT_FIGHT} is the one that cannot be re-derived from casualties afterwards. An
     * army that fought and lost nobody and an army that stood and watched both end at full strength,
     * and only the resolver knows which is which.
     */
    public enum Outcome {
        /** Left standing with an enemy it had actually engaged. */
        WON,
        /** Wiped out, or disbanded during the battle. */
        LOST,
        /**
         * Still standing when the round cap stopped the battle. Not a winner.
         *
         * A capped run means neither side could finish the other, so calling everyone left alive a
         * winner would contradict the very sentence beside it. The armies that were destroyed on the
         * way still lost; what is undecided is who would eventually have won.
         */
        UNDECIDED,
        /** Present on the hex but never engaged: no hostile army it could reach on this layer. */
        DID_NOT_FIGHT
    }

    private final Map<Pelotao, Integer> after = new IdentityHashMap<>();
    private final Map<Pelotao, Integer> before = new IdentityHashMap<>();
    private final Map<IExercito, Outcome> outcomes = new IdentityHashMap<>();
    private final List<String> notes = new ArrayList<>();
    private int rounds;

    /** @param survivors how many of this platoon are left when the battle ends */
    public void put(Pelotao original, int started, int survivors) {
        before.put(original, started);
        after.put(original, survivors);
    }

    /** Did this platoon take part at all? See the class note: absent is not the same as zero. */
    public boolean has(Pelotao original) {
        return after.containsKey(original);
    }

    /** How many are left, or -1 when this platoon took no part. */
    public int getAfter(Pelotao original) {
        final Integer ret = after.get(original);
        return ret == null ? -1 : ret;
    }

    /** How many died, or -1 when this platoon took no part. */
    public int getLost(Pelotao original) {
        final Integer was = before.get(original);
        return was == null ? -1 : was - after.get(original);
    }

    public void setOutcome(IExercito original, Outcome outcome) {
        outcomes.put(original, outcome);
    }

    /** How this army ended the battle, or null before a run and for an army that was not in it. */
    public Outcome getOutcome(IExercito original) {
        return outcomes.get(original);
    }

    public int getRounds() {
        return rounds;
    }

    public void setRounds(int rounds) {
        this.rounds = rounds;
    }

    /**
     * Anything the run could not do faithfully, in the player's words.
     *
     * The fidelity footer (T-804) is built from these. A result that quietly omits what it skipped
     * is worse than one that refuses to run, because it looks complete.
     */
    public List<String> getNotes() {
        return Collections.unmodifiableList(notes);
    }

    public void addNote(String note) {
        notes.add(note);
    }
}
