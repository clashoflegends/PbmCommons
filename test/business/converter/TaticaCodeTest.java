package business.converter;

import msgs.BaseMsgs;
import org.junit.jupiter.api.Test;
import persistenceCommons.SysApoio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * A tactic combo id is a CODE, and reading it as a number corrupts the army.
 *
 * The BattleSim's tactic combo is built from {@code BaseMsgs.taticasGb}, whose ids read "ca", "fl",
 * "pa", "ce", "gu", "em". The window parsed them with {@code SysApoio.parseInt}, which answers its
 * error sentinel -9999 for every one - so merely SELECTING an army in the roster wrote
 * {@code setTatica(-9999)} into it, and the combo, comparing -9999 against a real tactic, never
 * matched and always displayed the first entry.
 *
 * Nothing threw. {@code TitleFactory.getTaticaNome} swallows the out-of-range index and answers
 * "Padrao", so the army read as a plausible default all the way to the player. This test pins the
 * contract the window now relies on, and pins the trap it fell into so a future author does not
 * reach for parseInt again.
 */
public class TaticaCodeTest {

    /**
     * Every id in BOTH lists the combo can be built from survives the round trip.
     *
     * {@code CenarioFacade.listTaticas} answers {@code taticasTk} for a {@code ;ST2;} scenario and
     * {@code taticasGb} otherwise, so testing only the default list would have left the other half
     * of the scenarios unproven. {@code taticaToInt} falls through to {@code return 2} - Padrao -
     * for a code it does not know, which is the same silent-plausible-default shape as the sentinel
     * this test exists to keep out; an id outside its ten would corrupt the army just as quietly.
     */
    @Test
    void everyTacticCodeRoundTripsThroughTheConverter() {
        for (String[][] list : new String[][][]{BaseMsgs.taticasGb, BaseMsgs.taticasTk}) {
            for (String[] tatica : list) {
                final String codigo = tatica[1];
                final int numero = ConverterFactory.taticaToInt(codigo);
                assertEquals(codigo, ConverterFactory.taticaToCodigo(numero),
                        "tactic '" + codigo + "' must survive code -> int -> code");
            }
        }
    }

    /** The codes really are distinct numbers, so the combo can tell them apart. */
    @Test
    void theTacticCodesMapToDistinctNumbers() {
        assertEquals(0, ConverterFactory.taticaToInt("ca"));
        assertEquals(1, ConverterFactory.taticaToInt("fl"));
        assertEquals(2, ConverterFactory.taticaToInt("pa"));
        assertEquals(3, ConverterFactory.taticaToInt("ce"));
        assertEquals(4, ConverterFactory.taticaToInt("gu"));
        assertEquals(5, ConverterFactory.taticaToInt("em"));
    }

    /**
     * The trap itself, asserted so it cannot come back.
     *
     * parseInt does not fail loudly on these - it returns a number, and a number is exactly what
     * the caller wanted, which is why the bug survived review the first time.
     */
    @Test
    void parseIntIsNotAnAnswerForATacticCode() {
        for (String[][] list : new String[][][]{BaseMsgs.taticasGb, BaseMsgs.taticasTk}) {
            for (String[] tatica : list) {
                assertEquals(-9999, SysApoio.parseInt(tatica[1]),
                        "parseInt answers a sentinel, not a tactic, for '" + tatica[1] + "'");
                assertNotEquals(SysApoio.parseInt(tatica[1]),
                        ConverterFactory.taticaToInt(tatica[1]));
            }
        }
    }
}
