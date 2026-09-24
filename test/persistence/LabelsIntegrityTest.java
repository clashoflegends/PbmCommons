package persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The four translations against the English original: the ways a label file breaks a RUNNING client.
 *
 * <h3>Why this is a test and not a review</h3>
 *
 * Every failure below is silent until a player in that language reaches the screen, and none of them
 * is visible to anybody working in English. Two of the three have already shipped here.
 *
 * <h3>What it deliberately does NOT check</h3>
 *
 * Completeness. A translation missing a key falls back to the English base bundle, which is ugly but
 * correct, and failing a build over it would mean no key could ever be added without touching five
 * files in the same commit. Coverage is a release decision, not a build one.
 */
public class LabelsIntegrityTest {

    private static final String[] LANGUAGES = {"es", "ca", "it", "pt"};
    /** {@code %s}, {@code %d}, {@code %,d}, {@code %1$s} - the conversion, without the argument. */
    private static final Pattern PLACEHOLDER =
            Pattern.compile("%(?:\\d+\\$)?[-#+ 0,(]*\\d*(?:\\.\\d+)?([a-zA-Z%])");
    /** A two-byte UTF-8 letter read one byte at a time leaves this pair behind. */
    private static final int LEAD_ONE = 0xc2, LEAD_TWO = 0xc3, CONT_LOW = 0x80, CONT_HIGH = 0xbf;

    /**
     * A translation may not need MORE format arguments than the English does.
     *
     * That direction is the one that throws: {@code String.format} is handed as many arguments as
     * the ENGLISH sentence implies, so a translation carrying an extra conversion runs off the end
     * of the argument list and raises {@code MissingFormatArgumentException} - a crash in one
     * language only, invisible to everyone working in English.
     *
     * The other direction is PRINTED and not failed, because it is usually deliberate. All three
     * cases in this codebase are: {@code FILENAME.SAVE.REGRAS} is a localised filename template
     * that intentionally drops the game id ({@code reglas_%s}), {@code MISSING.ORDERS} omits the
     * actor name, and {@code ENVIAR.DONE} has no Java caller at all. Extra arguments are ignored by
     * {@code String.format}, so the worst case there is a shorter sentence.
     *
     * Compares the conversion LETTERS in order, not the raw text: {@code %1$s} exists so word order
     * can change, and a translator moving a placeholder within a sentence is correct.
     */
    @Test
    public void noTranslationNeedsMoreFormatArgumentsThanTheEnglish() throws IOException {
        final Properties base = load("/labels.properties");
        final List<String> fatal = new ArrayList<>(), benign = new ArrayList<>();
        for (String lang : LANGUAGES) {
            final Properties other = load("/labels_" + lang + ".properties");
            for (String key : other.stringPropertyNames()) {
                final String english = base.getProperty(key);
                if (english == null) {
                    continue;   // a key the base no longer has; stale, not dangerous
                }
                final String mine = conversions(english);
                final String theirs = conversions(other.getProperty(key));
                if (mine.equals(theirs)) {
                    continue;
                }
                final String line = String.format("%s %s: English takes %s, translation takes %s",
                        lang, key, mine.isEmpty() ? "nothing" : mine,
                        theirs.isEmpty() ? "nothing" : theirs);
                if (theirs.length() > mine.length()) {
                    fatal.add(line);
                } else {
                    benign.add(line);
                }
            }
        }
        if (!benign.isEmpty()) {
            System.out.println("Translations taking FEWER arguments (safe, usually deliberate):");
            System.out.println(join(benign));
        }
        assertTrue(fatal.isEmpty(),
                "a translation needing more arguments than the code supplies throws at runtime, "
                + "in one language only:" + System.lineSeparator() + join(fatal));
    }

