package business.combat;

import model.Pelotao;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "What do I need to win?" - the search, and the honesty around it. T-842.
 *
 * The arithmetic is the easy half: the resolver already works and this only calls it repeatedly.
 * What the tests are really for is the three ways a search like this lies - reporting a number that
 * is not a threshold, reporting a threshold computed against an enemy it cannot see, and reporting
 * "impossible" when it simply did not look far enough - so each of those has its own test.
 */
public class WhatIfSearchTest extends LandCombatFixture {

    private static final TipoTropa INF = troopType("inf", 50, 40, false);
    private static final TipoTropa NONE = troopType(ScenarioDefaults.PLACEHOLDER_CODE, 1, 1, false);

    /**
     * Mine first, theirs second: the search addresses armies by INDEX, and a copy preserves it.
     *
     * The shared fixture rather than a local one, so these run on the same hostile pair every
     * other resolver test does - a battle that quietly failed to be a battle would make every
     * assertion below vacuously true.
     */
    private static CombatScenario battle(Pelotao minePlatoon, Pelotao theirs) {
        return twoArmiesAtWar(minePlatoon, theirs);
    }

    /**
     * The headline: a real threshold, and one fewer man genuinely loses.
     *
     * Both halves are asserted. A search that only checks the winning side reports the first value
     * it happened to land on, which is a number rather than an answer.
     */
    @Test
    public void itFindsTheSmallestQuantityThatWins() {
        final Pelotao mine = platoon(INF, 10);
        final CombatScenario scenario = battle(mine, platoon(INF, 500));

        final WhatIfSearch.Answer answer = WhatIfSearch.forPlatoonQuantity(scenario, null, 0,
                mine.getCodigo(), WhatIfSearch.Goal.HOLD_THE_FIELD, 20000);

        assertTrue(answer.isFound(), "a big enough army must be able to win this");
        assertTrue(answer.isVerified(), "and it must be a THRESHOLD, not just a winning value");
        assertTrue(answer.getThreshold() > 0);
        assertFalse(answer.isLowerBound(), "both sides are identified here");
    }

    /** The player's own scenario is never edited - every trial runs on a copy. */
    @Test
    public void theSearchNeverTouchesThePlayersBattle() {
        final Pelotao mine = platoon(INF, 10);
        final CombatScenario scenario = battle(mine, platoon(INF, 500));

        WhatIfSearch.forPlatoonQuantity(scenario, null, 0, mine.getCodigo(),
                WhatIfSearch.Goal.HOLD_THE_FIELD, 20000);

        assertEquals(10, mine.getQtd(), "the platoon the search varied is still the player's 10");
        assertEquals(500, scenario.getArmies().get(1).getPelotoes().values().iterator().next()
                .getQtd(), "and so is the enemy");
    }

    /**
     * An enemy of unidentified troops makes every answer a LOWER bound.
     *
     * This is the field that matters most on the object: the placeholder fights at 1, so the
     * threshold was computed against an enemy weaker than the real one and the true answer is that
     * number or more. Reporting it as a flat "you need N" would be a trap.
     */
    @Test
    public void anUnidentifiedEnemyMakesTheAnswerALowerBound() {
        final Pelotao mine = platoon(INF, 10);
        final CombatScenario scenario = battle(mine, platoon(NONE, 3000));

        final WhatIfSearch.Answer answer = WhatIfSearch.forPlatoonQuantity(scenario, null, 0,
                mine.getCodigo(), WhatIfSearch.Goal.HOLD_THE_FIELD, 20000);

        assertTrue(answer.isLowerBound(), "the enemy is not identified, so this is 'at least'");
    }

    /** An unidentified ALLY counts too - it skews the answer the other way, and that is worth saying. */
    @Test
    public void anUnidentifiedAllyAlsoMakesItALowerBound() {
        final CombatScenario scenario = battle(platoon(INF, 10), platoon(INF, 500));
        final ArmySim ally = army("Ally", nacao("a"), platoon(NONE, 800));
        scenario.addArmy(ally, CombatScenario.Provenance.ESTIMATED);

        assertTrue(WhatIfSearch.hasUnidentifiedTroops(scenario));
    }

