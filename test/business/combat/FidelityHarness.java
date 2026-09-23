package business.combat;

import business.facade.BattleSimFacade;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import model.Local;
import model.Pelotao;
import model.World;
import org.junit.jupiter.api.Test;

import com.thoughtworks.xstream.XStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the REAL resolver against a real battle, so a forecast can be diffed against the turn.
 *
 * <h3>Why a harness and not just a test</h3>
 *
 * A forecast that disagrees with the turn has two possible causes: the loop is wrong, or the loop is
 * right and was handed different numbers. Separating those by argument is the whole job, so this
 * takes the battle as parameters and prints a machine-readable result that
 * {@code PbmOps/Dev/combatsim/run_fidelity.py} diffs against the Judge's own published lines.
 *
 * <pre>
 *   java business.combat.FidelityHarness &lt;pre-turn .egf&gt; &lt;hex&gt; [spec file]
 * </pre>
 *
 * <h3>The spec file supplies what the EGF cannot</h3>
 *
 * Two inputs are not in the pre-turn EGF and both move the numbers a long way:
 *
 * <ul>
 *   <li><b>Tactic.</b> The EGF carries LAST turn's; the one that ran came from this turn's orders.
 *       The turn text names it per army.</li>
 *   <li><b>Morale.</b> Not in the EGF at all for an army seen from outside, and it moves mid-turn
 *       (a refused challenge is worth {@code rand(10)+5}). So it is FITTED rather than guessed: the
 *       turn's deploy listing publishes each platoon's attack, morale is the only unknown left in
 *       that number, and sweeping it to match pins it exactly. That is how Seagard's Dorian was
 *       found to have fought at 9 rather than the 33 he started the turn with.</li>
 * </ul>
 *
 * One line per army, {@code |}-separated, name matched as a substring of the army's own:
 * <pre>
 *   army|Waldon Wynch|tactic=0|attack=2415
 * </pre>
 */
public class FidelityHarness {

