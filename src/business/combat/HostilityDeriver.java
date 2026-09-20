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
 * Builds the nation-by-nation {@link RelationshipMatrix} from the game type and whatever
 * relationships the loaded EGF actually contains, and projects it onto the armies present. The ONE
 * place the type-to-model mapping lives.
 *
 * <h3>Nations first, armies second</h3>
 *
 * This used to derive army pairs directly and never name a nation table at all. It is two steps now
 * because the Judge only ever had one input: {@code ExercitoControl.isInimigo(inimigo)} is exactly
 * {@code getNacaoControl().isInimigo(inimigo.getNacaoControl())}. Separating them is what lets the
 * player SEE the table and edit it (T-418), and it is what makes the game-type rules orderable
 * against the read ones - see {@link #deriveNations}.
 *
 * <h3>Three sources of truth, and no fourth</h3>
 *
 * <ol>
 *   <li><b>Read</b> a relationship row from a loaded EGF. Authoritative, and always preferred.</li>
 *   <li><b>Derive</b> by construction: in a Death Match every pair fights, and an NPC nation is
 *       everyone's sworn enemy. Both are complete answers needing no row - see {@link #isNpc}.</li>
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
    private final business.facade.NacaoFacade nacaoFacade = new business.facade.NacaoFacade();

    /**
     * @param partida      the game, for its type flags
     * @param armies       every army in the scenario
     * @param observer the player at the keyboard, whose own nations carry complete rows. Nothing
     *                 else is read unless the game runs public diplomacy; see {@link #isComplete}.
     * @return a populated matrix, every hostile cell tagged with where its answer came from
     */
    public HostilityMatrix derive(Partida partida, Collection<? extends IExercito> armies,
            Jogador observer) {
        final List<IExercito> list = new ArrayList<>(armies);
        return project(deriveNations(partida, nacoesOf(list), observer), list);
    }

    /**
     * The nation table: every nation against every other, valued and tagged with its source.
     *
     * <b>Game type first, read second.</b> That order is a fix, not a preference. The army-pair
     * derivation this replaces asked the EGF first and only consulted the game type when the read
     * came back null - but {@link #read} answers a DERIVED zero for a populated row that simply
     * lacks the other nation, so that fabricated neutral outranked a rule the game cannot violate
     * and a Death Match could be reported as a room full of peace. A rule that is true by
     * CONSTRUCTION cannot be beaten by an inference.
     *
     * <b>Each direction is read from its own nation's row, and never from the reverse one.</b> If
     * the observer's nation calls X an enemy, that is a fact about the observer, and it makes the
     * PAIR hostile through {@link RelationshipMatrix#isHostile}, which is the OR of both
     * directions - exactly as the Judge does. What it is not is evidence about X's own view, so the
     * reverse cell stays {@link RelationshipMatrix.Origin#ASSUMED} rather than being mirrored.
     * Filling that cell by symmetry is one of the extrapolation rules, and John put those in a
     * second pass on purpose: pass one reads, shows and lets him edit, so that a wrong rule can
     * never be mistaken for read data.
     *
     * @param nacoes   every nation in the battle, the city's owner included
     * @param observer the player at the keyboard, whose own nations carry complete rows
     */
    public RelationshipMatrix deriveNations(Partida partida, Collection<Nacao> nacoes,
            Jogador observer) {
        final RelationshipMatrix ret = new RelationshipMatrix();
        final List<Nacao> list = new ArrayList<>();
        for (Nacao nacao : nacoes) {
            if (nacao != null && !contains(list, nacao)) {
                list.add(nacao);
                ret.addNacao(nacao);
            }
        }
        final Map<Nacao, Map<Nacao, Integer>> known = readRows(partida, observer, list);
        final boolean everyoneHostile = isEveryoneHostile(partida);

        for (Nacao from : list) {
            for (Nacao to : list) {
                if (from == to) {
                    continue;
                }
                if (everyoneHostile || isNpc(from) || isNpc(to)) {
                    ret.set(from, to, RelationshipMatrix.SWORN_ENEMY,
                            RelationshipMatrix.Origin.FROM_GAME_TYPE);
                    continue;
                }
                final Integer read = read(known.get(from), to);
                if (read != null) {
                    ret.set(from, to, read, RelationshipMatrix.Origin.READ_FROM_EGF);
                    continue;
                }
                // Nothing could answer it, so default to neutral - and MARK it, because a guessed
                // peace looks exactly like a known one otherwise.
                //
                // Not hostile is the pessimistic direction, which is what makes it safe to default
                // to rather than merely convenient: in the engine each army spends its attack on
                // the armies in its own enemy list, so two of the observer's enemies who also fight
                // each other split their fire, while two who ignore each other both aim everything
                // at him. The assumption can therefore overstate a threat and cannot understate one.
                ret.set(from, to, RelationshipMatrix.NEUTRAL, RelationshipMatrix.Origin.ASSUMED);
            }
        }
        return ret;
    }

    /**
     * Projects the nation table onto the armies actually standing on the hex.
     *
     * A pure projection, and that is the Judge's own structure rather than a simplification:
     * {@code ExercitoControl.isInimigo(inimigo)} is nothing but
     * {@code getNacaoControl().isInimigo(inimigo.getNacaoControl())}. No property of the army takes
     * part. So everything interesting happens in the nation table, and this loop only decides which
     * PAIRS of the armies present inherit it.
     */
    public HostilityMatrix project(RelationshipMatrix nations, List<? extends IExercito> armies) {
        final HostilityMatrix ret = new HostilityMatrix();
        for (IExercito army : armies) {
            ret.addArmy(army);
        }
        for (int ii = 0; ii < armies.size(); ii++) {
            for (int jj = ii + 1; jj < armies.size(); jj++) {
                final IExercito one = armies.get(ii), other = armies.get(jj);
                final Nacao nOne = one.getNacao(), nOther = other.getNacao();
                if (nOne == null || nOther == null || nOne == nOther) {
                    // an army never fights itself, its own nation, or one whose owner is unknown
                    continue;
                }
                final RelationshipMatrix.Origin origin = nations.getPairOrigin(nOne, nOther);
                if (nations.isHostile(nOne, nOther)) {
                    ret.setHostile(one, other, toHostilityOrigin(origin));
                } else if (origin == RelationshipMatrix.Origin.ASSUMED) {
                    ret.markAssumed(one, other);
                }
            }
        }
        return ret;
    }

    /** The two enums say the same four things; they are separate so neither layer owns the other. */
    private static HostilityMatrix.Origin toHostilityOrigin(RelationshipMatrix.Origin origin) {
        if (origin == null) {
            return HostilityMatrix.Origin.ASSUMED;
        }
        switch (origin) {
            case READ_FROM_EGF:
                return HostilityMatrix.Origin.READ_FROM_EGF;
            case FROM_GAME_TYPE:
                return HostilityMatrix.Origin.FROM_GAME_TYPE;
            case PLAYER_EDITED:
                return HostilityMatrix.Origin.PLAYER_EDITED;
            default:
                return HostilityMatrix.Origin.ASSUMED;
        }
    }

    /** Identity, not equals: see the class note on why these nations are never used as map keys. */
    private static boolean contains(List<Nacao> list, Nacao wanted) {
        for (Nacao one : list) {
            if (one == wanted) {
                return true;
            }
        }
        return false;
    }

    private static List<Nacao> nacoesOf(Collection<? extends IExercito> armies) {
        final List<Nacao> ret = new ArrayList<>();
        for (IExercito army : armies) {
            ret.add(army.getNacao());
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
        return deriveNations(partida, Arrays.asList(mine, cityOwner), observer)
                .isHostile(mine, cityOwner);
    }

    /**
     * Death Match: every nation is hostile to every other by construction, diplomacy is disabled,
     * and the answer needs no relationship row at all.
     *
     * DEATH MATCH ONLY. This said "Death Match and Gun Boat", and the code has never consulted a
     * Gun Boat flag - in Gun Boat the nations simply cannot TALK to each other, which says nothing
     * about who is at war with whom, so there is no rule to consult. The sentence was describing a
     * rule that does not exist.
     *
     * Through {@link PartidaFacade}, not {@code Partida.isDeathMatch()}: a flag declared by the
     * SCENARIO counts as the game's, which is how the Judge reads it, and the model's own accessor
     * would silently answer "no" for such a game.
     */
    private boolean isEveryoneHostile(Partida partida) {
        return partidaFacade.isDeathMatch(partida);
    }

    /**
     * An NPC nation is everybody's sworn enemy, whatever the game type says.
     *
     * The Judge's rule, and it is unconditional:
     * {@code this.isNpc() || nacaoAlvo.isNpc() || getPartida().isDeathMatch()} all land on
     * {@code RELATIONSHIP_SWORNENEMY} in {@code NacaoControl.doCarregaRelacionamentosFresh}.
     *
     * <b>What the client can actually see, and what it cannot.</b> The Judge's own test is
     * {@code isNpcAggresive() || isDead() || (isAtiva() && hasHabilidade(";AIW;"))}, and
     * {@code npcAggresive} comes from the DB column {@code tp_nacao = 'AI'} - a field on
     * {@code NacaoControl} that is NOT in the EGF and has no counterpart on {@code model.Nacao}. So
     * the deciding datum is simply absent here and no amount of care recovers it.
     *
     * John, 2026-09-19: "for the NPC, just assume sworn enemy and aggressive." So aggression is not
     * inferred, it is assumed - any nation this can identify as an NPC at all is treated as an
     * active one. What is left is the identification, and it deliberately uses only signals that
     * cannot mistake a human player for an NPC:
     *
     * <ul>
     *   <li>the Barbarians, via the same test that already supplies a stand-in city owner;</li>
     *   <li>{@code ;AIW;} or {@code ;PAI;} on the nation, the two habilidades the Judge's own
     *       {@code isNpc}/{@code isNpcDefault} read. They reach the client only under
     *       {@code loadAll} or {@code ;SNAI;}, so they fire rarely - but when they are there they
     *       are authoritative and cost nothing to ask.</li>
     * </ul>
     *
     * Rejected: "any nation with no human player". {@code Jogador} is itself gated on export, and
     * {@code ;GAP;} hides it ON PURPOSE - so that test would declare a secret human player the
     * sworn enemy of everyone on the hex and invent a war that does not exist. Under-reporting an
     * NPC costs a guess the player can correct in the diplomacy panel; inventing a war between two
     * humans is a wrong answer presented as a rule.
     */
    private boolean isNpc(Nacao nacao) {
        return nacao != null && (nacao.hasHabilidade(";AIW;") || nacao.hasHabilidade(";PAI;")
                || nacaoFacade.isNacaoBarbarian(nacao));
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