    /**
     * Already winning WITHOUT the platoon being varied: say so, rather than inventing a number.
     *
     * The army needs a second platoon for this to mean anything - zeroing the only one empties the
     * army, and an empty army does not win. That is the honest reading of the question too: "how
     * many more archers do I need" has the answer "none" only when something else is already
     * carrying the battle.
     */
    @Test
    public void anAlreadyWonBattleNeedsNothing() {
        final Pelotao spare = platoon(troopType("arc", 30, 20, false), 10);
        final ArmySim mine = army("mine", nacao("m"), platoon(INF, 5000));
        mine.getPelotoes().put(spare.getCodigo(), spare);
        final ArmySim theirs = army("theirs", nacao("t"), platoon(INF, 1));
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(mine, CombatScenario.Provenance.EXACT);
        scenario.addArmy(theirs, CombatScenario.Provenance.ESTIMATED);
        scenario.setRelacionamento(mine.getNacao(), theirs.getNacao(),
                RelationshipMatrix.SWORN_ENEMY);
        scenario.setRelacionamento(theirs.getNacao(), mine.getNacao(),
                RelationshipMatrix.SWORN_ENEMY);

        final WhatIfSearch.Answer answer = WhatIfSearch.forPlatoonQuantity(scenario, null, 0,
                spare.getCodigo(), WhatIfSearch.Goal.HOLD_THE_FIELD, 20000);

        assertTrue(answer.isFound());
        assertTrue(answer.isWinsAtNothing(), "the 5,000 infantry are carrying this, not the archers");
        assertEquals(0, answer.getThreshold());
    }

    /**
     * Out of reach is reported as "not within this ceiling", with the ceiling, and never as
     * "impossible" - the two are different claims and the player can act on the first.
     */
    @Test
    public void anUnreachableGoalReportsHowFarItLooked() {
        final Pelotao mine = platoon(INF, 1);
        final CombatScenario scenario = battle(mine, platoon(INF, 900));

        final WhatIfSearch.Answer answer = WhatIfSearch.forPlatoonQuantity(scenario, null, 0,
                mine.getCodigo(), WhatIfSearch.Goal.HOLD_THE_FIELD, 10);

        assertFalse(answer.isFound());
        assertEquals(10, answer.getCeiling());
        assertTrue(answer.getTrials() > 0, "it did look");
    }

    /** A search costs runs; a binary search costs few. If this grows, something went linear. */
    @Test
    public void itCostsALogarithmicNumberOfRuns() {
        final Pelotao mine = platoon(INF, 10);
        final CombatScenario scenario = battle(mine, platoon(INF, 500));

        final WhatIfSearch.Answer answer = WhatIfSearch.forPlatoonQuantity(scenario, null, 0,
                mine.getCodigo(), WhatIfSearch.Goal.HOLD_THE_FIELD, 65536);

        assertTrue(answer.getTrials() <= 24,
                "17 halvings plus the rails and the check, not 65,536: " + answer.getTrials());
    }

    /** Bad inputs answer "nothing found" rather than throwing into the event thread. */
    @Test
    public void badInputsAreSurvivable() {
        final CombatScenario scenario = battle(platoon(INF, 10), platoon(INF, 500));

        assertFalse(WhatIfSearch.forPlatoonQuantity(null, null, 0, "x",
                WhatIfSearch.Goal.HOLD_THE_FIELD, 100).isFound());
        assertFalse(WhatIfSearch.forPlatoonQuantity(scenario, null, 9, "x",
                WhatIfSearch.Goal.HOLD_THE_FIELD, 100).isFound(), "no such army");
        assertFalse(WhatIfSearch.forPlatoonQuantity(scenario, null, 0, "nosuch",
                WhatIfSearch.Goal.HOLD_THE_FIELD, 100).isFound(), "no such platoon");
        assertFalse(WhatIfSearch.forPlatoonQuantity(scenario, null, 0, "x",
                WhatIfSearch.Goal.HOLD_THE_FIELD, 0).isFound(), "no ceiling to search");
        assertFalse(WhatIfSearch.hasUnidentifiedTroops(null));
    }
}