    /**
     * No loaded value may carry the signature of UTF-8 text decoded as ISO-8859-1.
     *
     * Tested through the RUNTIME BUNDLE rather than by inspecting bytes, and that distinction is
     * the whole point. A byte-level check calls three of these four files "not valid UTF-8 yet full
     * of UTF-8 sequences", concludes they must be mojibake, and is wrong - they render perfectly.
     * Whatever the loader does with a mixed file, the only question that matters is what the player
     * sees, so this asks the loader instead of second-guessing it.
     *
     * The signature is a C2 or C3 lead followed by a character in the continuation range. It is
     * specific enough not to false-positive: a legitimate A-tilde in Portuguese is followed by a
     * LETTER, not a continuation byte. It caught eight Italian keys that were genuinely broken
     * until 2026-09-23, and passed the other three files, which were genuinely fine.
     */
    @Test
    public void noLoadedLabelRendersAsMojibake() throws IOException {
        final List<String> problems = new ArrayList<>();
        for (String lang : LANGUAGES) {
            final ResourceBundle bundle =
                    ResourceBundle.getBundle("labels", Locale.forLanguageTag(lang));
            for (String key : Collections.list(bundle.getKeys())) {
                final String value = bundle.getString(key);
                if (looksDoubleDecoded(value)) {
                    problems.add(lang + " " + key + " = " + value);
                }
            }
        }
        assertTrue(problems.isEmpty(), "UTF-8 text decoded as ISO-8859-1 - the file mixes "
                + "encodings:" + System.lineSeparator() + join(problems));
    }

    /**
     * A value that is nothing but a glyph must be byte-identical in every language.
     *
     * These are the roster and outcome marks - a trophy, a skull, an eye, an hourglass, a question
     * mark, a middle dot - and there is nothing in them to translate. They are here because the
     * mojibake test above CANNOT see them go wrong: it looks for a C2/C3 lead, which is what a
     * Latin-1 letter degrades to, while an emoji leads with E2 or F0 and degrades differently.
     * A translator copying one through a tool that unescapes {@code \}{@code uXXXX} writes a raw
     * emoji, or re-escapes it as a malformed five-digit escape that Java reads as a different
     * character followed by a stray digit - and every other check in this class passes.
     *
     * Equality is the whole test, and it is exact: if English says it, every language says it.
     */
    @Test
    public void glyphOnlyValuesAreIdenticalInEveryLanguage() throws IOException {
        final Properties base = load("/labels.properties");
        final List<String> problems = new ArrayList<>();
        for (String key : base.stringPropertyNames()) {
            final String english = base.getProperty(key);
            if (english.isEmpty() || !isGlyphOnly(english)) {
                continue;
            }
            for (String lang : LANGUAGES) {
                final String theirs = load("/labels_" + lang + ".properties").getProperty(key);
                if (theirs != null && !theirs.equals(english)) {
                    problems.add(lang + " " + key + ": expected " + describe(english)
                            + " but holds " + describe(theirs));
                }
            }
        }
        assertTrue(problems.isEmpty(),
                "a glyph is not translatable:" + System.lineSeparator() + join(problems));
    }

    /** No letters, no digits, no spaces - just a mark. */
    private static boolean isGlyphOnly(String value) {
        for (int ii = 0; ii < value.length(); ii++) {
            if (Character.isLetterOrDigit(value.charAt(ii)) || value.charAt(ii) == ' ') {
                return false;
            }
        }
        return true;
    }

    /** Code points, because the difference is invisible printed. */
    private static String describe(String value) {
        final StringBuilder ret = new StringBuilder();
        for (int ii = 0; ii < value.length(); ii++) {
            ret.append(String.format("U+%04X ", (int) value.charAt(ii)));
        }
        return ret.toString().trim();
    }

