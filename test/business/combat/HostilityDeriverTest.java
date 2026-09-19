package business.combat;

import java.util.Arrays;
import model.Jogador;
import model.Nacao;
import model.Partida;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Who fights whom, derived from the game type and the one EGF the Counselor has.
 *
 * Three sources and no fourth: a relationship READ from a row the deriver can prove complete, an
 * inference FROM the game type, or a marked ASSUMPTION. The tests below exist because the tempting
 * shortcuts here are all silent ones - a reversed relationship test answers neutral without
 * complaining, and a guessed peace is indistinguishable from a known one unless it is tagged.
 *
 * The observer is what makes a row complete. The Counselor loads a single results file, so the only
 * complete rows in it are his own nations' - or every row, when the game runs public diplomacy.
 */
public class HostilityDeriverTest {

    private static Jogador jogador(String codigo) {
        final Jogador ret = new Jogador();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        return ret;
    }

    private static Nacao nacao(String codigo, String nome) {
        final Nacao ret = new Nacao();
        ret.setCodigo(codigo);
        ret.setNome(nome);
        return ret;
    }

    /** A nation of the observer's: the one kind whose relationship row arrives complete. */
    private static Nacao myNacao(String codigo, String nome, Jogador observer) {
        final Nacao ret = nacao(codigo, nome);
        ret.setOwner(observer);
        return ret;
    }

    private static ArmySim army(String nome, Nacao nacao) {
        final ArmySim ret = new ArmySim(nome, null, nacao);
        ret.setCodigo(nome);
        return ret;
    }

    private static Partida partida(String habilidades) {
        final Partida ret = new Partida();
        ret.setCodigo("g1");
        ret.setNome("Test Game");
        for (String cd : habilidades.split(",")) {
            if (!cd.isEmpty()) {
                final model.Habilidade hab = new model.Habilidade();
                hab.setCodigo(cd);
                hab.setNome(cd);
                ret.addHabilidade(hab);
            }
        }
        return ret;
    }

    /** Teach one nation about another. Mirrors what a loaded EGF carries for its own nation. */
    private static void relate(Nacao from, Nacao to, int valor) {
        from.getRelacionamentos().put(to, valor);
    }

    @Test
    public void deathMatchMakesEveryPairHostileWithoutAnyRelationshipRow() {
        final Nacao a = nacao("a", "A"), b = nacao("b", "B"), c = nacao("c", "C");
        final ArmySim one = army("one", a), two = army("two", b), three = army("three", c);

        final HostilityMatrix m = new HostilityDeriver()
                .derive(partida(";GDM;,;GND;"), Arrays.asList(one, two, three), null);

        assertTrue(m.isInimigo(one, two));
        assertTrue(m.isInimigo(two, three));
        assertTrue(m.isInimigo(one, three));
        assertEquals(HostilityMatrix.Origin.FROM_GAME_TYPE, m.getOrigin(one, three),
                "a Death Match needs no relationship row, and should say so");
        assertEquals(0, m.getAssumedPairs().size(), "nothing should be guessed in a Death Match");
    }

    @Test
    public void aReadRowBeatsAnyInference() {
        final Jogador me = jogador("j1");
        final Nacao mine = myNacao("m", "Mine", me), foe = nacao("f", "Foe");
        relate(mine, foe, -2);
        final ArmySim one = army("one", mine), two = army("two", foe);

        final HostilityMatrix m = new HostilityDeriver()
                .derive(partida(";FFA;"), Arrays.asList(one, two), me);

        assertTrue(m.isInimigo(one, two));
        assertEquals(HostilityMatrix.Origin.READ_FROM_EGF, m.getOrigin(one, two));
        assertEquals(0, m.getAssumedPairs().size(), "a pair we can read is not a guess");
    }

    @Test
    public void aPositiveRelationshipIsNotHostileAndIsNotAGuess() {
        final Jogador me = jogador("j1");
        final Nacao mine = myNacao("m", "Mine", me), ally = nacao("a", "Ally");
        relate(mine, ally, 2);
        final ArmySim one = army("one", mine), two = army("two", ally);

        final HostilityMatrix m = new HostilityDeriver()
                .derive(partida(";FFA;"), Arrays.asList(one, two), me);

        assertFalse(m.isInimigo(one, two));
        assertEquals(0, m.getAssumedPairs().size());
    }

