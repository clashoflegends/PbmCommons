package business.combat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import model.Cidade;
import model.Habilidade;
import model.Jogador;
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
 * R-40 and R-41: Run is disabled with a STATED reason, and never performs a silent no-op.
 *
 * A button that does nothing when pressed is the complaint the whole BattleSim rebuild started
 * from, so the gate is a model with a reason rather than a boolean, and it is tested here without a
 * bundle and without a window.
 *
 * The states are ordered most-fixable first. The player can put armies on the hex, can declare a war
 * in the diplomacy panel, and can give an empty army a composition - so the first answer he is handed
 * is always one he can act on. There was a fifth state, NO_ENGINE, for a scenario that was resolvable
 * with nothing to resolve it; T-801 landed the land resolver and took it and its flag away.
 */
public class CombatSimRunGateTest {

    private static Nacao nacao(String codigo, String nome) {
        final Nacao ret = new Nacao();
        ret.setCodigo(codigo);
        ret.setNome(nome);
        return ret;
    }

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

    private static Local hex(Cidade cidade) {
        final Terreno terreno = new Terreno();
        terreno.setCodigo("P");
        terreno.setNome("Plain");
        terreno.setAncoravel(true);
        final Local ret = new Local();
        ret.setCodigo("0350");
        ret.setCoordenadas("0350");
        ret.setTerreno(terreno);
        ret.setCidade(cidade);
        return ret;
    }

    private static ArmySim army(String nome, Nacao nacao, Local hex, Pelotao... pelotoes) {
        final ArmySim ret = new ArmySim(nome, hex.getTerreno(), nacao);
        ret.setCodigo(nome);
        for (Pelotao one : pelotoes) {
            ret.getPelotoes().put(one.getCodigo(), one);
        }
        return ret;
    }

    /** An empty hex opens the window. It is a real state, and Run must say so rather than sulk. */
    @Test
    public void anEmptyHexBlocksTheRunAndSaysWhich() {
        final CombatScenario scenario = new CombatScenario(null, hex(null));

        assertEquals(RunGate.NO_ARMIES, scenario.getRunGate());
        assertFalse(scenario.getRunGate().isRunnable());
    }

    /** Armies present and all at peace. Nothing to resolve, and nothing wrong. */
    @Test
    public void aHexWhereNobodyIsHostileBlocksTheRun() {
        final Local hex = hex(null);
        final Nacao mine = nacao("m", "Mine"), theirs = nacao("t", "Theirs");
        final CombatScenario scenario = new CombatScenario(null, hex);
        scenario.addArmy(army("mine", mine, hex, platoon(troopType("inf", false), 900)),
                CombatScenario.Provenance.EXACT);
        scenario.addArmy(army("theirs", theirs, hex, platoon(troopType("inf", false), 500)),
                CombatScenario.Provenance.ESTIMATED);
        // The peace has to be STATED now. Since 2026-09-21 an unread pair is assumed HOSTILE, so
        // two armies about which nothing is known do fight - which is the whole point of the new
        // default. "Nobody is hostile" is therefore a positive claim, and the player's own edit is
        // how the simulator hears one.
        scenario.setRelacionamento(mine, theirs, RelationshipMatrix.NEUTRAL);
        scenario.setRelacionamento(theirs, mine, RelationshipMatrix.NEUTRAL);

        assertEquals(RunGate.NO_HOSTILE_PAIR, scenario.getRunGate());
    }

    /**
     * The third state, and the one a boolean would have missed: hostile, and yet nobody can reach
     * anybody.
     *
     * Hex 0350, from a live screenshot: the player's fleet and an enemy land army, correctly read as
     * hostile and grouped as "Fighting against me". The fleet carries no land troops and the enemy
     * has no ships, so the sea layer has no enemy fleet, the land layer has nothing of his ashore,
     * and neither is ordered to assault the city. {@code hasCombat()} is TRUE throughout, because
     * hostility is a property of nations - so gating on it alone would have enabled Run and handed
     * the engine an empty battle.
     */
    @Test
    public void hostileButUnreachableBlocksTheRun() {
        final Local hex = hex(null);
        final Nacao mine = nacao("m", "Mine"), theirs = nacao("t", "Theirs");
        final CombatScenario scenario = new CombatScenario(null, hex);
        scenario.addArmy(army("my fleet", mine, hex, platoon(troopType("trireme", true), 46)),
                CombatScenario.Provenance.EXACT);
        scenario.addArmy(army("their host", theirs, hex, platoon(troopType("inf", false), 1200)),
                CombatScenario.Provenance.ESTIMATED);
        scenario.setRelacionamento(mine, theirs, RelationshipMatrix.SWORN_ENEMY);

        assertTrue(scenario.hasCombat(), "they ARE hostile - that is what makes this the trap");
        assertFalse(scenario.hasEngagement(), "and yet no layer can pair them");
        assertEquals(RunGate.NO_ENGAGEMENT, scenario.getRunGate());
    }

