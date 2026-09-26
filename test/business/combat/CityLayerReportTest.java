package business.combat;

import model.Cidade;
import model.Local;
import model.Nacao;
import model.Pelotao;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The four reporting gaps that made a two-layer battle unreadable.
 *
 * A battle is one event across up to three layers, and every one of these was a case where the
 * report described a different battle from the one that happened.
 */
public class CityLayerReportTest extends LandCombatFixture {

    private static Cidade city(Nacao owner) {
        final Cidade ret = new Cidade();
        ret.setCodigo("c1");
        ret.setNome("Lannisport");
        ret.setTamanho(4);
        ret.setFortificacao(3);
        ret.setLealdade(50);
        ret.setNacao(owner);
        return ret;
    }

    private static Local hexWithCity(Nacao owner) {
        final Local ret = hex();
        ret.setCidade(city(owner));
        return ret;
    }

    private static ArmySim besieger(String nome, Nacao nacao, Local local, int qtd) {
        final ArmySim ret = army(nome, nacao, platoon(troopType("inf", 60, 40, false), qtd));
        ret.setLocal(local);
        ret.setCombatLevel(CombatLevel.ATTACK_CITY);
        return ret;
    }

    private static CombatScenario assaultOnly(Nacao attacker, Nacao owner, Local local) {
        final CombatScenario ret = new CombatScenario(null, local);
        ret.setRelacionamento(attacker, owner, RelationshipMatrix.SWORN_ENEMY);
        return ret;
    }

    /**
     * Run must be reachable for ONE besieger against an ungarrisoned city.
     *
     * The matrix is built from ARMY PAIRS, and a city has no army - so the single case the city
     * layer exists for produced no hostile pair and the gate said "no two armies here are hostile".
     * True of the armies, and beside the point.
     */
    @Test
    public void oneBesiegerAgainstAnEmptyCityCanRun() {
        final Nacao attacker = nacao("att"), owner = nacao("own");
        final Local local = hexWithCity(owner);
        final CombatScenario scenario = assaultOnly(attacker, owner, local);
        scenario.addArmy(besieger("Besieger", attacker, local, 900),
                CombatScenario.Provenance.EXACT);

        assertTrue(scenario.hasCombat(), "the city is the other side");
        assertEquals(RunGate.READY, scenario.getRunGate());
    }

    /** A city assault is a round, and the layer must not report itself unfought. */
    @Test
    public void aPureCityAssaultReportsItsOwnRound() {
        final Nacao attacker = nacao("att"), owner = nacao("own");
        final Local local = hexWithCity(owner);
        final CombatScenario scenario = assaultOnly(attacker, owner, local);
        scenario.addArmy(besieger("Besieger", attacker, local, 900),
                CombatScenario.Provenance.EXACT);

        final CombatResult ret = new CombatChain().resolve(scenario, null);

        assertEquals(1, ret.getRounds(CombatLayer.CITY), "one round, always");
        assertTrue(LayerReport.of(scenario, ret, CombatLayer.CITY).isFought(),
                "a layer that just stormed a city did not 'not happen'");
        assertEquals(0, ret.getRounds(CombatLayer.ARMY), "and no land battle was fought");
    }

    /**
     * City casualties belong to the CITY table and must not be subtracted from the land one.
     *
     * Each table is a running total from its own start, so an unstamped loss lands in every table -
     * the land table showed an army melting from an assault that happened after it had ended.
     */
    @Test
    public void cityCasualtiesStayOutOfTheLandTable() {
        final Nacao attacker = nacao("att"), owner = nacao("own");
        final Local local = hexWithCity(owner);
        final CombatScenario scenario = assaultOnly(attacker, owner, local);
        final ArmySim besieger = besieger("Besieger", attacker, local, 900);
        // a defender, so the LAND layer runs too and the two tables can disagree
        final ArmySim garrison =
                army("Garrison", owner, platoon(troopType("def", 50, 40, false), 300));
        garrison.setLocal(local);
        scenario.addArmy(besieger, CombatScenario.Provenance.EXACT);
        scenario.addArmy(garrison, CombatScenario.Provenance.ESTIMATED);
        scenario.setRelacionamento(owner, attacker, RelationshipMatrix.SWORN_ENEMY);

        final CombatResult ret = new CombatChain().resolve(scenario, null);

        boolean sawCityLoss = false, sawLandLoss = false;
        for (CombatResult.RoundLoss loss : ret.getRoundLosses()) {
            sawCityLoss |= loss.getLayer() == CombatLayer.CITY;
            sawLandLoss |= loss.getLayer() == CombatLayer.ARMY;
        }
        assertTrue(sawLandLoss, "the land battle killed somebody");
        assertTrue(sawCityLoss, "and so did the walls");

        final LayerReport land = LayerReport.of(scenario, ret, CombatLayer.ARMY);
        final LayerReport cityTable = LayerReport.of(scenario, ret, CombatLayer.CITY);
        assertNotEquals(0, land.getRounds());
        assertEquals(1, cityTable.getRounds());
        // the land table's LAST column is what came out of the land battle, and the city table's
        // start is what went into the assault - the same number, which is the carry-over made
        // auditable by eye
        assertEquals(land.getRemaining(besieger, land.getRounds()),
                cityTable.getRemaining(besieger, 0),
                "the city layer starts where the land layer finished");
    }

