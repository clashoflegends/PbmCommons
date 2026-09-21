package business.combat;

import model.Cenario;
import model.Habilidade;
import model.Terreno;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whether the platoon table's row order means anything. T-437.
 *
 * John, 2026-09-20: "standard tactics splits the damage across all platoons equally, no specific
 * sequence like the other tactics. So not to mislead players, it hid the sequence."
 *
 * The old window's answer was to blank the whole platoon table under Standard - honest about the
 * order, and it took the troop list away and explained nothing. The rebuild keeps the list and says
 * in words what the order means, which is only safe if the rule is right. It is
 * {@code ExercitoControl.doCombateDano}:
 *
 * <pre>
 * if (isCombatTypeNaval() || (getTatica() != 2 &amp;&amp; cenario.hasCombatCasualtiesTactics()))
 *     doCombateDanoTatica()    // BY RANK
 * else
 *     doCombateBaixasPadrao()  // PROPORTIONAL
 * </pre>
 *
 * Three ways to have no sequence, not one - and the two beyond Standard are the ones a hand-written
 * check would have missed.
 */
public class CasualtyModeTest {

    private static final Terreno PLAIN = plain();

    private static Terreno plain() {
        final Terreno ret = new Terreno();
        ret.setCodigo("P");
        ret.setNome("Plain");
        return ret;
    }

    /** @param casualtyTactics whether the scenario carries {@code ;CTC;} */
    private static Cenario cenario(boolean casualtyTactics) {
        final Cenario ret = new Cenario();
        ret.setCodigo("c1");
        ret.setNome("Scenario");
        if (casualtyTactics) {
            final Habilidade hab = new Habilidade();
            hab.setCodigo(";CTC;");
            hab.setNome(";CTC;");
            ret.addHabilidade(hab);
        }
        return ret;
    }

    private static ArmySim army(int tatica) {
        final ArmySim ret = new ArmySim("Host", PLAIN, null);
        ret.setCodigo("a1");
        ret.setTatica(tatica);
        return ret;
    }

    /** Guerrilla in a scenario that ranks: the order is the answer, and the tactic is worth picking. */
    @Test
    public void aRankingTacticInARankingScenarioIsSequenced() {
        final CasualtyMode mode =
                CasualtyMode.of(army(4), cenario(true), CombatLayer.ARMY);

        assertEquals(CasualtyMode.BY_RANK, mode);
        assertTrue(mode.isSequenced());
    }

    /** Standard, John's case: equal split, so there is no first and no last. */
    @Test
    public void standardIsProportionalAndHasNoSequence() {
        final CasualtyMode mode = CasualtyMode.of(army(CasualtyMode.TATICA_STANDARD),
                cenario(true), CombatLayer.ARMY);

        assertEquals(CasualtyMode.PROPORTIONAL, mode);
        assertFalse(mode.isSequenced(), "showing this order as a ranking would mislead");
    }

    /**
     * A scenario WITHOUT {@code ;CTC;} has no sequence for ANY tactic. The case worth catching.
     *
     * Not obvious from the player's side at all: he picks Guerrilla, the table dutifully re-sorts,
     * and none of it means anything because the scenario never ranks land casualties.
     */
    @Test
    public void aScenarioWithoutCasualtyTacticsIsProportionalForEveryTactic() {
        for (int tatica = 0; tatica <= 9; tatica++) {
            assertEquals(CasualtyMode.PROPORTIONAL,
                    CasualtyMode.of(army(tatica), cenario(false), CombatLayer.ARMY),
                    "tactic " + tatica + " must not claim a sequence here");
        }
    }

    /**
     * Naval ALWAYS ranks, even under Standard and even where the scenario does not rank land.
     *
     * {@code isCombatTypeNaval()} short-circuits the whole condition, so one army can have a
     * meaningful order for its ships and none for its troops at the same time - which is why this
     * is answered per LAYER rather than per army.
     */
    @Test
    public void navalAlwaysRanksWhateverTheTacticOrScenario() {
        assertEquals(CasualtyMode.BY_RANK, CasualtyMode.of(army(CasualtyMode.TATICA_STANDARD),
                cenario(false), CombatLayer.NAVY));
        assertEquals(CasualtyMode.PROPORTIONAL, CasualtyMode.of(army(CasualtyMode.TATICA_STANDARD),
                cenario(false), CombatLayer.ARMY),
                "the same army, the same turn, two different answers");
    }

    /** No army and no scenario are real states here; neither may claim a sequence. */
    @Test
    public void theUnknownCasesDoNotClaimASequence() {
        assertFalse(CasualtyMode.of(null, cenario(true), CombatLayer.ARMY).isSequenced());
        assertFalse(CasualtyMode.of(army(4), null, CombatLayer.ARMY).isSequenced());
    }
}
