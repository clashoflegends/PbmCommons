package business.combat;

import business.facade.BattleSimFacade;
import business.facade.CenarioFacade;
import business.facade.ExercitoFacade;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Artefato;
import model.Cenario;
import model.Pelotao;
import model.Personagem;
import model.TipoTropa;
import msgs.BaseMsgs;

/**
 * Fights the land layer of a simulated battle, the way the live games actually fight it.
 *
 * <h3>Which engine this is, and why it matters</h3>
 *
 * The Judge has TWO combat engines. This one models {@code CombateTmpbm}, the traditional engine
 * that nearly every live game runs and the one the design named for MVP. The newer
 * {@code CombatBase / CombatArmy / CombatLand} family is a DIFFERENT algorithm - platoon against
 * platoon, one attack spent down one casualty-ordered list, stopping at the first defender that
 * survives - and modelling it here produced numbers wrong in a way a player would notice: with two
 * armies on one side, the first absorbed everything and the second took no casualties at all.
 *
 * {@code CombateTmpbm} works at ARMY level instead:
 *
 * <ol>
 *   <li>Each army computes ONE attack value for the round: {@code forcaPlus} plus {@code
 *       forcaBasica} scaled by the relationship and tactic modifiers.</li>
 *   <li>That attack is dealt to EVERY enemy, split by each enemy's share of the total enemy troop
 *       count. Two enemies of equal size each take half; nobody is skipped.</li>
 *   <li>Damage accumulates and is suffered at the END of the round, so every army in a round strikes
 *       the enemy as it stood when the round began.</li>
 *   <li>The banked damage becomes casualties either proportionally across every platoon or platoon
 *       by platoon down the casualty order - the same split {@link CasualtyMode} shows the
 *       player.</li>
 * </ol>
 *
 * Verified against a real turn: game 901, turn 8, the battle at 1141. The Judge dealt 1297 damage to
 * 778 Reavers with 7780 defence and killed 130 of them, and 1310 to 786 Reavers with 7860 defence
 * and killed 131. Both fall out of the arithmetic below exactly, including where it truncates.
 *
 * <h3>It orchestrates, it does not calculate</h3>
 *
 * Every number comes from something the Judge itself calls - {@link BattleSimFacade} for attack and
 * defence, {@link CenarioFacade} for the tactic table, {@link BaseMsgs#dificuldadeBonus} for the
 * relationship modifier. No combat arithmetic is reimplemented here, so the simulator cannot drift
 * away from the Judge one rounding rule at a time.
 *
 * <h3>It runs on COPIES</h3>
 *
 * The Judge resolves in place because its armies are the world. Here they are what the player typed,
 * and he will press Run again after changing a tactic - so the battle is fought by clones and the
 * originals are untouched.
 *
 * <h3>What it does NOT do, and says so</h3>
 *
 * <ul>
 *   <li>The NAVY and CITY layers. Land only; the three-layer chain is T-801's remainder, and every
 *       result says so.</li>
 *   <li>The commander's combat artifact and travelling-character bonuses inside {@code forcaPlus}.
 *       They are the secrecy seam (T-808) and are not in the EGF for an army seen from outside.</li>
 *   <li>{@code doTroopPowers} - corruption and undead raising. It needs the habilidade DAO, so a
 *       battle with {@code ;TCT;}-family troops is reported as incomplete rather than quietly
 *       resolved without them.</li>
 *   <li>Any narrative. Numbers only; the messages are T-802/T-803.</li>
 * </ul>
 */
public class LandCombatResolver {

    /**
     * A simulator has to stop; the Judge does not.
     *
     * {@code CombateTmpbm} loops while some army still has an enemy and still has defence, which is
     * safe there because a real battle always kills somebody. A simulator is handed whatever the
     * player typed, and two armies whose attack rounds to zero satisfy that condition forever.
     * Stopping loudly is the honest answer; a frozen window is not.
     */
    private static final int MAX_ROUNDS = 100;

    /**
     * What a travelling NPC adds to an army's attack, HERE: nothing. T-808, decision D-10.
     *
     * <h3>Withheld by design, not missing by accident</h3>
     *
     * NPC combat impact is one of the few things the game deliberately keeps private (John,
     * 2026-09-18, amending the parity target to "roughly 97 percent": "we don't want to give away
     * the few secrets such as NPCs behaviours and the calculation details of their impact in
     * combat"). This class is in PUBLIC PbmCommons, read by external collaborators, so the formula
     * cannot live here - and it did, spelled out, along with a worked magnitude. That is what this
     * removes.
     *
     * <h3>The player is not left stuck</h3>
     *
     * He is left INFORMED, which is the same seam an enemy commander's combat artifact already
     * uses: the result discloses the exclusion, and the army's **Attack bonus** field is where he
     * types what he believes it is worth. A number he supplied is honest; a number this class
     * derived from a private formula would be neither honest nor private.
     *
     * <h3>What this costs, stated plainly</h3>
     *
     * A battle with a dragon in it forecasts low until he fills that in. The gap is large - it was
     * the single biggest term in one checked battle. Design 6.1 is also candid that the protection
     * is weak: a deterministic simulator plus a published turn makes a flat additive term
     * recoverable by subtraction, so this buys friction rather than secrecy. That trade is John's
     * and it is recorded as D-10; if it moves, this constant and its disclosure move with it.
     */
    private static final int NPC_COMBAT_CONTRIBUTION = 0;

