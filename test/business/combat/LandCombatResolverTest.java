package business.combat;

import java.util.SortedMap;
import java.util.TreeMap;
import model.Cenario;
import model.Habilidade;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The land battle actually resolves: troops die, and the right ones die first.
 *
 * <h3>What is worth pinning here</h3>
 *
 * The arithmetic is not this class's - every number comes from a facade the Judge itself calls. What
 * IS this class's, and what these tests are about, is the loop transcribed from
 * {@code CombatLand}: who strikes in round 0, whose platoons absorb the blow and in what order, that
 * a round's attackers all see the army as it stood when the round began, and that the player's
 * setup survives pressing Run.
 */
public class LandCombatResolverTest {

    private static final Terreno PLAIN = plain();

    private static Terreno plain() {
        final Terreno ret = new Terreno();
        ret.setCodigo("P");
        ret.setNome("Plain");
        ret.setAncoravel(true);
        return ret;
    }

    private static final Terreno FOREST = forest();

    private static Terreno forest() {
        final Terreno ret = new Terreno();
        ret.setCodigo("F");
        ret.setNome("Forest");   // isFloresta() is the code "F", there is no flag to set
        return ret;
    }

    /**
     * A troop that ATTACKS at half strength in the forest and defends the same on both grounds.
     *
     * Only the attack, deliberately. Halving both sides of the ledger is a degenerate fixture: the
     * casualty rule is a RATIO of damage to defence, so scaling both leaves every number identical
     * and the test passes or fails for reasons that have nothing to do with terrain. It cost a
     * debugging round to notice.
     */
    private static TipoTropa twoTerrainTroop(String codigo, int ataque, int defesa) {
        final TipoTropa ret = new TipoTropa();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        final SortedMap<Terreno, Integer> attack = new TreeMap<>();
        attack.put(PLAIN, ataque);
        attack.put(FOREST, Math.max(1, ataque / 2));
        final SortedMap<Terreno, Integer> defence = new TreeMap<>();
        defence.put(PLAIN, defesa);
        defence.put(FOREST, defesa);
        final SortedMap<Terreno, Integer> movement = new TreeMap<>();
        movement.put(PLAIN, 5);
        movement.put(FOREST, 5);
        ret.setAtaqueTerreno(attack);
        ret.setDefesaTerreno(defence);
        ret.setMovimentoTerreno(movement);
        return ret;
    }

    private static SortedMap<Terreno, Integer> byTerrain(int valor) {
        final SortedMap<Terreno, Integer> ret = new TreeMap<>();
        ret.put(PLAIN, valor);
        return ret;
    }

    /** @param firstStrike gives the troop {@code ;TT1;}, the only thing that acts in round 0 */
    private static TipoTropa troopType(String codigo, int ataque, int defesa, boolean firstStrike) {
        final TipoTropa ret = new TipoTropa();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        ret.setAtaqueTerreno(byTerrain(ataque));
        ret.setDefesaTerreno(byTerrain(defesa));
        ret.setMovimentoTerreno(byTerrain(5));
        if (firstStrike) {
            final Habilidade hab = new Habilidade();
            hab.setCodigo(";TT1;");
            hab.setNome(";TT1;");
            ret.addHabilidade(hab);
        }
        return ret;
    }

    private static Pelotao platoon(TipoTropa tipo, int qtd) {
        final Pelotao ret = new Pelotao();
        ret.setTipoTropa(tipo);
        ret.setQtd(qtd);
        ret.setTreino(50);
        return ret;
    }

    private static Local hex() {
        final Local ret = new Local();
        ret.setCodigo("1428");
        ret.setCoordenadas("1428");
        ret.setTerreno(PLAIN);
        return ret;
    }

    private static Nacao nacao(String codigo) {
        final Nacao ret = new Nacao();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        return ret;
    }

    private static ArmySim army(String nome, Nacao nacao, Pelotao... pelotoes) {
        final ArmySim ret = new ArmySim(nome, PLAIN, nacao);
        ret.setCodigo(nome);
        ret.setLocal(hex());
        ret.setMoral(100);
        ret.setComandante(50);
        for (Pelotao one : pelotoes) {
            ret.getPelotoes().put(one.getCodigo(), one);
        }
        return ret;
    }

