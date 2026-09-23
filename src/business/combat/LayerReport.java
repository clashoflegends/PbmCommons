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

    private final CombatLayer layer;
    private final int rounds;
    private final List<ArmySim> armies = new ArrayList<>();
    private final Map<ArmySim, int[]> remaining = new LinkedHashMap<>();
    private String notFoughtReason;

    public LayerReport(CombatLayer layer, int rounds) {
        this.layer = layer;
        this.rounds = rounds;
    }

    /**
     * Builds the land layer's table from a finished result.
     *
     * Every army in the scenario gets a row, including the ones that never fought: an army standing
     * on the hex is part of what the player is looking at, and dropping it would leave him counting
     * rows to work out who is missing.
     */
    public static LayerReport ofLand(CombatScenario scenario, CombatResult result) {
        final LayerReport ret = new LayerReport(CombatLayer.ARMY, result.getRounds());
        if (scenario == null) {
            return ret;
        }
        for (ArmySim army : scenario.getArmies()) {
            ret.addArmy(army, startOf(army, result));
        }
        for (CombatResult.RoundLoss loss : result.getRoundLosses()) {
            final int[] row = ret.remaining.get(loss.getArmy());
            if (row == null) {
                continue;
            }
            // every column from this round onward drops, because the table is a running total and
            // an army that loses 200 in round 1 is 200 lighter for the rest of the battle
            for (int col = loss.getRound() + 1; col < row.length; col++) {
                row[col] -= loss.getLost();
            }
        }
        return ret;
    }

    /** What this army brought into the layer: the land troops it had when the fighting started. */
    private static int startOf(ArmySim army, CombatResult result) {
        int ret = 0;
        for (Pelotao pelotao : army.getPelotoes().values()) {
            if (result.has(pelotao)) {
                ret += pelotao.getQtd();
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
