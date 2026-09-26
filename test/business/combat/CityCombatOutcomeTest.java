package business.combat;

import model.Cidade;
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
 * The three remaining fidelity gaps in the city layer: where a fleet may land, whether anybody is
 * left to hold the city, and whether the walls actually kill anyone.
 */
public class CityCombatOutcomeTest extends LandCombatFixture {

    /** Non-anchorable ground, so only the docks clause can let a fleet ashore. */
    private static Terreno inland() {
        final Terreno ret = new Terreno();
        ret.setCodigo("M");
        ret.setNome("Mountain");
        ret.setAncoravel(false);
        return ret;
    }

    private static Cidade city(Nacao owner, int size, boolean docks) {
        final Cidade ret = new Cidade();
        ret.setCodigo("c1");
        ret.setNome("Lannisport");
        ret.setTamanho(size);
        ret.setFortificacao(1);
        ret.setLealdade(50);
        ret.setNacao(owner);
        // isDocasPorto is getDocas() > 0, an int on the city - not a habilidade
        ret.setDocas(docks ? 1 : 0);
        return ret;
    }

    private static Local hexOf(Terreno terreno, Cidade cidade) {
        final Local ret = new Local();
        ret.setCodigo("1428");
        ret.setCoordenadas("1428");
        ret.setTerreno(terreno);
        ret.setCidade(cidade);
        // BOTH ways, as a loaded world has them: getCityDefenseCombat reaches the ground through
        // city.getLocal(), so a city that does not know its own hex NPEs there the moment its
        // owner has one of the four terrain habilidades.
        cidade.setLocal(ret);
        return ret;
    }

    /** A fleet carrying troops: ships for burden, plus the land troops it would put ashore. */
    private static ArmySim fleetWithTroops(String nome, Nacao nacao, Local local) {
        // a TRANSPORT: capacity comes from the ;TTT; habilidade's value, and isEsquadraEmbarcada
        // is capacity >= burden, so 50 ships at 250 each comfortably carries 100 infantry
        final TipoTropa ship = shipType("ship");
        final Habilidade transport = new Habilidade();
        transport.setCodigo(";TTT;");
        transport.setNome(";TTT;");
        transport.setValor(250);
        ship.addHabilidade(transport);
        final Pelotao ships = platoon(ship, 50);
        final Pelotao troops = platoon(troopType("inf", 60, 40, false), 100);
        final ArmySim ret = army(nome, nacao, ships, troops);
        ret.setLocal(local);
        ret.setCombatLevel(CombatLevel.ATTACK_CITY);
        return ret;
    }

    private static CombatScenario atWar(Local local, Nacao attacker, Nacao owner) {
        final CombatScenario ret = new CombatScenario(null, local);
        ret.setRelacionamento(attacker, owner, RelationshipMatrix.SWORN_ENEMY);
        return ret;
    }

    /**
     * {@code Hexagono.isAncoravel()} is terrain OR docks, and the docks half was missing.
     *
     * A fleet assaulting a port city on non-coastal ground is admitted by the Judge at both the
     * gate and the selection filter. Turning it away here also contradicted
     * {@code LayerParticipation}, which gets it right - so the roster showed a city badge for an
     * assault the resolver refused to run.
     */
    @Test
    public void aFleetMayLandAtAPortCityOnNonAnchorableGround() {
        final Nacao attacker = nacao("att"), owner = nacao("own");
        final Cidade port = city(owner, 3, true);
        final Local local = hexOf(inland(), port);
        final CombatScenario scenario = atWar(local, attacker, owner);
        scenario.addArmy(fleetWithTroops("Fleet", attacker, local),
                CombatScenario.Provenance.EXACT);

        final CombatResult ret = new CombatChain().resolve(scenario, null);

        assertFalse(ret.getCityResult().getAttackers().isEmpty(),
                "the docks let him ashore, so he is at the walls");
    }

    /** And without docks, the same fleet on the same ground is turned away. */
    @Test
    public void thatSameFleetIsTurnedAwayWithoutDocks() {
        final Nacao attacker = nacao("att"), owner = nacao("own");
        final Local local = hexOf(inland(), city(owner, 3, false));
        final CombatScenario scenario = atWar(local, attacker, owner);
        scenario.addArmy(fleetWithTroops("Fleet", attacker, local),
                CombatScenario.Provenance.EXACT);

        assertTrue(new CombatChain().resolve(scenario, null).getCityResult().getAttackers()
                .isEmpty(), "nowhere to land");
    }

    /**
     * The Terrain combo has to move this gate too.
     *
     * It changes every other combat number, so a landing that ignores it is the same class of
     * defect as the terrain not reaching the armies at all - a control that looks like it works.
     */
    @Test
    public void thePlayersTerrainOverrideDecidesWhereAFleetCanLand() {
        final Nacao attacker = nacao("att"), owner = nacao("own");
        final Local local = hexOf(inland(), city(owner, 3, false));
        final CombatScenario scenario = atWar(local, attacker, owner);
        scenario.addArmy(fleetWithTroops("Fleet", attacker, local),
                CombatScenario.Provenance.EXACT);
        assertTrue(new CombatChain().resolve(scenario, null).getCityResult().getAttackers()
                .isEmpty(), "inland to start with");

        scenario.setTerreno(PLAIN);     // the fixture's anchorable ground

        assertFalse(new CombatChain().resolve(scenario, null).getCityResult().getAttackers()
                .isEmpty(), "and the override lets him ashore");
    }