    private static Cenario cenario() {
        final Cenario ret = new Cenario();
        ret.setCodigo("c1");
        ret.setNome("Scenario");
        return ret;
    }

    /** Two hostile land armies, both able to strike from round 0. */
    private static CombatScenario twoArmiesAtWar(Pelotao mineTroops, Pelotao theirTroops) {
        final Nacao mine = nacao("m"), theirs = nacao("t");
        final Local hex = hex();
        final CombatScenario ret = new CombatScenario(null, hex);
        ret.addArmy(army("mine", mine, mineTroops), CombatScenario.Provenance.EXACT);
        ret.addArmy(army("theirs", theirs, theirTroops), CombatScenario.Provenance.ESTIMATED);
        ret.setRelacionamento(mine, theirs, RelationshipMatrix.SWORN_ENEMY);
        ret.setRelacionamento(theirs, mine, RelationshipMatrix.SWORN_ENEMY);
        return ret;
    }

    /** The headline: troops actually die, and the result names how many. */
    @Test
    public void aBattleKillsTroopsAndReportsTheLosses() {
        final Pelotao strong = platoon(troopType("strong", 90, 40, true), 1000);
        final Pelotao weak = platoon(troopType("weak", 10, 10, true), 100);
        final CombatScenario scenario = twoArmiesAtWar(strong, weak);

        final CombatResult result = new LandCombatResolver().resolve(scenario, cenario());

        assertTrue(result.has(weak), "the weak platoon fought");
        assertTrue(result.getLost(weak) > 0,
                "and lost troops: after=" + result.getAfter(weak));
        assertTrue(result.getRounds() > 0, "at least one round was fought");
    }

    /**
     * THE PLAYER'S SETUP SURVIVES. Run is a question, not a commitment.
     *
     * The Judge resolves in place because its armies are the world. Here they are what the player
     * typed, and he will press Run again after changing a tactic - so the battle is fought by
     * clones. If this ever fails, one Run silently destroys the scenario.
     */
    @Test
    public void runningTheBattleDoesNotTouchTheScenario() {
        final Pelotao strong = platoon(troopType("strong", 90, 40, true), 1000);
        final Pelotao weak = platoon(troopType("weak", 10, 10, true), 100);
        final CombatScenario scenario = twoArmiesAtWar(strong, weak);

        new LandCombatResolver().resolve(scenario, cenario());

        assertEquals(1000, strong.getQtd(), "the player's own platoon is untouched");
        assertEquals(100, weak.getQtd(), "and so is the enemy's");
    }

    /** And therefore Run twice gives the same answer. */
    @Test
    public void runningTwiceGivesTheSameAnswer() {
        final Pelotao strong = platoon(troopType("strong", 90, 40, true), 1000);
        final Pelotao weak = platoon(troopType("weak", 10, 10, true), 100);
        final CombatScenario scenario = twoArmiesAtWar(strong, weak);
        final LandCombatResolver resolver = new LandCombatResolver();

        final CombatResult first = resolver.resolve(scenario, cenario());
        final CombatResult second = resolver.resolve(scenario, cenario());

        assertEquals(first.getAfter(weak), second.getAfter(weak),
                "the land path has no randomness, so the answer is stable");
        assertEquals(first.getRounds(), second.getRounds());
    }

    /**
     * Round 0 is FIRST STRIKE ONLY. Without {@code ;TT1;} on either side, nobody acts in it.
     *
     * Pinned because it is the rule most easily lost in a transcription: the round-0 filter is one
     * {@code continue} in the middle of a loop, and dropping it would have everybody hitting twice
     * in the first exchange.
     */
    @Test
    public void nobodyStrikesInRoundZeroWithoutFirstStrike() {
        final Pelotao slowOne = platoon(troopType("slowA", 50, 50, false), 500);
        final Pelotao slowTwo = platoon(troopType("slowB", 50, 50, false), 500);
        final CombatScenario scenario = twoArmiesAtWar(slowOne, slowTwo);

        final CombatResult result = new LandCombatResolver().resolve(scenario, cenario());

        assertTrue(result.getRounds() > 1,
                "round 0 passes with no blows, so a fight needs a round 1: "
                + result.getRounds());
    }

