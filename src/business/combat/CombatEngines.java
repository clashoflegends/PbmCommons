package business.combat;

import model.Ordem;
import model.Partida;

/**
 * Which of the Judge's two combat engines a game runs, answered from the EGF.
 *
 * <h3>There are two families and they are not variations of each other</h3>
 *
 * The traditional one, {@code CombateTmpbm}, is what almost every battle uses and is the ONLY one
 * {@link CombatChain} models. The newer family - {@code CombatBase}, {@code CombatArmy},
 * {@code CombatCity} - resolves platoon against platoon, which is a different resolution model
 * rather than a different set of numbers.
 *
 * <h3>How a game says which it uses</h3>
 *
 * {@code OrdemJudgeFactory} builds a {@code MilestoneCityCombats} - which runs {@code CombatCity} -
 * when the scenario carries an order row named {@code MilestoneCityCombates}. Those rows ride the
 * EGF in {@code Cenario.getOrdens()}, so this is read, not guessed. Confirmed by John, 2026-09-26:
 * the milestone is part of the new engine, used by WDO. Checked against a live file: game 866's
 * scenario carries {@code MilestoneCombates} and not the city one, which is why its assault at
 * Summerhall resolved through {@code CombateTmpbm} and why validating the city layer against that
 * battle was sound.
 *
 * <h3>Why this is a class and not a method on the scenario</h3>
 *
 * Because it is asked in two places at two different times: by the RADIAL MENU, before any
 * scenario exists, to decide whether to offer the new BattleSim at all; and by a finished result,
 * to disclose that the city numbers came from the wrong engine. Two readings of one question would
 * drift, and this is a question where drifting means the menu offers a tool the result then
 * disowns.
 */
public final class CombatEngines {

    /** The scenario order row that switches a game onto the newer family. */
    private static final String NEW_ENGINE_MILESTONE = "MilestoneCityCombates";

    private CombatEngines() {
    }

    /**
     * True when this game resolves city battles with the engine the simulator does NOT model.
     *
     * <b>Answers FALSE when it cannot tell</b>, and that is deliberate. An unreadable or partial
     * Partida means the traditional engine, because that is what almost every game runs and
     * because the failure modes are not symmetric: guessing "traditional" gives a player a
     * forecast plus a disclosure he can weigh, while guessing "new" would withdraw a working tool
     * from a game that wanted it, with nothing on screen explaining the absence.
     */
    public static boolean isOtherFamily(Partida partida) {
        if (partida == null || partida.getCenario() == null
                || partida.getCenario().getOrdens() == null) {
            return false;
        }
        for (Ordem ordem : partida.getCenario().getOrdens().values()) {
            if (ordem != null && NEW_ENGINE_MILESTONE.equalsIgnoreCase(ordem.getNome())) {
                return true;
            }
        }
        return false;
    }
}