    /**
     * No value may open with a space, because {@code Properties.load} eats it.
     *
     * A leading space after the {@code =} is not stored, so it is either a separator that never
     * arrives or a lie about who owns the layout - and in every language at once, which is why
     * nobody notices. {@code BATTLESIM.STATUS.EDITED} carried one in all five files: the status bar
     * concatenates it after another sentence and puts the real gap in the CODE, so the space bought
     * nothing, and had it survived it would have indented the sentence when it appears alone.
     *
     * Spacing between two labels belongs to the code that joins them. A label does not know what, if
     * anything, will be printed before it.
     *
     * Read as ISO-8859-1 bytes rather than through {@code Properties}, for the obvious reason: by
     * the time the loader has answered, the evidence is gone.
     */
    @Test
    public void noLabelValueOpensWithASpace() throws IOException {
        final List<String> problems = new ArrayList<>();
        for (String name : new String[]{"labels", "labels_es", "labels_ca", "labels_it",
            "labels_pt"}) {
            final String text = new String(bytes("/" + name + ".properties"),
                    StandardCharsets.ISO_8859_1);
            for (String line : text.split("\n")) {
                final int eq = line.indexOf('=');
                if (eq <= 0 || line.startsWith("#") || line.startsWith("!")
                        || eq + 1 >= line.length()) {
                    continue;
                }
                final char first = line.charAt(eq + 1);
                if (first == ' ' || first == '\t') {
                    problems.add(name + ": " + line.substring(0, eq) + " opens with whitespace");
                }
            }
        }
        assertTrue(problems.isEmpty(), "Properties.load strips it, so it never reaches the player:"
                + System.lineSeparator() + join(problems));
    }

    /** A duplicate key is legal, silent, and the last one wins. KI-048. */
    @Test
    public void noLabelFileRepeatsAKey() throws IOException {
        final List<String> problems = new ArrayList<>();
        for (String name : new String[]{"labels", "labels_es", "labels_ca", "labels_it",
            "labels_pt"}) {
            final List<String> seen = new ArrayList<>();
            final String text = new String(bytes("/" + name + ".properties"),
                    StandardCharsets.ISO_8859_1);
            for (String line : text.split("\n")) {
                final String trimmed = line.trim();
                final int eq = trimmed.indexOf('=');
                if (eq <= 0 || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                    continue;
                }
                final String key = trimmed.substring(0, eq);
                if (seen.contains(key)) {
                    problems.add(name + ": duplicate key " + key);
                } else {
                    seen.add(key);
                }
            }
        }
        assertTrue(problems.isEmpty(), join(problems));
    }

    private static boolean looksDoubleDecoded(String value) {
        for (int ii = 0; ii < value.length() - 1; ii++) {
            final int lead = value.charAt(ii), next = value.charAt(ii + 1);
            if ((lead == LEAD_ONE || lead == LEAD_TWO) && next >= CONT_LOW && next <= CONT_HIGH) {
                return true;
            }
        }
        return false;
    }

    /** The conversion letters, in order: "%s eats %,d" gives "s,d". */
    private static String conversions(String value) {
        final StringBuilder ret = new StringBuilder();
        final Matcher matcher = PLACEHOLDER.matcher(value);
        while (matcher.find()) {
            if ("%".equals(matcher.group(1))) {
                continue;       // an escaped literal percent takes no argument
            }
            if (ret.length() > 0) {
                ret.append(',');
            }
            ret.append(matcher.group(1));
        }
        return ret.toString();
    }

    private static Properties load(String resource) throws IOException {
        final Properties ret = new Properties();
        try (InputStream is = LabelsIntegrityTest.class.getResourceAsStream(resource)) {
            assertTrue(is != null, "missing " + resource);
            // ISO-8859-1 on purpose: this reads bytes-to-chars so the placeholder comparison is
            // unaffected by whichever encoding the runtime would have chosen for the file.
            ret.load(new InputStreamReader(is, StandardCharsets.ISO_8859_1));
        }
        return ret;
    }

    private static byte[] bytes(String resource) throws IOException {
        try (InputStream is = LabelsIntegrityTest.class.getResourceAsStream(resource)) {
            assertTrue(is != null, "missing " + resource);
            return is.readAllBytes();
        }
    }

    private static String join(List<String> lines) {
        final StringBuilder ret = new StringBuilder();
        for (String line : lines) {
            ret.append(line).append(System.lineSeparator());
        }
        return ret.toString();
    }
}
