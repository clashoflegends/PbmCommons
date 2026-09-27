package business.combat;

import model.Artefato;
import model.Exercito;
import model.Personagem;
import model.Pelotao;
import model.TipoTropa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The public side of the T-906 seam, and the property that makes it worth having.
 *
 * These tests do not check an NPC formula - there is none on this side and there never will be, by
 * decision D-10. They check the two things the PUBLIC half has to get right: that it contributes
 * nothing, and that it ADMITS to contributing nothing so the result can disclose the gap rather
 * than let a zero pass for an answer.
 */
public class NpcContributionTest extends LandCombatFixture {

    private static final TipoTropa INF = troopType("inf", 50, 40, false);

    /** A character carrying a combat artifact worth {@code valor}. */
    private static Personagem withArtifact(String nome, boolean npc, int valor) {
        final Personagem ret = new Personagem();
        ret.setNome(nome);
        ret.setNpc(npc);
        if (valor > 0) {
            final Artefato artefato = new Artefato();
            artefato.setNome(nome + "-blade");
            artefato.setValor(valor);
            artefato.setCombate(true);
            ret.setArtefatoCombateAtivo(artefato);
        }
        return ret;
    }

    /**
     * An ArmySim built from a model army, because the commander model has no setter.
     *
     * That is deliberate on ArmySim's part - the commander is BORROWED, never edited, which is the
     * ownership boundary the whole simulator rests on - so a test that wants one has to come in
     * through the same door the loader does.
     */
    private static ArmySim armyLedBy(Personagem commander) {
        final Exercito exercito = new Exercito();
        exercito.setCodigo("e1");
        exercito.setNome("mine");
        exercito.setNacao(nacao("m"));
        exercito.setLocal(hex());
        exercito.setMoral(100);
        exercito.setComandante(commander);
        final Pelotao one = platoon(INF, 900);
        exercito.getPelotoes().put(one.getCodigo(), one);
        return new ArmySim(exercito);
    }

    @Test
    public void thePublicImplementationContributesNothingAndSaysSo() {
        final ArmySim army = army("mine", nacao("m"), platoon(INF, 900));

        assertEquals(0, NpcContributionWithheld.INSTANCE.npcAttack(army, 3, false));
        assertEquals(0, NpcContributionWithheld.INSTANCE.npcAttack(army, 0, true));
        assertFalse(NpcContributionWithheld.INSTANCE.isModelled(),
                "a zero that cannot say it is a zero-by-design is indistinguishable from a bug");
    }

    /** A null army is a caller error, not a crash on the turn-resolution path. */
    @Test
    public void nullIsSurvivable() {
        assertEquals(0, NpcContributionWithheld.INSTANCE.npcAttack(null, 1, false));
    }

    /**
     * The seam does not swallow a PLAYER character's artifact.
     *
     * The line it replaced skipped NPCs inline, and the easy mistake when turning that into a seam
     * is to skip the whole traveller loop. A player character's combat artifact is his possession,
     * it is public, and the simulator has always counted it.
     */
    @Test
    public void aPlayerCharactersArtifactStillCounts() {
        final Personagem commander = withArtifact("Ser Barristan", false, 0);
        final Personagem companion = withArtifact("Ser Jorah", false, 500);
        commander.getLiderados().put(companion.getNome(), companion);
        final ArmySim army = armyLedBy(commander);

        final long withCompanion = LandCombatResolver.getForcaPlusFlat(army, 1, false);
        commander.getLiderados().clear();
        final long without = LandCombatResolver.getForcaPlusFlat(army, 1, false);

        assertEquals(500, withCompanion - without, "his sword is his, and it is public");
    }

    /**
     * An NPC companion adds NOTHING on this side, whatever it is carrying.
     *
     * Written with an NPC holding a valuable artifact on purpose: the withheld term is the NPC's
     * own combat worth, and routing it through the seam must not accidentally start counting an
     * artifact for a character the Judge counts differently.
     */
    @Test
    public void anNpcCompanionAddsNothingHere() {
        final Personagem commander = withArtifact("Ser Barristan", false, 0);
        final Personagem dragon = withArtifact("Drogon", true, 5000);
        commander.getLiderados().put(dragon.getNome(), dragon);
        final ArmySim army = armyLedBy(commander);

        final long withDragon = LandCombatResolver.getForcaPlusFlat(army, 1, false);
        commander.getLiderados().clear();
        final long without = LandCombatResolver.getForcaPlusFlat(army, 1, false);

        assertEquals(0, withDragon - without, "withheld by design - see NpcContribution");
    }

    /**
     * And the battle SAYS an NPC was left out, which is the half that makes the zero honest.
     *
     * Without this the simulator would quietly forecast a dragon battle low. The disclosure is
     * scoped to armies that have a commander, because a garrison has none and so has nobody
     * travelling with it.
     */
    @Test
    public void thePresenceOfAnNpcIsDisclosed() {
        final CombatScenario scenario = twoArmiesAtWar(platoon(INF, 900), platoon(INF, 900));
        assertFalse(scenario.hasWithheldNpcContribution(), "nobody is travelling with anyone yet");

        final Personagem commander = withArtifact("Ser Barristan", false, 0);
        commander.getLiderados().put("Drogon", withArtifact("Drogon", true, 5000));
        scenario.remArmy(scenario.getArmies().get(1));
        scenario.addArmy(armyLedBy(commander), CombatScenario.Provenance.ESTIMATED);

        assertTrue(scenario.hasWithheldNpcContribution(),
                "a dragon on the field has to be said out loud, even unmodelled");
    }
}
