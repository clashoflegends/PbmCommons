package business.combat;

import business.facade.BattleSimFacade;
import business.facade.CenarioFacade;
import business.facade.ExercitoFacade;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import model.Cenario;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;

/**
 * Resolves the LAND layer of a simulated battle, round by round, and reports the casualties.
 *
 * <h3>A transcription, not a reimplementation</h3>
 *
 * Every number here comes from a facade the Judge itself calls -
 * {@code BattleSimFacade.getPlatoonAttack} and {@code getPlatoonDefense},
 * {@code CenarioFacade.getTaticaBonus}, {@code ExercitoFacade.subTropaQt}, and the casualty
 * comparator behind {@code ComparatorFactory}. What this class contributes is the LOOP, transcribed
 * from {@code CombatLand.doDistributeDamage} and {@code doCombatRound}: snapshot, armies strongest
 * first, platoons in casualty order, attack points consumed down the defender's list.
 *
 * It exists because those two methods live in PbmJudge and take {@code ExercitoControl}. Moving
 * them is T-903 and it is the better end state; this is what lets the simulator answer today
 * without a Judge change, and it is meant to be DELETED when the real class moves across. The
 * arithmetic is shared either way, so the thing being thrown away is a loop, not a rules engine.
 *
 * <h3>It runs on COPIES</h3>
 *
 * The Judge resolves in place, because its armies are the world. Here the armies are the player's
 * setup, and he will press Run again after changing a tactic - so the battle is fought by clones
 * and the originals are untouched. The old window carried the same intent as a TODO it never
 * reached: "clone the army so that we can run multiple simulations without changing the BattleSim".
 *
 * <h3>What it does NOT do, and says so</h3>
 *
 * <ul>
 *   <li>The NAVY and CITY layers. This is the land layer only; the three-layer chain is T-801's
 *       remainder.</li>
 *   <li>{@code doTroopPowers} - corruption and undead raising. It needs the habilidade DAO, which
 *       the client has no access to, so a battle involving {@code ;TCT;}-family troops is reported
 *       as incomplete rather than quietly resolved without them.</li>
 *   <li>The troop-matchup modifiers. They are identity functions HERE because they are identity
 *       functions in the Judge - see {@link #attackModifier} and KI-055.</li>
 *   <li>Any narrative. Numbers only; the messages are T-802/T-803 and need the shared tokens
 *       (T-900).</li>
 * </ul>
 */
public class LandCombatResolver {

    /**
     * Stop after this many rounds and say so, rather than hanging.
     *
     * The Judge's loop is {@code while (isCombatCleared())} with no cap, which is safe there only
     * because a real battle always kills somebody. A SIMULATOR is handed whatever the player typed,
     * and an army whose attack rounds to zero against a defence that never falls would spin here
     * forever. Stopping loudly is the honest answer; a frozen window is not.
     */
    private static final int MAX_ROUNDS = 100;

    private final BattleSimFacade battleSimFacade = new BattleSimFacade();
    private final ExercitoFacade exercitoFacade = new ExercitoFacade();
    private final CenarioFacade cenarioFacade = new CenarioFacade();