    private static final BattleSimFacade battleSimFacade = new BattleSimFacade();
    private static final ExercitoFacade exercitoFacade = new ExercitoFacade();
    private final CenarioFacade cenarioFacade = new CenarioFacade();

    /**
     * Fights the land battle and reports what is left.
     *
     * @param scenario the player's setup, LEFT UNTOUCHED
     * @param cenario  for the tactic-versus-tactic bonus table and the casualty-order rule
     */
    public CombatResult resolve(CombatScenario scenario, Cenario cenario) {
        final CombatResult ret = new CombatResult();
        ret.addNote("BATTLESIM.RESULT.LANDONLY");
        if (scenario == null) {
            return ret;
        }
        // No sea layer in front of this entry, so the anchoring the chain does between the two
        // happens here instead. Without it the boats stay aboard and inflate their owner's share of
        // the incoming damage - the Seagard defect.
        final CombatCopies copies = CombatCopies.of(scenario);
        copies.doAnchor(scenario);
        return resolve(scenario, cenario, copies, ret);
    }

    /**
     * Fights the land battle on copies SOMEBODY ELSE owns, so a later layer inherits the result.
     *
     * This is the entry {@link CombatChain} uses, and the reason it exists is that the Judge runs
     * every layer on ONE set of {@code ExercitoControl} objects: casualties from the army layer
     * shrink what the city layer can bring to the walls, an army destroyed here does not assault,
     * the one-time attack magic is spent once across all of them, and ships anchored for the land
     * battle are gone from the troop share the city uses. A city layer handed the player's untouched
     * setup gets every one of those wrong.
     *
     * @param copies the working set, already anchored. NOT the player's armies.
     * @param ret    accumulated across layers, so one result describes the whole battle
     */
    CombatResult resolve(CombatScenario scenario, Cenario cenario, CombatCopies copies,
            CombatResult ret) {
        if (scenario == null || copies == null) {
            return ret;
        }
        final List<ArmySim> fighters = copies.inLayer(scenario, CombatLayer.ARMY);
        final Map<ArmySim, ArmySim> toOriginal = copies.toOriginal();
        if (fighters.size() < 2) {
            return ret;
        }
        noteWhatWasSkipped(fighters, ret);
        noteUnknownMorale(scenario, fighters, toOriginal, ret);

        final RelationshipMatrix relations = scenario.getRelationships();
        final HostilityMatrix matrix = scenario.getMatrix();
        // What an army has taken this round but not yet suffered. The Judge keeps this on the army
        // as combatDano; here it is a side map, so ArmySim stays a plain model object.
        final Map<ArmySim, Long> pending = new IdentityHashMap<>();
        // Who was ever in the fight. An army with no enemy on this layer is an observer, not a
        // winner, and only this loop can tell them apart - both end at full strength.
        final Map<ArmySim, Boolean> engaged = new IdentityHashMap<>();

        int round = 0;
        while (round < MAX_ROUNDS && hasLiveFight(scenario, fighters, matrix, toOriginal)) {
            doDistributeDamage(scenario, fighters, matrix, toOriginal, relations, cenario, pending,
                    engaged, round, ret);
            doApplyCasualties(fighters, toOriginal, cenario, pending, round, ret);
            round++;
        }
        ret.setRounds(CombatLayer.ARMY, round);
        if (round >= MAX_ROUNDS) {
            ret.addNote("BATTLESIM.RESULT.CAPPED");
        }
        report(scenario, fighters, toOriginal, engaged, round >= MAX_ROUNDS, ret);
        doSnapshotSurvivors(fighters, toOriginal, ret);
        return ret;
    }

