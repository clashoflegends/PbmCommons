package business.combat;

import java.util.Arrays;
import model.Cidade;
import model.Exercito;
import model.Habilidade;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Partida;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loading a hex: who is standing on it, and how much of each army the player actually knows.
 *
 * The interesting cases here are all invisible from the client code, and each of them silently
 * produces a confident wrong answer rather than a failure:
 *
 * <ul>
 *   <li>a foreign army usually arrives with its composition replaced by two placeholder platoons
 *       that attack and defend at 1, so simulating it unchanged reports a crushing win;</li>
 *   <li>a garrison is an ordinary army with no commander, and leaving it out understates a city
 *       assault;</li>
 *   <li>combat intent is not in the EGF at all, not even for the player's own armies.</li>
 * </ul>
 */
public class ScenarioLoaderTest {

    private static TipoTropa troopType(String codigo, boolean ships) {
        final TipoTropa ret = new TipoTropa();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        if (ships) {
            final Habilidade hab = new Habilidade();
            hab.setCodigo(";TTN;");
            hab.setNome(";TTN;");
            ret.addHabilidade(hab);
        }
        return ret;
    }

    private static Pelotao platoon(TipoTropa tipo, int qtd) {
        final Pelotao ret = new Pelotao();
        ret.setTipoTropa(tipo);
        ret.setQtd(qtd);
        return ret;
    }

    private static Nacao nacao(String codigo, Jogador owner) {
        final Nacao ret = new Nacao();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        if (owner != null) {
            // Nacao.setOwner back-registers into the Jogador and has no null guard. A foreign
            // nation simply never has it called, which is exactly how the EGF arrives.
            ret.setOwner(owner);
        }
        return ret;
    }

    private static Jogador jogador(String codigo) {
        final Jogador ret = new Jogador();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        return ret;
    }

    private static Local hex(Cidade cidade) {
        final Terreno terreno = new Terreno();
        terreno.setCodigo("P");
        terreno.setNome("Plain");
        terreno.setAncoravel(true);
        final Local ret = new Local();
        ret.setCodigo("1428");
        ret.setCoordenadas("1428");
        ret.setTerreno(terreno);
        if (cidade != null) {
            ret.setCidade(cidade);
        }
        return ret;
    }

    /** Open water: a fleet can win here and still have nowhere to put its troops. */
    private static Local deepWater() {
        final Terreno terreno = new Terreno();
        terreno.setCodigo("D");
        terreno.setNome("Deep water");
        final Local ret = new Local();
        ret.setCodigo("1429");
        ret.setCoordenadas("1429");
        ret.setTerreno(terreno);
        return ret;
    }

    /** A Death Match needs no relationship rows, so the fixtures stay about the thing under test. */
    private static Partida deathMatch() {
        return partida(";GDM;");
    }

    /** Places an army on a hex. Codigo first: setLocal registers it in the Local, keyed on it. */
    private static Exercito army(String codigo, Nacao nacao, Local local, int tatica,
            Pelotao... pelotoes) {
        final Exercito ret = new Exercito();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        ret.setNacao(nacao);
        ret.setTatica(tatica);
        ret.setLocal(local);
        for (Pelotao pelotao : pelotoes) {
            ret.getPelotoes().put(pelotao.getCodigo(), pelotao);
        }
        return ret;
    }

    private static Partida partida(String habilidades) {
        final Partida ret = new Partida();
        ret.setCodigo("g1");
        ret.setNome("g1");
        for (String cd : habilidades.split(",")) {
            if (!cd.isEmpty()) {
                final Habilidade hab = new Habilidade();
                hab.setCodigo(cd);
                hab.setNome(cd);
                ret.addHabilidade(hab);
            }
        }
        return ret;
    }

    @Test
    public void everyArmyOnTheHexIsLoaded() {
        final Jogador me = jogador("j1");
        final Nacao mine = nacao("m", me), foe = nacao("f", null);
        final Local local = hex(null);
        army("a1", mine, local, 0, platoon(troopType("inf", false), 900));
        army("a2", foe, local, 0, platoon(troopType("einf", false), 500));

        final CombatScenario s = new ScenarioLoader().load(partida(";FFA;"), local, me, null);

        assertEquals(2, s.getArmies().size());
        assertEquals(1400, s.getQtTropasTotal());
    }

