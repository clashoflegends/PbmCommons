package business.combat;

import java.util.Arrays;
import model.Habilidade;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Partida;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the roster tree and the status bar say, derived rather than assigned.
 *
 * The rules under test all bend the same way: the tree must never claim more than the hostility
 * matrix knows. Filing an unresolved third party under ALLIED would tell the player someone is on
 * his side, which is exactly the claim the matrix refused to make when it marked the pair assumed.
 */
public class ScenarioRosterTest {

    private static TipoTropa troopType(String codigo, boolean ships) {
        final TipoTropa ret = new TipoTropa();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        if (ships) {
            final Habilidade hab = new Habilidade();
            hab.setCodigo(";TTN;");
            hab.setNome(";TTN;");
            ret.addHabilidade(hab);
        }
        return ret;
    }

    private static Pelotao platoon(TipoTropa tipo, int qtd) {
        final Pelotao ret = new Pelotao();
        ret.setTipoTropa(tipo);
        ret.setQtd(qtd);
        return ret;
    }

    private static Jogador jogador(String codigo) {
        final Jogador ret = new Jogador();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        return ret;
    }

    private static Nacao nacao(String codigo, Jogador owner) {
        final Nacao ret = new Nacao();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        if (owner != null) {
            ret.setOwner(owner);
        }
        return ret;
    }

    private static Terreno terreno() {
        final Terreno ret = new Terreno();
        ret.setCodigo("P");
        ret.setNome("Plain");
        ret.setAncoravel(true);
        return ret;
    }

    private static Local local() {
        final Local ret = new Local();
        ret.setCodigo("1428");
        ret.setCoordenadas("1428");
        ret.setTerreno(terreno());
        return ret;
    }

    private static ArmySim army(String nome, Nacao nacao, Pelotao... pelotoes) {
        final ArmySim ret = new ArmySim(nome, terreno(), nacao);
        ret.setCodigo(nome);
        for (Pelotao pelotao : pelotoes) {
            ret.getPelotoes().put(pelotao.getCodigo(), pelotao);
        }
        return ret;
    }

    private static Partida partida(String habilidades) {
        final Partida ret = new Partida();
        ret.setCodigo("g1");
        ret.setNome("g1");
        for (String cd : habilidades.split(",")) {
            if (!cd.isEmpty()) {
                final Habilidade hab = new Habilidade();
                hab.setCodigo(cd);
                hab.setNome(cd);
                ret.addHabilidade(hab);
            }
        }
        return ret;
    }

    private static void relate(Nacao from, Nacao to, int valor) {
        from.getRelacionamentos().put(to, valor);
    }

    @Test
    public void myArmiesAndMyEnemiesLandInTheRightNodes() {
        final Jogador me = jogador("j1");
        final Nacao mine = nacao("m", me), foe = nacao("f", null);
        relate(mine, foe, -2);

        final CombatScenario s = new CombatScenario(partida(";FFA;"), local());
        s.setObserver(me);
        final ArmySim ours = army("ours", mine, platoon(troopType("inf", false), 900));
        final ArmySim theirs = army("theirs", foe, platoon(troopType("einf", false), 500));
        s.addArmy(ours, CombatScenario.Provenance.EXACT);
        s.addArmy(theirs, CombatScenario.Provenance.ESTIMATED);

        final ScenarioRoster roster = ScenarioRoster.of(s);

        assertEquals(ScenarioRoster.Group.MINE, roster.getGroup(ours));
        assertEquals(ScenarioRoster.Group.HOSTILE, roster.getGroup(theirs));
        assertEquals(Arrays.asList(ScenarioRoster.Group.MINE, ScenarioRoster.Group.HOSTILE),
                roster.getGroups(), "empty groups are not tree nodes");
        assertEquals(900, roster.getQtTropas(ScenarioRoster.Group.MINE));
        assertEquals(500, roster.getQtTropas(ScenarioRoster.Group.HOSTILE));
    }