    /**
     * One round of attacks. Nothing dies here: damage is banked and suffered at the end of it.
     *
     * The Judge's own comment for this is "efetua dano em todos os exercitos, fingindo ser
     * simultaneo" - pretend it is simultaneous. That is why the order of armies within a round
     * cannot change the answer, and why every strength below is read before anything falls.
     */
    private void doDistributeDamage(CombatScenario scenario, List<ArmySim> fighters,
            HostilityMatrix matrix, Map<ArmySim, ArmySim> toOriginal, RelationshipMatrix relations,
            Cenario cenario, Map<ArmySim, Long> pending, Map<ArmySim, Boolean> engaged, int round,
            CombatResult ret) {
        for (ArmySim army : fighters) {
            final List<ArmySim> enemies = enemiesOf(army, fighters, matrix, toOriginal, scenario);
            if (enemies.isEmpty()) {
                continue;
            }
            // ONCE per army per round, before the enemy loop. This is the denominator that splits
            // the attack between enemies; recomputing it inside would change every number.
            long qtTropsInimigo = 0;
            for (ArmySim enemy : enemies) {
                qtTropsInimigo += exercitoFacade.getQtTropasTotal(enemy);
            }
            if (qtTropsInimigo <= 0) {
                continue;
            }
            final long forcaBasica = getForcaBasica(army, round);
            final long forcaPlus = getForcaPlus(army, round);
            for (ArmySim enemy : enemies) {
                engaged.put(army, Boolean.TRUE);
                engaged.put(enemy, Boolean.TRUE);
                final long modRelacionamento = 100 - getBonusRelacionamento(relations,
                        toOriginal.get(army), toOriginal.get(enemy));
                final long modTatica = cenario == null ? 100
                        : cenarioFacade.getTaticaBonus(cenario, army.getTatica(), enemy.getTatica());
                // The Judge's exact expression, including where it truncates: TWO successive
                // integer divisions, not one combined multiply. Collapsing them moves the answer.
                final long ataqueFinal =
                        forcaPlus + (forcaBasica * modRelacionamento / 100 * modTatica / 100);
                final long dano =
                        exercitoFacade.getQtTropasTotal(enemy) * ataqueFinal / qtTropsInimigo;
                pending.put(enemy, banked(pending, enemy) + dano);
                ret.addRoundDamage(round, toOriginal.get(army), toOriginal.get(enemy),
                        ataqueFinal, dano, CombatLayer.ARMY);
            }
        }
    }

    /**
     * THE FLEET IS LEFT BEHIND. An army that fights ashore anchors its ships first.
     *
     * {@code CombateTmpbm.executaMsgBasicaCombateLand} calls {@code doAncoraBarcoAll()} on every
     * attacking army when the hex is anchorable, and {@code doAncoraEsquadras()} does the same for
     * the rest; the ships are moved into the hex's garrison and are simply not in the army when the
     * land battle runs. The Judge's own deploy listing for a land combat prints every platoon an
     * army still has, and for a fleet-borne assault it prints only the troops - which is how this
     * was noticed.
     *
     * It is not cosmetic. The attack is divided between enemies by each enemy's share of the TOTAL
     * troop count, ships included, so a fleet still attached inflates its owner's share of the
     * incoming damage. At Seagard that alone moved two nearly identical armies from losing 250 and
     * 253 to losing 264 and 240 - an asymmetry with no cause in the battle at all.
     *
     * A garrison keeps its ships: {@code doAncoraBarcoAll} returns early for one, since it is
     * already the thing the ships would be anchored into.
     */
    static SortedMap<String, Pelotao> doAncoraBarcos(ArmySim army, CombatScenario scenario) {
        final SortedMap<String, Pelotao> ret = new TreeMap<>();
        // TERRAIN OR DOCKS, the same Hexagono.isAncoravel() the Judge anchors on in both
        // executaMsgBasicaCombateLand and doAncoraEsquadras. The docks half was added to the two
        // GATES and not here, to the one place that actually removes the ships - so at a port city
        // on non-anchorable ground a fleet joined the assault with its boats still aboard, took an
        // inflated share of the city's returned damage and left its allies short. That is the
        // Seagard defect one layer over.
        //
        // The other three conditions are doAncoraBarcoAll's own first line, and they were missing:
        // a WATER hex has nothing to anchor to, a fleet carrying no land troops has no reason to
        // put anything ashore, and a garrison IS the thing ships are anchored into.
        if (!isAncoravel(scenario) || army.isGarrison()
                || isAgua(scenario) || exercitoFacade.isBarcoOnly(army)) {
            return ret;
        }
        for (Pelotao pelotao : new ArrayList<>(army.getPelotoes().values())) {
            // getQtd() <= 0 is skipped by the Judge, and KI-033 is why: anchoring an empty platoon
            // creates a garrison entry for a boat that is not there.
            if (pelotao.getQtd() <= 0 || pelotao.getTipoTropa() == null
                    || !pelotao.getTipoTropa().isBarcos()) {
                continue;
            }
            army.getPelotoes().remove(pelotao.getCodigo());
            ret.put(pelotao.getCodigo(), pelotao);
        }
        return ret;
    }

    /** {@code Hexagono.isAgua()}, through the player's Terrain override. */
    private static boolean isAgua(CombatScenario scenario) {
        return scenario.getTerreno() != null && scenario.getTerreno().isAgua();
    }

