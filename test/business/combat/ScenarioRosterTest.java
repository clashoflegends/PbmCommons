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
 * The matrix is the only input. Not teams, not alliances, not whose file is loaded, not any notion
 * of a "side" - the Judge does not model sides either, it gives each army a list of enemies it takes
 * damage from and that is the whole of it. So every assertion here reduces to a matrix lookup.
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
        assertEquals(ScenarioRoster.Group.FIGHTING_AGAINST_ME, roster.getGroup(theirs));
        assertEquals(Arrays.asList(ScenarioRoster.Group.MINE,
                ScenarioRoster.Group.FIGHTING_AGAINST_ME),
                roster.getGroups(), "empty groups are not tree nodes");
        assertEquals(900, roster.getQtTropas(ScenarioRoster.Group.MINE));
        assertEquals(500, roster.getQtTropas(ScenarioRoster.Group.FIGHTING_AGAINST_ME));
    }

    /**
     * Fighting WITH me is read off the matrix like everything else: it is not hostile to me, and it
     * is hostile to something that is. No alliance, no team, no loaded file comes into it.
     */
    @Test
    public void anArmyFightingMyEnemyIsFightingWithMe() {
        final Jogador me = jogador("j1");
        final Nacao mine = nacao("m", me), foe = nacao("f", null), friend = nacao("a", null);
        relate(mine, foe, -2);
        relate(mine, friend, 2);

        final CombatScenario s = new CombatScenario(partida(";FFA;"), local());
        s.setObserver(me);
        final ArmySim ours = army("ours", mine, platoon(troopType("inf", false), 900));
        final ArmySim enemy = army("enemy", foe, platoon(troopType("einf", false), 500));
        final ArmySim other = army("other", friend, platoon(troopType("ainf", false), 300));
        s.addArmy(ours, CombatScenario.Provenance.EXACT);
        s.addArmy(enemy, CombatScenario.Provenance.ESTIMATED);
        s.addArmy(other, CombatScenario.Provenance.ESTIMATED);

        // nothing in my EGF says the third nation fights my enemy, so it does not - yet
        assertEquals(ScenarioRoster.Group.NOT_FIGHTING_ME, ScenarioRoster.of(s).getGroup(other));

        // the matrix is the law, and the player may state it
        s.setHostile(other, enemy, true);

        assertEquals(ScenarioRoster.Group.FIGHTING_WITH_ME, ScenarioRoster.of(s).getGroup(other));
        assertEquals(300, ScenarioRoster.of(s).getQtTropas(ScenarioRoster.Group.FIGHTING_WITH_ME));
    }

    /** A friendly relationship on its own puts nobody in the battle. */
    @Test
    public void beingOnGoodTermsWithMeIsNotFightingWithMe() {
        final Jogador me = jogador("j1");
        final Nacao mine = nacao("m", me), friend = nacao("a", null);
        relate(mine, friend, 2);

        final CombatScenario s = new CombatScenario(partida(";FFA;"), local());
        s.setObserver(me);
        s.addArmy(army("ours", mine, platoon(troopType("inf", false), 900)),
                CombatScenario.Provenance.EXACT);
        final ArmySim other = army("other", friend, platoon(troopType("ainf", false), 300));
        s.addArmy(other, CombatScenario.Provenance.ESTIMATED);

        assertEquals(ScenarioRoster.Group.NOT_FIGHTING_ME, ScenarioRoster.of(s).getGroup(other));
    }

    /**
     * Watching someone else's battle. With no army of the observer's own there is no "me" to be with
     * or against, so everyone is outside his battle - and the fight between them is still reported.
     */
    @Test
    public void withNoArmyOfMyOwnNobodyIsWithOrAgainstMe() {
        final Jogador me = jogador("j1");
        final Nacao x = nacao("x", null), y = nacao("y", null);
        final CombatScenario s = new CombatScenario(partida(";GDM;"), local());
        s.setObserver(me);
        final ArmySim one = army("one", x, platoon(troopType("xinf", false), 900));
        final ArmySim two = army("two", y, platoon(troopType("yinf", false), 500));
        s.addArmy(one, CombatScenario.Provenance.ESTIMATED);
        s.addArmy(two, CombatScenario.Provenance.ESTIMATED);

        final ScenarioRoster roster = ScenarioRoster.of(s);

        assertEquals(ScenarioRoster.Group.NOT_FIGHTING_ME, roster.getGroup(one));
        assertEquals(ScenarioRoster.Group.NOT_FIGHTING_ME, roster.getGroup(two));
        assertTrue(s.hasCombat(), "they are still fighting each other, just not me");
        assertTrue(s.getMatrix().isInimigo(one, two));
    }

    /**
     * The player outranks the derivation. "Suppose he declares on me this turn" is a legitimate
     * what-if, and the edit has to survive the matrix being rebuilt - which it is, on every call.
     */
    @Test
    public void aPlayerEditOutranksTheDerivationAndSurvivesRebuilds() {
        final Jogador me = jogador("j1");
        final Nacao mine = nacao("m", me), other = nacao("x", null);

        final CombatScenario s = new CombatScenario(partida(";FFA;"), local());
        s.setObserver(me);
        final ArmySim ours = army("ours", mine, platoon(troopType("inf", false), 900));
        final ArmySim theirs = army("theirs", other, platoon(troopType("xinf", false), 500));
        s.addArmy(ours, CombatScenario.Provenance.EXACT);
        s.addArmy(theirs, CombatScenario.Provenance.ESTIMATED);

        assertEquals(ScenarioRoster.Group.NOT_FIGHTING_ME, ScenarioRoster.of(s).getGroup(theirs));

        s.setHostile(ours, theirs, true);

        assertEquals(ScenarioRoster.Group.FIGHTING_AGAINST_ME, ScenarioRoster.of(s).getGroup(theirs));
        assertEquals(HostilityMatrix.Origin.PLAYER_EDITED, s.getMatrix().getOrigin(ours, theirs));
        assertEquals(1, s.getEditedCount());
        assertTrue(s.getMatrix().isInimigo(ours, theirs), "still there on a second rebuild");

        s.clearHostilityEdits();

        assertEquals(ScenarioRoster.Group.NOT_FIGHTING_ME, ScenarioRoster.of(s).getGroup(theirs));
        assertEquals(0, s.getEditedCount());
    }

    /** An edit can also call off a fight the rules would impose. */
    @Test
    public void aPlayerEditCanMakePeaceInADeathMatch() {
        final Jogador me = jogador("j1");
        final CombatScenario s = new CombatScenario(partida(";GDM;"), local());
        s.setObserver(me);
        final ArmySim ours = army("ours", nacao("m", me), platoon(troopType("inf", false), 900));
        final ArmySim theirs = army("theirs", nacao("f", null), platoon(troopType("einf", false), 500));
        s.addArmy(ours, CombatScenario.Provenance.EXACT);
        s.addArmy(theirs, CombatScenario.Provenance.ESTIMATED);

        assertTrue(s.getMatrix().isInimigo(ours, theirs));

        s.setHostile(ours, theirs, false);

        assertFalse(s.getMatrix().isInimigo(ours, theirs));
        assertFalse(s.hasCombat(), "nobody left to fight");
        assertEquals(ScenarioRoster.Group.NOT_FIGHTING_ME, ScenarioRoster.of(s).getGroup(theirs));
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
