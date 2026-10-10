package business.facade;

import java.util.SortedMap;
import java.util.TreeMap;
import model.Cidade;
import model.Habilidade;
import model.Local;
import model.Nacao;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@code isCheckCitySizeCapToUpgrade} is the {@code ;SCC;} proximity cap: a town cannot become a big
 * city within 2 hexes of another big city, and a big city cannot become a metropolis within 3 hexes of
 * another metropolis. The Judge calls it from the upgrade order AND from natural loyalty growth; the
 * Counselor now calls the same method to mark capped cities in the cities tab, so a disagreement
 * between the two would show the player a cap the Judge does not apply, or hide one it does.
 *
 * The exemptions are the whole reason this has a test. Game 915 turn 6 refused to grow Nightfort
 * because the Night's Watch Wall was missing its {@code ;LKW;}/{@code ;PKW;} pair and the castles sit
 * one hex apart by design, so every Wall castle was permanently frozen. That was a data defect, but it
 * only surfaced because nothing exercised the exemption.
 *
 * Hexes are keyed "CCRR" the way the EGF keys them, and the grid is built with the real neighbour walk
 * ({@code LocalFacade.getIdentificacaoVizinho}) rather than by hand, so a distance here means the same
 * thing it means on the map.
 */
class CitySizeCapTest {

    private final CidadeFacade facade = new CidadeFacade();

    /** A rectangular patch of map big enough for a radius-3 walk around the centre. */
    private static SortedMap<String, Local> grid() {
        final SortedMap<String, Local> ret = new TreeMap<>();
        for (int col = 10; col <= 20; col++) {
            for (int row = 10; row <= 20; row++) {
                final String code = String.format("%02d%02d", col, row);
                final Local local = new Local();
                local.setCodigo(code);
                local.setCoordenadas(code);
                ret.put(code, local);
            }
        }
        return ret;
    }

    private static Nacao nacao(String codigo) {
        final Nacao ret = new Nacao();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        return ret;
    }

    private static Habilidade habilidade(String codigo) {
        final Habilidade ret = new Habilidade();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        return ret;
    }

    /** Drops a city of {@code size} belonging to {@code dono} on hex {@code code}. */
    private static Cidade city(SortedMap<String, Local> grid, String code, int size, Nacao dono) {
        final Cidade ret = new Cidade();
        ret.setCodigo("c" + code);
        ret.setNome("city" + code);
        ret.setTamanho(size);
        ret.setNacao(dono);
        ret.setLocal(grid.get(code));
        return ret;
    }

    /** The hex {@code steps} tiles due east of {@code code} - direction 3 is a pure column step. */
    private static String east(String code, int steps) {
        final LocalFacade lf = new LocalFacade();
        final Local probe = new Local();
        probe.setCodigo(code);
        String ret = code;
        for (int ii = 0; ii < steps; ii++) {
            probe.setCodigo(ret);
            ret = lf.getIdentificacaoVizinho(probe, 3);
        }
        return ret;
    }

    @Test
    void aTownIsBlockedByABigCityTwoHexesAway() {
        final SortedMap<String, Local> grid = grid();
        final Nacao mine = nacao("mine");
        final Cidade town = city(grid, "1515", 3, mine);
        city(grid, east("1515", 2), 4, nacao("theirs"));

        final Local blocker = facade.isCheckCitySizeCapToUpgrade(town, grid);
        assertNotNull(blocker, "a big city 2 hexes away caps a town");
        assertEquals(east("1515", 2), blocker.getCoordenadas(),
                "the rule must hand back the blocking hex, which is what the player is told");
    }

    @Test
    void aTownIsClearOfABigCityThreeHexesAway() {
        final SortedMap<String, Local> grid = grid();
        final Cidade town = city(grid, "1515", 3, nacao("mine"));
        city(grid, east("1515", 3), 4, nacao("theirs"));

        assertNull(facade.isCheckCitySizeCapToUpgrade(town, grid), "range for a town is 2, not 3");
    }

    @Test
    void aBigCityReachesThreeHexesAndOnlyAMetropolisBlocksIt() {
        final SortedMap<String, Local> grid = grid();
        final Cidade burgh = city(grid, "1515", 4, nacao("mine"));
        //another big city, however close, is not a metropolis and cannot cap this one
        city(grid, east("1515", 1), 4, nacao("theirs"));
        assertNull(facade.isCheckCitySizeCapToUpgrade(burgh, grid),
                "growing to metropolis is capped by metropolises, not by big cities");

        city(grid, east("1515", 3), 5, nacao("theirs"));
        assertNotNull(facade.isCheckCitySizeCapToUpgrade(burgh, grid),
                "a metropolis 3 hexes away caps a big city");
    }

    @Test
    void sizesOutsideThreeAndFourAreNeverCapped() {
        final SortedMap<String, Local> grid = grid();
        final Nacao theirs = nacao("theirs");
        city(grid, east("1515", 1), 5, theirs);

        for (int size : new int[]{0, 1, 2, 5}) {
            final Cidade other = city(grid, "1515", size, nacao("mine"));
            assertNull(facade.isCheckCitySizeCapToUpgrade(other, grid),
                    "only towns and big cities are candidates, size " + size + " is not");
        }
    }

    @Test
    void aCapitalIsNeverCapped() {
        final SortedMap<String, Local> grid = grid();
        final Nacao mine = nacao("mine");
        final Cidade town = city(grid, "1515", 3, mine);
        town.setCapital(true);
        city(grid, east("1515", 1), 4, nacao("theirs"));

        assertNull(facade.isCheckCitySizeCapToUpgrade(town, grid), "capitals are exempt by rule");
    }

    /**
     * The game-915 case. The Wall castles sit one hex apart on purpose, so the Night's Watch is exempt
     * wherever the hex carries {@code ;LKW;} AND the nation carries {@code ;PKW;}. Both halves are
     * required: the missing one in 915 was {@code ;PKW;} on the nation, and that is enough to freeze
     * every castle.
     */
    @Test
    void theNightsWatchWallIsExemptOnlyWithBothFlags() {
        final SortedMap<String, Local> grid = grid();
        final Nacao watch = nacao("watch");
        final Cidade nightfort = city(grid, "1515", 3, watch);
        city(grid, east("1515", 1), 5, nacao("theirs"));

        assertNotNull(facade.isCheckCitySizeCapToUpgrade(nightfort, grid),
                "with neither flag the Wall is capped like anywhere else - the 915 defect");

        nightfort.getLocal().addHabilidade(habilidade(";LKW;"));
        assertNotNull(facade.isCheckCitySizeCapToUpgrade(nightfort, grid),
                ";LKW; on the hex alone must not exempt - the nation half is what 915 was missing");

        watch.addHabilidade(habilidade(";PKW;"));
        assertNull(facade.isCheckCitySizeCapToUpgrade(nightfort, grid),
                ";LKW; hex plus ;PKW; nation is the Wall exemption");
    }

    /** {@code ;PIC;} shortens the range by its value - the Greyjoy bonus. */
    @Test
    void thePicBonusShortensTheRange() {
        final SortedMap<String, Local> grid = grid();
        final Nacao iron = nacao("iron");
        final Cidade town = city(grid, "1515", 3, iron);
        city(grid, east("1515", 2), 4, nacao("theirs"));

        assertNotNull(facade.isCheckCitySizeCapToUpgrade(town, grid), "range 2 reaches it");

        final Habilidade pic = habilidade(";PIC;");
        pic.setValor(1);
        iron.addHabilidade(pic);
        assertNull(facade.isCheckCitySizeCapToUpgrade(town, grid), ";PIC;=1 cuts the range to 1");
    }
}
