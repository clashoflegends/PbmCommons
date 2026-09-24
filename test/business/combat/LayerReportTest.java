package business.combat;

import model.Pelotao;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rounds table reads as a running total, and its last column IS the platoon table's After.
 *
 * Pinned because the table is built by subtracting losses forward across the row, which is the kind
 * of loop that is off by one in either direction without anyone noticing: a column that never drops
 * looks like a stalemate, and one that drops too early makes an army die a round before it did.
 */
public class LayerReportTest extends LandCombatFixture {

    @Test
    public void theTableRunsFromTheStartingStrengthDownToTheSurvivors() {
        final Pelotao strong = platoon(troopType("strong", 90, 40, false), 1000);
        final Pelotao weak = platoon(troopType("weak", 10, 10, false), 400);
        final CombatScenario scenario = twoArmiesAtWar(strong, weak);
        final CombatResult result = new LandCombatResolver().resolve(scenario, cenario());

        final LayerReport report = LayerReport.ofLand(scenario, result);

        assertEquals(result.getRounds(), report.getRounds(), "the layer carries its OWN round count");
        final ArmySim loser = scenario.getArmies().get(1);
        assertEquals(400, report.getRemaining(loser, 0), "column 0 is the strength it started with");
        assertEquals(result.getAfter(weak), report.getRemaining(loser, result.getRounds()),
                "and the last column is exactly what the platoon table shows as After");
    }

    /** An army that never fought still gets a row, flat across, rather than being dropped. */
    @Test
    public void anarmyThatDidNotFightIsStillInTheTable() {
        final Pelotao strong = platoon(troopType("strong", 90, 40, false), 1000);
        final Pelotao weak = platoon(troopType("weak", 10, 10, false), 400);
        final CombatScenario scenario = twoArmiesAtWar(strong, weak);
        final Pelotao watching = platoon(troopType("watch", 10, 10, false), 300);
        final ArmySim watcher = army("watcher", nacao("n"), watching);
        scenario.addArmy(watcher, CombatScenario.Provenance.EXACT);
        final CombatResult result = new LandCombatResolver().resolve(scenario, cenario());

        final LayerReport report = LayerReport.ofLand(scenario, result);

        assertTrue(report.getArmies().contains(watcher), "it is on the hex, so it is in the table");
        assertEquals(report.getRemaining(watcher, 0),
                report.getRemaining(watcher, report.getRounds()), "and its row never moves");
    }

    /**
     * A FLEET IS ABSENT FROM THE LAND TABLE, not present with nobody in it.
     *
     * A row of zeroes reads as an army that was wiped out, or one that turned up empty. The truth is
     * that it was never in this layer. The casualty summary already says "--" for it, and the two
     * tables sit on the same screen, so the rounds table has to say the same thing or they
     * contradict each other - which is exactly what 829 t24 showed, a vast navy carrying a row of
     * zeroes beside its own "--" one table up.
     */
    @Test
    public void afleetIsAbsentFromTheLandTableRatherThanZero() {
        final Pelotao strong = platoon(troopType("strong", 90, 40, false), 1000);
        final Pelotao weak = platoon(troopType("weak", 10, 10, false), 400);
        final CombatScenario scenario = twoArmiesAtWar(strong, weak);
        final ArmySim fleet = army("fleet", nacao("n"), platoon(shipType("kraken"), 350));
        scenario.addArmy(fleet, CombatScenario.Provenance.EXACT);
        final CombatResult result = new LandCombatResolver().resolve(scenario, cenario());

        final LayerReport report = LayerReport.ofLand(scenario, result);

        assertEquals(LayerReport.ABSENT, report.getRemaining(fleet, 0),
                "it brought nothing to the land layer, which is not the same as bringing zero");
        assertEquals(LayerReport.ABSENT, report.getRemaining(fleet, report.getRounds()),
                "and it stays absent rather than drifting to a number");
    }

    /** A layer that did not happen says why rather than rendering an empty grid. */
    @Test
    public void alayerThatDidNotHappenCarriesItsReason() {
        final LayerReport report = new LayerReport(CombatLayer.NAVY, 0);
        assertFalse(report.isFought());
        report.setNotFoughtReason("BATTLESIM.LAYER.NOTSIMULATED");
        assertEquals("BATTLESIM.LAYER.NOTSIMULATED", report.getNotFoughtReason());
    }
}
