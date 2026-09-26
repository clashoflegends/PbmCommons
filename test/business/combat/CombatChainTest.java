package business.combat;

import model.Cidade;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Terreno;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The layers share one set of army copies, which is the whole reason this class exists.
 *
 * {@code CombateTmpbm.executaCombates} runs sea, land and city on the same
 * {@code ExercitoControl} objects and re-tests the city AFTER the land battle - "checking city
 * again, attacking army may have lost the previous battle". Four consequences ride on that, and all
 * four were wrong when the city layer was first written against the player's untouched scenario.
 */
public class CombatChainTest extends LandCombatFixture {

    private static Cidade city(Nacao owner, int size, int fortification) {
        final Cidade ret = new Cidade();
        ret.setCodigo("c1");
        ret.setNome("Lannisport");
        ret.setTamanho(size);
        ret.setFortificacao(fortification);
        ret.setLealdade(50);
        if (owner != null) {
            ret.setNacao(owner);
        }
        return ret;
    }

    private static Local hexWithCity(Cidade cidade) {
        final Local ret = hex();
        ret.setCidade(cidade);
        return ret;
    }

    /** An army standing on THIS hex, so the city and the shared attack formula see the same Local. */
    private static ArmySim armyAt(String nome, Nacao nacao, Local local, Pelotao... pelotoes) {
        final ArmySim ret = army(nome, nacao, pelotoes);
        ret.setLocal(local);
        return ret;
    }

    /**
     * THE CONTRACT: the player's own armies come back untouched, however many layers ran.
     *
     * Run has to be repeatable. The city resolver used to fight on the originals, which spent the
     * one-time attack magic on the player's army and zeroed the spinner he had typed into.
     */
    @Test
    public void theChainNeverTouchesThePlayersOwnArmies() {
        final Nacao attacker = nacao("att"), owner = nacao("own");
        final Cidade cidade = city(owner, 3, 2);
        final Local local = hexWithCity(cidade);
        final CombatScenario scenario = new CombatScenario(null, local);
        final ArmySim army = armyAt("Besieger", attacker, local, platoon(troopType("inf", 60, 40, false), 900));
        army.setCombatLevel(CombatLevel.ATTACK_CITY);
        army.setCombateAtaqueOnetime(500);
        scenario.addArmy(army, CombatScenario.Provenance.EXACT);
        scenario.setRelacionamento(attacker, owner, RelationshipMatrix.SWORN_ENEMY);
        scenario.setRelacionamento(owner, attacker, RelationshipMatrix.SWORN_ENEMY);
        final int troopsBefore = army.getPelotoes().get("inf").getQtd();
        final int fortBefore = scenario.getCidade().getFortificacao();

        new CombatChain().resolve(scenario, null);

        assertEquals(500, army.getCombateAtaqueOnetime(),
                "the one-time magic is the player's input and must survive a Run");
        assertEquals(troopsBefore, army.getPelotoes().get("inf").getQtd(),
                "and so must his troop counts");
        assertEquals(fortBefore, scenario.getCidade().getFortificacao(),
                "and the city's fortification, or a second Run starts from a reduced wall");
    }

    /** Twice through the chain gives the same answer, which is the observable form of the above. */
    @Test
    public void runningTheChainTwiceGivesTheSameAnswer() {
        final Nacao attacker = nacao("att"), owner = nacao("own");
        final Local local = hexWithCity(city(owner, 3, 2));
        final CombatScenario scenario = new CombatScenario(null, local);
        final ArmySim army = armyAt("Besieger", attacker, local, platoon(troopType("inf", 60, 40, false), 900));
        army.setCombatLevel(CombatLevel.ATTACK_CITY);
        army.setCombateAtaqueOnetime(500);
        scenario.addArmy(army, CombatScenario.Provenance.EXACT);
        scenario.setRelacionamento(attacker, owner, RelationshipMatrix.SWORN_ENEMY);
        scenario.setRelacionamento(owner, attacker, RelationshipMatrix.SWORN_ENEMY);

        final CombatResult first = new CombatChain().resolve(scenario, null);
        final CombatResult second = new CombatChain().resolve(scenario, null);

        assertNotNull(first.getCityResult());
        assertNotNull(second.getCityResult());
        assertEquals(first.getCityResult().getAttackTotal(),
                second.getCityResult().getAttackTotal(),
                "same inputs, same assault - anything else means a layer edited the setup");
        assertEquals(first.getCityResult().getOutcome(), second.getCityResult().getOutcome());
    }

    /**
     * An army destroyed in the land battle does not then assault the walls.
     *
     * This is the Judge's re-test, and it is the one thing that cannot be bolted on after the fact:
     * only the shared copies know that the army died.
     */
    @Test
    public void anArmyWipedOutOnLandDoesNotAssaultTheCity() {
        final Nacao attacker = nacao("att"), owner = nacao("own");
        final Local local = hexWithCity(city(owner, 3, 2));
        final CombatScenario scenario = new CombatScenario(null, local);
        // tiny, and it is about to meet something enormous that also holds the city
        final ArmySim doomed = armyAt("Doomed", attacker, local, platoon(troopType("inf", 60, 40, false), 1));
        doomed.setCombatLevel(CombatLevel.ATTACK_CITY);
        final ArmySim defender =
                armyAt("Defender", owner, local, platoon(troopType("inf2", 60, 40, false), 50000));
        scenario.addArmy(doomed, CombatScenario.Provenance.EXACT);
        scenario.addArmy(defender, CombatScenario.Provenance.ESTIMATED);
        scenario.setRelacionamento(attacker, owner, RelationshipMatrix.SWORN_ENEMY);
        scenario.setRelacionamento(owner, attacker, RelationshipMatrix.SWORN_ENEMY);

        final CombatResult ret = new CombatChain().resolve(scenario, null);

        assertTrue(ret.getRounds() > 0, "the land battle happened");
        assertTrue(ret.getCityResult().getAttackers().isEmpty(),
                "and the army that died in it is not at the walls: "
                + ret.getCityResult().getAttackers().size() + " attacker(s)");
    }
}
