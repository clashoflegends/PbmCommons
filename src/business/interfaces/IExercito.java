/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.interfaces;

import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Personagem;
import model.Terreno;

/**
 *
 * @author jmoura
 */
public interface IExercito {
    //TODO: refactor to remove a bunch of tasks from ExercitoControl and move them to ExercitoFacade or ExercitoControlFacade 

    public int getMoral();

    public Local getLocal();

    public Terreno getTerreno();

    public Nacao getNacao();

    public SortedMap<String, Pelotao> getPelotoes();

    public int getTatica();

    public int getAttackBonus();

    public int getArmyDefenseBonus();

    public void setArmyDefenseBonus(int bonus);

    public void setCombatDamageClear();

    public boolean isGarrison();

    public boolean isGameHasResourceManagement();

    public void doDisband();

    public void doDisbandWithMsg();

    public void setDisband(boolean disband);

    public boolean isDisband();

    public Personagem getComandanteModel();

    public String getComandanteNome();

    public int getComandantePericia();

    @Override
    public String toString();

    // ---------------------------------------------------------------- T-901
    //
    // THE COMBAT GAP LIST. Everything below is what the engine classes call on an army but could
    // not reach through this interface, which is why CombatBase/CombatArmy/CombatLand are still
    // typed against the Judge's ExercitoControl and cannot move (T-902, T-903).
    //
    // They are DEFAULT methods on purpose, and the default THROWS rather than answering.
    //
    // Three implementors exist - ExercitoControl (the Judge), ArmySim (the simulator) and the
    // plain model Exercito - and only the first has combat behaviour today. Declaring these
    // abstract would have forced the other two to grow bodies in the same commit, which is exactly
    // the big-bang change this sequence exists to avoid; T-904 gives ArmySim its own.
    //
    // The default throws instead of returning an empty list or a zero because a silent plausible
    // default here would be a WRONG BATTLE rather than a missing feature: an empty enemy list means
    // "fights nobody", and nothing downstream could tell that from "this implementor has not been
    // taught yet". The plain model Exercito keeps the throwing default permanently - it is a data
    // record, and asking it to resolve combat is a caller bug worth hearing about.

    /**
     * The platoons as a flat collection.
     *
     * The one member of the gap list with a real default rather than a throw, because this IS its
     * definition everywhere it is implemented - the Judge's own body is the same expression.
     */
    default Collection<Pelotao> getPelotoesList() {
        return getPelotoes().values();
    }

    /** Applies the damage banked by {@link #sumCombateDano} and reports what it killed. */
    default List<String> doCombateDano() {
        throw new UnsupportedOperationException(notTaught("doCombateDano"));
    }

    /** Banks incoming damage for this round, to be applied when the round closes. */
    default void sumCombateDano(long combateDano) {
        throw new UnsupportedOperationException(notTaught("sumCombateDano"));
    }

    /**
     * The armies this one will actually exchange blows with.
     *
     * {@code Collection<? extends IExercito>} rather than {@code Collection<IExercito>} so the
     * Judge's existing {@code Collection<ExercitoControl>} satisfies it unchanged. Widening the
     * Judge's own signature would have been a combat-code edit for no gain.
     */
    default Collection<? extends IExercito> getInimigos() {
        throw new UnsupportedOperationException(notTaught("getInimigos"));
    }

    default void addInimigo(IExercito inimigo) {
        throw new UnsupportedOperationException(notTaught("addInimigo"));
    }

    default void remInimigos() {
        throw new UnsupportedOperationException(notTaught("remInimigos"));
    }

    default boolean isInimigo(IExercito inimigo) {
        throw new UnsupportedOperationException(notTaught("isInimigo"));
    }

    /** Whether survivors of this battle earn experience from it. */
    default void setCombateXp(boolean getXp) {
        throw new UnsupportedOperationException(notTaught("setCombateXp"));
    }

    /** Says WHICH implementor has not been taught, because the stack alone rarely makes it obvious. */
    private String notTaught(String method) {
        return getClass().getName() + " does not implement IExercito." + method
                + " - see T-901/T-904. Not a missing feature: this implementor has no combat"
                + " behaviour, and answering with an empty result would be a wrong battle.";
    }
}
