package business.combat;

import model.Cidade;
import model.Habilidade;
import model.Nacao;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which of the three layers an army fights in, and the reason it sits out the rest.
 *
 * A battle is a chain - navy, then army ashore, then the city - and an army can be in one, two or all
 * three. The badge these tests assert on (`N A C`, with a dot for each layer sat out) is what lets the
 * simulator show a fleet and a land army "side by side" without implying they fight the same battle.
 *
 * The reasons matter as much as the verdict: "why didn't my fleet defend the city?" is a support
 * question whose answer is a rule, and it comes from the same predicate that decided participation.
 */
public class LayerParticipationTest {

    private static final String SHIPS = ";TTN;";
    private static final String CARGO = ";TTT;";

    private static void addHab(TipoTropa troop, String codigo, int valor) {
        final Habilidade hab = new Habilidade();
        hab.setCodigo(codigo);
        hab.setNome(codigo);
        hab.setValor(valor);
        troop.addHabilidade(hab);
    }

    private static TipoTropa troopType(String codigo, boolean ships) {
        final TipoTropa ret = new TipoTropa();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        if (ships) {
            addHab(ret, SHIPS, 0);
        }
        return ret;
    }

    /**
     * A ship that can actually carry an army. Capacity comes from {@code ;TTT;}, NOT from
     * {@code ;TTN;}: a warship is a ship without being a transport, and "are the troops aboard"
     * (capacity >= burden) is what gates whether a fleet needs anchorage to put them ashore.
     */
    private static TipoTropa transportType(String codigo, int capacityEach) {
        final TipoTropa ret = troopType(codigo, true);
        addHab(ret, CARGO, capacityEach);
        return ret;
    }

    private static Pelotao platoon(TipoTropa tipo, int qtd) {
        final Pelotao ret = new Pelotao();
        ret.setTipoTropa(tipo);
        ret.setQtd(qtd);
        return ret;
    }

    private static Nacao nacao(String codigo) {
        final Nacao ret = new Nacao();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        return ret;
    }

    private static Terreno terreno(String codigo, boolean ancoravel) {
        final Terreno ret = new Terreno();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        ret.setAncoravel(ancoravel);
        return ret;
    }

    private static Cidade cidade(Nacao owner, int docas) {
        final Cidade ret = new Cidade();
        ret.setCodigo("c1");
        ret.setNome("City");
        ret.setNacao(owner);
        ret.setDocas(docas);
        return ret;
    }

    private static ArmySim army(String nome, Nacao nacao, Pelotao... pelotoes) {
        final ArmySim ret = new ArmySim(nome, null, nacao);
        ret.setCodigo(nome);
        for (Pelotao pelotao : pelotoes) {
            ret.getPelotoes().put(pelotao.getCodigo(), pelotao);
        }
        return ret;
    }

    /** A matrix in which these two will fight, so the enemy-present test passes. */
    private static HostilityMatrix atWar(ArmySim one, ArmySim two) {
        final HostilityMatrix ret = new HostilityMatrix();
        ret.setHostile(one, two, HostilityMatrix.Origin.READ_FROM_EGF);
        return ret;
    }

    @Test
    public void aFleetCarryingNoTroopsFightsAtSeaAndStops() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim fleet = army("fleet", mine, platoon(troopType("sh", true), 40));
        final ArmySim enemy = army("enemy", foe, platoon(troopType("sh", true), 30));

        final LayerParticipation p = LayerParticipation.of(fleet, CombatLevel.ATTACK_CITY,
                terreno("sea", false), cidade(foe, 0), atWar(fleet, enemy), true);

