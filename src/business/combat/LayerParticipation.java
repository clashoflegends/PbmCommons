package business.combat;

import business.facade.CidadeFacade;
import business.facade.ExercitoFacade;
import business.interfaces.IExercito;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import model.Cidade;
import model.Terreno;

/**
 * Which of the three layers an army actually fights in, and - just as importantly - WHY it sits out
 * the ones it does not.
 *
 * <h3>Participation is a property of PAIRS, not of armies</h3>
 *
 * This is the part that is easy to get wrong, and the first cut of this class got it wrong. Holding
 * ships does not put an army in the naval layer; holding ships AND facing an enemy that also holds
 * ships does. The Judge builds both fighting layers by pairing:
 *
 * <pre>
 *   naval: exercito.isEsquadra()   &amp;&amp; inimigo.isEsquadra()      (executaCombateNaval)
 *   land : !exercito.isBarcoOnly() &amp;&amp; !inimigo.isBarcoOnly()    (temCombateTerra)
 * </pre>
 *
 * So a fleet whose only enemy is a land army fights nothing at sea, and a land army whose only enemy
 * is a fleet fights nothing ashore. Deciding per army in isolation reports both as fighting, which is
 * wrong in the one direction a simulator must never be wrong: it over-promises.
 *
 * <h3>Attacking and being attacked</h3>
 *
 * The Judge iterates ATTACKING armies and, for each accepted pair, registers BOTH as each other's
 * enemy. An army that initiates nothing is still pulled into a fight someone else starts. Here the
 * attacker is anything above {@link CombatLevel#DEFEND_ONLY}, and the target filter
 * ({@link ArmySim#getTargetNacao()}, the Judge's {@code getCombateNacaoNumero()}) narrows whom it
 * will start on, without protecting it from being started on.
 *
 * <h3>The exclusion reason is the feature</h3>
 *
 * "Why didn't my fleet defend the city?" is a support question whose answer is a rule.
 * {@link #getReason} returns it, drawn from the SAME pass that decided participation, so the
 * explanation cannot drift from the behaviour.
 *
 * <h3>Recomputed, never cached</h3>
 *
 * An instance describes the roster at one moment in the chain. The Judge re-evaluates between layers
 * because fleets land their troops and attackers die, so callers must re-derive rather than hold one
 * of these from load time.
 */
public class LayerParticipation {

    /** Why an army takes no part in a layer. Each maps to exactly one test. */
    public enum Reason {
        /** It is in this layer. */
        FIGHTS,
        /** No ships at all, so nothing to fight a naval battle with. */
        NO_SHIPS,
        /** It has ships, but no enemy here has any. There is no battle at sea to join. */
        NO_NAVAL_ENEMY,
        /** A fleet carrying no land troops. It fights at sea and stops. */
        CARRIES_NO_TROOPS,
        /** It has land troops, but every enemy here is ships-only, or cannot land either. */
        NO_LAND_ENEMY,
        /** Troops are still aboard and the hex has neither anchorable ground nor a port. */
        CANNOT_LAND_HERE,
        /**
         * It is a fleet carrying land troops at a hex with nowhere to put them ashore, and the
         * server withheld its composition, so whether the troops are still aboard CANNOT be
         * answered.
         *
         * This is not pedantry. Transport capacity comes from {@code ;TTT;}, and the placeholder
         * troop type a foreign army collapses into carries {@code ;TTN;} but NOT {@code ;TTT;}. So
         * a placeholder fleet reads as capacity zero, which reads as "the troops are not aboard",
         * which reads as "they are ashore and will fight" - a battle reported that very likely will
         * not happen. Over-promising a fight is the one direction this class must never be wrong
         * in, so the uncertainty is named rather than resolved, and the army sits the layer out.
         */
        LANDING_UNKNOWN,
        /** Ordered to defend only, or to attack armies but not the city. */
        WILL_NOT_ASSAULT_CITY,
        /** There is no city here to assault. */
        NO_CITY,
        /** Not hostile to the city's owner. */
        NOT_HOSTILE_TO_CITY,
        /** Nobody here it is willing to fight at all. */
        NO_ENEMY_PRESENT,
        /** It had already been destroyed before this layer began. */
        DESTROYED_EARLIER
    }

    private static final ExercitoFacade exercitoFacade = new ExercitoFacade();
    private static final CidadeFacade cidadeFacade = new CidadeFacade();

    private final Map<CombatLayer, Reason> reasons = new EnumMap<>(CombatLayer.class);

    private LayerParticipation() {
    }

