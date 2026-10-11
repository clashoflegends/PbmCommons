package business.facade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Local;
import model.Ordem;
import model.Personagem;
import model.PersonagemOrdem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@code getLocalDestination} answers "where will this character be standing when the turn ends".
 *
 * Two very different callers depend on it and they are the reason it has two forms. The DRAWING
 * callers - {@code MapaManager.drawScoutOnMap} for the classic scout marker and the Counselor's
 * {@code ScoutFootprint} for the vector ring - would rather be a hex off than silent, because a
 * missing ring reads as "no scout there". The Counselor's turn review puts a SENTENCE in front of
 * the player and cannot guess at all.
 *
 * The three defects pinned below were all found building that review, and all three were live on the
 * map the whole time. Each test names the order from the real catalogue that triggers it; the
 * classification of every {@code tp_ordem='Mov'} order was taken from {@code ordem_cenario} across
 * games 850 to 907, not from the two examples that happened to be noticed first.
 */
class PersonagemFacadeDestinationTest {

    private final PersonagemFacade facade = new PersonagemFacade();
    private final SortedMap<String, Local> locais = new TreeMap<>();

    private Local hex(String codigo) {
        Local ret = locais.get(codigo);
        if (ret == null) {
            ret = new Local();
            ret.setCodigo(codigo);   //LocalFacade derives col/row from it
            locais.put(codigo, ret);
        }
        return ret;
    }

    private Personagem pers(String nome, String at) {
        final Personagem ret = new Personagem();
        ret.setNome(nome);
        ret.setCodigo(nome);
        ret.setLocal(hex(at));
        return ret;
    }

    /** An order of the given catalogue shape, dropped into the next free slot. */
    private void give(Personagem pc, String nome, String tipo, String params, String... values) {
        final Ordem o = new Ordem();
        o.setNome(nome);
        o.setTipo(tipo);
        o.setParametrosIde(params.split(";"));
        final PersonagemOrdem po = new PersonagemOrdem();
        po.setOrdem(o);
        po.setParametrosId(new ArrayList<>(Arrays.asList(values)));
        pc.setAcao(pc.getAcaoSize(), po);
    }

    private void idle(Personagem pc) {
        give(pc, "Ocultar", "Per", "None", " ");
    }

    // ----- defect 1: a movement-TYPED order is not necessarily a move -----

    /**
     * {@code Bloq} (840) is {@code tp_ordem='Mov'} with a bare {@code Direcao}, and
     * {@code Ordem.executa840} sets a blockade facing and subtracts morale without ever touching the
     * hex. The old {@code isMovimentoDirection} test matched it and walked it as a path, putting a
     * blockading commander one hex from where they stand - on the map, and in any warning built on
     * this. A blockading army commander is exactly who a player marches out to join.
     *
     * {@code MovCity} (845) is the same shape and moves a CITY. Across the whole catalogue those two
     * are the only movement-typed orders with a bare {@code Direcao}.
     */
    @Test
    void aBlockadeIsNotAMove() {
        final Personagem pc = pers("Jon", "0505");
        give(pc, "Bloq", "Mov", "Direcao", "E");

        assertEquals(hex("0505"), facade.getLocalDestination(pc, locais),
                "Bloq sets a facing, not a destination");
    }

    @Test
    void movingACityDoesNotMoveTheCharacter() {
        final Personagem pc = pers("Jon", "0505");
        give(pc, "MovCity", "Mov", "Direcao", "E");

        assertEquals(hex("0505"), facade.getLocalDestination(pc, locais));
    }

    /** The control: a real army move, whose path is walked exactly as the map draws it. */
    @Test
    void anArmyPathResolvesToItsLastHex() {
        final Personagem pc = pers("Jon", "0505");
        hex("0605");
        hex("0705");
        give(pc, "MovEx", "Mov", "Direcao_Ex", "E;E");   //the wire format is ;-separated

        assertEquals(hex("0705"), facade.getLocalDestination(pc, locais));
    }

    /**
     * {@code ArtMov} (826, "Use Movement Magic Item") carries its destination SECOND, behind the
     * item, and {@code FeitMov} (825, "Cast Movement Spell") behind the spell. Both land the
     * character on that hex, so the coordinate has to be looked for in every position.
     */
    @Test
    void aDestinationBehindAnotherParameterStillCounts() {
        final Personagem pc = pers("Jon", "0505");
        give(pc, "ArtMov", "Mov", "Artefato_Carried;Coordenada", "item", "0707");

        assertEquals(hex("0707"), facade.getLocalDestination(pc, locais));
    }

