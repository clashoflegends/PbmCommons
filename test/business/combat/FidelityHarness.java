package business.combat;

import business.facade.BattleSimFacade;
import business.facade.CenarioFacade;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import model.Local;
import model.Pelotao;
import model.TipoTropa;
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
        final List<String> lines = Files.readAllLines(spec.toPath(), StandardCharsets.UTF_8);
        // COMPOSITION FIRST, in its own pass. The army pass fits the commander skill against the
        // attack the Judge published, and that fit is meaningless until the army is holding the
        // right troops - run in file order it fitted the placeholder composition and reported a
        // 97% miss that looked like a resolver fault.
        final Set<ArmySim> cleared = new HashSet<>();
        for (String raw : lines) {
            if (raw.trim().startsWith("platoon|")) {
                applyPlatoon(scenario, raw.trim(), cleared);
            }
        }
        for (String raw : lines) {
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
                } else if ("commander".equals(kv[0])) {
                    army.setComandante(Integer.parseInt(kv[1].trim()));
                } else if ("plus".equals(kv[0])) {
                    // forcaPlus the client cannot see: an ENEMY army's dragon or combat artifact.
                    // Supplying it is the point - it separates "the model is wrong" from "the
                    // player could not know", and only the first is a defect.
                    army.setBonusAttack(Integer.parseInt(kv[1].trim()));
                } else if ("attack".equals(kv[0])) {
                    target = Integer.valueOf(kv[1].trim());
                }
            }
            if (target != null) {
                fitBonus(bsf, army, target);
            }
        }
    }

    /**
     * Fits the army bonus to the attack the Judge published.
     *
     * The bonus is {@code (commander skill + morale + 200) / 4}, so only the SUM of the two is
     * observable in the attack - which is why this sweeps the sum rather than morale alone. Morale
     * is the part that moves mid-turn (a refused challenge is worth {@code rand(10)+5}), but the
     * commander's skill moves too, from the experience he earns in the very battle being fought, and
     * pinning morale at its ceiling while the real gap was in the skill left a 1.5% error that the
     * by-rank casualty rule then concentrated entirely into the last platoon in the order.
     */
    private static void fitBonus(BattleSimFacade bsf, ArmySim army, int target) {
        int bestSum = army.getComandantePericia() + army.getMoral();
        int bestGap = Integer.MAX_VALUE;
        int bestAttack = 0;
        for (int sum = 0; sum <= 2 * MAX_MORAL; sum++) {
            army.setComandante(Math.min(sum, MAX_MORAL));
            army.setMoral(sum - Math.min(sum, MAX_MORAL));
            final int attack = bsf.getArmyAttackBaseNot(army, ";TTN;", army.getLocal());
            final int gap = Math.abs(attack - target);
            if (gap < bestGap) {
                bestGap = gap;
                bestSum = sum;
                bestAttack = attack;
            }
            if (gap == 0) {
                break;
            }
        }
        army.setComandante(Math.min(bestSum, MAX_MORAL));
        army.setMoral(bestSum - Math.min(bestSum, MAX_MORAL));
        System.out.println(String.format("FIT|%s|bonus=%d|attack=%d|target=%d|gap=%d",
                army.getNome(), bestSum, bestAttack, target, bestGap));
    }

    /**
     * Replaces an army's composition with the one the JUDGE recorded. The point of the harness.
     *
     * An EGF is one player's view and an unscouted enemy arrives as a placeholder, so validating
     * against it is guaranteed to mismatch and proves nothing about the resolver. The question worth
     * answering is the other one: GIVEN THE RIGHT INPUTS, does the simulator reproduce the turn? So
     * the composition comes from `ex_pelotao` - quantity, training, weapon and armour per platoon -
     * which is exactly what a player does by hand when he knows what he is facing.
     *
     * <pre>
     *   platoon|Elston Stone|clarryn2|485|25|100|100
     *                        ^troop   ^qty ^treino ^modAttack ^modDefence
     * </pre>
     *
     * The first platoon line for an army CLEARS what the EGF gave it; the rest add to that.
     */
    private static void applyPlatoon(CombatScenario scenario, String line, Set<ArmySim> cleared) {
        final String[] p = line.split("\\|");
        final ArmySim army = find(scenario, p[1]);
        if (army == null) {
            System.out.println("MISSING|" + p[1]);
            return;
        }
        final TipoTropa tipo = troopType(scenario, p[2].trim());
        if (tipo == null) {
            System.out.println("NOTROOP|" + p[2]);
            return;
        }
        if (cleared.add(army)) {
            army.getPelotoes().clear();
        }
        final Pelotao pelotao = new Pelotao();
        pelotao.setTipoTropa(tipo);
        pelotao.setQtd(Integer.parseInt(p[3].trim()));
        pelotao.setTreino(Integer.parseInt(p[4].trim()));
        pelotao.setModAtaque(Integer.parseInt(p[5].trim()));
        pelotao.setModDefesa(Integer.parseInt(p[6].trim()));
        army.getPelotoes().put(pelotao.getCodigo(), pelotao);
    }

    /** The scenario's own troop catalogue, keyed the way {@code ex_tipo_tropa.cd_tropa} keys it. */
    private static TipoTropa troopType(CombatScenario scenario, String codigo) {
        if (scenario.getPartida() == null || scenario.getPartida().getCenario() == null) {
            return null;
        }
        for (TipoTropa tipo : new CenarioFacade().getTipoTropas(scenario.getPartida().getCenario())) {
            if (codigo.equalsIgnoreCase(tipo.getCodigo())) {
                return tipo;
            }
        }
        return null;
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
        // round by round, so a divergence is located in the round it STARTS in rather than being
        // read off a total five rounds later
        for (CombatResult.RoundDamage hit : result.getRoundDamage()) {
            System.out.println(String.format("DMG|%d|%s|%s|%d|%d", hit.getRound(),
                    hit.getAttacker().getNome(), hit.getDefender().getNome(),
                    hit.getAttack(), hit.getDamage()));
        }
        for (CombatResult.RoundLoss loss : result.getRoundLosses()) {
            System.out.println(String.format("LOSS|%d|%s|%s|%d|%d", loss.getRound(),
                    loss.getArmy().getNome(), loss.getPlatoon().getTipoTropa().getNome(),
                    loss.getLost(), loss.getLeft()));
        }
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
                fitBonus(bsf, army, 2609);
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