    /**
     * The reason there are four groups. An unresolved third party is NOT an ally, and saying so was
     * the whole point of marking the pair assumed one layer down.
     */
    @Test
    public void anUnresolvedThirdPartyIsNeutralNotAllied() {
        final Jogador me = jogador("j1");
        final Nacao mine = nacao("m", me), other = nacao("x", null);
        // my EGF says nothing about x either way, so the pair is assumed not hostile

        final CombatScenario s = new CombatScenario(partida(";FFA;"), local());
        s.setObserver(me);
        final ArmySim ours = army("ours", mine, platoon(troopType("inf", false), 900));
        final ArmySim stranger = army("stranger", other, platoon(troopType("xinf", false), 500));
        s.addArmy(ours, CombatScenario.Provenance.EXACT);
        s.addArmy(stranger, CombatScenario.Provenance.ESTIMATED);

        assertEquals(ScenarioRoster.Group.NEUTRAL, ScenarioRoster.of(s).getGroup(stranger));
        assertTrue(s.getMatrix().isAssumed(ours, stranger), "and the matrix still says it guessed");
    }

    /** A merged EGF is the one positive signal the client actually holds. */
    @Test
    public void anAllyIsOneWhoseOwnEgfIsHere() {
        final Jogador me = jogador("j1");
        final Nacao mine = nacao("m", me), ally = nacao("a", jogador("j2"));
        relate(mine, ally, 2);

        final CombatScenario s = new CombatScenario(partida(";FFA;"), local());
        s.setObserver(me);
        final ArmySim ours = army("ours", mine, platoon(troopType("inf", false), 900));
        final ArmySim friend = army("friend", ally, platoon(troopType("ainf", false), 300));
        s.addArmy(ours, CombatScenario.Provenance.EXACT);
        s.addArmy(friend, CombatScenario.Provenance.ESTIMATED);

        assertEquals(ScenarioRoster.Group.NEUTRAL, ScenarioRoster.of(s).getGroup(friend),
                "a friendly relationship alone does not make its EGF present");

        s.addMergedNacao(ally);

        assertEquals(ScenarioRoster.Group.ALLIED, ScenarioRoster.of(s).getGroup(friend));
        assertEquals(300, ScenarioRoster.of(s).getQtTropas(ScenarioRoster.Group.ALLIED));
    }

    /**
     * Watching someone else's battle. With no army of the observer's own present, "not hostile to
     * mine" is vacuously true of everybody, so nobody may be called an ally on the strength of it.
     */
    @Test
    public void withNoArmyOfMyOwnNobodyIsAnAlly() {
        final Jogador me = jogador("j1");
        final Nacao x = nacao("x", null), y = nacao("y", null);
        final CombatScenario s = new CombatScenario(partida(";GDM;"), local());
        s.setObserver(me);
        s.addMergedNacao(x);
        final ArmySim one = army("one", x, platoon(troopType("xinf", false), 900));
        final ArmySim two = army("two", y, platoon(troopType("yinf", false), 500));
        s.addArmy(one, CombatScenario.Provenance.ESTIMATED);
        s.addArmy(two, CombatScenario.Provenance.ESTIMATED);

        final ScenarioRoster roster = ScenarioRoster.of(s);

        assertEquals(ScenarioRoster.Group.NEUTRAL, roster.getGroup(one));
        assertEquals(ScenarioRoster.Group.NEUTRAL, roster.getGroup(two));
        assertTrue(s.hasCombat(), "they are still fighting each other, just not me");
    }

    @Test
    public void aDeathMatchNeedsNoRowsAndGuessesNothing() {
        final Jogador me = jogador("j1");
        final CombatScenario s = new CombatScenario(partida(";GDM;"), local());
        s.setObserver(me);
        s.addArmy(army("a", nacao("m", me), platoon(troopType("inf", false), 900)),
                CombatScenario.Provenance.EXACT);
        s.addArmy(army("b", nacao("f", null), platoon(troopType("einf", false), 500)),
                CombatScenario.Provenance.ESTIMATED);

        final RosterDerivation d = RosterDerivation.of(s);

        assertEquals(RosterDerivation.Basis.GAME_TYPE, d.getBasis());
        assertEquals(0, d.getAssumedPairs());
        assertEquals(1, d.getPairsFromGameType());
        assertFalse(d.isDiplomacyEditable(), "diplomacy is disabled in a Death Match, not floating");
    }

