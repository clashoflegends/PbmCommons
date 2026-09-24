package business.combat;

import model.Local;
import model.Nacao;
import model.Terreno;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Three armies called "Garrison" have to be told apart, and a hex that never had the problem must
 * not pay for it.
 *
 * Game 802 turn 40 hex 1530, found by John testing the FFA path: two Wildlings garrisons and one
 * House Stark garrison. A garrison is an army without a commander, so its name is the generic word,
 * and every screen that names an army printed all three the same - roster, verdict, casualty table
 * and per-round table alike. "Garrison holds the field. Garrison, Garrison were destroyed."
 */
public class CombatScenarioDisplayNameTest {

    private static Nacao nacao(String nome) {
        final Nacao ret = new Nacao();
        ret.setNome(nome);
        ret.setCodigo(nome);
        return ret;
    }

    private static Local hex() {
        final Terreno terreno = new Terreno();
        terreno.setCodigo("P");
        terreno.setNome("Plain");
        final Local ret = new Local();
        ret.setCodigo("1530");
        ret.setCoordenadas("1530");
        ret.setTerreno(terreno);
        return ret;
    }

    private static ArmySim army(String nome, Nacao nacao, Local hex) {
        final ArmySim ret = new ArmySim(nome, hex.getTerreno(), nacao);
        ret.setCodigo(nome + System.identityHashCode(nacao) + Math.random());
        return ret;
    }

    /** Hex 1530 reproduced: two of one nation, one of another, all called the same thing. */
    @Test
    public void threeGarrisonsBecomeThreeDifferentNames() {
        final Local hex = hex();
        final Nacao wildlings = nacao("Wildlings"), stark = nacao("House Stark");
        final CombatScenario scenario = new CombatScenario(null, hex);
        final ArmySim one = army("Garrison", wildlings, hex);
        final ArmySim two = army("Garrison", wildlings, hex);
        final ArmySim three = army("Garrison", stark, hex);
        for (ArmySim each : new ArmySim[]{one, two, three}) {
            scenario.addArmy(each, CombatScenario.Provenance.ESTIMATED);
        }

        assertEquals("Garrison (Wildlings) 1", scenario.getDisplayName(one));
        assertEquals("Garrison (Wildlings) 2", scenario.getDisplayName(two));
        assertEquals("Garrison (House Stark)", scenario.getDisplayName(three),
                "one of its nation, so it needs no number");
    }

    /**
     * A hex where the names already differ is left completely alone.
     *
     * The qualification is a cure for a collision, not a house style. Adding the nation to every
     * army would lengthen every row in the common case to solve a problem that case does not have.
     */
    @Test
    public void armiesWithTheirOwnNamesAreNotQualified() {
        final Local hex = hex();
        final Nacao lannister = nacao("House Lannister"), tyrell = nacao("House Tyrell");
        final CombatScenario scenario = new CombatScenario(null, hex);
        final ArmySim jaime = army("Jaime Lannister", lannister, hex);
        final ArmySim colin = army("Colin Florent", tyrell, hex);
        scenario.addArmy(jaime, CombatScenario.Provenance.ESTIMATED);
        scenario.addArmy(colin, CombatScenario.Provenance.ESTIMATED);

        assertEquals("Jaime Lannister", scenario.getDisplayName(jaime));
        assertEquals("Colin Florent", scenario.getDisplayName(colin));
    }

    /** Two of a name but different nations: the nation settles it, so no ordinal is added. */
    @Test
    public void theNationAloneIsEnoughWhenItSeparatesThem() {
        final Local hex = hex();
        final Nacao wildlings = nacao("Wildlings"), stark = nacao("House Stark");
        final CombatScenario scenario = new CombatScenario(null, hex);
        final ArmySim one = army("Garrison", wildlings, hex);
        final ArmySim two = army("Garrison", stark, hex);
        scenario.addArmy(one, CombatScenario.Provenance.ESTIMATED);
        scenario.addArmy(two, CombatScenario.Provenance.ESTIMATED);

        assertEquals("Garrison (Wildlings)", scenario.getDisplayName(one));
        assertEquals("Garrison (House Stark)", scenario.getDisplayName(two));
    }

    /** A single army is never qualified, however generic its name. */
    @Test
    public void oneArmyNeedsNoQualification() {
        final Local hex = hex();
        final CombatScenario scenario = new CombatScenario(null, hex);
        final ArmySim alone = army("Garrison", nacao("Wildlings"), hex);
        scenario.addArmy(alone, CombatScenario.Provenance.ESTIMATED);

        assertEquals("Garrison", scenario.getDisplayName(alone));
    }

    @Test
    public void aNullArmyIsEmptyNotAnException() {
        assertEquals("", new CombatScenario(null, hex()).getDisplayName(null));
    }
}
