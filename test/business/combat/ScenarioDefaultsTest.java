package business.combat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import model.Exercito;
import model.Local;
import model.Pelotao;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Filling an enemy's blanks from the player's own file. T-837.
 *
 * The tests that matter here are the ones about what it must NOT do. Seeding numbers into somebody
 * else's army is the shape of change that goes wrong silently - it produces a confident answer
 * built on figures nobody typed - so the guards are pinned harder than the arithmetic: own armies
 * untouched, typed values untouched, a second press a no-op, and a seeded platoon that still
 * reports itself as a guess.
 */
public class ScenarioDefaultsTest extends LandCombatFixture {

    private static final TipoTropa INF = troopType("inf", 50, 40, false);
    private static final TipoTropa NONE = troopType(ScenarioDefaults.PLACEHOLDER_CODE, 1, 1, false);

    /** An army the player CAN count: a band, a head count and a morale. */
    private static Exercito counted(int band, int troops, int moral) {
        final Exercito ret = new Exercito();
        ret.setCodigo("e" + band + "-" + troops);
        ret.setNome(ret.getCodigo());
        ret.setTamanhoExercito(band);
        ret.setMoral(moral);
        final Pelotao one = platoon(INF, troops);
        ret.getPelotoes().put(one.getCodigo(), one);
        return ret;
    }

    private static CombatScenario scenarioWith(ArmySim army, CombatScenario.Provenance provenance) {
        final Local hex = hex();
        final CombatScenario ret = new CombatScenario(null, hex);
        ret.addArmy(army, provenance);
        return ret;
    }

    /** Band 4 is the mean of the band-4 armies, and of nothing else. */
    @Test
    public void theBandMeanIsFittedFromTheArmiesTheSameFileCanCount() {
        final ScenarioDefaults.Sample sample = ScenarioDefaults.Sample.from(Arrays.asList(
                counted(4, 2500, 40), counted(4, 3500, 60), counted(4, 4300, 50),
                counted(2, 300, 30), counted(5, 9000, 70)));

        assertEquals(3433, sample.troopsForBand(4), "(2500 + 3500 + 4300) / 3");
        assertEquals(3, sample.armiesInBand(4));
        assertEquals(300, sample.troopsForBand(2), "one army is still a sample");
        assertEquals(0, sample.troopsForBand(3), "no band-3 army was counted");
    }

    /**
     * An uncountable army must not drag its own band toward zero.
     *
     * This is the trap the whole fit rests on: the armies with no platoons are exactly the ones
     * being estimated, and averaging their zeroes in would shrink every estimate in the games where
     * the player can see least - which is where he needs it most.
     */
    @Test
    public void armiesWithNothingToCountAreNotAveragedIn() {
        final Exercito unseen = new Exercito();
        unseen.setCodigo("unseen");
        unseen.setTamanhoExercito(4);

        final ScenarioDefaults.Sample sample = ScenarioDefaults.Sample.from(Arrays.asList(
                counted(4, 3000, 50), unseen));

        assertEquals(3000, sample.troopsForBand(4), "not 1,500");
        assertEquals(1, sample.armiesInBand(4));
    }

    /** Ships are a different layer and a different band, so they are not land troops. */
    @Test
    public void theSampleCountsBodiesNotHulls() {
        final Exercito fleet = new Exercito();
        fleet.setCodigo("fleet");
        fleet.setTamanhoExercito(3);
        final Pelotao hulls = platoon(shipType("gal"), 40);
        final Pelotao cargo = platoon(INF, 900);
        fleet.getPelotoes().put(hulls.getCodigo(), hulls);
        fleet.getPelotoes().put(cargo.getCodigo(), cargo);

        assertEquals(900, ScenarioDefaults.Sample.from(Arrays.asList(fleet)).troopsForBand(3),
                "the 40 hulls are not part of the land band");
    }

    /** Morale 0 is the hole being filled, so it cannot be part of the average that fills it. */
    @Test
    public void theMoraleMeanIgnoresTheArmiesThatHaveNone() {
        final ScenarioDefaults.Sample sample = ScenarioDefaults.Sample.from(Arrays.asList(
                counted(2, 300, 40), counted(2, 300, 60), counted(2, 300, 0)));

        assertEquals(50, sample.getMoraleMean(), "(40 + 60) / 2");
        assertEquals(2, sample.getMoraleArmies());
    }

    /**
     * The commander rule, which is arithmetic rather than a fit.
     *
     * The server ships a foreign commander as {@code p_comandante / 10 * 10}, so 40 means 40..49
     * and 45 is the expected value. John's "base + 5".
     */
    @Test
    public void aForeignCommandersSkillBecomesTheMidpointOfHisDisclosedBand() {
        final ArmySim army = army("Enemy", nacao("f"), platoon(INF, 900));
        army.setComandante(40);
        final CombatScenario scenario = scenarioWith(army, CombatScenario.Provenance.ESTIMATED);

        final ScenarioDefaults.Filled filled = ScenarioDefaults.fill(scenario,
                ScenarioDefaults.Sample.from(Arrays.asList(counted(2, 300, 50))), NONE);

        assertEquals(45, army.getComandantePericia());
        assertEquals(1, filled.getCommanders());
    }

