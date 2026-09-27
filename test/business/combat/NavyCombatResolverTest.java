package business.combat;

import java.util.SortedMap;
import java.util.TreeMap;
import model.Habilidade;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sea layer, and the four questions it answers that no other layer does.
 *
 * The known answer at 866 t3 hex 2442 pins the arithmetic, but it needs the save archive and CI
 * skips it. These do not: they are the rules that decide whether the numbers are even about the
 * right things - which platoons the battle puts at risk, what happens to the cargo when the hulls
 * are gone, and who is allowed to fight whom.
 */
public class NavyCombatResolverTest extends LandCombatFixture {

    /**
     * ONE instance, because a troop type's attack and defence are keyed BY TERRAIN.
     *
     * A second water object would miss every lookup and the fleet would fight with an attack and a
     * defence of zero - which reads on screen as "no battle happened" rather than as a broken
     * fixture, and cost twenty minutes the first time.
     */
    private static final Terreno WATER = water();

    private static Terreno water() {
        final Terreno ret = new Terreno();
        ret.setCodigo("O");
        ret.setNome("High seas");
        ret.setAgua(true);
        ret.setAncoravel(false);
        return ret;
    }

    /** The fixture keys attack and defence on PLAIN and FOREST; ships also need open water. */
    private static void addWater(TipoTropa tipo, int ataque, int defesa) {
        tipo.getAtaqueTerreno().put(WATER, ataque);
        tipo.getDefesaTerreno().put(WATER, defesa);
        tipo.getMovimentoTerreno().put(WATER, 5);
    }

    /** A ship type carrying {@code ;TTN;}, and optionally the {@code ;TTT;} cargo capacity. */
    private static TipoTropa shipWith(String codigo, int capacity) {
        final TipoTropa ret = troopType(codigo, 60, 40, false);
        addWater(ret, 60, 40);
        final Habilidade naval = new Habilidade();
        naval.setCodigo(";TTN;");
        naval.setNome(";TTN;");
        ret.addHabilidade(naval);
        if (capacity > 0) {
            final Habilidade transport = new Habilidade();
            transport.setCodigo(";TTT;");
            transport.setNome(";TTT;");
            transport.setValor(capacity);
            ret.addHabilidade(transport);
        }
        return ret;
    }

    private static Local hexOf(Terreno terreno) {
        final Local ret = new Local();
        ret.setCodigo("2442");
        ret.setCoordenadas("2442");
        ret.setTerreno(terreno);
        return ret;
    }

    private static ArmySim fleet(String nome, Nacao nacao, Local local, Pelotao... platoons) {
        final ArmySim ret = army(nome, nacao, platoons);
        ret.setLocal(local);
        ret.setTerreno(local.getTerreno());
        ret.setCombatLevel(CombatLevel.ATTACK_ARMY);
        return ret;
    }

    private static CombatScenario atSea(Local local, Nacao one, Nacao two) {
        final CombatScenario ret = new CombatScenario(null, local);
        ret.setTerreno(local.getTerreno());
        ret.setRelacionamento(one, two, RelationshipMatrix.SWORN_ENEMY);
        ret.setRelacionamento(two, one, RelationshipMatrix.SWORN_ENEMY);
        return ret;
    }

    private static int lost(CombatResult result, String who, CombatLayer layer) {
        int ret = 0;
        for (CombatResult.RoundLoss loss : result.getRoundLosses()) {
            if (loss.getLayer() == layer && loss.getArmy().getNome().contains(who)) {
                ret += loss.getLost();
            }
        }
        return ret;
    }

    /**
     * The sea battle sinks HULLS, and the round it fights is round 1.
     *
     * Both halves matter. The land battle opens at round 0 with a first-strike round in which
     * almost nothing happens; the naval counter opens at 1 and swings immediately, which is why
     * the flat additions land in the first round at sea and the second on land.
     */
    @Test
    public void theSeaBattleSinksShipsFromRoundOne() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final Local local = hexOf(WATER);
        final CombatScenario scenario = atSea(local, mine, foe);
        scenario.addArmy(fleet("Big", mine, local, platoon(shipWith("ng", 0), 400)),
                CombatScenario.Provenance.EXACT);
        scenario.addArmy(fleet("Small", foe, local, platoon(shipWith("ng2", 0), 5)),
                CombatScenario.Provenance.EXACT);

        final CombatResult ret = new CombatChain().resolve(scenario, null);

