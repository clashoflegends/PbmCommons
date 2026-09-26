package business.combat;

import business.facade.BattleSimFacade;
import business.facade.ExercitoFacade;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import model.Cenario;
import model.Cidade;
import model.Pelotao;

/**
 * The CITY layer of {@code CombateTmpbm}, which is a different battle from the army layer and is
 * resolved by different arithmetic.
 *
 * <h3>Which engine this is</h3>
 *
 * {@code CombateTmpbm.executaCombateCidade}, reached from {@code executaCombates} after the naval
 * and army layers. NOT {@code CombatCity}, which belongs to the newer {@code CombatBase} family and
 * is out of scope for this phase - the same distinction that {@link LandCombatResolver} turns on,
 * and the one that cost a rewrite when the land layer was first built against the wrong engine.
 *
 * <h3>Two rounds, and only the first is optional</h3>
 *
 * <ol>
 *   <li><b>Round 0, siege against the fortification.</b> Fought only when some attacker carries
 *       siege engines. Every attacker's siege attack is summed and the total is put against the
 *       fortification ONCE; a reduction lowers the fortification, which lowers the defense round 1
 *       then computes. Armies take no damage in this round.</li>
 *   <li><b>Round 1, the army against the city.</b> Exactly one round, always. Every attacker's
 *       damage is summed into a single {@code ataqueTotal} and compared with the city's defense
 *       once - so the city layer has no rounds loop at all, unlike the army layer.</li>
 * </ol>
 *
 * <h3>The attack formula is the army one MINUS the tactic</h3>
 *
 * {@code dano = forcaPlus + (forcaBasica * modRelacionamento / 100)}. The Judge's own comment on the
 * loop is "calcula forca e constituicao das tropas, <b>sem tatica</b>", and there is indeed no
 * {@code modTatica} term - where the army layer has {@code forcaBasica * modRel / 100 * modTatica /
 * 100} with its two successive integer divisions. A tactic still decides who dies first, because
 * casualties run through the same {@code doCombateDano} machinery, but it does not change what the
 * walls take.
 *
 * <h3>The city is passive, and the damage split is by troop share</h3>
 *
 * The city never "attacks": its whole defense value is handed back to the attackers in proportion
 * to how many troops each brought, {@code defesa * myTroops / allAttackerTroops}. That is the same
 * shape as the army layer's split, against a single defender that cannot lose.
 *
 * <h3>A tie goes to the defender</h3>
 *
 * The verdict is one comparison, {@code ataqueTotal <= defesa} fails. Equality is a failed assault.
 *
 * <h3>What this deliberately does NOT do</h3>
 *
 * The consequences of the verdict - loyalty movements, pillage, the capture itself, razing a city
 * and the loyalty shock it sends through the owner's OTHER cities - are the Judge's to apply to a
 * real world, and several of them are random ({@code SysApoio.rand}). The verdict and the
 * casualties are deterministic, and they are what a simulator owes. See {@link CityOutcome}.
 */
public class CityCombatResolver {

    /** The Judge's city-assault gate: {@code getCombateNivel() >= 2}. */
    private static final int ATTACK_CITY_LEVEL = CombatLevel.ATTACK_CITY.getNivel();
    private static final int RAZE_LEVEL = CombatLevel.RAZE_CITY.getNivel();
    /** Siege engines: the habilidade whose VALUE is the per-unit damage to a fortification. */
    private static final String SIEGE_ENGINE = ";TTS;";
    /** A nation power that stops a captured camp being destroyed. */
    private static final String KEEP_CAMP_ON_CAPTURE = ";PHC;";
    /** A camp: the size at which a capture razes by default. */
    private static final int CAMP_SIZE = 1;
    /**
     * The city assault is round 1, always - {@code rounds++} happens once, before the army loop.
     * It matters because {@code getForcaPlus} answers ZERO at round 0 and spends the one-time
     * attack magic at any round above it.
     */
    private static final int CITY_ROUND = 1;

    /** How the assault ended. The three branches of the Judge's single comparison. */
    public enum CityOutcome {
        /** Nobody was ordered to assault, or nobody could. No round was fought. */
        NO_ASSAULT,
        /** {@code ataqueTotal <= defesa}. The walls held - and a tie holds. */
        REPELLED,
        /** The city fell and changes hands. */
        CAPTURED,
        /** The city fell and is destroyed: ordered to raze, or a captured camp. */
        RAZED
    }

    private final BattleSimFacade battleSimFacade = new BattleSimFacade();
    private final LandCombatResolver landResolver = new LandCombatResolver();
    private final ExercitoFacade exercitoFacade = new ExercitoFacade();

