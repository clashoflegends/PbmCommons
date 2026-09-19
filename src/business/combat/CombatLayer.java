package business.combat;

/**
 * The three engagements a battle resolves, in order.
 *
 * A battle in a hex is a CHAIN, not a single fight. {@code CombateTmpbm.executaCombates()} runs the
 * naval layer, then the land layer, then the city assault, and two things about that sequence matter
 * more than the order itself:
 *
 * <ul>
 *   <li><b>Participation is re-evaluated between layers.</b> Land combat is recomputed after the
 *       naval layer because surviving fleets land their troops ("check again after landing navies"),
 *       and the city assault is recomputed after the land layer because an attacker may have just
 *       lost. Deciding who fights once, up front, is wrong for exactly the battles worth
 *       simulating.</li>
 *   <li><b>Survivors carry over.</b> What enters a layer is what came out of the one before.</li>
 * </ul>
 *
 * One army can appear in one, two or all three layers, and with different troops in each: its
 * {@code ;TTN;} platoons fight at sea and the land troops they carry fight ashore.
 */
public enum CombatLayer {

    /** Fleet against fleet. Fought by any army holding at least one {@code ;TTN;} platoon. */
    NAVY,
    /** Army against army ashore. A fleet with no land troops never reaches this layer. */
    ARMY,
    /** The assault on the hex's city, by whoever survived the land layer and intends to attack it. */
    CITY;

    /** Short marker for the roster badge, as in {@code N A C}. */
    public String getBadge() {
        return this.name().substring(0, 1);
    }
}