    /**
     * The city table's last column has to be what the assault LEFT, not what went into it.
     *
     * A round number belongs to the resolver, and the two layers do not start counting in the same
     * place: the land battle opens at round 0, the city assault is round 1. The table subtracted
     * from "the round after this one" without allowing for that, so the city's only column fell one
     * off the end of a one-round row and every casualty at the walls was dropped. On screen: a
     * table reading 1,300 into the assault and 1,300 out of it, directly beneath a summary saying
     * the same army had just lost 593.
     */
    @Test
    public void theCityTableEndsWhereTheCasualtySummarySays() {
        final Nacao attacker = nacao("att"), owner = nacao("own");
        final Local local = hexWithCity(owner);
        final CombatScenario scenario = assaultOnly(attacker, owner, local);
        final ArmySim besieger = besieger("Besieger", attacker, local, 900);
        scenario.addArmy(besieger, CombatScenario.Provenance.EXACT);

        final CombatResult ret = new CombatChain().resolve(scenario, null);
        final LayerReport city = LayerReport.of(scenario, ret, CombatLayer.CITY);

        int lost = 0;
        for (CombatResult.RoundLoss loss : ret.getRoundLosses()) {
            if (loss.getLayer() == CombatLayer.CITY) {
                lost += loss.getLost();
            }
        }
        assertTrue(lost > 0, "the walls killed somebody, or this proves nothing");
        assertEquals(city.getRemaining(besieger, 0) - lost,
                city.getRemaining(besieger, city.getRounds()),
                "start minus the losses IS the last column");
        // and the platoon table, which reads the survivor snapshot, has to agree with both
        int after = 0;
        for (Pelotao pelotao : besieger.getPelotoes().values()) {
            after += ret.getAfter(pelotao);
        }
        assertEquals(after, city.getRemaining(besieger, city.getRounds()),
                "the two tables on one screen must not contradict each other");
    }

    /** Nobody may read as "city layer not simulated" once it has been. */
    @Test
    public void everyArmyGetsACityVerdictOnceTheLayerRuns() {
        final Nacao attacker = nacao("att"), owner = nacao("own");
        final Local local = hexWithCity(owner);
        final CombatScenario scenario = assaultOnly(attacker, owner, local);
        final ArmySim besieger = besieger("Besieger", attacker, local, 900);
        final ArmySim bystander =
                army("Bystander", owner, platoon(troopType("idle", 50, 40, false), 10));
        bystander.setLocal(local);
        scenario.addArmy(besieger, CombatScenario.Provenance.EXACT);
        scenario.addArmy(bystander, CombatScenario.Provenance.ESTIMATED);

        final CombatResult ret = new CombatChain().resolve(scenario, null);

        assertEquals(CombatResult.Outcome.DID_NOT_FIGHT,
                ret.getOutcome(bystander, CombatLayer.CITY),
                "a null here means 'not simulated', which would be a lie");
        assertNotEquals(null, ret.getOutcome(besieger, CombatLayer.CITY));
    }

    /** The chain says what it could not do, even when no land battle ran to say it for it. */
    @Test
    public void theChainDisclosesTheMissingSeaLayer() {
        final Nacao attacker = nacao("att"), owner = nacao("own");
        final Local local = hexWithCity(owner);
        final CombatScenario scenario = assaultOnly(attacker, owner, local);
        scenario.addArmy(besieger("Besieger", attacker, local, 900),
                CombatScenario.Provenance.EXACT);

        assertTrue(new CombatChain().resolve(scenario, null).getNotes()
                .contains("BATTLESIM.RESULT.NAVYNOTSIMULATED"),
                "a result that omits what it skipped looks complete");
    }
}
