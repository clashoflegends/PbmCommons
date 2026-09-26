package business.combat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Pelotao;

/**
 * One layer's rounds table: rows are armies, columns are rounds, cells are troops REMAINING.
 *
 * <h3>Why remaining and not lost</h3>
 *
 * Losses answer "what did this cost"; remaining answers "was it enough", and the second is the
 * question a player opens the simulator to ask. Reading down a column tells him who is still
 * standing after round 3; reading across a row tells him how fast an army is melting. Casualties are
 * the same numbers differenced, and the summary above the table gives him those.
 *
 * <h3>The START column is the point</h3>
 *
 * It is what ENTERED the layer, which for the land layer is what survived the sea layer and came
 * ashore. Once the three-layer chain lands, reading `start` down the page is how the carry-over
 * becomes auditable by eye instead of taken on trust. Today only the land layer resolves, so its
 * start is simply the composition on the hex, and the other two say why they are empty rather than
 * vanishing - "the city was never assaulted" and "the city held" are different answers.
 */
public class LayerReport {

    /** This army took no part in this layer, which is not the same as taking part and losing all. */
    public static final int ABSENT = -1;

    private final CombatLayer layer;
    private final int rounds;
    private final List<ArmySim> armies = new ArrayList<>();
    private final Map<ArmySim, int[]> remaining = new LinkedHashMap<>();
    private String notFoughtReason;

    public LayerReport(CombatLayer layer, int rounds) {
        this.layer = layer;
        this.rounds = rounds;
    }

    /** The land layer, kept for callers that only ever wanted that one. */
    public static LayerReport ofLand(CombatScenario scenario, CombatResult result) {
        return of(scenario, result, CombatLayer.ARMY);
    }

    /**
     * Builds ONE layer's table from a finished result.
     *
     * <h3>Only this layer's losses</h3>
     *
     * Each table is a running total from its own start column, so a loss belonging to another layer
     * poisons it: the city's round-1 casualties were being subtracted from the LAND table's columns
     * 2 onward, showing an army melting from an assault that happened after the land battle ended -
     * and on a pure city assault they vanished entirely, because the land table is only as wide as
     * the land battle's rounds. The layer stamp on {@link CombatResult.RoundLoss} is what makes the
     * filter possible.
     *
     * <h3>START is what ENTERED THIS layer</h3>
     *
     * Not the composition on the hex. An army that lost 200 at sea enters the land layer at 1,100,
     * and reading `start` down the page - 1,300, 1,100, 764 - is how the carry-over becomes
     * auditable by eye instead of taken on trust. It is computed by rewinding: the army's CURRENT
     * strength plus everything it lost in THIS layer and in every later one.
     */
    public static LayerReport of(CombatScenario scenario, CombatResult result, CombatLayer layer) {
        final LayerReport ret = new LayerReport(layer, result.getRounds(layer));
        if (scenario == null) {
            return ret;
        }
        for (ArmySim army : scenario.getArmies()) {
            ret.addArmy(army, startOf(army, result, layer));
        }
        final int base = baseRoundOf(result, layer);
        for (CombatResult.RoundLoss loss : result.getRoundLosses()) {
            if (loss.getLayer() != layer) {
                continue;
            }
            final int[] row = ret.remaining.get(loss.getArmy());
            if (row == null || row[0] == ABSENT) {
                continue;
            }
            // every column from this round onward drops, because the table is a running total and
            // an army that loses 200 in round 1 is 200 lighter for the rest of the battle
            for (int col = loss.getRound() - base + 1; col < row.length; col++) {
                row[col] -= loss.getLost();
            }
        }
        return ret;
    }

