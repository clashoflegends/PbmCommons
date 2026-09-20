package business.combat;

import java.util.Arrays;
import model.Jogador;
import model.Nacao;
import model.Partida;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The nation-by-nation table: T-418's model half.
 *
 * John, 2026-09-19: "What CS needs is a map with the matrix of relationship between nations...
 * Easily read from looping the nations... then having an edit panel for the players to adjust as
 * they want. On a second pass, we can then extrapolate the non-visible gaps."
 *
 * So pass one reads, shows and lets him edit. The tests here pin the three decisions that were not
 * obvious, each of which could be got wrong in a way nothing would report:
 *
 *   a cell is DIRECTIONAL, and hostility is the OR of the two directions, because that is what the
 *   Judge does;
 *   a rule true BY CONSTRUCTION outranks a value read from a row;
 *   a pair is only ASSUMED when NEITHER direction could be read.
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
    public void theObserversOwnRowAnswersTheDirectionItStatesAndOnlyThatOne() {
        final Jogador me = jogador("j1");
        final Nacao mine = myNacao("m", "Mine", me), foe = nacao("f", "Foe");
        relate(mine, foe, RelationshipMatrix.SWORN_ENEMY);

        final RelationshipMatrix m = new HostilityDeriver()
                .deriveNations(partida(";FFA;"), Arrays.asList(mine, foe), me);

        assertEquals(RelationshipMatrix.Origin.READ_FROM_EGF, m.getOrigin(mine, foe));
        assertEquals(RelationshipMatrix.Origin.ASSUMED, m.getOrigin(foe, mine),
                "his EGF carries HIS view; the enemy's own opinion was never exported");
        assertTrue(m.isHostile(mine, foe));
        assertEquals(RelationshipMatrix.Origin.READ_FROM_EGF, m.getPairOrigin(mine, foe));
    }

    /** Two third parties in a free-for-all: neither row is readable, so the pair is a real guess. */
    @Test
    public void aPairNeitherSideCanReadIsAssumed() {
        final Jogador me = jogador("j1");
        final Nacao mine = myNacao("m", "Mine", me), x = nacao("x", "X"), y = nacao("y", "Y");

        final RelationshipMatrix m = new HostilityDeriver()
                .deriveNations(partida(";FFA;"), Arrays.asList(mine, x, y), me);

        assertEquals(RelationshipMatrix.Origin.ASSUMED, m.getPairOrigin(x, y));
        assertFalse(m.isHostile(x, y), "and the guess goes the pessimistic way: not hostile");
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

        assertFalse(m.isHostile(mine, hidden));
        assertEquals(RelationshipMatrix.Origin.ASSUMED, m.getPairOrigin(mine, hidden),
                "unknown, and marked as unknown - not promoted to a rule");
    }

    /**
     * The gaps are NOT filled by mirroring. That is a second-pass rule and it stays unwritten.
     *
     * John put extrapolation - Locked Teams, Death Match, bidirectional symmetry - in a second pass
     * on purpose: pass one reads, shows and lets him edit, so a wrong rule can never be mistaken for
     * read data. Mirroring is the tempting one, because it looks like common sense and would quietly
     * turn every unknown half into an authoritative-looking answer.
     */
    @Test
    public void anUnreadableDirectionIsNotMirroredFromTheOneThatCouldBeRead() {
        final Jogador me = jogador("j1");
        final Nacao mine = myNacao("m", "Mine", me), ally = nacao("a", "Ally");
        relate(mine, ally, RelationshipMatrix.ALLY);

        final RelationshipMatrix m = new HostilityDeriver()
                .deriveNations(partida(";FFA;"), Arrays.asList(mine, ally), me);

        assertEquals(RelationshipMatrix.ALLY, m.getValor(mine, ally));
        assertEquals(RelationshipMatrix.NEUTRAL, m.getValor(ally, mine),
                "an alliance he signed is not evidence the other side still honours it");
        assertEquals(RelationshipMatrix.Origin.ASSUMED, m.getOrigin(ally, mine));
    }
}