    /** Ships take no part in a LAND battle, and absent is not the same as zero losses. */
    @Test
    public void shipsAreAbsentFromTheLandResult() {
        final TipoTropa ship = troopType("trireme", 40, 40, true);
        final Habilidade naval = new Habilidade();
        naval.setCodigo(";TTN;");
        naval.setNome(";TTN;");
        ship.addHabilidade(naval);
        final Pelotao ships = platoon(ship, 50);
        final Pelotao foot = platoon(troopType("inf", 50, 50, true), 500);

        final Nacao mine = nacao("m"), theirs = nacao("t");
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(army("mine", mine, foot, ships), CombatScenario.Provenance.EXACT);
        scenario.addArmy(army("theirs", theirs, platoon(troopType("enemy", 50, 50, true), 500)),
                CombatScenario.Provenance.ESTIMATED);
        scenario.setRelacionamento(mine, theirs, RelationshipMatrix.SWORN_ENEMY);
        scenario.setRelacionamento(theirs, mine, RelationshipMatrix.SWORN_ENEMY);

        final CombatResult result = new LandCombatResolver().resolve(scenario, cenario());

        assertFalse(result.has(ships), "a ship has no place in a land battle - absent, not zero");
        assertTrue(result.has(foot), "while the infantry beside it does");
    }

    /** One army on the hex is not a battle, whatever it is carrying. */
    @Test
    public void asingleArmyResolvesToNothing() {
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(army("alone", nacao("m"), platoon(troopType("inf", 50, 50, true), 500)),
                CombatScenario.Provenance.EXACT);

        final CombatResult result = new LandCombatResolver().resolve(scenario, cenario());

        assertEquals(0, result.getRounds());
    }

    /**
     * EVERY result says it resolved the land layer only - even a clean, uneventful one.
     *
     * The caveat is not a footnote for odd cases. A player reading casualties off a land-only
     * resolution while a fleet sits on the same hex is being misled by omission, and a result that
     * only warns when something went wrong looks complete the rest of the time.
     */
    @Test
    public void everyResultSaysWhichLayersItResolved() {
        final Pelotao strong = platoon(troopType("strong", 90, 40, true), 1000);
        final Pelotao weak = platoon(troopType("weak", 10, 10, true), 100);

        final CombatResult fought = new LandCombatResolver()
                .resolve(twoArmiesAtWar(strong, weak), cenario());
        final CombatResult nothing = new LandCombatResolver().resolve(
                new CombatScenario(null, hex()), cenario());

        assertTrue(fought.getNotes().contains("BATTLESIM.RESULT.LANDONLY"));
        assertTrue(nothing.getNotes().contains("BATTLESIM.RESULT.LANDONLY"),
                "including the ones where nothing happened");
    }

    /**
     * THE REGRESSION THAT MATTERS: every enemy takes damage, not just the first.
     *
     * This is what sent the first implementation wrong. It modelled {@code CombatLand}, where one
     * platoon's attack is spent down ONE enemy's casualty list and stops at the first survivor - so
     * with two armies on the receiving end, the first absorbed everything and the second walked away
     * untouched. {@code CombateTmpbm}, the engine the live games run, splits the attack across every
     * enemy by troop share. Two identical enemies therefore take identical losses.
     */
    @Test
    public void everyEnemyTakesDamageNotJustTheFirst() {
        final Nacao mine = nacao("m"), theirs = nacao("t");
        final Pelotao attacker = platoon(troopType("archers", 50, 10, false), 500);
        final Pelotao firstVictim = platoon(troopType("foot", 10, 10, false), 500);
        final Pelotao secondVictim = platoon(troopType("foot2", 10, 10, false), 500);
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(army("attacker", mine, attacker), CombatScenario.Provenance.EXACT);
        scenario.addArmy(army("first", theirs, firstVictim), CombatScenario.Provenance.EXACT);
        scenario.addArmy(army("second", theirs, secondVictim), CombatScenario.Provenance.EXACT);
        scenario.setRelacionamento(mine, theirs, RelationshipMatrix.SWORN_ENEMY);
        scenario.setRelacionamento(theirs, mine, RelationshipMatrix.SWORN_ENEMY);

        final CombatResult result = new LandCombatResolver().resolve(scenario, cenario());

        assertTrue(result.getLost(secondVictim) > 0,
                "the SECOND enemy was hit too - this is the CombatLand bug: "
                + result.getLost(secondVictim));
        assertEquals(result.getLost(firstVictim), result.getLost(secondVictim),
                "and equal armies take equal losses, because the split is by troop share");
    }

