package business.facade;

import business.combat.ArmySim;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An army with no nation must FAIL LOUDLY in the shared combat maths. It is not a guarded case.
 *
 * <h3>Why this test asserts a crash</h3>
 *
 * John, 2026-09-20: "I prefer to keep the NPE unguarded on the Judge. Better to stop the game from
 * making a mistake than allowing it to continue with an error. Loud failures are acceptable in this
 * cases that it shouldn't happen by design and intention... plus the Judge can see everything, so
 * it is a fatal flaw if it happens."
 *
 * That is decisive, and it is the standing rule of this project: data integrity beats avoiding an
 * NPE, and parking a game is the CORRECT outcome when the data is wrong. Server-side the Judge holds
 * the whole world, so an army with no nation is not a visibility gap - it is corruption, and a
 * simulation that carries on past it produces a turn result nobody can trust.
 *
 * A guard was briefly added here and has been reverted. This test exists so the next person who
 * finds the unguarded dereference reads the reasoning before "fixing" it.
 *
 * <h3>The client's side of the bargain</h3>
 *
 * The Counselor does NOT rely on this throwing or not throwing: {@code ScenarioLoader} forces a
 * nation onto every army it loads and the blank-army path falls back to the same stand-in, so a
 * nationless army cannot reach these methods from the client at all. Supplying the input is the
 * client's job; failing loudly on bad input is the facade's.
 */
public class BattleSimFacadeNationlessTest {

    private static final Terreno PLAIN = plain();

    private static Terreno plain() {
        final Terreno ret = new Terreno();
        ret.setCodigo("P");
        ret.setNome("Plain");
        ret.setAncoravel(true);
        return ret;
    }

    private static SortedMap<Terreno, Integer> byTerrain(int valor) {
        final SortedMap<Terreno, Integer> ret = new TreeMap<>();
        ret.put(PLAIN, valor);
        return ret;
    }

    private static TipoTropa troopType() {
        final TipoTropa ret = new TipoTropa();
        ret.setCodigo("inf");
        ret.setNome("Infantry");
        ret.setAtaqueTerreno(byTerrain(50));
        ret.setDefesaTerreno(byTerrain(40));
        ret.setMovimentoTerreno(byTerrain(5));
        return ret;
    }

    private static Pelotao platoon() {
        final Pelotao ret = new Pelotao();
        ret.setTipoTropa(troopType());
        ret.setQtd(900);
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

    private static ArmySim army(Nacao nacao) {
        final ArmySim ret = new ArmySim("Host", PLAIN, nacao);
        ret.setCodigo("a1");
        ret.setLocal(hex());
        final Pelotao one = platoon();
        ret.getPelotoes().put(one.getCodigo(), one);
        return ret;
    }

    private static Nacao nacao() {
        final Nacao ret = new Nacao();
        ret.setCodigo("n");
        ret.setNome("Nation");
        return ret;
    }

    /**
     * Defence THROWS on a nationless army, by design. Do not guard this.
     *
     * {@code getPlatoonDefense} dereferences {@code army.getNacao()} for the {@code ;PDB;}
     * capital-distance bonus. The exception is the feature: it stops a turn that is running on
     * corrupt data rather than quietly producing a number from it.
     */
    @Test
    public void defenceThrowsOnANationlessArmyAndThatIsIntended() {
        assertThrows(NullPointerException.class,
                () -> new BattleSimFacade().getPlatoonDefense(army(null), platoon()),
                "a nationless army is corruption; failing loudly is the correct outcome");
    }

    /**
     * Attack does NOT throw - it answers 0 - and that is the half worth worrying about.
     *
     * {@code getTroopAttack} wraps its body in a catch that answers {@code 0f}, documented for a
     * missing terrain entry ("nao tem a tropa, retorna forca 0"). It absorbs this fatal flaw too,
     * so the same corrupt army that correctly halts the defence calculation slips through the
     * attack one as a harmless-looking zero.
     *
     * By the very principle that keeps the defence unguarded, this is the WRONG shape: it is the
     * silent wrong answer rather than the loud stop. Pinned as the current behaviour so that
     * changing it is a deliberate act, and flagged for John as T-442 rather than fixed here -
     * narrowing that catch is a change to shared code with the Judge on the other end of it.
     */
    @Test
    public void attackSwallowsTheSameFlawAndAnswersZero() {
        final float attack = new BattleSimFacade().getPlatoonAttack(platoon(), army(null), hex());

        assertEquals(0f, attack, 0.0001f,
                "documented, not endorsed - see T-442 and this method's javadoc");
    }

    /** A real army computes normally, so neither assertion above is passing by accident. */
    @Test
    public void anArmyWithANationComputesNormally() {
        final BattleSimFacade facade = new BattleSimFacade();

        assertTrue(facade.getPlatoonDefense(army(nacao()), platoon()) > 0f);
        assertTrue(facade.getPlatoonAttack(platoon(), army(nacao()), hex()) > 0f);
    }
}