    /** {@code Hexagono.isAncoravel()}: anchorable terrain, or a city with docks. */
    private static boolean isAncoravel(CombatScenario scenario) {
        if (scenario.getTerreno() != null && scenario.getTerreno().isAncoravel()) {
            return true;
        }
        final model.Cidade city = scenario.getCidade();
        return city != null && new business.facade.CidadeFacade().isDocasPorto(city);
    }

    /**
     * The attack an army brings to the round.
     *
     * Round 0 is FIRST STRIKE: only troops carrying {@code ;TT1;} swing, which is why a battle
     * between two ordinary armies opens with a round in which nothing happens. Afterwards it is
     * every non-naval troop.
     */
    private long getForcaBasica(ArmySim army, int round) {
        return round == 0
                ? battleSimFacade.getArmyAttackBase(army, ";TT1;", army.getLocal())
                : battleSimFacade.getArmyAttackBaseNot(army, ";TTN;", army.getLocal());
    }

    /**
     * The flat additions, which are NOT scaled by the relationship or tactic modifiers.
     *
     * Partly out of reach, deliberately. The Judge also adds the commander's combat artifact and the
     * bonuses of characters travelling with the army; both are the secrecy seam (T-808), and neither
     * is in the EGF for an army the player can only see from outside. What IS here is what he can
     * type: the attack bonus and the one-time attack magic.
     *
     * The one-time magic is SPENT here, exactly as {@code getCombateAtaqueOnetimeReset()} spends it:
     * it lands in the first round that actually swings and never again. Left unreset it would be a
     * permanent buff, and the longer the battle the further the forecast would drift from the turn
     * it is meant to predict. Safe to zero because this is a copy - the player's own army keeps the
     * value he typed, so pressing Run twice still gives the same answer.
     */
    long getForcaPlus(ArmySim army, int round) {
        if (round == 0) {
            // Round 0 counts ONLY a first-strike artifact, and `Artefato` carries no isFirstStrike
            // on this side of the wire, so this returns zero. Verified against a real turn rather
            // than assumed: at 811 t58 hex 1630 every round-0 number matches exactly with zero here.
            return 0;
        }
        return getForcaPlusFlat(army);
    }

    /**
     * The same additions, with no round-0 rule in front of them.
     *
     * The sea layer needs this: its counter opens at 1 and there is no first-strike round at sea,
     * so {@code getForcaPlusNaval} lands in the FIRST round rather than the second. Sharing the
     * body is what keeps the one-time magic spent once across both - it is zeroed on the copy here,
     * so whichever layer swings first is the one that gets it.
     */
    static long getForcaPlusFlat(ArmySim army) {
        final long onetime = army.getCombateAtaqueOnetime();
        army.setCombateAtaqueOnetime(0);
        long ret = army.getAttackBonus() + onetime;
        final Personagem comandante = army.getComandanteModel();
        if (comandante == null) {
            return ret;
        }
        ret += combatArtifact(comandante);
        // Characters travelling WITH the commander, which is what getLiderados() holds - the same
        // set the Judge walks as getPersonagemViajandoIterator(). A PLAYER character contributes
        // its combat artifact; an NPC contributes nothing here, and the result says so - see
        // NPC_COMBAT_CONTRIBUTION.
        for (Personagem traveller : comandante.getLiderados().values()) {
            if (traveller == null || traveller.isRefem()) {
                continue;
            }
            ret += traveller.isNpc() ? NPC_COMBAT_CONTRIBUTION : combatArtifact(traveller);
        }
        return ret;
    }

    /** A combat artifact is worth its value, and anything else is worth nothing. */
    static long combatArtifact(Personagem personagem) {
        final Artefato artefato = personagem.getArtefatoCombateAtivo();
        return artefato != null && artefato.isCombate() ? artefato.getValor() : 0;
    }

    /**
     * The diplomacy matrix, turned into a damage modifier.
     *
     * This is the first place an edited relationship cell changes a NUMBER rather than merely who
     * fights whom, and the effect is large: {@link BaseMsgs#dificuldadeBonus} runs from -35 to +25,
     * so the same two armies can hit 35% harder or 25% softer on the strength of one cell. Worth
     * knowing when reading a result whose relationships were assumed rather than read.
     */
    static int getBonusRelacionamento(RelationshipMatrix relations, ArmySim army, ArmySim enemy) {
        if (relations == null || army == null || enemy == null
                || army.getNacao() == null || enemy.getNacao() == null) {
            return 0;
        }
        return BaseMsgs.dificuldadeBonus[relations.getValor(army.getNacao(), enemy.getNacao()) + 3];
    }

