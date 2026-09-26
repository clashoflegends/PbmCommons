package business.combat;

import model.Cenario;

/**
 * The whole battle on one hex, layer by layer, in the order {@code CombateTmpbm.executaCombates}
 * runs them.
 *
 * <h3>Why the chain is a thing and not just two calls</h3>
 *
 * Because the layers are not independent. The Judge's orchestrator (823-871) is:
 *
 * <pre>
 *   executaCombateNaval()                      // sea
 *   temCombateTerra()   -&gt; executaCombateExercito()   // land
 *   temCombateCidade()  -&gt; executaCombateCidade()     // city, RE-TESTED after the land battle
 * </pre>
 *
 * and every one of those runs on the same {@code ExercitoControl} objects. The re-test is the part
 * that is easy to miss and impossible to bolt on afterwards - its own comment says "checking city
 * again, attacking army may have lost the previous battle". An army wiped out ashore does not then
 * storm the walls; a mauled one storms them with what it has left.
 *
 * So the chain owns {@link CombatCopies} and hands the same set to each layer in turn. Four things
 * ride on that and all four were wrong when the city layer was first written against the player's
 * untouched scenario: carried-forward casualties, the dead not assaulting, one-time attack magic
 * spent once rather than once per layer, and ships anchored away before the troop share is divided.
 *
 * <h3>All three layers resolve</h3>
 *
 * Sea, then land, then city, with the anchoring between the first two - which is where
 * {@code executaMsgBasicaCombateLand} puts it, and it is load-bearing: anchor before the sea layer
 * and every ship is gone before the first naval round.
 *
 * What the chain still cannot answer is named in the result's notes rather than left to be
 * inferred: coastal drowning is a die roll, and the scorpion anti-dragon strike needs dragon
 * vitality that does not cross the wire.
 */
public class CombatChain {

    /** A city assault is one round, always - {@code rounds++} happens once, before the army loop. */
    private static final int CITY_ROUNDS = 1;

    private final NavyCombatResolver navyResolver = new NavyCombatResolver();
    private final LandCombatResolver landResolver = new LandCombatResolver();
    private final CityCombatResolver cityResolver = new CityCombatResolver();

    /**
     * Fights every implemented layer and reports them as one battle.
     *
     * @param scenario the player's setup, LEFT UNTOUCHED - the chain fights with copies of it
     * @param cenario  for the tactic-versus-tactic table and the casualty-order rule
     */
    public CombatResult resolve(CombatScenario scenario, Cenario cenario) {
        final CombatResult ret = new CombatResult();
        if (scenario == null) {
            return ret;
        }
        final CombatCopies copies = CombatCopies.of(scenario);

        // NOBODY has fought the sea or the city yet. Seeded BEFORE any layer, because a null
        // outcome MEANS "not simulated" - so without this the defending garrison, every neutral
        // army and, when no assault happens, EVERY army would read as though the layer never ran.
        // The land resolver does the same thing for its own layer and for the same reason.
        for (ArmySim army : scenario.getArmies()) {
            ret.setOutcome(army, CombatLayer.NAVY, CombatResult.Outcome.DID_NOT_FIGHT);
            ret.setOutcome(army, CombatLayer.CITY, CombatResult.Outcome.DID_NOT_FIGHT);
        }

        // SEA, first and with every hull still aboard. executaCombates calls executaCombateNaval()
        // before either of the Judge's two anchoring sites, so the fleets fight with their ships
        // and only the survivors put anything ashore.
        final java.util.Set<ArmySim> foughtAtSea = new java.util.HashSet<>(
                navyResolver.resolve(scenario, cenario, copies, ret));

        // ANCHOR, between the two, exactly where executaMsgBasicaCombateLand does it. Before the
        // sea layer existed this happened at copy time, which was the same thing; it is not any
        // more, and doing it here is the whole reason CombatCopies no longer does it itself.
        copies.doAnchor(scenario);

        // LAND. Accumulates into ret: rounds, per-army outcomes, casualties, the fidelity notes.
        landResolver.resolve(scenario, cenario, copies, ret);

        // CITY, on whoever is still standing. The re-test lives inside attackersOf, which reads the
        // copies and skips anything the land battle disbanded.
        final CityCombatResolver.CityResult city =
                cityResolver.resolve(scenario, cenario, copies, ret);
        ret.setCityResult(city);
        ret.setRounds(CombatLayer.CITY, city.getAttackers().isEmpty() ? 0 : CITY_ROUNDS);
        for (ArmySim attacker : city.getAttackers()) {
            final ArmySim original = copies.originalOf(attacker);
            if (original != null) {
                ret.setOutcome(original, CombatLayer.CITY, outcomeOf(city));
            }
        }
        // The unknown-morale disclosure belongs to the BATTLE, not to the land layer - and the land
        // resolver returns before writing it when fewer than two armies engage, which is exactly a
        // city-only assault. Written here when the land layer did not.
        if (ret.getNoteCount("BATTLESIM.RESULT.UNKNOWNMORALE") == 0) {
            noteUnknownMorale(scenario, city, ret);
        }
        // LAST, and over EVERY copy: the platoon table's After and Lost describe the end of the
        // BATTLE, not the end of a layer. Taken at the end of the land layer it showed an army
        // mauled at the walls at its pre-assault strength, and left a city-only attacker's table
        // empty after a battle it had just fought.
        LandCombatResolver.doSnapshotSurvivors(copies.all(), copies.toOriginal(),
                copies.anchored(), foughtAtSea, ret);
        return ret;
    }

    /**
     * How many of the armies AT THE WALLS are fighting on a morale the player had to guess.
     *
     * Scoped to the attackers, because that is who this layer's numbers depend on. The land
     * resolver counts its own fighters for the same reason; neither should count an army standing
     * on the hex taking no part.
     */
    private void noteUnknownMorale(CombatScenario scenario, CityCombatResolver.CityResult city,
            CombatResult ret) {
        int unknown = 0;
        for (ArmySim attacker : city.getAttackers()) {
            if (scenario.isMoraleUnknown(attacker)) {
                unknown++;
            }
        }
        if (unknown > 0) {
            ret.addNote("BATTLESIM.RESULT.UNKNOWNMORALE", unknown);
        }
    }

    /**
     * How the assault ended, from the attacker's point of view.
     *
     * Every attacker shares one verdict because the Judge compares ONE summed {@code ataqueTotal}
     * with the defense - there is no per-army success in the city layer, which is what makes it a
     * different battle from the army one rather than a variation on it.
     */
    private CombatResult.Outcome outcomeOf(CityCombatResolver.CityResult city) {
        switch (city.getOutcome()) {
            case CAPTURED:
            case RAZED:
                return CombatResult.Outcome.WON;
            case CAPTURED_NO_SURVIVOR:
                // A WIN, however it reads. The verdict block at CombateTmpbm:718-738 re-tests only
                // ataqueTotal <= defesa and destroiCidade, so the no-survivor path falls into the
                // final else and calls setCombateVenceu(TRUE, atacanteComandante) at 737 - after
                // doCityCaptured has already returned CombateAtaqueFalhouNosurvivor. The commanders
                // are credited with the victory. An earlier comment here asserted the opposite.
                return CombatResult.Outcome.WON;
            case REPELLED:
                return CombatResult.Outcome.LOST;
            default:
                return CombatResult.Outcome.DID_NOT_FIGHT;
        }
    }
}
