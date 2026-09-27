package business.combat;

import business.facade.BattleSimFacade;
import business.facade.CenarioFacade;
import business.facade.DrowningRule;
import business.facade.ExercitoFacade;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import business.services.ComparatorFactory;
import model.Cenario;
import model.Pelotao;

/**
 * The sea layer: fleet against fleet, transcribed from {@code CombateTmpbm.executaCombateNaval}.
 *
 * <h3>It is not the land battle with boats</h3>
 *
 * The two loops look alike and differ in every number that matters:
 *
 * <ul>
 *   <li><b>Only fleets fight.</b> Both sides must be an {@code isEsquadra()} - an army with at
 *       least one {@code ;TTN;} platoon. A land army standing on the same hex is not in this
 *       battle, and neither is a fleet's cargo.</li>
 *   <li><b>The attack counts SHIPS.</b> {@code getForcaBasicaNaval} is
 *       {@code getArmyAttackBase(army, ";TTN;", local)} - the ships only - where the land battle
 *       counts everything that is not a ship.</li>
 *   <li><b>The share is by HULLS, not bodies.</b> {@code dano = enemy.getTropaQtBarco() *
 *       ataqueFinal / qtTropsInimigo}, and both terms are ship counts. A transport full of
 *       infantry draws fire for the transport, not for the infantry.</li>
 *   <li><b>Rounds start at 1.</b> The Judge's counter opens at 1 and there is no first-strike
 *       round at sea, so the {@code ;TT1;} round the land battle opens with does not exist here.
 *       {@code getForcaPlusNaval} therefore lands in the FIRST round, not the second.</li>
 *   <li><b>Casualties are spent on ships.</b> {@code listaTropasAgua()}, so the cargo is untouched
 *       by the fighting - and then drowns or does not, which is a separate question below.</li>
 *   <li><b>No tactic exemption.</b> {@code doCombateDano} routes every naval army through
 *       {@code doCombateDanoTatica} regardless of the scenario's casualty rule, because
 *       {@code isCombatTypeNaval()} short-circuits the test.</li>
 * </ul>
 *
 * <h3>What the walls are to the city layer, drowning is to this one</h3>
 *
 * When a fleet loses every hull the troops it was carrying are in the water. All of the Judge's
 * branches are arithmetic now, so all of them are resolved here:
 *
 * <ul>
 *   <li><b>On water, all hulls lost:</b> everyone drowns and the army disbands.</li>
 *   <li><b>Ashore, no cargo capacity at all</b> (an escort that was never carrying anyone): no
 *       casualties.</li>
 *   <li><b>Ashore, carrying troops:</b> {@link business.facade.DrowningRule}, a function of the
 *       fleet commander's skill, bounded by what was embarked. This used to be
 *       {@code SysApoio.rand(15) + 10} and could only be estimated at its midpoint; T-817 removed
 *       the die from the Judge, so this layer now answers it exactly rather than disclosing it.</li>
 * </ul>
 *
 * <h3>Out of reach, and said so</h3>
 *
 * The scorpion anti-dragon strike ({@code ecf.doScorpionStrike}) fires before round 1 and is fully
 * deterministic in the Judge, but it needs each dragon's vitality and its death-power habilidades,
 * and neither crosses the wire - {@code Personagem} has no vitality and {@code Artefato} carries no
 * habilidades on this side. The same gap costs the naval artifact multiplier {@code ;INC5;}, which
 * {@code getValorCombateArtefatoNaval} applies and this cannot. Both are disclosed as notes when
 * the pieces that would trigger them are on the hex.
 */
public class NavyCombatResolver {

    /** The Judge's naval counter opens at 1. Kept, so a round number here IS the Judge's. */
    public static final int FIRST_ROUND = 1;
    /** Same guard as the land layer: a battle that cannot end is a bug, not a long battle. */
    private static final int MAX_ROUNDS = 100;
    private final BattleSimFacade battleSimFacade = new BattleSimFacade();
    private final CenarioFacade cenarioFacade = new CenarioFacade();
    private final ExercitoFacade exercitoFacade = new ExercitoFacade();

