package business.combat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import model.Exercito;
import model.Local;
import model.Nacao;
import model.Partida;
import model.World;

import com.thoughtworks.xstream.XStream;

/**
 * What an EGF actually says about a whole turn: the nations, the teams, the relationship rows the
 * server chose to export, and every hex where more than one nation is standing.
 *
 * <h3>Why this is not a test</h3>
 *
 * {@link FidelityHarness} answers "does the engine reproduce THIS battle", which presumes somebody
 * already knows which battle to look at. Finding that battle is the step before, and it was being
 * done by hand - grep a turn report for a header spelling, hope the hex is in the file you loaded.
 * This reads the same EGF the Counselor reads and lists the candidates, so a validation round starts
 * from the data rather than from a guess about it.
 *
 * <h3>The relationship dump is the point</h3>
 *
 * {@link HostilityDeriver} turns on one question - whose row can be believed - and that question is
 * answered by a server-side export rule with no client-side witness. Printing every row next to
 * every team flag is how a claim about that rule gets checked against a real file instead of being
 * reasoned about. See {@code HostilityDeriver.isComplete}.
 *
 * <pre>
 * mvn -q -pl PbmCommons test-compile
 * mvn -q -pl PbmCommons exec:java -Dexec.classpathScope=test \
 *     -Dexec.mainClass=business.combat.WorldProbe -Dexec.args="&lt;file.egf&gt;"
 * </pre>
 *
 * (exec:java does NOT recompile - run test-compile first or you are reading yesterday's classes.)
 */
public final class WorldProbe {

