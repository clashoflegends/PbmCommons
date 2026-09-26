package business.combat;

import java.util.SortedMap;
import java.util.TreeMap;
import model.Cenario;
import model.Cidade;
import model.Local;
import model.Nacao;
import model.Partida;
import model.Pelotao;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The wire format: what survives a round trip, and what is refused outright.
 *
 * Both halves matter equally and for the same reason. This is what an ALLY loads, so anything that
 * quietly fails to travel makes two people look at one battle and see different numbers - which is
 * the only outcome a sharing feature cannot have. And anything that quietly HALF-travels is worse:
 * a scenario the receiver did not build and cannot see the seams of.
 */
public class BattleSimTransferTest extends LandCombatFixture {

    private static final String TROOP = "inf", TROOP2 = "bow";

    /**
     * The receiving world's catalogue: its terrains and its troop types.
     *
     * NOT named {@code cenario()}. {@code LandCombatFixture} already has a no-arg one, and Java
     * prefers a non-varargs overload - so every call here silently bound to the fixture's empty
     * scenario and every army arrived with a null troop type. The compiler was perfectly happy;
     * eight tests died on a NullPointerException three frames from the cause.
     */
    private static Cenario catalogue(String... habilidades) {
        final Cenario ret = new Cenario();
        ret.getTerrenos().put(PLAIN.getCodigo(), PLAIN);
        ret.getTerrenos().put(FOREST.getCodigo(), FOREST);
        for (String codigo : new String[]{TROOP, TROOP2}) {
            final TipoTropa tipo = troopType(codigo, 60, 40, false);
            ret.getTipoTropas().put(codigo, tipo);
        }
        for (String hab : habilidades) {
            final model.Habilidade one = new model.Habilidade();
            one.setCodigo(hab);
            one.setNome(hab);
            ret.addHabilidade(one);
        }
        return ret;
    }

    private static Partida partida(Cenario cenario) {
        final Partida ret = new Partida();
        ret.setCenario(cenario);
        return ret;
    }

    private static SortedMap<String, Nacao> nacoes(Nacao... list) {
        final SortedMap<String, Nacao> ret = new TreeMap<>();
        for (Nacao one : list) {
            ret.put(one.getCodigo(), one);
        }
        return ret;
    }

    /** A scenario with something edited in every corner the format has to carry. */
    private static CombatScenario edited(Partida partida, Nacao mine, Nacao foe) {
        // The city goes ON THE HEX, which is how a real scenario gets one: setLocal takes it and
        // switches participation on. Setting it afterwards leaves cityParticipates false, and then
        // getNacoes() omits its owner - a state the application never produces.
        final Local local = hex();
        local.setTerreno(PLAIN);
        local.setCidade(cityOwnedBy(foe));
        final CombatScenario ret = new CombatScenario(partida, local);
        final TipoTropa tipo = partida.getCenario().getTipoTropas().get(TROOP);
        final Pelotao pelotao = platoon(tipo, 900);
        pelotao.setTreino(55);
        pelotao.setModAtaque(30);
        pelotao.setModDefesa(20);
        final ArmySim army = army("Joron", mine, pelotao);
        army.setCodigo("a1");
        army.setTatica(3);
        army.setMoral(42);
        army.setComandante(37);
        army.setCombatLevel(CombatLevel.RAZE_CITY);
        army.setBonusAttack(500);
        army.setBonusDefense(250);
        ret.addArmy(army, CombatScenario.Provenance.ESTIMATED);
        ret.setRelacionamento(mine, foe, RelationshipMatrix.SWORN_ENEMY);
        ret.setTerreno(FOREST);

        // the scenario's own clone of it, edited as a player would
        ret.getCidade().setTamanho(3);
        ret.getCidade().setFortificacao(2);
        ret.getCidade().setLealdade(52);
        return ret;
    }

    private static Cidade cityOwnedBy(Nacao owner) {
        final Cidade ret = new Cidade();
        ret.setCodigo("c1");
        ret.setNome("Riverrun");
        ret.setTamanho(3);
        ret.setFortificacao(2);
        ret.setLealdade(52);
        ret.setNacao(owner);
        return ret;
    }