    /**
     * Fights the sea battle on the chain's copies, before anything is put ashore.
     *
     * @param scenario the player's setup, LEFT UNTOUCHED
     * @param cenario  for the tactic-versus-tactic bonus table
     * @param copies   the working set, ships still aboard
     * @param ret      accumulated across layers, so one result describes the whole battle
     * @return the copies that actually fought, for the survivor snapshot
     */
    public List<ArmySim> resolve(CombatScenario scenario, Cenario cenario, CombatCopies copies,
            CombatResult ret) {
        final List<ArmySim> fought = new ArrayList<>();
        if (scenario == null || copies == null) {
            return fought;
        }
        final List<ArmySim> fleets = copies.inLayer(scenario, CombatLayer.NAVY);
        final Map<ArmySim, ArmySim> toOriginal = copies.toOriginal();
        if (fleets.size() < 2) {
            return fought;
        }
        noteWhatIsOutOfReach(fleets, ret);

        final RelationshipMatrix relations = scenario.getRelationships();
        final HostilityMatrix matrix = scenario.getMatrix();
        final Map<ArmySim, Long> pending = new IdentityHashMap<>();
        final Map<ArmySim, Boolean> engaged = new IdentityHashMap<>();

        int round = FIRST_ROUND;
        while (round < FIRST_ROUND + MAX_ROUNDS && hasLiveFight(fleets, matrix, toOriginal)) {
            doDistributeDamage(fleets, matrix, toOriginal, relations, cenario, pending, engaged,
                    round, ret);
            doApplyCasualties(fleets, toOriginal, scenario, pending, round, ret);
            round++;
        }
        final int rounds = round - FIRST_ROUND;
        ret.setRounds(CombatLayer.NAVY, rounds);
        if (rounds >= MAX_ROUNDS) {
            ret.addNote("BATTLESIM.RESULT.CAPPED");
        }
        for (ArmySim fleet : fleets) {
            if (Boolean.TRUE.equals(engaged.get(fleet))) {
                fought.add(fleet);
            }
        }
        report(fleets, toOriginal, engaged, ret);
        return fought;
    }

    /**
     * One round of attacks. Nothing sinks here: damage is banked and suffered at the end of it.
     *
     * The Judge's own word for it in the land loop is "fingindo ser simultaneo" - pretend it is
     * simultaneous - and the naval loop is built the same way, so the order of fleets within a
     * round cannot change the answer.
     */
    private void doDistributeDamage(List<ArmySim> fleets, HostilityMatrix matrix,
            Map<ArmySim, ArmySim> toOriginal, RelationshipMatrix relations, Cenario cenario,
            Map<ArmySim, Long> pending, Map<ArmySim, Boolean> engaged, int round,
            CombatResult ret) {
        for (ArmySim fleet : fleets) {
            final List<ArmySim> enemies = enemiesOf(fleet, fleets, matrix, toOriginal);
            if (enemies.isEmpty()) {
                continue;
            }
            // ONCE per fleet per round, before the enemy loop: this is the denominator that splits
            // the attack, and it is a count of HULLS - getInimigoTotalNaval sums getTropaQtBarco.
            long qtTropsInimigo = 0;
            for (ArmySim enemy : enemies) {
                qtTropsInimigo += ships(enemy);
            }
            if (qtTropsInimigo <= 0) {
                // The Judge divides by this and its own catch block divides again, so it throws.
                // Here it simply means nobody afloat is left to hit.
                continue;
            }
            final long forcaBasica = getForcaBasicaNaval(fleet);
            final long forcaPlus = getForcaPlusNaval(fleet);
            for (ArmySim enemy : enemies) {
                engaged.put(fleet, Boolean.TRUE);
                engaged.put(enemy, Boolean.TRUE);
                final long modRelacionamento = 100 - LandCombatResolver.getBonusRelacionamento(
                        relations, toOriginal.get(fleet), toOriginal.get(enemy));
                final long modTatica = cenario == null ? 100
                        : cenarioFacade.getTaticaBonus(cenario, fleet.getTatica(),
                                enemy.getTatica());
                // The Judge's exact expression, including where it truncates: TWO successive
                // integer divisions. The naval line is the land one term for term, which the city
                // line is NOT - it has no tactic term at all.
                final long ataqueFinal =
                        forcaPlus + (forcaBasica * modRelacionamento / 100 * modTatica / 100);
                final long dano = ships(enemy) * ataqueFinal / qtTropsInimigo;
                pending.put(enemy, banked(pending, enemy) + dano);
                ret.addRoundDamage(round, toOriginal.get(fleet), toOriginal.get(enemy),
                        ataqueFinal, dano, CombatLayer.NAVY);
            }
        }
    }

