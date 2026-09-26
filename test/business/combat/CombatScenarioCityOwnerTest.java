package business.combat;

import model.Cidade;
import model.Local;
import model.Nacao;
import model.Terreno;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The simulator must not write into the loaded world, and giving a city an owner used to.
 *
 * <h3>The leak</h3>
 *
 * {@code Cidade.setNacao} back-links - {@code nacao.addCidade(this)} - and {@code Nacao.addCidade}
 * is {@code cidades.add(cidade)} on a plain {@code ArrayList} with no dedup. The scenario's city is
 * a CLONE, so every call left a phantom city on the LOADED nation's list for the rest of the
 * session. It fired on every load whose city had no visible owner - {@code ScenarioLoader} puts
 * that at 27 of 207 cities in a live GoT12c EGF - and again on every change of the Ground panel's
 * owner combo, which a player can cycle freely.
 *
 * Same shape as the v2.1.928 defect, where the BattleSim edited the real armies: a what-if reaching
 * back into the world it was asked a question about.
 */
public class CombatScenarioCityOwnerTest {

    private static Nacao nacao(String codigo) {
        final Nacao ret = new Nacao();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        return ret;
    }

    /** A hex whose city has a real owner already, as the loaded world holds it. */
    private static Local hexOwnedBy(Nacao owner) {
        final Terreno terreno = new Terreno();
        terreno.setCodigo("P");
        terreno.setNome("Plain");
        final Cidade cidade = new Cidade();
        cidade.setCodigo("c1");
        cidade.setNome("Lannisport");
        cidade.setTamanho(4);
        if (owner != null) {
            cidade.setNacao(owner);     // the WORLD's own registration, which must survive
        }
        final Local ret = new Local();
        ret.setCodigo("0452");
        ret.setCoordenadas("0452");
        ret.setTerreno(terreno);
        ret.setCidade(cidade);
        return ret;
    }

    /** Opening the window on an ownerless city must leave the stand-in nation untouched. */
    @Test
    public void supplyingAMissingOwnerDoesNotAddAPhantomCityToTheNation() {
        final Nacao barbarians = nacao("barb");
        final CombatScenario scenario = new CombatScenario(null, hexOwnedBy(null));

        scenario.setCityOwnerIfUnknown(barbarians);

        assertSame(barbarians, scenario.getCidade().getNacao(), "the clone knows its owner");
        assertEquals(0, barbarians.getCidades().size(),
                "and the real nation has not been told about the clone");
    }

    /** And opening it repeatedly must not accumulate them. */
    @Test
    public void openingTheSameHexRepeatedlyAccumulatesNothing() {
        final Nacao barbarians = nacao("barb");
        for (int open = 0; open < 5; open++) {
            new CombatScenario(null, hexOwnedBy(null)).setCityOwnerIfUnknown(barbarians);
        }
        assertEquals(0, barbarians.getCidades().size(), "five opens, no phantoms");
    }

    /**
     * Cycling the owner combo is the worse case: the player can do it as often as he likes, and
     * every nation he passes through used to keep a copy.
     */
    @Test
    public void cyclingTheOwnerComboLeavesNoNationHoldingTheClone() {
        final Nacao one = nacao("one"), two = nacao("two"), three = nacao("three");
        final CombatScenario scenario = new CombatScenario(null, hexOwnedBy(null));

        for (int pass = 0; pass < 3; pass++) {
            scenario.setCityOwner(one);
            scenario.setCityOwner(two);
            scenario.setCityOwner(three);
        }

        assertSame(three, scenario.getCidade().getNacao(), "the last choice sticks");
        for (Nacao each : new Nacao[]{one, two, three}) {
            assertEquals(0, each.getCidades().size(),
                    each.getNome() + " must not be holding the scenario's clone");
        }
    }

    /**
     * The REAL city stays on its owner's list.
     *
     * The un-registration removes by identity for this reason: {@code BaseModel} overrides only
     * {@code compareTo}, by codigo, so the day anybody gives it an {@code equals} a plain
     * {@code List.remove} would start removing the world's own city of the same codigo instead of
     * the clone. This pins the behaviour that matters rather than the implementation.
     */
    @Test
    public void theWorldsOwnCityIsNotRemovedFromItsOwner() {
        final Nacao lannister = nacao("lannister");
        final Local hex = hexOwnedBy(lannister);
        final Cidade real = hex.getCidade();
        assertEquals(1, lannister.getCidades().size(), "the world registered its own city");

        final CombatScenario scenario = new CombatScenario(null, hex);
        scenario.setCityOwner(lannister);

        assertEquals(1, lannister.getCidades().size(), "still exactly one");
        assertSame(real, lannister.getCidades().get(0), "and it is the REAL city, not the clone");
    }
}
