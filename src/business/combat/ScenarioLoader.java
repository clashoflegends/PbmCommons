package business.combat;

import java.util.Collection;
import model.Exercito;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Partida;
import model.Pelotao;
import model.TipoTropa;

/**
 * Turns a hex into a {@link CombatScenario}: every army standing on it, each one tagged with how
 * much of it the player can actually see.
 *
 * <h3>Why loading needs a class of its own</h3>
 *
 * Reading {@code local.getExercitos()} is one line. Deciding what those armies MEAN is not, and the
 * two facts below are invisible from the client code - both were found by reading the server's
 * export path, and each one silently produces a confident, wrong simulation if missed.
 *
 * <h3>1. A foreign army's composition is usually DESTROYED on export</h3>
 *
 * At visibility level 4, {@code ServerExercitoDao.setVisPlatoonsUnknown} collapses every land
 * platoon into ONE synthetic platoon of troop type {@code none} and every ship into one of type
 * {@code ship}. The head COUNT survives exactly; the composition does not. Those two catalogue
 * entries carry attack and defense of <b>1 on every terrain</b>, so an army loaded as-is simulates
 * as very nearly harmless, and any battle involving it reports a crushing win. That is not
 * imprecision, it is a systematic error in the player's favour, so it is flagged as
 * {@link CombatScenario.Provenance#UNKNOWN_COMPOSITION} rather than passed off as an estimate.
 *
 * Detection is by troop-type CODIGO. It is tempting to look for the {@code ;TTR;} habilidade the
 * two entries carry, but nearly every real troop type carries it too - it is not a placeholder
 * marker. {@code ship} does correctly carry {@code ;TTN;}, so naval detection still works on a
 * placeholder fleet: it will be seen as a fleet, with unknown ships in it.
 *
 * At visibility level 2 the real platoons come across, which is why an outside view is not
 * automatically unknown. The client cannot ask which level it got; the placeholder types ARE the
 * signal.
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

    /** The catalogue entry a foreign army's land platoons collapse into. Attack/defense 1. */
    public static final String TROOP_UNKNOWN_LAND = "none";
    /** The catalogue entry a foreign army's ships collapse into. Attack/defense 1, carries ;TTN;. */
    public static final String TROOP_UNKNOWN_SHIP = "ship";

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
     * How much of this army the player really knows.
     *
     * Order matters. The placeholder check comes FIRST, because it is a statement about the data in
     * hand rather than about whose army it is: if the platoons are synthetic then the numbers are
     * unusable no matter who owns it. Ownership only decides between an exact read and an outside
     * view of real platoons, which may still be a turn stale.
     */
    public CombatScenario.Provenance provenanceOf(Exercito exercito, Jogador observer,
            Collection<Nacao> mergedNacoes) {
        if (hasUnknownComposition(exercito)) {
            return CombatScenario.Provenance.UNKNOWN_COMPOSITION;
        }
        return isFullyVisible(exercito.getNacao(), observer, mergedNacoes)
                ? CombatScenario.Provenance.EXACT
                : CombatScenario.Provenance.ESTIMATED;
    }

    /** Does this army hold at least one platoon the server replaced with a placeholder? */
    public boolean hasUnknownComposition(Exercito exercito) {
        if (exercito == null) {
            return false;
        }
        for (Pelotao pelotao : exercito.getPelotoes().values()) {
            if (isUnknownTroopType(pelotao == null ? null : pelotao.getTipoTropa())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Is this the catalogue's stand-in for a troop type the player may not see?
     *
     * By codigo, never by habilidade. See the class note: {@code ;TTR;} is on almost every real
     * troop type and identifies nothing.
     */
    public static boolean isUnknownTroopType(TipoTropa tipoTropa) {
        if (tipoTropa == null) {
            return false;
        }
        final String codigo = tipoTropa.getCodigo();
        return TROOP_UNKNOWN_LAND.equals(codigo) || TROOP_UNKNOWN_SHIP.equals(codigo);
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
