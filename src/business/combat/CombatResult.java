package business.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
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
    /**
     * Keyed per army AND PER LAYER, because a battle is three fights and they can end differently.
     *
     * A fleet can win at sea and the troops it lands can still be destroyed ashore; an army can
     * sweep the field and then break on the city walls. One verdict per army would have to pick one
     * of those and hide the rest, so the roster shows three marks and each says what happened in its
     * own layer. A layer with no entry has not been resolved - today that is the sea and the city.
     */
    private final Map<IExercito, Map<CombatLayer, Outcome>> outcomes = new IdentityHashMap<>();
    private final List<RoundLoss> roundLosses = new ArrayList<>();
    private final List<RoundDamage> roundDamage = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();
    private final Map<String, Integer> noteCounts = new java.util.HashMap<>();
    private int rounds;

    /**
     * What one platoon lost in one round.
     *
     * Kept because a total tells you THAT a forecast is wrong and a per-round trace tells you WHERE:
     * two runs that differ by a hundred troops at the end may agree perfectly until round 4, and
     * that is the round worth reading. It is also what the results pane needs (T-802) - John asked
     * to see the turn-by-turn numbers and the round count per layer, data points, no narrative.
     */
    public static class RoundLoss {

        private final int round;
        private final ArmySim army;
        private final Pelotao platoon;
        private final int lost;
        private final int left;

        RoundLoss(int round, ArmySim army, Pelotao platoon, int lost, int left) {
            this.round = round;
            this.army = army;
            this.platoon = platoon;
            this.lost = lost;
            this.left = left;
        }

        public int getRound() {
            return round;
        }

        /**
         * The SCENARIO'S own army, not the clone that fought: the resolver translates before
         * recording, because everything that reads this - the results pane, a fidelity diff -
         * speaks in terms of the armies the player is looking at.
         */
        public ArmySim getArmy() {
            return army;
        }

        public Pelotao getPlatoon() {
            return platoon;
        }

        public int getLost() {
            return lost;
        }

        public int getLeft() {
            return left;
        }
    }

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

    public void setOutcome(IExercito original, CombatLayer layer, Outcome outcome) {
        Map<CombatLayer, Outcome> byLayer = outcomes.get(original);
        if (byLayer == null) {
            byLayer = new EnumMap<>(CombatLayer.class);
            outcomes.put(original, byLayer);
        }
        byLayer.put(layer, outcome);
    }

    /**
     * How this army ended THIS layer, or null when the layer was not resolved.
     *
     * Null is a real answer and the roster must keep it distinguishable: the sea and city layers do
     * not resolve yet, so "no verdict" has to read as "not simulated" and never as "took no part".
     */
    public Outcome getOutcome(IExercito original, CombatLayer layer) {
        final Map<CombatLayer, Outcome> byLayer = outcomes.get(original);
        return byLayer == null ? null : byLayer.get(layer);
    }

    /** One attack, as it landed: who hit whom, with what, for how much. */
    public static class RoundDamage {

        private final int round;
        private final ArmySim attacker;
        private final ArmySim defender;
        private final long attack;
        private final long damage;

        RoundDamage(int round, ArmySim attacker, ArmySim defender, long attack, long damage) {
            this.round = round;
            this.attacker = attacker;
            this.defender = defender;
            this.attack = attack;
            this.damage = damage;
        }

        public int getRound() {
            return round;
        }

        public ArmySim getAttacker() {
            return attacker;
        }

        public ArmySim getDefender() {
            return defender;
        }

        /** {@code ataqueFinal} - per PAIR, since both modifiers depend on the defender. */
        public long getAttack() {
            return attack;
        }

        public long getDamage() {
            return damage;
        }
    }

    public void addRoundDamage(int round, ArmySim attacker, ArmySim defender, long attack,
            long damage) {
        roundDamage.add(new RoundDamage(round, attacker, defender, attack, damage));
    }

    /**
     * Every blow, round by round. The Judge publishes exactly this, which is what makes a forecast
     * checkable against a real turn at the level of a single attack rather than a final total.
     */
    public List<RoundDamage> getRoundDamage() {
        return Collections.unmodifiableList(roundDamage);
    }

    /** Records a platoon's losses for one round, keyed on the SCENARIO'S army and platoon. */
    public void addRoundLoss(int round, ArmySim army, Pelotao platoon, int lost, int left) {
        if (lost > 0) {
            roundLosses.add(new RoundLoss(round, army, platoon, lost, left));
        }
    }

    /** Every platoon's losses, round by round, in the order they happened. */
    public List<RoundLoss> getRoundLosses() {
        return Collections.unmodifiableList(roundLosses);
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

    /**
     * A note that carries a NUMBER, kept apart from the key so the view still owns the wording.
     *
     * "Some armies have no morale" is a footnote; "4 armies have no morale" is a reason to go and
     * fix them. The count is the part that makes the caveat actionable, and it cannot be recovered
     * later from the key alone.
     */
    public void addNote(String note, int count) {
        notes.add(note);
        noteCounts.put(note, count);
    }

    /** The number behind a note, or 0 when it carries none. */
    public int getNoteCount(String note) {
        final Integer ret = noteCounts.get(note);
        return ret == null ? 0 : ret;
    }
}
