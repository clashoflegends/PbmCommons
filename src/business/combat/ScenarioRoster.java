package business.combat;

import business.facade.ExercitoFacade;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import model.Nacao;

/**
 * The roster tree's shape: my armies, the ones fighting with me, the ones fighting against me, and
 * the ones not fighting me.
 *
 * <h3>The matrix is the only input</h3>
 *
 * Every answer here comes from {@link HostilityMatrix} and nothing else. Not from teams, not from
 * alliances, not from whose file is loaded, not from any notion of a "side". Those are constructs
 * that exist outside a battle and are irrelevant inside one: the Judge itself does not model sides,
 * it gives each army a list of enemies it takes damage from, and that list is the whole of it. A hex
 * with thirteen mutually hostile nations has thirteen armies that each fight twelve others, and
 * naming that "thirteen sides" adds nothing.
 *
 * So this class does not partition anything. It answers one question per army, against the matrix,
 * from the observer's point of view, and the observer's point of view is the only thing that makes
 * "with" and "against" mean anything at all.
 *
 * <h3>Why the fourth group is "not fighting ME"</h3>
 *
 * The four categories are the player's own: mine, with me, against me, not fighting. The last is
 * written as NOT_FIGHTING_ME because that is what it can actually test. An army with no enemies at
 * all and an army busy with a private war against a third party are both outside the player's
 * battle, and the roster has no basis for separating them - the second one IS fighting, so calling
 * it "not fighting" would be false. Its enemies are listed on its own row, which is where that
 * information belongs.
 *
 * <h3>With no army of the observer's own present</h3>
 *
 * Nothing special happens, deliberately. "With me" and "against me" are empty because there is no
 * "me", so every army lands in NOT_FIGHTING_ME and its own enemy list carries the battle. Watching
 * two other nations fight over a hex is a legitimate use and it reports the fight correctly.
 */
public class ScenarioRoster {

    /** Where an army sits relative to the player at the keyboard. Derived, never assigned. */
    public enum Group {
        /** The observer's own. */
        MINE,
        /** Not hostile to the observer, and hostile to something that IS hostile to the observer. */
        FIGHTING_WITH_ME,
        /** Hostile to at least one of the observer's armies here. */
        FIGHTING_AGAINST_ME,
        /**
         * Everyone else: outside the observer's battle.
         *
         * Not an error state, and not a claim that the army is idle. In a free-for-all this is where
         * most of a crowded hex lives.
         */
        NOT_FIGHTING_ME
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
        final HostilityMatrix matrix = scenario.getMatrix();
        final List<ArmySim> mine = new ArrayList<>();
        for (ArmySim army : armies) {
            if (isMine(army, scenario)) {
                mine.add(army);
            }
        }
        final List<ArmySim> againstMe = new ArrayList<>();
        for (ArmySim army : armies) {
            if (!isMine(army, scenario) && isHostileToAny(matrix, army, mine)) {
                againstMe.add(army);
            }
        }
        for (ArmySim army : armies) {
            ret.put(army, groupOf(army, scenario, mine, againstMe, matrix));
        }
        return ret;
    }

    private static Group groupOf(ArmySim army, CombatScenario scenario, List<ArmySim> mine,
            List<ArmySim> againstMe, HostilityMatrix matrix) {
        if (isMine(army, scenario)) {
            return Group.MINE;
        }
        if (isHostileToAny(matrix, army, mine)) {
            return Group.FIGHTING_AGAINST_ME;
        }
        // It fights someone who is fighting me, and it is not fighting me. That is the whole of
        // what "with me" can mean, and it is read off the matrix like everything else. With no
        // army of my own present againstMe is empty, so this cannot fire, which is correct.
        if (isHostileToAny(matrix, army, againstMe)) {
            return Group.FIGHTING_WITH_ME;
        }
        return Group.NOT_FIGHTING_ME;
    }

    private static boolean isHostileToAny(HostilityMatrix matrix, ArmySim army,
            List<ArmySim> others) {
        for (ArmySim other : others) {
            if (matrix.isInimigo(army, other)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMine(ArmySim army, CombatScenario scenario) {
        final Nacao nacao = army.getNacao();
        return nacao != null && scenario.getObserver() != null
                && nacao.getOwner() == scenario.getObserver();
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

    /** An army the roster never saw is outside the battle, the group that claims least. */
    public Group getGroup(ArmySim army) {
        final Group ret = ofArmy.get(army);
        return ret == null ? Group.NOT_FIGHTING_ME : ret;
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
