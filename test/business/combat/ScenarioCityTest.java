package business.combat;

import business.facade.BattleSimFacade;
import model.Cidade;
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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The city as a combat participant: the C of the N A C badge.
 *
 * A city is PASSIVE. Armies attack it, it damages the attackers, and it takes the result. It is not
 * an army and it does not choose anything, so the only questions here are what it is worth and
 * whether the player can retype it without touching the world.
 *
 * The city layer is TWO rounds, from {@code CombateTmpbm.executaCombateCidade}: round 0 is siege
 * engines against the fortification, fought only when an attacker carries them, and round 1 is the
 * single army-versus-city exchange. Round 0 comes first and can reduce the fortification, which is
 * why the two numbers are reported separately - it is the city's equivalent of the army layer's
 * first-strike round.
 */
public class ScenarioCityTest {

    private static Nacao nacao(String codigo, Jogador owner) {
        final Nacao ret = new Nacao();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        if (owner != null) {
            ret.setOwner(owner);
        }
        return ret;
    }

    private static Jogador jogador(String codigo) {
        final Jogador ret = new Jogador();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        return ret;
    }

    private static Terreno terreno() {
        final Terreno ret = new Terreno();
        ret.setCodigo("P");
        ret.setNome("Plain");
        ret.setAncoravel(true);
        return ret;
    }

    private static Cidade cidade(Nacao owner, int tamanho, int fortificacao, int lealdade) {
        final Cidade ret = new Cidade();
        ret.setCodigo("c1");
        ret.setNome("Seagard");
        ret.setNacao(owner);
        ret.setTamanho(tamanho);
        ret.setFortificacao(fortificacao);
        ret.setLealdade(lealdade);
        return ret;
    }

    private static Local hex(Cidade cidade) {
        final Local ret = new Local();
        ret.setCodigo("1141");
        ret.setCoordenadas("1141");
        ret.setTerreno(terreno());
        if (cidade != null) {
            ret.setCidade(cidade);
            cidade.setLocal(ret);
        }
        return ret;
    }

