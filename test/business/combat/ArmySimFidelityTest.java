package business.combat;

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
 * An ArmySim must describe the army it says it describes.
 *
 * Three defects found by review on 2026-09-19, all of the same shape: the object reported something
 * plausible that nobody had put there. None of them threw, none of them logged, and the simulator is
 * the one screen where a wrong number looks exactly like a right one.
 *
 *   Clone army dropped four fields, so a clone reverted to the class defaults.
 *   Every garrison arrived with a commander of skill 10, because the read threw and the catch only
 *   set the name.
 *   getTipoTropa() cached its answer and never rebuilt it, on a class whose entire purpose is to be
 *   edited.
 */
public class ArmySimFidelityTest {

    private static TipoTropa troopType(String codigo) {
        final TipoTropa ret = new TipoTropa();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        return ret;
    }

    private static Pelotao platoon(TipoTropa tipo, int qtd) {
        final Pelotao ret = new Pelotao();
        ret.setTipoTropa(tipo);
        ret.setQtd(qtd);
        return ret;
    }

    private static Local hex() {
        final Terreno terreno = new Terreno();
        terreno.setCodigo("P");
        terreno.setNome("Plain");
        final Local ret = new Local();
        ret.setCodigo("1428");
        ret.setCoordenadas("1428");
        ret.setTerreno(terreno);
        return ret;
    }

    /** A commanded army, as an EGF supplies one. */
    private static Exercito commanded(int skill) {
        final Exercito ret = new Exercito();
        ret.setCodigo("a1");
        ret.setNome("Host");
        ret.setLocal(hex());
        final Personagem comandante = new Personagem();
        comandante.setNome("Ser Barristan");
        comandante.setPericiaComandante(skill);
        ret.setComandante(comandante);
        ret.getPelotoes().put("inf", platoon(troopType("inf"), 900));
        return ret;
    }

    /** A garrison: an army with NO commander, which is the whole of the definition. */
    private static Exercito garrison() {
        final Exercito ret = new Exercito();
        ret.setCodigo("g1");
        ret.setNome("Garrison");
        ret.setLocal(hex());
        ret.getPelotoes().put("inf", platoon(troopType("inf"), 300));
        return ret;
    }

    /**
     * A garrison has no commander, so its skill is 0 - not the field initializer's 10.
     *
     * The 10 was not cosmetic: {@code BattleSimFacade} folds the commander skill into a quarter of
     * the army's combat value, so every garrison in the simulator fought harder than it should, and
     * {@code isGarrison()} - which IS {@code comandante <= 0} - answered false for the one kind of
     * army it exists to identify.
     */
    @Test
    void aGarrisonLoadsWithNoCommanderSkillAndKnowsItIsAGarrison() {
        final ArmySim sim = new ArmySim(garrison());

        assertEquals(0, sim.getComandantePericia(), "a garrison has no commander to have a skill");
        assertTrue(sim.isGarrison(), "and must still answer isGarrison()");
        assertEquals("", sim.getComandanteNome(), "no commander, no commander name");
    }

    /** The commanded case, so the fix cannot be "return 0 always". */
    @Test
    void aCommandedArmyKeepsItsCommander() {
        final ArmySim sim = new ArmySim(commanded(42));

        assertEquals(42, sim.getComandantePericia());
        assertFalse(sim.isGarrison());
        assertEquals("Ser Barristan", sim.getComandanteNome());
        assertEquals("Ser Barristan", sim.getNome());
    }

    /**
     * Clone army means clone. The four fields below were silently reset to the class defaults.
     *
     * The worst of them is the combat level: an army the player had set to "Defend only" came back
     * as ATTACK_ARMY, which is not a display glitch - it is the field that decides whether the army
     * initiates combat and which layers it enters.
     */
    @Test
    void cloningAnArmyCarriesTheFieldsThePlayerSet() {
        final Nacao target = new Nacao();
        target.setNome("Lannister");
        final ArmySim original = new ArmySim(commanded(30));
        original.setCombatLevel(CombatLevel.DEFEND_ONLY);
        original.setTargetNacao(target);
        original.setBonusAttack(15);
        original.setBonusDefense(-20);

        final ArmySim clone = new ArmySim(original);

        assertEquals(CombatLevel.DEFEND_ONLY, clone.getCombatLevel(),
                "a clone of a defending army that attacks is not a clone");
        assertSame(target, clone.getTargetNacao());
        assertEquals(15, clone.getAttackBonus());
        assertEquals(-20, clone.getArmyDefenseBonus());
        assertEquals(30, clone.getComandantePericia());
    }

    /** A cloned garrison is still a garrison, and does not acquire a commander on the way. */
    @Test
    void cloningAGarrisonKeepsItAGarrison() {
        final ArmySim clone = new ArmySim(new ArmySim(garrison()));

        assertEquals(0, clone.getComandantePericia());
        assertTrue(clone.isGarrison());
    }

    /**
     * getTipoTropa() answers for the army as it is NOW.
     *
     * Nothing calls it yet, which is exactly why it is pinned now: it caches on first read, and the
     * first caller would have been the engine.
     */
    @Test
    void theTroopTypeListFollowsPlatoonEdits() {
        final ArmySim sim = new ArmySim(commanded(30));
        assertEquals(1, sim.getTipoTropa().size());

        final TipoTropa archers = troopType("arc");
        sim.getPelotoes().put("arc", platoon(archers, 200));

        assertEquals(2, sim.getTipoTropa().size(), "a platoon was added, so a type was added");
        assertTrue(sim.getTipoTropa().contains(archers));

        sim.getPelotoes().clear();
        assertTrue(sim.getTipoTropa().isEmpty(), "and an emptied army holds no troop types");
    }
}
