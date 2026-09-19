package business.combat;

import business.facade.CidadeFacade;
import business.facade.ExercitoFacade;
import business.interfaces.IExercito;
import java.util.EnumMap;
import java.util.Map;
import model.Cidade;
import model.Terreno;

/**
 * Which of the three layers an army actually fights in, and - just as importantly - WHY it sits out
 * the ones it does not.
 *
 * <h3>Derived, never chosen</h3>
 *
 * Participation falls out of what the army is carrying, where it is standing and what it was told to
 * do. A player never assigns it. That is why the simulator can show a fleet and an army "side by
 * side" without misleading anyone: the badge says which engagements each one is actually in.
 *
 * <h3>The exclusion reason is the feature</h3>
 *
 * A navy that does not defend a city is a support question ("why didn't my fleet fight?"), and the
 * answer is a rule the player did not know. {@link #getReason} returns it, drawn from the SAME
 * predicate that decided participation, so the explanation cannot drift from the behaviour. One
 * method, two outputs.
 *
 * <h3>Recomputed, never cached</h3>
 *
 * An instance describes an army at one moment in the chain. The Judge re-evaluates between layers
 * because fleets land their troops and attackers die, so callers must re-derive rather than hold one
 * of these from load time.
 */
public class LayerParticipation {

    /** Why an army takes no part in a layer. Each maps to exactly one predicate. */
    public enum Reason {
        /** It is in this layer. */
        FIGHTS,
        /** No ships at all, so nothing to fight a naval battle with. */
        NO_SHIPS,
        /** A fleet carrying no land troops. It fights at sea and stops. */
        CARRIES_NO_TROOPS,
        /** Troops are still aboard and the hex has neither anchorable ground nor a port. */
        CANNOT_LAND_HERE,
        /** Ordered to defend only, or to attack armies but not the city. */
        WILL_NOT_ASSAULT_CITY,
        /** There is no city here to assault. */
        NO_CITY,
        /** Not hostile to the city's owner. */
        NOT_HOSTILE_TO_CITY,
        /** Nobody here it is willing to fight. */
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
     * Works out where this army fights.
     *
     * @param army     the army, read-only
     * @param level    its combat intent, which the interface does not carry
     * @param terreno  the hex's terrain
     * @param cidade   the city in the hex, or null when there is none
     * @param matrix   who is hostile to whom, for the enemy-present and city-owner tests
     * @return a participation for this army at this moment
     */
    public static LayerParticipation of(IExercito army, CombatLevel level, Terreno terreno,
            Cidade cidade, HostilityMatrix matrix, boolean hostileToCityOwner) {
        final LayerParticipation ret = new LayerParticipation();
        final boolean destroyed = exercitoFacade.getQtTropasTotal(army) <= 0;
        final boolean anyEnemy = matrix != null && !matrix.getInimigos(army).isEmpty();

        ret.reasons.put(CombatLayer.NAVY, navy(army, destroyed, anyEnemy));
        ret.reasons.put(CombatLayer.ARMY, land(army, terreno, cidade, destroyed, anyEnemy));
        ret.reasons.put(CombatLayer.CITY, city(army, level, terreno, cidade, destroyed, hostileToCityOwner));
        return ret;
    }

    private static Reason navy(IExercito army, boolean destroyed, boolean anyEnemy) {
        if (destroyed) {
            return Reason.DESTROYED_EARLIER;
        }
        if (!exercitoFacade.isEsquadra(army)) {
            return Reason.NO_SHIPS;
        }
        return anyEnemy ? Reason.FIGHTS : Reason.NO_ENEMY_PRESENT;
    }

    private static Reason land(IExercito army, Terreno terreno, Cidade cidade, boolean destroyed,
            boolean anyEnemy) {
        if (destroyed) {
            return Reason.DESTROYED_EARLIER;
        }
        if (exercitoFacade.isBarcoOnly(army)) {
            return Reason.CARRIES_NO_TROOPS;
        }
        if (cannotLand(army, terreno, cidade)) {
            return Reason.CANNOT_LAND_HERE;
        }
        return anyEnemy ? Reason.FIGHTS : Reason.NO_ENEMY_PRESENT;
    }

    private static Reason city(IExercito army, CombatLevel level, Terreno terreno, Cidade cidade,
            boolean destroyed, boolean hostileToCityOwner) {
        if (destroyed) {
            return Reason.DESTROYED_EARLIER;
        }
        if (cidade == null) {
            return Reason.NO_CITY;
        }
        if (level == null || !level.isAttackCity()) {
            return Reason.WILL_NOT_ASSAULT_CITY;
        }
        if (exercitoFacade.isBarcoOnly(army)) {
            return Reason.CARRIES_NO_TROOPS;
        }
        if (cannotLand(army, terreno, cidade)) {
            return Reason.CANNOT_LAND_HERE;
        }
        if (!hostileToCityOwner) {
            return Reason.NOT_HOSTILE_TO_CITY;
        }
        return Reason.FIGHTS;
    }

    /**
     * A loaded fleet with nowhere to put its troops.
     *
     * Gates BOTH the land layer and the city assault, because the Judge applies it in both places:
     * {@code temCombateTerra} refuses the landing, and {@code CombatCity.addArmies} drops the army
     * with a "cannot anchor" message rather than letting it storm the walls from the water.
     */
    private static boolean cannotLand(IExercito army, Terreno terreno, Cidade cidade) {
        return exercitoFacade.isEsquadra(army)
                && exercitoFacade.isEsquadraEmbarcada(army)
                && !isAncoravel(terreno, cidade);
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