    @Test
    public void aFreeForAllReportsHowManyPairsItGuessed() {
        final Jogador me = jogador("j1");
        final Nacao mine = nacao("m", me), x = nacao("x", null), y = nacao("y", null);
        relate(mine, x, -2);
        relate(mine, y, -2);

        final CombatScenario s = new CombatScenario(partida(";FFA;"), local());
        s.setObserver(me);
        s.addArmy(army("me", mine, platoon(troopType("inf", false), 900)),
                CombatScenario.Provenance.EXACT);
        s.addArmy(army("x1", x, platoon(troopType("xinf", false), 500)),
                CombatScenario.Provenance.ESTIMATED);
        s.addArmy(army("y1", y, platoon(troopType("yinf", false), 500)),
                CombatScenario.Provenance.ESTIMATED);

        final RosterDerivation d = RosterDerivation.of(s);

        assertEquals(RosterDerivation.Basis.PARTLY_ASSUMED, d.getBasis());
        assertEquals(1, d.getAssumedPairs(), "x versus y is the pair nothing can answer");
        assertEquals(2, d.getReadPairs());
        assertTrue(d.isDiplomacyEditable(), "FFA is where the post-MVP matrix editor belongs");
    }

    @Test
    public void everythingReadAndNothingLeftOverSaysSo() {
        final Jogador me = jogador("j1");
        final Nacao mine = nacao("m", me), foe = nacao("f", null);
        relate(mine, foe, -2);

        final CombatScenario s = new CombatScenario(partida(";GLA;"), local());
        s.setObserver(me);
        s.addArmy(army("me", mine, platoon(troopType("inf", false), 900)),
                CombatScenario.Provenance.EXACT);
        s.addArmy(army("f1", foe, platoon(troopType("einf", false), 500)),
                CombatScenario.Provenance.ESTIMATED);

        final RosterDerivation d = RosterDerivation.of(s);

        assertEquals(RosterDerivation.Basis.ALL_READ, d.getBasis());
        assertEquals(0, d.getAssumedPairs());
        assertFalse(d.isDiplomacyEditable(), "locked teams are fully determined by the rules");
    }

    /**
     * The platoon table's Lyr column. By the PLATOON's troop type, not by isEsquadra, which is an
     * army-level predicate requiring a quantity above zero - a ship platoon the player has just
     * emptied must not flicker to A while he retypes the number.
     */
    @Test
    public void theLayerColumnReadsThePlatoonsOwnTroopType() {
        assertEquals("N", CombatLayer.of(troopType("sh", true)).getBadge());
        assertEquals("A", CombatLayer.of(troopType("inf", false)).getBadge());
        assertEquals("N", CombatLayer.of(troopType("sh", true)).getBadge(),
                "an emptied ship platoon is still a ship platoon");
        assertEquals("A", CombatLayer.of(null).getBadge());
    }

    /** The army editor's "Fights in:" line, the badge in a form that can become words. */
    @Test
    public void fightsInListsTheLayersBehindTheBadge() {
        final Jogador me = jogador("j1");
        final Nacao mine = nacao("m", me), foe = nacao("f", null);
        final CombatScenario s = new CombatScenario(partida(";GDM;"), local());
        s.setObserver(me);
        final ArmySim fleet = army("fleet", mine,
                platoon(troopType("sh", true), 40), platoon(troopType("inf", false), 600));
        final ArmySim enemyFleet = army("efleet", foe,
                platoon(troopType("esh", true), 30), platoon(troopType("einf", false), 500));
        s.addArmy(fleet, CombatScenario.Provenance.EXACT);
        s.addArmy(enemyFleet, CombatScenario.Provenance.ESTIMATED);

        final LayerParticipation p = s.getParticipation().get(fleet);

        assertEquals("NA.", p.getBadge());
        assertEquals(Arrays.asList(CombatLayer.NAVY, CombatLayer.ARMY), p.getLayers());
    }
}
