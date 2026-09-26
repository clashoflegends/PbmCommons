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
import java.util.Arrays;
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
        // THE CHAIN, not the land resolver alone: a city assault only resolves if the layers run
        // in the Judge's order on one set of copies.
        final CombatResult result = new CombatChain().resolve(scenario,
                scenario.getPartida() == null ? null : scenario.getPartida().getCenario());
        report(scenario, result);
        reportCity(scenario, result);
    }

    /**
     * The city layer's numbers, in the shape the Judge publishes them, so a turn report can be read
     * straight down beside this output.
     *
     * The three lines to compare are {@code COMBATE.FEZ.DANO.CIDADE.ATAQUE} (what each attacker
     * inflicted), {@code COMBATE.FEZ.DANO.CIDADE.DEFESA} (what the city inflicted back) and the
     * casualties under each. {@code isShowCombatValues()} returns true unconditionally, so every
     * live game publishes them.
     */
    private static void reportCity(CombatScenario scenario, CombatResult result) {
        final CityCombatResolver.CityResult city = result.getCityResult();
        if (city == null || city.getAttackers().isEmpty()) {
            System.out.println("CITY|no assault");
            return;
        }
        System.out.println("CITY|" + city.getOutcome()
                + "|defence=" + city.getDefence()
                + "|attackTotal=" + city.getAttackTotal()
                + "|fortificationLost=" + city.getFortificationReduction()
                + "|raze=" + city.isRaze()
                + "|owner=" + (city.getOwner() == null ? "-" : city.getOwner().getNome()));
        for (ArmySim army : city.getAttackers()) {
            System.out.println("CITYATTACK|" + army.getNome()
                    + "|attack=" + city.getAttack(army)
                    + "|siege=" + city.getSiegeAttack(army)
                    + "|tookDamage=" + city.getDamage(army));
        }
        for (CombatResult.RoundLoss loss : result.getRoundLosses()) {
            System.out.println("CITYLOSS|" + loss.getArmy().getNome()
                    + "|" + (loss.getPlatoon().getTipoTropa() == null
                            ? "?" : loss.getPlatoon().getTipoTropa().getNome())
                    + "|lost=" + loss.getLost() + "|left=" + loss.getLeft()
                    + "|round=" + loss.getRound());
        }
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
    /**
     * {@code war|<nation>|<nation>} - the player's own declaration, both directions.
     *
     * Named by nation, not by army, because that is what diplomacy IS: the Judge reads
     * {@code getNacaoControl().isInimigo(...)} and no property of the army takes part. Matching is
     * on the nation's NAME as the roster prints it, case-insensitive, so a spec is readable next to
     * a turn report.
     */
    /**
     * {@code city|tamanho=3|fortificacao=2|lealdade=52|docas=0} - the city as the DB holds it at
     * turn N-1.
     *
     * The city is the input a player is least able to supply from his own results: size,
     * fortification and loyalty all feed {@code getCityDefenseCombat}, and for an enemy city all
     * three are filtered. Summerhall at 866 t1 is the worked example - the EGF gave a defense of
     * 14,000 where the Judge used 10,640, and every downstream number moved with it.
     */
    private static void applyCity(CombatScenario scenario, String line) {
        final model.Cidade city = scenario.getCidade();
        if (city == null) {
            System.out.println("NOCITY|" + line);
            return;
        }
        for (String part : line.split("\\|")) {
            final String[] kv = part.split("=", 2);
            if (kv.length < 2) {
                continue;
            }
            final int value = Integer.parseInt(kv[1].trim());
            if ("tamanho".equals(kv[0])) {
                city.setTamanho(value);
            } else if ("fortificacao".equals(kv[0])) {
                city.setFortificacao(value);
            } else if ("lealdade".equals(kv[0])) {
                city.setLealdade(value);
            } else if ("docas".equals(kv[0])) {
                city.setDocas(value);
            }
        }
        System.out.println("CITYIN|" + city.getNome()
                + "|tamanho=" + city.getTamanho()
                + "|fortificacao=" + city.getFortificacao()
                + "|lealdade=" + city.getLealdade()
                + "|docas=" + city.getDocas());
    }

    private static void applyWar(CombatScenario scenario, String line) {
        final String[] parts = line.split("\\|");
        if (parts.length < 3) {
            System.out.println("BADWAR|" + line);
            return;
        }
        final model.Nacao one = findNacao(scenario, parts[1]), two = findNacao(scenario, parts[2]);
        if (one == null || two == null) {
            System.out.println("NONATION|" + (one == null ? parts[1] : parts[2]));
            return;
        }
        scenario.setRelacionamento(one, two, RelationshipMatrix.SWORN_ENEMY);
        scenario.setRelacionamento(two, one, RelationshipMatrix.SWORN_ENEMY);
        System.out.println("WAR|" + one.getNome() + "|" + two.getNome());
    }

    private static model.Nacao findNacao(CombatScenario scenario, String nome) {
        for (model.Nacao one : scenario.getNacoes()) {
            if (one != null && one.getNome() != null
                    && one.getNome().trim().equalsIgnoreCase(nome.trim())) {
                return one;
            }
        }
        return null;
    }

    private static void applySpec(CombatScenario scenario, File spec) throws Exception {
        applySpec(scenario, Files.readAllLines(spec.toPath(), StandardCharsets.UTF_8));
    }

    /**
     * The same spec, held in the test rather than in a file beside it.
     *
     * A known-answer test IS its spec - the turn N-1 numbers read off the Judge are the input the
     * assertion is about - so keeping them in the method keeps the claim and its evidence together,
     * and stops a test passing because somebody edited a file it does not name.
     */
    private static void applySpec(CombatScenario scenario, List<String> lines) throws Exception {
        final BattleSimFacade bsf = new BattleSimFacade();
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
        // DECLARATIONS next, because participation is derived from the matrix and everything after
        // this reads it. "war|A|B" is exactly what the player does in the Diplomacy panel, and in a
        // free-for-all it is the only way a battle between two FOREIGN nations can be resolved at
        // all: neither row is readable, so the pair is assumed neutral and nothing engages.
        for (String raw : lines) {
            final String line = raw.trim();
            if (line.startsWith("war|")) {
                applyWar(scenario, line);
            }
        }
        // THE CITY, from the DB and not the EGF. An enemy city's size, fortification and loyalty
        // are what the OWNER knows; what the player sees is filtered, and the defense is built из
        // all three. Asserting them is the city half of the validation loop: turn N-1 state for
        // armies AND cities from the Judge or the DB, simulate, compare against turn N.
        for (String raw : lines) {
            final String line = raw.trim();
            if (line.startsWith("city|")) {
                applyCity(scenario, line);
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
                } else if ("level".equals(kv[0])) {
                    // Combat intent does NOT ride the EGF - combateNivel lives on the Judge's
                    // ExercitoControl - so every army loads at ATTACK_ARMY and a city assault can
                    // only be reproduced by asserting what the player actually ordered.
                    // 0 defend, 1 attack armies, 2 attack city, 3 raze.
                    for (CombatLevel one : CombatLevel.values()) {
                        if (one.getNivel() == Integer.parseInt(kv[1].trim())) {
                            army.setCombatLevel(one);
                        }
                    }
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
        // PER LAYER. The total is the answer to "did anything happen"; the Judge publishes a
        // land round count and a city assault separately, and a diff against one summed number
        // reads a land+city hex as one round too many.
        for (CombatLayer layer : CombatLayer.values()) {
            System.out.println("ROUNDS|" + layer.name() + "|" + result.getRounds(layer));
        }
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

    /**
     * The saves root, from {@code -Dclash.saves} or the {@code CLASH_SAVES} environment variable.
     *
     * NOT a path in the source. This is a PUBLIC repository, and a hard-coded save location names
     * the maintainer's machine and his user directory; the file names under it name the players.
     * Unset means the known-answer tests skip, which is what a contributor without the archive
     * should get - and skipping on the property rather than on {@code File.exists} also means a
     * machine off that network never touches a share that is not there, which on Windows is a
     * multi-second stall in the middle of the suite, not a quick false.
     */
    private static String savesRoot() {
        final String ret = System.getProperty("clash.saves", System.getenv("CLASH_SAVES"));
        return ret == null || ret.trim().isEmpty() ? null : ret.trim();
    }

    /**
     * The pre-turn scenario for one hex, from whichever player's EGF shows all of {@code mustSee}.
     *
     * By SEARCH rather than by file name, because this is a public repository and the EGF names
     * carry the players' logins. The search is not "any copy that has the hex", though: an EGF is
     * one player's intelligence, and most of the copies that contain 1660 show an empty hex or a
     * placeholder army. The armies the spec names are the ones the assertions are about, so the
     * right copy is the first one that can see them all - which is a property of the battle, not
     * of who happened to be watching it.
     */
    private static CombatScenario loadFromAnyPlayer(String game, String turn, String hex,
            String... mustSee) throws Exception {
        final String root = savesRoot();
        assumeTrue(root != null,
                "set -Dclash.saves=<Saves folder> to run the known-answer regressions");
        final File dir = new File(new File(root, game), turn);
        assumeTrue(dir.isDirectory(), "turn folder not reachable: " + dir);
        final File[] files = dir.listFiles();
        assumeTrue(files != null, "turn folder unreadable: " + dir);
        Arrays.sort(files);
        for (File egf : files) {
            if (!egf.getName().endsWith(".rr.egf")) {
                continue;
            }
            final CombatScenario ret = load(egf, hex);
            if (ret != null && showsAll(ret, mustSee)) {
                return ret;
            }
        }
        return null;
    }

    /**
     * Whether this copy shows those armies' COMPOSITION, not merely their names.
     *
     * The distinction is the whole fog of war: an unscouted enemy arrives in the EGF as a name and
     * a size band with ZERO platoons, so a copy that "has" both armies can still have nothing to
     * fight with. Searching on the name alone picked exactly such a copy and the assault reported
     * NO_ASSAULT - the right answer to the question that copy could actually ask.
     */
    private static boolean showsAll(CombatScenario scenario, String... names) {
        for (String name : names) {
            final ArmySim army = find(scenario, name);
            if (army == null || army.getPelotoes().isEmpty()) {
                return false;
            }
        }
        return names.length > 0;
    }

    /**
     * THE KNOWN ANSWER. Game 901 turn 8, the battle at 1141 (Seagard).
     *
     * The Judge published Waldon Wynch on 518 survivors and Joron Blacktide on 523, and this pins
     * the resolver to both. Given the turn's own tactics (both Charge, from the orders - the EGF has
     * last turn's Flank and Standard) and Dorian's morale fitted to his published attack of 2,609,
     * the resolver has to reproduce them exactly. Skipped unless the saves root is configured,
     * so CI and a machine without the archive are unaffected.
     */
    @Test
    public void reproducesSeagardExactly() throws Exception {
        final CombatScenario scenario = loadFromAnyPlayer("901_GoT12c_901", "007", "1141",
                "Waldon", "Joron", "Dorian");
        assumeTrue(scenario != null, "hex 1141 in no EGF of that turn");

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

        assertEquals(2, result.getRounds(CombatLayer.ARMY), "the Judge fought two rounds");
        assertEquals(518, survivors(scenario, result, "Waldon"), "Waldon Wynch's Reavers");
        assertEquals(523, survivors(scenario, result, "Joron"), "Joron Blacktide's Reavers");
        assertEquals(0, survivors(scenario, result, "Dorian"), "Dorian Rivers was destroyed");
    }

    /**
     * THE KNOWN ANSWER FOR THE CITY LAYER. Game 866 turn 2, the storming of Summerhall at 1660.
     *
     * Two armies with no land battle between them - they are allies - assault a city and raze it,
     * which is the shape the land resolver alone cannot produce and the whole reason the chain
     * exists. The Judge published every number this asserts, and the inputs are the turn 1 state
     * read off the DB rather than off the EGF: the city's own size, fortification and loyalty are
     * not exported to an enemy, and the tactic is the one the turn 2 orders carried, not the turn
     * 1 one the EGF still holds.
     *
     * Skipped unless the saves root is configured, so CI and a machine without the archive
     * are unaffected.
     */
    @Test
    public void reproducesSummerhallExactly() throws Exception {
        final CombatScenario scenario = loadFromAnyPlayer("866_GoT12c_866", "001", "1660",
                "Garth Tyrell", "Quentyn Martell");
        assumeTrue(scenario != null, "hex 1660 in no EGF of that turn");
        applySpec(scenario, Arrays.asList(
                "city|tamanho=3|fortificacao=2|lealdade=52|docas=0",
                "army|Garth Tyrell|level=2|tactic=0",
                "army|Quentyn Martell|level=3|tactic=0"));

        final CombatResult result = new CombatChain().resolve(scenario,
                scenario.getPartida().getCenario());
        final CityCombatResolver.CityResult city = result.getCityResult();

        assertEquals(CityCombatResolver.CityOutcome.RAZED, city.getOutcome(),
                "the Judge razed Summerhall");
        assertEquals(10640, city.getDefence(), "the walls");
        assertEquals(13881, city.getAttackTotal(), "and what came at them");
        assertEquals(1, result.getRounds(), "an assault with no land battle is still ONE round");
        assertEquals(8006, attackOn(city, "Garth"), "Garth Tyrell's attack");
        assertEquals(5875, attackOn(city, "Quentyn"), "Quentyn Martell's attack");
        assertEquals(6013, damageOn(city, "Garth"), "what the walls did to Garth");
        assertEquals(4626, damageOn(city, "Quentyn"), "and to Quentyn");
        assertEquals(336, cityLoss(result, "Garth"), "Heavy Infantry lost at the walls");
        assertEquals(309, cityLoss(result, "Quentyn"), "Sand Cavalry lost at the walls");
    }

    private static long attackOn(CityCombatResolver.CityResult city, String who) {
        for (ArmySim army : city.getAttackers()) {
            if (army.getNome().contains(who)) {
                return city.getAttack(army);
            }
        }
        return -1;
    }

    private static long damageOn(CityCombatResolver.CityResult city, String who) {
        for (ArmySim army : city.getAttackers()) {
            if (army.getNome().contains(who)) {
                return city.getDamage(army);
            }
        }
        return -1;
    }

    /** What that army lost IN THE CITY LAYER, so a land casualty cannot be counted here. */
    private static int cityLoss(CombatResult result, String who) {
        int ret = 0;
        for (CombatResult.RoundLoss loss : result.getRoundLosses()) {
            if (loss.getLayer() == CombatLayer.CITY && loss.getArmy().getNome().contains(who)) {
                ret += loss.getLost();
            }
        }
        return ret;
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
