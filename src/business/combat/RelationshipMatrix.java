package business.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import model.Nacao;

/**
 * Diplomacy, as a nation-by-nation table: what every nation in this battle thinks of every other,
 * where each answer came from, and which of them the player has overruled.
 *
 * <h3>Why the matrix is the law</h3>
 *
 * John, 2026-09-19: "What CS needs is a map with the matrix of relationship between nations... then
 * having an edit panel for the players to adjust as they want." The Judge agrees literally. Army
 * hostility is not an army property at all: {@code ExercitoControl.isInimigo(inimigo)} is exactly
 * {@code getNacaoControl().isInimigo(inimigo.getNacaoControl())}, which is
 * {@code getRelacionamento(nacao) < 0}. Nothing about the army - not its commander, not its combat
 * level, not its target nation - takes part in that decision. So the nation table is the input and
 * the army pairing is a projection of it, which is what this class and {@link HostilityDeriver} now
 * are.
 *
 * <h3>Cells hold the VALUE, not a boolean</h3>
 *
 * The domain is {@code -2..+4}, seven steps, and {@code msgs.BaseMsgs.nacaoRelacionamento[value + 2]}
 * already names all seven: sworn enemy, enemy, peace pact, friend, ally, vassal, lord. Storing a
 * boolean would throw away what the damage maths needs next - {@code NacaoFacade.getBonusRelacionamento}
 * reads the VALUE, not the sign - and would have to be rebuilt in Phase 5. It would also make the
 * edit panel lie: "hostile / not hostile" cannot express a vassal.
 *
 * <h3>Directional cells, symmetric hostility</h3>
 *
 * A cell is DIRECTIONAL, exactly like {@code Nacao.getRelacionamento}: A can see B as its Vassal (3)
 * while B sees A as its Lord (4), and a one-sided declaration of war is a real state of the world.
 *
 * Hostility is then the OR of the two directions, and that is the Judge's rule rather than a
 * convenience: {@code CombatArmy.isCombatCleared()} tests {@code exercito.isInimigo(inimigo)} and,
 * when it fires, writes BOTH {@code exercito.addInimigo(inimigo)} and
 * {@code inimigo.addInimigo(exercito)}. One nation declaring is enough to make both of them fight.
 *
 * <h3>Identity, never equals</h3>
 *
 * Nations are keyed by object identity. {@code Nacao} inherits {@code BaseModel.compareTo}, which
 * orders by codigo, and the source these cells are read from is a {@code TreeMap} keyed by
 * {@code Nacao} objects that XStream restores as references - a map that can fail {@code get(key)}
 * for a key that IS in its {@code keySet()}. That map is read by iteration in
 * {@link HostilityDeriver}; this one is identity-keyed so the hazard cannot reappear here.
 */
public class RelationshipMatrix {

    /** Where a cell's answer came from. Never merge these: the whole point is telling them apart. */
    public enum Origin {
        /** Read from a relationship row this scenario could PROVE complete. Authoritative. */
        READ_FROM_EGF,
        /**
         * True by CONSTRUCTION: the game type, or an NPC.
         *
         * Everyone is a sworn enemy in a Death Match, and an NPC nation is everyone's sworn enemy
         * in any game type at all. Both are rules the game cannot violate, so neither needs a
         * relationship row and neither can be overturned by one.
         *
         * Outranks a read, which is the opposite of what the army-pair derivation used to do. A
         * populated row that simply lacks the other nation answers "neutral", and that DERIVED zero
         * was winning over a rule the game cannot violate - so a Death Match could be reported as a
         * room full of peace.
         */
        FROM_GAME_TYPE,
        /**
         * The OTHER direction was read, and this one was mirrored from it.
         *
         * John, 2026-09-21: "A safer assumption would be that all diplomacy is bidirectional
         * (which it is in almost every case)."
         *
         * Much stronger than a blind guess and still not a read, which is why it is its own value.
         * A one-sided declaration IS possible - the reciprocity rules in
         * {@code NacaoControl.doArrumaRelacionamentos} are commented-out stubs - so this can be
         * wrong; it is simply wrong far less often than assuming nothing was said.
         */
        MIRRORED,
        /** Nothing could answer it from either side, so a default was applied. Must be disclosed. */
        ASSUMED,
        /** The player said so, and the player outranks every derivation. */
        PLAYER_EDITED
    }

    /** The relationship scale, as {@code GameStatusSettings} and {@code BaseMsgs} define it. */
    public static final int SWORN_ENEMY = -2;
    public static final int ENEMY = -1;
    public static final int NEUTRAL = 0;
    public static final int FRIEND = 1;
    public static final int ALLY = 2;
    public static final int VASSAL = 3;
    public static final int LORD = 4;

    private final List<Nacao> nacoes = new ArrayList<>();
    private final Map<Nacao, Map<Nacao, Integer>> valores = new IdentityHashMap<>();
    private final Map<Nacao, Map<Nacao, Origin>> origens = new IdentityHashMap<>();

