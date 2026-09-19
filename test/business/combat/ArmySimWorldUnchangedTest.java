package business.combat;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import model.Exercito;
import model.Pelotao;
import model.World;
import org.junit.jupiter.api.Test;
import persistenceCommons.XmlManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * The end-to-end half of {@link ArmySimOwnershipTest}: load a real EGF, run the edits a player would
 * make inside the BattleSim, and prove the loaded world is untouched afterwards.
 *
 * The unit test pins the contract on a hand-built fixture. This one pins it on an actual saved game,
 * because the defect it guards against was invisible precisely at this level: every ArmySim was built
 * from an Exercito that came out of an EGF, and the shared Pelotao objects meant a simulation edited
 * the army the player could then see on the map, in the Armies tab and in order composition.
 *
 * Fixture is the same committed game_88_20 used by EgfLoadTest. No live player data.
 */
public class ArmySimWorldUnchangedTest {

    private File fixture(String name) throws Exception {
        URL url = ArmySimWorldUnchangedTest.class.getResource("/egf/" + name);
        assertNotNull(url, "Test fixture missing from test/resources/egf/: " + name);
        return new File(url.toURI());
    }

    /** A snapshot of every editable platoon value in the world, in a stable order. */
    private static List<String> snapshotPlatoons(World world) {
        final List<String> ret = new ArrayList<>();
        for (Exercito army : world.getExercitos().values()) {
            for (Pelotao pelotao : army.getPelotoes().values()) {
                ret.add(String.format("%s|%s|%s|%d|%d|%d|%d",
                        army.getCodigo(),
                        pelotao.getCodigo(),
                        pelotao.getTipoTropa().getCodigo(),
                        pelotao.getQtd(),
                        pelotao.getTreino(),
                        pelotao.getModAtaque(),
                        pelotao.getModDefesa()));
            }
        }
        return ret;
    }

    @Test
    void aFullSimulationSessionLeavesTheLoadedWorldUntouched() throws Exception {
        final World world = (World) XmlManager.getInstance().get(fixture("game_88_20.rr.egf"));
        final List<String> before = snapshotPlatoons(world);
        assertFalse(before.isEmpty(), "fixture has no platoons, so this test would prove nothing");

        int simulatedArmies = 0;
        for (Exercito army : world.getExercitos().values()) {
            if (army.getPelotoes().isEmpty() || army.getLocal() == null) {
                continue;
            }
            // what the BattleSim does on open, then what a player does to it
            final ArmySim sim = new ArmySim(army);
            for (Pelotao pelotao : sim.getPelotoes().values()) {
                assertNotSame(army.getPelotoes().get(pelotao.getCodigo()), pelotao,
                        "simulated platoon is the same object as the one in the loaded world");
                pelotao.setQtd(pelotao.getQtd() + 1234);
                pelotao.setTreino(7);
                pelotao.setModAtaque(11);
                pelotao.setModDefesa(13);
            }
            // and what "clone army" does
            final ArmySim copy = new ArmySim(sim);
            for (Pelotao pelotao : copy.getPelotoes().values()) {
                pelotao.setQtd(1);
            }
            simulatedArmies++;
        }

        assertFalse(simulatedArmies == 0, "no army was simulated, so this test would prove nothing");
        assertEquals(before, snapshotPlatoons(world),
                "the BattleSim wrote back into the loaded world");
    }
}
