package business.combat;

import model.Exercito;
import model.Nacao;
import model.Terreno;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The two doors into the old battle simulator have to open on the SAME tactic.
 *
 * <h3>The report this is the regression for</h3>
 *
 * A player opened the old simulator on his own army and the right-hand platoon column was empty.
 * Nothing had broken: that column is the casualty ORDER, Standard has no casualty order (see
 * {@link CasualtyMode#TATICA_STANDARD} and ComparatorCasualtiesSorter's table), and the army was
 * carrying Standard.
 *
 * <h3>Why it looked broken anyway</h3>
 *
 * Because the two ways in disagreed. A brand new army in the simulator starts on Charge, so that
 * door always showed a full list. An army pulled off the map kept whatever tactic it was carrying,
 * and {@code Exercito} defaults that to Standard - which every army has until its owner orders a
 * tactic for the turn. So the door players actually use opened on a blank list, with nothing on
 * screen to say why, while the other door looked fine.
 *
 * <p>
 * {@code BattleSimulatorControlerNew.getArmyListTableModel} now sets every loaded army to Charge.
 * This test pins the two constants that have to agree for that to be the right value; the reset
 * itself is a one-liner at the load site, and it is only correct while these two match.
 */
public class OpeningTacticTest {

    private static Nacao nacao() {
        final Nacao ret = new Nacao();
        ret.setCodigo("m");
        ret.setNome("m");
        return ret;
    }

    @Test
    public void aFreshSimulatedArmyOpensOnCharge() {
        final ArmySim army = new ArmySim("Blank", new Terreno(), nacao());
        assertEquals(CasualtyMode.TATICA_CHARGE, army.getTatica(),
                "a new army in the simulator no longer starts on Charge, so the load site that forces "
                + "Charge on every other army now disagrees with it");
    }

    @Test
    public void aRealArmyDefaultsToStandardWhichIsWhyTheResetExists() {
        // Not a complaint about the model: Standard IS the right default for an army nobody has given
        // a tactic to. It is only wrong as an OPENING tactic for a window whose job is to rank
        // casualties, which Standard does not do.
        assertEquals(CasualtyMode.TATICA_STANDARD, new Exercito().getTatica(),
                "Exercito no longer defaults to Standard - re-check whether the simulator still needs "
                + "to override the loaded tactic at all");
        assertNotEquals(CasualtyMode.TATICA_CHARGE, new Exercito().getTatica(),
                "Charge and Standard have collapsed to the same id");
    }

    @Test
    public void copyingARealArmyKeepsItsTacticSoTheLoadSiteHasToOverrideIt() {
        // If this ever stops being true the override at the load site becomes dead code rather than a
        // fix, and the comment there stops describing what happens.
        final model.Local hex = new model.Local();
        hex.setCoordenadas("0101");
        hex.setTerreno(new Terreno());
        final Exercito real = new Exercito();
        real.setCodigo("e1");
        real.setNome("e1");
        real.setNacao(nacao());
        real.setLocal(hex);
        real.setTatica(CasualtyMode.TATICA_STANDARD);
        assertEquals(CasualtyMode.TATICA_STANDARD, new ArmySim(real).getTatica(),
                "the copy no longer carries the army's tactic, so the simulator's override is moot");
    }

    @Test
    public void standardIsTheOnlyTacticWithNoCasualtyRanking() {
        // The whole reason the blank column was correct, kept here so a change to the mode table has
        // to come past this test and the player-facing explanation that goes with it.
        final ArmySim standard = new ArmySim("s", new Terreno(), nacao());
        standard.setTatica(CasualtyMode.TATICA_STANDARD);
        assertEquals(CasualtyMode.PROPORTIONAL, CasualtyMode.of(standard, null, CombatLayer.ARMY),
                "Standard no longer spreads casualties evenly, so the empty platoon list it produces "
                + "in the old simulator is no longer explainable");
    }
}
