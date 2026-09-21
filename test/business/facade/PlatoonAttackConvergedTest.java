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
 * One platoon-attack formula, shared with the Judge. T-426, converged 2026-09-20 on John's call.
 *
 * <h3>What was wrong</h3>
 *
 * {@code ExercitoControlFacade.getPlatoonAttack} was a SECOND implementation of
 * {@code BattleSimFacade.getPlatoonAttack} and the two had drifted. Worse, the Judge used both:
 * {@code CombatLand} and {@code CombatCity} called its own copy, while {@code CombateTmpbm} and
 * {@code ExercitoControl.getForcaBasicaLand} called this one. So a nation with the {@code ;AAW;}
 * woods bonus got it in the new land engine's combat, and did NOT get it in the legacy engine's
 * combat or in its own reported strength.
 *
 * Checked term by term before merging: quantity, training, weapon, the allied-city doubling and
 * {@code ;PAB;}/{@code ;PABN;} were identical, and {@code getArmyBonusModifier / 100f} is exactly
 * the Judge's {@code (skill + moral + 200f) / 400f}. {@code ;AAW;} was the only rule that differed,
 * and it now lives here.
 *
 * <h3>Why it was safe to do now</h3>
 *
 * No nation in the live game carries {@code ;AAW;}, so no Judge number moved. Leaving it split
 * would have meant the copies drifting further the first time a scenario granted it.
 */
public class PlatoonAttackConvergedTest {

    private static final Terreno FOREST = terreno("F", "Forest");
    private static final Terreno PLAIN = terreno("P", "Plain");

    private static Terreno terreno(String codigo, String nome) {
        final Terreno ret = new Terreno();
        ret.setCodigo(codigo);
        ret.setNome(nome);
        ret.setAncoravel(true);
        return ret;
    }

    private static TipoTropa troopType() {
        final TipoTropa ret = new TipoTropa();
        ret.setCodigo("inf");
        ret.setNome("Infantry");
        final SortedMap<Terreno, Integer> ataque = new TreeMap<>();
        ataque.put(FOREST, 50);
        ataque.put(PLAIN, 50);
        ret.setAtaqueTerreno(ataque);
        return ret;
    }

    private static Pelotao platoon() {
        final Pelotao ret = new Pelotao();
        ret.setTipoTropa(troopType());
        ret.setQtd(900);
        ret.setTreino(50);
        return ret;
    }

    private static Local hex(Terreno terreno) {
        final Local ret = new Local();
        ret.setCodigo("1428");
        ret.setCoordenadas("1428");
        ret.setTerreno(terreno);
        return ret;
    }

    /** @param woodsBonus 0 for a nation without {@code ;AAW;} */
    private static Nacao nacao(int woodsBonus) {
        final Nacao ret = new Nacao();
        ret.setCodigo("n");
        ret.setNome("Nation");
        if (woodsBonus > 0) {
            final Habilidade hab = new Habilidade();
            hab.setCodigo(";AAW;");
            hab.setNome(";AAW;");
            hab.setValor(woodsBonus);
            ret.addHabilidade(hab);
        }
        return ret;
    }

    private static ArmySim army(Nacao nacao, Terreno terreno) {
        final ArmySim ret = new ArmySim("Host", terreno, nacao);
        ret.setCodigo("a1");
        ret.setLocal(hex(terreno));
        final Pelotao one = platoon();
        ret.getPelotoes().put(one.getCodigo(), one);
        return ret;
    }

    private static float attack(Nacao nacao, Terreno terreno) {
        return new BattleSimFacade().getPlatoonAttack(platoon(), army(nacao, terreno),
                hex(terreno), terreno);
    }

    /** The rule that only the Judge had: a woods nation hits harder in forest. */
    @Test
    public void theWoodsBonusAppliesInForest() {
        final float plain = attack(nacao(50), PLAIN);
        final float forest = attack(nacao(50), FOREST);

        assertTrue(forest > plain,
                "a ;AAW; nation must gain in forest: plain=" + plain + " forest=" + forest);
        assertEquals(plain * 1.5f, forest, 0.01f, "and gain exactly the habilidade's percentage");
    }

    /** A nation without the habilidade gains nothing, so the bonus is not applied to everybody. */
    @Test
    public void aNationWithoutTheHabilidadeGainsNothingInForest() {
        assertEquals(attack(nacao(0), PLAIN), attack(nacao(0), FOREST), 0.01f);
    }

    /** And the woods nation gains nothing outside forest. */
    @Test
    public void theWoodsBonusDoesNotApplyOutsideForest() {
        assertEquals(attack(nacao(0), PLAIN), attack(nacao(50), PLAIN), 0.01f);
    }

    /**
     * The TERRAIN parameter drives the bonus, not the hex.
     *
     * That is what makes the simulator's terrain combo work: an {@code ArmySim} carries an editable
     * terreno alongside the real hex, so "what if this were fought in forest" has to be answerable
     * from the parameter. Every live Judge caller passes its own hex, so the two coincide there.
     */
    @Test
    public void theTerrainParameterDecidesRatherThanTheHex() {
        final Nacao woods = nacao(50);
        final ArmySim onPlain = army(woods, PLAIN);
        final BattleSimFacade facade = new BattleSimFacade();

        final float asPlain = facade.getPlatoonAttack(platoon(), onPlain, hex(PLAIN), PLAIN);
        final float asForest = facade.getPlatoonAttack(platoon(), onPlain, hex(PLAIN), FOREST);

        assertTrue(asForest > asPlain,
                "the simulated terrain must decide: " + asPlain + " vs " + asForest);
    }
}
