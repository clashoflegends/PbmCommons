package business.combat;

import java.util.Arrays;
import model.Jogador;
import model.Nacao;
import model.Partida;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The nation-by-nation table: T-418's model half.
 *
 * John, 2026-09-19: "What CS needs is a map with the matrix of relationship between nations...
 * Easily read from looping the nations... then having an edit panel for the players to adjust as
 * they want. On a second pass, we can then extrapolate the non-visible gaps."
 *
 * Pass one read, showed and let him edit. The first extrapolation landed 2026-09-21, also on his
 * call - mirroring, and an unread pair assumed HOSTILE - so what these tests pin is now:
 *
 *   a cell is DIRECTIONAL, and hostility is the OR of the two directions, because that is what the
 *   Judge does;
 *   a rule true BY CONSTRUCTION outranks a value read from a row;
 *   an unreadable direction is MIRRORED from the readable one and labelled as such;
 *   a pair neither side can read is assumed HOSTILE, and disclosed.
 */
public class RelationshipMatrixTest {

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

    private static Nacao myNacao(String codigo, String nome, Jogador observer) {
        final Nacao ret = nacao(codigo, nome);
        ret.setOwner(observer);
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

    private static final String FRIENDS =
            "assumed FRIENDLY with each other, because the worst case for the player is that "
            + "neither of them is distracted by the other";

    private static void relate(Nacao from, Nacao to, int valor) {
        from.getRelacionamentos().put(to, valor);
    }

    // ------------------------------------------------------------------ the table itself

    /**
     * A vassal and its lord hold OPPOSITE values, so a symmetric cell could not express them.
     *
     * This is why the table is not a boolean and not mirrored: {@code Nacao.getRelacionamento} is
     * directional, and 3 ("that nation is my Vassal") against 4 ("that nation is my Lord") is a
     * perfectly ordinary pair of rows.
     */
    @Test
    public void aCellIsDirectional() {
        final RelationshipMatrix m = new RelationshipMatrix();
        final Nacao lord = nacao("l", "Lord"), vassal = nacao("v", "Vassal");
        m.set(lord, vassal, RelationshipMatrix.VASSAL, RelationshipMatrix.Origin.READ_FROM_EGF);
        m.set(vassal, lord, RelationshipMatrix.LORD, RelationshipMatrix.Origin.READ_FROM_EGF);

        assertEquals(RelationshipMatrix.VASSAL, m.getValor(lord, vassal));
        assertEquals(RelationshipMatrix.LORD, m.getValor(vassal, lord));
        assertFalse(m.isHostile(lord, vassal));
    }

    /**
     * ONE declaration is enough. The Judge's rule, not a convenience:
     * {@code CombatArmy.isCombatCleared()} tests {@code exercito.isInimigo(inimigo)} and then writes
     * BOTH enemy lists, so a nation that has not declared back still ends up in the battle.
     *
     * Asymmetry is real in this game, which is why this matters: the reciprocity rules in
     * {@code NacaoControl.doArrumaRelacionamentos} are commented-out stubs, and
     * {@code Ordem555PlaceCamp} has to set both directions by hand when it wants symmetry.
     */
    @Test
    public void oneSidedWarStillMakesThePairHostile() {
        final RelationshipMatrix m = new RelationshipMatrix();
        final Nacao aggressor = nacao("a", "A"), victim = nacao("v", "V");
        m.set(aggressor, victim, RelationshipMatrix.SWORN_ENEMY,
                RelationshipMatrix.Origin.READ_FROM_EGF);
        m.set(victim, aggressor, RelationshipMatrix.NEUTRAL, RelationshipMatrix.Origin.ASSUMED);

        assertTrue(m.isHostile(aggressor, victim));
        assertTrue(m.isHostile(victim, aggressor), "hostility has no direction, only the value does");
        assertEquals(RelationshipMatrix.Origin.READ_FROM_EGF,
                m.getPairOrigin(aggressor, victim),
                "the declaration decided it, not the silence pointing the other way");
    }

    /** A nation is never its own enemy, and its view of itself cannot be set. */
    @Test
    public void aNationIsNeutralTowardsItself() {
        final RelationshipMatrix m = new RelationshipMatrix();
        final Nacao one = nacao("a", "A");
        m.set(one, one, RelationshipMatrix.SWORN_ENEMY, RelationshipMatrix.Origin.PLAYER_EDITED);

        assertEquals(RelationshipMatrix.NEUTRAL, m.getValor(one, one));
        assertFalse(m.isHostile(one, one));
        assertNull(m.getOrigin(one, one));
    }

    /** A missing cell reads neutral, but must stay distinguishable from a cell that says neutral. */
    @Test
    public void anAbsentCellIsNotTheSameClaimAsAStatedNeutral() {
        final RelationshipMatrix m = new RelationshipMatrix();
        final Nacao a = nacao("a", "A"), b = nacao("b", "B");

        assertEquals(RelationshipMatrix.NEUTRAL, m.getValor(a, b));
        assertNull(m.getOrigin(a, b), "nothing was said, which is not the same as saying neutral");

        m.set(a, b, RelationshipMatrix.NEUTRAL, RelationshipMatrix.Origin.ASSUMED);
        assertEquals(RelationshipMatrix.NEUTRAL, m.getValor(a, b));
        assertEquals(RelationshipMatrix.Origin.ASSUMED, m.getOrigin(a, b));
    }

    // ------------------------------------------------------------------ derivation

    /**
     * A rule true BY CONSTRUCTION beats a value read from a row. This ORDER is the fix.
     *
     * The army-pair derivation asked the EGF first and consulted the game type only when the read
     * came back null - but a populated row that merely lacks the other nation answers a DERIVED
     * zero, never null. So in a Death Match that fabricated neutral outranked a rule the game
     * cannot violate, and the sim could report a room full of peace.
     */
    @Test
    public void theGameTypeOutranksAReadRelationship() {
        final Jogador me = jogador("j1");
        final Nacao mine = myNacao("m", "Mine", me), other = nacao("o", "Other");
        relate(mine, other, RelationshipMatrix.ALLY);       // an ally, on paper

        final RelationshipMatrix m = new HostilityDeriver()
                .deriveNations(partida(";GDM;"), Arrays.asList(mine, other), me);

        assertTrue(m.isHostile(mine, other), "a Death Match has no allies, whatever the row says");
        assertEquals(RelationshipMatrix.Origin.FROM_GAME_TYPE, m.getOrigin(mine, other));
    }

    /**
     * The observer's own row answers for the PAIR, and the reverse cell stays marked as a guess.
     *
     * Both halves matter. The pair is not a guess - reading his own row is how the whole Counselor
     * answers "is this my enemy", and calling it uncertain would make the disclosure count fire on
     * every normal game and stop meaning anything. But the cell nobody told him about is still
     * ASSUMED, so the panel can show it as such and he can correct it.
     */
    @Test
    public void theObserversOwnRowIsReadAndTheReverseIsMirroredFromIt() {
        final Jogador me = jogador("j1");
        final Nacao mine = myNacao("m", "Mine", me), foe = nacao("f", "Foe");
        relate(mine, foe, RelationshipMatrix.SWORN_ENEMY);

        final RelationshipMatrix m = new HostilityDeriver()
                .deriveNations(partida(";FFA;"), Arrays.asList(mine, foe), me);

        assertEquals(RelationshipMatrix.Origin.READ_FROM_EGF, m.getOrigin(mine, foe));
        assertEquals(RelationshipMatrix.Origin.MIRRORED, m.getOrigin(foe, mine),
                "his EGF carries HIS view; the enemy's own opinion is MIRRORED from it, not read");
        assertTrue(m.isHostile(mine, foe));
        assertEquals(RelationshipMatrix.Origin.READ_FROM_EGF, m.getPairOrigin(mine, foe),
                "and the pair reports the better-founded of its two directions");
    }

    /**
     * An unresolvable pair INVOLVING the player is assumed hostile to him. John, 2026-09-21:
     * "let's assume that they are hostile to me and my team while friends between them. As it is
     * the worse case and also the more probable."
     */
    @Test
    public void anUnresolvablePairInvolvingThePlayerIsAssumedHostileToHim() {
        final Jogador me = jogador("j1");
        final Nacao mine = myNacao("m", "Mine", me), stranger = nacao("s", "Stranger");
        // mine's row is EMPTY, so nothing can be read in either direction

        final RelationshipMatrix m = new HostilityDeriver()
                .deriveNations(partida(";FFA;"), Arrays.asList(mine, stranger), me);

        assertTrue(m.isHostile(mine, stranger),
                "a faction he cannot read is assumed to be coming for him");
        assertEquals(RelationshipMatrix.Origin.ASSUMED, m.getPairOrigin(mine, stranger),
                "and it is still disclosed as a guess");
    }

    /** Two third parties in a free-for-all: neither row is readable, so the pair is a real guess. */
    @Test
    public void aPairNeitherSideCanReadIsAssumed() {
        final Jogador me = jogador("j1");
        final Nacao mine = myNacao("m", "Mine", me), x = nacao("x", "X"), y = nacao("y", "Y");

        final RelationshipMatrix m = new HostilityDeriver()
                .deriveNations(partida(";FFA;"), Arrays.asList(mine, x, y), me);

        assertEquals(RelationshipMatrix.Origin.ASSUMED, m.getPairOrigin(x, y));
        assertFalse(m.isHostile(x, y), "X and Y are both third parties: " + FRIENDS);
    }

    /**
     * An NPC is everyone's sworn enemy, in any game type, outranking whatever a row says.
     *
     * The Judge's rule is unconditional: {@code this.isNpc() || nacaoAlvo.isNpc() ||
     * getPartida().isDeathMatch()} all land on SWORNENEMY. John, 2026-09-19: "for the NPC, just
     * assume sworn enemy and aggressive" - aggression is assumed rather than inferred, because
     * {@code npcAggresive} comes from a DB column that is not in the EGF.
     */
    @Test
    public void anNpcNationIsEveryonesSwornEnemy() {
        final Jogador me = jogador("j1");
        final Nacao mine = myNacao("m", "Mine", me), npc = nacao("n", "Wildlings");
        relate(mine, npc, RelationshipMatrix.ALLY);        // a treaty, on paper
        final model.Habilidade aiw = new model.Habilidade();
        aiw.setCodigo(";AIW;");
        aiw.setNome(";AIW;");
        npc.addHabilidade(aiw);

        final RelationshipMatrix m = new HostilityDeriver()
                .deriveNations(partida(";FFA;"), Arrays.asList(mine, npc), me);

        assertTrue(m.isHostile(mine, npc), "you do not sign treaties with an NPC");
        assertEquals(RelationshipMatrix.Origin.FROM_GAME_TYPE, m.getOrigin(mine, npc));
        assertEquals(RelationshipMatrix.Origin.FROM_GAME_TYPE, m.getOrigin(npc, mine),
                "and it holds in both directions, by construction");
    }

    /**
     * A nation with no visible Jogador is NOT an NPC, and this is the case that had to be refused.
     *
     * {@code Jogador} is gated on export and {@code ;GAP;} hides it ON PURPOSE, so "no player means
     * NPC" would declare a secret human player the sworn enemy of everyone on the hex - a war
     * invented by a rule. Under-reporting an NPC costs a guess the player can fix in the diplomacy
     * panel; that costs him a wrong answer dressed as a fact.
     */
    @Test
    public void aNationWithNoVisiblePlayerIsNotTreatedAsAnNpc() {
        final Jogador me = jogador("j1");
        final Nacao mine = myNacao("m", "Mine", me), hidden = nacao("h", "House Hidden");

        final RelationshipMatrix m = new HostilityDeriver()
                .deriveNations(partida(";FFA;"), Arrays.asList(mine, hidden), me);

        // It IS hostile now, because an unread pair is assumed hostile - but the ORIGIN is what
        // this test is about: a hidden player must stay a GUESS, never promoted to FROM_GAME_TYPE
        // the way a real NPC is.
        assertEquals(RelationshipMatrix.Origin.ASSUMED, m.getPairOrigin(mine, hidden),
                "unknown, and marked as unknown - not promoted to a rule");
        assertNotEquals(RelationshipMatrix.Origin.FROM_GAME_TYPE, m.getOrigin(mine, hidden),
                "a secret human player is not an NPC");
    }

    /**
     * An unreadable direction is MIRRORED from the one that can be read, and labelled as mirrored.
     *
     * John, 2026-09-21: "A safer assumption would be that all diplomacy is bidirectional (which it
     * is in almost every case)." This was deliberately NOT done in pass one, and the objection then
     * was that a rule filling a cell becomes indistinguishable from read data. The answer to that
     * objection is {@link RelationshipMatrix.Origin#MIRRORED}: the inference is made AND marked, so
     * the panel shows it in italic and the player can see which half nobody actually told him.
     *
     * It can still be wrong - a one-sided declaration is legal, since the reciprocity rules in
     * {@code NacaoControl.doArrumaRelacionamentos} are commented-out stubs - just far less often
     * than pretending nothing was said.
     */
    @Test
    public void anUnreadableDirectionIsMirroredFromTheOneThatCouldBeRead() {
        final Jogador me = jogador("j1");
        final Nacao mine = myNacao("m", "Mine", me), ally = nacao("a", "Ally");
        relate(mine, ally, RelationshipMatrix.ALLY);

        final RelationshipMatrix m = new HostilityDeriver()
                .deriveNations(partida(";FFA;"), Arrays.asList(mine, ally), me);

        assertEquals(RelationshipMatrix.ALLY, m.getValor(mine, ally));
        assertEquals(RelationshipMatrix.ALLY, m.getValor(ally, mine),
                "mirrored, because diplomacy is bidirectional in almost every case");
        assertEquals(RelationshipMatrix.Origin.MIRRORED, m.getOrigin(ally, mine),
                "and labelled as an inference, never as a read");
        assertEquals(RelationshipMatrix.Origin.READ_FROM_EGF, m.getOrigin(mine, ally),
                "while the direction that WAS read keeps its own origin");
    }

    /** Mirroring makes a one-sided war visible from both sides, which is what the Judge resolves. */
    @Test
    public void aReadDeclarationIsMirroredOntoTheSilentSide() {
        final Jogador me = jogador("j1");
        final Nacao mine = myNacao("m", "Mine", me), foe = nacao("f", "Foe");
        relate(mine, foe, RelationshipMatrix.SWORN_ENEMY);

        final RelationshipMatrix m = new HostilityDeriver()
                .deriveNations(partida(";FFA;"), Arrays.asList(mine, foe), me);

        assertEquals(RelationshipMatrix.SWORN_ENEMY, m.getValor(foe, mine));
        assertEquals(RelationshipMatrix.Origin.MIRRORED, m.getOrigin(foe, mine));
        assertTrue(m.isHostile(mine, foe));
    }
}