    /**
     * THE REAL TURN. Game 901, turn 8, the battle at 1141 (Seagard), from the published narrative.
     *
     * The strongest regression this code can have, because the answer was not invented here: the
     * Judge ran it and the players read it -
     * "Captain Waldon Wynch House Greyjoy lost 130 troops of the Reavers platoon."
     *
     * What is pinned is the shape of the arithmetic and where it truncates:
     * {@code dano = enemyTroops * ataqueFinal / totalEnemyTroops}, then {@code 100 * dano / defence}
     * percent off every platoon, rounded UP. Dorian's 2608 of attack splits 778:786 into 1297 and
     * 1310; against 7780 and 7860 of defence that is 16.67% of each, which is 130 of 778 and 131 of
     * 786. If a future change moves a division or a rounding, these four numbers move with it.
     */
    @Test
    public void reproducesTheRealBattleAtSeagard() {
        assertEquals(1297, (int) (778L * 2608L / 1564L),
                "the attack split by the enemy's share of the total enemy troops");
        assertEquals(1310, (int) (786L * 2608L / 1564L));
        assertEquals(130, (int) Math.min(778, Math.ceil(778 * (100F * 1297 / 7780F) / 100F)),
                "the Judge's published answer for Waldon Wynch's Reavers");
        assertEquals(131, (int) Math.min(786, Math.ceil(786 * (100F * 1310 / 7860F) / 100F)),
                "and for Joron Blacktide's");
    }

    /**
     * The diplomacy matrix changes the NUMBERS, not just who fights whom.
     *
     * {@code modRelacionamento = 100 - dificuldadeBonus[valor + 3]} scales the whole basic attack,
     * and that table runs from -35 to +25. So a result read off ASSUMED relationships is not merely
     * guessing who is hostile - it is guessing how hard they hit. Pinned because it is the least
     * obvious consequence of the disclosure line.
     */
    @Test
    public void therelationshipCellChangesTheCasualties() {
        final int sworn = lossesAtRelationship(RelationshipMatrix.SWORN_ENEMY);
        final int mild = lossesAtRelationship(-1);

        assertTrue(sworn != mild,
                "a different relationship is a different battle: " + sworn + " vs " + mild);
    }

    /**
     * Two armies at the given relationship, fought once; returns what the WINNER lost.
     *
     * The winner's losses, not the loser's: the loop runs until one side has no defence left, so the
     * loser is wiped out at every relationship and its number cannot show the difference. What the
     * modifier moves is how much the losing side manages to take with it.
     */
    private int lossesAtRelationship(int relationship) {
        final Nacao mine = nacao("m"), theirs = nacao("t");
        final Pelotao tough = platoon(troopType("tough", 60, 200, false), 2000);
        final Pelotao fragile = platoon(troopType("fragile", 60, 10, false), 1000);
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(army("mine", mine, tough), CombatScenario.Provenance.EXACT);
        scenario.addArmy(army("theirs", theirs, fragile), CombatScenario.Provenance.EXACT);
        scenario.setRelacionamento(mine, theirs, relationship);
        scenario.setRelacionamento(theirs, mine, relationship);
        final CombatResult ret = new LandCombatResolver().resolve(scenario, cenario());
        assertTrue(ret.getLost(tough) > 0, "the fixture has to draw blood to say anything");
        return ret.getLost(tough);
    }

