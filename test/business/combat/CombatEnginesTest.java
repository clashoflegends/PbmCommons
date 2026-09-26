package business.combat;

import model.Cenario;
import model.Ordem;
import model.Partida;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which engine a game runs, and what the answer is allowed to be when it cannot be read.
 *
 * This predicate decides whether the rebuilt BattleSim appears on the radial menu at all, so a
 * wrong answer is not a wrong number - it is a tool that is missing, or one that confidently
 * models the wrong engine. Both failures are silent, which is why the "cannot tell" case is
 * asserted as hard as the two real ones.
 */
public class CombatEnginesTest {

    private static Ordem ordem(String codigo, String nome) {
        final Ordem ret = new Ordem();
        ret.setCodigo(codigo);
        ret.setNome(nome);
        return ret;
    }

    private static Partida partida(Ordem... ordens) {
        final Cenario cenario = new Cenario();
        for (Ordem one : ordens) {
            cenario.getOrdens().put(one.getCodigo(), one);
        }
        final Partida ret = new Partida();
        ret.setCenario(cenario);
        return ret;
    }

    /** A WDO-shaped scenario: the city-combat milestone is the newer family's marker. */
    @Test
    public void theCityCombatMilestoneMeansTheOtherFamily() {
        assertTrue(CombatEngines.isOtherFamily(partida(
                ordem("1001", "MilestoneCalculos"),
                ordem("1100", "MilestoneCityCombates"))),
                "MilestoneCityCombates builds a CombatCity, which is the engine this does not model");
    }

    /**
     * A GoT-shaped scenario: MilestoneCombates is the TRADITIONAL one and must not be confused
     * with it.
     *
     * The two names differ by one word and one of them is a substring-ish neighbour of the other,
     * so a sloppy `contains` would have called every game in the corpus the new engine and removed
     * the BattleSim from all of them.
     */
    @Test
    public void theOrdinaryCombatMilestoneIsNotTheOtherFamily() {
        assertFalse(CombatEngines.isOtherFamily(partida(
                ordem("261", "MilestoneCombates"),
                ordem("1001", "MilestoneCalculos"),
                ordem("450", "MilestoneNpcArmyRecruit"))),
                "this is game 866's shape, and its city battle demonstrably ran on CombateTmpbm");
    }

    /**
     * Cannot tell -> traditional, deliberately.
     *
     * The failure modes are not symmetric. Guessing "traditional" gives the player a forecast plus
     * a disclosure he can weigh; guessing "new" withdraws a working tool from a game that wanted
     * it, with nothing on screen to explain the absence.
     */
    @Test
    public void anUnreadableGameIsTreatedAsTraditional() {
        assertFalse(CombatEngines.isOtherFamily(null), "no game at all");
        assertFalse(CombatEngines.isOtherFamily(new Partida()), "a game with no scenario");
        assertFalse(CombatEngines.isOtherFamily(partida()), "a scenario with no orders");
    }

    /** Case must not decide it: these rows are typed by hand into the scenario tables. */
    @Test
    public void theMilestoneNameIsMatchedWithoutCase() {
        assertTrue(CombatEngines.isOtherFamily(partida(ordem("1", "milestonecitycombates"))));
        assertTrue(CombatEngines.isOtherFamily(partida(ordem("1", "MILESTONECITYCOMBATES"))));
    }
}
