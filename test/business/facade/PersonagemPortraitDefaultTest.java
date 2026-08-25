package business.facade;

import model.Personagem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The class+gender default portrait. Pins John's rule (2026-08-24): highest NATURAL skill wins, and
 * on a tie the order is Comandante > Mago > Agente > Emissario.
 */
public class PersonagemPortraitDefaultTest {

    private final PersonagemFacade facade = new PersonagemFacade();

    /** sexo: 0=male, 1=female, 2=undefined. */
    private Personagem pc(int comandante, int mago, int agente, int emissario, int sexo) {
        Personagem p = new Personagem();
        //NATURAL, not the total: setPericiaComandante() sets the buffed total and leaves natural at
        //zero, so using it here would make every case fall through to blank.jpg and the test would
        //pass while asserting nothing.
        p.setPericiaComandanteNatural(comandante);
        p.setPericiaMagoNatural(mago);
        p.setPericiaAgenteNatural(agente);
        p.setPericiaEmissarioNatural(emissario);
        p.setSexo(sexo);
        return p;
    }

    @Test
    public void highestNaturalSkillWins() {
        assertEquals("default_comandante_m.jpg", facade.getDefaultPortraitFilename(pc(50, 10, 20, 30, 0)));
        assertEquals("default_mago_m.jpg", facade.getDefaultPortraitFilename(pc(10, 50, 20, 30, 0)));
        assertEquals("default_agente_m.jpg", facade.getDefaultPortraitFilename(pc(10, 20, 50, 30, 0)));
        assertEquals("default_emissario_m.jpg", facade.getDefaultPortraitFilename(pc(10, 20, 30, 50, 0)));
    }

    @Test
    public void tieBreaksComandanteMagoAgenteEmissario() {
        assertEquals("default_comandante_m.jpg", facade.getDefaultPortraitFilename(pc(40, 40, 40, 40, 0)));
        assertEquals("default_mago_m.jpg", facade.getDefaultPortraitFilename(pc(0, 40, 40, 40, 0)));
        assertEquals("default_agente_m.jpg", facade.getDefaultPortraitFilename(pc(0, 0, 40, 40, 0)));
        assertEquals("default_emissario_m.jpg", facade.getDefaultPortraitFilename(pc(0, 0, 0, 40, 0)));
    }

    @Test
    public void genderPicksTheFile() {
        assertEquals("default_comandante_f.jpg", facade.getDefaultPortraitFilename(pc(50, 0, 0, 0, 1)));
        assertEquals("default_comandante_m.jpg", facade.getDefaultPortraitFilename(pc(50, 0, 0, 0, 0)));
        //undefined takes the male art rather than dropping the class information entirely
        assertEquals("default_comandante_m.jpg", facade.getDefaultPortraitFilename(pc(50, 0, 0, 0, 2)));
    }

    @Test
    public void noClassAtAllKeepsTheHistoricalBlank() {
        assertEquals("blank.jpg", facade.getDefaultPortraitFilename(pc(0, 0, 0, 0, 1)));
        assertEquals("blank.jpg", facade.getDefaultPortraitFilename(null));
    }
}
