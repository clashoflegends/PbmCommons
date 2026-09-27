package business.combat;

import model.Cenario;
import model.Pelotao;

/**
 * The question inverted: not "what happens", but "what would it take". T-842.
 *
 * John: <i>"what do I need to win this combat or to capture this city?"</i>. The simulator already
 * answers forwards, and the resolver is deterministic and fast - a battle is a handful of rounds
 * over a handful of platoons - so answering backwards needs no new combat maths at all. It needs a
 * SEARCH: vary one number the player nominates, run the whole chain, and report the smallest value
 * that wins. "1,850 Heavy Infantry takes Riverrun; 1,800 does not."
 *
 * <h3>Binary search, with the assumption it rests on actually checked</h3>
 *
 * A binary search is only correct if the predicate is monotonic - if 1,850 wins then 1,900 must
 * too. That is very probably true of troop quantity and it is NOT guaranteed: casualties are
 * distributed by rank and by weight, so adding men changes which platoons absorb damage and in
 * principle a larger army can lose a battle a smaller one wins. Rather than assume it, the answer
 * is VERIFIED at the boundary - the value below the threshold must actually lose - and when that
 * check fails the answer says so instead of quietly reporting a number that is not a threshold.
 * An unverified binary search over a non-monotonic predicate returns a plausible wrong answer every
 * time, which is the exact failure this rebuild keeps being built to avoid.
 *
 * <h3>What it refuses to answer</h3>
 *
 * Anything with a die in it cannot have a threshold, only a likelihood. Since T-817 removed the
 * last roll from the modelled chain there is none left inside it, but two things outside it still
 * move a real turn - character deaths and city loyalty - and neither is modelled, so a threshold
 * here is a threshold for the battle, not a guarantee for the turn.
 *
 * <b>The bigger caveat is the enemy.</b> If any army in the battle is made of unidentified troops
 * - the placeholder type, which fights at 1 - then every figure here is computed against an enemy
 * weaker than the real one, so the threshold is a LOWER BOUND: the true answer is that number or
 * more, never less. {@link Answer#isLowerBound} says so, and the caller must pass it on. A "you
 * need 1,850" that is really "you need at least 1,850" is the difference between a tool and a trap.
 *
 * <h3>Nothing the player owns is touched</h3>
 *
 * Every trial runs on {@link CombatScenario#copy()}, so the window's own scenario, its armies and
 * its platoons are never edited and no result is left behind on them.
 */
public final class WhatIfSearch {

    /** Runs are cheap but not free, and a runaway ceiling should stop rather than hang. */
    private static final int MAX_TRIALS = 64;

    /** What the player is trying to achieve. */
    public enum Goal {
        /** The nominated army is left standing with the field to itself. */
        HOLD_THE_FIELD,
        /** The city falls, captured or razed. Whether the army survives is a separate question. */
        TAKE_THE_CITY
    }

    private WhatIfSearch() {
    }

    /** What the search found, including the reasons it might be worth less than it looks. */
    public static final class Answer {

        private boolean found;
        private int threshold;
        private int ceiling;
        private int trials;
        private boolean lowerBound;
        private boolean verified;
        private boolean winsAtNothing;

        /** True when some value at or below the ceiling achieves the goal. */
        public boolean isFound() {
            return found;
        }

        /** The smallest quantity that wins. Meaningless unless {@link #isFound}. */
        public int getThreshold() {
            return threshold;
        }

        /** The largest quantity tried, so "no answer" can say how far it looked. */
        public int getCeiling() {
            return ceiling;
        }

        public int getTrials() {
            return trials;
        }

        /**
         * The battle contains troops nobody has identified, so the real answer is this or MORE.
         *
         * See the class note. This is the single most important field on the object.
         */
        public boolean isLowerBound() {
            return lowerBound;
        }

        /**
         * The boundary check passed: one fewer loses, this many wins. A genuine threshold.
         *
         * False means the predicate is not monotonic in this battle and the number is only "a value
         * that wins", not "the smallest". Report it as such rather than dressing it up.
         */
        public boolean isVerified() {
            return verified;
        }

