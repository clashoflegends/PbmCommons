package business.facade;

import model.Cenario;
import model.Habilidade;
import model.Nacao;
import model.Ordem;
import org.junit.jupiter.api.Test;

import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Turn-0 startup packages (SNAs) are gated by the FILTER habilidades hanging off the package
 * (";FGOT;" = GoT only, ";FN001;" = Targaryen only, ...). The Judge runs a package only when the
 * nation carries EVERY one of them (Ordem105StartupPackage.criticaRequisitos); these tests pin the
 * client mirror of that rule, which is what keeps ineligible packages out of the order combo.
 */
class OrdemFacadeSnaFilterTest {

    private static final OrdemFacade facade = new OrdemFacade();

    private static Habilidade hab(String codigo, String tipo) {
        final Habilidade h = new Habilidade();
        h.setCodigo(codigo);
        h.setNome(codigo);
        h.setTipo(tipo);
        return h;
    }

    /** A package order carrying the given filters, shaped like a real 'Setup' ordem_cenario row. */
    private static Ordem pacote(String cdOrdem, String cdPacote, String... cdFiltros) {
        final Habilidade pack = hab(cdPacote, "PACKAGE");
        for (String f : cdFiltros) {
            pack.addHabilidade(hab(f, "FILTER"));
        }
        //a package always bundles at least one real power; it must not be mistaken for a filter
        pack.addHabilidade(hab(";PXX;", "NATION"));
        final Ordem ordem = new Ordem();
        ordem.setCodigo(cdOrdem);
        ordem.setNome(cdOrdem);
        ordem.setTipo("Misc");
        ordem.setTipoPersonagem("N");
        ordem.setRequisitos("Setup");
        ordem.addHabilidade(pack);
        return ordem;
    }

    private static Nacao nacao(String... cdHabilidades) {
        final Nacao n = new Nacao();
        n.setCodigo("1");
        n.setNome("House Stark");
        for (String c : cdHabilidades) {
            n.addHabilidade(hab(c, "FILTER"));
        }
        return n;
    }

    private static Cenario cenario(String codigo) {
        final Cenario c = new Cenario();
        c.setCodigo(codigo);
        c.setNome(codigo);
        return c;
    }

    // --- the rule itself ---

    @Test
    void packageWithNoFilterIsOfferedToEveryone() {
        assertTrue(facade.isPacoteNacaoOk(nacao(), pacote("9001", ";SP015;"), true, cenario("GOT13")));
    }

    @Test
    void nationCarryingEveryFilterIsOffered() {
        assertTrue(facade.isPacoteNacaoOk(nacao(";FGOT;", ";FN004;"),
                pacote("9002", ";SP029;", ";FGOT;", ";FN004;"), true, cenario("GOT13")));
    }

    @Test
    void everyFilterMustMatchNotJustOne() {
        //the Stark player carries ;FGOT; but not ;FN001; - "Targaryen only" must not be offered
        assertFalse(facade.isPacoteNacaoOk(nacao(";FGOT;", ";FN004;"),
                pacote("9003", ";SP030;", ";FGOT;", ";FN001;"), true, cenario("GOT13")));
    }

    @Test
    void firstAgeIgnoresTheGotGate() {
        //ME1A offers 40 GoT-tagged packages to nations that carry ;FT1A; - the Judge skips ;FGOT; there
        final Ordem ordem = pacote("9004", ";SP005;", ";FGOT;");
        assertFalse(facade.isPacoteNacaoOk(nacao(";FT1A;"), ordem, true, cenario("GOT13")));
        assertTrue(facade.isPacoteNacaoOk(nacao(";FT1A;"), ordem, true, cenario("ME1A")));
    }

    @Test
    void firstAgeExceptionDoesNotCoverNationGates() {
        assertFalse(facade.isPacoteNacaoOk(nacao(";FT1A;"),
                pacote("9005", ";SP030;", ";FGOT;", ";FN001;"), true, cenario("ME1A")));
    }