    /**
     * A garrison has NO commander, which is a different thing from one whose skill rounds to zero.
     * Promoting it to 5 would invent an officer.
     */
    @Test
    public void aCommanderlessArmyIsNotGivenAnOfficer() {
        final ArmySim army = army("Garrison", nacao("f"), platoon(INF, 900));
        army.setComandante(0);
        final CombatScenario scenario = scenarioWith(army, CombatScenario.Provenance.ESTIMATED);

        ScenarioDefaults.fill(scenario,
                ScenarioDefaults.Sample.from(Arrays.asList(counted(2, 300, 50))), NONE);

        assertEquals(0, army.getComandantePericia());
    }

    /** A skill that is not on a decade was typed by the player, so it is already his. */
    @Test
    public void aSkillThePlayerHasAlreadyTypedIsLeftAlone() {
        final ArmySim army = army("Enemy", nacao("f"), platoon(INF, 900));
        army.setComandante(47);
        final CombatScenario scenario = scenarioWith(army, CombatScenario.Provenance.ESTIMATED);

        ScenarioDefaults.fill(scenario,
                ScenarioDefaults.Sample.from(Arrays.asList(counted(2, 300, 50))), NONE);

        assertEquals(47, army.getComandantePericia());
    }

    /** The visibility-1 case: a band, and nothing else. */
    @Test
    public void anArmyWithNoPlatoonsGetsThePlaceholderAtTheBandsMean() {
        final ArmySim army = army("Unscouted", nacao("f"));
        army.getPelotoes().clear();
        army.setSizeBandLandIndex(4);
        army.setComandante(0);
        final CombatScenario scenario = scenarioWith(army, CombatScenario.Provenance.ESTIMATED);

        final ScenarioDefaults.Filled filled = ScenarioDefaults.fill(scenario,
                ScenarioDefaults.Sample.from(Arrays.asList(
                        counted(4, 3000, 50), counted(4, 4000, 50))), NONE);

        assertEquals(1, filled.getHeadCounts());
        assertEquals(1, army.getPelotoes().size());
        final Pelotao seeded = army.getPelotoes().values().iterator().next();
        assertEquals(3500, seeded.getQtd());
        assertEquals(ScenarioDefaults.PLACEHOLDER_CODE, seeded.getTipoTropa().getCodigo(),
                "composition is NOT invented - John ruled on that");
    }

    /**
     * A seeded platoon is a guess and has to keep saying so.
     *
     * An unregistered platoon reads MANUAL, which means "the player's own number". That is the one
     * thing this must never claim: it would drop the seeded count straight out of the accounting
     * that tells him how much of the answer is guesswork.
     */
    @Test
    public void aSeededPlatoonStillReportsItselfAsAGuess() {
        final ArmySim army = army("Unscouted", nacao("f"));
        army.getPelotoes().clear();
        army.setSizeBandLandIndex(3);
        final CombatScenario scenario = scenarioWith(army, CombatScenario.Provenance.ESTIMATED);

        ScenarioDefaults.fill(scenario,
                ScenarioDefaults.Sample.from(Arrays.asList(counted(3, 1700, 50))), NONE);

        assertEquals(CombatScenario.Provenance.ESTIMATED,
                scenario.getProvenance(army.getPelotoes().values().iterator().next()));
    }

    /** An army scouted to visibility 4 already carries the real head count. Leave it. */
    @Test
    public void anArmyThatAlreadyHasACountKeepsIt() {
        final ArmySim army = army("Scouted", nacao("f"), platoon(NONE, 3357));
        army.setComandante(0);
        final CombatScenario scenario = scenarioWith(army, CombatScenario.Provenance.ESTIMATED);

        final ScenarioDefaults.Filled filled = ScenarioDefaults.fill(scenario,
                ScenarioDefaults.Sample.from(Arrays.asList(counted(4, 9999, 50))), NONE);

        assertEquals(0, filled.getHeadCounts());
        assertEquals(3357, army.getPelotoes().values().iterator().next().getQtd());
    }

    /** The player's own armies are exact. Nothing here may touch them. */
    @Test
    public void ownArmiesAreNeverTouched() {
        final ArmySim mine = army("Mine", nacao("m"), platoon(INF, 900));
        mine.setMoral(0);
        mine.setComandante(40);
        final CombatScenario scenario = scenarioWith(mine, CombatScenario.Provenance.EXACT);

        final ScenarioDefaults.Filled filled = ScenarioDefaults.fill(scenario,
                ScenarioDefaults.Sample.from(Arrays.asList(counted(2, 300, 55))), NONE);

        assertEquals(0, filled.getTotal());
        assertEquals(0, mine.getMoral(), "a zero morale of my own is my own business");
        assertEquals(40, mine.getComandantePericia());
    }