    /** Two hostile land armies do engage, and nothing else is in the way: Run works. */
    @Test
    public void aResolvableScenarioIsReady() {
        final Local hex = hex(null);
        final Nacao mine = nacao("m", "Mine"), theirs = nacao("t", "Theirs");
        final CombatScenario scenario = new CombatScenario(null, hex);
        scenario.addArmy(army("mine", mine, hex, platoon(troopType("inf", false), 900)),
                CombatScenario.Provenance.EXACT);
        scenario.addArmy(army("theirs", theirs, hex, platoon(troopType("inf", false), 500)),
                CombatScenario.Provenance.ESTIMATED);
        scenario.setRelacionamento(mine, theirs, RelationshipMatrix.SWORN_ENEMY);

        assertEquals(RunGate.READY, scenario.getRunGate(),
                "everything the player controls is right, and the land resolver landed at T-801");
        assertTrue(scenario.getRunGate().isRunnable());
    }

    /**
     * R-41: an ASSUMED pair never blocks the run.
     *
     * "Allegiance is always derived, so it is never a blocker." Two third parties in a free-for-all
     * cannot be resolved, so they are assumed not hostile and DISCLOSED in the status bar. Refusing
     * to run because something was guessed would turn the disclosure into a punishment, and the
     * simulator's job is to answer the question with the information there is while saying which
     * parts were guesses.
     */
    @Test
    public void aGuessedRelationshipIsDisclosedAndNeverBlocksTheRun() {
        final Local hex = hex(null);
        final Jogador me = new Jogador();
        me.setCodigo("j1");
        me.setNome("me");
        final Nacao mine = nacao("m", "Mine");
        mine.setOwner(me);
        final Nacao foe = nacao("f", "Foe"), third = nacao("x", "Third");
        mine.getRelacionamentos().put(foe, RelationshipMatrix.SWORN_ENEMY);

        final CombatScenario scenario = new CombatScenario(null, hex);
        scenario.setObserver(me);
        scenario.addArmy(army("mine", mine, hex, platoon(troopType("inf", false), 900)),
                CombatScenario.Provenance.EXACT);
        scenario.addArmy(army("foe", foe, hex, platoon(troopType("inf", false), 500)),
                CombatScenario.Provenance.ESTIMATED);
        scenario.addArmy(army("third", third, hex, platoon(troopType("inf", false), 300)),
                CombatScenario.Provenance.ESTIMATED);

        assertTrue(scenario.getAssumedCount() > 0, "the third party's pairs are guesses");
        assertEquals(RunGate.READY, scenario.getRunGate(),
                "and a guess is disclosed, never used to refuse the run");
    }

    /** Every state carries a distinct answer; none of them is silently runnable. */
    @Test
    public void onlyReadyIsRunnable() {
        for (RunGate gate : RunGate.values()) {
            assertEquals(gate == RunGate.READY, gate.isRunnable(), gate.name());
        }
    }

    /**
     * Every gate has a sentence in the bundle, and no sentence outlives its gate.
     *
     * {@code BattleSimConverter} builds the key from {@code gate.name()}, so NOTHING greps: a label
     * for a deleted constant sits there forever looking current - BATTLESIM.RUN.DISABLED.NO_ENGINE
     * did, still promising an engine that arrived at T-801 - and a constant added without a label
     * fails SILENTLY, because {@code BundleManager.getString} logs a missing key and returns
     * "N/A (Missing Translation: ...)" rather than throwing. The player would read that in the
     * status bar. The key convention is the converter's; it is asserted here because this is the
     * module that ships the enum AND the bundle.
     *
     * READY maps to an EMPTY value on purpose - when Run works there is nothing to explain - so this
     * asks whether the key EXISTS, never whether it says anything.
     */
    @Test
    public void everyGateHasASentenceAndEverySentenceHasAGate() throws IOException {
        final Properties labels = new Properties();
        try (InputStream is = CombatSimRunGateTest.class.getResourceAsStream("/labels.properties")) {
            assertTrue(is != null, "missing /labels.properties");
            labels.load(new InputStreamReader(is, StandardCharsets.ISO_8859_1));
        }
        final List<String> expected = new ArrayList<>();
        for (RunGate gate : RunGate.values()) {
            final String key = keyFor(gate);
            expected.add(key);
            assertTrue(labels.containsKey(key), gate.name() + " has no label: " + key);
        }
        final List<String> orphans = new ArrayList<>();
        for (String key : labels.stringPropertyNames()) {
            // BATTLESIM.RUN.DISABLED. is the gate's own namespace - BATTLESIM.RUN.SIMULATION is the
            // button's caption and lives outside it. NO_ENGAGEMENT.EMPTY is a second, more specific
            // sentence for a gate that HAS one, so it is a legitimate extra; anything else under
            // the prefix has lost its constant.
            if (key.startsWith("BATTLESIM.RUN.DISABLED.") && !expected.contains(key)
                    && !key.endsWith(".EMPTY")) {
                orphans.add(key);
            }
        }
        assertTrue(orphans.isEmpty(), "labels for RunGates that no longer exist: " + orphans);
    }

    /** The converter's key convention, restated where the enum can be iterated. */
    private static String keyFor(RunGate gate) {
        return "BATTLESIM.RUN."
                + (gate == RunGate.READY ? "READY" : "DISABLED." + gate.name());
    }
}
