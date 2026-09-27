package business.combat;

import business.facade.CenarioFacade;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import model.Cenario;

/**
 * Every tactic against every tactic, scored on the whole battle. T-842b.
 *
 * <h3>Why a sweep rather than a prediction</h3>
 *
 * The enemy's tactic cannot be known. {@code vl_tactic} reaches the client only for a scouted army
 * and even then it is LAST TURN'S choice: in {@code CombateTmpbm} the player re-picks his tactic
 * every turn as part of his combat orders, so what he did last turn is history, not a commitment.
 * John, 2026-09-27: <i>"the tactic may (and likely will) change between the scout and the next
 * combat"</i>. There is therefore no intelligence problem to solve here and no prediction worth
 * making - only a robustness problem, and robustness is computable.
 *
 * <h3>His tactic changes two different things, and the second is the bigger one</h3>
 *
 * The obvious one is the multiplier: {@code bonusTatica[mine][his]} scales your attack and the
 * transpose scales his, between 80 and 120. The one that decides more battles is the CASUALTY
 * ORDER - his tactic sorts his own platoons into the order they die, on the single key of attack
 * value on this terrain, so it decides what of his is still standing when the land battle ends and
 * the city assault begins. Charge spends his hardest hitters first; Guerrilla spends them last.
 * That is why this sweeps the whole three-layer chain and scores the END of it, not a land round.
 *
 * <h3>Pinning him down</h3>
 *
 * John: <i>"an army with catapults must pick guerrila so that the catapults can survive to the city
 * round. Therefore, if I know my enemy needs the catapults to take the city, I can pin him
 * down."</i> That is the real feature, and it is DERIVED rather than hardcoded: each of his tactics
 * is scored against HIS objective too, and the ones that fail it drop out. If his siege train dies
 * before the walls under five of his six options, five of them were never available to him, and
 * whatever beats the sixth is the answer. No rule about catapults is written anywhere - the
 * simulation says which of his options work, and the same machinery will find patterns nobody has
 * named.
 *
 * <b>The inference rests on an assumed objective</b>, taken from his combat level, and that is the
 * one soft input in the whole thing. If he is not actually trying to take the city, the sharp
 * answer is sharp in the wrong direction. So both answers are produced - {@link #getSafeChoice}
 * against everything he could do, {@link #getSharpChoice} against what he needs to do - and the
 * caller must show which is which.
 *
 * <h3>Scope</h3>
 *
 * {@code CombateTmpbm} only, John's call: the other engine family gets its own simulator, and the
 * radial already withholds this one from those games. Also ONE pair at a time - your army against
 * one enemy - with every other army on the hex left as it stands. A full joint sweep over every
 * army's tactic is exponential and unreadable; the pair is the question a player actually asks.
 */
public final class TacticSweep {

    /** The traditional set, used when no scenario is available to ask. */
    private static final int[] TRADITIONAL = {0, 1, 2, 3, 4, 5};

    private TacticSweep() {
    }

    /** One combination, resolved. */
    public static final class Cell {

        private final int mine;
        private final int his;
        private boolean iAchieve;
        private boolean heAchieves;
        private int mySurvivors;
        private int hisSurvivors;

        Cell(int mine, int his) {
            this.mine = mine;
            this.his = his;
        }

        public int getMine() {
            return mine;
        }

        public int getHis() {
            return his;
        }

        /** Did I get what I came for, at the end of all three layers? */
        public boolean isMine() {
            return iAchieve;
        }

        /** Did HE, which is what decides whether this column was ever available to him. */
        public boolean isHis() {
            return heAchieves;
        }

        public int getMySurvivors() {
            return mySurvivors;
        }

        public int getHisSurvivors() {
            return hisSurvivors;
        }
    }

    /** The grid, plus the two readings of it that are worth putting in front of a player. */
    public static final class Result {

        private final List<Integer> myTactics = new ArrayList<>();
        private final List<Integer> hisTactics = new ArrayList<>();
        private final Set<Integer> hisViable = new LinkedHashSet<>();
        private Cell[][] cells = new Cell[0][0];
        private int safeChoice = -1;
        private int sharpChoice = -1;
        private int runs;

        public List<Integer> getMyTactics() {
            return myTactics;
        }

        public List<Integer> getHisTactics() {
            return hisTactics;
        }

        /** Row and column are POSITIONS in the two lists above, not tactic indices. */
        public Cell get(int myRow, int hisColumn) {
            return cells[myRow][hisColumn];
        }