    /**
     * Sinks ships, then asks who drowned.
     *
     * Ship platoons only, in the scenario's own casualty order, each absorbing damage up to its
     * defence and carrying the remainder to the next - the land layer's rule applied to a different
     * list, which is exactly what {@code doCombateDanoTatica} does when {@code isCombatTypeNaval()}.
     */
    private void doApplyCasualties(List<ArmySim> fleets, Map<ArmySim, ArmySim> toOriginal,
            CombatScenario scenario, Map<ArmySim, Long> pending, int round, CombatResult ret) {
        for (ArmySim fleet : fleets) {
            final long dano = banked(pending, fleet);
            pending.remove(fleet);
            if (dano <= 0) {
                continue;
            }
            // KI-010, and the Judge's own comment says why: capacity and burden are read BEFORE
            // the casualties, because once the hulls are gone getTransportesCapacity answers 0 and
            // 0 cannot tell an escort that was carrying nobody from cargo ships that just sank.
            // Reading it afterwards silently skipped the drowning of every army whose transports
            // went down - which is the only army the drowning rule is about.
            final float capacityBefore =
                    exercitoFacade.getTransportesCapacity(fleet.getPelotoes());
            final float burdenBefore = exercitoFacade.getTransportesBurden(fleet.getPelotoes());
            landResolver.doCasualtiesByRank(fleet, toOriginal.get(fleet), listaTropasAgua(fleet),
                    dano, round, ret, CombatLayer.NAVY);
            // getTropaQtTotal(), not the ship count: the Judge disbands on the WHOLE army being
            // gone, so a fleet that lost every hull but still carries infantry is not disbanded
            // here - it is handed to the drowning rule below, which is what decides.
            if (exercitoFacade.getQtTropasTotal(fleet) <= 0) {
                fleet.setDisband(true);
            }
            doAfogamento(fleet, toOriginal, scenario, capacityBefore, burdenBefore,
                    round, ret);
        }
    }

    /**
     * {@code listaTropasAgua()}: the ship platoons, in the casualty order the turn will use.
     *
     * Built here rather than beside {@code listaTropasTerra} in {@code ExercitoFacade}, which the
     * Judge calls - the guardrail. The comparator, the tactic and the terrain are the same three
     * arguments the Judge passes, and the terrain is the army's, which {@code setTerreno}
     * propagates from the player's Terrain override.
     */
    private List<Pelotao> listaTropasAgua(ArmySim fleet) {
        final List<Pelotao> ret = new ArrayList<>();
        for (Pelotao pelotao : fleet.getPelotoes().values()) {
            if (pelotao.getTipoTropa() != null && pelotao.getTipoTropa().isBarcos()) {
                ret.add(pelotao);
            }
        }
        ComparatorFactory.getComparatorCasualtiesPelotaoSorter(ret, fleet.getTatica(),
                fleet.getTerreno());
        return ret;
    }

