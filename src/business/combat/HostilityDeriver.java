package business.combat;

import business.facade.PartidaFacade;
import business.interfaces.IExercito;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import model.Jogador;
import model.Nacao;
import model.Partida;

/**
 * Builds a {@link HostilityMatrix} from the game type and whatever relationships the loaded EGFs
 * actually contain. The ONE place the type-to-model mapping lives.
 *
 * <h3>Three sources of truth, and no fourth</h3>
 *
 * <ol>
 *   <li><b>Read</b> a relationship row from a loaded EGF. Authoritative, and always preferred.</li>
 *   <li><b>Derive</b> from the game type: in a Death Match every pair fights, and that is complete.</li>
 *   <li><b>Assume</b>, and say so.</li>
 * </ol>
 *
 * Nothing else. Not nation colour, not "hostile to my enemy", not transitive alliance. An inference
 * that looks like intelligence is the one most likely to be confidently wrong.
 *
 * <h3>Only the observer's own rows are complete</h3>
 *
 * The Counselor loads exactly ONE results EGF. There is no merging of an ally's file into it, so
 * the only complete relationship rows in the world are the observer's own nations' - plus every row
 * when the game runs public diplomacy.
 *
 * Everything else is a fragment. The server exports a foreign nation's relationships only when they
 * are positive and aimed at the player, so a hostile nation arrives with an EMPTY map and a friendly
 * one with a map of exactly ONE entry, and {@code Nacao.getRelacionamento} catches the resulting
 * miss and answers 0 = neutral, silently. Asking such a nation about its enemies returns "none"
 * whatever the truth is.
 *
 * Completeness is therefore PROVEN inside this class rather than promised by the caller - see
 * {@link #isComplete}. A caller cannot be asked to carry a server-side export rule in its head, and
 * the one-entry fragment looks exactly like a real row from the outside.
 *
 * <h3>Why the relationship map is iterated, never queried</h3>
 *
 * {@code relacionamentos} is a {@code TreeMap} keyed by {@code Nacao} objects that XStream restores
 * as references. Such a map can fail {@code get(key)} for a key that IS in its {@code keySet()}, and
 * {@code getRelacionamento} turns that miss into a silent neutral. So this class reads each row once
 * by {@code entrySet()} and compares nations by identity afterwards. Never call
 * {@code isInimigo}/{@code getRelacionamento} from here.
 */
public class HostilityDeriver {

    private final PartidaFacade partidaFacade = new PartidaFacade();

    /**
     * @param partida      the game, for its type flags
     * @param armies       every army in the scenario
     * @param observer the player at the keyboard, whose own nations carry complete rows. Nothing
     *                 else is read unless the game runs public diplomacy; see {@link #isComplete}.
     * @return a populated matrix, every hostile cell tagged with where its answer came from
     */
    public HostilityMatrix derive(Partida partida, Collection<? extends IExercito> armies,
            Jogador observer) {
        final HostilityMatrix ret = new HostilityMatrix();
        for (IExercito army : armies) {
            ret.addArmy(army);
        }
        final List<IExercito> list = new ArrayList<>(armies);
        final List<Nacao> candidates = new ArrayList<>(list.size());
        for (IExercito army : list) {
            candidates.add(army.getNacao());
        }
        final Map<Nacao, Map<Nacao, Integer>> known = readRows(partida, observer, candidates);

        for (int ii = 0; ii < list.size(); ii++) {
            for (int jj = ii + 1; jj < list.size(); jj++) {
                final IExercito one = list.get(ii), other = list.get(jj);
                final Nacao nOne = one.getNacao(), nOther = other.getNacao();
                if (nOne == null || nOther == null || nOne == nOther) {
                    // an army never fights itself, its own nation, or an army whose owner is unknown
                    continue;
                }
                final Integer read = lookup(known, nOne, nOther);
                if (read != null) {
                    if (read < 0) {
                        ret.setHostile(one, other, HostilityMatrix.Origin.READ_FROM_EGF);
                    }
                    continue;
                }
                if (isEveryoneHostile(partida)) {
                    ret.setHostile(one, other, HostilityMatrix.Origin.FROM_GAME_TYPE);
                    continue;
                }
                // Nothing read it and the type does not settle it, so default to NOT hostile - and
                // MARK it, because a guessed peace looks exactly like a known one otherwise.
                //
                // Not hostile is the pessimistic direction, which is what makes it safe to default to
                // rather than merely convenient: in the engine each army spends its attack on the
                // armies in its own enemy list, so two of the observer's enemies who also fight each
                // other split their fire, while two who ignore each other both aim everything at him.
                // The assumption can therefore overstate a threat and cannot understate one.
                ret.markAssumed(one, other);
            }
        }
        return ret;
    }

