package business.facade;

import business.combat.ArmySim;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Habilidade;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An army with NO NATION is a real state, and the shared combat maths must survive it. T-440.
 *
 * <h3>Why this is worth pinning</h3>
 *
 * An ownerless actor is not a broken one. {@code business.combat.HostilityDeriver} handles "an army
 * whose owner is unknown" by name, the server withholds a nation from an army the player cannot
 * identify, and a blank army the player adds in the simulator starts without one.
 *
 * Both methods below used to mishandle it, in the two different ways this codebase gets things
 * wrong:
 *
 * <ul>
 *   <li>{@code getPlatoonDefense} dereferenced {@code getNacao()} unguarded and THREW. Loud, and it
 *       would have taken the BattleSim window down the moment T-427 put attack and defence on
 *       screen.</li>
 *   <li>{@code getPlatoonAttack} already caught the NPE in an outer handler and answered <b>0</b>.
 *       Silent, and worse: a real number, indistinguishable from a genuinely harmless army. The
 *       catch is there for a missing terrain entry and is right for that; it was never meant to
 *       absorb a nationless army.</li>
 * </ul>
 *
 * <h3>Scope of the fix</h3>
 *
 * {@code BattleSimFacade} is SHARED and the Judge calls it in several places, so the guard is
 * scoped to the two nation-bonus conditions in each method. Where the nation is non-null - every
 * army the Judge owns - the arithmetic is unchanged, which is what
 * {@link #aNationDoesNotChangeTheBaselineArithmetic} asserts.
 */
public class BattleSimFacadeNationlessTest {

    private static final Terreno PLAIN = terreno();

    private static Terreno terreno() {
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

    /** @param nacao null for an army whose owner the player cannot see */
    private static ArmySim army(Nacao nacao) {
        final ArmySim ret = new ArmySim("Host", PLAIN, nacao);
        ret.setCodigo("a1");
        ret.setLocal(hex());
        final Pelotao one = platoon();
        ret.getPelotoes().put(one.getCodigo(), one);
        return ret;
    }

    private static Nacao nacao(String codigo) {
        final Nacao ret = new Nacao();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        return ret;
    }

    /** Defence: used to throw. */
    @Test
    public void defenceIsComputedForAnArmyWithNoNation() {
        final float defence = new BattleSimFacade().getPlatoonDefense(army(null), platoon());

        assertTrue(defence > 0f, "an ownerless army still has troops that defend: " + defence);
    }

    /**
     * Attack: used to answer a SILENT ZERO, swallowed by the outer catch.
     *
     * The dangerous half. An exception gets noticed; a plausible number does not.
     */
    @Test
    public void attackIsComputedForAnArmyWithNoNationRatherThanASilentZero() {
        final float attack = new BattleSimFacade().getPlatoonAttack(platoon(), army(null), hex());

        assertTrue(attack > 0f,
                "an ownerless army still has troops that attack, and 0 is a lie: " + attack);
    }

    /**
     * The safety property the whole fix rests on: a plain nation changes NOTHING.
     *
     * The guard is scoped to the two nation-bonus conditions, so an army whose nation carries none
     * of those habilidades must compute exactly what a nationless one does. If this ever fails, the
     * guard has started changing the Judge's arithmetic.
     */
    @Test
    public void aNationDoesNotChangeTheBaselineArithmetic() {
        final BattleSimFacade facade = new BattleSimFacade();
        final Nacao plain = nacao("n");

        assertEquals(facade.getPlatoonDefense(army(null), platoon()),
                facade.getPlatoonDefense(army(plain), platoon()), 0.0001f);
        assertEquals(facade.getPlatoonAttack(platoon(), army(null), hex()),
                facade.getPlatoonAttack(platoon(), army(plain), hex()), 0.0001f);
    }

    /**
     * And a nation that DOES carry the bonus still gets it. The guard skips, it does not disable.
     *
     * {@code ;PDB;} is the capital-distance defence bonus; {@code getDistanciaToCapital} answers
     * 9999 for a nation with no capital, so the value has to clear that for the bonus to apply.
     */
    @Test
    public void aNationWithTheBonusStillReceivesIt() {
        final Nacao bonused = nacao("b");
        final Habilidade pdb = new Habilidade();
        pdb.setCodigo(";PDB;");
        pdb.setNome(";PDB;");
        pdb.setValor(99999);
        bonused.addHabilidade(pdb);

        final BattleSimFacade facade = new BattleSimFacade();

        assertTrue(facade.getPlatoonDefense(army(bonused), platoon())
                > facade.getPlatoonDefense(army(null), platoon()),
                "the ;PDB; bonus must still apply to a nation that has it");
    }
}
