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

    /** One node per nation, its own armies under it, its own troop total. */
    @Test
    public void armiesAreGroupedByTheNationThatOwnsThem() {
        final Jogador me = jogador("j1");
        final Nacao mine = nacao("m", me), foe = nacao("f", null);
        relate(mine, foe, -2);

        final CombatScenario s = new CombatScenario(partida(";FFA;"), local());
        s.setObserver(me);
        final ArmySim ours = army("ours", mine, platoon(troopType("inf", false), 900));
        final ArmySim more = army("more", mine, platoon(troopType("inf2", false), 100));
        final ArmySim theirs = army("theirs", foe, platoon(troopType("einf", false), 500));
        for (ArmySim one : new ArmySim[]{ours, more, theirs}) {
            s.addArmy(one, CombatScenario.Provenance.ESTIMATED);
        }

        final ScenarioRoster roster = ScenarioRoster.of(s);

        assertEquals(Arrays.asList(mine, foe), roster.getNacoes(), "hex order, and no duplicates");
        assertEquals(Arrays.asList(ours, more), roster.getArmies(mine));
        assertEquals(Arrays.asList(theirs), roster.getArmies(foe));
        assertEquals(1000, roster.getQtTropas(mine));
        assertEquals(500, roster.getQtTropas(foe));
    }

    /**
     * Each nation node carries who it fights, and that is not optional decoration.
     *
     * The four me-relative groups this replaced were the ONLY place in the window that said who
     * fights whom. Dropping them without this would have been a loss, not a simplification.
     */
    @Test
    public void eachNationNodeNamesTheNationsItFightsHere() {
        final Nacao one = nacao("one", null), two = nacao("two", null), bystander = nacao("by", null);
        relate(one, two, -2);

        // ;SPD;, because a FOREIGN row is only believed under public diplomacy - otherwise
        // HostilityDeriver cannot prove it complete and the pair falls to the assumed default.
        // The grouping is what is under test here, not the derivation.
        final CombatScenario s = new CombatScenario(partida(";FFA;,;SPD;"), local());
        s.addArmy(army("a", one, platoon(troopType("i1", false), 100)),
                CombatScenario.Provenance.ESTIMATED);
        s.addArmy(army("b", two, platoon(troopType("i2", false), 100)),
                CombatScenario.Provenance.ESTIMATED);
        s.addArmy(army("c", bystander, platoon(troopType("i3", false), 100)),
                CombatScenario.Provenance.ESTIMATED);

        final ScenarioRoster roster = ScenarioRoster.of(s);

        assertEquals(Arrays.asList(two), roster.getEnemies(one));
        assertEquals(Arrays.asList(one), roster.getEnemies(two),
                "hostility is the OR of both directions, as the Judge reads it");
        assertTrue(roster.getEnemies(bystander).isEmpty(),
                "a nation at war with nobody here gets no clause at all");
    }

    /**
     * THE REGRESSION THIS CHANGE EXISTS FOR: with no army of the observer's own, the tree still
     * names both nations and the war between them.
     *
     * It used to collapse into one node headed "Not fighting me" over a battle it was simulating -
     * game 802 turn 40 hex 1530, watched by a third party, was exactly that.
     */
    @Test
    public void aBattleTheObserverIsNotInStillReadsAsTwoNationsAtWar() {
        final Jogador me = jogador("j1");
        final Nacao one = nacao("one", null), two = nacao("two", null);
        relate(one, two, -2);

        final CombatScenario s = new CombatScenario(partida(";FFA;,;SPD;"), local());
        s.setObserver(me);          // owns neither nation on this hex
        final ArmySim a = army("a", one, platoon(troopType("i1", false), 400));
        final ArmySim b = army("b", two, platoon(troopType("i2", false), 600));
        s.addArmy(a, CombatScenario.Provenance.ESTIMATED);
        s.addArmy(b, CombatScenario.Provenance.ESTIMATED);

        final ScenarioRoster roster = ScenarioRoster.of(s);

        assertEquals(2, roster.getNacoes().size(), "two nations, two nodes - not one bucket");
        assertEquals(Arrays.asList(two), roster.getEnemies(one));
        assertEquals(400, roster.getQtTropas(one));
        assertEquals(600, roster.getQtTropas(two));
        assertTrue(s.hasCombat(), "and they really are fighting");
    }

    /** A player's declaration reaches the nation nodes, and survives a rebuild. */
    @Test
    public void aPlayerEditOutranksTheDerivationAndSurvivesRebuilds() {
        final Jogador me = jogador("j1");
        final Nacao mine = nacao("m", me), foe = nacao("f", null);

        final CombatScenario s = new CombatScenario(partida(";FFA;"), local());
        s.setObserver(me);
        s.addArmy(army("ours", mine, platoon(troopType("inf", false), 900)),
                CombatScenario.Provenance.EXACT);
        s.addArmy(army("theirs", foe, platoon(troopType("einf", false), 500)),
                CombatScenario.Provenance.ESTIMATED);

        // Nothing can be read about the foreign nation, and the last-resort default is deliberately
        // the worst case FOR THE OBSERVER - so they start out assumed hostile, not assumed at peace.
        assertEquals(Arrays.asList(foe), ScenarioRoster.of(s).getEnemies(mine),
                "unreadable, so assumed against him");

        s.setRelacionamento(mine, foe, RelationshipMatrix.NEUTRAL);
        s.setRelacionamento(foe, mine, RelationshipMatrix.NEUTRAL);
        assertTrue(ScenarioRoster.of(s).getEnemies(mine).isEmpty(),
                "the player said otherwise, and the roster is rebuilt from the matrix every time");

        s.clearHostilityEdits();
        assertEquals(Arrays.asList(foe), ScenarioRoster.of(s).getEnemies(mine),
                "reset puts the assumption back");
    }

    /** And peace can be declared even where the game type says everyone fights. */
    @Test
    public void aPlayerEditCanMakePeaceInADeathMatch() {
        final Nacao mine = nacao("m", null), foe = nacao("f", null);

        final CombatScenario s = new CombatScenario(partida(";GDM;"), local());
        s.addArmy(army("ours", mine, platoon(troopType("inf", false), 900)),
                CombatScenario.Provenance.EXACT);
        s.addArmy(army("theirs", foe, platoon(troopType("einf", false), 500)),
                CombatScenario.Provenance.ESTIMATED);

        assertEquals(Arrays.asList(foe), ScenarioRoster.of(s).getEnemies(mine),
                "a Death Match makes every pair hostile by construction");

        s.setRelacionamento(mine, foe, RelationshipMatrix.NEUTRAL);
        s.setRelacionamento(foe, mine, RelationshipMatrix.NEUTRAL);
        assertTrue(ScenarioRoster.of(s).getEnemies(mine).isEmpty(),
                "and the player still outranks it");
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

        assertEquals("N A \u00b7", p.getBadge());
        assertEquals(Arrays.asList(CombatLayer.NAVY, CombatLayer.ARMY), p.getLayers());
    }
}
