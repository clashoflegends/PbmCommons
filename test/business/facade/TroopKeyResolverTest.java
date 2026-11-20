package business.facade;

import java.util.SortedMap;
import java.util.TreeMap;
import model.TipoTropa;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TroopKeyResolverTest {
    private static SortedMap<String, TipoTropa> roster(String... codes) {
        SortedMap<String, TipoTropa> roster = new TreeMap<>();
        for (String code : codes) {
            TipoTropa troop = new TipoTropa(); troop.setCodigo(code); roster.put(code, troop);
        }
        return roster;
    }

    @Test void familiesResolveOnlyWithinTheSuppliedRoster() {
        for (String code : new String[]{"cpblood", "cpblood2", "cpblood3"}) {
            var troops = roster(code);
            assertSame(troops.get(code), TroopKeyResolver.resolve(" cpblood ", troops));
        }
        var ships = roster("kraken2", "wm2");
        assertSame(ships.get("kraken2"), TroopKeyResolver.resolve("kraken", ships));
        assertNull(TroopKeyResolver.resolve("cpblood", ships));
    }

    @Test void exactMatchWinsEvenWhenSubstringSortsFirst() {
        var troops = roster("bowmen", "wm", "wm2");
        assertSame(troops.get("wm"), TroopKeyResolver.resolve("wm", troops));
    }

    @Test void retainsLegacyFallbackOrderAndCaseRules() {
        var troops = roster("bowmen", "wm2");
        assertSame(troops.get("bowmen"), TroopKeyResolver.resolve("wm", troops));
        assertNull(TroopKeyResolver.resolve("WM", troops));
        troops = roster("CPBLOOD", "cpblood2", "cpblood3");
        assertSame(troops.get("CPBLOOD"), TroopKeyResolver.resolve("cpblood", troops));
    }

    @Test void preservesNullEmptyAndMissingKeyBehaviour() {
        var troops = roster("a", "b");
        assertNull(TroopKeyResolver.resolve(null, troops));
        assertNull(TroopKeyResolver.resolve("missing", troops));
        //Legacy callers own validation; extraction must not silently change even this edge case.
        assertSame(troops.get("a"), TroopKeyResolver.resolve("  ", troops));
        assertNull(TroopKeyResolver.resolve("", new TreeMap<>()));
    }
}
