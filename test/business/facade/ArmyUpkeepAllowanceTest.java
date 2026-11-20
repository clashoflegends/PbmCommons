package business.facade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import model.Exercito;
import model.Habilidade;
import model.HabilidadeNacao;
import model.Nacao;
import model.Pelotao;
import model.TipoTropa;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArmyUpkeepAllowanceTest {
    private static Pelotao troop(String code, int quantity, int upkeep, boolean fast) {
        TipoTropa type = new TipoTropa(); type.setCodigo(code); type.setUpkeepMoney(upkeep);
        if (fast) {
            Habilidade power = new Habilidade(); power.setCodigo(";TTF;"); type.addHabilidade(power);
        }
        Pelotao platoon = new Pelotao(); platoon.setTipoTropa(type); platoon.setQtd(quantity);
        return platoon;
    }

    private static Exercito army(Nacao nation, Pelotao... platoons) {
        Exercito army = new Exercito(); army.setNacao(nation);
        for (Pelotao platoon : platoons) army.getPelotoes().put(platoon.getCodigo(), platoon);
        return army;
    }

    private static Nacao nation(int allowance) {
        Nacao nation = new Nacao();
        Habilidade power = new Habilidade(); power.setCodigo(";NMF;"); power.setValor(allowance);
        nation.addHabilidade(power); return nation;
    }

    @Test void exampleIsCheapestFirstAcrossMergedSplitAndReversedArmies() {
        Nacao nation = nation(5000);
        Pelotao riders = troop("cpblood3", 4000, 3, true), knights = troop("cpk", 1500, 6, true);
        var facade = new NacaoFacade();
        for (List<Exercito> armies : List.of(List.of(army(nation, riders, knights)),
                List.of(army(nation, riders), army(nation, knights)),
                List.of(army(nation, knights), army(nation, riders)),
                List.of(army(nation, troop("cpblood3", 2000, 3, true), troop("cpk", 750, 6, true)),
                        army(nation, troop("cpblood3", 2000, 3, true), troop("cpk", 750, 6, true))))) {
            int gross = facade.getCustoExercitoNacao(nation, armies);
            int discount = facade.getDescontoExercitoNacao(nation, armies);
            assertEquals(21000, gross); assertEquals(18000, discount); assertEquals(3000, gross - discount);
        }
    }

    @Test void numericVariantsUseActualUpkeepAndNeverRecruitmentCost() {
        Pelotao cheap = troop("zvariant007", 40, 3, true), expensive = troop("avariant12", 40, 6, true);
        cheap.getTipoTropa().setRecruitCostMoney(1000000);
        assertEquals(180, ArmyUpkeepAllowance.discount(List.of(expensive, cheap), 0, 50));
    }

    @Test void allocationIsIndependentOfInputOrderAndDoesNotMutateInputs() {
        List<Pelotao> platoons = new ArrayList<>(List.of(troop("x", 30, 6, true),
                troop("b", 40, 3, true), troop("a", 20, 3, true)));
        for (int rotation = 0; rotation < 3; rotation++) {
            Collections.rotate(platoons, 1);
            var before = List.copyOf(platoons);
            assertEquals(240, ArmyUpkeepAllowance.discount(platoons, 0, 70));
            assertEquals(before, platoons);
            assertEquals(90, platoons.stream().mapToInt(Pelotao::getQtd).sum());
        }
    }

    @Test void ineligibleAndForeignTroopsDoNotConsumeAllowance() {
        Nacao owner = nation(10), foreign = nation(10);
        var ownArmy = army(owner, troop("foot", 1000, 1, false), troop("horse", 20, 6, true));
        assertEquals(60, new NacaoFacade().getDescontoExercitoNacao(owner,
                List.of(army(foreign, troop("foreign", 1000, 1, true)), ownArmy)));
        assertEquals(0, new NacaoFacade().getDescontoExercitoNacao(new Nacao(), List.of(ownArmy)));
    }

    @Test void emptyZeroExactAndOversizedAllowances() {
        var platoons = List.of(troop("horse", 20, 6, true));
        assertEquals(0, ArmyUpkeepAllowance.discount(List.of(), 0, 10));
        assertEquals(0, ArmyUpkeepAllowance.discount(platoons, 0, 0));
        assertEquals(120, ArmyUpkeepAllowance.discount(platoons, 0, 20));
        assertEquals(120, ArmyUpkeepAllowance.discount(platoons, 0, 100));
        assertEquals(0, ArmyUpkeepAllowance.discount(List.of(troop("horse", 0, 6, true)), 0, 20));
    }

    @Test void zeroUpkeepEligibleTroopsStillCountTowardTheHeadcountAllowance() {
        assertEquals(30, ArmyUpkeepAllowance.discount(List.of(troop("paid", 20, 6, true),
                troop("free", 5, 0, true)), 0, 10));
    }

    @Test void legacyBloodriderAllowanceKeepsEligibilityAndPartialPlatoonPrecedence() {
        Pelotao riders = troop("cpblood12", 4000, 3, true), knights = troop("cpk", 1500, 6, true);
        assertEquals(9000, ArmyUpkeepAllowance.discount(List.of(knights, riders), 3000, 0));
        //The original 0053 branch skips this entire Bloodrider platoon after covering 3000.
        //Its uncovered 1000 must not newly consume NMF; the 1500 knights consume NMF instead.
        assertEquals(18000, ArmyUpkeepAllowance.discount(List.of(knights, riders), 3000, 1500));
        Nacao nation = nation(1500);
        HabilidadeNacao blood = new HabilidadeNacao(); blood.setCodigo("0053"); blood.setVlHabilidadeNacao(3000);
        nation.addHabilidadeNacao(blood);
        assertEquals(18000, new NacaoFacade().getDescontoExercitoNacao(nation, List.of(army(nation, riders, knights))));
        //Legacy 0053 requires the Bloodrider code, not the generic fast-troop flag.
        assertEquals(30, ArmyUpkeepAllowance.discount(List.of(troop("cpblood2", 10, 3, false)), 10, 0));
    }

    @Test void legacyOverlappingAllowancesBreakSameCodeTiesIndependentlyOfArmyOrder() {
        Pelotao small = troop("cpblood3", 1, 1, true), large = troop("cpblood3", 2, 1, true);
        Pelotao knights = troop("cpk", 3, 5, true);
        assertEquals(8, ArmyUpkeepAllowance.discount(List.of(small, large, knights), 1, 3));
        assertEquals(8, ArmyUpkeepAllowance.discount(List.of(knights, large, small), 1, 3));
    }
}