    /**
     * What happens to the cargo when the hulls are gone.
     *
     * Every branch of the Judge's {@code doAfogamento} that this layer can reach, resolved. The
     * rate itself comes from {@link business.facade.DrowningRule}, which the Judge calls too.
     */
    private void doAfogamento(ArmySim fleet, Map<ArmySim, ArmySim> toOriginal,
            CombatScenario scenario, float capacityBefore, float burdenBefore, int round,
            CombatResult ret) {
        final boolean agua = scenario.getTerreno() != null && scenario.getTerreno().isAgua();
        if (ships(fleet) > 0) {
            // Hulls left, but maybe not enough of them. On water the Judge throws overboard
            // whatever no longer fits, deterministically, by weight - and it only asks this of an
            // army that is NOT fully embarked.
            if (agua && !exercitoFacade.isEsquadraEmbarcada(fleet)) {
                doJettison(fleet, toOriginal, round, ret);
            }
            return;
        }
        if (exercitoFacade.getQtTropasTotal(fleet) <= 0) {
            return;
        }
        if (agua) {
            // Every hull lost in open water: the Judge disbands the army and removes every platoon.
            for (Pelotao pelotao : new ArrayList<>(fleet.getPelotoes().values())) {
                final int was = pelotao.getQtd();
                if (was <= 0) {
                    continue;
                }
                pelotao.setQtd(0);
                ret.addRoundLoss(round, toOriginal.get(fleet), pelotao, was, 0, CombatLayer.NAVY);
            }
            fleet.setDisband(true);
            ret.addNote("BATTLESIM.RESULT.DROWNED");
            return;
        }
        if (capacityBefore <= 0f) {
            // KI-010 case 1: an escort with no cargo capacity - a lone trireme, say - was never
            // carrying anybody, so they were ashore all along and walk away.
            return;
        }
        if (burdenBefore <= 0f) {
            return;
        }
        // KI-010 cases 2 and 3 differ only in the BOUND. Case 2 is a fleet that was carrying part
        // of its army, so only the embarked part was ever at sea and the drowned weight stops at
        // the capacity; case 3 carried all of it and has no bound to apply.
        // The cap is the RULE, ruled by John 2026-09-26 (D-23), not an artefact to be smoothed
        // away: a proposal to take the rate off the embarked share instead - which would have made
        // these two cases one - was declined. This mirrors the Judge exactly, down to the weight
        // function: its ExercitoControlFacade.getPesoPelotao delegates to the same
        // ExercitoFacade.getTransportesBurden called here, so one function bounds both sides.
        final float bound = capacityBefore < burdenBefore ? capacityBefore : Float.MAX_VALUE;
        // One rate for the fleet, read once, because a fleet has one commander. The Judge used to
        // roll this per platoon; T-817 removed the die from both sides at the same call.
        final int rate = DrowningRule.percent(fleet.getComandantePericia());
        float drowned = 0f;
        boolean any = false;
        for (Pelotao pelotao : new ArrayList<>(fleet.getPelotoes().values())) {
            if (pelotao.getTipoTropa() == null || pelotao.getTipoTropa().isBarcos()
                    || pelotao.getQtd() <= 0 || drowned >= bound) {
                continue;
            }
            final int was = pelotao.getQtd();
            int lost = was * rate / 100;
            final float pesoPorTropa = exercitoFacade.getTransportesBurden(pelotao) / was;
            if (pesoPorTropa > 0f && bound != Float.MAX_VALUE) {
                lost = (int) Math.min(lost, (bound - drowned) / pesoPorTropa);
            }
            if (lost <= 0) {
                continue;
            }
            pelotao.setQtd(was - lost);
            ret.addRoundLoss(round, toOriginal.get(fleet),
                    LandCombatResolver.originalOf(toOriginal.get(fleet), pelotao), lost,
                    was - lost, CombatLayer.NAVY);
            drowned += lost * pesoPorTropa;
            any = true;
        }
        if (any) {
            // Said once, and it names the rate rather than the loss, because after T-817 the rate
            // is a consequence of who the player put in command - which is the point of having
            // removed the die, and is invisible unless the result says it out loud.
            ret.addNote("BATTLESIM.RESULT.DROWNINGSKILL", rate);
        }
    }

