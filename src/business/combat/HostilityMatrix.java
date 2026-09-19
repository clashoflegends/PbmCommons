package business.combat;

import business.interfaces.IExercito;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Who is fighting whom, as a symmetric pairwise relation over the armies in a scenario, plus WHERE
 * each answer came from.
 *
 * <h3>Why pairwise and not "sides"</h3>
 *
 * The Judge has no concept of sides. {@code CombatArmy.isCombatCleared()} tests every army against
 * every other with {@code isInimigo()} and pushes both directions into per-army enemy lists, so a
 * thirteen-way free-for-all is simply thirteen armies with thirteen enemy lists. A simulator built on
 * two sides could not represent what the Judge resolves, and would disagree on exactly the battles
 * where it matters.
 *
 * <h3>Why every cell carries an origin</h3>
 *
 * A client cannot always know the truth. It reads relationships only from the EGFs it has loaded;
 * beyond that it infers from the game type, and in a free-for-all it sometimes has to assume. Those
 * three are very different claims and collapsing them loses the only thing that makes an assumed
 * number safe to act on: knowing it was assumed. {@link Origin} keeps them apart, and it is what lets
 * the result disclose its own guesses instead of presenting them as fact.
 *
 * <h3>Identity, not equality</h3>
 *
 * Armies are keyed by object identity. Two distinct armies of the same nation, or two blank armies a
 * player just added, must stay distinct, and {@code BaseModel.compareTo} would collapse them by
 * codigo. Identity also sidesteps the reference-keyed map hazard that bites EGF-deserialized
 * {@code TreeMap}s.
 */
public class HostilityMatrix {

    /** Where a cell's answer came from. Never merge these: the whole point is telling them apart. */
    public enum Origin {
        /** Read from a relationship map in a loaded EGF. Authoritative. */
        READ_FROM_EGF,
        /** Implied by the game type, e.g. everyone is hostile in a Death Match. Reliable. */
        FROM_GAME_TYPE,
        /** Neither of the above could answer it, so a default was applied. Must be disclosed. */
        ASSUMED
    }

    private final Map<IExercito, Map<IExercito, Origin>> hostile = new IdentityHashMap<>();
    private final List<IExercito> armies = new ArrayList<>();
    private final List<IExercito[]> assumed = new ArrayList<>();

    /**
     * Registers an army so it appears in the matrix even if it turns out to be hostile to nobody.
     * An army fighting no one is a real and interesting state, not an absence.
     */
    public void addArmy(IExercito army) {
        for (IExercito known : armies) {
            if (known == army) {
                return;
            }
        }
        armies.add(army);
    }

    /** Every army in the scenario, in insertion order. */
    public List<IExercito> getArmies() {
        return Collections.unmodifiableList(armies);
    }

    /**
     * Records that two armies will fight, in both directions.
     *
     * Symmetric on purpose: the Judge writes both directions too
     * ({@code exercito.addInimigo(inimigo); inimigo.addInimigo(exercito);}). A one-way hostility
     * would mean one army attacks while the other does not defend, which the engine cannot express.
     *
     * An army is never hostile to itself; that call is ignored rather than rejected, because
     * derivation loops naturally compare every army with every army including itself.
     */
    public void setHostile(IExercito one, IExercito other, Origin origin) {
        if (one == other) {
            return;
        }
        addArmy(one);
        addArmy(other);
        put(one, other, origin);
        put(other, one, origin);
    }

    private void put(IExercito from, IExercito to, Origin origin) {
        Map<IExercito, Origin> row = hostile.get(from);
        if (row == null) {
            row = new IdentityHashMap<>();
            hostile.put(from, row);
        }
        row.put(to, origin);
    }

    /**
     * Will these two fight? Matches {@code ExercitoControl.isInimigo} semantics: an army is never its
     * own enemy, and anything not recorded as hostile is not hostile.
     */
    public boolean isInimigo(IExercito one, IExercito other) {
        if (one == other) {
            return false;
        }
        final Map<IExercito, Origin> row = hostile.get(one);
        return row != null && row.containsKey(other);
    }

    /**
     * @return where this cell's answer came from, or null when the two are not hostile. A null here
     *         means "they do not fight", not "unknown"; unknown-and-defaulted is {@link Origin#ASSUMED}.
     */
    public Origin getOrigin(IExercito one, IExercito other) {
        final Map<IExercito, Origin> row = hostile.get(one);
        return row == null ? null : row.get(other);
    }

    /** Every army this one will fight, in scenario order. */
    public List<IExercito> getInimigos(IExercito army) {
        final List<IExercito> ret = new ArrayList<>();
        for (IExercito other : armies) {
            if (isInimigo(army, other)) {
                ret.add(other);
            }
        }
        return ret;
    }

    /** Is anybody fighting anybody? The engine has nothing to resolve when this is false. */
    public boolean hasCombat() {
        for (IExercito army : armies) {
            if (!getInimigos(army).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Records that this pair was GUESSED, whichever way the guess went.
     *
     * Tracked separately from the hostile cells because the common guess produces no cell at all: the
     * default for an unresolvable pair is "not hostile", which is indistinguishable from a confidently
     * known peace unless it is marked here. Leaving it implicit would hide exactly the pairs that most
     * need disclosing, which is the failure this whole origin mechanism exists to prevent.
     */
    public void markAssumed(IExercito one, IExercito other) {
        if (one == other) {
            return;
        }
        addArmy(one);
        addArmy(other);
        for (IExercito[] pair : assumed) {
            if ((pair[0] == one && pair[1] == other) || (pair[0] == other && pair[1] == one)) {
                return;
            }
        }
        assumed.add(new IExercito[]{one, other});
    }

    /**
     * Every pair whose relationship was guessed rather than read or derived, each listed once.
     *
     * This is the disclosure list. A result built on a non-empty set here is still worth showing, but
     * it must say so: the player is entitled to know which part of his answer was a guess.
     */
    public List<IExercito[]> getAssumedPairs() {
        return Collections.unmodifiableList(assumed);
    }

    /** Was this particular pair guessed? */
    public boolean isAssumed(IExercito one, IExercito other) {
        for (IExercito[] pair : assumed) {
            if ((pair[0] == one && pair[1] == other) || (pair[0] == other && pair[1] == one)) {
                return true;
            }
        }
        return false;
    }

    /** How many hostile pairs were assumed. Convenience for the status bar. */
    public int getAssumedCount() {
        return getAssumedPairs().size();
    }
}
