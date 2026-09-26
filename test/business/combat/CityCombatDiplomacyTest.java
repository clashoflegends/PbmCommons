package business.combat;

import model.Cidade;
import model.Local;
import model.Nacao;
import model.Pelotao;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The city layer reads diplomacy through the scenario's matrix, and reads it in ONE direction.
 *
 * Two separate faults, both verdict-changing, both found by an adversarial review of the first cut:
 *
 * <ol>
 *   <li>the relationship VALUE came off the raw model, so the player's own declarations never
 *       reached the damage and a foreign attacker's empty row read as neutral;</li>
 *   <li>hostility was the OR of both directions, where {@code CombateTmpbm} gates the assault on
 *       {@code exercito.isInimigo(cidade.getNacaoControl())} - the attacker's row alone.</li>
 * </ol>
 */
public class CityCombatDiplomacyTest extends LandCombatFixture {

    private static Cidade city(Nacao owner) {
        final Cidade ret = new Cidade();
        ret.setCodigo("c1");
        ret.setNome("Lannisport");
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

    private static ArmySim besieger(String nome, Nacao nacao, Local local) {
        final Pelotao inf = platoon(troopType("inf", 60, 40, false), 900);
        final ArmySim ret = army(nome, nacao, inf);
        ret.setLocal(local);
        ret.setCombatLevel(CombatLevel.ATTACK_CITY);
        return ret;
    }

    /**
     * Declaring war in the Diplomacy panel must RAISE the assault.
     *
     * Sworn enemy is -25 on {@code dificuldadeBonus}, so {@code modRelacionamento} goes 100 -> 125
     * and the attack gains a quarter of {@code forcaBasica}. Reading the raw model instead of the
     * matrix threw the declaration away and left it at 100.
     */
    @Test
    public void aDeclarationOfWarReachesTheDamage() {
        final Nacao attacker = nacao("att"), owner = nacao("own");
        final Local local = hexWithCity(owner);
        final CombatScenario scenario = new CombatScenario(null, local);
        scenario.addArmy(besieger("Besieger", attacker, local), CombatScenario.Provenance.EXACT);
        // nothing readable either way: the nations carry no relationship rows at all
        scenario.setRelacionamento(attacker, owner, RelationshipMatrix.ENEMY);
        final long atEnemy = new CombatChain().resolve(scenario, null)
                .getCityResult().getAttackTotal();

        scenario.setRelacionamento(attacker, owner, RelationshipMatrix.SWORN_ENEMY);
        final long atSworn = new CombatChain().resolve(scenario, null)
                .getCityResult().getAttackTotal();

        assertTrue(atSworn > atEnemy,
                "sworn enemy must hit harder than enemy: " + atSworn + " vs " + atEnemy);
    }

    /**
     * The city never initiates: its owner hating an army does not make that army assault it.
     *
     * The Judge reads the ATTACKER'S row. With the OR, a neutral army parked on a hostile city's
     * hex was reported storming the walls.
     */
    @Test
    public void theCityOwnersHatredDoesNotDragANeutralArmyIntoAnAssault() {
        final Nacao passerby = nacao("passer"), owner = nacao("own");
        final Local local = hexWithCity(owner);
        final CombatScenario scenario = new CombatScenario(null, local);
        scenario.addArmy(besieger("Passerby", passerby, local), CombatScenario.Provenance.EXACT);
        // the CITY hates him; he does not hate the city
        scenario.setRelacionamento(owner, passerby, RelationshipMatrix.SWORN_ENEMY);
        scenario.setRelacionamento(passerby, owner, RelationshipMatrix.NEUTRAL);

        final CombatResult ret = new CombatChain().resolve(scenario, null);

        assertTrue(ret.getCityResult().getAttackers().isEmpty(),
                "he is not assaulting anything");
        assertEquals(CityCombatResolver.CityOutcome.NO_ASSAULT,
                ret.getCityResult().getOutcome());
    }

    /** And the other direction does: his own declaration puts him at the walls. */
    @Test
    public void hisOwnDeclarationPutsHimAtTheWalls() {
        final Nacao attacker = nacao("att"), owner = nacao("own");
        final Local local = hexWithCity(owner);
        final CombatScenario scenario = new CombatScenario(null, local);
        scenario.addArmy(besieger("Besieger", attacker, local), CombatScenario.Provenance.EXACT);
        scenario.setRelacionamento(attacker, owner, RelationshipMatrix.SWORN_ENEMY);
        scenario.setRelacionamento(owner, attacker, RelationshipMatrix.NEUTRAL);

        final CombatResult ret = new CombatChain().resolve(scenario, null);

        assertEquals(1, ret.getCityResult().getAttackers().size());
    }

    /** The same rule on the participation side, so the roster badge agrees with the resolver. */
    @Test
    public void theRosterBadgeAgreesWithTheResolverOnDirection() {
        final Nacao passerby = nacao("passer"), owner = nacao("own");
        final Local local = hexWithCity(owner);
        final CombatScenario scenario = new CombatScenario(null, local);
        final ArmySim army = besieger("Passerby", passerby, local);
        scenario.addArmy(army, CombatScenario.Provenance.EXACT);
        scenario.setRelacionamento(owner, passerby, RelationshipMatrix.SWORN_ENEMY);
        scenario.setRelacionamento(passerby, owner, RelationshipMatrix.NEUTRAL);

        assertFalse(scenario.getParticipation().get(army).isIn(CombatLayer.CITY),
                "the badge must not show a city fight the resolver will not run");
    }
}