    /**
     * Resolves the city layer on the working set the earlier layers have already fought with.
     *
     * The copies are not an implementation detail here, they are the correctness: the Judge
     * re-tests the city AFTER the army layer - "checking city again, attacking army may have lost
     * the previous battle" - so the armies that storm the walls are the survivors, at their
     * post-casualty strength, with their ships already anchored away and their one-time attack
     * magic already spent if the army layer swung. {@link CombatCopies} is what carries all of
     * that across, and {@link CombatChain} is what runs the layers in the Judge's order.
     */
    CityResult resolve(CombatScenario scenario, CombatCopies copies) {
        final CityResult ret = new CityResult();
        if (scenario == null || copies == null) {
            return ret;
        }
        final Cidade city = scenario.getCidadeAtiva();
        if (city == null || city.getNacao() == null) {
            return ret;
        }
        final List<ArmySim> attackers = attackersOf(scenario, copies, city);
        if (attackers.isEmpty()) {
            return ret;
        }
        ret.setAttackers(attackers);
        ret.setRaze(isRaze(attackers, city));

        // ROUND 0: siege engines against the fortification, and only if somebody brought them.
        if (isSiegeExpected(attackers)) {
            long siegeTotal = 0;
            for (ArmySim army : attackers) {
                final int siege = siegeAttackOf(army);
                ret.putSiegeAttack(army, siege);
                siegeTotal += siege;
            }
            final int reduction = battleSimFacade.getCitySiegeCombatFactor(city, (int) siegeTotal);
            ret.setFortificationReduction(reduction);
            if (reduction > 0) {
                // On the SCENARIO's own cloned Cidade, never the loaded world's: CombatScenario
                // clones it on setLocal precisely so a what-if cannot reach the real city.
                city.setFortificacao(Math.max(0, city.getFortificacao() - reduction));
            }
        }

        // ROUND 1: the army against the city. One round, one comparison.
        final int defence = battleSimFacade.getCityDefenseCombat(city);
        ret.setDefence(defence);
        long attackTotal = 0;
        long troopsTotal = 0;
        for (ArmySim army : attackers) {
            troopsTotal += exercitoFacade.getQtTropasTotal(army);
        }
        for (ArmySim army : attackers) {
            final long attack = attackOf(army, city, scenario.getRelationships());
            ret.putAttack(army, attack);
            attackTotal += attack;
        }
        ret.setAttackTotal(attackTotal);

        // The city's whole defense comes back at the attackers, split by troop share.
        if (troopsTotal > 0) {
            for (ArmySim army : attackers) {
                final long damage =
                        (long) defence * exercitoFacade.getQtTropasTotal(army) / troopsTotal;
                ret.putDamage(army, damage);
            }
        }
        ret.setOutcome(attackTotal <= defence ? CityOutcome.REPELLED
                : ret.isRaze() ? CityOutcome.RAZED : CityOutcome.CAPTURED);
        return ret;
    }

    /**
     * The Judge's ships-only test is a SAFETY GUARD, not a rule.
     *
     * John, 2026-09-25: navies cannot attack cities, so by the rules and the game design a
     * ships-only army should never be in the attacking list at all. That is why
     * {@code temCombateCidade:1116} can afford to be a bare {@code return false} for the whole hex
     * where everything around it is a {@code continue} - it guards a state that cannot legally
     * arise, so the difference is unreachable.
     *
     * Applied per army here, which is what the property means ({@code isBarcoOnly()} is
     * {@code getTropaQtTotal() == getTropaQtBarco()}) and what the selection loop in
     * {@code executaCombateCidade} does.
     */
    /**
     * Every SURVIVING army that is ordered, able and hostile enough to assault the walls.
     *
     * Reads the copies, so an army the land battle destroyed is not here - {@code canAssault} tests
     * {@code isDisband()} and the copy is where that became true.
     */
    private List<ArmySim> attackersOf(CombatScenario scenario, CombatCopies copies, Cidade city) {
        final RelationshipMatrix relations = scenario.getRelationships();
        final List<ArmySim> ret = new ArrayList<>();
        for (ArmySim army : copies.all()) {
            if (canAssault(army, city, relations, scenario)) {
                ret.add(army);
            }
        }
        return ret;
    }

