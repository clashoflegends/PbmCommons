package business.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Pelotao;

/**
 * The armies the simulation actually fights with: ONE copy of each, shared by every layer.
 *
 * <h3>Why one set and not one per layer</h3>
 *
 * The Judge runs the whole of {@code CombateTmpbm.executaCombates} on a single set of
 * {@code ExercitoControl} objects, and four things depend on that:
 *
 * <ul>
 *   <li><b>Casualties carry forward.</b> An army mauled in the army layer brings less to the walls,
 *       both in attack and in its share of the city's returned damage.</li>
 *   <li><b>The dead do not assault.</b> {@code executaCombates} re-tests the city AFTER the army
 *       layer - "checking city again, attacking army may have lost the previous battle".</li>
 *   <li><b>One-time attack magic is spent ONCE.</b> {@code getForcaPlus} zeroes it, and in the
 *       Judge that is the same field on the same object across layers. Two layers on two copy sets
 *       would each find it unspent and credit it twice.</li>
 *   <li><b>Anchored ships stay anchored.</b> They leave the army before the land battle and are
 *       therefore already gone from the troop count the city layer divides damage by.</li>
 * </ul>
 *
 * <h3>Copies, and why they must be copies</h3>
 *
 * The player's own armies are never touched. Run has to be repeatable - press it twice, get the
 * same answer - and the numbers in the editor are his inputs, not the simulation's scratch space.
 * That is the contract {@code LandCombatResolver} has always kept and the one
 * {@code CityCombatResolver} broke before this class existed: it spent the one-time magic on the
 * originals and zeroed the player's own spinner.
 *
 * <h3>Anchoring happens ONCE, and AFTER the sea layer</h3>
 *
 * Ships leave an army that fights ashore, and the Judge does it per layer -
 * {@code doAncoraBarcoAll} inside {@code executaMsgBasicaCombateLand} for the land battle,
 * {@code doAncoraEsquadras} before a city-only one. On a single working set the second call has
 * nothing left to do, so doing it once is the same thing.
 *
 * WHEN it happens is not a detail. {@code executaCombates} calls {@code executaCombateNaval()}
 * FIRST, before either anchoring site, so the fleets fight at sea with their hulls and only then
 * put troops ashore. This class used to anchor at copy time, which was indistinguishable from the
 * Judge while the sea layer did not exist and became wrong the moment it did: every ship would have
 * been gone before the first naval round. So it is {@link #doAnchor} now, called by the chain
 * between the sea and land layers, and by the standalone land entry that has no sea layer in front
 * of it.
 *
 * The ships are SET ASIDE, not destroyed. {@link #anchored} is where they go - the sim's stand-in
 * for the hex garrison {@code sumBarcoAncorado} puts them in - so the final snapshot can tell a
 * fleet that beached its boats from one that lost them.
 */
final class CombatCopies {

    private final List<ArmySim> copies = new ArrayList<>();
    private final Map<ArmySim, ArmySim> toOriginal = new IdentityHashMap<>();
    /**
     * The city, copied PER RUN for the same reason the armies are.
     *
     * {@code CombatScenario.setLocal} clones the loaded world's city ONCE, at load - which protects
     * the world but makes that clone the player's durable input, sitting behind the Ground panel's
     * size, fortification and loyalty controls. The siege round subtracts from the fortification,
     * so without a second copy Run #1 lowers the wall, Run #2 starts from the lowered wall and
     * reports a different defense and possibly a different verdict with nothing changed.
     */
    private model.Cidade city;
    /** Per copy, the ship platoons anchored out of it. The sim's hex garrison. */
    private final Map<ArmySim, SortedMap<String, Pelotao>> anchored = new IdentityHashMap<>();

    private CombatCopies() {
    }

    /**
     * One copy of every army on the hex, ships still aboard, ready for the SEA layer.
     *
     * EVERY army, not just the ones fighting on some layer: an army that takes no part still has to
     * be reported, and a later layer may admit an army an earlier one declined. Filtering is the
     * layer's job, through {@link #inLayer}.
     *
     * Not anchored. See {@link #doAnchor}, and the class note on why the order matters.
     */
    static CombatCopies of(CombatScenario scenario) {
        final CombatCopies ret = new CombatCopies();
        if (scenario == null) {
            return ret;
        }
        final model.Cidade active = scenario.getCidadeAtiva();
        ret.city = active == null ? null : active.clone();
        for (ArmySim original : scenario.getArmies()) {
            final ArmySim copy = new ArmySim(original);
            ret.copies.add(copy);
            ret.toOriginal.put(copy, original);
        }
        return ret;
    }

    /**
     * Puts every fleet's boats ashore, once, before anybody fights on land.
     *
     * Idempotent, because the Judge's two anchoring sites are: the second finds nothing left to do.
     */
    void doAnchor(CombatScenario scenario) {
        if (scenario == null) {
            return;
        }
        for (ArmySim copy : copies) {
            final SortedMap<String, Pelotao> aside =
                    LandCombatResolver.doAncoraBarcos(copy, scenario);
            if (aside.isEmpty()) {
                continue;
            }
            anchored.computeIfAbsent(copy, key -> new TreeMap<>()).putAll(aside);
        }
    }

    /** Per copy, the ship platoons this run put ashore. Never null. */
    Map<ArmySim, SortedMap<String, Pelotao>> anchored() {
        return anchored;
    }

    /** The city this run may damage, or null when no city takes part. Never the scenario's own. */
    model.Cidade city() {
        return city;
    }

    /** Every copy, in the hex's own order. */
    List<ArmySim> all() {
        return Collections.unmodifiableList(copies);
    }

    /** Copy back to the army the player is looking at, for reporting. */
    Map<ArmySim, ArmySim> toOriginal() {
        return toOriginal;
    }

    ArmySim originalOf(ArmySim copy) {
        return toOriginal.get(copy);
    }

    /**
     * The copies whose ORIGINAL takes part in this layer, minus anything already destroyed.
     *
     * Participation is derived from the scenario - the player's setup - because that is where the
     * orders and the diplomacy live, and neither changes because a battle happened. What DOES
     * change is who is still standing, so the disband test is applied to the COPY. That pairing is
     * the re-gate the Judge performs between layers, and getting it backwards either resurrects the
     * dead or loses an army's orders.
     */
    List<ArmySim> inLayer(CombatScenario scenario, CombatLayer layer) {
        final List<ArmySim> ret = new ArrayList<>();
        if (scenario == null) {
            return ret;
        }
        final Map<ArmySim, LayerParticipation> participation = scenario.getParticipation();
        for (ArmySim copy : copies) {
            if (copy.isDisband()) {
                continue;
            }
            final LayerParticipation part = participation.get(toOriginal.get(copy));
            if (part != null && part.isIn(layer)) {
                ret.add(copy);
            }
        }
        return ret;
    }
}