    // ----- defect 2: the leader branch belongs after the loop -----

    /**
     * The old code put the {@code lider} branch INSIDE the loop over orders, so it answered with the
     * leader's destination the moment it met any non-movement order and never read a move in a later
     * slot. The Judge does the opposite: {@code Ordem.executa810} has an explicit
     * {@code isViajandoCom} branch that takes a passenger out of the group, so their own move wins.
     */
    @Test
    void aPassengersOwnMoveInALaterSlotBeatsTheLeader() {
        final Personagem leader = pers("Jeor", "0505");
        hex("0605");
        give(leader, "MovEx", "Mov", "Direcao_Ex", "E");
        final Personagem pc = pers("Jon", "0505");
        pc.setLider(leader);
        idle(pc);                                             // slot 0: not a move
        give(pc, "MovPers", "Mov", "Coordenada_12", "0909");  // slot 1: their own move

        assertEquals(hex("0909"), facade.getLocalDestination(pc, locais),
                "they leave the group; the leader's hex is not where they land");
    }

    /**
     * The mirror case, and the one the old shape got backwards: with NO orders at all the loop never
     * ran, so the leader branch inside it never ran either and a passenger fell through to its own
     * current hex instead of following whoever is carrying it.
     */
    @Test
    void aPassengerWithNoOrdersFollowsItsLeader() {
        final Personagem leader = pers("Jeor", "0505");
        hex("0605");
        give(leader, "MovEx", "Mov", "Direcao_Ex", "E");
        final Personagem pc = pers("Jon", "0505");
        pc.setLider(leader);

        assertEquals(hex("0605"), facade.getLocalDestination(pc, locais));
    }

    /** A cycle in the lider chain is corrupt data: say nothing rather than answer with a wrong hex. */
    @Test
    void aLeaderCycleTerminates() {
        final Personagem a = pers("Jon", "0505");
        final Personagem b = pers("Jeor", "0505");
        a.setLider(b);
        b.setLider(a);

        assertNull(facade.getLocalDestination(a, locais));
    }

    // ----- defect 3: a move with no readable destination is unknown, not stationary -----

    /**
     * {@code ArtMov} 805 is movement-typed and carries ONLY {@code Artefato_Carried}. The character
     * goes somewhere; nothing in the parameters says where. A drawing caller still wants its best
     * guess, but a caller that must not guess has to be able to tell this apart from "staying put".
     */
    @Test
    void aMoveWithNoReadableDestinationIsUnknownToAStrictCaller() {
        final Personagem pc = pers("Jon", "0505");
        give(pc, "ArtMov", "Mov", "Artefato_Carried", "item");

        assertEquals(hex("0505"), facade.getLocalDestination(pc, locais),
                "a drawing caller would rather have the current hex than nothing");
        assertNull(facade.getLocalDestination(pc, locais, pers -> false),
                "but it is NOT 'they are staying put', and a warning must not say so");
    }

    // ----- the two contracts -----

    /**
     * A character with no orders is the ambiguous case. In a SUBMITTED turn "no orders" really does
     * mean "not moving" and the data says so (42 of 45 successful rendezvous, 1 of 161 failures).
     * While the player is still PLANNING it means nothing of the sort, and the commonest way to meet
     * it is that they have not got to that character yet.
     */
    @Test
    void stillPlanningIsNotTheSameAsStayingPut() {
        final Personagem pc = pers("Jon", "0505");
        idle(pc);

        assertEquals(hex("0505"), facade.getLocalDestination(pc, locais),
                "the guessing form never returns null for a character standing on a hex");
        assertEquals(hex("0505"), facade.getLocalDestination(pc, locais, pers -> false),
                "done planning and not moving: the current hex is the right answer");
        assertNull(facade.getLocalDestination(pc, locais, pers -> true),
                "still being planned: there is no answer yet, and a guess would be a false warning");
    }

    @Test
    void aNullCharacterIsNotAnException() {
        assertNull(facade.getLocalDestination(null, locais));
        assertNull(facade.getLocalDestination(null, locais, pers -> true));
    }
}
