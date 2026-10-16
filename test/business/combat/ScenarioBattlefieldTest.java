package business.combat;

import business.facade.BattleSimFacade;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Cenario;
import model.Cidade;
import model.Exercito;
import model.Habilidade;
import model.Local;
import model.Nacao;
import model.Partida;
import model.Pelotao;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioBattlefieldTest extends LandCombatFixture {

    private static Local ground(String coordinate) {
        final Local ret = hex();
        ret.setCodigo(coordinate);
        ret.setCoordenadas(coordinate);
        return ret;
    }

    private static Cidade city(Local ground, Nacao owner) {
        final Cidade ret = new Cidade();
        ret.setCodigo("city-" + ground.getCoordenadas());
        ret.setNome("Target city");
        ret.setLocal(ground);
        if (owner != null) {
            ret.setNacao(owner);
        }
        ret.setTamanho(3);
        ret.setFortificacao(2);
        ret.setLealdade(50);
        ret.setDocas(1);
        return ret;
    }

    private static Exercito resident(String code, Local ground, Nacao owner) {
        final Exercito ret = new Exercito();
        ret.setCodigo(code);
        ret.setNome(code);
        ret.setNacao(owner);
        ret.setLocal(ground);
        final Pelotao troop = platoon(twoTerrainTroop("inf", 60, 40), 500);
        ret.getPelotoes().put(troop.getCodigo(), troop);
        return ret;
    }

    private static ArmySim only(CombatScenario scenario) {
        return scenario.getArmies().get(0);
    }

    @Test
    void changingTargetsKeepsStagedEditsAndReplacesDestinationResidents() {
        final Nacao mine = nacao("mine"), foe = nacao("foe");
        final Local origin = ground("1428"), first = ground("1429"), second = ground("1430");
        resident("mine", origin, mine);
        final Exercito defender = resident("first", first, foe);
        resident("second", second, foe);
        city(first, foe);
        final CombatScenario sim = new ScenarioLoader().load(null, origin, null);
        final ArmySim staged = only(sim);
        staged.setMoral(77);
        staged.getPelotoes().get("inf").setQtd(1234);

        assertTrue(sim.chooseBattlefield(first, null));
        final ArmySim imported = sim.getArmies().get(1);
        assertTrue(sim.isDestinationArmy(imported));
        assertEquals(CombatScenario.Provenance.ESTIMATED, sim.getProvenance(imported));
        assertNotSame(defender.getPelotoes().get("inf"), imported.getPelotoes().get("inf"));
        imported.setMoral(82);
        assertFalse(sim.chooseBattlefield(first, null));
        assertEquals(82, imported.getMoral(), "same hex must not reset edited defenders");

        assertTrue(sim.chooseBattlefield(second, null));
        assertEquals(2, sim.getArmies().size());
        assertSame(staged, only(sim));
        assertEquals(77, staged.getMoral());
        assertEquals(1234, staged.getPelotoes().get("inf").getQtd());
        assertFalse(sim.getArmies().contains(imported));
        assertEquals("second", sim.getArmies().get(1).getCodigo());
        assertNull(sim.getCidade());
        assertFalse(sim.isCityParticipates());
        for (ArmySim army : sim.getArmies()) {
            assertSame(sim.getLocal(), army.getLocal());
        }
    }

    @Test
    void returningHomeDoesNotDuplicateTheStagedArmy() {
        final Nacao mine = nacao("mine"), foe = nacao("foe");
        final Local origin = ground("1428"), target = ground("1429");
        resident("mine", origin, mine);
        resident("defender", target, foe);
        final CombatScenario sim = new ScenarioLoader().load(null, origin, null);
        sim.chooseBattlefield(target, null);
        sim.chooseBattlefield(origin, null);
        assertEquals(1, sim.getArmies().size());
        assertEquals("mine", only(sim).getCodigo());
        assertFalse(sim.isDestinationArmy(only(sim)));
    }

    @Test
    void copiedCityGroundAndNationListsNeverMutateTheLoadedWorld() {
        final Nacao owner = nacao("owner"), other = nacao("other");
        final Local origin = ground("1428"), target = ground("1429");
        final Cidade real = city(target, owner);
        resident("defender", target, owner);
        final int ownerCities = owner.getCidades().size();
        final CombatScenario sim = new CombatScenario(null, origin);
        sim.chooseBattlefield(target, null);
        sim.setTerreno(FOREST);
        sim.setCityOwner(other);
        sim.setCityFortificacao(5);
        sim.setCityLealdade(90);

        assertNotSame(target, sim.getLocal());
        assertNotSame(real, sim.getCidade());
        assertSame(sim.getLocal(), sim.getCidade().getLocal());
        assertSame(sim.getCidade(), sim.getLocal().getCidade());
        assertSame(FOREST, sim.getLocal().getTerreno());
        assertSame(PLAIN, target.getTerreno());
        assertSame(real, target.getCidade());
        assertSame(target, real.getLocal());
        assertSame(owner, real.getNacao());
        assertEquals(2, real.getFortificacao());
        assertEquals(50, real.getLealdade());
        assertEquals(ownerCities, owner.getCidades().size());
        assertTrue(other.getCidades().isEmpty());
        assertEquals(1, target.getExercitos().size());
        assertSame(target, target.getExercitos().get("defender").getLocal());
    }

    @Test
    void locationDependentTroopDefenseUsesTheChosenCity() {
        final Nacao mine = nacao("mine");
        final Local origin = ground("1428"), target = ground("1429");
        city(target, mine);
        final TipoTropa type = twoTerrainTroop("inf", 60, 40);
        final Habilidade hab = new Habilidade();
        hab.setCodigo(";TTD;");
        type.addHabilidade(hab);
        final Pelotao troop = platoon(type, 500);
        final ArmySim attacker = army("mine", mine, troop);
        final CombatScenario sim = new CombatScenario(null, origin);
        sim.addArmy(attacker, CombatScenario.Provenance.EXACT);
        final BattleSimFacade math = new BattleSimFacade();
        final float outside = math.getPlatoonDefense(attacker, troop);

        sim.chooseBattlefield(target, null);

        assertEquals(outside * 2, math.getPlatoonDefense(attacker, troop), 0.001f);
    }

    @Test
    void distanceToCapitalAndCityTerrainBonusesFollowTheDestination() {
        final Nacao mine = nacao("mine");
        final Local capitalGround = ground("1428"), far = ground("5050");
        mine.setCapital(city(capitalGround, mine));
        final Habilidade nearCapital = new Habilidade();
        nearCapital.setCodigo(";PDB;");
        nearCapital.setValor(2);
        mine.addHabilidade(nearCapital);
        final Pelotao troop = platoon(twoTerrainTroop("inf", 60, 40), 500);
        final ArmySim army = army("mine", mine, troop);
        final CombatScenario sim = new CombatScenario(null, capitalGround);
        sim.addArmy(army, CombatScenario.Provenance.EXACT);
        final BattleSimFacade math = new BattleSimFacade();
        final float atCapital = math.getPlatoonDefense(army, troop);
        sim.chooseBattlefield(far, null);
        assertEquals(atCapital / 1.2f, math.getPlatoonDefense(army, troop), 0.01f);

        final Habilidade woods = new Habilidade();
        woods.setCodigo(";NWD;");
        woods.setValor(50);
        mine.addHabilidade(woods);
        sim.chooseBattlefield(capitalGround, null);
        final int onPlain = sim.getCityDefense();
        sim.setTerreno(FOREST);
        assertEquals(onPlain + onPlain / 2, sim.getCityDefense());
        assertSame(PLAIN, capitalGround.getTerreno());
    }

    @Test
    void aForkKeepsImportMembershipAndOwnsItsBattlefield() {
        final Nacao owner = nacao("owner");
        final Local target = ground("1429");
        city(target, owner);
        resident("defender", target, owner);
        final CombatScenario sim = new CombatScenario(null, ground("1428"));
        sim.chooseBattlefield(target, null);
        final CombatScenario copy = sim.copy();
        assertNotSame(sim.getLocal(), copy.getLocal());
        assertSame(copy.getLocal(), only(copy).getLocal());
        assertSame(copy.getLocal(), copy.getCidade().getLocal());
        copy.setTerreno(FOREST);
        copy.chooseBattlefield(ground("1430"), null);
        assertTrue(copy.getArmies().isEmpty());
        assertEquals(1, sim.getArmies().size());
        assertSame(PLAIN, sim.getLocal().getTerreno());
        assertTrue(sim.isCityParticipates());
    }

    @Test
    void unknownCityOwnerAndOpenWaterAreValidDestinations() {
        final Nacao fallback = nacao("unknown");
        final Local target = ground("1429");
        city(target, null);
        final CombatScenario sim = new CombatScenario(null, ground("1428"));
        sim.chooseBattlefield(target, fallback);
        assertSame(fallback, sim.getCidade().getNacao());
        assertTrue(fallback.getCidades().isEmpty());
        final Local sea = ground("1430");
        final model.Terreno water = new model.Terreno();
        water.setCodigo("D");
        sea.setTerreno(water);
        sim.chooseBattlefield(sea, fallback);
        assertSame(water, sim.getTerreno());
        assertNull(sim.getCidade());
        assertNull(sim.getLocal().getCidade());
    }

    @Test
    void aSaveRestoresTheBattlefieldAndDestinationReplacement() throws Exception {
        final Nacao owner = nacao("owner");
        final Local target = ground("1429");
        owner.setCapital(city(ground("1428"), owner));
        final Habilidade nearCapital = new Habilidade();
        nearCapital.setCodigo(";PDB;");
        nearCapital.setValor(2);
        owner.addHabilidade(nearCapital);
        city(target, owner);
        resident("defender", target, owner);
        final Cenario catalogue = cenario();
        catalogue.getTerrenos().put(PLAIN.getCodigo(), PLAIN);
        catalogue.getTipoTropas().put("inf", twoTerrainTroop("inf", 60, 40));
        final Partida game = new Partida();
        game.setCenario(catalogue);
        final model.Jogador observer = new model.Jogador();
        observer.setCodigo("player");
        owner.setOwner(observer);
        game.setJogadorAtivo(observer);
        final SortedMap<String, Nacao> nations = new TreeMap<>();
        nations.put(owner.getCodigo(), owner);
        final CombatScenario sim = new CombatScenario(game, ground("1428"));
        sim.chooseBattlefield(target, null);
        final int cityCount = owner.getCidades().size();
        final String saved = BattleSimTransfer.write(sim);
        final CombatScenario restored = BattleSimTransfer.read(saved, game, nations);
        assertEquals("1429", restored.getLocal().getCoordenadas());
        assertSame(observer, restored.getObserver());
        assertEquals(sim.getCityDefense(), restored.getCityDefense());
        assertSame(restored.getLocal(), only(restored).getLocal());
        assertSame(restored.getLocal(), restored.getCidade().getLocal());
        assertEquals(cityCount, owner.getCidades().size());
        final BattleSimFacade math = new BattleSimFacade();
        assertEquals(math.getPlatoonDefense(only(sim), only(sim).getPelotoes().values().iterator().next()),
                math.getPlatoonDefense(only(restored), only(restored).getPelotoes().values().iterator().next()),
                0.001f, "saved coordinates remain valid inputs to capital-distance bonuses");
        restored.chooseBattlefield(ground("1430"), null);
        assertTrue(restored.getArmies().isEmpty());
        restored.chooseBattlefield(target, null);
        assertEquals(CombatScenario.Provenance.EXACT, restored.getProvenance(only(restored)),
                "fresh imports still recognize the receiving player's own troops");

        final String legacy = saved.replace("<version>2</version>", "<version>1</version>")
                .replace("<destination>true</destination>", "");
        assertEquals(1, BattleSimTransfer.read(legacy, game, nations).getArmies().size());
        for (String bad : new String[]{"x", "142", "14280", "1a28", "-128"}) {
            final String malformed = saved.replace("<hex>1429</hex>", "<hex>" + bad + "</hex>");
            final BattleSimTransfer.TransferException ex = assertThrows(
                    BattleSimTransfer.TransferException.class,
                    () -> BattleSimTransfer.read(malformed, game, nations));
            assertEquals("BATTLESIM.TRANSFER.FORMAT", ex.getReasonKey());
        }
        final String missingHex = legacy.replace("<hex>1429</hex>", "");
        final BattleSimTransfer.TransferException ex = assertThrows(
                BattleSimTransfer.TransferException.class,
                () -> BattleSimTransfer.read(missingHex, game, nations));
        assertEquals("BATTLESIM.TRANSFER.HEX", ex.getReasonKey());
        assertEquals(1, BattleSimTransfer.readArmies(missingHex, game, nations).size(),
                "legacy saves without a location still recover through Paste army");
    }
}
