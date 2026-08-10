package persistence.reports;

import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins {@link SysReport#escapeHtml} (single-pass table) to the ORIGINAL 68-call
 * {@code String.replaceAll} chain it replaced, so the player-facing report HTML cannot drift. The
 * reference method below was GENERATED from the pre-change source, not retyped.
 *
 * Why the rewrite: the old chain compiled a fresh regex Pattern and rescanned the whole string ~70
 * times PER REPORT CELL - for every character, city and army of every nation. Three jstack samples of
 * a Judge turn (2026-08-09) landed inside Pattern.compile / Matcher.find in ascToHtml every time.
 *
 * Equivalence is structural, not lucky: every source is a single character and no source character
 * occurs in any replacement, so sequential replacement and one left-to-right pass cannot differ.
 * These tests exist to keep it that way if someone adds a rule.
 */
public class SysReportEscapeTest {

    /** The original implementation, verbatim, as the oracle. */
    private static String reference(String original, boolean convertNewLine) {
        String ret = original;
        ret = ret.replaceAll("\u00C0", "&Agrave;");
        ret = ret.replaceAll("\u00C1", "&Aacute;");
        ret = ret.replaceAll("\u00C2", "&Acirc;");
        ret = ret.replaceAll("\u00C3", "&Atilde;");
        ret = ret.replaceAll("\u00C4", "&Auml;");
        ret = ret.replaceAll("\u00C5", "&Aring;");
        ret = ret.replaceAll("\u00C6", "&AElig;");
        ret = ret.replaceAll("\u00C7", "&Ccedil;");
        ret = ret.replaceAll("\u00C8", "&Egrave;");
        ret = ret.replaceAll("\u00C9", "&Eacute;");
        ret = ret.replaceAll("\u00CA", "&Ecirc;");
        ret = ret.replaceAll("\u00CB", "&Euml;");
        ret = ret.replaceAll("\u00CC", "&Igrave;");
        ret = ret.replaceAll("\u00CD", "&Iacute;");
        ret = ret.replaceAll("\u00CE", "&Icirc;");
        ret = ret.replaceAll("\u00CF", "&Iuml;");
        ret = ret.replaceAll("\u00D0", "&ETH;");
        ret = ret.replaceAll("\u00D1", "&Ntilde;");
        ret = ret.replaceAll("\u0152", "&OElig;");
        ret = ret.replaceAll("\u00D2", "&Ograve;");
        ret = ret.replaceAll("\u00D3", "&Oacute;");
        ret = ret.replaceAll("\u00D4", "&Ocirc;");
        ret = ret.replaceAll("\u00D5", "&Otilde;");
        ret = ret.replaceAll("\u00D6", "&Ouml;");
        ret = ret.replaceAll("\u00D8", "&Oslash;");
        ret = ret.replaceAll("\u0160", "&Scaron;");
        ret = ret.replaceAll("\u00D9", "&Ugrave;");
        ret = ret.replaceAll("\u00DA", "&Uacute;");
        ret = ret.replaceAll("\u00DB", "&Ucirc;");
        ret = ret.replaceAll("\u00DC", "&Uuml;");
        ret = ret.replaceAll("\u00DD", "&Yacute;");
        ret = ret.replaceAll("\u00DE", "&THORN;");
        ret = ret.replaceAll("\u0178", "&Yuml;");
        ret = ret.replaceAll("\u00E0", "&agrave;");
        ret = ret.replaceAll("\u00E1", "&aacute;");
        ret = ret.replaceAll("\u00E2", "&acirc;");
        ret = ret.replaceAll("\u00E3", "&atilde;");
        ret = ret.replaceAll("\u00E4", "&auml;");
        ret = ret.replaceAll("\u00E5", "&aring;");
        ret = ret.replaceAll("\u00E6", "&aelig;");
        ret = ret.replaceAll("\u00E7", "&ccedil;");
        ret = ret.replaceAll("\u00E8", "&egrave;");
        ret = ret.replaceAll("\u00E9", "&eacute;");
        ret = ret.replaceAll("\u00EA", "&ecirc;");
        ret = ret.replaceAll("\u00EB", "&euml;");
        ret = ret.replaceAll("\u00EC", "&igrave;");
        ret = ret.replaceAll("\u00ED", "&iacute;");
        ret = ret.replaceAll("\u00EE", "&icirc;");
        ret = ret.replaceAll("\u00EF", "&iuml;");
        ret = ret.replaceAll("\u00F0", "&eth;");
        ret = ret.replaceAll("\u00F1", "&ntilde;");
        ret = ret.replaceAll("\u0153", "&oelig;");
        ret = ret.replaceAll("\u00F2", "&ograve;");
        ret = ret.replaceAll("\u00F3", "&oacute;");
        ret = ret.replaceAll("\u00F4", "&ocirc;");
        ret = ret.replaceAll("\u00F5", "&otilde;");
        ret = ret.replaceAll("\u00F6", "&ouml;");
        ret = ret.replaceAll("\u00F8", "&oslash;");
        ret = ret.replaceAll("\u0161", "&scaron;");
        ret = ret.replaceAll("\u00F9", "&ugrave;");
        ret = ret.replaceAll("\u00FA", "&uacute;");
        ret = ret.replaceAll("\u00FB", "&ucirc;");
        ret = ret.replaceAll("\u00FC", "&uuml;");
        ret = ret.replaceAll("\u00FD", "&yacute;");
        ret = ret.replaceAll("\u00FE", "&thorn;");
        ret = ret.replaceAll("\u00FF", "&yuml;");
        ret = ret.replaceAll("\\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
        if (convertNewLine) {
            ret = ret.replaceAll("\\n", "<br>");
        }
        return ret;
    }

    private static void assertSame(String input, boolean convertNewLine) {
        assertEquals(reference(input, convertNewLine), SysReport.escapeHtml(input, convertNewLine),
                "escaping differs for: " + input);
    }

    @Test
    public void everyCharacterInRangeMatchesTheOldChain() {
        for (char cc = 0; cc < 0x200; cc++) {
            assertSame(String.valueOf(cc), true);
            assertSame(String.valueOf(cc), false);
            assertSame("Ser " + cc + " of House " + cc, true);
        }
    }

    @Test
    public void realisticNamesMatchTheOldChain() {
        final String[] names = {
            "", "plain ascii name", "João Gurgel", "André Racz",
            "François Dürer", "Niño Muñoz", "Círdan the Shipwright",
            "Björn Ångström", "Škoda ž", "Fingolfin & Co <b>",
            "tab\there", "line\nbreak", "mixed\té\nvalueÇ", "&amp; already escaped"
        };
        for (String name : names) {
            assertSame(name, true);
            assertSame(name, false);
        }
    }

    @Test
    public void randomMixesMatchTheOldChain() {
        final Random rnd = new Random(20260809L);          // fixed seed: reproducible failures
        final StringBuilder sb = new StringBuilder();
        for (int run = 0; run < 500; run++) {
            sb.setLength(0);
            final int len = rnd.nextInt(40);
            for (int ii = 0; ii < len; ii++) {
                sb.append((char) rnd.nextInt(0x200));
            }
            assertSame(sb.toString(), rnd.nextBoolean());
        }
    }

    /**
     * The real thing: every distinct accented name in the live dev database - character names
     * (Guthlaf, Tharudan, Thrar III...), nation names and artifact descriptions - run
     * through both implementations. Extracted 2026-08-09 from tmpbm_plutao (GAME CONTENT ONLY - no player names, this repo is
     * public); these are exactly the
     * "elf names, player names" the escaping exists for, so a report rendered from real data cannot
     * differ. Kept as a resource so this stays reproducible without a database.
     */
    @Test
    public void realAccentedNamesFromTheDatabaseMatchTheOldChain() throws Exception {
        final java.net.URL url = getClass().getResource("/accented_names.txt");
        assertNotNull(url, "accented_names.txt corpus missing from test resources");
        final java.util.List<String> names = java.nio.file.Files.readAllLines(
                java.nio.file.Paths.get(url.toURI()), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(names.size() > 200, "corpus unexpectedly small: " + names.size());
        int accented = 0;
        for (String name : names) {
            for (int ii = 0; ii < name.length(); ii++) {
                if (name.charAt(ii) > 0x7F) {
                    accented++;
                    break;
                }
            }
            assertSame(name, true);
            assertSame(name, false);
            assertSame("Lord " + name + " of the North\tand\nelsewhere", true);
        }
        assertTrue(accented > 200, "corpus is not exercising the accent path: " + accented);
    }

    @Test
    public void textWithNothingToEscapeIsReturnedUnchanged() {
        final String plain = "Nothing to escape here 12345 <b>bold</b>";
        assertEquals(plain, SysReport.escapeHtml(plain, true));
    }
}
