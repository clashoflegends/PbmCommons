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

    private static business.facade.ExercitoFacade exercitoFacade() {
        return new business.facade.ExercitoFacade();
    }
}
