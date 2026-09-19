package business.combat;

import model.Cidade;
import model.Habilidade;
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
 * The scenario object: what a BattleSim window owns, and how trustworthy it says its numbers are.
 *
 * Two things are pinned here that are easy to get wrong later. City participation defaults OFF when
 * the hex has no city, rather than standing a dummy city in - the dummy is what forced the old damage
 * code to special-case a null nation. And provenance never lands on {@code Pelotao}, because that is
 * a {@code model.*} class serialized into the EGF and this is scratch state.
 */
public class CombatScenarioTest {

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

    private static Nacao nacao(String codigo) {
        final Nacao ret = new Nacao();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        return ret;
    }

    private static Terreno terreno(String codigo) {
        final Terreno ret = new Terreno();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        return ret;
    }

    private static Local local(Terreno t, Cidade c) {
        final Local ret = new Local();
        ret.setCodigo("1428");
        ret.setCoordenadas("1428");
        ret.setTerreno(t);
        if (c != null) {
            ret.setCidade(c);
        }
        return ret;
    }

    private static ArmySim army(String nome, Nacao nacao, Pelotao... pelotoes) {
        final ArmySim ret = new ArmySim(nome, null, nacao);
        ret.setCodigo(nome);
        for (Pelotao pelotao : pelotoes) {
            ret.getPelotoes().put(pelotao.getCodigo(), pelotao);
        }
        return ret;
    }

    private static Partida deathMatch() {
        final Partida ret = new Partida();
        ret.setCodigo("g1");
        ret.setNome("g1");
        final Habilidade hab = new Habilidade();
        hab.setCodigo(";GDM;");
        hab.setNome(";GDM;");
        ret.addHabilidade(hab);
        return ret;
    }

    @Test
    public void aHexWithNoCityDoesNotFightOne() {
        final CombatScenario s = new CombatScenario(deathMatch(), local(terreno("plain"), null));

        assertFalse(s.isCityParticipates(), "no city means no city layer, not a dummy one");
        assertEquals(null, s.getCidadeAtiva());
    }

    @Test
    public void aHexWithACityFightsItByDefaultButCanBeTurnedOff() {
        final Nacao owner = nacao("o");
        final Cidade city = new Cidade();
        city.setCodigo("c1");
        city.setNome("City");
        city.setNacao(owner);

        final CombatScenario s = new CombatScenario(deathMatch(), local(terreno("plain"), city));
        assertTrue(s.isCityParticipates());

        s.setCityParticipates(false);
        assertFalse(s.isCityParticipates(), "the player can ask what happens if he ignores the city");
        assertEquals(null, s.getCidadeAtiva());
    }

    @Test
    public void provenanceIsRememberedPerArmyAndPerPlatoon() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final Pelotao ours = platoon(troopType("inf", false), 900);
        final Pelotao theirs = platoon(troopType("inf", false), 500);
        final ArmySim ourArmy = army("ours", mine, ours);
        final ArmySim theirArmy = army("theirs", foe, theirs);

        final CombatScenario s = new CombatScenario(deathMatch(), local(terreno("plain"), null));
        s.addArmy(ourArmy, CombatScenario.Provenance.EXACT);
        s.addArmy(theirArmy, CombatScenario.Provenance.ESTIMATED);

        assertEquals(CombatScenario.Provenance.EXACT, s.getProvenance(ourArmy));
        assertEquals(CombatScenario.Provenance.EXACT, s.getProvenance(ours));
        assertEquals(CombatScenario.Provenance.ESTIMATED, s.getProvenance(theirArmy));
        assertEquals(CombatScenario.Provenance.ESTIMATED, s.getProvenance(theirs));
    }

    @Test
    public void editingAnEstimateMakesItThePlayersOwnNumber() {
        final Pelotao guess = platoon(troopType("inf", false), 500);
        final ArmySim enemy = army("enemy", nacao("f"), guess);
        final CombatScenario s = new CombatScenario(deathMatch(), local(terreno("plain"), null));
        s.addArmy(enemy, CombatScenario.Provenance.ESTIMATED);

        s.setEdited(guess);

        assertEquals(CombatScenario.Provenance.MANUAL, s.getProvenance(guess));
    }

    /** A platoon the player adds himself was never in any EGF, so its numbers are his. */
    @Test
    public void aPlatoonNobodyTrackedIsThePlayersOwn() {
        final CombatScenario s = new CombatScenario(deathMatch(), local(terreno("plain"), null));
        assertEquals(CombatScenario.Provenance.MANUAL,
                s.getProvenance(platoon(troopType("inf", false), 1)));
    }

    @Test
    public void theMatrixIsRebuiltWhenTheRosterChanges() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim ourArmy = army("ours", mine, platoon(troopType("inf", false), 900));
        final CombatScenario s = new CombatScenario(deathMatch(), local(terreno("plain"), null));
        s.addArmy(ourArmy, CombatScenario.Provenance.EXACT);

        assertFalse(s.hasCombat(), "one army has nobody to fight");

        s.addArmy(army("theirs", foe, platoon(troopType("inf", false), 500)),
                CombatScenario.Provenance.ESTIMATED);

        assertTrue(s.hasCombat(), "a stale matrix would still say there is no fight");
    }

    @Test
    public void layerMembershipIsReportedPerLayer() {
        final Nacao mine = nacao("m"), foe = nacao("f");
        final ArmySim fleet = army("fleet", mine, platoon(troopType("sh", true), 40));
        final ArmySim land = army("land", foe, platoon(troopType("inf", false), 500));

        final CombatScenario s = new CombatScenario(deathMatch(), local(terreno("plain"), null));
        s.addArmy(fleet, CombatScenario.Provenance.EXACT);
        s.addArmy(land, CombatScenario.Provenance.ESTIMATED);

        assertEquals(1, s.getArmies(CombatLayer.NAVY).size());
        assertTrue(s.getArmies(CombatLayer.NAVY).contains(fleet));
        assertFalse(s.getArmies(CombatLayer.ARMY).contains(fleet),
                "a fleet with no troops does not fight ashore");
        assertEquals(540, s.getQtTropasTotal());
    }

    @Test
    public void removingAnArmyForgetsItsProvenanceToo() {
        final Pelotao pelotao = platoon(troopType("inf", false), 900);
        final ArmySim ourArmy = army("ours", nacao("m"), pelotao);
        final CombatScenario s = new CombatScenario(deathMatch(), local(terreno("plain"), null));
        s.addArmy(ourArmy, CombatScenario.Provenance.EXACT);

        s.remArmy(ourArmy);

        assertEquals(0, s.getArmies().size());
        assertEquals(CombatScenario.Provenance.MANUAL, s.getProvenance(pelotao),
                "a forgotten platoon must not keep claiming it came from an EGF");
    }
}
