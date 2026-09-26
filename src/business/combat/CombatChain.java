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
 * <h3>What is not here yet</h3>
 *
 * The sea layer. {@code LandCombatResolver} is the only engine besides the new city one, so the
 * chain runs land then city and the result says plainly that the sea layer was not simulated. That
 * is a missing resolver, not a missing hook: when it arrives it goes in front of the land call and
 * needs no other change, because the copies are already threaded.
 */
public class CombatChain {

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

        // LAND. Accumulates into ret: rounds, per-army outcomes, casualties, the fidelity notes.
        landResolver.resolve(scenario, cenario, copies, ret);

        // CITY, on whoever is still standing. The re-test lives inside attackersOf, which reads the
        // copies and skips anything the land battle disbanded.
        final CityCombatResolver.CityResult city = cityResolver.resolve(scenario, cenario, copies, ret);
        ret.setCityResult(city);
        for (ArmySim attacker : city.getAttackers()) {
            final ArmySim original = copies.originalOf(attacker);
            if (original != null) {
                ret.setOutcome(original, CombatLayer.CITY, outcomeOf(city));
            }
        }
        return ret;
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