        /** The goal is already met with this platoon emptied: nothing more is needed. */
        public boolean isWinsAtNothing() {
            return winsAtNothing;
        }
    }

    /**
     * How many of one platoon it takes.
     *
     * @param scenario the player's battle. Never modified.
     * @param cenario  the scenario catalogue the chain resolves against.
     * @param armyIndex which army owns the platoon, by position in {@code getArmies()} - copies
     *                  preserve that order, which is what makes the trial army findable.
     * @param platoonCodigo the platoon's codigo, which a copy preserves.
     * @param goal     what counts as winning.
     * @param ceiling  the largest quantity worth trying.
     */
    public static Answer forPlatoonQuantity(CombatScenario scenario, Cenario cenario,
            int armyIndex, String platoonCodigo, Goal goal, int ceiling) {
        final Answer ret = new Answer();
        ret.ceiling = Math.max(0, ceiling);
        if (scenario == null || platoonCodigo == null || armyIndex < 0
                || armyIndex >= scenario.getArmies().size() || ret.ceiling <= 0) {
            return ret;
        }
        ret.lowerBound = hasUnidentifiedTroops(scenario);

        // Asked FIRST, because a player whose army already wins wants to hear that and not a
        // number. It is also the search's lower rail: without it, a battle won at zero reports a
        // threshold of 1, which is true and useless.
        if (wins(scenario, cenario, armyIndex, platoonCodigo, 0, goal, ret)) {
            ret.found = true;
            ret.winsAtNothing = true;
            ret.threshold = 0;
            ret.verified = true;
            return ret;
        }
        if (!wins(scenario, cenario, armyIndex, platoonCodigo, ret.ceiling, goal, ret)) {
            // Not reachable within the ceiling. Say how far it looked rather than "impossible":
            // the player can raise it, and the two statements are not the same claim.
            return ret;
        }
        int low = 1;
        int high = ret.ceiling;
        while (low < high && ret.trials < MAX_TRIALS) {
            final int mid = low + (high - low) / 2;
            if (wins(scenario, cenario, armyIndex, platoonCodigo, mid, goal, ret)) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        ret.found = true;
        ret.threshold = low;
        // The assumption, checked. One fewer must lose, or this is not a threshold - see the class
        // note on why an unverified binary search is worse than no search.
        ret.verified = low <= 1
                || !wins(scenario, cenario, armyIndex, platoonCodigo, low - 1, goal, ret);
        return ret;
    }

    /** One trial: a fresh copy, one number changed, the whole chain run. */
    private static boolean wins(CombatScenario scenario, Cenario cenario, int armyIndex,
            String platoonCodigo, int quantity, Goal goal, Answer ret) {
        ret.trials++;
        final CombatScenario trial = scenario.copy();
        final ArmySim army = trial.getArmies().get(armyIndex);
        final Pelotao platoon = army.getPelotoes().get(platoonCodigo);
        if (platoon == null) {
            return false;
        }
        platoon.setQtd(quantity);
        final CombatResult result = new CombatChain().resolve(trial, cenario);
        return achieved(result, army, goal);
    }

    private static boolean achieved(CombatResult result, ArmySim army, Goal goal) {
        if (result == null) {
            return false;
        }
        if (goal == Goal.TAKE_THE_CITY) {
            final CityCombatResolver.CityResult city = result.getCityResult();
            return city != null
                    && (city.getOutcome() == CityCombatResolver.CityOutcome.CAPTURED
                    || city.getOutcome() == CityCombatResolver.CityOutcome.RAZED);
        }
        return result.getOutcome(army, CombatLayer.ARMY) == CombatResult.Outcome.WON;
    }

    /**
     * Is any army in this battle made of troops nobody has identified?
     *
     * Asked of the WHOLE battle rather than of the enemy, because an unidentified ally distorts the
     * answer in the opposite direction - it makes the player's own side look weaker and the
     * threshold too high - and both directions are worth a warning.
     */
    public static boolean hasUnidentifiedTroops(CombatScenario scenario) {
        if (scenario == null) {
            return false;
        }
        for (ArmySim army : scenario.getArmies()) {
            if (ScenarioDefaults.unknownTroops(army) > 0) {
                return true;
            }
        }
        return false;
    }
}