    /**
     * The case the whole design turns on. In a free-for-all the observer knows his own row and
     * nothing else, so a pair of third parties cannot be resolved - and must be MARKED, because a
     * guessed peace looks exactly like a known one.
     */
    @Test
    public void thirdPartyPairsAreAssumedNotHostileAndSaidSo() {
        final Jogador me = jogador("j1");
        final Nacao mine = myNacao("m", "Mine", me), x = nacao("x", "X"), y = nacao("y", "Y");
        relate(mine, x, -2);
        relate(mine, y, -2);
        final ArmySim mineArmy = army("me", mine), one = army("one", x), two = army("two", y);

        final HostilityMatrix m = new HostilityDeriver()
                .derive(partida(";FFA;"), Arrays.asList(mineArmy, one, two), me);

        assertTrue(m.isInimigo(mineArmy, one), "my own row is authoritative");
        assertTrue(m.isInimigo(mineArmy, two), "my own row is authoritative");
        assertFalse(m.isInimigo(one, two), "X vs Y is unknowable, so they do not fight");
        assertTrue(m.isAssumed(one, two), "and that has to be marked as a guess");
        assertEquals(1, m.getAssumedPairs().size());
    }

    /**
     * An ally's own row is never present, because there is never a second EGF.
     *
     * The Counselor loads exactly ONE results file. What it holds about a foreign nation is whatever
     * the server chose to export into that file, which for a friendly foreigner is a single positive
     * entry aimed at the observer - never that nation's own complete set. So a pair between an ally
     * and a third party stays unresolved however friendly the ally is.
     */
    @Test
    public void anAlliedNationsRowIsStillAFragment() {
        final Jogador me = jogador("j1");
        final Nacao mine = myNacao("m", "Mine", me), ally = nacao("a", "Ally"), foe = nacao("f", "Foe");
        relate(mine, ally, 2);
        relate(ally, mine, 2);      // the one entry the server exports about a friendly foreigner
        // ally-versus-foe is absent: the ally's own row was never exported into my file
        final ArmySim mineArmy = army("me", mine), friend = army("friend", ally),
                enemy = army("enemy", foe);

        final HostilityMatrix m = new HostilityDeriver()
                .derive(partida(";FFA;"), Arrays.asList(mineArmy, friend, enemy), me);

        assertTrue(m.isAssumed(friend, enemy),
                "a one-entry fragment cannot answer for a nation it never mentions");
        assertFalse(m.isInimigo(friend, enemy));
    }

    /**
     * The silent trap this class exists to avoid. A hostile foreign nation ships an EMPTY
     * relationship map, and asking it about its enemies answers "none" without complaining. So a
     * foreign nation's silence must never be read as peace - and here it is not even read, because
     * no row present can be shown to be complete.
     */
    @Test
    public void aForeignHostileNationsEmptyMapIsNotEvidenceOfPeace() {
        final Nacao mine = nacao("m", "Mine"), foe = nacao("f", "Foe");
        relate(mine, foe, -2);          // but nobody here is the observer, so nothing is complete
        final ArmySim me = army("me", mine), enemy = army("enemy", foe);

        final HostilityMatrix m = new HostilityDeriver()
                .derive(partida(";FFA;"), Arrays.asList(me, enemy), null);

        assertFalse(m.isInimigo(me, enemy), "an unreadable row cannot see the war");
        assertTrue(m.isAssumed(me, enemy),
                "so it must be reported as a guess, not as a known peace");
    }