    /**
     * Fights the land battle and reports what is left.
     *
     * @param scenario the player's setup, LEFT UNTOUCHED
     * @param cenario  for the tactic-versus-tactic bonus table
     */
    public CombatResult resolve(CombatScenario scenario, Cenario cenario) {
        final CombatResult ret = new CombatResult();
        // Stated on EVERY result, including the ones that resolve cleanly. A land-only answer read
        // beside a fleet sitting on the same hex is wrong by omission, and omission is exactly what
        // a footnote-only caveat produces. It leaves the moment the sea and city layers land.
        ret.addNote("BATTLESIM.RESULT.LANDONLY");
        if (scenario == null) {
            return ret;
        }
        // the land fighters, as COPIES, paired back to the originals
        final List<ArmySim> fighters = new ArrayList<>();
        final Map<ArmySim, ArmySim> toOriginal = new IdentityHashMap<>();
        final Map<ArmySim, LayerParticipation> participation = scenario.getParticipation();
        for (ArmySim original : scenario.getArmies()) {
            final LayerParticipation part = participation.get(original);
            if (part == null || !part.isIn(CombatLayer.ARMY)) {
                continue;
            }
            final ArmySim copy = new ArmySim(original);
            fighters.add(copy);
            toOriginal.put(copy, original);
        }
        if (fighters.size() < 2) {
            return ret;
        }
        noteWhatWasSkipped(fighters, ret);

        final HostilityMatrix matrix = scenario.getMatrix();
        int round = 0;
        while (round < MAX_ROUNDS && hasLiveFight(fighters, matrix, toOriginal)) {
            doDistributeDamage(fighters, matrix, toOriginal, scenario.getTerreno(), cenario, round);
            round++;
        }
        ret.setRounds(round);
        if (round >= MAX_ROUNDS) {
            ret.addNote("BATTLESIM.RESULT.CAPPED");
        }
        for (ArmySim copy : fighters) {
            final ArmySim original = toOriginal.get(copy);
            for (Pelotao was : original.getPelotoes().values()) {
                if (was.getTipoTropa() != null && was.getTipoTropa().isBarcos()) {
                    continue;   // ships take no part in the LAND layer - absent, not zero
                }
                final Pelotao now = copy.getPelotoes().get(was.getCodigo());
                ret.put(was, was.getQtd(), now == null ? 0 : now.getQtd());
            }
        }
        return ret;
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
     * Is there still a fight? The stand-in for {@code CombatArmy.isCombatCleared()}.
     *
     * Rebuilt every round rather than cached, exactly as the Judge rebuilds its enemy lists: an
     * army wiped out in round N has to stop being a target in round N+1.
     */
    private boolean hasLiveFight(List<ArmySim> fighters, HostilityMatrix matrix,
            Map<ArmySim, ArmySim> toOriginal) {
        for (ArmySim one : fighters) {
            if (!enemiesOf(one, fighters, matrix, toOriginal).isEmpty()
                    && exercitoFacade.getQtTropasTotal(one) > 0) {
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
     * battle resolves in zero rounds, which is exactly what it did first time.
     */
    private List<ArmySim> enemiesOf(ArmySim army, List<ArmySim> fighters, HostilityMatrix matrix,
            Map<ArmySim, ArmySim> toOriginal) {
        final List<ArmySim> ret = new ArrayList<>();
        for (ArmySim other : fighters) {
            if (other != army && exercitoFacade.getQtTropasTotal(other) > 0
                    && matrix.isInimigo(toOriginal.get(army), toOriginal.get(other))) {
                ret.add(other);
            }
        }
        return ret;
    }

    /**
     * One round: every army in turn, strongest first, spends its platoons' attack.
     *
     * The SNAPSHOT is the subtle part and it is the Judge's own comment - "create a snapshot of
     * pelotoes each round, to apply same damage, even after casualties". Every attacker in a round
     * strikes the army as it stood when the round began, so two attackers do not queue up behind
     * each other's casualties. Losses are written back only once the round ends.
     */
    private void doDistributeDamage(List<ArmySim> fighters, HostilityMatrix matrix,
            Map<ArmySim, ArmySim> toOriginal, Terreno terreno, Cenario cenario, int round) {
        final Map<ArmySim, List<Pelotao>> remaining = new IdentityHashMap<>();
        for (ArmySim army : fighters) {
            remaining.put(army, exercitoFacade.listaTropasTerra(army));
        }
        for (ArmySim army : strongestFirst(fighters)) {
            for (Pelotao attacker : new ArrayList<>(army.getPelotoes().values())) {
                final TipoTropa tipo = attacker.getTipoTropa();
                if (tipo == null || tipo.isBarcos()) {
                    continue;
                }
                if (round == 0 && !tipo.hasHabilidade(";TT1;")) {
                    continue;       // round 0 is first strike only
                }
                doCombatRound(army, attacker, fighters, matrix, toOriginal, remaining, terreno,
                        cenario);
            }
        }
        doSaveCasualties(fighters, remaining);
    }

    /** Strongest first: land attack plus naval attack weighted ten times, as the Judge orders them. */
    private List<ArmySim> strongestFirst(List<ArmySim> fighters) {
        final List<ArmySim> ret = new ArrayList<>(fighters);
        Collections.sort(ret, new Comparator<ArmySim>() {
            @Override
            public int compare(ArmySim one, ArmySim other) {
                return (int) (attackWeight(other) - attackWeight(one));
            }
        });
        return ret;
    }

    private float attackWeight(ArmySim army) {
        return battleSimFacade.getArmyAttackBaseLand(army, army.getLocal())
                + battleSimFacade.getArmyAttackBase(army, ";TTN;", army.getLocal()) * 10;
    }

    /**
     * One platoon's attack, spent down the enemy's casualty-ordered list.
     *
     * The shape to keep: attack points are consumed by each defending platoon's DEFENCE. A platoon
     * that dies outright costs its whole defence and the rest carries on to the next one down the
     * list; a platoon that survives absorbs everything left, and the attack ends there. That is why
     * the casualty order matters so much to the player - it decides who absorbs and who is spared.
     */
    private void doCombatRound(ArmySim army, Pelotao attacker, List<ArmySim> fighters,
            HostilityMatrix matrix, Map<ArmySim, ArmySim> toOriginal,
            Map<ArmySim, List<Pelotao>> remaining, Terreno terreno, Cenario cenario) {
        final List<ArmySim> enemies = enemiesOf(army, fighters, matrix, toOriginal);
        Collections.sort(enemies, new Comparator<ArmySim>() {
            @Override
            public int compare(ArmySim one, ArmySim other) {
                return (int) (attackWeight(other) - attackWeight(one));
            }
        });
        for (ArmySim enemy : enemies) {
            doCancelMagic(army, enemy);
            final long base = (long) battleSimFacade.getPlatoonAttack(attacker, army,
                    army.getLocal(), terreno);
            final long modTatica = cenario == null ? 100
                    : cenarioFacade.getTaticaBonus(cenario, army.getTatica(), enemy.getTatica());
            long attack = base * modTatica / 100;
            for (Pelotao defender : new ArrayList<>(remaining.get(enemy))) {
                if (attack <= 0) {
                    return;
                }
                final float attackMod = attackModifier(attack, attacker.getTipoTropa(),
                        defender.getTipoTropa());
                final float defenseMod = defenseModifier(
                        battleSimFacade.getPlatoonDefense(enemy, defender),
                        attacker.getTipoTropa(), defender.getTipoTropa());
                if (defenseMod <= 0f) {
                    continue;       // nothing to absorb the blow, and no divide by zero
                }
                final int available = defender.getQtd();
                final int lost = (int) Math.min(available,
                        Math.ceil(available * attackMod / defenseMod));
                if (lost >= available) {
                    remaining.get(enemy).remove(defender);
                    attack -= (long) defenseMod;    // excess carries to the next platoon down
                } else {
                    defender.setQtd(available - lost);
                    return;                          // the attack is spent
                }
            }
        }
    }

    /**
     * Attack magic against defence magic: each cancels the other, and the remainder stands.
     *
     * Transcribed from {@code CombatLand.doCancelMagic}. Both values are plain ints on
     * {@link business.interfaces.IExercito}, already carrying whatever the pre-combat spell orders
     * put there - so the commander's magic is in the battle without any of the commander's chain.
     */
    private void doCancelMagic(ArmySim attacker, ArmySim defender) {
        final int matt = attacker.getCombateAtaqueOnetime();
        final int mdef = defender.getArmyDefenseBonus();
        if (matt == 0 || mdef == 0) {
            return;
        }
        if (matt > mdef) {
            attacker.setCombateAtaqueOnetime(matt - mdef);
            defender.setArmyDefenseBonus(0);
        } else if (matt < mdef) {
            attacker.setCombateAtaqueOnetime(0);
            defender.setArmyDefenseBonus(mdef - matt);
        } else {
            attacker.setCombateAtaqueOnetime(0);
            defender.setArmyDefenseBonus(0);
        }
    }

    /**
     * The troop-matchup attack modifier. An IDENTITY function, on purpose - see KI-055.
     *
     * {@code ExercitoControlFacade.getPlatoonAttackModifier} computes five matchup bonuses
     * (dwarf-vs-orc, orc-vs-dwarf, ranged-vs-flying, small-vs-giant, the giant penalty) and then
     * returns the value it captured BEFORE applying any of them. It is a no-op in the Judge, so it
     * is a no-op here: a simulator's job is to predict the turn that will actually run.
     *
     * Named rather than omitted so the seam exists. When KI-055 is fixed - John's call, ideally
     * inside the engine merge, since the flags are WDO's and the new engine adds a flying layer -
     * this method and its sibling are the two places that change.
     */
    private float attackModifier(long attack, TipoTropa attacker, TipoTropa defender) {
        return attack;
    }

    /** The defence side of the same no-op. See {@link #attackModifier} and KI-055. */
    private float defenseModifier(float defense, TipoTropa attacker, TipoTropa defender) {
        return defense;
    }

    /** Writes the round's losses back, through the shared subtraction the Judge uses. */
    private void doSaveCasualties(List<ArmySim> fighters, Map<ArmySim, List<Pelotao>> remaining) {
        for (ArmySim army : fighters) {
            final List<Pelotao> left = remaining.get(army);
            for (Pelotao pelotao : new ArrayList<>(army.getPelotoes().values())) {
                if (pelotao.getTipoTropa() == null || pelotao.getTipoTropa().isBarcos()) {
                    continue;
                }
                int survivors = 0;
                for (Pelotao one : left) {
                    if (one.getTipoTropa() == pelotao.getTipoTropa()) {
                        survivors = one.getQtd();
                        break;
                    }
                }
                exercitoFacade.subTropaQt(army, pelotao.getTipoTropa(),
                        pelotao.getQtd() - survivors);
            }
        }
    }
}