    /**
     * The walls kill. The Judge does {@code sumCombateDano} then {@code doCombateDano}, and the
     * class used to compute the damage and never spend it.
     */
    @Test
    public void theWallsInflictRealCasualties() {
        final Nacao attacker = nacao("att"), owner = nacao("own");
        // size 5, heavily fortified: a defense worth having
        final Cidade fortress = city(owner, 5, false);
        fortress.setFortificacao(5);
        final Local local = hexOf(PLAIN, fortress);
        final CombatScenario scenario = atWar(local, attacker, owner);
        final Pelotao inf = platoon(troopType("inf", 60, 40, false), 900);
        final ArmySim besieger = army("Besieger", attacker, inf);
        besieger.setLocal(local);
        besieger.setCombatLevel(CombatLevel.ATTACK_CITY);
        scenario.addArmy(besieger, CombatScenario.Provenance.EXACT);

        final CombatResult ret = new CombatChain().resolve(scenario, null);
        final ArmySim survivor = ret.getCityResult().getAttackers().get(0);

        assertTrue(ret.getCityResult().getDamage(survivor) > 0, "the city hit back");
        assertTrue(exercitoFacade().getQtTropasTotal(survivor) < 900,
                "and somebody died for it: "
                + exercitoFacade().getQtTropasTotal(survivor) + " of 900 left");
        assertEquals(900, inf.getQtd(), "on the COPY - the player's own platoon is untouched");
    }

    /**
     * The Terrain combo has to move the CITY's defence, not just the armies'.
     *
     * {@code getCityDefenseCombat} reads the ground off {@code city.getLocal().getTerreno()}, and
     * four national habilidades turn on it - {@code ;PCD;} on mountain is the one used here. The
     * armies always honoured the override, because the platoon formula takes terrain as a
     * parameter and {@code setTerreno} propagates it; the city did not, so a player moving the
     * combo watched every number on screen change except the one he was attacking. A control that
     * looks like it works is worse than one that is missing.
     */
    @Test
    public void thePlayersTerrainOverrideMovesTheCityDefence() {
        final Nacao attacker = nacao("att"), owner = nacao("own");
        // ;PCD; doubles the defence on mountain ground
        final Habilidade mountainDefence = new Habilidade();
        mountainDefence.setCodigo(";PCD;");
        mountainDefence.setNome(";PCD;");
        mountainDefence.setValor(100);
        owner.addHabilidade(mountainDefence);

        final Local local = hexOf(PLAIN, city(owner, 3, false));
        final CombatScenario scenario = atWar(local, attacker, owner);
        scenario.addArmy(besiegerNamed("Besieger", attacker, local, 900),
                CombatScenario.Provenance.EXACT);

        final int onPlain = new CombatChain().resolve(scenario, null).getCityResult().getDefence();
        scenario.setTerreno(inland());      // the fixture's mountain
        final int onMountain =
                new CombatChain().resolve(scenario, null).getCityResult().getDefence();

        assertTrue(onMountain > onPlain,
                "the mountain has to reach the walls: " + onPlain + " -> " + onMountain);
        assertEquals(onPlain * 2, onMountain, ";PCD; at 100 percent doubles it");
    }

    /**
     * Two attackers, equal strength: the city goes to the one standing FIRST on the hex.
     *
     * {@code doCityCaptured} compares with a strict {@code <} from a maximum of 0, walking
     * {@code atacantes} in hex order, so the first army holding the maximum keeps it. This side
     * walks {@code copies.all()}, which is the same order - and a tie is precisely where that
     * stops being a detail, because the two armies belong to different nations and the city
     * changes hands to one of them.
     */
    @Test
    public void aTiedClaimGoesToTheArmyStandingFirstOnTheHex() {
        final Nacao first = nacao("one"), second = nacao("two"), owner = nacao("own");
        final Local local = hexOf(PLAIN, city(owner, 3, false));
        final CombatScenario scenario = atWar(local, first, owner);
        scenario.setRelacionamento(second, owner, RelationshipMatrix.SWORN_ENEMY);
        // identical in every term that feeds getAttack(1)
        final ArmySim one = besiegerNamed("Alpha", first, local, 4000);
        final ArmySim two = besiegerNamed("Bravo", second, local, 4000);
        scenario.addArmy(one, CombatScenario.Provenance.EXACT);
        scenario.addArmy(two, CombatScenario.Provenance.EXACT);

        final CombatResult ret = new CombatChain().resolve(scenario, null);

        // a CAMP (size 1) is razed on capture rather than held, so this is a real city
        assertEquals(CityCombatResolver.CityOutcome.CAPTURED, ret.getCityResult().getOutcome(),
                "between them they beat the walls");
        assertEquals(first, ret.getCityResult().getOwner().getNacao(),
                "the tie goes to the army the hex lists first, as it does in the turn");
    }

    private static ArmySim besiegerNamed(String nome, Nacao nacao, Local local, int qtd) {
        final ArmySim ret = army(nome, nacao, platoon(troopType("inf", 60, 40, false), qtd));
        ret.setLocal(local);
        ret.setCombatLevel(CombatLevel.ATTACK_CITY);
        ret.setMoral(100);
        return ret;
    }

    private static business.facade.ExercitoFacade exercitoFacade() {
        return new business.facade.ExercitoFacade();
    }
}
