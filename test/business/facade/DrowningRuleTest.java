package business.facade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The drowning curve, pinned to the unit.
 *
 * This is worth more than a normal arithmetic test, because the number it produces is written into
 * two codebases that ship on different days: a rounding change here silently desynchronises the
 * Judge from the simulator, and the only symptom is a forecast that is a few soldiers out. So the
 * anchors, the knee, both clamps and the rounding are all nailed down rather than sampled.
 */
public class DrowningRuleTest {

    /** John's three, stated as requirements. If any of these move, the rule has changed. */
    @Test
    public void hitsAllThreeAnchorsExactly() {
        assertEquals(25, DrowningRule.percent(10), "C10");
        assertEquals(18, DrowningRule.percent(50), "C50");
        assertEquals(11, DrowningRule.percent(100), "C100");
    }

    /**
     * A garrison is an army whose commander skill is 0, so it takes the floor. Confirmed by John
     * rather than inherited - see the class note.
     */
    @Test
    public void aFleetWithNoCommanderTakesTheFloor() {
        assertEquals(25, DrowningRule.percent(0), "garrison");
        assertEquals(25, DrowningRule.percent(-5), "and nothing below it goes lower");
    }

    /** Skill above the cap buys nothing more. There is no 10% commander. */
    @Test
    public void bothClampsHold() {
        assertEquals(25, DrowningRule.percent(1));
        assertEquals(11, DrowningRule.percent(101));
        assertEquals(11, DrowningRule.percent(150));
        assertEquals(11, DrowningRule.percent(Integer.MAX_VALUE));
    }

    /**
     * The knee has to agree with itself. Both segments meet at 50, and neither jumps across it -
     * the one place a piecewise fit can be wrong without being obviously wrong.
     */
    @Test
    public void theTwoSegmentsMeetAtTheKnee() {
        assertEquals(18, DrowningRule.percent(49), "just below");
        assertEquals(18, DrowningRule.percent(50), "at it");
        assertEquals(18, DrowningRule.percent(51), "just above");
    }

    /** Rounding the drop, not the rate: skill 30 sits on an exact half and resolves downward. */
    @Test
    public void anExactHalfResolvesInThePlayersFavour() {
        assertEquals(21, DrowningRule.percent(30), "25 - 3.5, and the drop rounds up to 4");
    }

    /** A better commander never drowns more men, at any skill in the range. */
    @Test
    public void theCurveNeverRises() {
        int previous = DrowningRule.percent(-100);
        for (int skill = -100; skill <= 200; skill++) {
            final int rate = DrowningRule.percent(skill);
            assertTrue(rate <= previous, "rate rose at skill " + skill);
            assertTrue(rate >= 11 && rate <= 25, "rate left the range at skill " + skill);
            previous = rate;
        }
    }

    /**
     * The whole range is reachable. A curve that quietly collapsed onto three or four values would
     * pass every test above and make the commander choice meaningless.
     */
    @Test
    public void everyPercentBetweenElevenAndTwentyFiveIsReachable() {
        final boolean[] seen = new boolean[26];
        for (int skill = 0; skill <= 100; skill++) {
            seen[DrowningRule.percent(skill)] = true;
        }
        for (int rate = 11; rate <= 25; rate++) {
            assertTrue(seen[rate], "no commander skill produces " + rate + " percent");
        }
    }
}