    /** Nor anything the player typed himself. */
    @Test
    public void manualArmiesAreNeverTouched() {
        final ArmySim typed = army("Typed", nacao("f"), platoon(INF, 900));
        typed.setMoral(0);
        final CombatScenario scenario = scenarioWith(typed, CombatScenario.Provenance.MANUAL);

        assertEquals(0, ScenarioDefaults.fill(scenario,
                ScenarioDefaults.Sample.from(Arrays.asList(counted(2, 300, 55))), NONE).getTotal());
        assertEquals(0, typed.getMoral());
    }

    /** Pressing it twice must be the same as pressing it once. */
    @Test
    public void asecondFillChangesNothing() {
        final ArmySim army = army("Unscouted", nacao("f"));
        army.getPelotoes().clear();
        army.setMoral(0);
        army.setComandante(40);
        army.setSizeBandLandIndex(3);
        final CombatScenario scenario = scenarioWith(army, CombatScenario.Provenance.ESTIMATED);
        final ScenarioDefaults.Sample sample = ScenarioDefaults.Sample.from(
                Arrays.asList(counted(3, 1700, 50)));

        final ScenarioDefaults.Filled first = ScenarioDefaults.fill(scenario, sample, NONE);
        final ScenarioDefaults.Filled second = ScenarioDefaults.fill(scenario, sample, NONE);

        assertEquals(3, first.getTotal(), "morale, commander and head count");
        assertEquals(0, second.getTotal(), "and nothing left to do");
        assertEquals(45, army.getComandantePericia(), "not 50 - it did not band-shift twice");
        assertEquals(1, army.getPelotoes().size());
        assertEquals(1700, army.getPelotoes().values().iterator().next().getQtd());
    }

    /** No placeholder in the catalogue: say so rather than quietly skip the count. */
    @Test
    public void aMissingPlaceholderIsReportedRatherThanIgnored() {
        final ArmySim army = army("Unscouted", nacao("f"));
        army.getPelotoes().clear();
        army.setSizeBandLandIndex(3);
        final CombatScenario scenario = scenarioWith(army, CombatScenario.Provenance.ESTIMATED);

        final ScenarioDefaults.Filled filled = ScenarioDefaults.fill(scenario,
                ScenarioDefaults.Sample.from(Arrays.asList(counted(3, 1700, 50))), null);

        assertTrue(filled.isPlaceholderMissing());
        assertEquals(0, filled.getHeadCounts());
        assertTrue(army.getPelotoes().isEmpty());
    }

    /** An unranked army (band 0) has no band to fit, and 0 is an absence rather than "tiny". */
    @Test
    public void anUnrankedArmyGetsNoHeadCount() {
        final ArmySim army = army("Unranked", nacao("f"));
        army.getPelotoes().clear();
        army.setSizeBandLandIndex(0);
        final CombatScenario scenario = scenarioWith(army, CombatScenario.Provenance.ESTIMATED);

        final ScenarioDefaults.Filled filled = ScenarioDefaults.fill(scenario,
                ScenarioDefaults.Sample.from(Arrays.asList(counted(3, 1700, 50))), NONE);

        assertEquals(0, filled.getHeadCounts());
        assertTrue(army.getPelotoes().isEmpty());
    }

    /**
     * An empty file fills only what needs no file, and must not divide by zero doing it.
     *
     * The commander rule is the one that survives here, and that is the point rather than an
     * accident: it is arithmetic on a number the server already disclosed, so it works in a game
     * where the player can see nothing else. Morale and the head count both need a sample and
     * correctly decline to guess without one.
     */
    @Test
    public void anEmptySampleFillsOnlyWhatNeedsNoSample() {
        final ArmySim army = army("Unscouted", nacao("f"));
        army.getPelotoes().clear();
        army.setMoral(0);
        army.setComandante(40);
        army.setSizeBandLandIndex(3);
        final CombatScenario scenario = scenarioWith(army, CombatScenario.Provenance.ESTIMATED);

        final ScenarioDefaults.Filled filled = ScenarioDefaults.fill(scenario,
                ScenarioDefaults.Sample.from(new ArrayList<Exercito>()), NONE);

        assertEquals(45, army.getComandantePericia(), "the commander rule needs no sample");
        assertEquals(1, filled.getCommanders());
        assertEquals(0, filled.getMorale(), "and morale will not be guessed without one");
        assertEquals(0, filled.getHeadCounts());
        assertFalse(filled.isPlaceholderMissing(), "there was no band to fit, not a missing type");
        assertEquals(0, army.getMoral());
        assertTrue(army.getPelotoes().isEmpty());
    }

    /** A null sample or scenario is a caller bug, not a crash. */
    @Test
    public void nullsAreSurvivable() {
        assertEquals(0, ScenarioDefaults.fill(null, ScenarioDefaults.Sample.from(
                new ArrayList<Exercito>()), NONE).getTotal());
        assertEquals(0, ScenarioDefaults.fill(new CombatScenario(null, hex()), null, NONE)
                .getTotal());
        final List<Exercito> nulls = new ArrayList<>();
        nulls.add(null);
        assertEquals(0, ScenarioDefaults.Sample.from(nulls).getMoraleMean());
    }
}
