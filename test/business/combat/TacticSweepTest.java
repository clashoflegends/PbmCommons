package business.combat;

import model.Pelotao;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every tactic against every tactic, on the whole chain. T-842b.
 *
 * The sweep exists because the enemy's tactic is unknowable - he re-picks it every turn, so a
 * scouted {@code vl_tactic} is history rather than a commitment - and the tests are written around
 * the two claims that follow from that: the grid must cover the scenario's OWN tactic set and
 * nothing else, and the recommendation must be a worst case rather than a prediction.
 */
public class TacticSweepTest extends LandCombatFixture {

    private static final TipoTropa INF = troopType("inf", 50, 40, false);

    private static CombatScenario battle(Pelotao mine, Pelotao theirs) {
        return twoArmiesAtWar(mine, theirs);
    }

    /**
     * A mixed army, because a one-platoon army cannot show a casualty ORDER.
     *
     * Half the effect a tactic has is deciding which of your platoons dies first, and with one
     * platoon there is nothing to order. A grid built on single-platoon armies would look flat for
     * a reason that has nothing to do with the code.
     */
    private static CombatScenario mixedBattle(int mineQty, int theirsQty) {
        final CombatScenario ret = twoArmiesAtWar(platoon(INF, mineQty), platoon(INF, theirsQty));
        for (ArmySim army : ret.getArmies()) {
            final Pelotao siege = platoon(troopType("cat", 95, 15, false), 20);
            final Pelotao light = platoon(troopType("skr", 20, 25, false), 300);
            army.getPelotoes().put(siege.getCodigo(), siege);
            army.getPelotoes().put(light.getCodigo(), light);
        }
        return ret;
    }

