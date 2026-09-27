package business.combat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import model.Exercito;
import model.Pelotao;
import model.TipoTropa;

/**
 * Fills the blanks an enemy army arrives with, using the player's own intelligence. T-837.
 *
 * <h3>What this is for</h3>
 *
 * A foreign army reaches the client with holes in it, and they are holes rather than zeroes: the
 * server never sends {@code vl_moral} for somebody else's army, so morale reads 0 when it is worth
 * up to a quarter of the attack; at visibility 1 it sends no platoons at all, so a host the player
 * can plainly see is "vast" counts as zero troops and every layer declines it. Typing all of that
 * in by hand is the thing that stops a player running the simulation at all.
 *
 * John, 2026-09-27: <i>"an option to Fill in all default values with average numbers? i.e. average
 * morale, average number of troops for the army size, average commander skill (base +5) for all
 * unknowns"</i>.
 *
 * <h3>Averages of WHAT - the part that makes this honest rather than invented</h3>
 *
 * Every figure here is measured from the same EGF, never from a constant somebody picked:
 *
 * <ul>
 *   <li><b>Troops</b> come from the size band, and the band is a PERCENTILE, not a size:
 *       {@code MilestoneCalculos} ranks every army in the game and buckets it, so "huge army" means
 *       "top 15% of armies in THIS game" and carries no absolute number. A fixed band-to-count
 *       table would be wrong in every scenario and wrong again ten turns later. But the same EGF
 *       hands over the distribution - every army the player CAN count carries its band too - so the
 *       sample is fitted from his own file each time. See T-835.</li>
 *   <li><b>Morale</b> is the mean of the armies he can read.</li>
 *   <li><b>Commander skill</b> is the only one with an exact answer, and it is arithmetic rather
 *       than a fit. {@code ServerPersonagemDao.seeCommander} ships a foreign commander as
 *       {@code p_comandante / 10 * 10} - its own comment says <i>"arredondando a pericia para a
 *       dezena (40, 50,...)"</i> - so a commander who arrives as 40 is somewhere in 40..49 and his
 *       expected skill is 45. That is John's "base + 5", and it is the midpoint of a band the
 *       server disclosed rather than a guess about a number it withheld.</li>
 * </ul>
 *
 * <h3>What it deliberately does NOT do: invent a composition</h3>
 *
 * John ruled on this, 2026-09-27, when asked directly. A band gives a HEAD COUNT and nothing about
 * what those men are, so a filled army gets ONE platoon of the placeholder type {@code none} -
 * which is exactly what the server itself sends at visibility 4 - and not a fabricated mix.
 *
 * <b>The consequence has to be said out loud, because it is large.</b> {@code none} carries attack
 * and defence of 1 on every terrain, so a filled army of 3,000 fights at about 3,000 where a real
 * 3,000-man host would be worth fifty times that. The filled number is therefore a FLOOR on the
 * enemy, not an estimate of him, and the result says so. Filling the count is still worth doing: it
 * turns a battle the tool refuses to run into one it will run, with the head count right and the
 * quality openly unknown, which is the state the player is actually in.
 *
 * For the same reason an army scouted to visibility 4 - where the server already sent the exact
 * head count as a {@code none} platoon - is left alone except for its morale and commander. Its
 * count is real data, and retyping its composition would replace intelligence with a guess.
 *
 * <h3>Only ESTIMATED armies, and only empty values</h3>
 *
 * The player's own armies are exact and are never touched. Neither is anything he typed himself:
 * this only ever writes where the value is absent, so pressing the button twice changes nothing the
 * second time, and it can be pressed after hand-editing without undoing the edits.
 */
public final class ScenarioDefaults {

    /** The decade the server rounds a foreign commander's skill down to. */
    public static final int COMMANDER_BAND = 10;
    /** Half of it: the expected skill inside a disclosed band. John's "base + 5". */
    public static final int COMMANDER_MIDPOINT = COMMANDER_BAND / 2;
    /** Bands run 1..5; 0 means the server did not rank this army at all. */
    private static final int BANDS = 6;

    private ScenarioDefaults() {
    }

    /**
     * What the player can already count, which is what every estimate here is fitted to.
     *
     * Built from the whole map rather than from the battle's hex: a hex holds two or three armies
     * and a band is a ranking across the entire game, so a sample of three says nothing. The
     * caller supplies the armies because walking the world is the client's job, not this class's.
     */
    public static final class Sample {

        private final int[] bandMean = new int[BANDS];
        private final int[] bandCount = new int[BANDS];
        private int moraleMean;
        private int moraleCount;

        /**
         * @param armies every army the player's file describes, from anywhere on the map.
         */
        public static Sample from(Collection<Exercito> armies) {
            final Sample ret = new Sample();
            final List<List<Integer>> byBand = new ArrayList<>();
            for (int ii = 0; ii < BANDS; ii++) {
                byBand.add(new ArrayList<Integer>());
            }
            int moraleSum = 0;
            for (Exercito army : armies == null ? new ArrayList<Exercito>() : armies) {
                if (army == null) {
                    continue;
                }
                if (army.getMoral() > 0) {
                    moraleSum += army.getMoral();
                    ret.moraleCount++;
                }
                final int band = army.getTamanhoExercito();
                final int troops = landTroops(army.getPelotoes().values());
                // A zero count is the very hole this class fills, so it must not be averaged into
                // the answer - it would drag every band toward zero in exactly the games where the
                // player can see least, which is where he needs the estimate most.
                if (band > 0 && band < BANDS && troops > 0) {
                    byBand.get(band).add(troops);
                }
            }
            ret.moraleMean = ret.moraleCount == 0 ? 0 : moraleSum / ret.moraleCount;
            for (int band = 1; band < BANDS; band++) {
                final List<Integer> counts = byBand.get(band);
                int sum = 0;
                for (Integer one : counts) {
                    sum += one;
                }
                ret.bandCount[band] = counts.size();
                ret.bandMean[band] = counts.isEmpty() ? 0 : sum / counts.size();
            }
            return ret;
        }