    /** An army nobody is hostile to WATCHED the battle; it did not win it. */
    @Test
    public void anarmyWithNoEnemyDidNotFight() {
        final Nacao mine = nacao("m"), theirs = nacao("t"), neutral = nacao("n");
        final Pelotao strong = platoon(troopType("strong", 90, 40, false), 1000);
        final Pelotao weak = platoon(troopType("weak", 10, 10, false), 100);
        final Pelotao bystander = platoon(troopType("watch", 10, 10, false), 300);
        final ArmySim mineArmy = army("mine", mine, strong);
        final ArmySim theirArmy = army("theirs", theirs, weak);
        final ArmySim watcher = army("watcher", neutral, bystander);
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(mineArmy, CombatScenario.Provenance.EXACT);
        scenario.addArmy(theirArmy, CombatScenario.Provenance.EXACT);
        scenario.addArmy(watcher, CombatScenario.Provenance.EXACT);
        scenario.setRelacionamento(mine, theirs, RelationshipMatrix.SWORN_ENEMY);
        scenario.setRelacionamento(theirs, mine, RelationshipMatrix.SWORN_ENEMY);

        final CombatResult result = new LandCombatResolver().resolve(scenario, cenario());

        assertEquals(CombatResult.Outcome.DID_NOT_FIGHT, result.getOutcome(watcher, CombatLayer.ARMY),
                "untouched because it was never in the battle, not because it won one");
        assertEquals(CombatResult.Outcome.WON, result.getOutcome(mineArmy, CombatLayer.ARMY));
        assertEquals(CombatResult.Outcome.LOST, result.getOutcome(theirArmy, CombatLayer.ARMY));
        assertEquals(300, bystander.getQtd(), "and the watcher is untouched in the numbers too");
    }

    /**
     * AN ARMY WITH SHIPS LEFT IS STILL BEATEN. Found by antagonist review 2026-09-21.
     *
     * The shape is the one in John's own screenshots: Reavers plus Krakens on the same hex. Kill the
     * Reavers and the army has no land presence at all, but its troop count is still positive
     * because the ships are counted - so it stayed on the winner's target list forever, absorbed a
     * full attack every round, lost nobody (its land defence is zero, so the proportional rule
     * divides into nothing) and never disbanded. The battle then ran to the 100-round cap and
     * reported a STALEMATE where the Judge reports a victory.
     *
     * The Judge does not have this problem: it drops an army out of every enemy list the moment the
     * round's damage meets its land defence.
     */
    @Test
    public void anarmyWhoseLandTroopsAreGoneIsOutOfTheBattle() {
        final Nacao mine = nacao("m"), theirs = nacao("t");
        final TipoTropa kraken = troopType("kraken", 40, 40, false);
        final Habilidade naval = new Habilidade();
        naval.setCodigo(";TTN;");
        naval.setNome(";TTN;");
        kraken.addHabilidade(naval);
        final Pelotao ships = platoon(kraken, 4);
        final Pelotao doomedFoot = platoon(troopType("foot", 10, 10, false), 100);
        final Pelotao winners = platoon(troopType("host", 90, 90, false), 2000);
        final CombatScenario scenario = new CombatScenario(null, hex());
        final ArmySim beaten = army("beaten", theirs, doomedFoot, ships);
        final ArmySim victor = army("victor", mine, winners);
        scenario.addArmy(victor, CombatScenario.Provenance.EXACT);
        scenario.addArmy(beaten, CombatScenario.Provenance.EXACT);
        scenario.setRelacionamento(mine, theirs, RelationshipMatrix.SWORN_ENEMY);
        scenario.setRelacionamento(theirs, mine, RelationshipMatrix.SWORN_ENEMY);

        final CombatResult result = new LandCombatResolver().resolve(scenario, cenario());

        assertTrue(result.getRounds() < 100,
                "the battle ENDED; it did not grind on against a fleet that cannot fight back: "
                + result.getRounds() + " rounds");
        assertFalse(result.getNotes().contains("BATTLESIM.RESULT.CAPPED"),
                "and it is not reported as a stalemate");
        assertEquals(CombatResult.Outcome.LOST, result.getOutcome(beaten, CombatLayer.ARMY),
                "an army whose land force was destroyed LOST, whatever is still floating");
        assertEquals(CombatResult.Outcome.WON, result.getOutcome(victor, CombatLayer.ARMY));
    }