    /** Morale is bounded in the game; sweeping past it would only invite a false fit. */
    private static final int MAX_MORAL = 100;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: FidelityHarness <pre-turn .egf> <hex> [spec file]");
            System.exit(2);
        }
        final CombatScenario scenario = load(new File(args[0]), args[1]);
        if (scenario == null) {
            System.out.println("ERROR|no such hex " + args[1]);
            System.exit(1);
        }
        if (args.length > 2) {
            applySpec(scenario, new File(args[2]));
        }
        report(scenario, new LandCombatResolver().resolve(scenario,
                scenario.getPartida() == null ? null : scenario.getPartida().getCenario()));
    }

    /** Builds the scenario exactly as the window does, so the harness cannot drift from the app. */
    private static CombatScenario load(File egf, String hex) throws Exception {
        final File xml = persistenceCommons.ZipManager.getInstance().doUncompressGzip(egf);
        final World world;
        try (InputStream is = new BufferedInputStream(new FileInputStream(xml));
                InputStreamReader reader = new InputStreamReader(is, "UTF-8")) {
            final XStream xs = new XStream();
            xs.allowTypesByWildcard(new String[]{"model.**"});
            world = (World) xs.fromXML(reader);
        }
        final Local local = world.getLocal(hex);
        return local == null ? null : new ScenarioLoader().load(world.getPartida(), local,
                world.getPartida() == null ? null : world.getPartida().getJogadorAtivo());
    }

    /**
     * Applies the turn's own tactics, and FITS each army's morale to the attack the Judge published.
     *
     * Fitting rather than assuming: morale is the only unknown left in a published platoon attack,
     * so the value that reproduces it is the value the army fought at. When no morale in range
     * reproduces it the nearest is used and the gap is printed - a miss here explains every
     * downstream difference and must not be swallowed.
     */
    private static void applySpec(CombatScenario scenario, File spec) throws Exception {
        final BattleSimFacade bsf = new BattleSimFacade();
        for (String raw : Files.readAllLines(spec.toPath(), StandardCharsets.UTF_8)) {
            final String line = raw.trim();
            if (!line.startsWith("army|")) {
                continue;
            }
            final String[] parts = line.split("\\|");
            final ArmySim army = find(scenario, parts[1]);
            if (army == null) {
                System.out.println("MISSING|" + parts[1]);
                continue;
            }
            Integer target = null;
            for (int i = 2; i < parts.length; i++) {
                final String[] kv = parts[i].split("=", 2);
                if ("tactic".equals(kv[0])) {
                    army.setTatica(Integer.parseInt(kv[1].trim()));
                } else if ("moral".equals(kv[0])) {
                    army.setMoral(Integer.parseInt(kv[1].trim()));
                } else if ("attack".equals(kv[0])) {
                    target = Integer.valueOf(kv[1].trim());
                }
            }
            if (target != null) {
                fitMoral(bsf, army, target);
            }
        }
    }

    private static void fitMoral(BattleSimFacade bsf, ArmySim army, int target) {
        int bestMoral = army.getMoral();
        int bestGap = Integer.MAX_VALUE;
        int bestAttack = 0;
        for (int moral = 0; moral <= MAX_MORAL; moral++) {
            army.setMoral(moral);
            final int attack = bsf.getArmyAttackBaseNot(army, ";TTN;", army.getLocal());
            final int gap = Math.abs(attack - target);
            if (gap < bestGap) {
                bestGap = gap;
                bestMoral = moral;
                bestAttack = attack;
            }
            if (gap == 0) {
                break;
            }
        }
        army.setMoral(bestMoral);
        System.out.println(String.format("FIT|%s|moral=%d|attack=%d|target=%d|gap=%d",
                army.getNome(), bestMoral, bestAttack, target, bestGap));
    }

    private static ArmySim find(CombatScenario scenario, String name) {
        for (ArmySim army : scenario.getArmies()) {
            if (army.getNome() != null && army.getNome().contains(name.trim())) {
                return army;
            }
        }
        return null;
    }

    /** Machine-readable, because the diff is done by the Python side against the turn text. */
    private static void report(CombatScenario scenario, CombatResult result) {
        System.out.println("ROUNDS|" + result.getRounds());
        for (String note : result.getNotes()) {
            System.out.println("NOTE|" + note);
        }
        for (ArmySim army : scenario.getArmies()) {
            System.out.println(String.format("ARMY|%s|%s|tactic=%d|moral=%d", army.getNome(),
                    result.getOutcome(army, CombatLayer.ARMY), army.getTatica(), army.getMoral()));
            for (Pelotao pelotao : army.getPelotoes().values()) {
                if (!result.has(pelotao)) {
                    continue;
                }
                System.out.println(String.format("PLATOON|%s|%s|%d|%d|%d", army.getNome(),
                        pelotao.getTipoTropa().getNome(), pelotao.getQtd(),
                        result.getAfter(pelotao), result.getLost(pelotao)));
            }
        }
    }

    // ------------------------------------------------------------------ regression

    private static final String SEAGARD =
            "//marte/Users/gurgel/Documents/Cpbm/Saves/901_GoT12c_901/007/"
            + "game_901_7.murazorgames.rr.egf";

    /**
     * THE KNOWN ANSWER. Game 901 turn 8, the battle at 1141 (Seagard).
     *
     * The Judge published Waldon Wynch on 518 survivors and Joron Blacktide on 523, and this pins
     * the resolver to both. Given the turn's own tactics (both Charge, from the orders - the EGF has
     * last turn's Flank and Standard) and Dorian's morale fitted to his published attack of 2,609,
     * the resolver has to reproduce them exactly. Skipped when the share is unreachable, so CI and
     * a machine without it are unaffected.
     */
    @Test
    public void reproducesSeagardExactly() throws Exception {
        final File egf = new File(SEAGARD);
        assumeTrue(egf.exists(), "EGF not reachable: " + SEAGARD);
        final CombatScenario scenario = load(egf, "1141");
        assumeTrue(scenario != null, "hex 1141 not in this EGF");

        final BattleSimFacade bsf = new BattleSimFacade();
        for (ArmySim army : scenario.getArmies()) {
            if (army.getNome().contains("Dorian")) {
                fitMoral(bsf, army, 2609);
            } else {
                army.setTatica(0);      // CA - Charge, as the turn's orders set it
            }
        }
        final CombatResult result = new LandCombatResolver().resolve(scenario,
                scenario.getPartida().getCenario());

        assertEquals(2, result.getRounds(), "the Judge fought two rounds");
        assertEquals(518, survivors(scenario, result, "Waldon"), "Waldon Wynch's Reavers");
        assertEquals(523, survivors(scenario, result, "Joron"), "Joron Blacktide's Reavers");
        assertEquals(0, survivors(scenario, result, "Dorian"), "Dorian Rivers was destroyed");
    }

    private static int survivors(CombatScenario scenario, CombatResult result, String who) {
        final List<Integer> found = new ArrayList<>();
        for (ArmySim army : scenario.getArmies()) {
            if (!army.getNome().contains(who)) {
                continue;
            }
            for (Pelotao pelotao : army.getPelotoes().values()) {
                if (result.has(pelotao)) {
                    found.add(result.getAfter(pelotao));
                }
            }
        }
        return found.size() == 1 ? found.get(0) : -1;
    }
}
