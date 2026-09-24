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
 * <h3>Four sources of truth, and no fifth</h3>
 *
 * <ol>
 *   <li><b>Derive</b> by construction, and FIRST: in a Death Match every pair fights, in a locked
 *       team game the team flags settle every pair, and an NPC nation is everyone's sworn enemy.
 *       All three are complete answers needing no row - see {@link #isNpc} and {@link #byTeam}.
 *       First because a rule the game cannot violate must not lose to an inference; the argument
 *       is on {@link #deriveNations}.</li>
 *   <li><b>Read</b> a relationship row from a loaded EGF, when that row can be PROVEN complete.</li>
 *   <li><b>Mirror</b> the other direction when only it can be read - diplomacy is bidirectional
 *       in almost every case.</li>
 *   <li><b>Assume</b> the worst case - hostile to the player, friendly among themselves - and
 *       say so.</li>
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
                // An EXPLICIT entry in a row this class could prove complete outranks the team
                // rule, for one reason and it is not a special case: the flags describe TURN ZERO
                // and the row describes now.
                //
                // doCarregaRelacionamentosFresh seeds the table from the team flags, and only when
                // isRandom(); every later turn reads whatever the stored table has become
                // (doCarregaRelacionamentosDb). Nothing protects it in between - ;GND; is a SITE
                // flag that stops the diplomacy order being offered to players, and the Judge never
                // reads it at all. So the observer's own row is simply the more current answer, and
                // the several Judge paths that can move a relationship (Ordem188Diplomacy,
                // MilestoneDiplomacia, DiplomaticChange, Ordem555PlaceCamp) are examples of that,
                // not the reason for it.
                //
                // EXPLICIT, not read(): a populated row that simply lacks the other nation answers
                // a DERIVED zero, and letting that fabricated neutral outrank a construction rule
                // is how a Death Match once came out a room full of peace. That path stays below
                // the team rule, where it was.
                final Integer stated = readExplicit(known.get(from), to);
                if (stated != null) {
                    ret.set(from, to, stated, RelationshipMatrix.Origin.READ_FROM_EGF);
                    continue;
                }
                final Integer team = byTeam(partida, from, to);
                if (team != null) {
                    ret.set(from, to, team, RelationshipMatrix.Origin.FROM_GAME_TYPE);
                    continue;
                }
                final Integer read = read(known.get(from), to);
                if (read != null) {
                    ret.set(from, to, read, RelationshipMatrix.Origin.READ_FROM_EGF);
                    continue;
                }
                // MIRROR the other direction when it can be read. John, 2026-09-21: "A safer
                // assumption would be that all diplomacy is bidirectional (which it is in almost
                // every case)." A one-sided declaration is possible - the reciprocity rules in
                // NacaoControl are commented-out stubs - so this can be wrong, just far less often
                // than pretending nothing was said. Marked MIRRORED, never READ.
                final Integer reverse = read(known.get(to), from);
                if (reverse != null) {
                    ret.set(from, to, reverse, RelationshipMatrix.Origin.MIRRORED);
                    continue;
                }
                // Neither side could answer. The default depends on WHO the pair is, because
                // the worst case is not the same for every pair. John, 2026-09-21: "let's assume
                // that they are hostile to me and my team while friends between them. As it is the
                // worse case and also the more probable."
                //
                // That is sharper than a blanket answer either way, and it is worth recording why
                // both blanket answers are wrong. Assume everything peaceful and an unknown nation
                // ignores the player, which understates what he is walking into. Assume everything
                // hostile and the unknowns fight EACH OTHER, splitting their attacks - in the
                // engine an army spends its damage on the armies in its own enemy list - so the
                // threat to him comes out LOWER than the peaceful assumption would give. Hostile
                // everywhere is not the pessimistic reading; it only looks like it.
                //
                // The genuine worst case is the one below: they all come for him, and none of them
                // is distracted by the others. It is also the likelier one - a player who cannot
                // read a relationship is usually the outsider, not the confidant.
                //
                // ENEMY rather than SWORN_ENEMY: hostile enough to fight, without claiming the
                // extreme the game reserves for a declared blood feud.
                final boolean againstTheObserver =
                        isObservers(from, observer) || isObservers(to, observer);
                ret.set(from, to,
                        againstTheObserver ? RelationshipMatrix.ENEMY : RelationshipMatrix.NEUTRAL,
                        RelationshipMatrix.Origin.ASSUMED);
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
                }
                // Marked SEPARATELY from hostility, not as its else-branch. Since the default
                // became "assume hostile" the two coincide, and folding them together silently
                // dropped the disclosure count to zero - every guess became an unannounced war.
                // R-15 is about whether the player was TOLD, which is independent of the answer.
                if (origin == RelationshipMatrix.Origin.ASSUMED) {
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
     * A locked team game answers the WHOLE table from the team flags, for every pair, including two
     * nations the observer has nothing to do with.
     *
     * <h3>This is what makes a battle the player is not in simulable at all</h3>
     *
     * Without it, two foreign nations fall all the way through to {@link #deriveNations}'s last
     * resort. That default is built around the observer - "hostile to me, friendly among
     * themselves" - and neither of them is the observer, so the pair comes out NEUTRAL, the matrix
     * reports no combat, and {@link RunGate#NO_HOSTILE_PAIR} disables Run on a hex where two armies
     * are plainly about to fight. Verified on game 903 turn 3 hex 1815: Athens and Persia, one BLUE
     * one RED, neither of them Macedon's, and the simulator would not start.
     *
     * <h3>Why this is a rule and not a guess</h3>
     *
     * It is {@code NacaoControl.doCarregaRelacionamentosFresh}, transcribed. Same team is
     * {@code RELATIONSHIP_ALLY}, a different team is {@code RELATIONSHIP_SWORNENEMY}, and
     * {@code ;GSL;} is tested first because its own comment says it "superseeds LockedAlliances".
     * Checked against every complete row in all four player EGFs of game 903 turn 3: four nations,
     * sixteen cells, every one of them exactly what the team flags predict. See
     * {@code HostilityDeriverTeamTest}.
     *
     * The datum is free. {@code ServerNacaoDao} sets {@code nm_alianca} in the block it calls
     * "publicos", before any visibility gating, so the team flag ships in every EGF for every
     * nation - even under {@code ;GAP;}, which hides the OWNER and leaves the team alone.
     *
     * <h3>{@code ;GND;} is required, and it does less than it looks</h3>
     *
     * The Judge's rule SEEDS the table, and it only runs at all when {@code isRandom()}; after that
     * {@code doCarregaRelacionamentosDb} reads whatever the table has become. So a team flag
     * describes turn zero and nothing keeps it true afterwards - {@code ;GND;} is a SITE flag that
     * stops the diplomacy order being offered, and no Judge code reads it. It is used here as a
     * proxy for "the players cannot renegotiate", which makes the seed a good default; where the
     * seed is wrong, the observer's own row says so and outranks this. {@code ConverterFactory.getGameType} emits {@code ;GLA;;GND;} together for every
     * team type, so this costs nothing in practice - but a hand-built game carrying {@code ;GLA;}
     * alone gets no rule here, and falls through to the read and the assumption, which is right.
     *
     * <h3>An empty team is not a team</h3>
     *
     * {@code "-"} is what the Judge writes for a nation on no team, and {@code MilestoneGameOver}
     * treats it as the Barbarians' marker. Two such nations are not allies, so a blank, a null and
     * a {@code "-"} all mean "cannot answer" and fall through. The Judge does not need this guard
     * because its NPC branch catches neutrals first; ours cannot, since the deciding column
     * {@code tp_nacao} is not in the EGF - see {@link #isNpc}.
     *
     * @return the relationship value, or null when the game type cannot answer this pair
     */
    private Integer byTeam(Partida partida, Nacao from, Nacao to) {
        final boolean withLord = partidaFacade.isTeamWithLord(partida);
        if (!withLord && !partidaFacade.isTeamLocked(partida)) {
            return null;
        }
        // FFA is tested BEFORE the team branches in the Judge and settles every pair at neutral,
        // so a game carrying both flags is a free-for-all and the team flags say nothing. Only a
        // hand-built game can be in that state - ConverterFactory never emits the pair - but the
        // whole claim of this method is that it is a transcription, and a transcription that skips
        // a branch is just a paraphrase.
        if (partidaFacade.isFreeForAll(partida)) {
            return null;
        }
        // ;GND; required, but NOT because it freezes the table - nothing does. It is a SITE flag
        // (PbmSite's own comment: "it is now a site-only flag") that stops the diplomacy order
        // being offered, and the Judge never reads it. What it buys is a proxy: where players
        // cannot renegotiate, the turn-zero seed is usually still the truth, so deriving from the
        // flags is sound. Where it is NOT the truth the observer's own row says so, and that read
        // now outranks this rule - see deriveNations. Without ;GND; the flags are only a seed and
        // this stands down entirely.
        if (!partidaFacade.isDiplomacyDisabled(partida)) {
            return null;
        }
        final String one = teamOf(from), other = teamOf(to);
        if (one == null || other == null) {
            return null;
        }
        if (!one.equals(other)) {
            return RelationshipMatrix.SWORN_ENEMY;
        }
        if (withLord) {
            // THE JUDGE'S NUMBERS, not the matching constant NAMES, and they are not the same
            // thing: GameStatusSettings has RELATIONSHIP_VASAL = 4 and RELATIONSHIP_LORD = 3, while
            // RelationshipMatrix - following BaseMsgs.nacaoRelacionamento and Nacao.isLord, which
            // both read 4 as the LORD - has them the other way round. Transcribing by name would
            // have written the opposite number into every graded cell of a ;GSL; game.
            //
            // It is the NUMBER that matters: getBonusRelacionamento indexes a table with it and
            // the Diplomacy panel names it from BaseMsgs. Combat is unaffected either way -
            // dificuldadeBonus is 25 at 2, 3 and 4 alike - so this is about the word the panel
            // shows and about a read cell and a derived cell agreeing.
            if (from.hasHabilidade(";NSL;")) {
                return 4;   // the Judge's RELATIONSHIP_VASAL
            }
            if (to.hasHabilidade(";NSL;")) {
                return 3;   // the Judge's RELATIONSHIP_LORD
            }
        }
        return RelationshipMatrix.ALLY;
    }

    /**
     * A nation's team EXACTLY as the Judge compares it, or null when the flag is absent entirely.
     *
     * <b>{@code "-"} is a team name here, and that is a correction.</b> This used to treat it as
     * "no team" and fall through, on the reasoning that two teamless nations are not allies. The
     * Judge does not agree and does not need to: {@code "BLUE".equals("-")} is false, so a teamed
     * nation and a teamless one are SWORN ENEMIES, and two teamless ones are ALLIES. Falling
     * through instead reproduced the exact bug this method was written to fix - a Hidden Team game
     * ({@code ;GAP;}) hides the owner, so {@code isNacaoBarbarian} cannot identify the NPC either,
     * and a barbarian stack against a foreign army came out NEUTRAL with Run disabled.
     *
     * Compared RAW, no trimming, because the Judge compares raw. A stray space in {@code nm_alianca}
     * would then read as a different team in both places rather than as an unreported war in one.
     */
    private static String teamOf(Nacao nacao) {
        final String ret = nacao == null ? null : nacao.getTeamFlag();
        return ret == null || ret.isEmpty() ? null : ret;
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
     * <b>Through {@link PartidaFacade}, which is a DELIBERATE DIVERGENCE and was documented as the
     * opposite.</b> The facade chains game then scenario; the Judge does not.
     * {@code PartidaControl.isDeathMatch()} is {@code getPartidaModel().isDeathMatch()}, i.e.
     * {@code Partida.hasHabilidade(";GDM;")} against the GAME's own map only, and
     * {@code doCarregaRelacionamentosFresh} never goes near the chaining accessor. This javadoc used
     * to claim the chain was "how the Judge reads it", which is exactly backwards.
     *
     * The chain is kept because a scenario that declares a game type is describing the game, and
     * answering "no" to it would be the worse failure. It is only reachable at all if a scenario row
     * carries one of these codes: none of 25 sampled live EGFs does, so the divergence is latent
     * today. The same applies to {@link #byTeam}'s reading of {@code ;GLA;}, {@code ;GSL;} and
     * {@code ;GND;}.
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
        return everythingExported || isObservers(nacao, observer);
    }

    /**
     * Is this one of the player's own nations?
     *
     * Identity against the observer, exactly as {@link #isComplete} tests it and for the same
     * reason: {@code getOwner()} is set on the observer's own nations, and the server does set an
     * owner on an ALLY's nation in a team-locked game - the ally's owner, not the observer's - so
     * identity is what distinguishes them.
     *
     * <b>"My team" is deliberately still not answered here, and no longer needs to be.</b> John's
     * rule was "hostile to me and my team", and a locked-team ally is part of that worst case - but
     * in a locked team game {@link #byTeam} has already settled every pair by construction, so no
     * pair involving a teammate ever reaches this default. What is left is FFA and Battle Royale,
     * where the team flag says nothing and "my team" has no meaning either. So this covers the
     * player's own nations, which is now the whole of what it can mean.
     */
    private boolean isObservers(Nacao nacao, Jogador observer) {
        return observer != null && nacao != null && nacao.getOwner() == observer;
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
    /**
     * The value this row STATES about that nation, or null when it does not mention it.
     *
     * The difference from {@link #read} is the whole of it: this never manufactures a zero. A
     * populated row lacking an entry is a real answer and means neutral, but it is a DERIVED answer
     * and must not outrank a rule the game cannot violate - so the two live at different heights in
     * {@link #deriveNations}, and only the stated one goes above the team rule.
     *
     * The map is the identity-keyed copy {@link #readRows} built by iteration, not the model's own
     * TreeMap, so {@code get} is safe here.
     */
    private Integer readExplicit(Map<Nacao, Integer> row, Nacao about) {
        return row == null || row.isEmpty() ? null : row.get(about);
    }

    private Integer read(Map<Nacao, Integer> row, Nacao about) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        final Integer ret = row.get(about);
        return ret == null ? Integer.valueOf(0) : ret;
    }
}
