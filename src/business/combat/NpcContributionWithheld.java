package business.combat;

import business.interfaces.IExercito;

/**
 * The client's side of the T-906 seam: it does not know, and it says so.
 *
 * This is the whole public implementation of {@link NpcContribution}, and it is deliberately not a
 * partial one. An approximation would be worse than a zero in both directions: too low and the
 * player is told his enemy is weaker than he is, too high and the number is an invention nobody can
 * check. Zero is at least a bound he can reason about, and {@link #isModelled} is what turns it
 * from a silent omission into a stated one.
 *
 * <h3>Not a stub to be filled in later</h3>
 *
 * D-10 made this permanent. The "not modelled" list in the result shrinks towards zero as the
 * simulator improves; the "withheld by design" list never empties, and this is its largest member.
 * A future reader who sees a zero here and thinks it is unfinished should read {@link
 * NpcContribution} first.
 */
public final class NpcContributionWithheld implements NpcContribution {

    /** Stateless, so one is enough. */
    public static final NpcContribution INSTANCE = new NpcContributionWithheld();

    private NpcContributionWithheld() {
    }

    @Override
    public long npcAttack(IExercito army, int round, boolean naval) {
        return 0;
    }

    @Override
    public boolean isModelled() {
        return false;
    }
}
