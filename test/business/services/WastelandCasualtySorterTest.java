package business.services;

import java.util.ArrayList;
import java.util.List;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WastelandCasualtySorterTest {
    private static Terreno terrain(String code) {
        Terreno terrain = new Terreno();
        terrain.setCodigo(code);
        return terrain;
    }

    private static TipoTropa troop(int id, Terreno terrain, boolean filled) {
        TipoTropa troop = new TipoTropa();
        troop.setId(id);
        troop.setCodigo("troop" + id);
        if (filled) {
            troop.getAtaqueTerreno().put(terrain, 0);
            troop.getDefesaTerreno().put(terrain, 0);
            troop.getMovimentoTerreno().put(terrain, 999);
        }
        return troop;
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9})
    void legacyMissingWastelandMatchesExplicitBackfillForEveryTactic(int tactic) {
        Terreno wasteland = terrain("W");
        TipoTropa missing = troop(1, wasteland, false);
        TipoTropa filled = troop(1, wasteland, true);
        TipoTropa existing = troop(2, wasteland, true);
        existing.getAtaqueTerreno().put(wasteland, 1);
        existing.getDefesaTerreno().put(wasteland, 1);
        existing.getMovimentoTerreno().put(wasteland, 1);
        ComparatorCasualtiesSorter comparator = new ComparatorCasualtiesSorter(tactic, wasteland);
        assertEquals(comparator.compare(filled, existing), comparator.compare(missing, existing));
        assertEquals(comparator.compare(existing, filled), comparator.compare(existing, missing));
        List<TipoTropa> troops = new ArrayList<>(List.of(existing, missing, troop(3, wasteland, false)));
        assertDoesNotThrow(() -> troops.sort(comparator));
        assertTrue(missing.getAtaqueTerreno().isEmpty(), "Sorting must not mutate a shared troop definition");
        assertTrue(missing.getDefesaTerreno().isEmpty());
        assertTrue(missing.getMovimentoTerreno().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3, 4, 5, 6, 7, 8, 9})
    void everyMissingKeyMatchesFilledWastelandEvenWhenEarlierKeysTie(int tactic) {
        Terreno wasteland = terrain("W");
        for (int stat = 0; stat < 3; stat++) {
            TipoTropa missing = troop(1, wasteland, true);
            TipoTropa other = troop(2, wasteland, true);
            ComparatorCasualtiesSorter comparator = new ComparatorCasualtiesSorter(tactic, wasteland);
            int expected = comparator.compare(missing, other);
            switch (stat) {
                case 0 -> missing.getAtaqueTerreno().clear();
                case 1 -> missing.getDefesaTerreno().clear();
                case 2 -> missing.getMovimentoTerreno().clear();
            }
            assertEquals(expected, comparator.compare(missing, other));
            assertEquals(-expected, comparator.compare(other, missing));
        }
    }

    @Test
    void playableTerrainMissingDataRemainsAnError() {
        Terreno plain = terrain("P");
        assertThrows(NullPointerException.class,
                () -> new ComparatorCasualtiesSorter(0, plain).compare(troop(1, plain, false), troop(2, plain, true)));
    }

    @Test
    void existingValuesAndZeroAreNotOverridden() {
        for (String code : List.of("P", "W")) {
            Terreno terrain = terrain(code);
            TipoTropa zero = troop(1, terrain, true);
            TipoTropa stronger = troop(2, terrain, true);
            stronger.getAtaqueTerreno().put(terrain, 15);
            assertTrue(new ComparatorCasualtiesSorter(0, terrain).compare(stronger, zero) < 0);
            assertTrue(new ComparatorCasualtiesSorter(4, terrain).compare(stronger, zero) > 0);
            zero.getMovimentoTerreno().put(terrain, 0);
            stronger.getMovimentoTerreno().put(terrain, 1);
            assertTrue(new ComparatorCasualtiesSorter(3, terrain).compare(stronger, zero) < 0);
        }
    }
}