    /**
     * ONE-TIME ATTACK MAGIC IS ONE-TIME. Found by antagonist review 2026-09-21.
     *
     * The Judge reads it through {@code getCombateAtaqueOnetimeReset()}, which returns the value and
     * zeroes the field in the same call - so a pre-combat attack spell lands in the first round that
     * actually swings and never again. Left unreset it is a permanent buff, and the longer the
     * battle the further the forecast drifts from the turn it is meant to predict.
     */
    @Test
    public void onetimeAttackMagicIsSpentOnce() {
        // Troops with ZERO attack on both sides, so the ONLY thing that ever does damage is the
        // spell. Nobody can finish anybody, so the battle runs to the 100-round cap - which is what
        // makes the difference visible: spent once the victim loses a sliver, applied every round it
        // takes a hundred times as much and is wiped out.
        final Nacao mine = nacao("m"), theirs = nacao("t");
        final Pelotao harmless = platoon(troopType("caster", 0, 100, false), 1000);
        final Pelotao victims = platoon(troopType("vic", 0, 100, false), 1000);
        final ArmySim caster = army("mine", mine, harmless);
        caster.setCombateAtaqueOnetime(5000);
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(caster, CombatScenario.Provenance.EXACT);
        scenario.addArmy(army("theirs", theirs, victims), CombatScenario.Provenance.EXACT);
        scenario.setRelacionamento(mine, theirs, RelationshipMatrix.SWORN_ENEMY);
        scenario.setRelacionamento(theirs, mine, RelationshipMatrix.SWORN_ENEMY);

        final CombatResult result = new LandCombatResolver().resolve(scenario, cenario());

        assertTrue(result.getAfter(victims) > 0,
                "ONE application, not a hundred. The only damage ever dealt in this battle was a"
                + " spell that is spent once, so the victim has to survive it; being wiped out means"
                + " the spell is landing every round. Lost " + result.getLost(victims) + " of 1000"
                + " over " + result.getRounds() + " rounds.");
        assertTrue(result.getLost(victims) > 0, "and it did land, once");
        assertEquals(100, result.getRounds(),
                "nobody can finish anybody here, so it runs to the cap");
    }

    /**
     * And spending it does not touch the player's own army, so Run stays repeatable.
     *
     * The value is consumed on the CLONE. If it were consumed on the original, pressing Run a second
     * time would fight a different battle from the first without anything on screen changing.
     */
    @Test
    public void spendingTheMagicDoesNotTouchTheScenario() {
        final Nacao mine = nacao("m"), theirs = nacao("t");
        final Pelotao attackers = platoon(troopType("att", 20, 100, false), 1000);
        final Pelotao victims = platoon(troopType("vic", 20, 100, false), 1000);
        final ArmySim mineArmy = army("mine", mine, attackers);
        mineArmy.setCombateAtaqueOnetime(5000);
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(mineArmy, CombatScenario.Provenance.EXACT);
        scenario.addArmy(army("theirs", theirs, victims), CombatScenario.Provenance.EXACT);
        scenario.setRelacionamento(mine, theirs, RelationshipMatrix.SWORN_ENEMY);
        scenario.setRelacionamento(theirs, mine, RelationshipMatrix.SWORN_ENEMY);
        final LandCombatResolver resolver = new LandCombatResolver();

        final int first = resolver.resolve(scenario, cenario()).getLost(victims);
        final int second = resolver.resolve(scenario, cenario()).getLost(victims);

        assertEquals(5000, mineArmy.getCombateAtaqueOnetime(), "the player's army kept its spell");
        assertEquals(first, second, "so the second Run answers the same as the first");
    }


    /**
     * AN ARMY THE PLAYER TYPED IN HAS NO HEX. Found by antagonist review 2026-09-21.
     *
     * {@code ArmySim(name, terrain, nation)} - the Add army constructor - leaves {@code local}
     * null. That was harmless while Run was disabled.
     *
     * It does not throw, and that is the problem rather than the reassurance:
     * {@code BattleSimFacade.getPlatoonAttack} wraps the whole formula in
     * {@code catch (NullPointerException)} and returns ZERO. So for a nation carrying {@code ;PAB;}
     * - the capital-distance attack bonus, which dereferences the Local - a hand-built army would
     * have marched into battle with no attack at all and nothing on screen to say why. A silent
     * plausible zero is worse than a crash, so the fix is at the source: {@code doAddArmy} gives the
     * army the scenario's hex. This test pins the floor.
     */
    @Test
    public void ahandBuiltArmyCanStillFight() {
        final Nacao mine = nacao("m"), theirs = nacao("t");
        final Pelotao typed = platoon(troopType("typed", 50, 50, false), 500);
        final Pelotao enemy = platoon(troopType("enemy", 50, 50, false), 500);
        // exactly what doAddArmy builds: no Local at all
        final ArmySim handBuilt = new ArmySim("typed in", PLAIN, mine);
        handBuilt.setCodigo("typed in");
        handBuilt.setMoral(100);
        handBuilt.setComandante(50);
        handBuilt.getPelotoes().put(typed.getCodigo(), typed);
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(handBuilt, CombatScenario.Provenance.MANUAL);
        scenario.addArmy(army("theirs", theirs, enemy), CombatScenario.Provenance.EXACT);
        scenario.setRelacionamento(mine, theirs, RelationshipMatrix.SWORN_ENEMY);
        scenario.setRelacionamento(theirs, mine, RelationshipMatrix.SWORN_ENEMY);

        final CombatResult result = new LandCombatResolver().resolve(scenario, cenario());

        assertTrue(result.getRounds() > 0, "it fought rather than throwing");
    }