    /**
     * Ordered, disband, hostile, and able to get ashore.
     *
     * {@code isEsquadraEmbarcada && !isAncoravel} is the one that is easy to miss: a fleet carrying
     * an army cannot assault a city it has nowhere to land at, and the Judge tells the player so.
     */
    private boolean canAssault(ArmySim army, Cidade city, RelationshipMatrix relations,
            CombatScenario scenario) {
        if (army.isDisband() || army.getCombatLevel() == null
                || army.getCombatLevel().getNivel() < ATTACK_CITY_LEVEL) {
            return false;
        }
        // isHostileFrom, not isHostile: the Judge's gate is exercito.isInimigo(cityOwner), which is
        // the ATTACKER'S row and nothing else. A city whose owner hates a neutral army does not
        // thereby drag it into an assault - the city never initiates.
        if (army.getNacao() == null
                || !relations.isHostileFrom(army.getNacao(), city.getNacao())) {
            return false;
        }
        if (exercitoFacade.isBarcoOnly(army)) {
            return false;       // all ships: nothing to put against a wall
        }
        return !(exercitoFacade.isEsquadraEmbarcada(army)
                && scenario.getLocal() != null && !scenario.getLocal().getTerreno().isAncoravel());
    }

    /**
     * Razed rather than captured: ordered to raze, or a captured CAMP.
     *
     * A size-1 city is destroyed by the act of taking it unless the attacker's nation carries
     * {@code ;PHC;}. Checked per attacker and latched, exactly as the Judge does - one raze order
     * among several attackers razes the city for all of them.
     */
    private boolean isRaze(List<ArmySim> attackers, Cidade city) {
        for (ArmySim army : attackers) {
            if (army.getCombatLevel() != null
                    && army.getCombatLevel().getNivel() >= RAZE_LEVEL) {
                return true;
            }
            if (city.getTamanho() == CAMP_SIZE && army.getNacao() != null
                    && !army.getNacao().hasHabilidade(KEEP_CAMP_ON_CAPTURE)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSiegeExpected(List<ArmySim> attackers) {
        for (ArmySim army : attackers) {
            if (exercitoFacade.isSiege(army)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code ExercitoControlFacade.getArmySiegeAtaque}, rebuilt from shared parts.
     *
     * The Judge's own method lives in {@code domain.services}, which the client cannot reach, so
     * this is a TRANSCRIPTION and carries the usual drift risk: it is two sums, and both terms come
     * from code the Judge itself calls.
     *
     * <ul>
     *   <li>every platoon holding {@code ;TTS;} contributes {@code qtd * habilidadeValor(";TTS;")},
     *       the flat per-unit damage a siege engine does to a wall;</li>
     *   <li>every platoon whose troop type is in the siege CATEGORY contributes its ordinary
     *       platoon attack, through the shared {@code getPlatoonAttack}.</li>
     * </ul>
     *
     * The two are not the same test and a platoon can answer both, which is deliberate in the
     * Judge - a siege engine does its wall damage AND swings like a unit.
     */
    private int siegeAttackOf(ArmySim army) {
        float ret = 0;
        for (Pelotao platoon : army.getPelotoes().values()) {
            if (platoon.getTipoTropa() == null) {
                continue;
            }
            if (platoon.getQtd() > 0 && platoon.getTipoTropa().hasHabilidade(SIEGE_ENGINE)) {
                ret += (float) platoon.getQtd()
                        * platoon.getTipoTropa().getHabilidadeValor(SIEGE_ENGINE);
            }
            if (platoon.getTipoTropa().isSiegeCategory()) {
                ret += battleSimFacade.getPlatoonAttack(platoon, army, army.getLocal());
            }
        }
        return Math.round(ret);
    }

    /**
     * {@code forcaPlus + (forcaBasica * modRelacionamento / 100)} - ONE integer division, and no
     * tactic term. See the class note.
     *
     * {@code getForcaPlus} is the army layer's, at round 1: the attack bonus, the one-time magic,
     * the commander's combat artifact and any travelling characters. It is spent here exactly as it
     * is spent there, because in the Judge it is the same method on the same army.
     */
    private long attackOf(ArmySim army, Cidade city, RelationshipMatrix relations) {
        final long forcaBasica =
                battleSimFacade.getArmyAttackBaseLand(army, army.getLocal());
        // THE ARMY LAYER'S getForcaPlus, at round 1, and reused rather than reimplemented: in the
        // Judge this is literally the same exercito.getForcaPlus(rounds) call that the army layer
        // makes. Writing a second copy here would be a copy free to drift, and the pieces it adds -
        // the one-time magic, the commander's combat artifact, a travelling dragon worth commander
        // skill x 100 - are exactly the ones a forecast is most wrong without.
        final long forcaPlus = landResolver.getForcaPlus(army, CITY_ROUND);
        final long modRelacionamento = 100 - bonusRelacionamento(army, city, relations);
        return forcaPlus + (forcaBasica * modRelacionamento / 100);
    }

    /**
     * {@code 100 - getBonusRelacionamento(cityOwner)}, read through the SCENARIO'S MATRIX.
     *
     * The arithmetic is the Judge's - {@code NacaoControl.getBonusRelacionamento} is literally
     * {@code BaseMsgs.dificuldadeBonus[getRelacionamento(nacao) + 3]} - but the SOURCE matters more
     * than the arithmetic, and reading it off the model directly was wrong three ways at once:
     *
     * <ul>
     *   <li><b>The player's diplomacy edits never reached the number.</b> Declaring war in the
     *       matrix panel should raise the assault by 25% of {@code forcaBasica} - sworn enemy is
     *       -25 on the table, so {@code modRelacionamento} goes from 100 to 125 - and it did
     *       nothing. {@code getParticipation} carries a note saying this exact bug, the city layer
     *       re-deriving without the overrides, was already found and fixed once at the
     *       participation level. It had come back in the damage maths.</li>
     *   <li><b>A foreign nation ships an EMPTY relationship map.</b> The headline case - an enemy
     *       assaulting MY city - read neutral and under-forecast the attacker by that same 25%,
     *       while the resolver had already decided the pair WAS hostile. Internally contradictory.</li>
     *   <li><b>{@code relacionamentos} is a TreeMap keyed by model objects</b>, which XStream
     *       restores as references - the miss-on-a-key-that-is-in-keySet hazard the matrix is
     *       identity-keyed to avoid.</li>
     * </ul>
     *
     * DIRECTIONAL, like the gate: the Judge reads the attacker's row.
     */
    private int bonusRelacionamento(ArmySim army, Cidade city, RelationshipMatrix relations) {
        if (army.getNacao() == null || city.getNacao() == null) {
            return 0;
        }
        return msgs.BaseMsgs.dificuldadeBonus[
                relations.getValor(army.getNacao(), city.getNacao()) + 3];
    }

    /** Everything one city assault produced, per attacker where it is per attacker. */
    public static class CityResult {

        private final Map<ArmySim, Integer> siegeAttack = new IdentityHashMap<>();
        private final Map<ArmySim, Long> attack = new IdentityHashMap<>();
        private final Map<ArmySim, Long> damage = new IdentityHashMap<>();
        private List<ArmySim> attackers = new ArrayList<>();
        private CityOutcome outcome = CityOutcome.NO_ASSAULT;
        private int defence;
        private int fortificationReduction;
        private long attackTotal;
        private boolean raze;

        public List<ArmySim> getAttackers() {
            return attackers;
        }

        void setAttackers(List<ArmySim> attackers) {
            this.attackers = attackers;
        }

        public CityOutcome getOutcome() {
            return outcome;
        }

        void setOutcome(CityOutcome outcome) {
            this.outcome = outcome;
        }

        /** The number round 1 had to beat. */
        public int getDefence() {
            return defence;
        }

        void setDefence(int defence) {
            this.defence = defence;
        }

        /** Summed across every attacker, which is what the single comparison uses. */
        public long getAttackTotal() {
            return attackTotal;
        }

        void setAttackTotal(long attackTotal) {
            this.attackTotal = attackTotal;
        }

        /** How many points of fortification round 0 knocked off, or zero. */
        public int getFortificationReduction() {
            return fortificationReduction;
        }

        void setFortificationReduction(int fortificationReduction) {
            this.fortificationReduction = fortificationReduction;
        }

        /** Whether a successful assault razes rather than captures. */
        public boolean isRaze() {
            return raze;
        }

        void setRaze(boolean raze) {
            this.raze = raze;
        }

        public int getSiegeAttack(ArmySim army) {
            final Integer ret = siegeAttack.get(army);
            return ret == null ? 0 : ret;
        }

        void putSiegeAttack(ArmySim army, int value) {
            siegeAttack.put(army, value);
        }

        public long getAttack(ArmySim army) {
            final Long ret = attack.get(army);
            return ret == null ? 0 : ret;
        }

        void putAttack(ArmySim army, long value) {
            attack.put(army, value);
        }

        /** What the walls did back to this army, its share of the city's defense. */
        public long getDamage(ArmySim army) {
            final Long ret = damage.get(army);
            return ret == null ? 0 : ret;
        }

        void putDamage(ArmySim army, long value) {
            damage.put(army, value);
        }
    }
}
