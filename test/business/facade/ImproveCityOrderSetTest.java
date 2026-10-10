package business.facade;

import model.Ordem;
import model.PersonagemOrdem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code AcaoFacade.isImproveCitySize} is what tells the Counselor that an order about to be saved
 * would grow a city, and therefore that the {@code ;SCC;} proximity cap applies to it.
 *
 * It is keyed on the order CODE, and the set has to match PbmJudge's {@code OrdemJudgeFactory} switch
 * exactly - those are the codes routed to {@code OrdemBaseImproveCity}, whose {@code criticaRequisitos}
 * runs the cap. Too narrow and the player is not warned; too wide and they are warned about an order
 * the cap never touches. 548 belongs in the set although it is named for fortification, because its
 * {@code executa} calls {@code sumTamanho()} as well.
 *
 * The one code that is deliberately OUT and sits right beside them is 555/556 Place Camp: founding is
 * not capped, only upgrading is, which is the exact confusion the player report started from.
 */
class ImproveCityOrderSetTest {

    private final AcaoFacade facade = new AcaoFacade();

    private static PersonagemOrdem ordem(String codigo) {
        final Ordem ret = new Ordem();
        ret.setCodigo(codigo);
        ret.setNome("order" + codigo);
        final PersonagemOrdem po = new PersonagemOrdem();
        po.setOrdem(ret);
        return po;
    }

    @Test
    void everyOrderTheJudgeRoutesToImproveCityIsInTheSet() {
        for (String codigo : new String[]{"548", "550", "551", "552", "553", "554"}) {
            assertTrue(facade.isImproveCitySize(ordem(codigo)),
                    codigo + " is dispatched to OrdemBaseImproveCity and so is capped by ;SCC;");
        }
    }

    @Test
    void foundingACampIsNotCapped() {
        assertFalse(facade.isImproveCitySize(ordem("555")), "Place Camp founds, it does not upgrade");
        assertFalse(facade.isImproveCitySize(ordem("556")));
        assertFalse(facade.isImproveCitySize(ordem("557")), "Create Camp founds, it does not upgrade");
    }

    @Test
    void neighbouringCodesAreNotSweptIn() {
        assertFalse(facade.isImproveCitySize(ordem("494")), "Fortify City raises fortification only");
        assertFalse(facade.isImproveCitySize(ordem("520")), "Improve Loyalty is not a size change");
        assertFalse(facade.isImproveCitySize(ordem("547")));
        assertFalse(facade.isImproveCitySize(ordem("549")));
    }

    @Test
    void survivesTheJunkTheEgfCanCarry() {
        assertFalse(facade.isImproveCitySize(null));
        assertFalse(facade.isImproveCitySize(new PersonagemOrdem()), "a slot with no order must not warn");
        assertFalse(facade.isImproveCitySize(ordem("")), "a blank code must not parse into the set");
        assertFalse(facade.isImproveCitySize(ordem("nonsense")));
    }
}
