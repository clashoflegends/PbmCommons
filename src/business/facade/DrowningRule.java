package business.facade;

/**
 * How many of a fleet's troops drown when its last hull goes down within reach of shore.
 *
 * <h3>This class is called by the Judge. Changing it changes live turns.</h3>
 *
 * It is the one rule the simulator and the Judge must agree on to the unit, so it exists once and
 * both call it. It deliberately does NOT live in {@code business.combat}: that package has no Judge
 * callers at all, which is what makes every BattleSim change safe to ship without auditing the
 * Judge, and one import would quietly end that. A pure {@code int -> int} function in a package the
 * Judge already depends on costs nothing and keeps the invariant.
 *
 * <h3>The rule (John, 2026-09-26, T-817)</h3>
 *
 * Drowning used to be {@code SysApoio.rand(15) + 10} - 11% to 25%, a die. It is now a function of
 * the fleet commander's skill, with the die removed:
 *
 * <ul>
 *   <li>skill 10 or less: <b>25%</b></li>
 *   <li>skill 50: <b>18%</b></li>
 *   <li>skill 100 or more: <b>11%</b></li>
 * </ul>
 *
 * Piecewise linear with the knee at 50, John's choice between the two candidate fits, because he
 * stated the three anchors as requirements rather than as samples - a single line through 10 and
 * 100 misses the middle one by a point. The range is unchanged, so no fleet drowns at a rate the
 * old die could not have produced; what changes is that the number is now earned rather than
 * rolled, which is the "the fun is in the decision, not the dice" principle applied to the one
 * place in naval combat that violated it.
 *
 * <h3>A fleet with no commander takes the floor</h3>
 *
 * Confirmed by John, same day, rather than inherited: a garrison is an army whose commander skill
 * is 0 ({@code isGarrison()} is {@code comandante <= 0}), so it lands on the 25% floor every time.
 * Nobody is steering, and the rule says so.
 *
 * <h3>Integer arithmetic, pinned</h3>
 *
 * The drop from the segment's top is rounded half-up as integers - {@code (d * 7 + 20) / 40} below
 * the knee, {@code (d * 7 + 25) / 50} above it - so there is no float anywhere and the two sides
 * cannot disagree by a rounding mode. Rounding the DROP rather than the rate means an exact half
 * resolves in the player's favour: skill 30 is 21%, not the 22% that rounding 21.5 upward would
 * give.
 */
public final class DrowningRule {

    /** At or below this skill nobody is steering, so the rate is the worst the rule allows. */
    private static final int SKILL_FLOOR = 10;
    /** John's middle anchor, and the knee of the two segments. */
    private static final int SKILL_KNEE = 50;
    /** At or above this skill the rate is the best the rule allows. */
    private static final int SKILL_CAP = 100;

    private static final int PERCENT_FLOOR = 25;
    private static final int PERCENT_KNEE = 18;
    private static final int PERCENT_CAP = 11;

    /** Each segment gives up the same 7 points, over a different span of skill. */
    private static final int SEGMENT_SPAN = PERCENT_FLOOR - PERCENT_KNEE;

    private DrowningRule() {
    }

    /**
     * @param periciaComandante the fleet commander's {@code periciaComandante}. Both sides read the
     *                          same field: the Judge through
     *                          {@code ExercitoControl.getComandantePericia()}, the simulator
     *                          through {@code ArmySim}, which copies it off the same accessor and
     *                          lets the player edit it. A commanderless army answers 0 on both.
     * @return whole percent of each carried platoon that drowns, 11 to 25, never outside it.
     */
    public static int percent(int periciaComandante) {
        if (periciaComandante <= SKILL_FLOOR) {
            return PERCENT_FLOOR;
        }
        if (periciaComandante >= SKILL_CAP) {
            return PERCENT_CAP;
        }
        if (periciaComandante <= SKILL_KNEE) {
            final int span = SKILL_KNEE - SKILL_FLOOR;
            return PERCENT_FLOOR
                    - ((periciaComandante - SKILL_FLOOR) * SEGMENT_SPAN + span / 2) / span;
        }
        final int span = SKILL_CAP - SKILL_KNEE;
        return PERCENT_KNEE - ((periciaComandante - SKILL_KNEE) * SEGMENT_SPAN + span / 2) / span;
    }
}