        assertTrue(ret.getRounds(CombatLayer.NAVY) > 0, "they fought");
        assertTrue(lost(ret, "Small", CombatLayer.NAVY) > 0, "and hulls went down");
        for (CombatResult.RoundLoss loss : ret.getRoundLosses()) {
            if (loss.getLayer() == CombatLayer.NAVY) {
                assertTrue(loss.getRound() >= NavyCombatResolver.FIRST_ROUND,
                        "no round 0 at sea: " + loss.getRound());
            }
        }
    }

    /** Lose every hull in open water and the cargo goes down with them. No die, no doubt. */
    @Test
    public void everyHullLostInOpenWaterDrownsTheCargo() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final Local local = hexOf(WATER);
        final CombatScenario scenario = atSea(local, mine, foe);
        final ArmySim doomed = fleet("Doomed", foe, local,
                platoon(shipWith("ng2", 500), 2), platoon(troopType("inf", 60, 40, false), 400));
        scenario.addArmy(fleet("Armada", mine, local, platoon(shipWith("ng", 0), 900)),
                CombatScenario.Provenance.EXACT);
        scenario.addArmy(doomed, CombatScenario.Provenance.EXACT);

        final CombatResult ret = new CombatChain().resolve(scenario, null);

        assertTrue(ret.getNotes().contains("BATTLESIM.RESULT.DROWNED"), "they drowned");
        assertFalse(ret.getNotes().contains("BATTLESIM.RESULT.DROWNINGESTIMATED"),
                "nothing is estimated about open water: everyone drowns");
        // Asked of the RESULT, not of the army: the sim fights on copies and the player's own
        // platoons are never touched, so the doomed army still reads full strength on the screen
        // behind the dialog. That is the contract, not a leak.
        int after = 0;
        for (Pelotao pelotao : doomed.getPelotoes().values()) {
            after += ret.getAfter(pelotao);
        }
        assertEquals(0, after, "nobody is left");
        assertEquals(400, doomed.getPelotoes().get("inf").getQtd(),
                "and the player's own platoon is untouched");
    }

    /**
     * Ashore, the cargo drowns at the agreed flat rate and the result says the figure is an
     * estimate.
     *
     * The Judge still rolls 11% to 25% until T-817 replaces the die on both sides with a function
     * of the commander's skill, so the number here is the midpoint rather than the turn's. That is
     * John's call (T-816) and the note is what stops it reading as a promise. See KI-056.
     *
     * KI-010 is the trap underneath and it was live here for an afternoon: capacity has to be read
     * BEFORE the hulls sink, because afterwards it reads zero, and zero is also how an escort that
     * was carrying nobody reads. Taking it afterwards silently skipped the drowning of exactly the
     * army the rule is about.
     */
    @Test
    public void ashoreTheCargoDrownsAtTheAgreedRate() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final Local local = hexOf(PLAIN);       // anchorable, not water
        final CombatScenario scenario = atSea(local, mine, foe);
        final ArmySim carrier = fleet("Carrier", foe, local,
                platoon(shipWith("ng2", 500), 2), platoon(troopType("inf", 60, 40, false), 400));
        carrier.setCombatLevel(CombatLevel.DEFEND_ONLY);        // so it stays aboard to be sunk
        carrier.setComandante(50);                              // T-817: 50 is the 18% anchor
        scenario.addArmy(fleet("Armada", mine, local, platoon(shipWith("ng", 0), 900)),
                CombatScenario.Provenance.EXACT);
        scenario.addArmy(carrier, CombatScenario.Provenance.EXACT);

        final CombatResult ret = new CombatChain().resolve(scenario, null);

        assertTrue(ret.getNotes().contains("BATTLESIM.RESULT.DROWNINGSKILL"),
                "the rate is named, because the player chose it when he chose the commander");
        assertFalse(ret.getNotes().contains("BATTLESIM.RESULT.DROWNED"),
                "they are not in open water");
        assertEquals(72, lost(ret, "Carrier", CombatLayer.NAVY) - 2,
                "18% of the 400 infantry, and the 2 hulls are the rest of the layer's losses");
    }

    /**
     * A fleet that was carrying only PART of its army drowns only the part that was aboard.
     *
     * This is the embarked-versus-not distinction, and the Judge does not decide it with
     * {@code isEsquadraEmbarcada()} even though its own comment says so: {@code doAfogamento}
     * branches on {@code capacityBefore < burdenBefore}, which is the same predicate spelled out.
     * The consequence is a BOUND - the total drowned WEIGHT stops at the cargo capacity, because
     * the rest of the army was standing on the beach the whole time.
     *
     * Here: one transport of capacity 50 carrying 400 infantry, each weighing 1. The flat rate
     * would drown 72; the bound allows 50. Asserting the bound bites is the point - a test where
     * 18% happens to fit inside the capacity would pass with no bound at all.
     */
    @Test
    public void aPartlyEmbarkedArmyDrownsOnlyWhatWasAboard() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final Local local = hexOf(PLAIN);
        final CombatScenario scenario = atSea(local, mine, foe);
        final ArmySim carrier = fleet("Carrier", foe, local,
                platoon(shipWith("ng2", 50), 1), platoon(troopType("inf", 60, 40, false), 400));
        carrier.setCombatLevel(CombatLevel.DEFEND_ONLY);
        carrier.setComandante(50);                              // 18%, so the bound is what bites
        scenario.addArmy(fleet("Armada", mine, local, platoon(shipWith("ng", 0), 900)),
                CombatScenario.Provenance.EXACT);
        scenario.addArmy(carrier, CombatScenario.Provenance.EXACT);

        final CombatResult ret = new CombatChain().resolve(scenario, null);

        final int drowned = lost(ret, "Carrier", CombatLayer.NAVY) - 1;   // minus the one hull
        assertEquals(50, drowned, "bounded by what the transport could ever have been carrying");
        assertTrue(drowned < 72,
                "and that is strictly less than the unbounded rate, or the bound did nothing");
    }

    /**
     * Fully embarked, so there is no bound and every platoon loses the flat rate.
     *
     * The Judge's third branch iterates {@code pelotoes.values()} with no capacity test at all -
     * the whole army was at sea, so there is nothing to hold back.
     */
    @Test
    public void aFullyEmbarkedArmyDrownsTheFlatRateFromEveryPlatoon() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final Local local = hexOf(PLAIN);
        final CombatScenario scenario = atSea(local, mine, foe);
        // capacity 2 x 500 = 1,000 against a burden of 300 + 200 = 500: comfortably embarked
        final ArmySim carrier = fleet("Carrier", foe, local,
                platoon(shipWith("ng2", 500), 2),
                platoon(troopType("inf", 60, 40, false), 300),
                platoon(troopType("bow", 55, 35, false), 200));
        carrier.setCombatLevel(CombatLevel.DEFEND_ONLY);
        carrier.setComandante(50);
        scenario.addArmy(fleet("Armada", mine, local, platoon(shipWith("ng", 0), 900)),
                CombatScenario.Provenance.EXACT);
        scenario.addArmy(carrier, CombatScenario.Provenance.EXACT);

        final CombatResult ret = new CombatChain().resolve(scenario, null);

        assertEquals(54 + 36 + 2, lost(ret, "Carrier", CombatLayer.NAVY),
                "both platoons at 18%, plus the two hulls");
    }

    /**
     * The same fleet, the same battle, a different commander - and a different number of men reach
     * the beach. T-817.
     *
     * This is the one test that would still pass if the rate were a constant, so it is written to
     * fail in that case: it runs the identical scenario twice and only the commander changes. A
     * garrison has no commander at all and takes the floor, which is John's call rather than a
     * side effect of the clamp - see {@code DrowningRuleTest}.
     */
    @Test
    public void whoCommandsTheFleetDecidesHowManyDrown() {
        assertEquals(100, drownedUnderCommander(0), "no commander: the 25% floor");
        assertEquals(72, drownedUnderCommander(50), "skill 50: 18%");
        assertEquals(44, drownedUnderCommander(100), "skill 100: the 11% cap");
    }

    /** One transport, 400 infantry, every hull sunk ashore. Only the commander varies. */
    private int drownedUnderCommander(int skill) {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final Local local = hexOf(PLAIN);
        final CombatScenario scenario = atSea(local, mine, foe);
        final ArmySim carrier = fleet("Carrier", foe, local,
                platoon(shipWith("ng2", 500), 2), platoon(troopType("inf", 60, 40, false), 400));
        carrier.setCombatLevel(CombatLevel.DEFEND_ONLY);
        carrier.setComandante(skill);
        scenario.addArmy(fleet("Armada", mine, local, platoon(shipWith("ng", 0), 900)),
                CombatScenario.Provenance.EXACT);
        scenario.addArmy(carrier, CombatScenario.Provenance.EXACT);
        final CombatResult ret = new CombatChain().resolve(scenario, null);
        return lost(ret, "Carrier", CombatLayer.NAVY) - 2;     // minus the two hulls
    }

    /** An escort that was carrying nobody loses its hulls and nobody drowns. */
    @Test
    public void anEscortCarryingNobodyLosesOnlyItsHulls() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final Local local = hexOf(PLAIN);
        final CombatScenario scenario = atSea(local, mine, foe);
        final ArmySim escort = fleet("Escort", foe, local, platoon(shipWith("ng2", 0), 2));
        escort.setCombatLevel(CombatLevel.DEFEND_ONLY);
        scenario.addArmy(fleet("Armada", mine, local, platoon(shipWith("ng", 0), 900)),
                CombatScenario.Provenance.EXACT);
        scenario.addArmy(escort, CombatScenario.Provenance.EXACT);

        final CombatResult ret = new CombatChain().resolve(scenario, null);

        assertFalse(ret.getNotes().contains("BATTLESIM.RESULT.DROWNINGESTIMATED"),
                "there was nobody aboard to drown");
    }

    /**
     * A land army cannot reach a fleet that has not landed, however much it wants to.
     *
     * {@code temCombateTerra} asks this from the ATTACKER's side only, and asking it symmetrically
     * fought a land battle the Judge did not - at 866 t3 hex 2442, for 2,000 casualties.
     */
    @Test
    public void anArmyAshoreCannotReachAFleetStillAboard() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final Local local = hexOf(PLAIN);
        final CombatScenario scenario = atSea(local, mine, foe);
        final ArmySim ashore = army("Ashore", mine, platoon(troopType("inf", 60, 40, false), 900));
        ashore.setLocal(local);
        ashore.setTerreno(local.getTerreno());
        ashore.setCombatLevel(CombatLevel.ATTACK_ARMY);
        // fully embarked and NOT attacking, so it neither lands nor is landed on
        final ArmySim afloat = fleet("Afloat", foe, local,
                platoon(shipWith("ng2", 5000), 20), platoon(troopType("inf2", 60, 40, false), 10));
        afloat.setCombatLevel(CombatLevel.DEFEND_ONLY);
        scenario.addArmy(ashore, CombatScenario.Provenance.EXACT);
        scenario.addArmy(afloat, CombatScenario.Provenance.EXACT);

        final CombatResult ret = new CombatChain().resolve(scenario, null);

        assertEquals(0, ret.getRounds(CombatLayer.ARMY),
                "the Judge ignores this pair and sends the player a message about it");
    }

    /**
     * A fleet that anchored for a LAND battle keeps its ships out of the casualty summary.
     *
     * They were on the beach, not in danger, and adding them to the before and after would move
     * the percentage in the cost line without a single hull having been at risk.
     */
    @Test
    public void anchoredShipsAreNotInTheCasualtyTotals() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final Local local = hexOf(PLAIN);
        final CombatScenario scenario = atSea(local, mine, foe);
        final Pelotao boats = platoon(shipWith("ng", 0), 40);
        final ArmySim lander = fleet("Lander", mine, local, boats,
                platoon(troopType("inf", 60, 40, false), 900));
        final ArmySim defender = army("Defender", foe,
                platoon(troopType("inf2", 50, 40, false), 900));
        defender.setLocal(local);
        defender.setTerreno(local.getTerreno());
        scenario.addArmy(lander, CombatScenario.Provenance.EXACT);
        scenario.addArmy(defender, CombatScenario.Provenance.EXACT);

        final CombatResult ret = new CombatChain().resolve(scenario, null);

        assertTrue(ret.getRounds(CombatLayer.ARMY) > 0, "they fought ashore");
        assertFalse(ret.has(boats), "the boats never entered the battle, so they are not in it");
        assertEquals(40, boats.getQtd(), "and the player's own platoon is untouched");
    }

    /** The sea table counts hulls; the land table counts the troops that got ashore. */
    @Test
    public void eachLayerCountsItsOwnPlatoons() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final Local local = hexOf(WATER);
        final CombatScenario scenario = atSea(local, mine, foe);
        final ArmySim big = fleet("Big", mine, local, platoon(shipWith("ng", 0), 400));
        final ArmySim small = fleet("Small", foe, local,
                platoon(shipWith("ng2", 500), 6), platoon(troopType("inf", 60, 40, false), 100));
        scenario.addArmy(big, CombatScenario.Provenance.EXACT);
        scenario.addArmy(small, CombatScenario.Provenance.EXACT);

        final CombatResult ret = new CombatChain().resolve(scenario, null);
        final LayerReport sea = LayerReport.of(scenario, ret, CombatLayer.NAVY);

        assertEquals(400, sea.getRemaining(big, 0), "hulls, and only hulls, enter the sea table");
        assertEquals(6, sea.getRemaining(small, 0), "the 100 infantry are cargo, not combatants");
    }

}