    /**
     * The fragment, and the reason completeness is proven rather than promised. The server exports a
     * foreign nation's single POSITIVE entry when it points at the observer, so that nation arrives
     * with a row of exactly one entry. It is not empty, so an empty-row guard sails past it, and
     * reading it would answer "neutral" for every third party it never mentions.
     */
    @Test
    public void aForeignNationsOneEntryFragmentIsNotACompleteRow() {
        final Jogador me = jogador("j1");
        final Nacao mine = myNacao("m", "Mine", me), friendly = nacao("a", "Friendly"),
                x = nacao("x", "X");
        relate(friendly, mine, 2);
        final ArmySim mineArmy = army("me", mine), them = army("them", friendly),
                third = army("third", x);

        final HostilityMatrix m = new HostilityDeriver()
                .derive(partida(";FFA;"), Arrays.asList(mineArmy, them, third), me);

        assertTrue(m.isAssumed(them, third),
                "a one-entry fragment must not answer for a nation it never mentions");
        assertFalse(m.isInimigo(them, third));
    }

    /**
     * With public diplomacy every row is exported in full, so a third-party pair the observer could
     * never otherwise see is readable and nothing needs to be assumed.
     *
     * The observer still gets a relationship of his own here. That is not decoration: the
     * belt-and-braces empty-row guard in {@code read} cannot tell a genuinely friendless nation from
     * a censored export, so an all-empty row still falls through to an assumption even under
     * {@code ;SPD;}. It over-marks, which is the harmless direction, and it is worth keeping as a
     * second line of defence behind the completeness proof.
     */
    @Test
    public void publicDiplomacyMakesEveryRowComplete() {
        final Nacao mine = nacao("m", "Mine"), x = nacao("x", "X"), y = nacao("y", "Y");
        relate(x, y, -2);
        relate(mine, x, 2);
        final ArmySim me = army("me", mine), one = army("one", x), two = army("two", y);

        final HostilityMatrix m = new HostilityDeriver()
                .derive(partida(";FFA;,;SPD;"), Arrays.asList(me, one, two), null);

        assertTrue(m.isInimigo(one, two), "X vs Y is readable when diplomacy is public");
        assertEquals(HostilityMatrix.Origin.READ_FROM_EGF, m.getOrigin(one, two));
        assertEquals(0, m.getAssumedPairs().size());
    }

    /** Ownership is identity against the observer, not a name or a flag. */
    @Test
    public void onlyTheObserversOwnNationCarriesACompleteRow() {
        final Jogador me = jogador("j1"), someoneElse = jogador("j2");
        final Nacao mine = myNacao("m", "Mine", me), foe = nacao("f", "Foe");
        relate(mine, foe, -2);
        final ArmySim one = army("one", mine), two = army("two", foe);

        assertEquals(HostilityMatrix.Origin.READ_FROM_EGF, new HostilityDeriver()
                .derive(partida(";FFA;"), Arrays.asList(one, two), me).getOrigin(one, two));

        assertTrue(new HostilityDeriver()
                .derive(partida(";FFA;"), Arrays.asList(one, two), someoneElse).isAssumed(one, two),
                "another player's view of my nation reads nothing");
    }

    @Test
    public void twoArmiesOfTheSameNationNeverFight() {
        final Nacao mine = nacao("m", "Mine");
        final ArmySim one = army("one", mine), two = army("two", mine);

        final HostilityMatrix m = new HostilityDeriver()
                .derive(partida(";GDM;"), Arrays.asList(one, two), null);

        assertFalse(m.isInimigo(one, two), "not even in a Death Match");
        assertFalse(m.hasCombat());
    }

    @Test
    public void cityHostilityUsesTheSameSafeReading() {
        final Jogador me = jogador("j1");
        final Nacao mine = myNacao("m", "Mine", me), owner = nacao("o", "Owner");
        relate(mine, owner, -2);
        final ArmySim army = army("me", mine);
        final HostilityDeriver deriver = new HostilityDeriver();

        assertTrue(deriver.isHostileToCity(partida(";FFA;"), army, owner, me));
        assertFalse(deriver.isHostileToCity(partida(";FFA;"), army, mine, me),
                "an army never assaults its own nation's city");
        assertFalse(deriver.isHostileToCity(partida(";FFA;"), army, owner, null),
                "unresolvable means no assault, not an assumed one");
    }
}