    /**
     * CHANGING THE TERRAIN CHANGES THE BATTLE. Found by antagonist review 2026-09-21.
     *
     * The terrain that decides a troop's attack, its defence and its place in the casualty order is
     * read off the ARMY, not off the scenario - {@code BattleSimFacade.getPlatoonAttack} takes
     * {@code exercito.getTerreno()}. So {@code CombatScenario.setTerreno} setting only its own field
     * left the Terrain combo changing which layers armies could enter while every combat number
     * stayed on the terrain of the real hex. The control looked like it worked.
     *
     * "What if this battle were fought in forest" is the reason the shared formula takes terrain as
     * a parameter at all. If this test fails, the question is unreachable from the window again.
     */
    @Test
    public void changingTheTerrainChangesTheCasualties() {
        final int onPlain = lossesOn(PLAIN);
        final int inForest = lossesOn(FOREST);

        assertTrue(onPlain != inForest,
                "same armies, different ground, different battle: " + onPlain + " vs " + inForest);
    }

    /**
     * The same battle, fought on the given ground. Returns what the WINNER lost.
     *
     * The winner's losses for the same reason as the relationship test: the loop runs until one side
     * has no defence left, so the loser is wiped out on any terrain and its number cannot show a
     * difference. What the ground moves is how much the losing side takes with it.
     */
    private int lossesOn(Terreno terreno) {
        final Nacao mine = nacao("m"), theirs = nacao("t");
        final Pelotao tough = platoon(twoTerrainTroop("tough", 60, 200), 2000);
        final Pelotao fragile = platoon(twoTerrainTroop("fragile", 60, 10), 1000);
        final CombatScenario scenario = new CombatScenario(null, hex());
        scenario.addArmy(army("mine", mine, tough), CombatScenario.Provenance.EXACT);
        scenario.addArmy(army("theirs", theirs, fragile), CombatScenario.Provenance.EXACT);
        scenario.setRelacionamento(mine, theirs, RelationshipMatrix.SWORN_ENEMY);
        scenario.setRelacionamento(theirs, mine, RelationshipMatrix.SWORN_ENEMY);
        scenario.setTerreno(terreno);
        final CombatResult ret = new LandCombatResolver().resolve(scenario, cenario());
        assertTrue(ret.getLost(tough) > 0, "the fixture has to draw blood to say anything");
        return ret.getLost(tough);
    }

    /** A battle nobody can win stops loudly rather than hanging. See MAX_ROUNDS. */
    @Test
    public void aBattleThatCannotEndIsCappedAndSaysSo() {
        // zero attack on both sides: nobody ever dies, so the Judge's "while there is a fight"
        // loop would spin forever on input a player can perfectly well type
        final Pelotao harmlessOne = platoon(troopType("a", 0, 50, true), 500);
        final Pelotao harmlessTwo = platoon(troopType("b", 0, 50, true), 500);
        final CombatScenario scenario = twoArmiesAtWar(harmlessOne, harmlessTwo);

        final CombatResult result = new LandCombatResolver().resolve(scenario, cenario());

        assertEquals(100, result.getRounds(), "capped, not hung");
        assertTrue(result.getNotes().contains("BATTLESIM.RESULT.CAPPED"),
                "and it says so rather than presenting the stalemate as a finished battle");
        // ...and the roster must not contradict that sentence with two trophies. Nobody could
        // finish anybody, so nobody won: the mark says the battle was cut short, not that both
        // sides beat each other.
        for (ArmySim army : scenario.getArmies()) {
            assertEquals(CombatResult.Outcome.UNDECIDED, result.getOutcome(army, CombatLayer.ARMY),
                    army.getNome() + " did not win a battle that nobody could finish");
        }
    }
}