    /**
     * The placeholder trap. A foreign army at visibility level 4 arrives as one {@code none} platoon
     * and one {@code ship} platoon, both attacking and defending at 1 on every terrain. Counting it
     * as an ESTIMATE would let the simulator report a crushing win over an army it knows nothing
     * about, which is the one direction this tool must never be wrong in.
     */
    @Test
    public void aForeignArmyOfPlaceholderPlatoonsIsFlaggedAsUnknownNotEstimated() {
        final Jogador me = jogador("j1");
        final Nacao mine = nacao("m", me), foe = nacao("f", null);
        final Local local = hex(null);
        army("a1", mine, local, 0, platoon(troopType("inf", false), 900));
        army("a2", foe, local, 0,
                platoon(troopType(ScenarioLoader.TROOP_UNKNOWN_LAND, false), 500),
                platoon(troopType(ScenarioLoader.TROOP_UNKNOWN_SHIP, true), 20));

        final CombatScenario s = new ScenarioLoader().load(partida(";FFA;"), local, me, null);

        assertEquals(1, s.getArmiesWithUnknownComposition().size());
        assertEquals("a2", s.getArmiesWithUnknownComposition().get(0).getCodigo());
        for (ArmySim army : s.getArmies()) {
            if ("a1".equals(army.getCodigo())) {
                assertEquals(CombatScenario.Provenance.EXACT, s.getProvenance(army));
            } else {
                assertEquals(CombatScenario.Provenance.UNKNOWN_COMPOSITION, s.getProvenance(army));
            }
        }
    }

    /**
     * The other half: a foreign army seen at visibility level 2 carries its REAL platoons. The
     * client cannot ask which level it got, so the placeholder types are the only signal, and an
     * outside view must not be flagged as unknown just for being foreign.
     */
    @Test
    public void aForeignArmyWithRealPlatoonsIsAnEstimateNotAnUnknown() {
        final Jogador me = jogador("j1");
        final Local local = hex(null);
        army("a2", nacao("f", null), local, 0, platoon(troopType("einf", false), 500));

        final CombatScenario s = new ScenarioLoader().load(partida(";FFA;"), local, me, null);

        assertEquals(CombatScenario.Provenance.ESTIMATED, s.getProvenance(s.getArmies().get(0)));
        assertTrue(s.getArmiesWithUnknownComposition().isEmpty());
    }

    /** Supplying a composition of his own is how the player clears the flag. */
    @Test
    public void editingAPlaceholderPlatoonRetiresTheUnknownFlag() {
        final Local local = hex(null);
        army("a2", nacao("f", null), local, 0,
                platoon(troopType(ScenarioLoader.TROOP_UNKNOWN_LAND, false), 500));

        final CombatScenario s = new ScenarioLoader().load(partida(";FFA;"), local, jogador("j1"), null);
        final ArmySim loaded = s.getArmies().get(0);
        assertEquals(1, s.getArmiesWithUnknownComposition().size());

        for (Pelotao pelotao : loaded.getPelotoes().values()) {
            s.setEdited(pelotao);
        }

        assertTrue(s.getArmiesWithUnknownComposition().isEmpty(),
                "once the player has said what he thinks is there, it is his number");
    }

    /**
     * A merged ally's armies are exact, and the caller is the one who says which allies those are.
     * Deciding it here would duplicate Counselor knowledge this library cannot see.
     */
    @Test
    public void aMergedAllysArmiesAreExact() {
        final Jogador me = jogador("j1");
        final Nacao ally = nacao("a", jogador("j2"));
        final Local local = hex(null);
        army("a3", ally, local, 0, platoon(troopType("ainf", false), 300));

        final CombatScenario notMerged = new ScenarioLoader()
                .load(partida(";FFA;"), local, me, null);
        assertEquals(CombatScenario.Provenance.ESTIMATED,
                notMerged.getProvenance(notMerged.getArmies().get(0)));

        final CombatScenario merged = new ScenarioLoader()
                .load(partida(";FFA;"), local, me, Arrays.asList(ally));
        assertEquals(CombatScenario.Provenance.EXACT,
                merged.getProvenance(merged.getArmies().get(0)));
        assertTrue(merged.getMergedNacoes().contains(ally),
                "and the scenario keeps it, so the hostility matrix reads that ally's row too");
    }

    /**
     * A garrison is an ordinary army with no commander. It comes through the same door, and it has
     * to: leaving it out understates a city assault.
     */
    @Test
    public void aGarrisonIsLoadedLikeAnyOtherArmy() {
        final Nacao owner = nacao("o", null);
        final Cidade cidade = new Cidade();
        cidade.setCodigo("c1");
        cidade.setNome("City");
        cidade.setNacao(owner);
        final Local local = hex(cidade);
        army("g1", owner, local, 0, platoon(troopType("mil", false), 200));

        final CombatScenario s = new ScenarioLoader().load(partida(";FFA;"), local, jogador("j1"), null);

        assertEquals(1, s.getArmies().size());
        assertEquals(200, s.getQtTropasTotal());
        assertTrue(s.isCityParticipates(), "the hex has a city, so the city layer is on");
    }

    /**
     * Combat intent is NOT in the EGF - {@code combateNivel} and {@code combateNacaoNumero} live on
     * the Judge's ExercitoControl. Every army therefore loads at the default and the player states
     * his assumptions. Tactic IS carried, and used to be dropped by both copy constructors.
     */
    @Test
    public void tacticSurvivesTheLoadAndIntentIsAlwaysTheDefault() {
        final Local local = hex(null);
        army("a1", nacao("m", null), local, 3, platoon(troopType("inf", false), 900));

        final ArmySim loaded = new ScenarioLoader()
                .load(partida(";FFA;"), local, null, null).getArmies().get(0);

        assertEquals(3, loaded.getTatica());
        assertEquals(CombatLevel.ATTACK_ARMY, loaded.getCombatLevel());
        assertEquals(null, loaded.getTargetNacao());
    }

