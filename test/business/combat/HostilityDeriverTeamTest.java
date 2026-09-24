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
     * {@code "-"} is a team NAME, exactly as the Judge compares it.
     *
     * This test asserted the opposite until 2026-09-23, and the assumption behind it reproduced the
     * very bug this rule was written to fix. Treating {@code "-"} as "no team" made the rule decline
     * any pair with a teamless nation in it, so in a Hidden Team game - where {@code ;GAP;} hides
     * the owner and {@code isNacaoBarbarian} therefore cannot identify the NPC either - a barbarian
     * stack facing a foreign army came out NEUTRAL and Run was disabled. The Judge simply compares:
     * {@code "BLUE".equals("-")} is false, so they are sworn enemies.
     */
    @Test
    public void aTeamlessNationIsAnEnemyOfEveryTeam() {
        final Partida partida = lockedTeams();
        final Nacao barbarians = nacao("Barbarians", "-"), athens = nacao("Athens", BLUE);

        final RelationshipMatrix matrix = new HostilityDeriver()
                .deriveNations(partida, Arrays.asList(barbarians, athens), null);

        assertTrue(matrix.isHostile(barbarians, athens),
                "a different team is a different team, whatever it is called");
        assertEquals(RelationshipMatrix.Origin.FROM_GAME_TYPE,
                matrix.getPairOrigin(barbarians, athens));
    }

    /** And two of them share it, which is what the Judge does with any two equal flags. */
    @Test
    public void twoTeamlessNationsShareTheSameTeamName() {
        final Partida partida = lockedTeams();
        final Nacao one = nacao("Neutral1", "-"), other = nacao("Neutral2", "-");

        final RelationshipMatrix matrix = new HostilityDeriver()
                .deriveNations(partida, Arrays.asList(one, other), null);

        assertEquals(RelationshipMatrix.ALLY, matrix.getValor(one, other),
                "the Judge answers ALLY for two equal flags and does not special-case this one");
    }

    /**
     * {@code ;GSL;} grades a team internally, and the NUMBERS come from the Judge, not from the
     * matching constant names.
     *
     * {@code GameStatusSettings.RELATIONSHIP_VASAL} is 4 and {@code RELATIONSHIP_LORD} is 3, while
     * {@code RelationshipMatrix.VASSAL} is 3 and {@code LORD} is 4 - the client follows
     * {@code BaseMsgs.nacaoRelacionamento} and {@code Nacao.isLord}, which read 4 as the lord. So
     * transcribing the Judge branch by CONSTANT NAME writes the opposite number, which is what this
     * code did until 2026-09-23. Combat does not notice ({@code dificuldadeBonus} is 25 at 2, 3 and
     * 4 alike) but the Diplomacy panel names the cell from the number, and a derived cell has to
     * agree with a read one.
     */
    @Test
    public void teamWithLordUsesTheJudgesOwnNumbers() {
        final Partida partida = new Partida();
        partida.addHabilidade(habilidade(";GSL;"));
        partida.addHabilidade(habilidade(";GND;"));
        final Nacao lord = nacao("Lord", BLUE), vassal = nacao("Vassal", BLUE);
        lord.addHabilidade(habilidade(";NSL;"));

        final RelationshipMatrix matrix = new HostilityDeriver()
                .deriveNations(partida, Arrays.asList(lord, vassal), null);

        assertEquals(4, matrix.getValor(lord, vassal), "the Judge writes RELATIONSHIP_VASAL = 4");
        assertEquals(3, matrix.getValor(vassal, lord), "and RELATIONSHIP_LORD = 3 the other way");
    }

    /**
     * A free-for-all is settled at neutral BEFORE the team branches in the Judge, so team flags say
     * nothing there even when the game somehow carries both.
     */
    @Test
    public void freeForAllOutranksTheTeamFlags() {
        final Partida partida = lockedTeams();
        partida.addHabilidade(habilidade(";FFA;"));
        final Nacao athens = nacao("Athens", BLUE), persia = nacao("Persia", RED);

        final RelationshipMatrix matrix = new HostilityDeriver()
                .deriveNations(partida, Arrays.asList(athens, persia), null);

        assertFalse(RelationshipMatrix.Origin.FROM_GAME_TYPE
                .equals(matrix.getPairOrigin(athens, persia)),
                "the team rule must stand down in a free-for-all");
    }

    /**
     * A war the observer can actually READ beats the team flags, because the flags describe turn
     * zero and the row describes now.
     *
     * {@code doCarregaRelacionamentosFresh} SEEDS the table from the flags, and only when
     * {@code isRandom()}; every later turn reads whatever the stored table has become. Nothing
     * keeps the two in step - {@code ;GND;} is a SITE flag that stops the diplomacy order being
     * offered and the Judge never reads it, so several Judge paths can move a relationship
     * afterwards. Which one did is not the point and this test does not name one: the point is that
     * a stated entry is current and a flag is not.
     */
    @Test
    public void aStatedWarWithATeammateOutranksTheTeamFlags() {
        final Partida partida = lockedTeams();
        final Jogador observer = new Jogador();
        observer.setNome("Angel Alonso Padilla");
        final Nacao macedon = nacao("Macedon", BLUE), athens = nacao("Athens", BLUE);
        macedon.setOwner(observer);     // only his own row can be proven complete
        // he camped where he should not, so the Judge broke the pact with his own teammate
        macedon.addRelacionamento(athens, RelationshipMatrix.SWORN_ENEMY);

        final RelationshipMatrix matrix = new HostilityDeriver()
                .deriveNations(partida, Arrays.asList(macedon, athens), observer);

        assertTrue(matrix.isHostile(macedon, athens),
                "his own EGF says they are at war and it is right");
        assertEquals(RelationshipMatrix.Origin.READ_FROM_EGF, matrix.getOrigin(macedon, athens),
                "and it is a READ, not a construction");
    }

    /**
     * But a row that merely FAILS TO MENTION a nation must not beat the team rule.
     *
     * That distinction is the reason the two reads sit at different heights. A populated row
     * lacking an entry answers a derived zero, and letting that fabricated neutral outrank a
     * construction rule is how a Death Match once came out as a room full of peace.
     */
    @Test
    public void aRowThatSimplyOmitsANationDoesNotBeatTheTeamRule() {
        final Partida partida = lockedTeams();
        final Jogador observer = new Jogador();
        observer.setNome("Angel Alonso Padilla");
        final Nacao macedon = nacao("Macedon", BLUE), athens = nacao("Athens", BLUE);
        final Nacao persia = nacao("Persia", RED);
        macedon.setOwner(observer);
        // a complete row, which happens not to mention Persia at all
        macedon.addRelacionamento(athens, RelationshipMatrix.ALLY);

        final RelationshipMatrix matrix = new HostilityDeriver()
                .deriveNations(partida, Arrays.asList(macedon, athens, persia), observer);

        assertEquals(RelationshipMatrix.SWORN_ENEMY, matrix.getValor(macedon, persia),
                "the team flags answer this one");
        assertEquals(RelationshipMatrix.Origin.FROM_GAME_TYPE,
                matrix.getOrigin(macedon, persia));
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