    /**
     * Puts a nation in the table even if it turns out to be at peace with everyone.
     *
     * A nation fighting nobody is a real and interesting state, not an absence, and the edit panel
     * needs a row for it or the player cannot declare war from it.
     */
    public void addNacao(Nacao nacao) {
        if (nacao == null) {
            return;
        }
        for (Nacao known : nacoes) {
            if (known == nacao) {
                return;
            }
        }
        nacoes.add(nacao);
    }

    /** Every nation in this battle, in insertion order. The panel's rows and columns. */
    public List<Nacao> getNacoes() {
        return Collections.unmodifiableList(nacoes);
    }

    /**
     * Records what {@code from} thinks of {@code to}. One direction only, as the model stores it.
     *
     * A nation's view of itself is fixed at neutral and cannot be set, matching
     * {@code Nacao.getRelacionamento}, which short-circuits on {@code this == nacao}.
     */
    public void set(Nacao from, Nacao to, int valor, Origin origin) {
        if (from == null || to == null || from == to) {
            return;
        }
        addNacao(from);
        addNacao(to);
        row(valores, from).put(to, valor);
        row(origens, from).put(to, origin);
    }

    private <T> Map<Nacao, T> row(Map<Nacao, Map<Nacao, T>> outer, Nacao key) {
        Map<Nacao, T> ret = outer.get(key);
        if (ret == null) {
            ret = new IdentityHashMap<>();
            outer.put(key, ret);
        }
        return ret;
    }

    /**
     * What {@code from} thinks of {@code to}: {@code -2..+4}, or {@link #NEUTRAL} when there is no
     * cell.
     *
     * A missing cell reads neutral because that is what the rest of the game does with an unstated
     * relationship. It is NOT the same claim as a cell that says neutral, and the difference is
     * recoverable: {@link #getOrigin} answers null for a missing cell and {@link Origin#ASSUMED} for
     * one that was guessed. Nothing may collapse those two.
     */
    public int getValor(Nacao from, Nacao to) {
        if (from == null || to == null || from == to) {
            return NEUTRAL;
        }
        final Map<Nacao, Integer> row = valores.get(from);
        final Integer ret = row == null ? null : row.get(to);
        return ret == null ? NEUTRAL : ret;
    }

    /** @return where this cell came from, or null when there is no cell at all. */
    public Origin getOrigin(Nacao from, Nacao to) {
        if (from == null || to == null || from == to) {
            return null;
        }
        final Map<Nacao, Origin> row = origens.get(from);
        return row == null ? null : row.get(to);
    }

    /**
     * Will these two fight? The OR of the two directions, because the Judge writes both enemy lists
     * the moment either direction reads hostile. See the class note.
     */
    public boolean isHostile(Nacao one, Nacao other) {
        if (one == null || other == null || one == other) {
            return false;
        }
        return getValor(one, other) < 0 || getValor(other, one) < 0;
    }

    /**
     * The origin that should be reported for the PAIR: the BEST-founded of its two directions.
     *
     * When they fight, it is the origin of whichever direction is hostile - a read declaration of
     * war outranks the peaceful silence pointing the other way, and one declaration is enough.
     *
     * When they do not fight, the STRONGEST claim wins, not the weakest. That is the decision the
     * assumption count turns on, so it is worth stating why. A nation's proven-complete row is the
     * best evidence that exists about whether it is at war, and reading the observer's own row is
     * how the whole Counselor already answers "is this my enemy"
     * ({@code mine.isInimigo(theirs)}). The reverse cell stays ASSUMED and the panel still shows it
     * that way - the player can see and edit the half nobody told him about - but the PAIR is not a
     * guess merely because the other nation did not volunteer its opinion. Calling it one would
     * make the disclosure count fire on every normal game and stop meaning anything, which is the
     * failure R-15 exists to prevent, arrived at from the other side.
     *
     * ASSUMED is therefore reserved for the pairs where NEITHER direction could be read: two third
     * parties in a free-for-all, which is the case the design turns on.
     *
     * @return null when neither direction has a cell
     */
    public Origin getPairOrigin(Nacao one, Nacao other) {
        if (one == null || other == null || one == other) {
            return null;
        }
        final Origin there = getOrigin(one, other);
        final Origin back = getOrigin(other, one);
        if (isHostile(one, other)) {
            return getValor(one, other) < 0 ? there : back;
        }
        return rank(there) >= rank(back) ? there : back;
    }

    /** How well-founded a claim is. Higher wins when the two directions disagree about that. */
    private static int rank(Origin origin) {
        if (origin == null) {
            return 0;
        }
        switch (origin) {
            case PLAYER_EDITED:
                return 5;
            case FROM_GAME_TYPE:
                return 4;
            case READ_FROM_EGF:
                return 3;
            case MIRRORED:
                return 2;
            default:
                return 1;
        }
    }
}
