package business.combat;

import business.interfaces.IExercito;

/**
 * The public/private seam for what an NPC travelling with a commander adds to an attack. T-906.
 *
 * <h3>Why this interface has to exist BEFORE the engine moves</h3>
 *
 * D-10 settled that the simulator's parity is bounded at roughly 97 percent on purpose: troop
 * versus troop resolution becomes public, and NPC behaviour does NOT. The remaining steps of the
 * refactor (T-901 to T-905) move the engine classes into PbmCommons, which is a public repository
 * with outside collaborators. Move them first and the NPC rule goes public as a side effect of a
 * file move, and it cannot be taken back. So the line is drawn here, while the rule still only has
 * one caller.
 *
 * <h3>What is on each side of it</h3>
 *
 * <b>Public:</b> troop versus troop resolution, tactics, terrain, morale, commander skill, training,
 * weapons and armour, damage distribution, casualty application, the round loop - and the
 * commander's own combat ARTIFACT, which is a player's possession rather than a designer's rule.
 *
 * <b>Private:</b> what each class of NPC is worth, and the conditions under which it counts at all.
 * That lives behind this interface and is implemented in PbmJudge.
 *
 * <h3>The existence of the bonus is already public, and stays so</h3>
 *
 * {@code PersonagemControl.getDescricaoBonusCombate()} is printed in the deploy header of every
 * turn, so a player already knows an NPC helped. Only the MAGNITUDE and the gating are withheld,
 * and the simulator says out loud that it is withholding them rather than quietly returning a
 * number that is short.
 *
 * <h3>Be honest about how strong this protection is</h3>
 *
 * Weaker than it looks, and the design says so. A deterministic public simulator plus an observable
 * real outcome means the hidden term is recoverable by subtraction: run the simulator on a battle
 * with an NPC in it, read the Judge's published result, and the difference IS the contribution.
 * Because the term is flat and additive, one battle per NPC type is enough. This interface stops
 * the rule being READ; it does not stop it being measured. It is drawn anyway, because
 * "reverse-engineerable with effort" and "published in a public repository" are different things -
 * the same judgement the project already made about shipping private modules as compiled jars.
 */
public interface NpcContribution {

    /**
     * Flat attack added by the NPCs travelling with this army's commander.
     *
     * @param army  the army whose commander's companions are being asked about.
     * @param round the combat round. The Judge's rule gates NPCs out of round 0, so the round is
     *              part of the question rather than something the caller filters beforehand.
     * @param naval true when this is the sea layer, which values some things differently.
     * @return the extra attack, or 0 when this implementation does not model it.
     */
    long npcAttack(IExercito army, int round, boolean naval);

    /**
     * Does this implementation actually model the contribution?
     *
     * Asked so the simulator can DISCLOSE the gap rather than infer it from a zero. Zero is a real
     * answer - an army with no NPCs travelling contributes nothing - and a tool that cannot tell
     * "nobody is there" from "we will not tell you" ends up either crying wolf on every battle or
     * staying silent on the ones that matter.
     */
    boolean isModelled();
}
