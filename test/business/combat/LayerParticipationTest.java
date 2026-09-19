package business.combat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    /**
     * Participation for one army, with the roster it is standing in.
     *
     * The roster is not optional: naval and land membership are decided by PAIRING, so an army
     * cannot answer either question alone.
     */
    private static LayerParticipation partOf(ArmySim army, CombatLevel level, Terreno terreno,
            Cidade cidade, boolean hostileToCity, ArmySim... roster) {
        army.setCombatLevel(level);
        final List<ArmySim> all = new ArrayList<>(Arrays.asList(roster));
        if (!all.contains(army)) {
            all.add(0, army);
        }
        final HostilityMatrix matrix = new HostilityMatrix();
        for (ArmySim a : all) {
            matrix.addArmy(a);
            for (ArmySim b : all) {
                if (a != b && a.getNacao() != b.getNacao()) {
                    matrix.setHostile(a, b, HostilityMatrix.Origin.READ_FROM_EGF);
                }
            }
        }
        return LayerParticipation.of(army, all, terreno, cidade, matrix, hostileToCity);
    }

    @Test
    public void aFleetCarryingNoTroopsFightsAtSeaAndStops() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim fleet = army("fleet", mine, platoon(troopType("sh", true), 40));
        final ArmySim enemy = army("enemy", foe, platoon(troopType("sh", true), 30));

        final LayerParticipation p = partOf(fleet, CombatLevel.ATTACK_CITY,
                terreno("sea", false), cidade(foe, 0), true, enemy);

        assertEquals("N \u00b7 \u00b7", p.getBadge());
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
        // the enemy must ALSO be a fleet, or there is no battle at sea to join
        final ArmySim enemy = army("enemy", foe,
                platoon(troopType("esh", true), 30), platoon(troopType("einf", false), 500));

        final LayerParticipation p = partOf(fleet, CombatLevel.ATTACK_ARMY,
                terreno("coast", true), null, false, enemy);

        assertEquals("N A \u00b7", p.getBadge());
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
        final ArmySim enemy = army("enemy", foe, platoon(troopType("esh", true), 500));

        final LayerParticipation p = partOf(fleet, CombatLevel.ATTACK_ARMY,
                terreno("deep", false), null, false, enemy);

        assertEquals(LayerParticipation.Reason.CANNOT_LAND_HERE, p.getReason(CombatLayer.ARMY));
        assertEquals("N \u00b7 \u00b7", p.getBadge());
    }

    /** A city with docks is anchorage even when the ground is not. */
    @Test
    public void aPortLetsTheSameFleetLand() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim fleet = army("fleet", mine,
                platoon(transportType("sh", 100), 400), platoon(troopType("inf", false), 10));
        final ArmySim enemy = army("enemy", foe, platoon(troopType("inf", false), 500));

        final LayerParticipation p = partOf(fleet, CombatLevel.ATTACK_ARMY,
                terreno("deep", false), cidade(foe, 2), true, enemy);

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
        final ArmySim enemy = army("enemy", foe, platoon(troopType("esh", true), 500));

        // a city with no docks, on ground that is not anchorable
        final LayerParticipation p = partOf(fleet, CombatLevel.RAZE_CITY,
                terreno("deep", false), cidade(foe, 0), true, enemy);

        assertEquals(LayerParticipation.Reason.CANNOT_LAND_HERE, p.getReason(CombatLayer.CITY));
        assertEquals("N \u00b7 \u00b7", p.getBadge());
    }

    /**
     * PAIRING, and the bug an antagonist pass caught. Holding ships does not put an army at sea;
     * holding ships AND facing a fleet does. The Judge's naval loop requires
     * exercito.isEsquadra() AND inimigo.isEsquadra().
     */
    @Test
    public void aFleetWhoseOnlyEnemyIsOnLandFightsNoNavalBattle() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim fleet = army("fleet", mine,
                platoon(troopType("sh", true), 40), platoon(troopType("inf", false), 600));
        final ArmySim landEnemy = army("enemy", foe, platoon(troopType("einf", false), 500));

        final LayerParticipation p = partOf(fleet, CombatLevel.ATTACK_ARMY,
                terreno("coast", true), null, false, landEnemy);

        assertEquals(LayerParticipation.Reason.NO_NAVAL_ENEMY, p.getReason(CombatLayer.NAVY));
        assertEquals("\u00b7 A \u00b7", p.getBadge(), "it still fights ashore, just not at sea");
    }

    /**
     * The mirror image: temCombateTerra requires NEITHER side to be ships-only, so a land army whose
     * only enemy is a bare fleet has nothing to fight ashore.
     */
    @Test
    public void aLandArmyWhoseOnlyEnemyIsABareFleetFightsNothingAshore() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim land = army("land", mine, platoon(troopType("inf", false), 900));
        final ArmySim fleet = army("fleet", foe, platoon(troopType("sh", true), 40));

        final LayerParticipation p = partOf(land, CombatLevel.ATTACK_ARMY,
                terreno("coast", true), null, false, fleet);

        assertEquals(LayerParticipation.Reason.NO_LAND_ENEMY, p.getReason(CombatLayer.ARMY));
        assertFalse(p.isInAnyLayer());
    }

    /**
     * The target filter, the Judge's getCombateNacaoNumero(). An army told to attack one nation does
     * not start on another - but is still dragged in by anyone who starts on IT.
     */
    @Test
    public void aTargetedArmyOnlyStartsOnTheNationItNamed() {
        final Nacao mine = nacao("m"), x = nacao("x"), y = nacao("y");
        final ArmySim me = army("me", mine, platoon(troopType("inf", false), 900));
        final ArmySim targetX = army("x1", x, platoon(troopType("xinf", false), 500));
        final ArmySim bystanderY = army("y1", y, platoon(troopType("yinf", false), 500));
        targetX.setCombatLevel(CombatLevel.DEFEND_ONLY);
        bystanderY.setCombatLevel(CombatLevel.DEFEND_ONLY);

        me.setTargetNacao(x);
        assertTrue(partOf(me, CombatLevel.ATTACK_ARMY, terreno("plain", false), null, false,
                targetX, bystanderY).isIn(CombatLayer.ARMY),
                "it does engage the nation it named");

        me.setTargetNacao(nacao("zzz"));
        assertEquals(LayerParticipation.Reason.NO_ENEMY_PRESENT,
                partOf(me, CombatLevel.ATTACK_ARMY, terreno("plain", false), null, false,
                        targetX, bystanderY).getReason(CombatLayer.ARMY),
                "nobody here is the nation it named, and nobody here is attacking it");
    }

    /** Defend-only does not initiate, but is still pulled into a fight someone else starts. */
    @Test
    public void defendOnlyStillFightsWhenAttacked() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim defender = army("def", mine, platoon(troopType("inf", false), 900));
        final ArmySim attacker = army("att", foe, platoon(troopType("einf", false), 500));
        attacker.setCombatLevel(CombatLevel.ATTACK_ARMY);

        final LayerParticipation p = partOf(defender, CombatLevel.DEFEND_ONLY,
                terreno("plain", false), null, false, attacker);

        assertTrue(p.isIn(CombatLayer.ARMY), "being attacked is not optional");
    }

    /** Two defend-only armies stare at each other and nothing happens. */
    @Test
    public void twoDefendersNeverStartAnything() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim one = army("one", mine, platoon(troopType("inf", false), 900));
        final ArmySim two = army("two", foe, platoon(troopType("einf", false), 500));
        two.setCombatLevel(CombatLevel.DEFEND_ONLY);

        final LayerParticipation p = partOf(one, CombatLevel.DEFEND_ONLY,
                terreno("plain", false), null, false, two);

        assertEquals(LayerParticipation.Reason.NO_ENEMY_PRESENT, p.getReason(CombatLayer.ARMY));
    }

    @Test
    public void aLandArmyNeverAppearsInTheNavalLayer() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim land = army("land", mine, platoon(troopType("inf", false), 900));
        final ArmySim enemy = army("enemy", foe, platoon(troopType("inf", false), 500));

        final LayerParticipation p = partOf(land, CombatLevel.ATTACK_CITY,
                terreno("plain", false), cidade(foe, 0), true, enemy);

        assertEquals("\u00b7 A C", p.getBadge());
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

        assertEquals(LayerParticipation.Reason.WILL_NOT_ASSAULT_CITY,
                partOf(land, CombatLevel.DEFEND_ONLY, plain, city, true, enemy)
                        .getReason(CombatLayer.CITY));
        assertEquals(LayerParticipation.Reason.WILL_NOT_ASSAULT_CITY,
                partOf(land, CombatLevel.ATTACK_ARMY, plain, city, true, enemy)
                        .getReason(CombatLayer.CITY));
        assertTrue(partOf(land, CombatLevel.ATTACK_CITY, plain, city, true, enemy)
                .isIn(CombatLayer.CITY));
        assertTrue(partOf(land, CombatLevel.RAZE_CITY, plain, city, true, enemy)
                .isIn(CombatLayer.CITY));
    }

    @Test
    public void anArmyDoesNotAssaultACityItIsNotHostileTo() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim land = army("land", mine, platoon(troopType("inf", false), 900));
        final ArmySim enemy = army("enemy", foe, platoon(troopType("inf", false), 500));

        final LayerParticipation p = partOf(land, CombatLevel.RAZE_CITY,
                terreno("plain", false), cidade(nacao("neutral"), 0), false, enemy);

        assertEquals(LayerParticipation.Reason.NOT_HOSTILE_TO_CITY, p.getReason(CombatLayer.CITY));
    }

    @Test
    public void anArmyWithNobodyToFightSitsOutTheFightingLayers() {
        final ArmySim lonely = army("lonely", nacao("m"), platoon(troopType("inf", false), 900));

        final LayerParticipation p = partOf(lonely, CombatLevel.ATTACK_ARMY,
                terreno("plain", false), null, false);

        assertEquals(LayerParticipation.Reason.NO_ENEMY_PRESENT, p.getReason(CombatLayer.ARMY));
        assertFalse(p.isInAnyLayer());
    }

    /** Carry-over: an army wiped out earlier in the chain takes no part in what follows. */
    @Test
    public void adestroyedArmyIsOutOfEveryLayer() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim wiped = army("wiped", mine, platoon(troopType("inf", false), 0));
        final ArmySim enemy = army("enemy", foe, platoon(troopType("inf", false), 500));

        final LayerParticipation p = partOf(wiped, CombatLevel.RAZE_CITY,
                terreno("plain", false), cidade(foe, 0), true, enemy);

        assertEquals("\u00b7 \u00b7 \u00b7", p.getBadge());
        assertEquals(LayerParticipation.Reason.NO_TROOPS, p.getReason(CombatLayer.CITY));
    }
}