    /**
     * Too few hulls left for the load, on water: the excess goes over the side.
     *
     * {@code doAfogamento}'s third top-level branch, and the one piece of the drowning rule that is
     * pure arithmetic - platoons are dropped whole while their weight fits inside the overload, and
     * the one that straddles it loses the fraction that does. No die anywhere.
     */
    private void doJettison(ArmySim fleet, Map<ArmySim, ArmySim> toOriginal, int round,
            CombatResult ret) {
        float over = exercitoFacade.getTransportesBurden(fleet.getPelotoes())
                - exercitoFacade.getTransportesCapacity(fleet.getPelotoes());
        if (over <= 0f) {
            return;
        }
        boolean any = false;
        for (Pelotao pelotao : new ArrayList<>(fleet.getPelotoes().values())) {
            if (over <= 0f || pelotao.getTipoTropa() == null
                    || pelotao.getTipoTropa().isBarcos() || pelotao.getQtd() <= 0) {
                continue;
            }
            final float peso = exercitoFacade.getTransportesBurden(pelotao);
            final int was = pelotao.getQtd();
            final int lost = peso <= over ? was
                    : (int) Math.min(was, Math.ceil(over / peso * was));
            if (lost <= 0) {
                continue;
            }
            pelotao.setQtd(was - lost);
            ret.addRoundLoss(round, toOriginal.get(fleet),
                    LandCombatResolver.originalOf(toOriginal.get(fleet), pelotao), lost,
                    was - lost, CombatLayer.NAVY);
            over -= peso <= over ? peso : over;
            any = true;
        }
        if (any) {
            ret.addNote("BATTLESIM.RESULT.OVERLOADED");
        }
    }

    /** {@code getForcaBasicaNaval}: the SHIPS' attack, and only the ships'. */
    private long getForcaBasicaNaval(ArmySim fleet) {
        return battleSimFacade.getArmyAttackBase(fleet, ";TTN;", fleet.getLocal());
    }

    /**
     * {@code getForcaPlusNaval}: the flat additions, which the modifiers do not scale.
     *
     * The land layer's {@code getForcaPlus} term for term, with one difference this side of the
     * wire cannot honour: the Judge reads {@code getArtefatoCombateValorNaval()}, which multiplies
     * a combat artifact's value by its {@code ;INC5;} habilidade. {@code Artefato} carries no
     * habilidades to the client, so the base value is used and the multiplier is disclosed.
     *
     * The one-time attack magic is SPENT here, in round 1 - which at sea is the first round that
     * swings. Spending it on the copy is what stops the land layer crediting it a second time.
     */
    private long getForcaPlusNaval(ArmySim fleet) {
        return LandCombatResolver.getForcaPlusFlat(fleet, FIRST_ROUND, true);
    }

    private final LandCombatResolver landResolver = new LandCombatResolver();

    /** {@code getTropaQtBarco()}: hulls, by the {@code ;TTN;} habilidade. */
    private long ships(ArmySim fleet) {
        long ret = 0;
        for (Pelotao pelotao : fleet.getPelotoes().values()) {
            if (pelotao.getTipoTropa() != null && pelotao.getTipoTropa().isBarcos()) {
                ret += pelotao.getQtd();
            }
        }
        return ret;
    }

    /**
     * {@code getConstituicaoTotalNaval()}: the ships' defence plus the army bonus.
     *
     * Computed here rather than added to {@code BattleSimFacade}, which the Judge calls.
     */
    private long constituicaoNaval(ArmySim fleet) {
        final long total = battleSimFacade.getArmyDefense(fleet, true);
        return total > 0 ? total + battleSimFacade.getArmyDefenseBonus(fleet) : 0;
    }

