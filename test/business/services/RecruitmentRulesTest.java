package business.services;

import baseLib.BaseModel;
import model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecruitmentRulesTest {
    static void ability(BaseModel target, String token, int value) {
        Habilidade h = new Habilidade(); h.setCodigo(token); h.setValor(value); target.addHabilidade(h);
    }

    @Test void cultureAndMultiplierStackWithExactIntegerTruncation() {
        Nacao n = new Nacao();
        ability(n, ";PRT;", 31); ability(n, ";NRE;", 17);
        ability(n, ";TCS2;", 150); ability(n, ";TCS3;", 133);
        assertEquals(447, RecruitmentRules.citySlots(3, true, n));
        assertEquals(399, RecruitmentRules.citySlots(3, false, n));
    }

    @Test void perOrderRuleUsesGameBeforeScenarioAndDoesNotReplaceTheCityPool() {
        Partida p = new Partida(); p.setCenario(new Cenario()); Nacao n = new Nacao();
        ability(p.getCenario(), ";GRL100;", 100); ability(n, ";NRE;", 20);
        assertEquals(120, RecruitmentRules.orderSlots(500, p, n));
        ability(p, ";GRL100;", 80);
        assertEquals(100, RecruitmentRules.orderSlots(500, p, n));
        assertEquals(500, RecruitmentRules.orderSlots(500, new Partida(), n));
    }

    @Test void leatherComponentsRemainSeparateAndPrecedenceIsStable() {
        TipoTropa t = new TipoTropa(); t.setCodigo("test"); Nacao n = new Nacao();
        ability(t, ";TTL;", 1); ability(t, ";TTC;", 1); ability(t, ";TEL2;", 2); ability(t, ";TEL3;", 3);
        ability(t, ";TEI200;", 200); ability(t, ";TEI10;", 10);
        var m = RecruitmentRules.materials(t, n, true, false, false, false, false, false);
        assertEquals(2, m.cavalryLeather()); assertEquals(2, m.extraLeather());
        assertEquals(1, m.mounts()); assertEquals(200, m.iron());
        assertArrayEquals(new long[]{40, 0, 2000, 0, 0, 0, 10}, m.demand(10));
    }

    @Test void waiversDependOnCityAndFastTroopConditions() {
        TipoTropa t = new TipoTropa(); t.setCodigo("test"); ability(t, ";TTC;", 1);
        Nacao n = new Nacao(); ability(n, ";PCR;", 1);
        assertEquals(0, RecruitmentRules.materials(t,n,false,true,false,false,false,false).mounts());
        assertEquals(1, RecruitmentRules.materials(t,n,false,false,false,false,false,false).mounts());
        n = new Nacao(); ability(n, ";PBT;", 1); ability(n, ";PDR;", 1);
        assertFalse(RecruitmentRules.waivesLeatherMounts(t,n,false,true,true));
        ability(t, ";TTF;", 1);
        assertTrue(RecruitmentRules.waivesLeatherMounts(t,n,false,true,false));
        assertTrue(RecruitmentRules.waivesLeatherMounts(t,n,false,false,true));
    }

    @Test void woodDiscountsRetainPrecedenceTruncationAndLegacyFallback() {
        TipoTropa t = new TipoTropa(); t.setCodigo("ship"); ability(t,";TTN;",1); ability(t,";TTW;",101);
        Nacao n = new Nacao();
        assertEquals(50, RecruitmentRules.materials(t,n,true,false,false,false,true,true).wood());
        assertEquals(33, RecruitmentRules.materials(t,n,true,false,false,false,false,true).wood());
        ability(n,";PSW;",4); ability(n,";PSW3;",3);
        assertEquals(25, RecruitmentRules.materials(t,n,true,false,false,false,true,true).wood());
        ability(t,";TTW2;",2);
        assertEquals(0, RecruitmentRules.materials(t,n,true,false,false,false,true,true).wood());
    }

    @Test void catapultNewRulesChangesOnlyFallbackWoodCost() {
        TipoTropa t = new TipoTropa(); t.setCodigo("wm"); ability(t,";TTS;",1); ability(t,";TTW;",100);
        assertEquals(50, RecruitmentRules.materials(t,new Nacao(),true,false,false,false,false,false).wood());
        assertEquals(100, RecruitmentRules.materials(t,new Nacao(),false,false,false,false,false,false).wood());
        ability(t,";TTW100;",123);
        assertEquals(123, RecruitmentRules.materials(t,new Nacao(),true,false,false,false,false,false).wood());
    }

    @Test void absentWoodDoesNotEvaluateInvalidDiscountDivisor() {
        TipoTropa t = new TipoTropa(); t.setCodigo("ship"); ability(t,";TTN;",1); ability(t,";TEB50;",50);
        Nacao n = new Nacao(); ability(n,";PSW;",0);
        assertEquals(50, RecruitmentRules.materials(t,n,true,false,false,false,false,false).bronze());
        ability(t,";TTW;",100);
        assertThrows(ArithmeticException.class, () -> RecruitmentRules.materials(t,n,true,false,false,false,false,false));
    }

    @Test void largeMaterialBillsUseLongArithmetic() {
        var m = new RecruitmentRules.Materials(0,0,0,1000,0,0,0,0);
        assertEquals(2_147_483_647_000L, m.demand(Integer.MAX_VALUE)[2]);
    }
}
