package business.facade;

import model.Exercito;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An army carries TWO size bands and the display shows one of them.
 *
 * The case that found it is game 906 turn 3 hex 0452: House Tyrell's {@code Colin Florent} sits off
 * Lannisport reported as a "huge navy" with zero countable troops, while its {@code tamanhoExercito}
 * is 5 - the top bucket, a vast army, which is the force it can put ashore. The BattleSim showed the
 * naval word and dropped the one that decides whether the landing can be contested.
 */
public class ExercitoSizeBandTest {

    private final ExercitoFacade facade = new ExercitoFacade();

    /** Colin Florent's two numbers, and the fact that they disagree. */
    @Test
    public void aFleetsLandBandIsHiddenBehindItsNavalOne() {
        final Exercito fleet = new Exercito();
        fleet.setTamanhoEsquadra(4);
        fleet.setTamanhoExercito(5);

        final String shown = facade.getDescricaoTamanho(fleet);
        final String land = facade.getDescricaoTamanhoTerra(fleet);

        assertTrue(shown.length() > 0, "the display picks the naval band");
        assertTrue(land.length() > 0, "the land band is there to be read");
        assertNotEquals(shown, land,
                "the two bands describe different things, which is why both have to be shown");
    }

    /** With no ships the two agree, so nothing is added to the sentence. */
    @Test
    public void aPlainArmyHasOneBandAndTheyMatch() {
        final Exercito army = new Exercito();
        army.setTamanhoEsquadra(0);
        army.setTamanhoExercito(5);

        assertEquals(facade.getDescricaoTamanho(army), facade.getDescricaoTamanhoTerra(army));
    }

    /**
     * Band 0 is "unestimated", an absence rather than a size, so it reads as nothing at all.
     *
     * Returning the zero-th label instead would put the word for "cannot tell" where a size belongs
     * and make an unranked army look like a measured one.
     */
    @Test
    public void anUnrankedArmyReportsNoLandBandRatherThanTheZerothWord() {
        final Exercito unranked = new Exercito();
        unranked.setTamanhoExercito(0);

        assertEquals("", facade.getDescricaoTamanhoTerra(unranked));
    }

    /** A band outside the scale must not throw; the array is six long and the server owns it. */
    @Test
    public void anOutOfRangeBandIsEmptyNotAnException() {
        final Exercito odd = new Exercito();
        odd.setTamanhoExercito(99);

        assertEquals("", facade.getDescricaoTamanhoTerra(odd));
    }

    @Test
    public void aNullArmyIsEmptyNotAnException() {
        assertEquals("", facade.getDescricaoTamanhoTerra(null));
    }
}