    /**
     * Whether anybody still has an enemy afloat.
     *
     * {@code isVaiDebandarNaval()} is the Judge's test and it compares LIVE damage against the
     * naval constitution - but {@code doCombateDano} has already cleared the damage by the time the
     * end-of-round loop reads it, so in practice it asks whether the fleet still has ships worth
     * anything. That is what is asked here, directly.
     */
    private boolean hasLiveFight(List<ArmySim> fleets, HostilityMatrix matrix,
            Map<ArmySim, ArmySim> toOriginal) {
        for (ArmySim fleet : fleets) {
            if (fleet.isDisband() || constituicaoNaval(fleet) <= 0) {
                continue;
            }
            if (!enemiesOf(fleet, fleets, matrix, toOriginal).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Hostile, still afloat, and a fleet. An army with no hulls is out of this battle. */
    private List<ArmySim> enemiesOf(ArmySim fleet, List<ArmySim> fleets, HostilityMatrix matrix,
            Map<ArmySim, ArmySim> toOriginal) {
        final List<ArmySim> ret = new ArrayList<>();
        if (matrix == null || ships(fleet) <= 0 || fleet.isDisband()) {
            return ret;
        }
        for (ArmySim other : fleets) {
            if (other == fleet || other.isDisband() || ships(other) <= 0) {
                continue;
            }
            // The Judge guards the pair with isInimigo AND a same-nation test. Both, because a
            // nation that is its own enemy in the matrix would otherwise fight itself.
            if (toOriginal.get(fleet).getNacao() == toOriginal.get(other).getNacao()) {
                continue;
            }
            // The Judge's naval outer loop is getExercitosAtacandoIterator() too, so a pair only
            // engages when one of them gave the order. Two fleets that are both standing off draw
            // no blood, and on a three-way hex that is the difference between a battle and a
            // stand-off.
            if ((attacks(fleet) || attacks(other))
                    && matrix.isInimigo(toOriginal.get(fleet), toOriginal.get(other))) {
                ret.add(other);
            }
        }
        return ret;
    }

    /**
     * WON is {@code isEsquadra() && !isDisband()} - still a fleet, and still here.
     *
     * A fleet whose hulls all sank but whose troops reached shore has LOST the sea battle even
     * though the army lives, which is the Judge's own verdict: the test is on being a fleet, not on
     * being alive.
     */
    private void report(List<ArmySim> fleets, Map<ArmySim, ArmySim> toOriginal,
            Map<ArmySim, Boolean> engaged, CombatResult ret) {
        for (ArmySim fleet : fleets) {
            final ArmySim original = toOriginal.get(fleet);
            if (original == null) {
                continue;
            }
            if (!Boolean.TRUE.equals(engaged.get(fleet))) {
                ret.setOutcome(original, CombatLayer.NAVY, CombatResult.Outcome.DID_NOT_FIGHT);
            } else if (ships(fleet) > 0 && !fleet.isDisband()) {
                ret.setOutcome(original, CombatLayer.NAVY, CombatResult.Outcome.WON);
            } else {
                ret.setOutcome(original, CombatLayer.NAVY, CombatResult.Outcome.LOST);
            }
        }
    }

    /** What the Judge can do here and this cannot, said once and only when it applies. */
    private void noteWhatIsOutOfReach(List<ArmySim> fleets, CombatResult ret) {
        for (ArmySim fleet : fleets) {
            for (Pelotao pelotao : fleet.getPelotoes().values()) {
                if (pelotao.getTipoTropa() != null
                        && pelotao.getTipoTropa().hasHabilidade(";TDK;")) {
                    // Scorpions on the hex, and they strike before round 1.
                    ret.addNote("BATTLESIM.RESULT.SCORPIONUNKNOWN");
                    return;
                }
            }
        }
    }

    private static boolean attacks(ArmySim fleet) {
        return fleet.getCombatLevel() != CombatLevel.DEFEND_ONLY;
    }

    private static long banked(Map<ArmySim, Long> pending, ArmySim army) {
        final Long ret = pending.get(army);
        return ret == null ? 0 : ret;
    }
}
