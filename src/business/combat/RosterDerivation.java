package business.combat;

import java.util.List;
import model.Partida;

/**
 * The facts behind the status bar: how this scenario decided who fights whom, and how much of that
 * was guessed.
 *
 * <h3>Facts, not a sentence</h3>
 *
 * The wireframe's status line is three different sentences depending on the game type, and every
 * word of it has to come from {@code labels.properties} - there are no content strings in this
 * layer. So this class answers the questions the sentence is built from and the Counselor does the
 * wording. That also keeps it testable: the interesting thing to pin is "does it know the game type
 * and the assumption count", not how a JLabel reads.
 *
 * <h3>Why the assumption count is on the status bar at all</h3>
 *
 * R-15. A guessed peace looks exactly like a known one once it reaches a number, so the one place
 * the player can see the difference is here. The count is of PAIRS, not armies, because that is what
 * was actually assumed: one unresolved pair can involve two armies the player never thinks about.
 */
public class RosterDerivation {

    /** Which sentence the status bar should use. */
    public enum Basis {
        /** Every pair came from the game type. A Death Match needs no relationship row at all. */
        GAME_TYPE,
        /** Everything was read from a loaded EGF, and nothing was left over. */
        ALL_READ,
        /** Some pairs could not be resolved and were assumed not hostile. */
        PARTLY_ASSUMED
    }

    private final Basis basis;
    private final int assumedPairs;
    private final int readPairs;
    private final int fromGameType;
    private final boolean diplomacyEditable;

    private RosterDerivation(Basis basis, int assumedPairs, int readPairs, int fromGameType,
            boolean diplomacyEditable) {
        this.basis = basis;
        this.assumedPairs = assumedPairs;
        this.readPairs = readPairs;
        this.fromGameType = fromGameType;
        this.diplomacyEditable = diplomacyEditable;
    }

    public static RosterDerivation of(CombatScenario scenario) {
        if (scenario == null) {
            return new RosterDerivation(Basis.ALL_READ, 0, 0, 0, false);
        }
        final HostilityMatrix matrix = scenario.getMatrix();
        int read = 0;
        int type = 0;
        final List<ArmySim> armies = scenario.getArmies();
        for (int ii = 0; ii < armies.size(); ii++) {
            for (int jj = ii + 1; jj < armies.size(); jj++) {
                final HostilityMatrix.Origin origin = matrix.getOrigin(armies.get(ii), armies.get(jj));
                if (origin == HostilityMatrix.Origin.READ_FROM_EGF) {
                    read++;
                } else if (origin == HostilityMatrix.Origin.FROM_GAME_TYPE) {
                    type++;
                }
            }
        }
        final int assumed = matrix.getAssumedCount();
        final Basis basis;
        if (assumed > 0) {
            basis = Basis.PARTLY_ASSUMED;
        } else if (read == 0 && type > 0) {
            basis = Basis.GAME_TYPE;
        } else {
            basis = Basis.ALL_READ;
        }
        return new RosterDerivation(basis, assumed, read, type,
                isDiplomacyEditable(scenario.getPartida()));
    }

    public Basis getBasis() {
        return basis;
    }

    /** Pairs nothing could resolve, defaulted to not hostile and marked. R-15. */
    public int getAssumedPairs() {
        return assumedPairs;
    }

    /** Pairs answered by a relationship row this scenario could prove complete. */
    public int getReadPairs() {
        return readPairs;
    }

    /** Pairs the game type settled on its own, needing no row. */
    public int getPairsFromGameType() {
        return fromGameType;
    }

    /**
     * Should the status bar offer the [review] affordance, and the toolbar enable [Diplomacy..]?
     *
     * Only where diplomacy is genuinely floating. In a Death Match or a locked-team game the matrix
     * is fully determined by the rules, so an editable one would let the player build a scenario the
     * Judge cannot produce. The editor itself is post-MVP (R-32); this is the gate it will hang on.
     */
    public boolean isDiplomacyEditable() {
        return diplomacyEditable;
    }

    private static boolean isDiplomacyEditable(Partida partida) {
        return partida != null && (partida.isFreeForAll() || partida.isBattleRoyal());
    }
}
