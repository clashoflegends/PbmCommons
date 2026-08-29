package business.converter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Site frequency string to game habilidade flag.
 *
 * This mapping is the one place a new game's cadence is decided: the Site ships the requested
 * frequency as a short string, and the Judge turns it into the ;GF*; flag it stores on the new
 * game.
 *
 * It is worth pinning because the failure is SILENT. An unrecognised value does not throw, it
 * returns ";GF1;" - a perfectly plausible weekly game - so a mismatch produces a game that looks
 * correct everywhere except in the deadline the players actually get. "43D" was accepted by the
 * Site's own frequency list from 2017 and never matched here (only "4D3D" was, which the
 * Site has never sent), so every alternating-cadence game requested in that window was quietly
 * created as weekly. Game 908 was the one that got noticed.
 *
 * The values below are the complete set the Site's frequency list offers. If one is added there,
 * add it here too.
 */
public class GameFrequencyTest {

    /** The regression. Both spellings map, because the Site sends the second one. */
    @Test
    public void alternatingCadenceMaps() {
        assertEquals(";GF43;", ConverterFactory.getGameFrequency("43D"));
        assertEquals(";GF43;", ConverterFactory.getGameFrequency("4D3D"));
    }

    /**
     * The specific damage: an unmapped cadence is indistinguishable from a real weekly game.
     * If this ever fails it means 43D silently became a 7-day game again.
     */
    @Test
    public void alternatingCadenceIsNotWeekly() {
        assertNotEquals(ConverterFactory.getGameFrequency("7D"),
                ConverterFactory.getGameFrequency("43D"));
    }

    /** Every value the Site's frequency combo can actually produce resolves to its own flag. */
    @Test
    public void everySelectableFrequencyMaps() {
        assertEquals(";GFH1;", ConverterFactory.getGameFrequency("1H"));
        assertEquals(";GFH2;", ConverterFactory.getGameFrequency("2H"));
        assertEquals(";GFH6;", ConverterFactory.getGameFrequency("6H"));
        assertEquals(";GFH12;", ConverterFactory.getGameFrequency("12H"));
        assertEquals(";GF0;", ConverterFactory.getGameFrequency("1D"));
        assertEquals(";GF5;", ConverterFactory.getGameFrequency("2D"));
        assertEquals(";GF3;", ConverterFactory.getGameFrequency("3D"));
        assertEquals(";GF4;", ConverterFactory.getGameFrequency("4D"));
        assertEquals(";GF8;", ConverterFactory.getGameFrequency("8D"));
        assertEquals("", ConverterFactory.getGameFrequency("S"));
        // the weekday-pinned variants collapse onto their base cadence by design
        for (String suffix : new String[]{"", "1", "2", "3", "4", "5", "6", "7"}) {
            assertEquals(";GF1;", ConverterFactory.getGameFrequency("7D" + suffix), "7D" + suffix);
            assertEquals(";GF2;", ConverterFactory.getGameFrequency("14D" + suffix), "14D" + suffix);
        }
    }

    /** Case is not the Site's contract to keep, so it must not be ours to depend on. */
    @Test
    public void matchingIsCaseInsensitive() {
        assertEquals(";GF43;", ConverterFactory.getGameFrequency("43d"));
        assertEquals(";GFH12;", ConverterFactory.getGameFrequency("12h"));
    }

    /**
     * An unknown value still falls back to weekly rather than throwing - a game must be creatable
     * even from a value nobody anticipated. The fallback now also logs a warning, which is the part
     * that was missing while 43D was broken.
     */
    @Test
    public void unknownFrequencyFallsBackToWeekly() {
        assertEquals(";GF1;", ConverterFactory.getGameFrequency("nonsense"));
    }
}