        assertEquals("N..", p.getBadge());
        assertTrue(p.isIn(CombatLayer.NAVY));
        assertEquals(LayerParticipation.Reason.CARRIES_NO_TROOPS, p.getReason(CombatLayer.ARMY));
        assertEquals(LayerParticipation.Reason.CARRIES_NO_TROOPS, p.getReason(CombatLayer.CITY),
                "a fleet with no troops cannot storm a city however aggressive its orders");
    }

    @Test
    public void aFleetCarryingTroopsFightsAtSeaAndThenAshore() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim fleet = army("fleet", mine,
                platoon(troopType("sh", true), 40), platoon(troopType("inf", false), 600));
        final ArmySim enemy = army("enemy", foe, platoon(troopType("inf", false), 500));

        final LayerParticipation p = LayerParticipation.of(fleet, CombatLevel.ATTACK_ARMY,
                terreno("coast", true), null, atWar(fleet, enemy), false);

        assertEquals("NA.", p.getBadge());
        assertEquals(LayerParticipation.Reason.NO_CITY, p.getReason(CombatLayer.CITY));
    }

    /**
     * Winning at sea is not the same as getting ashore. Deep water with no port stops a loaded fleet
     * at the waterline, which is the rule players most often meet without an explanation.
     */
    @Test
    public void aLoadedFleetCannotLandWhereThereIsNoAnchorageOrPort() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim fleet = army("fleet", mine,
                platoon(transportType("sh", 100), 400), platoon(troopType("inf", false), 10));
        final ArmySim enemy = army("enemy", foe, platoon(troopType("inf", false), 500));

        final LayerParticipation p = LayerParticipation.of(fleet, CombatLevel.ATTACK_ARMY,
                terreno("deep", false), null, atWar(fleet, enemy), false);

        assertEquals(LayerParticipation.Reason.CANNOT_LAND_HERE, p.getReason(CombatLayer.ARMY));
        assertEquals("N..", p.getBadge());
    }

    /** A city with docks is anchorage even when the ground is not. */
    @Test
    public void aPortLetsTheSameFleetLand() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim fleet = army("fleet", mine,
                platoon(transportType("sh", 100), 400), platoon(troopType("inf", false), 10));
        final ArmySim enemy = army("enemy", foe, platoon(troopType("inf", false), 500));

        final LayerParticipation p = LayerParticipation.of(fleet, CombatLevel.ATTACK_ARMY,
                terreno("deep", false), cidade(foe, 2), atWar(fleet, enemy), true);

        assertTrue(p.isIn(CombatLayer.ARMY), "docks are anchorage");
    }

    /**
     * The same landing rule gates the city, not just the ground. The Judge applies it twice:
     * {@code temCombateTerra} refuses the landing and {@code CombatCity.addArmies} drops the army
     * with a "cannot anchor" message. A fleet cannot storm walls from the water.
     */
    @Test
    public void aLoadedFleetThatCannotLandAlsoCannotStormTheCity() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim fleet = army("fleet", mine,
                platoon(transportType("sh", 100), 400), platoon(troopType("inf", false), 10));
        final ArmySim enemy = army("enemy", foe, platoon(troopType("inf", false), 500));

        // a city with no docks, on ground that is not anchorable
        final LayerParticipation p = LayerParticipation.of(fleet, CombatLevel.RAZE_CITY,
                terreno("deep", false), cidade(foe, 0), atWar(fleet, enemy), true);

        assertEquals(LayerParticipation.Reason.CANNOT_LAND_HERE, p.getReason(CombatLayer.CITY));
        assertEquals("N..", p.getBadge());
    }

    @Test
    public void aLandArmyNeverAppearsInTheNavalLayer() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim land = army("land", mine, platoon(troopType("inf", false), 900));
        final ArmySim enemy = army("enemy", foe, platoon(troopType("inf", false), 500));

        final LayerParticipation p = LayerParticipation.of(land, CombatLevel.ATTACK_CITY,
                terreno("plain", false), cidade(foe, 0), atWar(land, enemy), true);

        assertEquals(".AC", p.getBadge());
        assertEquals(LayerParticipation.Reason.NO_SHIPS, p.getReason(CombatLayer.NAVY));
    }

    /** The intent field. Without it the simulator answers a different question than the one asked. */
    @Test
    public void combatLevelDecidesWhetherTheCityIsAssaulted() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim land = army("land", mine, platoon(troopType("inf", false), 900));
        final ArmySim enemy = army("enemy", foe, platoon(troopType("inf", false), 500));
        final Terreno plain = terreno("plain", false);
        final Cidade city = cidade(foe, 0);
        final HostilityMatrix war = atWar(land, enemy);

        assertEquals(LayerParticipation.Reason.WILL_NOT_ASSAULT_CITY,
                LayerParticipation.of(land, CombatLevel.DEFEND_ONLY, plain, city, war, true)
                        .getReason(CombatLayer.CITY));
        assertEquals(LayerParticipation.Reason.WILL_NOT_ASSAULT_CITY,
                LayerParticipation.of(land, CombatLevel.ATTACK_ARMY, plain, city, war, true)
                        .getReason(CombatLayer.CITY));
        assertTrue(LayerParticipation.of(land, CombatLevel.ATTACK_CITY, plain, city, war, true)
                .isIn(CombatLayer.CITY));
        assertTrue(LayerParticipation.of(land, CombatLevel.RAZE_CITY, plain, city, war, true)
                .isIn(CombatLayer.CITY));
    }

    @Test
    public void anArmyDoesNotAssaultACityItIsNotHostileTo() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim land = army("land", mine, platoon(troopType("inf", false), 900));
        final ArmySim enemy = army("enemy", foe, platoon(troopType("inf", false), 500));

        final LayerParticipation p = LayerParticipation.of(land, CombatLevel.RAZE_CITY,
                terreno("plain", false), cidade(nacao("neutral"), 0), atWar(land, enemy), false);

        assertEquals(LayerParticipation.Reason.NOT_HOSTILE_TO_CITY, p.getReason(CombatLayer.CITY));
    }

    @Test
    public void anArmyWithNobodyToFightSitsOutTheFightingLayers() {
        final ArmySim lonely = army("lonely", nacao("m"), platoon(troopType("inf", false), 900));

        final LayerParticipation p = LayerParticipation.of(lonely, CombatLevel.ATTACK_ARMY,
                terreno("plain", false), null, new HostilityMatrix(), false);

        assertEquals(LayerParticipation.Reason.NO_ENEMY_PRESENT, p.getReason(CombatLayer.ARMY));
        assertFalse(p.isInAnyLayer());
    }

    /** Carry-over: an army wiped out earlier in the chain takes no part in what follows. */
    @Test
    public void adestroyedArmyIsOutOfEveryLayer() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim wiped = army("wiped", mine, platoon(troopType("inf", false), 0));
        final ArmySim enemy = army("enemy", foe, platoon(troopType("inf", false), 500));

        final LayerParticipation p = LayerParticipation.of(wiped, CombatLevel.RAZE_CITY,
                terreno("plain", false), cidade(foe, 0), atWar(wiped, enemy), true);

        assertEquals("...", p.getBadge());
        assertEquals(LayerParticipation.Reason.DESTROYED_EARLIER, p.getReason(CombatLayer.CITY));
    }
}