    /**
     * End of round: everything banked is suffered at once, by whichever casualty rule applies.
     *
     * The two rules are {@code ExercitoControl.doCombateDano}'s, and they are the ones
     * {@link CasualtyMode} already names above the platoon table - so what the player was told about
     * the order is what actually happens to him here.
     */
    void doApplyCasualties(List<ArmySim> fighters, Map<ArmySim, ArmySim> toOriginal,
            Cenario cenario, Map<ArmySim, Long> pending, int round, CombatResult ret) {
        doApplyCasualties(fighters, toOriginal, cenario, pending, round, ret, CombatLayer.ARMY);
    }

    /**
     * The same machinery for any layer, because in the Judge it IS the same machinery.
     *
     * {@code exercito.sumCombateDano(dano)} then {@code doCombateDano()} is what turns damage into
     * dead platoons, and {@code executaCombateCidade} calls exactly that pair - so a city assault
     * kills by the same BY_RANK or PROPORTIONAL rule, chosen by the same tactic test. The layer is
     * a parameter only because {@link CasualtyMode} short-circuits the sea layer to BY_RANK.
     */
    void doApplyCasualties(List<ArmySim> fighters, Map<ArmySim, ArmySim> toOriginal,
            Cenario cenario, Map<ArmySim, Long> pending, int round, CombatResult ret,
            CombatLayer layer) {
        for (ArmySim army : fighters) {
            long dano = banked(pending, army);
            pending.put(army, 0L);
            if (dano <= 0) {
                continue;
            }
            // Defensive magic absorbs first, and what it absorbs it spends.
            final int bonusDefesa = army.getArmyDefenseBonus();
            if (bonusDefesa > 0) {
                army.setArmyDefenseBonus((int) Math.max(bonusDefesa - dano, 0));
                dano = Math.max(dano - bonusDefesa, 0);
            }
            if (dano <= 0) {
                continue;
            }
            final ArmySim original = toOriginal.get(army);
            if (CasualtyMode.of(army, cenario, layer) == CasualtyMode.BY_RANK) {
                doCasualtiesByRank(army, original, dano, round, ret, layer);
            } else {
                doCasualtiesProportional(army, original, dano, round, ret, layer);
            }
            if (exercitoFacade.getQtTropasTotal(army) <= 0) {
                army.setDisband(true);
            }
        }
    }

    /**
     * Standard: the same PERCENTAGE off every platoon, so nobody is spared and nobody is singled
     * out.
     *
     * Note the {@code Math.ceil} per platoon. It is the Judge's, and it means total losses can
     * slightly exceed the damage dealt. Reproduced rather than corrected: the simulator's job is to
     * predict the turn that will actually run.
     */
    private void doCasualtiesProportional(ArmySim army, ArmySim original, long dano, int round,
            CombatResult ret, CombatLayer layer) {
        final float constituicao = battleSimFacade.getArmyDefenseTotalLand(army);
        if (constituicao <= 0) {
            return;
        }
        final float percent = 100F * dano / constituicao;
        for (Pelotao pelotao : new ArrayList<>(army.getPelotoes().values())) {
            final TipoTropa tipo = pelotao.getTipoTropa();
            if (tipo == null || tipo.isBarcos()) {
                continue;
            }
            final int qtd = percent >= 100F ? pelotao.getQtd()
                    : (int) Math.min(pelotao.getQtd(), Math.ceil(pelotao.getQtd() * percent / 100F));
            final int before = pelotao.getQtd();
            exercitoFacade.subTropaQt(army, tipo, qtd);
            ret.addRoundLoss(round, original, originalOf(original, pelotao), qtd, before - qtd, layer);
        }
    }

    /**
     * Every other tactic: platoons are consumed in turn, down the casualty order.
     *
     * The order IS the answer here, which is why the platoon table is sorted by it. A platoon that
     * dies outright costs its whole defence and the rest of the damage carries on to the next one
     * down; the first platoon that survives absorbs what is left and the damage stops there.
     */
    private void doCasualtiesByRank(ArmySim army, ArmySim original, long dano, int round,
            CombatResult ret, CombatLayer layer) {
        doCasualtiesByRank(army, original, exercitoFacade.listaTropasTerra(army), dano, round, ret,
                layer);
    }

    /**
     * The same rule, over whichever list the layer consumes.
     *
     * The sea layer hands it SHIPS - {@code listaTropasAgua()} - and takes the identical path,
     * because {@code doCombateDano} routes every naval army through {@code doCombateDanoTatica}
     * whatever the scenario's casualty rule says. One implementation of the Judge's rank rule, so
     * the two layers cannot drift.
     */
    void doCasualtiesByRank(ArmySim army, ArmySim original, List<Pelotao> platoons, long dano,
            int round, CombatResult ret, CombatLayer layer) {
        for (Pelotao pelotao : platoons) {
            if (dano <= 0) {
                return;
            }
            final float constituicao = battleSimFacade.getPlatoonDefense(army, pelotao);
            if (constituicao <= 0) {
                continue;       // nothing to absorb the blow, and no divide by zero
            }
            final int before = pelotao.getQtd();
            if (dano >= constituicao) {
                exercitoFacade.subTropaQt(army, pelotao.getTipoTropa(), before);
                ret.addRoundLoss(round, original, originalOf(original, pelotao), before, 0, layer);
                dano -= (long) constituicao;
            } else {
                final int qtd = (int) Math.min(before, Math.ceil(before * dano / constituicao));
                exercitoFacade.subTropaQt(army, pelotao.getTipoTropa(), qtd);
                ret.addRoundLoss(round, original, originalOf(original, pelotao), qtd,
                        before - qtd, layer);
                return;
            }
        }
    }

