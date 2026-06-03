package model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import persistenceCommons.XmlManager;

import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that XmlManager can load EGF files across format vintages.
 * game_88 / game_96 are old files using the XStream 1.3.1 format
 * (tree-map alias, no-comparator elements). Loading them exercises the
 * backward-compat TreeMap/TreeSet converters and the readResolve machinery.
 */
class EgfLoadTest {

    private File fixture(String name) throws Exception {
        URL url = EgfLoadTest.class.getResource("/egf/" + name);
        assertNotNull(url, "Test fixture missing from test/resources/egf/: " + name);
        return new File(url.toURI());
    }

    // --- basic load ---

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"game_88_20.rr.egf", "game_96_6.rr.egf", "orders_88_21.alexandre.rc.egf"})
    void loadsWithoutException(String name) throws Exception {
        assertNotNull(XmlManager.getInstance().get(fixture(name)));
    }

    // --- World structure ---

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"game_88_20.rr.egf", "game_96_6.rr.egf"})
    void worldCollectionsNotNull(String name) throws Exception {
        World w = (World) XmlManager.getInstance().get(fixture(name));
        assertNotNull(w.getPartida(),    "partida null");
        assertNotNull(w.getNacoes(),     "nacoes null");
        assertNotNull(w.getCidades(),    "cidades null");
        assertNotNull(w.getExercitos(),  "exercitos null");
        assertNotNull(w.getLocais(),     "locais null");
        assertNotNull(w.getPersonagens(),"personagens null");
    }

    @Test
    void game88IdentityCheck() throws Exception {
        World w = (World) XmlManager.getInstance().get(fixture("game_88_20.rr.egf"));
        assertEquals(88,  w.getPartida().getId(),    "game id");
        assertEquals(20,  w.getPartida().getTurno(), "turn number");
        assertFalse(w.getNacoes().isEmpty(), "nacoes should not be empty");
    }

    // --- readResolve: field absent from old EGF gets default ---

    @Test
    void readResolveFiresOnOldFormat() throws Exception {
        // World.packages was added after game_88 was created — it is absent from the
        // file. Without the custom ReflectionConverter invoking readResolve(),
        // getPackages() would return null and crash the Counselor UI.
        World w = (World) XmlManager.getInstance().get(fixture("game_88_20.rr.egf"));
        assertNotNull(w.getPackages(),
                "World.packages is null — ReflectionConverter readResolve() did not fire");
    }

    // --- orders file ---

    @Test
    void ordersFileLoadsAsComando() throws Exception {
        Object result = XmlManager.getInstance().get(fixture("orders_88_21.alexandre.rc.egf"));
        assertInstanceOf(Comando.class, result);
        assertNotNull(((Comando) result).getJogadorNome());
    }
}
