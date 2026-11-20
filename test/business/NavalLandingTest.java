package business;

import business.facade.LocalFacade;
import java.util.List;
import model.Habilidade;
import model.Local;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NavalLandingTest {
    static Terreno terrain(String code, boolean water) {
        Terreno t = new Terreno(); t.setCodigo(code); t.setNome(code);
        t.setAgua(water); t.setAncoravel("L".equals(code)); return t;
    }
    static Local hex(String code, Terreno t) {
        Local l = new Local(); l.setCodigo(code); l.setCoordenadas(code); l.setTerreno(t);
        l.setRio(""); l.setRiacho(""); l.setEstrada(""); l.setPonte(""); l.setVau("");
        return l;
    }
    static MovimentoExercito movement(Local from, Local to, int direction) {
        TipoTropa ship = new TipoTropa(); ship.setCodigo("ship"); ship.setNome("ship");
        Habilidade naval = new Habilidade(); naval.setCodigo(";TTN;"); ship.addHabilidade(naval);
        ship.getMovimentoTerreno().put(from.getTerreno(), 2);
        ship.getMovimentoTerreno().put(to.getTerreno(), 5);
        MovimentoExercito m = new MovimentoExercito(); m.addTropasAll(List.of(ship));
        m.setOrigem(from); m.setDestino(to); m.setDirecao(direction);
        m.setPorAgua(true); m.setComida(true); m.setLimiteMovimento(14); return m;
    }
    @Test void landingAllowsEveryLandTerrainWithoutChangingItsNavalCost() {
        for (String code : List.of("H", "P", "F", "M", "S", "D")) {
            Local sea = hex("0204", terrain("C", true));
            Local land = hex("0304", terrain(code, false)); land.setLanding(" 66 ");
            MovimentoExercito m = movement(sea, land, 3);
            assertEquals(5, m.getCustoMovimento(), code);
            assertTrue(new LocalFacade().isAncoravel(land), code);
            assertEquals(code, land.getTerreno().getCodigo());
        }
    }
    @Test void unmarkedLandAndWrongSideRemainBlocked() {
        Local sea = hex("0204", terrain("C", true));
        Local hill = hex("0304", terrain("H", false));
        MovimentoExercito m = movement(sea, hill, 3);
        assertEquals(9999, m.getCustoMovimento());
        hill.setLanding("3"); assertEquals(9999, m.getCustoMovimento());
    }
    @Test void riverShoreDepartureAndLandMovementKeepTheirCosts() {
        Local sea = hex("0204", terrain("C", true));
        Local hill = hex("0304", terrain("H", false)); hill.setRio("6");
        MovimentoExercito m = movement(sea, hill, 3);
        int river = m.getCustoMovimento(); hill.setLanding("6");
        assertEquals(river, m.getCustoMovimento());
        m.setPorAgua(false); int land = m.getCustoMovimento(); hill.setLanding("");
        assertEquals(land, m.getCustoMovimento());
        hill.setRio(""); m.setPorAgua(true); m.setOrigem(hill); m.setDestino(sea);
        m.setDirecao(6); m.setDirecaoAnterior(0); assertTrue(m.getCustoMovimento() < 9999);
        Local shore = hex("0304", terrain("L", false)); m = movement(sea, shore, 3);
        int before = m.getCustoMovimento(); shore.setLanding("6");
        assertEquals(before, m.getCustoMovimento());
    }

    @Test void landingOnlyArrivalIsTerminalButAnIndependentRiverIsNot() {
        Local sea = hex("0204", terrain("C", true));
        Local hill = hex("0304", terrain("H", false)); hill.setLanding("6");
        MovimentoExercito m = movement(sea, hill, 3);
        assertTrue(m.isLandingStop());
        hill.setRio("6"); assertFalse(m.isLandingStop());
        hill.setRio("3"); assertTrue(m.isLandingStop(), "unconnected river is not admission");
        m.setPorAgua(false); assertFalse(m.isLandingStop());
        hill.setRio(""); hill.setTerreno(terrain("L", false)); m.setPorAgua(true);
        assertFalse(m.isLandingStop(), "native Shore behaviour is unchanged");
    }

    @Test void rangePaysTerrainCostAndDoesNotExpandPastLandingOrCheapFakeBudget() {
        Local sea = hex("0204", terrain("C", true));
        Local hill = hex("0304", terrain("H", false)); hill.setLanding("36");
        Local beyond = hex("0404", sea.getTerreno());
        java.util.SortedMap<String, Local> map = new java.util.TreeMap<>();
        for (Local l : List.of(sea, hill, beyond)) map.put(l.getCodigo(), l);
        MovimentoExercito m = movement(sea, hill, 3); m.setLimiteMovimento(4);
        var facade = new business.facade.ExercitoFacade();
        assertFalse(facade.calculateArmyRangeHexes(map, m).containsKey(hill));
        m.setOrigem(sea); m.setLimiteMovimento(14);
        var range = facade.calculateArmyRangeHexes(map, m);
        assertEquals(5, range.get(hill).getCost());
        assertFalse(range.containsKey(beyond));
        m.setOrigem(hill);
        assertTrue(facade.calculateArmyRangeHexes(map, m).containsKey(beyond), "fresh departure");
        hill.setRio("36"); m.setOrigem(sea);
        assertTrue(facade.calculateArmyRangeHexes(map, m).containsKey(beyond), "independent river continues");
    }

    @Test void allSixSidesAreDirectionalAndFoodEvasionAndImpassableCostsAreUnchanged() {
        Local sea = hex("0506", terrain("C", true));
        var facade = new LocalFacade();
        for (int direction = 1; direction <= 6; direction++) {
            Local hill = hex(facade.getIdentificacaoVizinho(sea, direction), terrain("H", false));
            int opposite = business.converter.ConverterFactory.getDirecao(direction + 3);
            hill.setLanding("" + opposite);
            MovimentoExercito m = movement(sea, hill, direction);
            assertTrue(m.isLandingStop()); assertEquals(5, m.getCustoMovimento());
            m.setComida(false); assertEquals(7, m.getCustoMovimento());
            m.setEvasivo(true); assertEquals(14, m.getCustoMovimento());
            hill.setLanding(null); assertEquals(9999, m.getCustoMovimento());
        }
        Local hill = hex("0606", terrain("H", false)); hill.setLanding("6");
        MovimentoExercito m = movement(sea, hill, 3);
        TipoTropa ship = new TipoTropa(); ship.setCodigo("blocked");
        Habilidade naval = new Habilidade(); naval.setCodigo(";TTN;"); ship.addHabilidade(naval);
        ship.getMovimentoTerreno().put(hill.getTerreno(), 0);
        m.addTropas(ship); assertEquals(999, m.getCustoMovimento(), "Landing does not rewrite costs");
    }

    @Test void coastalForecastUsesTwoBoundedSearchesOnAProductionSizedMap() {
        java.util.SortedMap<String, Local> map = new java.util.TreeMap<>();
        Terreno water = terrain("C", true), hill = terrain("H", false);
        for (int col = 1; col <= 60; col++) {
            for (int row = 1; row <= 80; row++) {
                Local local = hex(String.format("%02d%02d", col, row), col % 8 == 0 ? hill : water);
                if (col % 8 == 0) local.setLanding("6");
                map.put(local.getCodigo(), local);
            }
        }
        MovimentoExercito settings = movement(map.get("2340"), map.get("2440"), 3);
        long before = System.nanoTime();
        var forecast = org.junit.jupiter.api.Assertions.assertTimeout(java.time.Duration.ofSeconds(10),
                () -> NavalRange.forecast(map, settings, NavalRange.landingHexes(map)));
        System.out.println("Landing forecast, 4800 coastal hexes: " + (System.nanoTime() - before) / 1_000_000 + " ms");
        assertTrue(forecast.firstTurn().containsKey(map.get("2440")));
        assertTrue(forecast.secondTurn().containsKey(map.get("2540")));
        assertFalse(forecast.firstTurn().containsKey(map.get("2540")), "no first-turn transit");
    }

    @Test void disconnectedLandingDoesNotChangeLegacyRangeCostsOrPaths() {
        Terreno water = terrain("C", true), shore = terrain("L", false);
        Local start = hex("0204", water), coast = hex("0304", shore);
        java.util.SortedMap<String, Local> map = new java.util.TreeMap<>();
        map.put(start.getCodigo(), start); map.put(coast.getCodigo(), coast);
        var facade = new business.facade.ExercitoFacade();
        MovimentoExercito m = movement(start, coast, 3);
        var baseline = facade.calculateArmyRangeHexes(map, m);
        Local remote = hex("2020", terrain("H", false)); remote.setLanding("6");
        Local remoteSea = hex("1920", water);
        map.put(remote.getCodigo(), remote); map.put(remoteSea.getCodigo(), remoteSea);
        m.setOrigem(start);
        var tagged = facade.calculateArmyRangeHexes(map, m);
        assertEquals(baseline.keySet(), tagged.keySet());
        assertEquals(baseline.get(coast).getCost(), tagged.get(coast).getCost());
        assertEquals(baseline.get(coast).toString(), tagged.get(coast).toString());
    }

    @Test void cheapTerminalArrivalDoesNotHideAnIndependentRiverArrivalAtTheSameHex() {
        Terreno water = terrain("C", true), hills = terrain("H", false);
        Local start = hex("0204", water), alternate = hex("0303", water);
        Local landing = hex("0304", hills), end = hex("0404", water);
        landing.setLanding("6"); landing.setRio("23");
        java.util.SortedMap<String, Local> map = new java.util.TreeMap<>();
        for (Local l : List.of(start, alternate, landing, end)) map.put(l.getCodigo(), l);
        MovimentoExercito m = movement(start, landing, 3);
        var paths = new business.facade.ExercitoFacade().calculateArmyRangeHexes(map, m);
        assertEquals(5, paths.get(landing).getCost());
        assertTrue(paths.get(landing).getLastMove().isLandingStop());
        assertEquals(9, paths.get(end).getCost()); // water2 + hills5 + water2
        assertEquals(3, paths.get(end).getSteps().size());
    }
}
