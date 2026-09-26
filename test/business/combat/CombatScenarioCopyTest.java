package business.combat;

import model.Cidade;
import model.Local;
import model.Nacao;
import model.Pelotao;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A cloned scenario carries every edit and shares none of them.
 *
 * The feature is a FORK: the player edits, runs, clones, edits the clone, runs again, and compares
 * the two windows. That only works if the copy starts where he left off AND the two stop being the
 * same thing the moment it is made. Both halves are failure modes with no symptom: a copy that drops
 * an edit silently answers a different question, and one that shares state silently changes the
 * window he is comparing against.
 */
public class CombatScenarioCopyTest extends LandCombatFixture {

    private static Cidade city(Nacao owner) {
        final Cidade ret = new Cidade();
        ret.setCodigo("c1");
        ret.setNome("Riverrun");
        ret.setTamanho(3);
        ret.setFortificacao(2);
        ret.setLealdade(50);
        ret.setNacao(owner);
        return ret;
    }

    private static Local hexWithCity(Nacao owner) {
        final Local ret = hex();
        ret.setCidade(city(owner));
        return ret;
    }

    /** A scenario with something edited in every corner a player can reach. */
    private static CombatScenario edited(Nacao mine, Nacao foe, Local local) {
        final CombatScenario ret = new CombatScenario(null, local);
        final ArmySim army = army("Joron", mine, platoon(troopType("inf", 60, 40, false), 900));
        army.setLocal(local);
        army.setTatica(3);
        army.setMoral(42);
        army.setCombatLevel(CombatLevel.RAZE_CITY);
        army.setBonusAttack(500);
        ret.addArmy(army, CombatScenario.Provenance.ESTIMATED);
        ret.setRelacionamento(mine, foe, RelationshipMatrix.SWORN_ENEMY);
        ret.setTerreno(FOREST);
        return ret;
    }

    private static ArmySim only(CombatScenario scenario) {
        return scenario.getArmies().get(0);
    }

    /** Every edit the player can make survives the fork. */
    @Test
    public void theCopyCarriesEveryEdit() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final CombatScenario original = edited(mine, foe, hexWithCity(foe));

        final CombatScenario clone = original.copy();
        final ArmySim army = only(clone);

        assertEquals(3, army.getTatica(), "tactic");
        assertEquals(42, army.getMoral(), "morale");
        assertEquals(CombatLevel.RAZE_CITY, army.getCombatLevel(), "combat level");
        assertEquals(500, army.getAttackBonus(), "attack bonus");
        assertEquals(900, army.getPelotoes().get("inf").getQtd(), "platoon quantity");
        assertEquals(FOREST, clone.getTerreno(), "the terrain override, not the hex's");
        assertTrue(clone.getMatrix().isInimigo(army, army) == false, "sanity");
        assertEquals(RelationshipMatrix.SWORN_ENEMY,
                clone.getRelationships().getValor(mine, foe), "the declared relationship");
    }

    /**
     * Provenance follows the copies.
     *
     * Keyed by object identity, so this is the one thing that cannot survive by accident. It draws
     * the "(?)" on an unknown morale, prints EXACT or ESTIMATED under the army, and is counted by
     * the unknown-morale disclosure - all of which would go quiet rather than wrong.
     */
    @Test
    public void provenanceIsReKeyedOntoTheCopies() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final CombatScenario original = edited(mine, foe, hexWithCity(foe));
        only(original).setMoral(0);         // so isMoraleUnknown has something to answer about

        final CombatScenario clone = original.copy();
        final ArmySim army = only(clone);

        assertEquals(CombatScenario.Provenance.ESTIMATED, clone.getProvenance(army),
                "an army that was a guess is still a guess in the fork");
        assertTrue(clone.isMoraleUnknown(army),
                "and the '(?)' marker survives, which is what the player reads");
        assertEquals(CombatScenario.Provenance.ESTIMATED,
                clone.getProvenance(army.getPelotoes().get("inf")),
                "platoon provenance too: it decides whose number the table is showing");
    }

    /** Editing the clone must not reach back into the window it was forked from. */
    @Test
    public void editingTheCopyLeavesTheOriginalAlone() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final CombatScenario original = edited(mine, foe, hexWithCity(foe));
        final CombatScenario clone = original.copy();

        only(clone).setTatica(5);
        only(clone).setMoral(99);
        only(clone).getPelotoes().get("inf").setQtd(1);
        clone.setTerreno(PLAIN);
        clone.setRelacionamento(mine, foe, RelationshipMatrix.ALLY);

        assertEquals(3, only(original).getTatica(), "tactic");
        assertEquals(42, only(original).getMoral(), "morale");
        assertEquals(900, only(original).getPelotoes().get("inf").getQtd(), "platoon quantity");
        assertEquals(FOREST, original.getTerreno(), "terrain");
        assertEquals(RelationshipMatrix.SWORN_ENEMY,
                original.getRelationships().getValor(mine, foe),
                "a declaration in one window is not a declaration in the other");
    }

    /**
     * The city is copied, not borrowed.
     *
     * Size, fortification and loyalty are all editable, and the scenario's city is ALREADY a clone
     * of the world's. Forking without cloning again would make two windows share one set of walls.
     */
    @Test
    public void theCityIsNotSharedWithTheOriginal() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final CombatScenario original = edited(mine, foe, hexWithCity(foe));
        original.getCidade().setLealdade(77);

        final CombatScenario clone = original.copy();

        assertNotSame(original.getCidade(), clone.getCidade(), "two windows, two sets of walls");
        assertEquals(77, clone.getCidade().getLealdade(), "the edited loyalty came across");
        clone.getCidade().setFortificacao(5);
        assertEquals(2, original.getCidade().getFortificacao(),
                "and raising the clone's walls does not raise the original's");
    }

    /** The armies and their platoons are new objects, or none of the above can hold. */
    @Test
    public void nothingMutableIsSharedByReference() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final CombatScenario original = edited(mine, foe, hexWithCity(foe));
        final CombatScenario clone = original.copy();

        assertNotSame(only(original), only(clone), "the army");
        assertNotSame(only(original).getPelotoes().get("inf"),
                only(clone).getPelotoes().get("inf"), "the platoon");
        // the NATION is deliberately shared: it is the world's, read-only here, and the
        // relationship maps are keyed on its identity
        assertEquals(only(original).getNacao(), only(clone).getNacao(),
                "the nation is the world's and stays shared");
    }

    /** A fork of a hex with no city is a fork, not a crash. */
    @Test
    public void aScenarioWithNoCityCopiesCleanly() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final CombatScenario clone = edited(mine, foe, hex()).copy();

        assertFalse(clone.isCityParticipates(), "no city to take part");
        assertEquals(1, clone.getArmies().size());
    }
}