    private static ArmySim only(CombatScenario scenario) {
        return scenario.getArmies().get(0);
    }

    /** Everything the player edited comes back, in a world that has the same codes. */
    @Test
    public void everyEditSurvivesTheRoundTrip() throws Exception {
        final Cenario cenario = catalogue();
        final Partida partida = partida(cenario);
        final Nacao mine = nacao("m"), foe = nacao("f");
        final CombatScenario original = edited(partida, mine, foe);

        final CombatScenario back = BattleSimTransfer.read(
                BattleSimTransfer.write(original), partida, nacoes(mine, foe));
        final ArmySim army = only(back);

        assertEquals("a1", army.getCodigo(), "codigo");
        assertEquals(3, army.getTatica(), "tactic");
        assertEquals(42, army.getMoral(), "morale");
        assertEquals(37, army.getComandantePericia(), "commander skill");
        assertEquals(CombatLevel.RAZE_CITY, army.getCombatLevel(), "combat level");
        assertEquals(500, army.getAttackBonus(), "attack bonus");
        assertEquals(250, army.getArmyDefenseBonus(), "defense bonus");
        assertEquals(FOREST, back.getTerreno(), "the terrain OVERRIDE, not the hex's");
        assertEquals(RelationshipMatrix.SWORN_ENEMY,
                back.getRelationships().getValor(mine, foe), "the declared relationship");

        final Pelotao pelotao = army.getPelotoes().values().iterator().next();
        assertEquals(900, pelotao.getQtd(), "quantity");
        assertEquals(55, pelotao.getTreino(), "training");
        assertEquals(30, pelotao.getModAtaque(), "weapon");
        assertEquals(20, pelotao.getModDefesa(), "armour");

        assertEquals(3, back.getCidade().getTamanho(), "city size");
        assertEquals(2, back.getCidade().getFortificacao(), "fortification");
        assertEquals(52, back.getCidade().getLealdade(), "loyalty");
        assertEquals(foe, back.getCidade().getNacao(), "the city's owner, resolved by code");
    }

    /**
     * Everything that arrives is MANUAL, whatever it was when it left.
     *
     * John, 2026-09-26: "We assume we are streamlining player typing stuff." It is also the honest
     * answer - MANUAL makes no EXACT or ESTIMATED claim and does not inflate the unknown-morale
     * count, so an ally reads "these are his figures" rather than "the game told me this".
     */
    @Test
    public void everythingArrivesAsManual() throws Exception {
        final Cenario cenario = catalogue();
        final Partida partida = partida(cenario);
        final Nacao mine = nacao("m"), foe = nacao("f");
        final CombatScenario original = edited(partida, mine, foe);
        assertEquals(CombatScenario.Provenance.ESTIMATED, original.getProvenance(only(original)),
                "it left as a guess");

        final CombatScenario back = BattleSimTransfer.read(
                BattleSimTransfer.write(original), partida, nacoes(mine, foe));

        assertEquals(CombatScenario.Provenance.MANUAL, back.getProvenance(only(back)),
                "and arrives as the player's own");
        assertFalse(back.isMoraleUnknown(only(back)),
                "so it carries no '(?)' and does not inflate the disclosure");
    }

    /** The file must not be mistakable for an EGF, or one day something will try to load it as one. */
    @Test
    public void theFileIsNothingLikeAnEgf() {
        final Cenario cenario = catalogue();
        final Partida partida = partida(cenario);
        final String xml = BattleSimTransfer.write(edited(partida, nacao("m"), nacao("f")));

        assertTrue(xml.startsWith("<battlesim>"), "its own root: " + xml.substring(0, 40));
        assertFalse(xml.contains("model."), "no model.* class tag anywhere in it");
        assertFalse(xml.contains("business.combat.ArmySim"), "and no engine class either");
    }

    /** One army on the clipboard, with everything the player set on it. T-841. */
    @Test
    public void oneArmyRoundTripsAsAFragment() throws Exception {
        final Cenario cenario = catalogue();
        final Partida partida = partida(cenario);
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim source = only(edited(partida, mine, foe));

        final java.util.List<ArmySim> back = BattleSimTransfer.readArmies(
                BattleSimTransfer.writeArmy(source), partida, nacoes(mine, foe));

        assertEquals(1, back.size());
        assertEquals(3, back.get(0).getTatica(), "tactic");
        assertEquals(42, back.get(0).getMoral(), "morale");
        assertEquals(CombatLevel.RAZE_CITY, back.get(0).getCombatLevel(), "combat level");
        assertEquals(900, back.get(0).getPelotoes().values().iterator().next().getQtd(),
                "the platoon came with it");
    }