        /** The mean land head count of the armies the player can count in this band, or 0. */
        public int troopsForBand(int band) {
            return band > 0 && band < BANDS ? bandMean[band] : 0;
        }

        /** How many armies that mean rests on. One army is a sample of one; say so if it shows. */
        public int armiesInBand(int band) {
            return band > 0 && band < BANDS ? bandCount[band] : 0;
        }

        public int getMoraleMean() {
            return moraleMean;
        }

        public int getMoraleArmies() {
            return moraleCount;
        }
    }

    /** What a fill actually did, so the window can say it rather than change things silently. */
    public static final class Filled {

        private int morale;
        private int commanders;
        private int headCounts;
        private boolean placeholderMissing;

        public int getMorale() {
            return morale;
        }

        public int getCommanders() {
            return commanders;
        }

        public int getHeadCounts() {
            return headCounts;
        }

        /** True when an army needed a head count and the scenario has no {@code none} type. */
        public boolean isPlaceholderMissing() {
            return placeholderMissing;
        }

        public int getTotal() {
            return morale + commanders + headCounts;
        }
    }

    /**
     * Writes the sample's averages into every hole of every ESTIMATED army.
     *
     * @param placeholder the {@code none} troop type from this scenario's catalogue, or null when
     *                    it carries none - in which case head counts are skipped and
     *                    {@link Filled#isPlaceholderMissing} says so instead of the count silently
     *                    not appearing.
     */
    public static Filled fill(CombatScenario scenario, Sample sample, TipoTropa placeholder) {
        final Filled ret = new Filled();
        if (scenario == null || sample == null) {
            return ret;
        }
        for (ArmySim army : scenario.getArmies()) {
            if (scenario.getProvenance(army) != CombatScenario.Provenance.ESTIMATED) {
                continue;
            }
            if (army.getMoral() <= 0 && sample.getMoraleMean() > 0) {
                army.setMoral(sample.getMoraleMean());
                ret.morale++;
            }
            if (isBandBaseOnly(army.getComandantePericia())) {
                army.setComandante(army.getComandantePericia() + COMMANDER_MIDPOINT);
                ret.commanders++;
            }
            if (fillHeadCount(scenario, army, sample, placeholder, ret)) {
                ret.headCounts++;
            }
        }
        return ret;
    }

    /**
     * A foreign commander's skill always arrives on a multiple of ten, because that is what the
     * server rounded it to. Zero is a different thing entirely - a garrison, which HAS no commander
     * - so it is left where it is rather than promoted to 5.
     */
    private static boolean isBandBaseOnly(int pericia) {
        return pericia > 0 && pericia % COMMANDER_BAND == 0;
    }

    /**
     * The visibility-1 case: a band, a name, and no platoons at all.
     *
     * Guarded on having NO land troops rather than on the platoon list being empty, because a
     * fleet arrives with its ships and no cargo and that is the same hole - its land band is the
     * size of the force it can put ashore, which is the interesting number for a fleet off a coast.
     */
    private static boolean fillHeadCount(CombatScenario scenario, ArmySim army, Sample sample,
            TipoTropa placeholder, Filled ret) {
        if (landTroops(army.getPelotoes().values()) > 0) {
            return false;
        }
        final int troops = sample.troopsForBand(army.getSizeBandLandIndex());
        if (troops <= 0) {
            return false;
        }
        if (placeholder == null) {
            ret.placeholderMissing = true;
            return false;
        }
        final Pelotao pelotao = new Pelotao();
        pelotao.setTipoTropa(placeholder);
        pelotao.setQtd(troops);
        army.getPelotoes().put(pelotao.getCodigo(), pelotao);
        // ESTIMATED, not MANUAL: the player did not type this and it must keep reporting itself as
        // somebody else's army. MANUAL is what an edit means, and it would quietly remove this
        // army from the "(?)" accounting that tells him how much of the answer is guesswork.
        scenario.setProvenance(pelotao, CombatScenario.Provenance.ESTIMATED);
        return true;
    }

    /** Bodies, not hulls: ships are a different layer and a different band. */
    private static int landTroops(Collection<Pelotao> platoons) {
        int ret = 0;
        for (Pelotao pelotao : platoons) {
            if (pelotao != null && pelotao.getTipoTropa() != null
                    && !pelotao.getTipoTropa().isBarcos()) {
                ret += pelotao.getQtd();
            }
        }
        return ret;
    }

    /** The placeholder the server itself uses for troops it will not identify. */
    public static TipoTropa placeholderOf(model.Cenario cenario) {
        return cenario == null ? null : cenario.getTipoTropas().get(PLACEHOLDER_CODE);
    }

    /** {@code ex_tipo_tropa.cd_tropa}, and it is global - the same row in every scenario. */
    public static final String PLACEHOLDER_CODE = "none";
}