    /** Six by six on the traditional set, and every cell resolved. */
    @Test
    public void itResolvesEveryCombination() {
        final TacticSweep.Result grid = TacticSweep.sweep(
                battle(platoon(INF, 900), platoon(INF, 900)), cenario(), 0, 1);

        assertEquals(6, grid.getMyTactics().size());
        assertEquals(6, grid.getHisTactics().size());
        assertEquals(36, grid.getRuns());
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 6; col++) {
                assertEquals(grid.getMyTactics().get(row), grid.get(row, col).getMine());
                assertEquals(grid.getHisTactics().get(col), grid.get(row, col).getHis());
            }
        }
    }

    /**
     * The axes come from the SCENARIO, never from a constant.
     *
     * T-822: the two engine families overlap but neither contains the other, and an index from the
     * wrong one reads a cell of the bonus table nobody filled - which is zero, and multiplies an
     * army's whole troop attack away silently. A sweep would walk into every such cell at once, so
     * this is the guard that matters most in the class.
     */
    @Test
    public void theAxesComeFromTheScenariosOwnTacticSet() {
        final int[] traditional = TacticSweep.validTactics(null);

        assertEquals(6, traditional.length);
        assertEquals(0, traditional[0]);
        assertEquals(5, traditional[5]);
    }

    /** The player's battle is never touched: every cell runs on a copy. */
    @Test
    public void theSweepNeverTouchesThePlayersBattle() {
        final CombatScenario scenario = battle(platoon(INF, 900), platoon(INF, 900));
        scenario.getArmies().get(0).setTatica(3);
        scenario.getArmies().get(1).setTatica(5);

        TacticSweep.sweep(scenario, cenario(), 0, 1);

        assertEquals(3, scenario.getArmies().get(0).getTatica());
        assertEquals(5, scenario.getArmies().get(1).getTatica());
        assertEquals(900, scenario.getArmies().get(0).getPelotoes().values().iterator().next()
                .getQtd(), "and nobody died in the player's copy");
    }

    /**
     * The recommendation is a WORST CASE, not a best case.
     *
     * Pinned by construction rather than by inspection: the chosen tactic's floor across his six
     * must be at least as good as every other tactic's floor. A tactic that wins spectacularly
     * against one of his options and collapses against another must not be recommended, and that is
     * precisely the shape the multiplier table produces - every row of it has exactly one counter
     * at 80.
     */
    @Test
    public void theSafeChoiceMaximisesTheWorstCase() {
        final TacticSweep.Result grid = TacticSweep.sweep(
                battle(platoon(INF, 1200), platoon(INF, 900)), cenario(), 0, 1);

        final int chosen = grid.getSafeChoice();
        assertTrue(chosen >= 0);
        final int chosenFloor = floorOf(grid, chosen);
        for (Integer mine : grid.getMyTactics()) {
            assertTrue(chosenFloor >= floorOf(grid, mine),
                    "tactic " + mine + " has a better floor than the recommended " + chosen);
        }
    }

    /** The lowest number of my men left, across everything he could do. */
    private static int floorOf(TacticSweep.Result grid, int myTactic) {
        final int row = grid.getMyTactics().indexOf(myTactic);
        int ret = Integer.MAX_VALUE;
        for (int col = 0; col < grid.getHisTactics().size(); col++) {
            ret = Math.min(ret, grid.get(row, col).getMySurvivors());
        }
        return ret;
    }

    /** Coverage counts the columns I beat, which is the other way a player reads the grid. */
    @Test
    public void coverageCountsTheColumnsIBeat() {
        final TacticSweep.Result grid = TacticSweep.sweep(
                battle(platoon(INF, 9000), platoon(INF, 10)), cenario(), 0, 1);

        for (Integer mine : grid.getMyTactics()) {
            assertEquals(6, grid.coverageOf(mine),
                    "against ten men, every tactic of mine wins every column");
        }
    }

    /**
     * When nothing he does achieves his goal, the sharp answer falls back to the safe one.
     *
     * An empty viable set is not "he is pinned to nothing", it is "this battle is already decided",
     * and a recommendation derived from an empty column set would be derived from nothing at all.
     */
    @Test
    public void anEnemyWhoCannotWinAnywayLeavesTheSharpAnswerEqualToTheSafeOne() {
        final TacticSweep.Result grid = TacticSweep.sweep(
                battle(platoon(INF, 9000), platoon(INF, 10)), cenario(), 0, 1);

        assertTrue(grid.getHisViable().isEmpty(), "ten men hold nothing against nine thousand");
        assertEquals(grid.getSafeChoice(), grid.getSharpChoice());
    }

    /**
     * A free enemy makes the two answers the same; the sharp one only bites once he is pinned.
     *
     * This is the honest half of John's pin-down idea: the sharp answer is worth something exactly
     * when his objective narrows his options, and worth nothing when it does not.
     */
    @Test
    public void afreeEnemyMakesBothAnswersAgree() {
        final TacticSweep.Result grid = TacticSweep.sweep(
                battle(platoon(INF, 10), platoon(INF, 9000)), cenario(), 0, 1);

        assertEquals(6, grid.getHisViable().size(), "he wins whatever he picks");
        assertEquals(grid.getSafeChoice(), grid.getSharpChoice());
    }

    /**
     * Tactics actually MOVE the result, or none of this is worth showing.
     *
     * The weakest useful assertion in the class and the one that would catch the worst bug: a sweep
     * whose 36 cells were all identical would rank them, recommend one, and mean nothing.
     */
    @Test
    public void theGridIsNotFlat() {
        final TacticSweep.Result grid = TacticSweep.sweep(mixedBattle(1000, 1000), cenario(), 0, 1);

        final int first = grid.get(0, 0).getMySurvivors();
        boolean varies = false;
        for (int row = 0; row < grid.getMyTactics().size() && !varies; row++) {
            for (int col = 0; col < grid.getHisTactics().size(); col++) {
                if (grid.get(row, col).getMySurvivors() != first) {
                    varies = true;
                    break;
                }
            }
        }
        assertTrue(varies, "the tactic pair must change the outcome");
    }

    /** Charge and Guerrilla are a mirror pair on attack, so they must not agree everywhere. */
    @Test
    public void chargeAndGuerrillaDoNotProduceTheSameBattle() {
        final TacticSweep.Result grid = TacticSweep.sweep(mixedBattle(1000, 1000), cenario(), 0, 1);
        final int charge = grid.getMyTactics().indexOf(0);
        final int guerrilla = grid.getMyTactics().indexOf(4);

        boolean differ = false;
        for (int col = 0; col < grid.getHisTactics().size(); col++) {
            if (grid.get(charge, col).getMySurvivors()
                    != grid.get(guerrilla, col).getMySurvivors()) {
                differ = true;
                break;
            }
        }
        assertTrue(differ, "opposite ends of the same casualty key cannot be identical");
    }

    /**
     * With NO CITY on the hex, nobody is trying to take one, whatever their combat level says.
     *
     * This is the regression for the defect a probe found while writing these: CombatScenario
     * defaults every army's level to ATTACK_CITY because intent does not ride the EGF, so before
     * the guard both sides read as failing an objective that did not exist, the viable set came out
     * empty for everyone, and the sharp recommendation rested on nothing. Silently, and on the
     * commonest kind of battle there is.
     */
    @Test
    public void withNoCityEverybodyIsJustTryingToHoldTheField() {
        final CombatScenario scenario = battle(platoon(INF, 9000), platoon(INF, 10));
        assertEquals(CombatLevel.ATTACK_CITY, scenario.getArmies().get(0).getCombatLevel(),
                "the default level really is ATTACK_CITY, which is what made this a trap");
        assertFalse(scenario.isCityParticipates(), "and there is no city to take");

        final TacticSweep.Result grid = TacticSweep.sweep(scenario, cenario(), 0, 1);

        for (int col = 0; col < grid.getHisTactics().size(); col++) {
            assertTrue(grid.get(0, col).isMine(),
                    "nine thousand against ten holds the field in every column");
        }
    }

    /** Bad inputs answer an empty grid rather than throwing. */
    @Test
    public void badInputsAreSurvivable() {
        final CombatScenario scenario = battle(platoon(INF, 900), platoon(INF, 900));

        assertEquals(0, TacticSweep.sweep(null, cenario(), 0, 1).getRuns());
        assertEquals(0, TacticSweep.sweep(scenario, cenario(), 0, 0).getRuns(), "same army twice");
        assertEquals(0, TacticSweep.sweep(scenario, cenario(), -1, 1).getRuns());
        assertEquals(0, TacticSweep.sweep(scenario, cenario(), 0, 9).getRuns());
        assertNotEquals(0, TacticSweep.sweep(scenario, cenario(), 0, 1).getRuns());
        assertFalse(TacticSweep.sweep(null, cenario(), 0, 1).getHisViable().iterator().hasNext());
    }
}