        /**
         * The tactics that get HIM what he came for. Everything else is a column he cannot afford.
         *
         * Empty means none of his options work, in which case the battle is already decided and any
         * tactic of yours will do; full means his hands are free and only {@link #getSafeChoice}
         * means anything.
         */
        public Set<Integer> getHisViable() {
            return hisViable;
        }

        /** Best worst case against ANY tactic of his. The answer when you trust nothing. */
        public int getSafeChoice() {
            return safeChoice;
        }

        /**
         * Best worst case against the tactics he can actually afford.
         *
         * Equals the safe choice when his hands are free. When his objective pins him to one
         * option this is a straight best response, which is the case John described.
         */
        public int getSharpChoice() {
            return sharpChoice;
        }

        /** How many of my tactics achieve my goal against this one of his. */
        public int coverageOf(int myTactic) {
            int ret = 0;
            final int row = myTactics.indexOf(myTactic);
            if (row < 0) {
                return ret;
            }
            for (int col = 0; col < hisTactics.size(); col++) {
                if (cells[row][col].isMine()) {
                    ret++;
                }
            }
            return ret;
        }

        public int getRuns() {
            return runs;
        }
    }

    /**
     * Resolves the whole grid.
     *
     * @param scenario the player's battle. Never modified: every cell runs on a copy.
     * @param cenario  the catalogue, which also decides which tactic indices are legal here. Using
     *                 the wrong family's index silently zeroes an army's whole troop attack, so the
     *                 axes come from the scenario rather than from a constant (T-822).
     * @param myIndex  my army's position in {@code getArmies()}; copies preserve it.
     * @param hisIndex the one enemy whose tactic is being swept.
     */
    public static Result sweep(CombatScenario scenario, Cenario cenario, int myIndex, int hisIndex) {
        final Result ret = new Result();
        if (scenario == null || myIndex < 0 || hisIndex < 0
                || myIndex >= scenario.getArmies().size()
                || hisIndex >= scenario.getArmies().size() || myIndex == hisIndex) {
            return ret;
        }
        for (int tactic : validTactics(cenario)) {
            ret.myTactics.add(tactic);
            ret.hisTactics.add(tactic);
        }
        ret.cells = new Cell[ret.myTactics.size()][ret.hisTactics.size()];
        final WhatIfSearch.Goal myGoal = goalOf(scenario, scenario.getArmies().get(myIndex));
        final WhatIfSearch.Goal hisGoal = goalOf(scenario, scenario.getArmies().get(hisIndex));
        for (int row = 0; row < ret.myTactics.size(); row++) {
            for (int col = 0; col < ret.hisTactics.size(); col++) {
                ret.cells[row][col] = run(scenario, cenario, myIndex, hisIndex,
                        ret.myTactics.get(row), ret.hisTactics.get(col), myGoal, hisGoal);
                ret.runs++;
            }
        }
        for (int col = 0; col < ret.hisTactics.size(); col++) {
            for (int row = 0; row < ret.myTactics.size(); row++) {
                if (ret.cells[row][col].isHis()) {
                    ret.hisViable.add(ret.hisTactics.get(col));
                    break;
                }
            }
        }
        ret.safeChoice = bestAgainst(ret, ret.hisTactics);
        ret.sharpChoice = ret.hisViable.isEmpty() ? ret.safeChoice
                : bestAgainst(ret, new ArrayList<>(ret.hisViable));
        return ret;
    }

    /**
     * Maximin over a lexicographic score: achieving the goal beats not achieving it, and among
     * equals, more of my men left is better.
     *
     * Ranking on survivors alone would prefer a tactic that loses gently to one that wins narrowly.
     * Ranking on the goal alone cannot separate six tactics that all fail, which is the common case
     * against a stronger enemy - and "which of these loses least badly" is exactly what a player
     * reaching for this screen wants to know.
     */
    private static int bestAgainst(Result grid, List<Integer> hisOptions) {
        int best = -1;
        boolean bestWins = false;
        int bestFloor = Integer.MIN_VALUE;
        for (int row = 0; row < grid.myTactics.size(); row++) {
            boolean winsAll = true;
            int floor = Integer.MAX_VALUE;
            for (Integer his : hisOptions) {
                final int col = grid.hisTactics.indexOf(his);
                if (col < 0) {
                    continue;
                }
                winsAll &= grid.cells[row][col].isMine();
                floor = Math.min(floor, grid.cells[row][col].getMySurvivors());
            }
            if (best < 0 || (winsAll && !bestWins)
                    || (winsAll == bestWins && floor > bestFloor)) {
                best = grid.myTactics.get(row);
                bestWins = winsAll;
                bestFloor = floor;
            }
        }
        return best;
    }

