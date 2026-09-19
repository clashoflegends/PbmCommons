package business.combat;

import model.Exercito;
import model.Local;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The BattleSim owns its platoons, and shares its troop catalogue.
 *
 * Pinned because the failure mode was silent and reached the player. Both ArmySim copy constructors
 * used to do {@code platoons.putAll(source)}, which copies the MAP but shares the Pelotao instances,
 * so retyping a platoon's quantity or troop type inside the simulator edited the real army loaded
 * from the EGF, everywhere in the Counselor, for the rest of the session. Nothing warned, and the
 * simulator is exactly the screen where a player expects to be able to type anything he likes.
 *
 * The two halves of the contract pull in opposite directions and both are asserted here:
 *
 *   Pelotao  -> NOT shared. It holds the four values the simulator lets the player edit.
 *   TipoTropa -> SHARED on purpose. It is the scenario's read-only troop catalogue; the simulator
 *                repoints a platoon at a different entry but never edits an entry, and copying it
 *                would break identity comparisons against the catalogue.
 *
 * A future change that makes the catalogue editable (magic items, NPCs and powers as catalog) moves
 * TipoTropa across that line and this test grows a case. Until then, a failure here means the
 * simulator is writing into the loaded world again.
 */
public class ArmySimOwnershipTest {

    private static TipoTropa troopType(String codigo, String nome) {
        final TipoTropa ret = new TipoTropa();
        ret.setCodigo(codigo);
        ret.setNome(nome);
        return ret;
    }

    private static Pelotao platoon(TipoTropa tipo, int qtd, int treino, int ataque, int defesa) {
        final Pelotao ret = new Pelotao();
        ret.setTipoTropa(tipo);
        ret.setQtd(qtd);
        ret.setTreino(treino);
        ret.setModAtaque(ataque);
        ret.setModDefesa(defesa);
        return ret;
    }

    /**
     * ArmySim(Exercito) reads {@code getLocal().getTerreno()} without a guard, so the fixture needs a
     * placed army. A real army loaded from an EGF always has one; the blank-army constructor takes a
     * Terreno directly and never comes through here.
     */
    private static Exercito armyWithOnePlatoon(Pelotao pelotao) {
        final Terreno terreno = new Terreno();
        terreno.setCodigo("F");
        terreno.setNome("Forest");
        final Local local = new Local();
        local.setTerreno(terreno);
        local.setCoordenadas("1428");

        final Exercito ret = new Exercito();
        // codigo first: setLocal() registers the army in the Local, keyed on it.
        ret.setCodigo("a1");
        ret.setNome("Test Army");
        ret.setLocal(local);
        ret.getPelotoes().put(pelotao.getCodigo(), pelotao);
        return ret;
    }

    @Test
    public void editingASimulatedPlatoonDoesNotTouchTheLoadedArmy() {
        final TipoTropa heavyHorse = troopType("hc", "Heavy Horse");
        final Pelotao real = platoon(heavyHorse, 400, 70, 60, 80);
        final Exercito loaded = armyWithOnePlatoon(real);

        final ArmySim sim = new ArmySim(loaded);
        final Pelotao simulated = sim.getPelotoes().get(heavyHorse.getCodigo());

        assertNotSame(real, simulated, "the simulator must not share Pelotao objects with the EGF");

        simulated.setQtd(999);
        simulated.setTreino(1);
        simulated.setModAtaque(2);
        simulated.setModDefesa(3);

        assertEquals(400, real.getQtd(), "quantity leaked back into the loaded army");
        assertEquals(70, real.getTreino(), "training leaked back into the loaded army");
        assertEquals(60, real.getModAtaque(), "weapon leaked back into the loaded army");
        assertEquals(80, real.getModDefesa(), "armour leaked back into the loaded army");
    }

    @Test
    public void repointingASimulatedPlatoonDoesNotTouchTheLoadedArmy() {
        final TipoTropa heavyHorse = troopType("hc", "Heavy Horse");
        final TipoTropa pikemen = troopType("pk", "Pikemen");
        final Pelotao real = platoon(heavyHorse, 400, 70, 60, 80);
        final Exercito loaded = armyWithOnePlatoon(real);

        final ArmySim sim = new ArmySim(loaded);
        sim.getPelotoes().get(heavyHorse.getCodigo()).setTipoTropa(pikemen);

        assertSame(heavyHorse, real.getTipoTropa(), "troop type leaked back into the loaded army");
    }

    @Test
    public void theTroopCatalogueIsSharedOnPurpose() {
        final TipoTropa heavyHorse = troopType("hc", "Heavy Horse");
        final Exercito loaded = armyWithOnePlatoon(platoon(heavyHorse, 400, 70, 60, 80));

        final ArmySim sim = new ArmySim(loaded);

        assertSame(heavyHorse, sim.getPelotoes().get(heavyHorse.getCodigo()).getTipoTropa(),
                "TipoTropa is the read-only scenario catalogue and must stay shared");
    }

    @Test
    public void cloningASimulatedArmyDetachesItFromTheOriginal() {
        final TipoTropa heavyHorse = troopType("hc", "Heavy Horse");
        final Exercito loaded = armyWithOnePlatoon(platoon(heavyHorse, 400, 70, 60, 80));

        final ArmySim first = new ArmySim(loaded);
        final ArmySim second = new ArmySim(first);

        final Pelotao firstPlatoon = first.getPelotoes().get(heavyHorse.getCodigo());
        final Pelotao secondPlatoon = second.getPelotoes().get(heavyHorse.getCodigo());

        assertNotSame(firstPlatoon, secondPlatoon, "a cloned army must not share platoons with its source");

        secondPlatoon.setQtd(1);
        assertEquals(400, firstPlatoon.getQtd(), "the clone wrote through to the army it was cloned from");
    }

    @Test
    public void addingAndRemovingPlatoonsDoesNotResizeTheLoadedArmy() {
        final TipoTropa heavyHorse = troopType("hc", "Heavy Horse");
        final Exercito loaded = armyWithOnePlatoon(platoon(heavyHorse, 400, 70, 60, 80));

        final ArmySim sim = new ArmySim(loaded);
        sim.getPelotoes().remove(heavyHorse.getCodigo());
        sim.getPelotoes().put("pk", platoon(troopType("pk", "Pikemen"), 300, 40, 50, 40));

        assertEquals(1, loaded.getPelotoes().size(), "the loaded army lost or gained a platoon");
        assertSame(heavyHorse, loaded.getPelotoes().get(heavyHorse.getCodigo()).getTipoTropa());
    }
}
