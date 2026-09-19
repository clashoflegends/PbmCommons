package business.combat;

import java.util.Collection;
import model.Exercito;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Partida;

/**
 * Turns a hex into a {@link CombatScenario}: every army standing on it, each one tagged with how
 * much of it the player can actually see.
 *
 * <h3>Why loading needs a class of its own</h3>
 *
 * Reading {@code local.getExercitos()} is one line. Deciding what those armies MEAN is not, and the
 * two facts below are invisible from the client code.
 *
 * <h3>1. An unscouted foreign army arrives with placeholder platoons, and that is BY DESIGN</h3>
 *
 * At visibility level 4, {@code ServerExercitoDao.setVisPlatoonsUnknown} collapses every land
 * platoon into ONE synthetic platoon of troop type {@code none} and every ship into one of type
 * {@code ship}. The head COUNT survives exactly; the composition does not, and those two catalogue
 * entries carry attack and defense of 1 on every terrain. At visibility level 2 - a scouted or
 * reconned army, which is generally the one that matters - the real platoons come across.
 *
 * <b>The loader does not special-case any of this, and must not start.</b> Not knowing what is in
 * an enemy stack IS the game: that is what scouting orders are for, and what a player who has not
 * scouted is expected to guess at. The placeholder pair is the intelligence the player actually
 * holds, so it is what the simulator runs on, loaded like any other outside view. A player who
 * wants a better answer scouts the army or types what he thinks is in it, and editing retags those
 * platoons as his own.
 *
 * This note exists to stop a future reader "fixing" it. Two plausible-looking features were built
 * on the opposite assumption and removed: a distinct unknown-composition provenance, and a
 * cannot-tell state in {@link LayerParticipation} for a placeholder fleet's landing.
 *
 * <h3>2. Combat intent does not ride the EGF at all</h3>
 *
 * {@code combateNivel} and {@code combateNacaoNumero} live on the Judge's {@code ExercitoControl},
 * not on {@code model.Exercito}, so nothing here can recover what an army was actually ordered to
 * do - not even the player's own. Every army therefore loads at the default
 * {@link CombatLevel#ATTACK_ARMY} and the player states his assumptions. That is a property of the
 * game (intent is exactly the sort of thing a player spends orders to learn), not a gap to fill in
 * with a guess.
 *
 * <h3>What this class deliberately does NOT do</h3>
 *
 * It does not work out which allies' EGFs were merged. That test is Counselor knowledge - an
 * autoloaded ally's actors carry loaded orders, and fogged actors do not - so the caller passes the
 * answer in. Re-deriving it here would duplicate {@code WorldControler} inside a library that
 * cannot see it.
 */
public class ScenarioLoader {

    /**
     * Loads every army standing on a hex, garrisons included.
     *
     * A garrison is an ordinary {@code Exercito} with no commander, parked at the Local, so it
     * arrives through the same door. It has to: leaving it out understates a city assault, which is
     * the direction a simulator must never be wrong in.
     *
     * @param partida      the game, for its type flags
     * @param local        the hex
     * @param observer     the player at the keyboard
     * @param mergedNacoes nations whose own EGF was merged in, decided by the caller
     */
    public CombatScenario load(Partida partida, Local local, Jogador observer,
            Collection<Nacao> mergedNacoes) {
        final CombatScenario ret = new CombatScenario(partida, local);
        ret.setObserver(observer);
        if (mergedNacoes != null) {
            for (Nacao nacao : mergedNacoes) {
                ret.addMergedNacao(nacao);
            }
        }
        if (local == null) {
            return ret;
        }
        for (Exercito exercito : local.getExercitos().values()) {
            if (exercito == null) {
                continue;
            }
            ret.addArmy(new ArmySim(exercito), provenanceOf(exercito, observer, mergedNacoes));
        }
        return ret;
    }

    /**
     * How much of this army the player really knows: what his own EGF carried, or what he can see
     * from outside.
     *
     * Two values, not three. Whether an outside view arrived as real platoons or as the placeholder
     * pair is not a distinction the loader draws - see the class note.
     */
    public CombatScenario.Provenance provenanceOf(Exercito exercito, Jogador observer,
            Collection<Nacao> mergedNacoes) {
        return isFullyVisible(exercito.getNacao(), observer, mergedNacoes)
                ? CombatScenario.Provenance.EXACT
                : CombatScenario.Provenance.ESTIMATED;
    }

    /**
     * Was this nation's own EGF the source of what we hold about it?
     *
     * {@code getOwner()} identifies the observer's OWN nations and nothing else. The server does set
     * an owner on an ally's nation in a team-locked game, but it sets the ally's own owner, so
     * comparing against the observer stays the right test. Compared by identity rather than
     * {@code Jogador.isNacao}, which leaks once allied EGFs merge.
     */
    private boolean isFullyVisible(Nacao nacao, Jogador observer, Collection<Nacao> mergedNacoes) {
        if (nacao == null) {
            return false;
        }
        if (observer != null && nacao.getOwner() == observer) {
            return true;
        }
        if (mergedNacoes == null) {
            return false;
        }
        for (Nacao merged : mergedNacoes) {
            if (merged == nacao) {
                return true;
            }
        }
        return false;
    }
}