    /**
     * A WHOLE saved battle on the clipboard yields its armies too.
     *
     * Both are things a player will plausibly have copied - the army he selected, or the contents
     * of a .bsim someone sent him - and refusing the second would be a distinction he has no reason
     * to expect.
     */
    @Test
    public void awholeSavedBattleYieldsItsArmies() throws Exception {
        final Cenario cenario = catalogue();
        final Partida partida = partida(cenario);
        final Nacao mine = nacao("m"), foe = nacao("f");

        final java.util.List<ArmySim> back = BattleSimTransfer.readArmies(
                BattleSimTransfer.write(edited(partida, mine, foe)), partida, nacoes(mine, foe));

        assertEquals(1, back.size(), "the battle's one army");
    }

    /**
     * A pasted army arrives with NO hex, and the receiving side must supply one.
     *
     * The file cannot carry a Local - it is the receiver's world that has hexes - so the army comes
     * back with a null one. That is not cosmetic: every attack lookup runs through the shared
     * formula with that Local, and the formula SWALLOWS the resulting NPE and returns zero, so a
     * pasted army would go into battle with no attack at all and nothing on screen to say why. The
     * same trap doAddArmy already carries a comment about. This pins the precondition that
     * doAddPastedArmy exists to satisfy.
     */
    @Test
    public void aPastedArmyHasNoHexUntilTheReceiverGivesItOne() throws Exception {
        final Cenario cenario = catalogue();
        final Partida partida = partida(cenario);
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim source = only(edited(partida, mine, foe));

        final ArmySim back = BattleSimTransfer.readArmies(
                BattleSimTransfer.writeArmy(source), partida, nacoes(mine, foe)).get(0);

        assertEquals(null, back.getLocal(),
                "the transfer does not invent a hex - doAddPastedArmy supplies it");
    }

    /** A fragment carrying a foreign troop type refuses, like a whole file does. */
    @Test
    public void aFragmentWithAnUnknownTroopDeclines() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final String xml = BattleSimTransfer.writeArmy(
                only(edited(partida(catalogue()), mine, foe)));
        final Cenario other = catalogue();
        other.getTipoTropas().remove(TROOP);

        final BattleSimTransfer.TransferException ex =
                assertThrows(BattleSimTransfer.TransferException.class,
                        () -> BattleSimTransfer.readArmies(xml, partida(other), nacoes(mine, foe)));

