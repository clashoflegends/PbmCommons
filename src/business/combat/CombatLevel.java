package business.combat;

/**
 * How far an army is willing to take a fight, mirroring the Judge's {@code ExercitoControl.combateNivel}.
 *
 * The Judge stores this as a plain int and documents it in one comment at the field
 * ({@code 0 so defende, 1 ataca exercito, 2 ataca CP, 3 destroi cp}). The simulator needs the same
 * four states, and needs them EDITABLE, because without this it cannot tell "my army sits in the hex
 * and defends" from "my army storms the city" - which is the commonest question anyone brings to a
 * simulator.
 *
 * {@link #getNivel()} keeps the Judge's integer so the two can be compared directly when the combat
 * engine is eventually shared. The city assault gate is {@code >= ATTACK_CITY}.
 *
 * Note {@link #RAZE_CITY} is not always reachable: the Judge clamps it down when the target city is
 * raze-protected ({@code setCombateNivel} -> {@code isRazeProtected()}), so a simulator offering it
 * unconditionally would promise an outcome the turn cannot produce.
 */
public enum CombatLevel {

    /** Fights only if attacked. Never initiates, never assaults the city. */
    DEFEND_ONLY(0),
    /** Attacks enemy armies in the hex, but leaves the city alone. */
    ATTACK_ARMY(1),
    /** Attacks enemy armies and then assaults the city. */
    ATTACK_CITY(2),
    /** Assaults the city and razes it. Clamped down by the Judge where raze protection applies. */
    RAZE_CITY(3);

    private final int nivel;

    CombatLevel(int nivel) {
        this.nivel = nivel;
    }

    /** The Judge's {@code combateNivel} integer for this level. */
    public int getNivel() {
        return nivel;
    }

    /** Does this level reach the city layer? Mirrors the Judge's {@code getCombateNivel() >= 2} gate. */
    public boolean isAttackCity() {
        return this.nivel >= ATTACK_CITY.nivel;
    }

    /**
     * @param nivel the Judge's integer
     * @return the matching level, or {@link #DEFEND_ONLY} for anything out of range - the safe end,
     *         because over-stating intent would invent an assault the player never ordered.
     */
    public static CombatLevel valueOf(int nivel) {
        for (CombatLevel ret : values()) {
            if (ret.nivel == nivel) {
                return ret;
            }
        }
        return DEFEND_ONLY;
    }
}