    private WorldProbe() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("usage: WorldProbe <file.egf> [hex]");
            return;
        }
        final World world = read(new File(args[0]));
        final Partida partida = world.getPartida();
        System.out.println("GAME|" + (partida == null ? "?" : partida.getCodigo())
                + "|turn=" + (partida == null ? "?" : partida.getTurno()));
        System.out.println("TYPE|" + gameType(partida));
        System.out.println("OBSERVER|" + (partida == null || partida.getJogadorAtivo() == null
                ? "(none)" : partida.getJogadorAtivo().getNome()));
        doNations(partida, world);
        doCrowdedHexes(world);
        if (args.length > 1) {
            doHex(world, args[1]);
        }
    }

    /**
     * One hex as the BattleSim would build it: who is there, what the matrix decided, and whether
     * Run would be enabled.
     *
     * The gate is the headline. A hex can hold two armies plainly about to fight and still report
     * {@code NO_HOSTILE_PAIR}, because hostility is a property of the NATION TABLE and the table can
     * fail to answer - which is exactly the case a locked team game used to hit for two nations
     * neither of which was the observer's.
     */
    private static void doHex(World world, String hex) {
        final Local local = world.getLocal(hex);
        if (local == null) {
            System.out.println("GATE|" + hex + "|no such hex on this map");
            return;
        }
        final CombatScenario scenario = new ScenarioLoader().load(world.getPartida(), local,
                world.getPartida() == null ? null : world.getPartida().getJogadorAtivo());
        System.out.println("GATE|" + hex + "|" + scenario.getRunGate(true)
                + "|armies=" + scenario.getArmies().size()
                + "|troops=" + scenario.getQtTropasTotal()
                + "|assumedPairs=" + scenario.getAssumedCount());
        final RelationshipMatrix matrix = scenario.getRelationships();
        final List<Nacao> present = scenario.getNacoes();
        for (int ii = 0; ii < present.size(); ii++) {
            for (int jj = ii + 1; jj < present.size(); jj++) {
                final Nacao one = present.get(ii), other = present.get(jj);
                System.out.println("PAIR|" + one.getNome() + "|" + other.getNome()
                        + "|hostile=" + matrix.isHostile(one, other)
                        + "|" + matrix.getValor(one, other) + "/" + matrix.getValor(other, one)
                        + "|" + matrix.getPairOrigin(one, other));
            }
        }
        for (ArmySim army : scenario.getArmies()) {
            final LayerParticipation where = scenario.getParticipation().get(army);
            final StringBuilder layers = new StringBuilder();
            for (CombatLayer layer : CombatLayer.values()) {
                layers.append(layers.length() == 0 ? "" : ",").append(layer.name()).append('=')
                        .append(where.isIn(layer) ? "IN" : where.getReason(layer));
            }
            System.out.println("ARMY|" + army.getNome()
                    + "|" + (army.getNacao() == null ? "?" : army.getNacao().getNome())
                    + "|troops=" + new business.facade.ExercitoFacade().getQtTropasTotal(army)
                    + "|platoons=" + army.getPelotoes().size()
                    // The size BAND, which the server exports at visibility 1, 2 and 4 alike
                    // (ServerExercitoDao.setVisSize) and which the simulator currently ignores. At
                    // visibility 1 it is the ONLY thing known about an army's strength: no platoons
                    // come across at all, so getQtTropasTotal answers 0 for an army the player can
                    // plainly see is "vast".
                    + "|band=" + army.getSizeBand()
                    + "|moral=" + army.getMoral()
                    + "|moralUnknown=" + scenario.isMoraleUnknown(army)
                    + "|" + scenario.getProvenance(army)
                    + "|" + layers);
            for (model.Pelotao platoon : army.getPelotoes().values()) {
                System.out.println("  PLATOON|" + army.getNome()
                        + "|" + (platoon.getTipoTropa() == null
                                ? "?" : platoon.getTipoTropa().getCodigo())
                        + "|qtd=" + platoon.getQtd()
                        + "|treino=" + platoon.getTreino());
            }
        }
    }

    /**
     * The flags that decide relationships by construction, named rather than counted.
     *
     * These four are the whole of the game-type input: {@code ;GDM;} and {@code ;GLA;}/{@code ;GSL;}
     * each settle every pair on their own, {@code ;GND;} says the settlement cannot drift, and
     * {@code ;SPD;} is the one flag that makes every nation's row readable.
     */
    private static String gameType(Partida partida) {
        if (partida == null) {
            return "?";
        }
        final StringBuilder ret = new StringBuilder();
        for (String code : new String[]{";GDM;", ";GLA;", ";GSL;", ";GND;", ";SPD;", ";FFA;",
            ";GBR;", ";GAP;"}) {
            if (partida.hasHabilidade(code)) {
                ret.append(code);
            }
        }
        return ret.length() == 0 ? "(none of the type flags)" : ret.toString();
    }

    /**
     * One line per nation: team, owner, and the relationship row exactly as it arrived.
     *
     * The row is iterated, never queried. {@code relacionamentos} is a TreeMap keyed by Nacao
     * objects that XStream restores as references, and such a map can miss a {@code get(key)} for a
     * key that is in its own keySet - which {@code getRelacionamento} turns into a silent neutral.
     */
    private static void doNations(Partida partida, World world) {
        for (Nacao nacao : nacoes(world)) {
            final StringBuilder row = new StringBuilder();
            for (Map.Entry<Nacao, Integer> entry : nacao.getRelacionamentos().entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                row.append(row.length() == 0 ? "" : " ")
                        .append(entry.getKey().getNome()).append('=').append(entry.getValue());
            }
            System.out.println("NATION|" + nacao.getNome()
                    + "|team=" + nacao.getTeamFlag()
                    + "|owner=" + (nacao.getOwner() == null ? "-" : nacao.getOwner().getNome())
                    + "|rows=" + nacao.getRelacionamentos().size()
                    + "|" + (row.length() == 0 ? "(empty)" : row));
        }
    }

    /**
     * Every hex holding armies of two or more DIFFERENT nations: the candidate battles.
     *
     * Two nations standing together is not proof of a fight - they may be allies, and whether they
     * fight is {@link CombatScenario}'s question, not this one's. It is proof that the hex is worth
     * opening, which is all a candidate list owes.
     */
    private static void doCrowdedHexes(World world) {
        for (Local local : world.getLocais().values()) {
            if (local == null || local.getExercitos() == null) {
                continue;
            }
            final List<Nacao> present = new ArrayList<>();
            int troops = 0;
            for (Exercito army : local.getExercitos().values()) {
                if (army == null) {
                    continue;
                }
                troops += new business.facade.ExercitoFacade().getQtTropasTotal(army);
                addNacao(present, army.getNacao());
            }
            if (present.size() < 2) {
                continue;
            }
            final StringBuilder names = new StringBuilder();
            for (Nacao nacao : present) {
                names.append(names.length() == 0 ? "" : ",").append(nacao.getNome())
                        .append('/').append(nacao.getTeamFlag());
            }
            System.out.println("HEX|" + local.getCoordenadas()
                    + "|armies=" + local.getExercitos().size()
                    + "|troops=" + troops
                    + "|city=" + (local.getCidade() == null ? "-" : local.getCidade().getNome())
                    + "|" + names);
        }
    }

    /** Identity, never equals: BaseModel.compareTo collapses nations by codigo. */
    private static void addNacao(List<Nacao> list, Nacao nacao) {
        if (nacao == null) {
            return;
        }
        for (Nacao known : list) {
            if (known == nacao) {
                return;
            }
        }
        list.add(nacao);
    }

    /** Nations reached through the armies and cities, since World exposes no nation index. */
    private static List<Nacao> nacoes(World world) {
        final List<Nacao> ret = new ArrayList<>();
        for (Local local : world.getLocais().values()) {
            if (local == null) {
                continue;
            }
            if (local.getCidade() != null) {
                addNacao(ret, local.getCidade().getNacao());
            }
            if (local.getExercitos() == null) {
                continue;
            }
            for (Exercito army : local.getExercitos().values()) {
                if (army != null) {
                    addNacao(ret, army.getNacao());
                }
            }
        }
        return ret;
    }

    private static World read(File egf) throws Exception {
        final File xml = persistenceCommons.ZipManager.getInstance().doUncompressGzip(egf);
        try (InputStream is = new BufferedInputStream(new FileInputStream(xml));
                InputStreamReader reader = new InputStreamReader(is, "UTF-8")) {
            final XStream xs = new XStream();
            xs.allowTypesByWildcard(new String[]{"model.**"});
            return (World) xs.fromXML(reader);
        }
    }
}
