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
        private boolean ceilingStalemate;

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

        /**
         * At the ceiling the battle did not resolve at all: it ran to the round cap, undecided.
         *
         * This is a DIFFERENT answer from "you lose", and the difference is the useful part. The
         * model rewards quality over numbers - two evenly matched sides grind, and piling on men
         * lengthens the battle rather than winning it - so "no quantity wins" often means "numbers
         * are not the lever here", not "this is hopeless". A player told that will go and change
         * his tactic, his training or his commander instead of recruiting into a stalemate.
         */
        public boolean isCeilingStalemate() {
            return ceilingStalemate;
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
            // the player can raise it, and the two statements are not the same claim. And record
            // WHICH kind of failure it was - see isCeilingStalemate.
            ret.ceilingStalemate = goal == Goal.HOLD_THE_FIELD && isStalemate(scenario, cenario,
                    armyIndex, platoonCodigo, ret.ceiling);
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

    /** Did the biggest army tried merely fail to finish, rather than lose? */
    private static boolean isStalemate(CombatScenario scenario, Cenario cenario, int armyIndex,
            String platoonCodigo, int quantity) {
        final CombatScenario trial = scenario.copy();
        final ArmySim army = trial.getArmies().get(armyIndex);
        final Pelotao platoon = army.getPelotoes().get(platoonCodigo);
        if (platoon == null) {
            return false;
        }
        platoon.setQtd(quantity);
        final CombatResult result = new CombatChain().resolve(trial, cenario);
        return result != null
                && result.getOutcome(army, CombatLayer.ARMY) == CombatResult.Outcome.UNDECIDED;
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
     * What the player should read the FIRST answer against: assume the unknown troops are the worst
     * they could be.
     *
     * John, 2026-09-27: <i>"a what-if when there is an unknown troops on the other side can be
     * 'what if I am facing the worst case scenario, aka the strongest troops for that nation on
     * that terrain'"</i>. It is the other end of the bracket. Left alone, an unidentified enemy
     * fights at 1 and every forecast is a floor; retyped to the best that nation could field here,
     * it becomes a ceiling. The truth is between them, and two runs bound it.
     *
     * <h3>Why this is allowed to guess a composition when {@code ScenarioDefaults} is not</h3>
     *
     * Because the two are doing opposite things, and a future reader will otherwise see a
     * contradiction. {@code ScenarioDefaults} fills in what the player is PRESUMED to know, so a
     * guessed composition there would be presented as intelligence - and John's reason for
     * refusing it stands: troop strength is per terrain, so guessing cavalry on ground that
     * punishes cavalry is not slightly wrong, it is differently wrong, and nobody can tell which.
     * This is a NAMED HYPOTHESIS the player asked for by pressing a button labelled "worst case".
     * Its whole value is that it is not what he knows.
     *
     * <h3>"Strongest" is measured, not judged</h3>
     *
     * Every troop type the army's race can field is tried in the platoon's place and scored with
     * the same {@code getPlatoonAttack} the battle itself uses - so terrain, nation bonuses and the
     * platoon's own training all count, and no separate notion of "best" can drift from the one
     * that decides the fight. Attack ranks, defence breaks ties: what makes an enemy worst is that
     * he kills more of you, and surviving longer to keep doing it is the tiebreak.
     *
     * Ships stay ships. A {@code ship} placeholder is retyped only among naval types and a
     * {@code none} placeholder only among land ones, or a fleet's hulls would come back as
     * infantry and the sea layer would vanish.
     *
     * <b>Mutates the scenario it is given</b>, like {@code ScenarioDefaults.fill}. Callers hand it
     * a {@link CombatScenario#copy()} - the point is a second window beside the first, not a change
     * to the battle the player set up.
     *
     * @return how many platoons were retyped, and the types chosen, for the sentence that explains
     *         what he is now looking at.
     */
    public static WorstCase toWorstCase(CombatScenario scenario, Cenario cenario) {
        final WorstCase ret = new WorstCase();
        if (scenario == null) {
            return ret;
        }
        final business.facade.ExercitoFacade facade = new business.facade.ExercitoFacade();
        for (ArmySim army : scenario.getArmies()) {
            for (Pelotao pelotao : army.getPelotoes().values()) {
                if (!ScenarioDefaults.isPlaceholder(pelotao)) {
                    continue;
                }
                final model.TipoTropa best = strongestFor(army, pelotao, cenario, facade);
                if (best == null) {
                    continue;
                }
                pelotao.setTipoTropa(best);
                scenario.setProvenance(pelotao, CombatScenario.Provenance.ESTIMATED);
                ret.platoons++;
                ret.troops += pelotao.getQtd();
                if (!ret.types.contains(best.getNome())) {
                    ret.types.add(best.getNome());
                }
            }
        }
        return ret;
    }

    /** What a worst-case swap did, so the window can say what it is showing. */
    public static final class WorstCase {

        private int platoons;
        private int troops;
        private final java.util.List<String> types = new java.util.ArrayList<>();

        public int getPlatoons() {
            return platoons;
        }

        public int getTroops() {
            return troops;
        }

        /** The types chosen, in the order found, for naming them in the explanation. */
        public java.util.List<String> getTypes() {
            return java.util.Collections.unmodifiableList(types);
        }
    }

    /**
     * The hardest-hitting type this army's race could have put in that platoon, on this ground.
     *
     * Scored through the real formula rather than off the catalogue's raw numbers, so a type that
     * is strong in the abstract but poor on THIS terrain does not win. The race's own list is the
     * candidate set - that is what "for that nation" means - and a nation whose race carries no
     * list falls back to the scenario catalogue, which over-states rather than under-states and is
     * the right direction for a worst case.
     */
    private static model.TipoTropa strongestFor(ArmySim army, Pelotao pelotao, Cenario cenario,
            business.facade.ExercitoFacade facade) {
        final boolean naval = pelotao.getTipoTropa() != null && pelotao.getTipoTropa().isBarcos();
        final model.TipoTropa original = pelotao.getTipoTropa();
        model.TipoTropa best = null;
        int bestAttack = -1;
        int bestDefence = -1;
        for (model.TipoTropa candidate : candidates(army, cenario)) {
            if (candidate == null || candidate.isBarcos() != naval
                    || ScenarioDefaults.PLACEHOLDER_CODE.equals(candidate.getCodigo())
                    || ScenarioDefaults.PLACEHOLDER_SHIP_CODE.equals(candidate.getCodigo())) {
                continue;
            }
            // Measured by putting the candidate IN the platoon, which is the only way the training,
            // the terrain and the nation bonuses all reach the number.
            pelotao.setTipoTropa(candidate);
            final int attack = facade.getAtaquePelotao(pelotao, army);
            final int defence = facade.getDefesaPelotao(pelotao, army);
            if (attack > bestAttack || (attack == bestAttack && defence > bestDefence)) {
                best = candidate;
                bestAttack = attack;
                bestDefence = defence;
            }
        }
        pelotao.setTipoTropa(original);
        return best;
    }

    /** That nation's race, or the whole catalogue when it has none - see {@code strongestFor}. */
    private static java.util.Collection<model.TipoTropa> candidates(ArmySim army, Cenario cenario) {
        if (army.getNacao() != null && army.getNacao().getRaca() != null
                && !army.getNacao().getRaca().getTropas().isEmpty()) {
            return army.getNacao().getRaca().getTropas().keySet();
        }
        return cenario == null ? new java.util.ArrayList<model.TipoTropa>()
                : cenario.getTipoTropas().values();
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