    /**
     * Is there still a fight? The Judge's {@code fim} test, in its own terms.
     *
     * "Some army still has an enemy and still has something to fight with." Rebuilt every round
     * rather than cached, exactly as the Judge drops a wiped-out army out of everyone's enemy list.
     */
    private boolean hasLiveFight(CombatScenario scenario, List<ArmySim> fighters,
            HostilityMatrix matrix, Map<ArmySim, ArmySim> toOriginal) {
        for (ArmySim one : fighters) {
            if (isStillInTheLandBattle(one)
                    && !enemiesOf(one, fighters, matrix, toOriginal, scenario).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The matrix decides, and it is keyed on the ORIGINALS - the copies are not in it.
     *
     * Hence {@code toOriginal}: every hostility question has to be translated back to the army the
     * scenario knows about. Getting this backwards silently answers "nobody is hostile" and the
     * battle resolves in zero rounds.
     */
    private List<ArmySim> enemiesOf(ArmySim army, List<ArmySim> fighters, HostilityMatrix matrix,
            Map<ArmySim, ArmySim> toOriginal, CombatScenario scenario) {
        final List<ArmySim> ret = new ArrayList<>();
        for (ArmySim other : fighters) {
            if (other != army && isStillInTheLandBattle(other)
                    && canReachAshore(army, other, scenario)
                    && matrix.isInimigo(toOriginal.get(army), toOriginal.get(other))) {
                ret.add(other);
            }
        }
        return ret;
    }

    /**
     * Can these two actually come to blows on land, or is one of them still afloat?
     *
     * {@code executaMsgBasicaCombateLand} 1014-1022, which is four cases and only two of them
     * fight. {@code isEsquadraEmbarcada()} means the army fits entirely in its own transports - it
     * is at sea, not ashore:
     *
     * <ul>
     *   <li><b>Both afloat:</b> the Judge THROWS here ("combate naval deveria ter resolvido isto
     *       antes"). Nothing to do on land.</li>
     *   <li><b>Only the other one afloat:</b> ignored, and its own comment says why - a land army
     *       cannot reach a fleet that has not landed.</li>
     *   <li><b>This one afloat, on anchorable ground, the other ashore:</b> the amphibious assault.
     *       It fights.</li>
     *   <li><b>Neither afloat:</b> an ordinary land battle.</li>
     * </ul>
     *
     * Asked of the COPIES and after the chain has anchored, because that is when the Judge asks it:
     * an army that put its boats on the beach is no longer aboard them. Before the sea layer
     * existed this could not change during a run and the rule went unmodelled; a naval battle that
     * sinks one side's transports changes it in the middle of the very hex it decides.
     */
    private boolean canReachAshore(ArmySim army, ArmySim other, CombatScenario scenario) {
        return startsIt(army, other, scenario) || startsIt(other, army, scenario);
    }

    /**
     * {@code temCombateTerra} 1144-1173, asked of ONE attacker about ONE defender.
     *
     * The direction is the whole rule. The Judge's outer loop is
     * {@code getExercitosAtacandoIterator()}, so the four embarkation cases are only ever evaluated
     * from the side that gave the order - and two of the four say no:
     *
     * <ul>
     *   <li><b>Attacker ashore, defender afloat:</b> ignored, and the Judge sends the player a
     *       message saying so. A land army cannot reach a fleet that has not landed.</li>
     *   <li><b>Both afloat:</b> the Judge THROWS ("combate naval deveria ter resolvido isto
     *       antes"). Nothing to do on land.</li>
     *   <li><b>Attacker afloat, defender ashore, anchorable ground:</b> the amphibious assault.
     *       It fights.</li>
     *   <li><b>Neither afloat:</b> an ordinary land battle.</li>
     * </ul>
     *
     * Asking it symmetrically was wrong in a way that only showed once the sea layer ran: at 866
     * t3 hex 2442 the naval battle sank one side's transports, leaving one army ashore and one
     * still aboard. The Judge fought no land battle. Asked from both sides, the afloat one "could
     * have landed" and the sim fought one, for 2,000 casualties that never happened.
     */
    private boolean startsIt(ArmySim attacker, ArmySim defender, CombatScenario scenario) {
        if (attacker.getCombatLevel() == CombatLevel.DEFEND_ONLY) {
            return false;
        }
        final boolean afloat = exercitoFacade.isEsquadraEmbarcada(attacker);
        if (exercitoFacade.isEsquadraEmbarcada(defender)) {
            return false;       // both afloat, or a land army reaching for a fleet: neither fights
        }
        return afloat ? isAncoravel(scenario) : true;
    }

    /**
     * Has this army anything left to fight a LAND battle with?
     *
     * Land defence, not troop count, and the difference is not academic - it is the shape of half
     * the armies in a coastal game. An army of Reavers and Krakens that loses its Reavers still has
     * a positive troop count, because the ships are counted, so a count-based test kept it on the
     * winner's target list forever: it absorbed a full attack every round, lost nobody (the
     * proportional rule divides by a land defence of zero), and never disbanded. The battle then ran
     * to the round cap and reported a STALEMATE where the Judge reports a victory.
     *
     * The Judge reaches the same place from the other side: it drops an army out of every enemy list
     * the moment a round's damage meets its land defence.
     */
    private boolean isStillInTheLandBattle(ArmySim army) {
        return battleSimFacade.getArmyDefenseTotalLand(army) > 0;
    }

    /**
     * Survivors, losses, and how each army ended up, all keyed back to the player's own objects.
     *
     * EVERY army in the scenario gets an outcome, not only the ones that fought - a fleet with no
     * land troops, an army whose only neighbours are friendly, an army the layer rules kept out. The
     * roster shows a mark against each of them, so "no mark" has to mean "no run yet" and never "the
     * resolver forgot about this one". Only the fighters get platoon numbers, though: an army that
     * was not in the battle shows "--", which is a different statement from surviving it intact.
     */
    private void report(CombatScenario scenario, List<ArmySim> fighters,
            Map<ArmySim, ArmySim> toOriginal, Map<ArmySim, Boolean> engaged, boolean capped,
            CombatResult ret) {
        for (ArmySim army : scenario.getArmies()) {
            ret.setOutcome(army, CombatLayer.ARMY, CombatResult.Outcome.DID_NOT_FIGHT);
        }
        for (ArmySim copy : fighters) {
            ret.setOutcome(toOriginal.get(copy), CombatLayer.ARMY,
                    outcomeOf(copy, engaged, capped));
        }
    }

    /**
     * What every platoon has LEFT at the end of the whole battle - the After and Lost columns.
     *
     * <b>Taken once, by the chain, after the last layer.</b> It used to be taken here, at the end of
     * the land layer, over the land FIGHTERS only. Two things were wrong with that: an army mauled
     * at the walls showed its pre-assault strength, so the platoon table contradicted the round
     * table on the same screen; and an army that ONLY assaulted the city was never in {@code
     * fighters} at all, so its platoon table stayed empty after a battle it had just fought.
     *
     * Ships are still skipped. They take no part ashore, and "--" is a different statement from
     * surviving intact - the day the sea layer lands it is the sea layer's business to fill them in.
     */
    static void doSnapshotSurvivors(List<ArmySim> copies, Map<ArmySim, ArmySim> toOriginal,
            CombatResult ret) {
        doSnapshotSurvivors(copies, toOriginal, null, null, ret);
    }

    /**
     * The same snapshot, with the ships the run set aside and the ones that actually fought.
     *
     * <h3>Why an anchored ship is not a sunk one</h3>
     *
     * Anchoring REMOVES the ship platoons from the army, so a snapshot that only reads the copy
     * finds nothing where the boats were and reports the whole fleet destroyed. The anchored set is
     * where they went, and reading it back is what makes "left them on the beach" different from
     * "lost them".
     *
     * <h3>Why ships are in the table only when they fought</h3>
     *
     * The summary counts what the battle put at risk. A fleet that anchored and marched inland
     * risked its troops and not its boats, and adding 40 anchored ships to its Before and After
     * would move the percentage in the cost line without a single ship having been in danger. So a
     * ship platoon enters the table only for an army that fought at SEA - where ships are exactly
     * what was at risk, and where leaving them out would give a fleet that lost half its hulls a
     * casualty table reading nothing lost.
     *
     * @param anchored     per copy, the ship platoons the run put ashore; null when none were
     * @param foughtAtSea  the copies the sea layer resolved for; null before that layer exists
     */
    static void doSnapshotSurvivors(List<ArmySim> copies, Map<ArmySim, ArmySim> toOriginal,
            Map<ArmySim, SortedMap<String, Pelotao>> anchored, Set<ArmySim> foughtAtSea,
            CombatResult ret) {
        for (ArmySim copy : copies) {
            final ArmySim original = toOriginal.get(copy);
            if (original == null) {
                continue;
            }
            final SortedMap<String, Pelotao> aside =
                    anchored == null ? null : anchored.get(copy);
            final boolean atSea = foughtAtSea != null && foughtAtSea.contains(copy);
            for (Pelotao was : original.getPelotoes().values()) {
                final boolean ships = was.getTipoTropa() != null && was.getTipoTropa().isBarcos();
                if (ships && !atSea) {
                    continue;
                }
                Pelotao now = copy.getPelotoes().get(was.getCodigo());
                if (now == null && aside != null) {
                    now = aside.get(was.getCodigo());
                }
                ret.put(was, was.getQtd(), now == null ? 0 : now.getQtd());
            }
        }
    }

    /**
     * The Judge's own three-way split, from the report it writes at the end of a battle: an army
     * that never had an enemy did not take part, one left standing won, one wiped out lost.
     *
     * The distinction that needs the battle to have been fought is the first. An army that fought
     * and lost nobody and an army that stood by and watched both end at full strength, and calling
     * the first an observer would hide the fact that it was in a battle at all.
     */
    private CombatResult.Outcome outcomeOf(ArmySim copy, Map<ArmySim, Boolean> engaged,
            boolean capped) {
        if (!Boolean.TRUE.equals(engaged.get(copy))) {
            return CombatResult.Outcome.DID_NOT_FIGHT;
        }
        // LAND defence, not troop count: an army whose land force was destroyed lost the battle
        // whatever is still floating offshore, and counting its ships would report a defeat as a
        // victory. This holds even in a capped battle - being wiped out is not undecided.
        if (copy.isDisband() || !isStillInTheLandBattle(copy)) {
            return CombatResult.Outcome.LOST;
        }
        // A capped run means NEITHER side could finish the other, so nobody left alive won it.
        // Marking them all victorious would contradict the sentence printed beside the marks.
        return capped ? CombatResult.Outcome.UNDECIDED : CombatResult.Outcome.WON;
    }

    /**
     * MORALE ZERO IS NOT NEUTRAL, it is the floor, and for an enemy it usually means "not exported".
     *
     * An army seen from outside arrives with no morale at all, and zero is indistinguishable from an
     * army that is genuinely broken. The difference is worth up to a quarter of its attack, because
     * the army bonus is {@code (commander + morale + 200) / 4} - so every unscouted enemy fights
     * systematically weaker than it will, and a battle decided by several of them can come out the
     * wrong way round while looking perfectly confident.
     *
     * Seen live: game 901 turn 8 at Lannisport, four Lannister and Tully armies all at zero against
     * Tyrells carrying their real morale. The simulator had the Tyrells holding the field; the turn
     * had them destroyed. Nothing on screen suggested the answer was built on a guess.
     *
     * So it is counted and stated. Guessing a better number would be worse - it would be just as
     * wrong and no longer visible.
     */
    private void noteUnknownMorale(CombatScenario scenario, List<ArmySim> fighters,
            Map<ArmySim, ArmySim> toOriginal, CombatResult ret) {
        int unknown = 0;
        // FIGHTERS, not every copy. toOriginal used to hold only this layer's participants; since
        // CombatCopies took over the copying it holds every army on the hex, so counting its keys
        // reported unscouted fleets offshore as unknown-morale armies in a LAND battle none of them
        // were in. The note is the fidelity disclosure - inflating it fails R-15 from the other side.
        for (ArmySim copy : fighters) {
            if (scenario.isMoraleUnknown(toOriginal.get(copy))) {
                unknown++;
            }
        }
        if (unknown > 0) {
            ret.addNote("BATTLESIM.RESULT.UNKNOWNMORALE", unknown);
        }
    }

    /** Anything the run cannot do faithfully has to be said, not silently dropped. */
    private void noteWhatWasSkipped(List<ArmySim> fighters, CombatResult ret) {
        for (ArmySim army : fighters) {
            for (Pelotao pelotao : army.getPelotoes().values()) {
                final TipoTropa tipo = pelotao.getTipoTropa();
                if (tipo != null && (tipo.hasHabilidade(";TCT;") || tipo.hasHabilidade(";TCY;")
                        || tipo.hasHabilidade(";TCF;"))) {
                    ret.addNote("BATTLESIM.RESULT.NOTROOPPOWERS");
                    return;
                }
            }
        }
    }

    /**
     * The scenario's own platoon for one the battle was fought with.
     *
     * The round log exists to be READ - by the results pane and by a fidelity diff - and both speak
     * in terms of the armies the player is looking at, not the clones. Matching on the troop code is
     * the same join {@code report} uses to hand back survivors.
     */
    static Pelotao originalOf(ArmySim original, Pelotao copy) {
        if (original == null || copy == null) {
            return copy;
        }
        final Pelotao ret = original.getPelotoes().get(copy.getCodigo());
        return ret == null ? copy : ret;
    }

    private long banked(Map<ArmySim, Long> pending, ArmySim army) {
        final Long ret = pending.get(army);
        return ret == null ? 0L : ret;
    }
}