    /**
     * Works out where every army in the roster fights.
     *
     * Roster-level because the naval and land layers are decided by pairing (see the class note): an
     * army cannot answer either question on its own.
     *
     * @param armies             the whole roster
     * @param terreno            the hex's terrain
     * @param cidade             the city taking part, or null
     * @param matrix             who is hostile to whom
     * @param hostileToCityOwner per army, whether it is hostile to the city's owner. Supplied by the
     *                           caller because reading a relationship safely needs the loaded-EGF
     *                           context, which belongs to {@link HostilityDeriver}.
     * @param unknownComposition armies whose platoons the server replaced with placeholders. Also
     *                           supplied rather than derived, for the same reason: provenance is
     *                           {@code CombatScenario}'s to know. See {@link Reason#LANDING_UNKNOWN}.
     * @return one participation per army, keyed by identity
     */
    public static Map<ArmySim, LayerParticipation> forRoster(List<ArmySim> armies, Terreno terreno,
            Cidade cidade, HostilityMatrix matrix, Map<ArmySim, Boolean> hostileToCityOwner,
            Collection<ArmySim> unknownComposition) {
        final Map<ArmySim, LayerParticipation> ret = new IdentityHashMap<>();
        for (ArmySim army : armies) {
            final LayerParticipation p = new LayerParticipation();
            final List<ArmySim> enemies = enemiesOf(army, armies, matrix);
            p.reasons.put(CombatLayer.NAVY, navy(army, enemies));
            p.reasons.put(CombatLayer.ARMY,
                    land(army, enemies, terreno, cidade, unknownComposition));
            p.reasons.put(CombatLayer.CITY, city(army, terreno, cidade,
                    hostileToCityOwner != null && Boolean.TRUE.equals(hostileToCityOwner.get(army)),
                    unknownComposition));
            ret.put(army, p);
        }
        return ret;
    }

    /** Every army's composition is known. */
    public static Map<ArmySim, LayerParticipation> forRoster(List<ArmySim> armies, Terreno terreno,
            Cidade cidade, HostilityMatrix matrix, Map<ArmySim, Boolean> hostileToCityOwner) {
        return forRoster(armies, terreno, cidade, matrix, hostileToCityOwner, null);
    }

    /** Convenience for one army. It still needs the roster, because pairing does. */
    public static LayerParticipation of(ArmySim army, List<ArmySim> armies, Terreno terreno,
            Cidade cidade, HostilityMatrix matrix, boolean hostileToCityOwner) {
        final Map<ArmySim, Boolean> city = new IdentityHashMap<>();
        city.put(army, hostileToCityOwner);
        return forRoster(armies, terreno, cidade, matrix, city, null).get(army);
    }

    /**
     * Everyone this army will actually exchange blows with.
     *
     * Hostility is necessary but not sufficient: a pair only engages if at least one of them is
     * willing to start it, mirroring the Judge iterating attacking armies and registering both sides.
     * A destroyed army neither starts nor receives.
     */
    private static List<ArmySim> enemiesOf(ArmySim army, List<ArmySim> armies, HostilityMatrix matrix) {
        final List<ArmySim> ret = new ArrayList<>();
        if (matrix == null || isDestroyed(army)) {
            return ret;
        }
        for (ArmySim other : armies) {
            if (other == army || isDestroyed(other) || !matrix.isInimigo(army, other)) {
                continue;
            }
            if (initiates(army, other) || initiates(other, army)) {
                ret.add(other);
            }
        }
        return ret;
    }

    /**
     * Would this army start a fight with that one? Defend-only never initiates, and a target filter
     * means it only initiates against the one nation it named.
     */
    private static boolean initiates(ArmySim attacker, ArmySim target) {
        if (!attacker.getCombatLevel().isAttackArmy()) {
            return false;
        }
        return attacker.getTargetNacao() == null || attacker.getTargetNacao() == target.getNacao();
    }

    private static boolean isDestroyed(IExercito army) {
        return exercitoFacade.getQtTropasTotal(army) <= 0;
    }

    private static Reason navy(ArmySim army, List<ArmySim> enemies) {
        if (isDestroyed(army)) {
            return Reason.DESTROYED_EARLIER;
        }
        if (!exercitoFacade.isEsquadra(army)) {
            return Reason.NO_SHIPS;
        }
        if (enemies.isEmpty()) {
            return Reason.NO_ENEMY_PRESENT;
        }
        for (ArmySim enemy : enemies) {
            if (exercitoFacade.isEsquadra(enemy)) {
                return Reason.FIGHTS;
            }
        }
        return Reason.NO_NAVAL_ENEMY;
    }

    private static Reason land(ArmySim army, List<ArmySim> enemies, Terreno terreno, Cidade cidade,
            Collection<ArmySim> unknownComposition) {
        if (isDestroyed(army)) {
            return Reason.DESTROYED_EARLIER;
        }
        if (exercitoFacade.isBarcoOnly(army)) {
            return Reason.CARRIES_NO_TROOPS;
        }
        final Landing landing = landing(army, terreno, cidade, unknownComposition);
        if (landing == Landing.UNKNOWN) {
            return Reason.LANDING_UNKNOWN;
        }
        if (landing == Landing.ABOARD) {
            return Reason.CANNOT_LAND_HERE;
        }
        if (enemies.isEmpty()) {
            return Reason.NO_ENEMY_PRESENT;
        }
        for (ArmySim enemy : enemies) {
            // an enemy whose landing is UNKNOWN is not counted as present ashore, for the same
            // reason: claiming a battle we cannot show will happen is the forbidden direction
            if (!exercitoFacade.isBarcoOnly(enemy)
                    && landing(enemy, terreno, cidade, unknownComposition) == Landing.ASHORE) {
                return Reason.FIGHTS;
            }
        }
        return Reason.NO_LAND_ENEMY;
    }

