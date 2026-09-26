package business.combat;

import business.facade.ExercitoFacade;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import model.Nacao;

/**
 * The roster tree's shape: one node per NATION on the hex, its armies under it, and who it fights.
 *
 * <h3>It used to group by relation to the player, and that was wrong</h3>
 *
 * The four groups were MINE, FIGHTING_WITH_ME, FIGHTING_AGAINST_ME and NOT_FIGHTING_ME. They read
 * well while the player was always in the fight, and they fell apart the moment he was not: with no
 * army of his own on the hex there is no "me", so every army landed in NOT_FIGHTING_ME and the tree
 * showed a single node headed <i>"Not fighting me (28,594)"</i> over a battle it was actively
 * simulating. Game 802 turn 40 hex 1530, watched by a third party, is exactly that.
 *
 * John, 2026-09-23: <i>"The narrative might be different, but the battle outcome/casualties will be
 * the same. Judge's math is POV agnostic. We can drop the POV view from BattleSim if it simplifies
 * things."</i> It does. The observer decides which numbers arrive pre-filled - that is provenance,
 * and it stays - but it decides nothing about the answer, so it has no business shaping the tree.
 *
 * <h3>Nation is the honest grouping, and it is the Judge's own</h3>
 *
 * This class's previous javadoc already argued most of the way there: the Judge "does not model
 * sides, it gives each army a list of enemies it takes damage from, and that list is the whole of
 * it", and naming a crowded hex "thirteen sides" adds nothing. One node per nation with its enemies
 * on it IS that structure, and it reads identically whether or not the player has an army present.
 *
 * <h3>The enemies have to come with it</h3>
 *
 * Dropping the four groups would otherwise LOSE information rather than simplify: they were the only
 * place in the whole window that said who fights whom. Nothing else shows it - the army editor
 * answers per LAYER, and the full matrix lives behind the Diplomacy button. So a nation node carries
 * {@link #getEnemies}, the matrix projected onto the nations actually present, which is the same
 * fact the four groups were encoding and is now stated directly instead of relative to somebody.
 */
public class ScenarioRoster {

    private static final ExercitoFacade exercitoFacade = new ExercitoFacade();

    /**
     * Identity-keyed and insertion-ordered, both deliberately.
     *
     * Identity because {@code Nacao} inherits {@code BaseModel.compareTo}, which orders by codigo
     * and would collapse two distinct nations that happen to share one. Insertion order because it
     * is the hex's own order: sorting by strength would be friendlier to read once and useless to
     * read twice, since a row would move every time the player edited a quantity.
     */
    private final Map<Nacao, List<ArmySim>> byNacao = new IdentityHashMap<>();
    private final List<Nacao> order = new ArrayList<>();
    private final Map<Nacao, List<Nacao>> enemies = new IdentityHashMap<>();

    private ScenarioRoster() {
    }

    /**
     * Groups a scenario's armies by the nation that owns them.
     *
     * An army with no nation is not dropped - it gets its own node under a null key, because losing
     * a row is worse than an odd-looking one and {@code ScenarioLoader} forces a stand-in owner
     * precisely so this should not happen.
     */
    public static ScenarioRoster of(CombatScenario scenario) {
        final ScenarioRoster ret = new ScenarioRoster();
        if (scenario == null) {
            return ret;
        }
        for (ArmySim army : scenario.getArmies()) {
            ret.put(army.getNacao(), army);
        }
        final RelationshipMatrix relations = scenario.getRelationships();
        for (Nacao one : ret.order) {
            final List<Nacao> hostile = new ArrayList<>();
            for (Nacao other : ret.order) {
                if (one != other && one != null && other != null
                        && relations.isHostile(one, other)) {
                    hostile.add(other);
                }
            }
            ret.enemies.put(one, hostile);
        }
        return ret;
    }

    private void put(Nacao nacao, ArmySim army) {
        List<ArmySim> armies = byNacao.get(nacao);
        if (armies == null) {
            armies = new ArrayList<>();
            byNacao.put(nacao, armies);
            order.add(nacao);
        }
        armies.add(army);
    }

    /** Every nation with an army here, in the hex's own order. The tree's nodes. */
    public List<Nacao> getNacoes() {
        return Collections.unmodifiableList(order);
    }

    /** This nation's armies, in load order. */
    public List<ArmySim> getArmies(Nacao nacao) {
        final List<ArmySim> ret = byNacao.get(nacao);
        return ret == null ? Collections.<ArmySim>emptyList() : Collections.unmodifiableList(ret);
    }

    /**
     * Who this nation fights, among the nations ON THIS HEX.
     *
     * Scoped to the hex because the node is about this battle: a war with somebody who is not here
     * changes nothing about what happens on this ground, and listing it would pad every row with
     * diplomacy the player did not ask about. Empty is a real and common answer.
     */
    public List<Nacao> getEnemies(Nacao nacao) {
        final List<Nacao> ret = enemies.get(nacao);
        return ret == null ? Collections.<Nacao>emptyList() : Collections.unmodifiableList(ret);
    }

    /** Running troop total for a nation node, as in the wireframe's right-hand column. */
    public int getQtTropas(Nacao nacao) {
        int ret = 0;
        for (ArmySim army : getArmies(nacao)) {
            ret += exercitoFacade.getQtTropasTotal(army);
        }
        return ret;
    }
}
