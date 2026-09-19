package business.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import business.facade.ExercitoFacade;
import model.Nacao;

/**
 * The roster tree's shape: which side each army is on, from the observer's point of view.
 *
 * <h3>Why this is not a TreeModel</h3>
 *
 * It is the structure a {@code JTree} renders, not the tree itself. Keeping it as plain objects here
 * means the grouping rules are unit-tested without a window, and means the Counselor's wrapper is a
 * dumb adapter. It also keeps the whole thing on the right side of the eventual
 * combat-code-into-PbmCommons move.
 *
 * <h3>Four groups, not three</h3>
 *
 * The design sketched MINE / ALLIED / HOSTILE. Implementing it turned up a fourth case that has to
 * exist: in a free-for-all most foreign armies are neither known allies nor known enemies, and
 * {@link HostilityMatrix} deliberately marks those pairs as ASSUMED rather than resolved. Filing
 * them under ALLIED would tell the player they are on his side, which is precisely the claim the
 * matrix refused to make. So they get {@link Group#NEUTRAL}, and the status bar says how many pairs
 * that rests on.
 *
 * <h3>Why "allied" means a merged EGF</h3>
 *
 * Not-hostile is not the same as allied, and the matrix only tracks hostility. The one positive
 * signal the client actually holds is that an ally's EGF was merged into this world: the Counselor
 * autoloads an ally's file and nobody else's. So ALLIED is "its own EGF is here and it is not
 * hostile to me", which is conservative in the right direction - a real ally whose file was not
 * loaded shows up as NEUTRAL rather than being claimed as a friend.
 *
 * <h3>What this deliberately does NOT do</h3>
 *
 * It does not partition a free-for-all into sides. "Side" is only well defined when not-hostile is
 * transitive, which is exactly what a free-for-all breaks: A and B may both be at peace with C and
 * at war with each other. Any grouping that tried would be inventing structure. A multi-side team
 * game is the case where sides ARE well defined, and that is a later task, not an MVP one.
 */
public class ScenarioRoster {

    /** Where an army sits relative to the player at the keyboard. */
    public enum Group {
        /** The observer's own. */
        MINE,
        /** Its own EGF was merged in and it is not hostile to the observer. */
        ALLIED,
        /** Hostile to at least one of the observer's armies here. */
        HOSTILE,
        /**
         * Everyone else: no read relationship, or a read peace that is not an alliance.
         *
         * Not an error state. In a free-for-all this is where most of the hex lives, and saying so
         * is the honest answer.
         */
        NEUTRAL
    }

    private static final ExercitoFacade exercitoFacade = new ExercitoFacade();

    private final Map<Group, List<ArmySim>> byGroup = new EnumMap<>(Group.class);
    private final Map<ArmySim, Group> ofArmy = new IdentityHashMap<>();

    private ScenarioRoster() {
        for (Group group : Group.values()) {
            byGroup.put(group, new ArrayList<ArmySim>());
        }
    }

    /**
     * Groups a scenario's armies.
     *
     * Order within a group is load order, which is the hex's own order. Deliberately not sorted by
     * strength: the roster is a map of who is present, and re-ordering it as the player edits would
     * make rows move under the cursor.
     */
    public static ScenarioRoster of(CombatScenario scenario) {
        final ScenarioRoster ret = new ScenarioRoster();
        if (scenario == null) {
            return ret;
        }
        final List<ArmySim> armies = scenario.getArmies();
        final List<ArmySim> mine = new ArrayList<>();
        for (ArmySim army : armies) {
            if (isMine(army, scenario)) {
                mine.add(army);
            }
        }
        final HostilityMatrix matrix = scenario.getMatrix();
        for (ArmySim army : armies) {
            ret.put(army, groupOf(army, scenario, mine, matrix));
        }
        return ret;
    }

    private static Group groupOf(ArmySim army, CombatScenario scenario, List<ArmySim> mine,
            HostilityMatrix matrix) {
        if (isMine(army, scenario)) {
            return Group.MINE;
        }
        for (ArmySim ours : mine) {
            if (matrix.isInimigo(ours, army)) {
                return Group.HOSTILE;
            }
        }
        // Not hostile to anything of mine - but with no army of my own present that is vacuously
        // true, so it is not evidence of friendship and must not be read as any.
        return isMerged(army, scenario) && !mine.isEmpty() ? Group.ALLIED : Group.NEUTRAL;
    }

    private static boolean isMine(ArmySim army, CombatScenario scenario) {
        final Nacao nacao = army.getNacao();
        return nacao != null && scenario.getObserver() != null
                && nacao.getOwner() == scenario.getObserver();
    }

    private static boolean isMerged(ArmySim army, CombatScenario scenario) {
        final Nacao nacao = army.getNacao();
        if (nacao == null) {
            return false;
        }
        for (Nacao merged : scenario.getMergedNacoes()) {
            if (merged == nacao) {
                return true;
            }
        }
        return false;
    }

    private void put(ArmySim army, Group group) {
        byGroup.get(group).add(army);
        ofArmy.put(army, group);
    }

    /** Groups that actually hold an army, in enum order. An empty group is not a tree node. */
    public List<Group> getGroups() {
        final List<Group> ret = new ArrayList<>();
        for (Group group : Group.values()) {
            if (!byGroup.get(group).isEmpty()) {
                ret.add(group);
            }
        }
        return ret;
    }

    public List<ArmySim> getArmies(Group group) {
        return Collections.unmodifiableList(byGroup.get(group));
    }

    /** An army the roster never saw is NEUTRAL, the group that claims least. */
    public Group getGroup(ArmySim army) {
        final Group ret = ofArmy.get(army);
        return ret == null ? Group.NEUTRAL : ret;
    }

    /** Running troop total for a group node, as in the wireframe's right-hand column. */
    public int getQtTropas(Group group) {
        int ret = 0;
        for (ArmySim army : byGroup.get(group)) {
            ret += exercitoFacade.getQtTropasTotal(army);
        }
        return ret;
    }
}