    /**
     * The city assault is unary: it is the army against the walls, not against another army, so no
     * pairing applies. The city's garrison, if it has one, meets them in the land layer like anyone
     * else.
     */
    private static Reason city(ArmySim army, Terreno terreno, Cidade cidade,
            boolean hostileToCityOwner, Collection<ArmySim> unknownComposition) {
        if (isDestroyed(army)) {
            return Reason.DESTROYED_EARLIER;
        }
        if (cidade == null) {
            return Reason.NO_CITY;
        }
        if (!army.getCombatLevel().isAttackCity()) {
            return Reason.WILL_NOT_ASSAULT_CITY;
        }
        if (exercitoFacade.isBarcoOnly(army)) {
            return Reason.CARRIES_NO_TROOPS;
        }
        final Landing landing = landing(army, terreno, cidade, unknownComposition);
        if (landing == Landing.UNKNOWN) {
            return Reason.LANDING_UNKNOWN;
        }
        if (landing == Landing.ABOARD) {
            return Reason.CANNOT_LAND_HERE;
        }
        if (!hostileToCityOwner) {
            return Reason.NOT_HOSTILE_TO_CITY;
        }
        return Reason.FIGHTS;
    }

    /** Whether this army's troops can reach the ground here. */
    private enum Landing {
        /** They are on it, or can step onto it. */
        ASHORE,
        /** A loaded fleet with nowhere to put them. */
        ABOARD,
        /** Cannot be answered from the data in hand. See {@link Reason#LANDING_UNKNOWN}. */
        UNKNOWN
    }

    /**
     * A loaded fleet with nowhere to put its troops.
     *
     * Gates BOTH the land layer and the city assault, because the Judge applies it in both places:
     * {@code temCombateTerra} refuses the landing, and {@code CombatCity.addArmies} drops the army
     * with a "cannot anchor" message rather than letting it storm the walls from the water.
     *
     * The UNKNOWN case exists because {@code isEsquadraEmbarcada} compares transport CAPACITY
     * against burden, and capacity comes from {@code ;TTT;} - a habilidade the placeholder troop
     * types do not carry. A withheld composition therefore cannot answer this question, and reading
     * capacity zero as "the troops are ashore" would invent a battle. Only a fleet that is actually
     * carrying land troops at an unanchorable hex is affected; anywhere a fleet can anchor, the
     * question does not arise.
     */
    private static Landing landing(IExercito army, Terreno terreno, Cidade cidade,
            Collection<ArmySim> unknownComposition) {
        if (!exercitoFacade.isEsquadra(army) || isAncoravel(terreno, cidade)) {
            return Landing.ASHORE;
        }
        if (contains(unknownComposition, army)) {
            return Landing.UNKNOWN;
        }
        return exercitoFacade.isEsquadraEmbarcada(army) ? Landing.ABOARD : Landing.ASHORE;
    }

    /** By identity: these are simulator-owned objects, and equals() is not defined for them. */
    private static boolean contains(Collection<ArmySim> armies, IExercito army) {
        if (armies == null) {
            return false;
        }
        for (ArmySim one : armies) {
            if (one == army) {
                return true;
            }
        }
        return false;
    }

    /**
     * Can a fleet put troops ashore here? Mirrors the Judge's {@code Hexagono.isAncoravel()}:
     * anchorable terrain OR a city with docks. A fleet can win at sea and still be unable to land.
     */
    private static boolean isAncoravel(Terreno terreno, Cidade cidade) {
        if (terreno != null && terreno.isAncoravel()) {
            return true;
        }
        return cidade != null && cidadeFacade.isDocasPorto(cidade);
    }

    /** Does the army take part in this layer? */
    public boolean isIn(CombatLayer layer) {
        return Reason.FIGHTS == reasons.get(layer);
    }

    /** Why it does, or does not, take part in this layer. */
    public Reason getReason(CombatLayer layer) {
        return reasons.get(layer);
    }

    /** The roster badge, as in {@code N A C}, with a dot for each layer sat out. */
    public String getBadge() {
        final StringBuilder ret = new StringBuilder();
        for (CombatLayer layer : CombatLayer.values()) {
            ret.append(isIn(layer) ? layer.getBadge() : ".");
        }
        return ret.toString();
    }

    /** Does it fight at all? */
    public boolean isInAnyLayer() {
        for (CombatLayer layer : CombatLayer.values()) {
            if (isIn(layer)) {
                return true;
            }
        }
        return false;
    }
}