    private static TipoTropa troopType(String codigo, String habilidade) {
        final TipoTropa ret = new TipoTropa();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        if (habilidade != null) {
            final Habilidade hab = new Habilidade();
            hab.setCodigo(habilidade);
            hab.setNome(habilidade);
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

    private static ArmySim army(String nome, Nacao nacao, CombatLevel level, Pelotao... pelotoes) {
        final ArmySim ret = new ArmySim(nome, terreno(), nacao);
        ret.setCodigo(nome);
        ret.setCombatLevel(level);
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

    /**
     * The ownership boundary, one level up from the platoons.
     *
     * Loyalty, size and fortification are all editable, so the same sharing that let a retyped
     * platoon reach the loaded world would let a retyped city do it. The scenario clones.
     */
    @Test
    public void retypingTheCityDoesNotTouchTheOneLoadedFromTheEgf() {
        final Cidade real = cidade(nacao("o", null), 3, 3, 100);
        final CombatScenario s = new CombatScenario(deathMatch(), hex(real));

        assertNotSame(real, s.getCidade(), "the scenario owns its city");

        s.setCityLealdade(0);
        s.setCityTamanho(5);
        s.setCityFortificacao(5);

        assertEquals(0, s.getCidade().getLealdade());
        assertEquals(5, s.getCidade().getTamanho());
        assertEquals(5, s.getCidade().getFortificacao());
        assertEquals(100, real.getLealdade(), "the loaded city must be untouched");
        assertEquals(3, real.getTamanho());
        assertEquals(3, real.getFortificacao());
    }

    @Test
    public void theThreeInputsAreClampedToTheirRealRanges() {
        final CombatScenario s = new CombatScenario(deathMatch(), hex(cidade(nacao("o", null), 3, 3, 50)));

        s.setCityLealdade(500);
        s.setCityTamanho(99);
        s.setCityFortificacao(-4);

        assertEquals(100, s.getCidade().getLealdade());
        assertEquals(5, s.getCidade().getTamanho());
        assertEquals(0, s.getCidade().getFortificacao());
    }

    /**
     * The parity point, and the bug the old window has.
     *
     * The old BattleSim shows {@code getCityDefense(t, f, l)}, the BASE. The Judge's combat uses
     * {@code getCityDefenseCombat}, which also applies four nation powers - {@code ;PFD;} when
     * fortified, {@code ;PCD;} in mountains, {@code ;NWD;} in forest, {@code ;NWS;} in swamp.
     * Showing the base understates the defender, which is wrong in the ATTACKER's favour, the one
     * direction this tool must never be wrong in.
     *
     * ({@code defenseBonus} is in that formula too but is unreachable from here:
     * {@code Cidade.getDefenseBonus()} is hardcoded to 0 with no-op setters, so it is Judge-side
     * state that never rides the EGF. The nation powers are the whole of the client-side gap.)
     */
    @Test
    public void defenseIsTheCombatFigureNotTheBase() {
        final Nacao owner = nacao("o", null);
        final Habilidade fortified = new Habilidade();
        fortified.setCodigo(";PFD;");
        fortified.setNome(";PFD;");
        fortified.setValor(50);
        owner.addHabilidade(fortified);

        final Cidade city = cidade(owner, 3, 3, 100);
        final CombatScenario s = new CombatScenario(deathMatch(), hex(city));
        final BattleSimFacade facade = new BattleSimFacade();

        final int base = facade.getCityDefense(3, 3, 100);
        assertEquals(base + facade.getCityFortficationDefense(city) * 50 / 100, s.getCityDefense(),
                "a defender's fortification power is part of what the attackers actually face");
        assertTrue(s.getCityDefense() > base, "and the old window's base figure understates it");
    }

    /** A city at zero loyalty defends at DOUBLE, which is the opposite of most people's guess. */
    @Test
    public void zeroLoyaltyDoublesTheDefenceRatherThanRemovingIt() {
        final CombatScenario s = new CombatScenario(deathMatch(), hex(cidade(nacao("o", null), 3, 3, 100)));
        final int atFullLoyalty = s.getCityDefense();

        s.setCityLealdade(0);

        assertEquals(atFullLoyalty, s.getCityDefense(),
                "100 percent loyalty and 0 both double the base, by the formula's own branch");
    }

    @Test
    public void aHexWithNoCityHasNoDefenceAndNoSiege() {
        final CombatScenario s = new CombatScenario(deathMatch(), hex(null));

        assertEquals(0, s.getCityDefense());
        assertEquals(0, s.getCityFortificationDefense());
        assertFalse(s.isSiegeExpected());
        assertFalse(s.isCityParticipates());
    }

    /** Turning the city off is a what-if, and it must take its defence out of the fight with it. */
    @Test
    public void aCityTheP1ayerSwitchesOffContributesNothing() {
        final CombatScenario s = new CombatScenario(deathMatch(), hex(cidade(nacao("o", null), 3, 3, 100)));
        assertTrue(s.getCityDefense() > 0);

        s.setCityParticipates(false);

        assertEquals(0, s.getCityDefense());
        assertEquals(0, s.getCityFortificationDefense());
    }

    /**
     * Round 0 is fought only when an army that is ACTUALLY assaulting the city brings siege engines.
     * An army with engines that is not attacking the city does not bring them to the walls.
     */
    @Test
    public void roundZeroNeedsASiegeEngineOnAnArmyThatIsAssaultingTheCity() {
        final Jogador me = jogador("j1");
        final Nacao mine = nacao("m", me), owner = nacao("o", null);
        final CombatScenario s = new CombatScenario(deathMatch(), hex(cidade(owner, 3, 3, 100)));
        s.setObserver(me);

        final ArmySim besieger = army("besieger", mine, CombatLevel.ATTACK_CITY,
                platoon(troopType("ram", ";TTS;"), 10), platoon(troopType("inf", null), 900));
        s.addArmy(besieger, CombatScenario.Provenance.EXACT);
        s.addArmy(army("garrison", owner, CombatLevel.DEFEND_ONLY,
                platoon(troopType("mil", null), 200)), CombatScenario.Provenance.ESTIMATED);

        assertTrue(s.isSiegeExpected(), "it has engines and it is storming the walls");
        assertTrue(s.getCityFortificationDefense() > 0, "and there is a fortification to attack");

        besieger.setCombatLevel(CombatLevel.ATTACK_ARMY);

        assertFalse(s.isSiegeExpected(),
                "an army not assaulting the city does not bring its engines to the walls");
    }

    /** The Judge counts ;TYTS; as siege too, and this copy used to miss it. */
    @Test
    public void theOtherSiegeHabilidadeCountsAsWell() {
        final Jogador me = jogador("j1");
        final Nacao mine = nacao("m", me), owner = nacao("o", null);
        final CombatScenario s = new CombatScenario(deathMatch(), hex(cidade(owner, 3, 3, 100)));
        s.setObserver(me);
        s.addArmy(army("besieger", mine, CombatLevel.ATTACK_CITY,
                platoon(troopType("tower", ";TYTS;"), 10), platoon(troopType("inf", null), 900)),
                CombatScenario.Provenance.EXACT);
        s.addArmy(army("garrison", owner, CombatLevel.DEFEND_ONLY,
                platoon(troopType("mil", null), 200)), CombatScenario.Provenance.ESTIMATED);

        assertTrue(s.isSiegeExpected());
    }
}