    /**
     * The round number this layer's FIRST column stands for.
     *
     * A round number is the resolver's, not the table's, and the two layers do not start counting
     * in the same place: the land battle's first exchange is round 0, but the city assault is round
     * 1 - deliberately, because {@code getForcaPlus} answers zero at round 0 and the siege round
     * occupies it. Subtracting the base is what lets one table shape serve both. Without it the
     * city's only column fell one off the end of a one-round row and every casualty at the walls
     * was silently dropped: a table showing 1,300 into the assault and 1,300 out of it, beside a
     * summary saying the same army had lost 593.
     */
    private static int baseRoundOf(CombatResult result, CombatLayer layer) {
        int ret = Integer.MAX_VALUE;
        for (CombatResult.RoundLoss loss : result.getRoundLosses()) {
            if (loss.getLayer() == layer) {
                ret = Math.min(ret, loss.getRound());
            }
        }
        return ret == Integer.MAX_VALUE ? 0 : ret;
    }

    /**
     * What this army had when it ENTERED the given layer.
     *
     * The plain {@link #startOf(ArmySim, CombatResult)} reads the SCENARIO's own platoons, and the
     * simulation fights on copies - so it is the strength at the START OF THE BATTLE, not the
     * current one. Entering a later layer therefore means subtracting what the EARLIER layers took,
     * which is what makes each table's start equal the previous table's last column without either
     * of them knowing about the other.
     *
     * Layer order is the enum's own - sea, land, city - and it is the order the Judge resolves
     * them in, so {@code ordinal()} is the chain.
     */
    private static int startOf(ArmySim army, CombatResult result, CombatLayer layer) {
        final int ret = startOf(army, result);
        if (ret == ABSENT) {
            return ABSENT;
        }
        int lostEarlier = 0;
        for (CombatResult.RoundLoss loss : result.getRoundLosses()) {
            if (loss.getArmy() == army && loss.getLayer() != null
                    && loss.getLayer().ordinal() < layer.ordinal()) {
                lostEarlier += loss.getLost();
            }
        }
        return Math.max(0, ret - lostEarlier);
    }

    /**
     * What this army brought into the layer, or ABSENT when it brought nothing to this one.
     *
     * A fleet with no land troops is not an army of zero men, and a row of zeroes says the wrong
     * thing about it - it reads as wiped out, or as present and empty, when the truth is that it was
     * never in this layer at all. The casualty summary already draws that distinction with "--"; the
     * rounds table has to draw the same one or the two tables contradict each other on the same
     * screen. Seen at 829 t24, where a vast navy and a second fleet sat in the land table as rows of
     * zeroes beside their own "--" in the summary above.
     */
    private static int startOf(ArmySim army, CombatResult result) {
        int ret = ABSENT;
        for (Pelotao pelotao : army.getPelotoes().values()) {
            if (result.has(pelotao)) {
                ret = Math.max(ret, 0) + pelotao.getQtd();
            }
        }
        return ret;
    }

    private void addArmy(ArmySim army, int start) {
        final int[] row = new int[rounds + 1];
        for (int col = 0; col < row.length; col++) {
            row[col] = start;
        }
        armies.add(army);
        remaining.put(army, row);
    }

    public CombatLayer getLayer() {
        return layer;
    }

    /** How many rounds THIS layer fought. One number per layer, never one for the whole battle. */
    public int getRounds() {
        return rounds;
    }

    public List<ArmySim> getArmies() {
        return armies;
    }

    /**
     * Troops left after the given round; column 0 is the start, before anyone swung.
     *
     * So a two-round battle has three columns - start, after round 0, after round 1 - and the last
     * one is what the platoon table's After column shows.
     */
    public int getRemaining(ArmySim army, int column) {
        final int[] row = remaining.get(army);
        return row == null || column < 0 || column >= row.length ? -1 : row[column];
    }

    /** Set when the layer did not happen, so it can say why instead of rendering an empty grid. */
    public void setNotFoughtReason(String reason) {
        this.notFoughtReason = reason;
    }

    public String getNotFoughtReason() {
        return notFoughtReason;
    }

    public boolean isFought() {
        return notFoughtReason == null && rounds > 0;
    }
}
