package business.combat;

import model.Nacao;
import model.Terreno;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Every {@link ArmySim} has a codigo, from the moment it is constructed.
 *
 * <h3>The crash this is the regression for</h3>
 *
 * Crash 327292 (game 898, Counselor 930): "New army" then "Clone army" in the old BattleSim threw
 * a NullPointerException out of {@code BaseModel.setCodigo}, which normalises the string it is
 * given and therefore cannot be given null. The three-argument constructor left the codigo unset
 * and the copy constructor reads it.
 *
 * <h3>Why the fix is here and not a guard in the copy constructor</h3>
 *
 * Because the copy constructor is not the only reader. {@code CombatScenario.copy()} and
 * {@code CombatCopies} both go through it, which means Clone window, the what-if search and the
 * tactic sweep would each have hit the same throw the day any other caller forgot to assign one.
 * A codigo-less ArmySim is the defect; the copy constructor faithfully copying one is not.
 */
public class ArmySimCodigoTest {

    private static final Terreno PLAIN = new Terreno();

    private static Nacao nacao() {
        final Nacao ret = new Nacao();
        ret.setCodigo("m");
        ret.setNome("m");
        return ret;
    }

    @Test
    public void ahandBuiltArmyHasACodigo() {
        assertNotNull(new ArmySim("Blank", PLAIN, nacao()).getCodigo(),
                "the three-argument constructor is what 'New army' calls");
    }

    /** Two of them must not collide, or a roster loses a row and a result loses an outcome. */
    @Test
    public void twoHandBuiltArmiesDoNotShareOne() {
        assertNotEquals(new ArmySim("Blank", PLAIN, nacao()).getCodigo(),
                new ArmySim("Blank", PLAIN, nacao()).getCodigo());
    }

    /** The crash itself: build one by hand, then clone it. */
    @Test
    public void cloningAHandBuiltArmyDoesNotThrow() {
        final ArmySim original = new ArmySim("Blank", PLAIN, nacao());

        final ArmySim clone = new ArmySim(original);

        assertEquals(original.getCodigo(), clone.getCodigo(),
                "a clone carries the original's codigo, as it always did when there was one");
    }

    /** And the scenario-level copy, which Clone window, the what-if and the sweep all use. */
    @Test
    public void copyingAScenarioHoldingAHandBuiltArmyDoesNotThrow() {
        final model.Local hex = new model.Local();
        hex.setCodigo("1428");
        hex.setCoordenadas("1428");
        hex.setTerreno(PLAIN);
        final CombatScenario scenario = new CombatScenario(null, hex);
        scenario.addArmy(new ArmySim("Blank", PLAIN, nacao()), CombatScenario.Provenance.MANUAL);

        assertEquals(1, scenario.copy().getArmies().size());
    }

    /**
     * A caller that wants its own codigo still gets it.
     *
     * The constructor supplying one must not become a thing that silently overrides the roster
     * identities the window assigns - {@code doAddArmy} and the paste path both set their own
     * afterwards, and {@code findByCodigo} depends on that.
     */
    @Test
    public void acallerCanStillAssignItsOwn() {
        final ArmySim army = new ArmySim("Blank", PLAIN, nacao());

        army.setCodigo("sim-mine");

        assertEquals("sim-mine", army.getCodigo());
    }
}