    /** One combination on a fresh copy, resolved through all three layers. */
    private static Cell run(CombatScenario scenario, Cenario cenario, int myIndex, int hisIndex,
            int myTactic, int hisTactic, WhatIfSearch.Goal myGoal, WhatIfSearch.Goal hisGoal) {
        final Cell ret = new Cell(myTactic, hisTactic);
        final CombatScenario trial = scenario.copy();
        final ArmySim mine = trial.getArmies().get(myIndex);
        final ArmySim his = trial.getArmies().get(hisIndex);
        mine.setTatica(myTactic);
        his.setTatica(hisTactic);
        final CombatResult result = new CombatChain().resolve(trial, cenario);
        ret.iAchieve = achieved(result, mine, myGoal);
        ret.heAchieves = achieved(result, his, hisGoal);
        ret.mySurvivors = survivors(result, mine);
        ret.hisSurvivors = survivors(result, his);
        return ret;
    }

    /**
     * What an army was trying to do, read off its combat level rather than assumed.
     *
     * <b>Gated on there BEING a city</b>, and that guard is load-bearing rather than defensive.
     * {@code CombatScenario} defaults every army's combat level to {@code ATTACK_CITY} because
     * intent does not ride the EGF (D-17), so on a hex with no city every army would read as trying
     * to take one, every army would fail to, the viable set would come out EMPTY for both sides,
     * and the sharp recommendation would be derived from nothing - silently, on the commonest kind
     * of battle there is. Found by a probe when a one-sided fight reported nobody achieving
     * anything.
     *
     * It is still the softest input here and the caller has to say so: for a foreign army the level
     * is whatever the player left in the combo. That is exactly why the sweep produces a safe
     * answer as well as a sharp one.
     */
    private static WhatIfSearch.Goal goalOf(CombatScenario scenario, ArmySim army) {
        return army != null && scenario != null && scenario.isCityParticipates()
                && army.getCombatLevel() == CombatLevel.ATTACK_CITY
                ? WhatIfSearch.Goal.TAKE_THE_CITY : WhatIfSearch.Goal.HOLD_THE_FIELD;
    }

    private static boolean achieved(CombatResult result, ArmySim army, WhatIfSearch.Goal goal) {
        if (result == null) {
            return false;
        }
        if (goal == WhatIfSearch.Goal.TAKE_THE_CITY) {
            final CityCombatResolver.CityResult city = result.getCityResult();
            return city != null && city.getAttackers().contains(army)
                    && (city.getOutcome() == CityCombatResolver.CityOutcome.CAPTURED
                    || city.getOutcome() == CityCombatResolver.CityOutcome.RAZED);
        }
        return result.getOutcome(army, CombatLayer.ARMY) == CombatResult.Outcome.WON;
    }

    /**
     * Bodies left at the END of the chain, read from the RESULT.
     *
     * Not from the army. The chain fights on its own copies ({@code CombatCopies}) and never edits
     * the scenario it was handed, so an army's own platoons still hold their pre-battle numbers
     * afterwards - reading them gives the same figure for all 36 cells, a grid that is flat by
     * construction, and a recommendation drawn from nothing. Caught because a test asserted the
     * grid was not flat, which is the weakest-looking assertion in the suite and the one that
     * earned its place.
     *
     * A platoon that took no part answers -1 rather than 0, and its men are still standing, so it
     * keeps its full count.
     */
    private static int survivors(CombatResult result, ArmySim army) {
        int ret = 0;
        for (model.Pelotao pelotao : army.getPelotoes().values()) {
            if (pelotao.getTipoTropa() == null || pelotao.getTipoTropa().isBarcos()) {
                continue;
            }
            final int after = result == null ? -1 : result.getAfter(pelotao);
            ret += after < 0 ? pelotao.getQtd() : after;
        }
        return ret;
    }

    /**
     * The tactic indices this scenario actually uses.
     *
     * Never a constant. The two engine families overlap but neither contains the other, and an
     * index from the wrong one reads a cell of {@code bonusTatica} that nobody filled - which is 0,
     * and a {@code modTatica} of 0 multiplies an army's entire troop attack away with no error and
     * no message. A sweep is precisely the thing that would walk into every such cell at once.
     */
    public static int[] validTactics(Cenario cenario) {
        if (cenario == null) {
            return TRADITIONAL.clone();
        }
        final Set<Integer> keys = new CenarioFacade().listTaticasAsList(cenario).keySet();
        if (keys.isEmpty()) {
            return TRADITIONAL.clone();
        }
        final int[] ret = new int[keys.size()];
        int at = 0;
        for (Integer key : keys) {
            ret[at++] = key;
        }
        return ret;
    }
}
