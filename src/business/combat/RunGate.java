package business.combat;

/**
 * Whether this scenario can be resolved, and if not, the ONE thing standing in the way.
 *
 * <h3>Why a gate and not a boolean</h3>
 *
 * R-40: "In MVP, Run is disabled with a stated reason. It never performs a silent no-op." A button
 * that does nothing when pressed is the complaint that started the whole rebuild, and a boolean
 * cannot carry a reason. This enum is the model half; {@code BattleSimConverter} turns it into the
 * sentence in the status bar, so the rule can be tested without a bundle and without a window.
 *
 * <h3>Order matters: the fixable answers come first</h3>
 *
 * More than one of these can be true at once. They are reported most-fixable first, because the
 * player can do something about an empty roster, about a room full of friends, and about armies that
 * cannot reach each other - so the first answer he is given is always the one he can act on.
 *
 * <h3>There was a fifth state, and T-801 took it away</h3>
 *
 * {@code NO_ENGINE} said the scenario was resolvable and the engine was not built yet. It was last in
 * the order because it was what was left once everything the player controls was already right, and
 * it was written to disappear rather than be unpicked. T-801 landed the land resolver, so it is gone,
 * along with the {@code engineExists} flag that produced it. A later layer does not bring it back: a
 * scenario that engages on ANY layer is {@link #READY}, and the result names the layers it resolved.
 *
 * <h3>Allegiance is never a blocker</h3>
 *
 * R-41 says so explicitly, and it is worth restating where the gate lives: a pair nothing could
 * resolve is ASSUMED not hostile and disclosed in the status bar, never used to refuse the run. The
 * simulator's job is to answer the question the player asked with the information he has, saying
 * which parts were guesses. Refusing because something was guessed would make the disclosure a
 * punishment.
 */
public enum RunGate {

    /** Nothing has been put on the hex. */
    NO_ARMIES,

    /**
     * Armies are here, but no two of them are hostile. Nothing to resolve, and nothing wrong.
     *
     * Fixable by the player, in the diplomacy panel: the matrix is the law, and if he believes the
     * war is coming he can say so and see what falls out of it.
     */
    NO_HOSTILE_PAIR,

    /**
     * Hostile pairs exist, and yet no army takes part in ANY layer.
     *
     * A real state and an easy one to miss, because {@code hasCombat()} is a property of the
     * relationship matrix alone - it says a hostile PAIR exists, not that anybody ever exchanges
     * blows. Live example from hex 0350: the player's fleet and an enemy land army, correctly read
     * as hostile and grouped as "Fighting against me", where the fleet carries no land troops and
     * the enemy has no ships. Every layer declines: no enemy fleet at sea, nothing of his ashore,
     * and neither is ordered to assault the city. Without this state the button would have enabled
     * and the engine resolved an empty battle.
     *
     * Often fixable too, and the army editor already says how: the per-layer reasons name the one
     * test that stopped each army, and several of them are orders the player can change.
     */
    NO_ENGAGEMENT,

    /** Resolvable, and there is something to resolve it with. */
    READY;

    /** The only state in which the Run button may be enabled. */
    public boolean isRunnable() {
        return this == READY;
    }
}
