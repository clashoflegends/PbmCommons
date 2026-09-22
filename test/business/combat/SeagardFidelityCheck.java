package business.combat;

import business.facade.BattleSimFacade;
import business.facade.CenarioFacade;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import model.Local;
import model.World;
import msgs.BaseMsgs;
import org.junit.jupiter.api.Test;

import com.thoughtworks.xstream.XStream;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Prints the simulator's INPUTS for one real battle, beside the Judge's published ones.
 *
 * <h3>Why this is a printer and not an assertion</h3>
 *
 * A forecast that disagrees with the turn has exactly two possible causes: the loop is wrong, or the
 * loop is right and was handed different numbers. Chasing the second as though it were the first is
 * how an afternoon disappears. This dumps what the simulator believes about the armies - attack,
 * defence, tactic, relationship - so it can be read against the Judge's own output line by line, and
 * the question is settled before any code is touched.
 *
 * <h3>Battle of Seagard, hex 1141, game 901 turn 8</h3>
 *
 * The Judge's numbers, from {@code 08_04_DB.NACAO.NOME.GREYJOY.murazorgames.htm}:
 *
 * <pre>
 *   Waldon Wynch   778 Marines   A 2,415  D 7,780   morale band 05
 *   Joron Blacktide 786 Marines  A 2,350  D 7,860   morale band 06
 *   Dorian Rivers  500 Archers   A 2,609  D 5,000   morale band 00
 *   round 0: Dorian ataqueFinal 2608 -> 1297 on Waldon, 1310 on Joron; nobody else swings
 *   round 1: Dorian 2608 -> 1296 / 1311; Waldon 2764 and Joron 2691 -> Dorian destroyed
 *   survivors: Waldon 518, Joron 523
 * </pre>
 *
 * SKIPPED when the EGF is not reachable, so it never breaks CI or a machine without the share.
 */
public class SeagardFidelityCheck {

    private static final String EGF =
            "//marte/Users/gurgel/Documents/Cpbm/Saves/901_GoT12c_901/007/"
            + "game_901_7.murazorgames.rr.egf";
    private static final String HEX = "1141";

    @Test
    public void printWhatTheSimulatorBelievesAboutSeagard() throws Exception {
        final File file = new File(EGF);
        assumeTrue(file.exists(), "EGF not reachable: " + EGF);

        final World world = (World) load(file);
        final Local local = world.getLocal(HEX);
        assumeTrue(local != null, "hex " + HEX + " not in this EGF");

        final CombatScenario scenario = new ScenarioLoader().load(world.getPartida(), local,
                world.getPartida().getJogadorAtivo());
        final BattleSimFacade bsf = new BattleSimFacade();
        final CenarioFacade cf = new CenarioFacade();
        final RelationshipMatrix relations = scenario.getRelationships();

        System.out.println("=== " + HEX + " terrain=" + name(scenario.getTerreno())
                + " city=" + (scenario.getCidade() == null ? "none"
                        : scenario.getCidade().getNome()));
        for (ArmySim army : scenario.getArmies()) {
            System.out.println(String.format(
                    "%-22s nation=%-12s tactic=%d morale=%3d cmdr=%3d  A=%,7d D=%,7d  layers=%s",
                    army.getNome(), name(army.getNacao()), army.getTatica(), army.getMoral(),
                    army.getComandantePericia(),
                    bsf.getArmyAttackBaseNot(army, ";TTN;", army.getLocal()),
                    bsf.getArmyDefenseTotalLand(army),
                    scenario.getParticipation().get(army)));
            System.out.println("    firstStrike(round 0) A="
                    + bsf.getArmyAttackBase(army, ";TT1;", army.getLocal()));
        }
        System.out.println("--- pairwise modifiers (attacker -> defender) ---");
        for (ArmySim from : scenario.getArmies()) {
            for (ArmySim to : scenario.getArmies()) {
                if (from == to || from.getNacao() == to.getNacao()) {
                    continue;
                }
                final int valor = relations.getValor(from.getNacao(), to.getNacao());
                System.out.println(String.format(
                        "%-22s -> %-22s rel=%2d modRel=%3d modTatica=%3d hostile=%s",
                        from.getNome(), to.getNome(), valor,
                        100 - BaseMsgs.dificuldadeBonus[valor + 3],
                        cf.getTaticaBonus(scenario.getPartida().getCenario(), from.getTatica(),
                                to.getTatica()),
                        scenario.getMatrix().isInimigo(from, to)));
            }
        }
        run(scenario, "AS THE EGF HAS IT");

        // and again with the tactics the turn actually ran with. The EGF carries LAST turn's
        // tactic; Charge came from the turn-8 orders, which are not in it.
        for (ArmySim army : scenario.getArmies()) {
            if (!army.getNome().contains("Dorian")) {
                army.setTatica(0);      // CA, Charge
            }
        }
        run(scenario, "WITH THE TACTICS THE TURN ACTUALLY USED (both Greyjoy on Charge)");

        // The Judge's own deploy listing for this battle shows ONE platoon per army - the land one.
        // displayTipoTropa prints every platoon when the combat is not naval, so the fleets were
        // genuinely not with the armies by the time they fought. The troop-share split that decides
        // how the attack is divided counts every platoon, so a fleet still attached in the EGF moves
        // the numbers. Third pass: what the sim says once the ships are out of the way.
        for (ArmySim army : scenario.getArmies()) {
            army.getPelotoes().values().removeIf(p -> p.getTipoTropa().isBarcos());
        }
        run(scenario, "AND WITHOUT THE FLEETS, WHICH THE JUDGE DID NOT HAVE ON THE HEX");
    }

    private void run(CombatScenario scenario, String title) {
        final CombatResult result = new LandCombatResolver().resolve(scenario,
                scenario.getPartida().getCenario());
        System.out.println("--- " + title + ": " + result.getRounds() + " rounds ---");
        for (ArmySim army : scenario.getArmies()) {
            for (model.Pelotao pelotao : army.getPelotoes().values()) {
                if (pelotao.getTipoTropa().isBarcos()) {
                    continue;
                }
                System.out.println(String.format("    %-18s %-12s %,6d -> %,6d (lost %,6d)  %s",
                        army.getNome(), pelotao.getTipoTropa().getNome(), pelotao.getQtd(),
                        result.getAfter(pelotao), result.getLost(pelotao),
                        result.getOutcome(army, CombatLayer.ARMY)));
            }
        }
    }

    private static String name(Object model) {
        return model == null ? "null" : String.valueOf(model);
    }

    private static Object load(File egf) throws Exception {
        final File xml = persistenceCommons.ZipManager.getInstance().doUncompressGzip(egf);
        try (InputStream is = new BufferedInputStream(new FileInputStream(xml));
                InputStreamReader reader = new InputStreamReader(is, "UTF-8")) {
            final XStream xs = new XStream();
            xs.allowTypesByWildcard(new String[]{"model.**"});
            return xs.fromXML(reader);
        }
    }
}
