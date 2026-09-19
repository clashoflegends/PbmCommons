package business.combat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import model.Nacao;
import model.Partida;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Who fights whom, derived from the game type and the EGFs actually loaded.
 *
 * Three sources and no fourth: a relationship READ from a loaded EGF, an inference FROM the game
 * type, or a marked ASSUMPTION. The tests below exist because the tempting shortcuts here are all
 * silent ones - a reversed relationship test answers neutral without complaining, and a guessed peace
 * is indistinguishable from a known one unless it is tagged.
 */
public class HostilityDeriverTest {

    private static Nacao nacao(String codigo, String nome) {
        final Nacao ret = new Nacao();
        ret.setCodigo(codigo);
        ret.setNome(nome);
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
                .derive(partida(";GDM;,;GND;"), Arrays.asList(one, two, three), new ArrayList<>());

        assertTrue(m.isInimigo(one, two));
        assertTrue(m.isInimigo(two, three));
        assertTrue(m.isInimigo(one, three));
        assertEquals(HostilityMatrix.Origin.FROM_GAME_TYPE, m.getOrigin(one, three),
                "a Death Match needs no relationship row, and should say so");
        assertEquals(0, m.getAssumedPairs().size(), "nothing should be guessed in a Death Match");
    }

    @Test
    public void aReadRowBeatsAnyInference() {
        final Nacao mine = nacao("m", "Mine"), foe = nacao("f", "Foe");
        relate(mine, foe, -2);
        final ArmySim one = army("one", mine), two = army("two", foe);

        final HostilityMatrix m = new HostilityDeriver()
                .derive(partida(";FFA;"), Arrays.asList(one, two), Arrays.asList(mine));

        assertTrue(m.isInimigo(one, two));
        assertEquals(HostilityMatrix.Origin.READ_FROM_EGF, m.getOrigin(one, two));
        assertEquals(0, m.getAssumedPairs().size(), "a pair we can read is not a guess");
    }

    @Test
    public void aPositiveRelationshipIsNotHostileAndIsNotAGuess() {
        final Nacao mine = nacao("m", "Mine"), ally = nacao("a", "Ally");
        relate(mine, ally, 2);
        final ArmySim one = army("one", mine), two = army("two", ally);

        final HostilityMatrix m = new HostilityDeriver()
                .derive(partida(";FFA;"), Arrays.asList(one, two), Arrays.asList(mine));

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
        final Nacao mine = nacao("m", "Mine"), x = nacao("x", "X"), y = nacao("y", "Y");
        relate(mine, x, -2);
        relate(mine, y, -2);
        final ArmySim me = army("me", mine), one = army("one", x), two = army("two", y);

        final HostilityMatrix m = new HostilityDeriver()
                .derive(partida(";FFA;"), Arrays.asList(me, one, two), Arrays.asList(mine));

        assertTrue(m.isInimigo(me, one), "my own row is authoritative");
        assertTrue(m.isInimigo(me, two), "my own row is authoritative");
        assertFalse(m.isInimigo(one, two), "X vs Y is unknowable, so they do not fight");
        assertTrue(m.isAssumed(one, two), "and that has to be marked as a guess");
        assertEquals(1, m.getAssumedPairs().size());
    }

    /**
     * Known rows scale with EGFs LOADED, not with one point of view. Merging an ally's EGF resolves
     * pairs the observer alone could not.
     */
    @Test
    public void aMergedAllyContributesItsOwnCompleteRow() {
        final Nacao mine = nacao("m", "Mine"), ally = nacao("a", "Ally"), foe = nacao("f", "Foe");
        relate(mine, ally, 2);
        relate(ally, foe, -2);
        final ArmySim me = army("me", mine), friend = army("friend", ally), enemy = army("enemy", foe);
        final List<ArmySim> all = Arrays.asList(me, friend, enemy);

        final HostilityMatrix alone = new HostilityDeriver()
                .derive(partida(";FFA;"), all, Arrays.asList(mine));
        assertFalse(alone.isInimigo(friend, enemy), "without the ally's EGF this is unknowable");
        assertTrue(alone.isAssumed(friend, enemy));

        final HostilityMatrix merged = new HostilityDeriver()
                .derive(partida(";FFA;"), all, Arrays.asList(mine, ally));
        assertTrue(merged.isInimigo(friend, enemy), "the ally's own row answers it");
        assertEquals(HostilityMatrix.Origin.READ_FROM_EGF, merged.getOrigin(friend, enemy));
        assertFalse(merged.isAssumed(friend, enemy));
    }

    /**
     * The silent trap this class exists to avoid. A hostile foreign nation ships an EMPTY
     * relationship map, and asking it about its enemies answers "none" without complaining. So an
     * unloaded nation's silence must never be read as peace.
     */
    @Test
    public void anUnloadedHostileNationsEmptyMapIsNotEvidenceOfPeace() {
        final Nacao mine = nacao("m", "Mine"), foe = nacao("f", "Foe");
        relate(mine, foe, -2);          // my EGF knows
        // foe.relacionamentos stays empty, exactly as the server exports it
        final ArmySim me = army("me", mine), enemy = army("enemy", foe);

        final HostilityMatrix m = new HostilityDeriver()
                .derive(partida(";FFA;"), Arrays.asList(me, enemy), Arrays.asList(foe));

        assertFalse(m.isInimigo(me, enemy),
                "reading only the enemy's empty row cannot see the war");
        assertTrue(m.isAssumed(me, enemy),
                "so it must be reported as a guess, not as a known peace");
    }

    @Test
    public void twoArmiesOfTheSameNationNeverFight() {
        final Nacao mine = nacao("m", "Mine");
        final ArmySim one = army("one", mine), two = army("two", mine);

        final HostilityMatrix m = new HostilityDeriver()
                .derive(partida(";GDM;"), Arrays.asList(one, two), Arrays.asList(mine));

        assertFalse(m.isInimigo(one, two), "not even in a Death Match");
        assertFalse(m.hasCombat());
    }

    @Test
    public void cityHostilityUsesTheSameSafeReading() {
        final Nacao mine = nacao("m", "Mine"), owner = nacao("o", "Owner");
        relate(mine, owner, -2);
        final ArmySim me = army("me", mine);
        final HostilityDeriver deriver = new HostilityDeriver();

        assertTrue(deriver.isHostileToCity(partida(";FFA;"), me, owner, Arrays.asList(mine)));
        assertFalse(deriver.isHostileToCity(partida(";FFA;"), me, mine, Arrays.asList(mine)),
                "an army never assaults its own nation's city");
        assertFalse(deriver.isHostileToCity(partida(";FFA;"), me, owner, new ArrayList<>()),
                "unresolvable means no assault, not an assumed one");
    }
}
