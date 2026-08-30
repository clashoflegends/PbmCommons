package business.converter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Site game type to the Judge flags that define it.
 *
 * Pinned because a type is a COMBINATION, not one flag, and because the failure mode is silent: a
 * game created with the wrong type looks completely normal, it just has the wrong allies. That is
 * how game 908 shipped a Locked Teams game to a player who asked for Free For All.
 *
 * The composition, and the player-facing wording for each, is in
 * PbmOps/Meta/GAME_TYPE_DEFINITIONS.md. If a row changes there it changes here.
 */
public class GameTypeTest {

    @Test
    public void theFourBaseTypes() {
        assertEquals(";FFA;", ConverterFactory.getGameType("FFA"));
        assertEquals(";GDM;;GND;", ConverterFactory.getGameType("DM"));
        assertEquals(";GLA;;GND;", ConverterFactory.getGameType("TEAM"));
        assertEquals(";FFA;;GBR;", ConverterFactory.getGameType("GBR"));
    }

    /** Gun Boat and Hidden Team are the same secrecy flag on two different bases. */
    @Test
    public void hiddenPlayersRidesOnABase() {
        assertEquals(";GDM;;GND;;GAP;", ConverterFactory.getGameType("GB"));
        assertEquals(";GLA;;GND;;GAP;", ConverterFactory.getGameType("HIDDEN"));
    }

    /** Iron Price is a modifier on Locked Teams, the only base it has ever been built on. */
    @Test
    public void ironPrice() {
        assertEquals(";GLA;;GND;;GAI;", ConverterFactory.getGameType("IRON"));
    }

    /**
     * ;GND; is what separates the closed types from the open ones. Every base except FFA and GBR
     * carries it; if that ever inverts, a Free For All has lost its diplomacy or a team game has
     * gained one.
     */
    @Test
    public void diplomacyIsOffForEveryClosedTypeAndOnForTheOpenOnes() {
        for (String closed : new String[]{"DM", "TEAM", "GB", "HIDDEN", "IRON"}) {
            assertTrue(ConverterFactory.getGameType(closed).contains(";GND;"), closed);
        }
        for (String open : new String[]{"FFA", "GBR"}) {
            assertTrue(!ConverterFactory.getGameType(open).contains(";GND;"), open);
        }
    }

    /**
     * FACTION was merged into TEAM on the Site. A payload queued before that merge can still be
     * sitting in the queue, so the alias has to keep resolving.
     */
    @Test
    public void factionStillResolvesToLockedTeams() {
        assertEquals(ConverterFactory.getGameType("TEAM"), ConverterFactory.getGameType("FACTION"));
    }

    /**
     * The important one. An unknown or missing type must leave the game exactly as the template
     * built it, NOT fall back to some plausible default. A wrong-but-believable type is the failure
     * this whole change exists to remove, and guessing here would reintroduce it one level up.
     */
    @Test
    public void unknownTypeChangesNothing() {
        assertEquals("", ConverterFactory.getGameType("nonsense"));
        assertEquals("", ConverterFactory.getGameType(""));
        assertEquals("", ConverterFactory.getGameType(null));
    }

    /** The Site's stored values are not case-normalised, so ours must not care. */
    @Test
    public void matchingIsCaseInsensitiveAndTrims() {
        assertEquals(";GLA;;GND;", ConverterFactory.getGameType("team"));
        assertEquals(";GLA;;GND;;GAP;", ConverterFactory.getGameType(" Hidden "));
    }

    /**
     * Every flag this converter emits must either be tagged removable by the ;FGT; migration, or be
     * a deliberate add-only exception. Otherwise setting a type leaves the previous type's flags in
     * place and the game ends up being two types at once. Keep in step with
     * Temp/2026-08-29_game_type_filter.sql.
     *
     * ;GBR; is the one exception. It composes with a base instead of replacing one (FAB08a is a
     * Locked Teams game with battle royale scoring), and the type list has no "Locked Teams +
     * Battle Royale" entry, so a player choosing Locked Teams is not saying "not battle royale".
     * Stripping it would silently swap the victory condition. Requesting GBR still adds it.
     */
    @Test
    public void everyEmittedFlagIsRemovableOrDeliberatelyAddOnly() {
        final String removable = ";FFA;;GDM;;GLA;;GND;;GAP;;GAI;";
        final String addOnly = ";GBR;";
        for (String tp : new String[]{"FFA", "DM", "TEAM", "GBR", "GB", "HIDDEN", "IRON"}) {
            for (String flag : ConverterFactory.getGameType(tp).split(";")) {
                if (!flag.isEmpty()) {
                    assertTrue(removable.contains(";" + flag + ";") || addOnly.contains(";" + flag + ";"),
                            tp + " emits " + flag + ", which is neither removable nor add-only");
                }
            }
        }
    }

    /**
     * A team game scored on city domination (;GLA;;GND; plus ;GBR;) must survive a TEAM request.
     * Guards the FAB08a shape: ;GBR; is not in the removable set, so requesting Locked Teams from
     * such a template keeps battle royale scoring rather than reverting it to victory points.
     */
    @Test
    public void requestingTeamDoesNotClaimBattleRoyale() {
        assertTrue(!ConverterFactory.getGameType("TEAM").contains(";GBR;"));
        assertTrue(!ConverterFactory.getGameType("DM").contains(";GBR;"));
    }
}
