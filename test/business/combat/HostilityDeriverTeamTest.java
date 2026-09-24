package business.combat;

import java.util.Arrays;
import java.util.List;
import model.Habilidade;
import model.Jogador;
import model.Nacao;
import model.Partida;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The locked-team rule, against the arrangement of a real game.
 *
 * <h3>The fixture is game 903, not an invention</h3>
 *
 * Greek4b_903 is {@code ;GLA;;GND;} with four nations on two teams - Macedon and Athens BLUE,
 * Sparta and Persia RED - plus the Barbarians on no team. All four player EGFs of turn 3 were read
 * with {@code WorldProbe}, and each observer's own complete relationship row is reproduced below
 * verbatim. That is sixteen cells decided by the server, and the rule under test has to agree with
 * every one of them.
 *
 * The case that matters most is the one no observer row covers: Athens against Persia, two nations
 * neither of which belongs to the player at the keyboard. Before this rule it came out NEUTRAL and
 * disabled Run on hex 1815, where those two armies were standing on Miletus.
 */
public class HostilityDeriverTeamTest {

    private static final String BLUE = "BLUE", RED = "RED";

    /** Macedon's own row, as game 903 turn 3 exports it to Angel Alonso Padilla. */
    @Test
    public void theTeamRuleReproducesMacedonsOwnRow() {
        final Partida partida = lockedTeams();
        final Nacao macedon = nacao("Macedon", BLUE), athens = nacao("Athens", BLUE);
        final Nacao sparta = nacao("Sparta", RED), persia = nacao("Persia", RED);
        final RelationshipMatrix matrix = new HostilityDeriver()
                .deriveNations(partida, Arrays.asList(macedon, athens, sparta, persia), null);

        assertEquals(RelationshipMatrix.ALLY, matrix.getValor(macedon, athens), "Athens=2");
        assertEquals(RelationshipMatrix.SWORN_ENEMY, matrix.getValor(macedon, sparta), "Sparta=-2");
        assertEquals(RelationshipMatrix.SWORN_ENEMY, matrix.getValor(macedon, persia), "Persia=-2");
    }

    /** All four rows at once: same team +2, different team -2, sixteen cells, no exceptions. */
    @Test
    public void everyCellOfTheRealGameAgreesWithTheTeamFlags() {
        final Partida partida = lockedTeams();
        final Nacao macedon = nacao("Macedon", BLUE), athens = nacao("Athens", BLUE);
        final Nacao sparta = nacao("Sparta", RED), persia = nacao("Persia", RED);
        final List<Nacao> all = Arrays.asList(macedon, athens, sparta, persia);
        final RelationshipMatrix matrix = new HostilityDeriver()
                .deriveNations(partida, all, null);

        for (Nacao from : all) {
            for (Nacao to : all) {
                if (from == to) {
                    continue;
                }
                final boolean sameTeam = from.getTeamFlag().equals(to.getTeamFlag());
                assertEquals(sameTeam ? RelationshipMatrix.ALLY : RelationshipMatrix.SWORN_ENEMY,
                        matrix.getValor(from, to), from.getNome() + " -> " + to.getNome());
                assertEquals(RelationshipMatrix.Origin.FROM_GAME_TYPE,
                        matrix.getOrigin(from, to), from.getNome() + " -> " + to.getNome());
            }
        }
    }

    /**
     * Hex 1815, Miletus: Athens against Persia with no army of the observer's anywhere near it.
     *
     * The regression this rule exists for. Neither nation's row can be read - the server exports a
     * foreign nation's relationships only when they are positive and aimed at the player - so before
     * the team rule both directions landed on the observer-shaped default, which has nothing to say
     * about two strangers, and the pair came out at peace.
     */
    @Test
    public void twoForeignNationsOnOpposingTeamsFightEvenWithNoObserverPresent() {
        final Partida partida = lockedTeams();
        final Nacao athens = nacao("Athens", BLUE), persia = nacao("Persia", RED);
        // An observer who owns neither of them, which is the whole point.
        final Jogador observer = new Jogador();
        observer.setNome("Angel Alonso Padilla");

        final RelationshipMatrix matrix = new HostilityDeriver()
                .deriveNations(partida, Arrays.asList(athens, persia), observer);

        assertTrue(matrix.isHostile(athens, persia), "Athens and Persia are on opposing teams");
        assertEquals(RelationshipMatrix.Origin.FROM_GAME_TYPE, matrix.getPairOrigin(athens, persia),
                "a rule, not a guess - so it must not be reported as an assumption");
    }

    /**
     * Without {@code ;GND;} the team flag describes turn zero only, so it decides nothing here.
     *
     * The Judge's team rule lives in {@code doCarregaRelacionamentosFresh}, which SEEDS the table
     * and then only runs again for a random game; every later turn reads the stored table. Locked
     * diplomacy is what stops that table drifting away from the flags.
     */
    @Test
    public void aTeamFlagWithoutLockedDiplomacyIsNotARule() {
        final Partida partida = new Partida();
        partida.addHabilidade(habilidade(";GLA;"));       // teams, but diplomacy still open
        final Nacao athens = nacao("Athens", BLUE), persia = nacao("Persia", RED);

        final RelationshipMatrix matrix = new HostilityDeriver()
                .deriveNations(partida, Arrays.asList(athens, persia), null);

        assertEquals(RelationshipMatrix.Origin.ASSUMED, matrix.getPairOrigin(athens, persia),
                "no row could be read and the flags are only a seed, so this is an assumption");
    }

    /**
     * Two nations with no team are not teammates.
     *
     * {@code "-"} is what the Judge writes for a nation on no team. Treating it as a team name would
     * declare every neutral nation an ally of every other, and the client cannot fall back on the
     * NPC branch to catch them the way the Judge can - the column that decides it is not in the EGF.
     */
    @Test
    public void theEmptyTeamIsNotATeam() {
        final Partida partida = lockedTeams();
        final Nacao one = nacao("Neutral1", "-"), other = nacao("Neutral2", "-");

        final RelationshipMatrix matrix = new HostilityDeriver()
                .deriveNations(partida, Arrays.asList(one, other), null);

        assertFalse(RelationshipMatrix.Origin.FROM_GAME_TYPE
                .equals(matrix.getPairOrigin(one, other)),
                "'-' means no team, so the team rule must not answer this pair");
    }

    /** {@code ;GLA;;GND;}, which is what {@code ConverterFactory} emits for every team type. */
    private static Partida lockedTeams() {
        final Partida ret = new Partida();
        ret.addHabilidade(habilidade(";GLA;"));
        ret.addHabilidade(habilidade(";GND;"));
        return ret;
    }

    private static Habilidade habilidade(String codigo) {
        final Habilidade ret = new Habilidade();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        return ret;
    }

    private static Nacao nacao(String nome, String team) {
        final Nacao ret = new Nacao();
        ret.setNome(nome);
        ret.setCodigo(nome);
        ret.setTeamFlag(team);
        return ret;
    }
}