        assertEquals("BATTLESIM.TRANSFER.TROOP", ex.getReasonKey());
        assertEquals(TROOP, ex.getOffending());
    }

    /** And the clipboard usually holds something else entirely. */
    @Test
    public void pastingRubbishDeclinesCleanly() {
        for (String junk : new String[]{"", "an army", "<battlesim-army", "hello world"}) {
            assertThrows(BattleSimTransfer.TransferException.class,
                    () -> BattleSimTransfer.readArmies(junk, partida(catalogue()), nacoes()),
                    "should refuse: " + junk);
        }
    }

    // ------------------------------------------------------------------ the refusals

    private static String writtenIn(Cenario cenario, Nacao mine, Nacao foe) {
        return BattleSimTransfer.write(edited(partida(cenario), mine, foe));
    }

    /** A troop type the receiving game does not have declines the WHOLE load, and names it. */
    @Test
    public void anUnknownTroopTypeDeclinesAndSaysWhich() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final String xml = writtenIn(catalogue(), mine, foe);
        final Cenario other = catalogue();
        other.getTipoTropas().remove(TROOP);            // a different scenario's catalogue

        final BattleSimTransfer.TransferException ex =
                assertThrows(BattleSimTransfer.TransferException.class,
                        () -> BattleSimTransfer.read(xml, partida(other), nacoes(mine, foe)));

        assertEquals("BATTLESIM.TRANSFER.TROOP", ex.getReasonKey());
        assertEquals(TROOP, ex.getOffending(),
                "naming it is the difference between a dead end and an explanation");
    }

    /** A nation the receiving world does not have, likewise. */
    @Test
    public void anUnknownNationDeclines() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final String xml = writtenIn(catalogue(), mine, foe);

        final BattleSimTransfer.TransferException ex =
                assertThrows(BattleSimTransfer.TransferException.class,
                        () -> BattleSimTransfer.read(xml, partida(catalogue()), nacoes(mine)));

        assertEquals("BATTLESIM.TRANSFER.NACAO", ex.getReasonKey());
        assertEquals("f", ex.getOffending());
    }

    /** And a terrain, which is the third thing John named. */
    @Test
    public void anUnknownTerrainDeclines() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final String xml = writtenIn(catalogue(), mine, foe);
        final Cenario other = catalogue();
        other.getTerrenos().remove(FOREST.getCodigo());  // the override's ground is gone

        final BattleSimTransfer.TransferException ex =
                assertThrows(BattleSimTransfer.TransferException.class,
                        () -> BattleSimTransfer.read(xml, partida(other), nacoes(mine, foe)));

        assertEquals("BATTLESIM.TRANSFER.TERRENO", ex.getReasonKey());
    }

    /**
     * A tactic from the OTHER engine family declines. T-822, and the case that nearly got waved
     * through.
     *
     * The first reading of this said a tactic always resolves. It does not: the two families fill
     * different, overlapping index sets of one {@code int[10][10]}, so an index from the wrong one
     * reads a cell nobody filled. That cell is zero, and a modTatica of zero multiplies the army's
     * entire troop attack away - silently, with the battle still reporting a result.
     */
    @Test
    public void aTacticFromTheOtherEngineFamilyDeclines() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        // written in an ;ST2; game, where 7 is a real tactic
        final Cenario wdo = catalogue(";ST2;");
        final CombatScenario source = edited(partida(wdo), mine, foe);
        only(source).setTatica(7);
        final String xml = BattleSimTransfer.write(source);

        final BattleSimTransfer.TransferException ex =
                assertThrows(BattleSimTransfer.TransferException.class,
                        () -> BattleSimTransfer.read(xml, partida(catalogue()), nacoes(mine, foe)));

        assertEquals("BATTLESIM.TRANSFER.TACTIC", ex.getReasonKey());
        assertEquals("7", ex.getOffending());
    }

    /** And the same in reverse: a traditional tactic has no row in the alternate table either. */
    @Test
    public void aTraditionalTacticDeclinesInAnAlternateGame() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final CombatScenario source = edited(partida(catalogue()), mine, foe);
        only(source).setTatica(5);                       // Ambush: traditional only
        final String xml = BattleSimTransfer.write(source);

        assertThrows(BattleSimTransfer.TransferException.class,
                () -> BattleSimTransfer.read(xml, partida(catalogue(";ST2;")), nacoes(mine, foe)));
    }

    /** A file from a future build is refused rather than half-read. */
    @Test
    public void aFileFromTheFutureDeclines() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final String xml = writtenIn(catalogue(), mine, foe)
                .replace("<version>1</version>", "<version>99</version>");

        final BattleSimTransfer.TransferException ex =
                assertThrows(BattleSimTransfer.TransferException.class,
                        () -> BattleSimTransfer.read(xml, partida(catalogue()), nacoes(mine, foe)));

        assertEquals("BATTLESIM.TRANSFER.VERSION", ex.getReasonKey());
        assertEquals("99", ex.getOffending());
    }

    /** Anything that is not one of our files gets one answer, not a stack trace. */
    @Test
    public void rubbishOnTheClipboardDeclinesCleanly() {
        for (String junk : new String[]{"", "hello", "<xml/>", "<battlesim", "{\"a\":1}"}) {
            final BattleSimTransfer.TransferException ex =
                    assertThrows(BattleSimTransfer.TransferException.class,
                            () -> BattleSimTransfer.read(junk, partida(catalogue()), nacoes()),
                            "should refuse: " + junk);
            assertEquals("BATTLESIM.TRANSFER.FORMAT", ex.getReasonKey());
        }
    }
}