    /**
     * Is this army hostile to the owner of a city? The city-assault gate, answered by the same three
     * sources and the same safe reading as {@link #derive}.
     *
     * Kept here rather than in {@link LayerParticipation} so that every relationship read in the
     * simulator goes through one class. A caller tempted to write {@code mine.isInimigo(theirs)}
     * inline would reintroduce exactly the silent-neutral miss this class exists to avoid.
     *
     * @return true when they fight; false covers both a known peace and an unresolvable pair, since
     *         an army that cannot be shown to be hostile does not assault
     */
    public boolean isHostileToCity(Partida partida, IExercito army, Nacao cityOwner,
            Jogador observer) {
        if (army == null || cityOwner == null) {
            return false;
        }
        final Nacao mine = army.getNacao();
        if (mine == null || mine == cityOwner) {
            return false;
        }
        final Integer read = lookup(
                readRows(partida, observer, Arrays.asList(mine, cityOwner)), mine, cityOwner);
        if (read != null) {
            return read < 0;
        }
        return isEveryoneHostile(partida);
    }

    /**
     * Death Match and Gun Boat: every nation is hostile to every other by construction, diplomacy is
     * disabled, and the answer needs no relationship row at all.
     *
     * Through {@link PartidaFacade}, not {@code Partida.isDeathMatch()}: a flag declared by the
     * SCENARIO counts as the game's, which is how the Judge reads it, and the model's own accessor
     * would silently answer "no" for such a game.
     */
    private boolean isEveryoneHostile(Partida partida) {
        return partidaFacade.isDeathMatch(partida);
    }

    /**
     * Reads every nation whose row this class can PROVE is complete, ONCE, by iteration, into an
     * identity-keyed lookup. See the class note: querying these maps directly is unreliable and
     * fails silently.
     *
     * The proof is done here rather than trusted from the caller, because the caller cannot be
     * asked to remember a server-side export rule. See {@link #isComplete}.
     */
    private Map<Nacao, Map<Nacao, Integer>> readRows(Partida partida, Jogador observer,
            Collection<Nacao> candidates) {
        final Map<Nacao, Map<Nacao, Integer>> ret = new IdentityHashMap<>();
        final boolean everythingExported = partida != null && partida.hasHabilidade(";SPD;");
        for (Nacao nacao : candidates) {
            if (nacao == null || ret.containsKey(nacao)
                    || !isComplete(nacao, everythingExported, observer)) {
                continue;
            }
            final Map<Nacao, Integer> row = new IdentityHashMap<>();
            for (Map.Entry<Nacao, Integer> entry : nacao.getRelacionamentos().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    row.put(entry.getKey(), entry.getValue());
                }
            }
            ret.put(nacao, row);
        }
        return ret;
    }

    /**
     * Can this nation's relationship row be trusted as its COMPLETE set? Exactly two ways, and
     * they mirror the server's export rule one for one:
     *
     * <ol>
     *   <li>the game runs with public diplomacy, so every row is exported in full;</li>
     *   <li>the nation is the observer's own, so his own EGF carries all of it.</li>
     * </ol>
     *
     * <b>Every other row is a fragment, and a fragment is worse than nothing.</b> The server also
     * exports a foreign nation's single POSITIVE entry when it is aimed at the observer, so a
     * friendly foreign nation arrives with a row of size one. That row is not empty, so the
     * empty-row guard in {@link #read} does not catch it, and reading it would answer "neutral"
     * for every third party it never mentions - authoritative, tagged READ_FROM_EGF, and wrong.
     * That is the silent-neutral trap one layer up, which is why completeness is PROVEN here
     * instead of being a promise the caller makes.
     *
     * There is no third way, and in particular an ally's file is not one: the Counselor loads a
     * single results EGF and never merges another, so no foreign nation's own row is ever present.
     *
     * {@code getOwner()} is set on the observer's own nations only. The server does set an owner on
     * an ally's nation in a team-locked game, but it sets the ALLY's owner, not the observer's, so
     * identity against the observer stays the right test. Compared by identity, never
     * {@code Jogador.isNacao}, which can answer for a nation that is not the observer's.
     */
    private boolean isComplete(Nacao nacao, boolean everythingExported, Jogador observer) {
        return everythingExported || (observer != null && nacao.getOwner() == observer);
    }

    /**
     * The relationship between two nations, from whichever of them has a loaded row, or null when
     * neither does.
     *
     * Both directions are tried because either nation's EGF may be the one that was loaded, and
     * relationships are reciprocated in practice. A nation with a loaded row but no entry for the
     * other is a real answer, not a miss: it means the two are neutral, and it returns 0.
     */
    private Integer lookup(Map<Nacao, Map<Nacao, Integer>> known, Nacao one, Nacao other) {
        final Integer fromOne = read(known.get(one), other);
        if (fromOne != null) {
            return fromOne;
        }
        return read(known.get(other), one);
    }

    /**
     * Reads one nation's row, or null when that row cannot answer.
     *
     * <b>An EMPTY row is not evidence of peace.</b> That is the whole trap restated: the server
     * exports a foreign nation's relationships only when they are positive and aimed at the player,
     * so a nation at war with everyone arrives with nothing in its map. Treating that silence as
     * neutrality is how the reversed test fails silently, and passing such a nation in as "loaded"
     * would smuggle the same bug in through the front door. An empty row therefore carries no
     * information and the pair falls through to an explicit assumption.
     *
     * A POPULATED row that simply lacks the other nation is a real answer, and means neutral: a
     * nation's own EGF carries its complete set.
     */
    private Integer read(Map<Nacao, Integer> row, Nacao about) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        final Integer ret = row.get(about);
        return ret == null ? Integer.valueOf(0) : ret;
    }
}
