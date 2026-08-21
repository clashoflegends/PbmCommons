package business.facade;

import model.Cenario;
import model.Habilidade;
import model.Ordem;
import model.Partida;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The guard orders need two different help texts: the old one, which is deliberately vague because
 * the mechanic is secret dice, and a published formula for games flying ;GAG;. Which one a player
 * sees has to follow the game, not the order.
 *
 * The regression that matters most is the FIRST test: every live game is on the no-flag path, and a
 * game without ;GAG; must render byte-identical to before this existed.
 */
class OrdemFacadeGuardHelpTest {

    private static final String BASE = "The character will act as a bodyguard.";
    private final OrdemFacade facade = new OrdemFacade();

    private static Ordem ordem(int numero) {
        final Ordem ret = new Ordem();
        ret.setNumero(numero);
        ret.setAjuda(BASE);
        return ret;
    }

    /** A game carrying the given flags, with the scenario carrying none. */
    private static Partida game(String... flags) {
        final Partida ret = new Partida();
        ret.setCenario(new Cenario());
        for (String flag : flags) {
            ret.addHabilidade(habilidade(flag, 0));
        }
        return ret;
    }

    private static Habilidade habilidade(String codigo, int valor) {
        final Habilidade ret = new Habilidade();
        ret.setCodigo(codigo);
        ret.setValor(valor);
        return ret;
    }

    // ---------------------------------------------------------------- the no-flag path

    @Test
    void aGameWithoutTheFlagIsUntouched() {
        assertEquals(BASE, facade.getAjuda(ordem(610), game()));
        assertEquals(BASE, facade.getAjuda(ordem(605), game()));
    }

    @Test
    void noGameLoadedIsUntouched() {
        //the Battle Simulator and the Actions tab can render help with no turn open
        assertEquals(BASE, facade.getAjuda(ordem(610), null));
    }

    @Test
    void onlyTheTwoGuardOrdersGainTheBlock() {
        final Partida g = game(";GAG;");
        assertTrue(facade.getAjuda(ordem(605), g).length() > BASE.length(), "605 guard location");
        assertTrue(facade.getAjuda(ordem(610), g).length() > BASE.length(), "610 guard character");
        assertEquals(BASE, facade.getAjuda(ordem(615), g), "615 assassinate is not a guard order");
        assertEquals(BASE, facade.getAjuda(ordem(690), g), "690 steal gold is not a guard order");
    }

    // ---------------------------------------------------------------- the flag combinations

    @Test
    void gagAloneExplainsTheMechanicButPromisesNoFloor() {
        final String help = facade.getAjuda(ordem(610), game(";GAG;"));
        assertTrue(help.startsWith(BASE), "the scenario text stays first and unedited");
        assertTrue(help.contains("Defence"), "the formula is spelled out");
        assertTrue(help.contains("tie goes to the guard"));
        assertFalse(help.contains("never killed by a single attack"),
                ";GGF;/;GAF; are independent of ;GAG; - a game can carry the rule and no floor");
    }

    @Test
    void eachFloorAppearsOnlyWithItsOwnFlagAndCarriesItsOwnValue() {
        final Partida withGuardFloor = game(";GAG;");
        withGuardFloor.addHabilidade(habilidade(";GGF;", 70));
        final String guardOnly = facade.getAjuda(ordem(610), withGuardFloor);
        assertTrue(guardOnly.contains("uninjured guard of skill 70"));
        assertFalse(guardOnly.contains("uninjured aggressor"), ";GAF; is not set");

        final Partida withBoth = game(";GAG;");
        withBoth.addHabilidade(habilidade(";GGF;", 70));
        withBoth.addHabilidade(habilidade(";GAF;", 85));
        final String both = facade.getAjuda(ordem(610), withBoth);
        assertTrue(both.contains("uninjured guard of skill 70"));
        assertTrue(both.contains("uninjured aggressor of natural skill 85"));
    }

    @Test
    void aFloorSetToSomethingOtherThanTheDefaultShowsThatValue() {
        //test games can be set to anything, so the text must never hardcode 70/85
        final Partida g = game(";GAG;");
        g.addHabilidade(habilidade(";GGF;", 55));
        assertTrue(facade.getAjuda(ordem(610), g).contains("uninjured guard of skill 55"));
    }

    @Test
    void aFloorWithoutTheRuleFlagChangesNothing() {
        final Partida g = game();
        g.addHabilidade(habilidade(";GGF;", 70));
        assertEquals(BASE, facade.getAjuda(ordem(610), g),
                "the floors only mean anything inside the deterministic gate");
    }

    // ---------------------------------------------------------------- game vs scenario

    @Test
    void aFlagInheritedFromTheVariantCountsToo() {
        //Cenario.habilidades comes from variante.habilidades, and the Judge's
        //PartidaControl.hasHabilidade ORs game with scenario. Help that disagreed with the Judge
        //would be worse than no help at all.
        final Partida g = new Partida();
        final Cenario cenario = new Cenario();
        cenario.addHabilidade(habilidade(";GAG;", 0));
        g.setCenario(cenario);
        assertTrue(facade.getAjuda(ordem(610), g).contains("Defence"));
    }

    @Test
    void theGamesOwnValueBeatsTheScenarios() {
        final Partida g = new Partida();
        final Cenario cenario = new Cenario();
        cenario.addHabilidade(habilidade(";GAG;", 0));
        cenario.addHabilidade(habilidade(";GGF;", 70));
        g.setCenario(cenario);
        g.addHabilidade(habilidade(";GGF;", 40));
        assertTrue(facade.getAjuda(ordem(610), g).contains("uninjured guard of skill 40"),
                "game overrides scenario, exactly as PartidaControl.getHabilidadeValor does");
    }
}
