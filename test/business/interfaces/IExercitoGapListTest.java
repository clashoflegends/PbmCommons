package business.interfaces;

import business.combat.ArmySim;
import model.Exercito;
import model.Nacao;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The combat gap list on {@link IExercito}, and the property that makes it safe to add. T-901.
 *
 * <h3>What these tests are actually protecting</h3>
 *
 * Not the behaviour - there is none here yet. They protect the DEFAULT, which throws. The tempting
 * "fix" when a throw shows up in a stack trace is to make it return an empty list or a zero, and
 * that would be the worst possible change: an empty enemy list means "fights nobody", so the engine
 * would resolve a battle in which nobody attacks anybody and report it as a result. A throw is
 * loud, a zero is a wrong battle, and nothing downstream can tell a wrong battle from a right one.
 *
 * So each of these is written to fail if somebody softens the default.
 */
public class IExercitoGapListTest {

    private static final Terreno PLAIN = new Terreno();

    private static Pelotao platoon(int qtd) {
        final TipoTropa tipo = new TipoTropa();
        tipo.setCodigo("inf");
        tipo.setNome("inf");
        final Pelotao ret = new Pelotao();
        ret.setTipoTropa(tipo);
        ret.setQtd(qtd);
        return ret;
    }

    private static Nacao nacao() {
        final Nacao ret = new Nacao();
        ret.setCodigo("m");
        ret.setNome("m");
        return ret;
    }

    /**
     * The one member of the list with a real default, because this IS its definition.
     *
     * Asserted on both public implementors rather than one: it is the only default that answers, so
     * it is the only one that could quietly answer differently for one of them.
     */
    @Test
    public void getPelotoesListIsTheValuesOfGetPelotoes() {
        final ArmySim sim = new ArmySim("mine", PLAIN, nacao());
        final Pelotao one = platoon(900);
        sim.getPelotoes().put(one.getCodigo(), one);

        // No setLocal here: Exercito.setLocal files the army into the hex's map under its own
        // codigo, and a bare new Exercito() has none - the NPE that produces looks like a platoon
        // problem and is not one.
        final Exercito model = new Exercito();
        final Pelotao two = platoon(400);
        model.getPelotoes().put(two.getCodigo(), two);

        assertEquals(1, sim.getPelotoesList().size());
        assertEquals(900, sim.getPelotoesList().iterator().next().getQtd());
        assertEquals(1, model.getPelotoesList().size());
        assertEquals(400, model.getPelotoesList().iterator().next().getQtd());
    }

    /**
     * Everything else THROWS on an implementor that has no combat behaviour.
     *
     * ArmySim gets its own bodies in T-904 and these will start failing then, which is the point -
     * the test is a checklist of what T-904 still owes, and it has to be edited deliberately rather
     * than drift green.
     */
    @Test
    public void theRestThrowOnArmySimUntilT904TeachesIt() {
        final ArmySim sim = new ArmySim("mine", PLAIN, nacao());

        assertThrows(UnsupportedOperationException.class, () -> sim.doCombateDano());
        assertThrows(UnsupportedOperationException.class, () -> sim.sumCombateDano(100));
        assertThrows(UnsupportedOperationException.class, () -> sim.getInimigos());
        assertThrows(UnsupportedOperationException.class, () -> sim.addInimigo(sim));
        assertThrows(UnsupportedOperationException.class, () -> sim.remInimigos());
        assertThrows(UnsupportedOperationException.class, () -> sim.isInimigo(sim));
        assertThrows(UnsupportedOperationException.class, () -> sim.setCombateXp(true));
    }

    /**
     * And PERMANENTLY on the plain model army, which is a data record rather than a combatant.
     *
     * No task will ever teach this one. Asking a {@code model.Exercito} to resolve combat is a
     * caller bug, and it should say so on the first call rather than on the tenth reading of a
     * result that looked plausible.
     */
    @Test
    public void theRestThrowOnThePlainModelArmyForever() {
        final Exercito model = new Exercito();

        assertThrows(UnsupportedOperationException.class, () -> model.doCombateDano());
        assertThrows(UnsupportedOperationException.class, () -> model.getInimigos());
        assertThrows(UnsupportedOperationException.class, () -> model.setCombateXp(true));
    }

    /** The message names the CLASS, because a bare UnsupportedOperationException says nothing. */
    @Test
    public void theRefusalSaysWhoRefused() {
        final ArmySim sim = new ArmySim("mine", PLAIN, nacao());

        final UnsupportedOperationException thrown = assertThrows(
                UnsupportedOperationException.class, () -> sim.getInimigos());

        assertTrue(thrown.getMessage().contains("ArmySim"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("getInimigos"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("T-901"), "and points at the sequence it belongs to");
    }
}
