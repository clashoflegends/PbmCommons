package business.combat;

import business.facade.CenarioFacade;
import business.interfaces.IExercito;
import model.Cenario;

/**
 * How damage is spread across an army's platoons: down a ranked list, or evenly over all of them.
 *
 * <h3>Why this has to be said out loud</h3>
 *
 * John, 2026-09-20: "standard tactics splits the damage across all platoons equally, no specific
 * sequence like the other tactics. So not to mislead players, it hid the sequence."
 *
 * The old BattleSim's answer was to blank the platoon table entirely under the Standard tactic
 * ({@code BattleSimPlatoonCasualtyControlerNew:101-103}) - honest about the order, and it took the
 * troop list away with it and explained nothing. The rebuild shows the list and therefore has to
 * say what the ORDER means, because a list of platoons in rows looks like a sequence whether or not
 * one exists. A row order presented as a casualty order when there is none is exactly the silent
 * plausible answer this project keeps hunting.
 *
 * <h3>The rule, from {@code ExercitoControl.doCombateDano}</h3>
 *
 * <pre>
 * if (isCombatTypeNaval() || (getTatica() != 2 &amp;&amp; cenario.hasCombatCasualtiesTactics()))
 *     doCombateDanoTatica()    // BY RANK - walk the sorted list, destroy each platoon in turn
 * else
 *     doCombateBaixasPadrao()  // PROPORTIONAL - every land platoon loses the same percentage
 * </pre>
 *
 * So there are THREE ways to end up without a sequence, not the one that was obvious:
 *
 * <ol>
 *   <li>the tactic is Standard (2) - John's case;</li>
 *   <li>the scenario does not carry {@code ;CTC;}, in which case NO tactic ranks casualties on
 *       land, whatever the player picks;</li>
 *   <li>and conversely, naval combat ALWAYS ranks, even under Standard - so one army can have a
 *       meaningful order for its ships and none for its troops at the same time.</li>
 * </ol>
 *
 * That is why this is answered per LAYER rather than per army.
 */
public enum CasualtyMode {

    /**
     * Damage walks a sorted list and consumes platoons in turn. The row order IS the answer.
     *
     * This is the mode that makes the tactic worth choosing, and the reason the platoon table is
     * sorted by {@code ComparatorCasualtiesSorter} at all - John: "in some terrains catapults will
     * die before infantry unless guerrilla is selected... you want to spend the infantry to win the
     * army combat to use the catapults in the city layer."
     */
    BY_RANK,

    /**
     * Every platoon in the layer loses the same PERCENTAGE. There is no first and no last.
     *
     * {@code doCombateBaixasPadrao} turns the damage into a percentage of the army's total
     * constitution and applies it to each land platoon in turn, so nothing is spared and nothing is
     * consumed first. Note it skips {@code isBarcos()} platoons outright - ships are not touched on
     * this path at all.
     */
    PROPORTIONAL;

    /** The tactic id the game gives "Standard", and the one that spreads damage evenly. */
    public static final int TATICA_STANDARD = 2;

    /**
     * @param army   the army whose platoons are being shown
     * @param cenario the scenario, for {@code ;CTC;}
     * @param layer  which layer's platoons; naval always ranks
     * @return how damage will be spread over that layer's platoons
     */
    public static CasualtyMode of(IExercito army, Cenario cenario, CombatLayer layer) {
        if (layer == CombatLayer.NAVY) {
            // isCombatTypeNaval() short-circuits the whole condition in the Judge
            return BY_RANK;
        }
        if (army == null || army.getTatica() == TATICA_STANDARD) {
            return PROPORTIONAL;
        }
        return cenario != null && new CenarioFacade().hasCombatCasualtiesTactics(cenario)
                ? BY_RANK : PROPORTIONAL;
    }

    /** Does the row order of a platoon list mean anything in this mode? */
    public boolean isSequenced() {
        return this == BY_RANK;
    }
}