    @Test
    void failsOpenWhenTheHabilidadeTypeIsUnreadable() {
        //XStream builds models without the constructor, so an old EGF can hand us a null tipo:
        //isPackage()/isFilter() NPE on it. Unreadable must mean "show the order", never a crash.
        final Habilidade broken = new Habilidade();
        broken.setCodigo(";SP030;");
        broken.setNome("broken");
        final Ordem ordem = new Ordem();
        ordem.setCodigo("9006");
        ordem.setTipo("Misc");
        ordem.setTipoPersonagem("N");
        ordem.setRequisitos("Setup");
        ordem.addHabilidade(broken);
        assertTrue(facade.isPacoteNacaoOk(nacao(";FGOT;"), ordem, true, cenario("GOT13")));

        final Ordem comFiltroQuebrado = pacote("9007", ";SP030;");
        comFiltroQuebrado.getHabilidades().get(";SP030;").addHabilidade(hab(";FN001;", null));
        assertTrue(facade.isPacoteNacaoOk(nacao(";FGOT;"), comFiltroQuebrado, true, cenario("GOT13")));
    }

    @Test
    void isInertOutsideNationPackagesMode() {
        final Ordem ordem = pacote("9008", ";SP030;", ";FN001;");
        assertTrue(facade.isPacoteNacaoOk(nacao(";FGOT;"), ordem, false, cenario("GOT13")));
    }

    @Test
    void isInertForNonNationActors() {
        assertTrue(facade.isPacoteNacaoOk(new model.Personagem(),
                pacote("9009", ";SP030;", ";FN001;"), true, cenario("GOT13")));
    }

    @Test
    void nullCenarioDoesNotBlowUp() {
        assertFalse(facade.isPacoteNacaoOk(nacao(";FT1A;"), pacote("9010", ";SP005;", ";FGOT;"), true, null));
    }

    // --- through the combo builder, where it actually bites ---

    private static SortedMap<String, Ordem> ordens(Ordem... items) {
        final SortedMap<String, Ordem> map = new TreeMap<>();
        for (Ordem o : items) {
            map.put(o.getCodigo(), o);
        }
        return map;
    }

    private static boolean lists(Ordem[] items, String cdOrdem) {
        for (Ordem o : items) {
            if (o.getCodigo().equals(cdOrdem)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void comboDropsPackagesTheNationCannotBuy() {
        final Ordem mine = pacote("9011", ";SP029;", ";FGOT;", ";FN004;");
        final Ordem theirs = pacote("9012", ";SP030;", ";FGOT;", ";FN001;");
        final Ordem[] items = facade.getOrdensDisponiveis(ordens(mine, theirs),
                nacao(";FGOT;", ";FN004;"), 0, false, true, cenario("GOT13"));
        assertTrue(lists(items, "9011"), "own-nation package must stay listed");
        assertFalse(lists(items, "9012"), "other nation's package must be dropped");
    }

    @Test
    void allOrdersStillListsEverything() {
        final Ordem theirs = pacote("9013", ";SP030;", ";FGOT;", ";FN001;");
        final Ordem[] items = facade.getOrdensDisponiveis(ordens(theirs),
                nacao(";FGOT;", ";FN004;"), 0, true, true, cenario("GOT13"));
        assertTrue(lists(items, "9013"), "ticking ALL is the escape hatch and must bypass the gate");
    }

    @Test
    void anAlreadySavedPackageIsNeverDroppedFromItsOwnSlot() {
        //a package bought before this filter existed must not vanish from the slot holding it
        final Ordem theirs = pacote("9014", ";SP030;", ";FGOT;", ";FN001;");
        final Nacao nacao = nacao(";FGOT;", ";FN004;");
        final model.PersonagemOrdem po = new model.PersonagemOrdem();
        po.setOrdem(theirs);
        nacao.setAcao(0, po);
        assertTrue(lists(facade.getOrdensDisponiveis(ordens(theirs), nacao, 0, false, true, cenario("GOT13")), "9014"),
                "saved in the slot being edited");
        assertFalse(lists(facade.getOrdensDisponiveis(ordens(theirs), nacao, 1, false, true, cenario("GOT13")), "9014"),
                "but not offered again in a different slot");
    }
}
