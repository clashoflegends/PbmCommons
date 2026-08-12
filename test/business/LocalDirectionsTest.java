package business;

import business.facade.LocalFacade;
import java.util.Arrays;
import model.Local;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A hex's six direction sets (river, brook, ford, road, bridge, landing) are one digit per side and
 * live in {@code varchar(6)} columns - exactly the six sides, no slack. The map editor concatenates
 * onto whatever it loaded, and most stored rows are a single space, so a hex that ends up using all
 * six sides needs 7 characters and the world import dies on a truncation error, aborting the run.
 * That happened on hex 1533 of GoT08c with {@code landing=" 432165"}.
 */
class LocalDirectionsTest {

    private static Local hexWithLanding(String landing) {
        final Local local = new Local();
        local.setLanding(landing);
        return local;
    }

    @Test
    void allSixSidesFitTheColumnOnceThePaddingIsGone() {
        final Local local = hexWithLanding(" 432165");
        assertEquals(7, local.getLanding().length(), "the reported value really is over the limit");

        LocalFacade.normalizeDirections(local);

        assertEquals("432165", local.getLanding());
        assertTrue(local.getLanding().length() <= 6, "a six-sided hex must fit varchar(6)");
    }

    @Test
    void normalizingKeepsWhichSidesAreSet() {
        final Local local = hexWithLanding(" 432165");
        LocalFacade.normalizeDirections(local);

        for (int side = 1; side <= 6; side++) {
            assertTrue(local.isLanding(side), "side " + side + " stopped being a landing");
        }
    }

    @Test
    void aPaddedButEmptySetBecomesEmpty() {
        final Local local = hexWithLanding(" ");
        LocalFacade.normalizeDirections(local);
        assertEquals("", local.getLanding());
    }

    @Test
    void everyDirectionSetIsNormalisedNotJustLanding() {
        final Local local = new Local();
        local.setRio(" 12");
        local.setRiacho(" 3");
        local.setVau(" 4");
        local.setEstrada(" 123456");
        local.setPonte(" 5");
        local.setLanding(" 6");

        LocalFacade.normalizeDirections(local);

        assertEquals(Arrays.asList("12", "3", "4", "123456", "5", "6"),
                Arrays.asList(local.getRio(), local.getRiacho(), local.getVau(),
                        local.getEstrada(), local.getPonte(), local.getLanding()));
    }

    @Test
    void aNullSetBecomesEmptyRatherThanBlowingUp() {
        final Local local = new Local();
        local.setLanding(null);
        LocalFacade.normalizeDirections(local);
        assertEquals("", local.getLanding());
    }
}