    /** Loading must not hand the simulator the world's own platoons. */
    @Test
    public void loadingDoesNotShareThePlatoonsWithTheLoadedWorld() {
        final Local local = hex(null);
        final Pelotao real = platoon(troopType("inf", false), 900);
        army("a1", nacao("m", null), local, 0, real);

        final ArmySim loaded = new ScenarioLoader()
                .load(partida(";FFA;"), local, null, null).getArmies().get(0);
        final Pelotao simulated = loaded.getPelotoes().values().iterator().next();

        assertNotSame(real, simulated);
        simulated.setQtd(1);
        assertEquals(900, real.getQtd(), "the loaded world must be untouched");
    }

    @Test
    public void anEmptyHexLoadsAnEmptyScenario() {
        final CombatScenario s = new ScenarioLoader()
                .load(partida(";FFA;"), hex(null), jogador("j1"), null);

        assertEquals(0, s.getArmies().size());
        assertFalse(s.hasCombat());
    }

    /**
     * The placeholder fleet, and the over-promise it would otherwise cause.
     *
     * A foreign army with both land troops and ships arrives as exactly two platoons: one
     * {@code none} and one {@code ship}. {@code ship} carries {@code ;TTN;}, so it is correctly seen
     * as a fleet - but transport CAPACITY comes from {@code ;TTT;}, which neither placeholder
     * carries. So capacity reads 0, burden reads 500, {@code isEsquadraEmbarcada} reads false, and
     * the roster concludes the troops are already ashore and will fight - at a hex with nowhere to
     * land. The real army almost certainly had transports and would never have landed.
     *
     * That is a battle reported that will not happen, the one direction this tool must never be
     * wrong in, and it is reachable from an ordinary "open BattleSim on this hex" click.
     */
    @Test
    public void aPlaceholderFleetDoesNotClaimToLandWhereItCannotAnchor() {
        final Jogador me = jogador("j1");
        final Nacao mine = nacao("m", me), foe = nacao("f", null);
        final Local local = deepWater();
        army("a1", mine, local, 0, platoon(troopType("inf", false), 900));
        army("a2", foe, local, 0,
                platoon(troopType(ScenarioLoader.TROOP_UNKNOWN_LAND, false), 500),
                platoon(troopType(ScenarioLoader.TROOP_UNKNOWN_SHIP, true), 20));

        final CombatScenario s = new ScenarioLoader().load(deathMatch(), local, me, null);
        ArmySim placeholder = null;
        for (ArmySim army : s.getArmies()) {
            if ("a2".equals(army.getCodigo())) {
                placeholder = army;
            }
        }

        final LayerParticipation p = s.getParticipation().get(placeholder);
        assertEquals(LayerParticipation.Reason.LANDING_UNKNOWN, p.getReason(CombatLayer.ARMY),
                "capacity 0 from a missing ;TTT; must not be read as troops ashore");
        assertFalse(p.isIn(CombatLayer.ARMY));
    }

    /**
     * The same army where it CAN anchor. Being aboard or ashore stops mattering, so there is no
     * uncertainty to report and it fights normally. The flag must narrow the claim, not blanket it.
     */
    @Test
    public void aPlaceholderFleetStillFightsWhereItCanLand() {
        final Jogador me = jogador("j1");
        final Nacao mine = nacao("m", me), foe = nacao("f", null);
        final Local local = hex(null);          // plain ground, anchorable
        army("a1", mine, local, 0, platoon(troopType("inf", false), 900));
        army("a2", foe, local, 0,
                platoon(troopType(ScenarioLoader.TROOP_UNKNOWN_LAND, false), 500),
                platoon(troopType(ScenarioLoader.TROOP_UNKNOWN_SHIP, true), 20));

        final CombatScenario s = new ScenarioLoader().load(deathMatch(), local, me, null);
        for (ArmySim army : s.getArmies()) {
            if ("a2".equals(army.getCodigo())) {
                assertTrue(s.getParticipation().get(army).isIn(CombatLayer.ARMY),
                        "nothing is uncertain about landing where you can anchor");
            }
        }
    }

    /** ;TTR; is on almost every real troop type, so it identifies nothing. Codigo is the signal. */
    @Test
    public void placeholderDetectionIsByCodigoNotByHabilidade() {
        assertTrue(ScenarioLoader.isUnknownTroopType(troopType(ScenarioLoader.TROOP_UNKNOWN_LAND, false)));
        assertTrue(ScenarioLoader.isUnknownTroopType(troopType(ScenarioLoader.TROOP_UNKNOWN_SHIP, true)));
        assertFalse(ScenarioLoader.isUnknownTroopType(troopType("inf", false)));
        assertFalse(ScenarioLoader.isUnknownTroopType(null));
    }
}
