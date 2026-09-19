package business.combat;

import model.TipoTropa;

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
    /**
     * The assault on the hex's city, by whoever survived the land layer and intends to attack it.
     *
     * TWO rounds, not one. Round 0 is siege engines against the FORTIFICATION and is fought only
     * when an attacker carries them; round 1 is the single army-versus-city exchange. Round 0 comes
     * first and can reduce the fortification, lowering the defense round 1 then computes - the
     * city's equivalent of the army layer's first-strike round.
     *
     * The city itself is passive throughout: it is attacked, it damages the attackers in proportion
     * to their troop counts, and it takes the result. It never chooses anything.
     */
    CITY;

    /** Short marker for the roster badge, as in {@code N A C}. */
    public String getBadge() {
        return this.name().substring(0, 1);
    }

    /**
     * Which layer a single platoon fights in, for the platoon table's {@code Lyr} column.
     *
     * Never {@link #CITY}: the city assault is made by the army as a whole, by whichever of its land
     * troops survived, so no platoon belongs to it on its own.
     *
     * The test is the PLATOON's troop type, not {@code isEsquadra}, which is an army-level predicate
     * and additionally requires a quantity above zero. A platoon typed to ships is a naval platoon
     * whether or not the player has just emptied it, and its row must not flicker between N and A
     * while he retypes the number.
     */
    public static CombatLayer of(TipoTropa tipoTropa) {
        return tipoTropa != null && tipoTropa.isBarcos() ? NAVY : ARMY;
    }
}
