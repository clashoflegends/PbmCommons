package business.combat;

import business.facade.ExercitoFacade;
import model.Exercito;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Personagem;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the Judge's engine will demand of an {@link ArmySim}, pinned before the engine arrives.
 *
 * A survey of {@code CombatLand} / {@code CombatBase} / {@code CombatArmy} found four ways this
 * class would have failed against the real engine, and the reason they are worth a test of their own
 * is that NONE of them is visible to the compiler or to a search for calls on the army:
 *
 * <ul>
 *   <li>three {@link business.interfaces.IExercito} methods were declared and threw, so every
 *       compile-time check said the contract was met;</li>
 *   <li>{@code compareTo} was inherited from {@code BaseModel} and compared CODIGO, while the engine
 *       sorts armies by STRENGTH in two places it never names;</li>
 *   <li>{@code getComandanteModel} returned null, and its only consumer swallows the NPE and answers
 *       "not a hero" - a dropped combat bonus with no warning.</li>
 * </ul>
 */
public class ArmySimEngineContractTest {

    private static TipoTropa troopType(String codigo, int ataque) {
        final TipoTropa ret = new TipoTropa();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        final java.util.SortedMap<Terreno, Integer> ataques = new java.util.TreeMap<>();
        ataques.put(PLAIN, ataque);
        ret.setAtaqueTerreno(ataques);
        return ret;
    }

    private static final Terreno PLAIN = plain();

    private static Terreno plain() {
        final Terreno ret = new Terreno();
        ret.setCodigo("P");
        ret.setNome("Plain");
        ret.setAncoravel(true);
        return ret;
    }

    private static Pelotao platoon(TipoTropa tipo, int qtd) {
        final Pelotao ret = new Pelotao();
        ret.setTipoTropa(tipo);
        ret.setQtd(qtd);
        ret.setTreino(50);
        return ret;
    }

    private static Local hex() {
        final Local ret = new Local();
        ret.setCodigo("1428");
        ret.setCoordenadas("1428");
        ret.setTerreno(PLAIN);
        return ret;
    }

    private static Nacao nacao() {
        final Nacao ret = new Nacao();
        ret.setCodigo("n");
        ret.setNome("Nation");
        return ret;
    }

    private static ArmySim army(String codigo, int qtd) {
        final ArmySim ret = new ArmySim(codigo, PLAIN, nacao());
        ret.setCodigo(codigo);
        ret.setLocal(hex());
        final Pelotao one = platoon(troopType("inf", 50), qtd);
        ret.getPelotoes().put(one.getCodigo(), one);
        return ret;
    }

    // ------------------------------------------------------------------ the three stubs

    /**
     * The magic-defence exchange writes this five times per land combat, and it threw.
     *
     * Declared on IExercito, so nothing at compile time could tell.
     */
    @Test
    public void theDefenceBonusCanBeWrittenByTheEngine() {
        final ArmySim one = army("a", 900);

        one.setArmyDefenseBonus(35);

        assertEquals(35, one.getArmyDefenseBonus());
    }

    /**
     * Killing the last troop marks the army, and does not throw.
     *
     * The path is {@code ExercitoFacade.subTropaQt} -> {@code setDisband(true)} ->
     * {@code isPrecisaDebandar} -> {@code doDisbandWithMsg()}, and both writers used to throw.
     */
    @Test
    public void wipingOutAnArmyDisbandsItThroughTheSharedFacade() {
        final ArmySim one = army("a", 100);
        final TipoTropa inf = one.getPelotoes().get("inf").getTipoTropa();

        new ExercitoFacade().subTropaQt(one, inf, 100);

        assertTrue(one.isDisband(), "the last troop died");
        assertTrue(one.getPelotoes().isEmpty(), "and nothing is left to keep fighting with");
    }

    /**
     * An army with no platoons is NOT disbanded. It is unscouted.
     *
     * {@code isDisband()} used to answer {@code getPelotoes().isEmpty()}, and the server sends an
     * unscouted enemy with a size band and no platoons at all - so an army nobody had scouted
     * reported itself destroyed before a blow was struck. Same mistake as the old
     * DESTROYED_EARLIER reason, one layer down.
     */
    @Test
    public void anUnscoutedArmyWithNoPlatoonsIsNotDisbanded() {
        final ArmySim unscouted = new ArmySim("enemy", PLAIN, nacao());
        unscouted.setCodigo("enemy");

        assertTrue(unscouted.getPelotoes().isEmpty());
        assertFalse(unscouted.isDisband(), "nothing has been destroyed - nothing is known");
    }

