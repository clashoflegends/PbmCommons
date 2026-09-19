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
 * Three things here are invisible from the client code:
 *
 * <ul>
 *   <li>an unscouted foreign army arrives with its composition replaced by two placeholder
 *       platoons, and is loaded exactly as it comes - that is the game, not a defect;</li>
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

        final CombatScenario s = new ScenarioLoader().load(partida(";FFA;"), local, me);

        assertEquals(2, s.getArmies().size());
        assertEquals(1400, s.getQtTropasTotal());
    }

    /**
     * Only the observer's own armies are exact. There is no second EGF to make anyone else's so.
     *
     * An ally's armies are an outside view like any other: what the server chose to put in the
     * observer's single results file. Friendly diplomacy does not change that.
     */
    @Test
    public void onlyMyOwnArmiesAreExact() {
        final Jogador me = jogador("j1");
        final Nacao ally = nacao("a", jogador("j2"));
        final Local local = hex(null);
        army("a3", ally, local, 0, platoon(troopType("ainf", false), 300));

        final CombatScenario s = new ScenarioLoader().load(partida(";FFA;"), local, me);

        assertEquals(CombatScenario.Provenance.ESTIMATED, s.getProvenance(s.getArmies().get(0)));
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

        final CombatScenario s = new ScenarioLoader().load(partida(";FFA;"), local, jogador("j1"));

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
                .load(partida(";FFA;"), local, null).getArmies().get(0);

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
                .load(partida(";FFA;"), local, null).getArmies().get(0);
        final Pelotao simulated = loaded.getPelotoes().values().iterator().next();

        assertNotSame(real, simulated);
        simulated.setQtd(1);
        assertEquals(900, real.getQtd(), "the loaded world must be untouched");
    }

    @Test
    public void anEmptyHexLoadsAnEmptyScenario() {
        final CombatScenario s = new ScenarioLoader()
                .load(partida(";FFA;"), hex(null), jogador("j1"));

        assertEquals(0, s.getArmies().size());
        assertFalse(s.hasCombat());
    }

    /**
     * An unscouted foreign army arrives as two placeholder platoons, and the simulator loads it
     * exactly like any other outside view. No special provenance, no warning, no gate.
     *
     * This is pinned because it looks like a bug and is not. Not knowing what is in an enemy stack
     * IS the game - scouting is how a player finds out, and guessing is what he does if he did not.
     * The placeholder pair is the intelligence he actually holds, so it is what the simulator runs
     * on. A first pass here built an UNKNOWN_COMPOSITION provenance and a cannot-tell landing state
     * on the opposite assumption; both were removed.
     */
    @Test
    public void anUnscoutedEnemyIsLoadedAsIsLikeAnyOtherOutsideView() {
        final Jogador me = jogador("j1");
        final Nacao mine = nacao("m", me), foe = nacao("f", null);
        final Local local = hex(null);
        army("a1", mine, local, 0, platoon(troopType("inf", false), 900));
        // exactly what the server sends for an army the player has not scouted
        army("a2", foe, local, 0,
                platoon(troopType("none", false), 500),
                platoon(troopType("ship", true), 20));

        final CombatScenario s = new ScenarioLoader().load(deathMatch(), local, me);

        assertEquals(2, s.getArmies().size());
        assertEquals(1420, s.getQtTropasTotal(), "the head count is exact even when the mix is not");
        for (ArmySim army : s.getArmies()) {
            final CombatScenario.Provenance expected = "a1".equals(army.getCodigo())
                    ? CombatScenario.Provenance.EXACT
                    : CombatScenario.Provenance.ESTIMATED;
            assertEquals(expected, s.getProvenance(army));
        }
    }

    /**
     * And it fights: a placeholder fleet carrying troops is not held back by the fact that the
     * player cannot see its transports.
     *
     * {@code ship} carries {@code ;TTN;} so it is correctly seen as a fleet, but capacity comes
     * from {@code ;TTT;}, which the placeholders lack - so it reads as "troops not aboard" and
     * lands. That is the faithful answer for the input in hand. The player who needs better scouts
     * the army or retypes its platoons.
     */
    @Test
    public void anUnscoutedFleetStillTakesPartOnTheGroundItReachesFor() {
        final Jogador me = jogador("j1");
        final Nacao mine = nacao("m", me), foe = nacao("f", null);
        final Local local = deepWater();
        army("a1", mine, local, 0, platoon(troopType("inf", false), 900));
        army("a2", foe, local, 0,
                platoon(troopType("none", false), 500),
                platoon(troopType("ship", true), 20));

        final CombatScenario s = new ScenarioLoader().load(deathMatch(), local, me);
        for (ArmySim army : s.getArmies()) {
            if ("a2".equals(army.getCodigo())) {
                assertTrue(s.getParticipation().get(army).isIn(CombatLayer.ARMY));
            }
        }
    }
}