    // ------------------------------------------------------------------ ordering

    /**
     * Armies order by STRENGTH, as {@code ExercitoControl.compareTo} does - not by codigo.
     *
     * The engine sorts by this in {@code getExercitosSorted} and uses the army as a TreeMap key for
     * the casualty snapshot. Inherited from {@code BaseModel} it compared codigo strings, so a
     * battle would have resolved in alphabetical order, silently.
     */
    @Test
    public void armiesOrderByStrengthNotByCodigo() {
        final ArmySim weakButFirstAlphabetically = army("aaa", 10);
        final ArmySim strongButLastAlphabetically = army("zzz", 5000);

        assertTrue(weakButFirstAlphabetically.compareTo(strongButLastAlphabetically) < 0,
                "the small army is weaker, whatever its codigo sorts to");
        assertTrue(strongButLastAlphabetically.compareTo(weakButFirstAlphabetically) > 0);
    }

    /** A blank army has no hex and no codigo, and must not take a sort down with it. */
    @Test
    public void aBlankArmyIsSortableAndDoesNotThrow() {
        final ArmySim blank = new ArmySim("new army", PLAIN, nacao());

        assertEquals(0, blank.compareTo(new ArmySim("other", PLAIN, nacao())),
                "two armies with nothing in them are equally weak, not an NPE");
    }

    // ------------------------------------------------------------------ the commander

    /**
     * The hero defence bonus reached the simulator again.
     *
     * {@code getComandanteModel()} returned null with a FIXME, and its only consumer -
     * {@code ExercitoFacade.isHero} - reads {@code .isHero()} inside a catch that answers false. So
     * {@code BattleSimFacade.getPlatoonDefense} silently dropped the {@code ;TAH;} bonus from every
     * army. Nothing threw and nothing logged; the number was simply lower than the Judge's.
     */
    @Test
    public void aHeroCommanderIsVisibleToTheSharedFacades() {
        final Personagem hero = new Personagem();
        hero.setCodigo("p1");
        hero.setNome("Ser Barristan");
        hero.setPericiaComandante(60);
        // isHero() is getOrdensExtraQt() > 0 - a count, not a habilidade. Asserted the other way
        // round first and the test passed while proving nothing, because both sides answered false.
        hero.setOrdensExtraQt(2);

        final Exercito loaded = new Exercito();
        loaded.setCodigo("a1");
        loaded.setNome("Host");
        loaded.setLocal(hex());
        loaded.setComandante(hero);
        final Pelotao one = platoon(troopType("inf", 50), 900);
        loaded.getPelotoes().put(one.getCodigo(), one);

        final ArmySim sim = new ArmySim(loaded);

        assertSame(hero, sim.getComandanteModel(), "BORROWED, and actually there");
        assertTrue(new ExercitoFacade().isHero(loaded), "the fixture really is a hero");
        assertTrue(new ExercitoFacade().isHero(sim),
                "and the simulator must say so too, or the ;TAH; defence bonus is dropped");
    }

    /** A garrison has no commander, so there is no model to borrow, and that must not throw. */
    @Test
    public void aGarrisonHasNoCommanderModel() {
        final Exercito garrison = new Exercito();
        garrison.setCodigo("g1");
        garrison.setNome("Garrison");
        garrison.setLocal(hex());

        final ArmySim sim = new ArmySim(garrison);

        assertEquals(null, sim.getComandanteModel());
        assertFalse(new ExercitoFacade().isHero(sim));
        assertTrue(sim.isGarrison());
    }

    /** Cloning carries the commander, so a clone is as heroic as the thing it copies. */
    @Test
    public void cloningCarriesTheCommanderModel() {
        final Personagem hero = new Personagem();
        hero.setCodigo("p1");
        hero.setNome("Ser Barristan");
        final Exercito loaded = new Exercito();
        loaded.setCodigo("a1");
        loaded.setNome("Host");
        loaded.setLocal(hex());
        loaded.setComandante(hero);

        assertSame(hero, new ArmySim(new ArmySim(loaded)).getComandanteModel());
    }
}
