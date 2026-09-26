package business.combat;

import business.facade.CenarioFacade;
import com.thoughtworks.xstream.XStream;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import model.Cenario;
import model.Cidade;
import model.Nacao;
import model.Partida;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;

/**
 * A BattleSim scenario as TEXT, and back again. T-844.
 *
 * <h3>One format, four transports</h3>
 *
 * Copy, paste, save, load and share are the same problem wearing different hats: write scenario
 * state as text, read it back somewhere else. Only the transport differs - the system clipboard for
 * a fragment, a file for a whole battle, that same file attached to an email for an ally. So this
 * is built once and the transports sit on top of it. Four serialisations built separately would
 * disagree, and the disagreement would surface as an ally opening a shared battle and seeing
 * different numbers, which is the one outcome a sharing feature cannot have.
 *
 * <h3>This is NOT an EGF and must never be mistaken for one</h3>
 *
 * It uses XStream because the EGF proves XStream survives twenty years of model change, and because
 * it is already a dependency. It shares nothing else: its own root element, its own DTOs, its own
 * version number, and a different file extension. Constraint 4 is about not breaking EGF
 * compatibility, and the cheapest way to honour that is for this file to be obviously a different
 * thing. Nothing here writes a {@code model.*} class tag.
 *
 * <h3>What travels, and what is looked up</h3>
 *
 * Everything the receiving Counselor already owns travels as a CODE and is resolved on arrival:
 * nations, terrain, troop types. Writing them out whole would drag a copy of the world into the
 * file, and an ally would load a stale one. What travels as VALUES is what the player actually
 * edited: the armies, their platoons, the city's size, fortification and loyalty, and the
 * relationship matrix.
 *
 * <h3>A mismatch declines the WHOLE thing</h3>
 *
 * John, 2026-09-26: "we can decline pasting or loading when there is a mismatch. i.e. a terrain,
 * tactic or troop type that is not in this EGF will decline the entire paste/load." All or nothing:
 * a partial load is a scenario the player did not build and cannot see the seams of, while a
 * refusal is obvious and recoverable. {@link TransferException} carries WHICH code failed, because
 * "cannot load this file" is a dead end and "this file uses troop type X, which this game does not
 * have" tells him it came from another scenario and that retrying will not help.
 *
 * <h3>Provenance on arrival is MANUAL</h3>
 *
 * John, same day: "We assume we are streamlining player typing stuff." Which is what these features
 * are - a paste is the player typing faster, not the simulator learning something. It is also the
 * honest answer: MANUAL carries no "(?)" marker, makes no EXACT or ESTIMATED claim, and does not
 * inflate the unknown-morale count, so an ally reads "these are his figures".
 */
public final class BattleSimTransfer {

    /**
     * Bumped when the shape changes in a way an older reader could misread.
     *
     * Checked on read and refused when it is from the future, because a silently ignored unknown
     * field is how a shared battle loads looking complete and is not.
     */
    public static final int VERSION = 1;

    /** The root tag, deliberately nothing an EGF would ever contain. */
    private static final String ROOT = "battlesim";

    private BattleSimTransfer() {
    }

    /** Refused, and WHY, in terms the player can act on. */
    public static class TransferException extends Exception {

        private static final long serialVersionUID = 1L;
        /** A label key naming the kind of thing that was missing. */
        private final String reasonKey;
        /** The offending code, verbatim, so the message can quote it. */
        private final String offending;

        TransferException(String reasonKey, String offending) {
            super(reasonKey + ": " + offending);
            this.reasonKey = reasonKey;
            this.offending = offending;
        }

        public String getReasonKey() {
            return reasonKey;
        }

        public String getOffending() {
            return offending;
        }
    }

    // ------------------------------------------------------------------ write

    /** The whole scenario as text: the save file, and the clipboard payload. */
    public static String write(CombatScenario scenario) {
        final Transfer dto = new Transfer();
        dto.version = VERSION;
        dto.hex = scenario.getLocal() == null ? null : scenario.getLocal().getCoordenadas();
        dto.terreno = scenario.getTerreno() == null ? null : scenario.getTerreno().getCodigo();
        dto.cityParticipates = scenario.isCityParticipates();
        dto.city = cityOf(scenario.getCidade());
        for (ArmySim army : scenario.getArmies()) {
            dto.armies.add(armyOf(army));
        }
        // The EDITS, not the derived matrix. getRelationships() is built over getNacoes(), which
        // is the armies present plus the ACTIVE city's owner - so a nation the player declared on
        // whose army is not on the hex, or whose city is switched off, has no row there and its
        // declaration would not travel. Found by a round-trip test losing exactly that case.
        for (java.util.Map.Entry<Nacao, java.util.Map<Nacao, Integer>> row
                : scenario.getRelationshipEdits().entrySet()) {
            for (java.util.Map.Entry<Nacao, Integer> cell : row.getValue().entrySet()) {
                final Rel rel = new Rel();
                rel.from = row.getKey().getCodigo();
                rel.to = cell.getKey().getCodigo();
                rel.valor = cell.getValue();
                dto.relationships.add(rel);
            }
        }
        return xstream().toXML(dto);
    }

    private static Army armyOf(ArmySim army) {
        final Army ret = new Army();
        ret.codigo = army.getCodigo();
        ret.nome = army.getNome();
        ret.nacao = army.getNacao() == null ? null : army.getNacao().getCodigo();
        ret.tactic = army.getTatica();
        ret.moral = army.getMoral();
        ret.comandante = army.getComandantePericia();
        ret.combatLevel = army.getCombatLevel() == null ? null : army.getCombatLevel().name();
        ret.target = army.getTargetNacao() == null ? null : army.getTargetNacao().getCodigo();
        ret.attackBonus = army.getAttackBonus();
        ret.defenseBonus = army.getArmyDefenseBonus();
        for (Pelotao pelotao : army.getPelotoes().values()) {
            final Platoon one = new Platoon();
            one.tipoTropa = pelotao.getTipoTropa() == null ? null
                    : pelotao.getTipoTropa().getCodigo();
            one.qtd = pelotao.getQtd();
            one.treino = pelotao.getTreino();
            one.modAtaque = pelotao.getModAtaque();
            one.modDefesa = pelotao.getModDefesa();
            ret.platoons.add(one);
        }
        return ret;
    }

    private static City cityOf(Cidade cidade) {
        if (cidade == null) {
            return null;
        }
        final City ret = new City();
        ret.codigo = cidade.getCodigo();
        ret.nome = cidade.getNome();
        ret.tamanho = cidade.getTamanho();
        ret.fortificacao = cidade.getFortificacao();
        ret.lealdade = cidade.getLealdade();
        ret.docas = cidade.getDocas();
        ret.nacao = cidade.getNacao() == null ? null : cidade.getNacao().getCodigo();
        return ret;
    }

    // ------------------------------------------------------------------ read

    /**
     * Rebuilds a scenario in the RECEIVER'S world, or refuses and says what it choked on.
     *
     * @param nacoes the receiving world's nations, by code - {@code World.getNacoes()}. Passed in
     *               rather than reached for, because {@code World} is the Counselor's and this
     *               class is shared.
     */
    public static CombatScenario read(String xml, Partida partida,
            SortedMap<String, Nacao> nacoes) throws TransferException {
        final Transfer dto = parse(xml);
        if (dto.version > VERSION) {
            // An unknown field silently ignored is how a shared battle loads looking complete and
            // is not. Refusing is the only honest answer to a file from the future.
            throw new TransferException("BATTLESIM.TRANSFER.VERSION", String.valueOf(dto.version));
        }
        final Cenario cenario = partida == null ? null : partida.getCenario();
        final CombatScenario ret = new CombatScenario();
        ret.setPartida(partida);
        ret.setTerreno(dto.terreno == null ? null : terreno(cenario, dto.terreno));
        for (Army one : dto.armies) {
            ret.addArmy(army(one, cenario, nacoes), CombatScenario.Provenance.MANUAL);
        }
        if (dto.city != null) {
            ret.setCidade(city(dto.city, nacoes));
        }
        ret.setCityParticipates(dto.cityParticipates);
        for (Rel rel : dto.relationships) {
            ret.setRelacionamento(nacao(nacoes, rel.from), nacao(nacoes, rel.to), rel.valor);
        }
        return ret;
    }

    private static ArmySim army(Army dto, Cenario cenario, SortedMap<String, Nacao> nacoes)
            throws TransferException {
        final Nacao nacao = nacao(nacoes, dto.nacao);
        final ArmySim ret = new ArmySim(dto.nome, null, nacao);
        ret.setCodigo(dto.codigo);
        // TACTICS ARE A DECLINE CASE, and the first reading of this said they were not - see T-822.
        // The two engine families fill DIFFERENT, overlapping index sets of one array, so an index
        // from the wrong family reads a cell nobody filled, which is zero, and a modTatica of zero
        // multiplies the army's whole troop attack away. Silently. Checking it here is the only
        // place that can catch it.
        if (!isTacticValid(cenario, dto.tactic)) {
            throw new TransferException("BATTLESIM.TRANSFER.TACTIC", String.valueOf(dto.tactic));
        }
        ret.setTatica(dto.tactic);
        ret.setMoral(dto.moral);
        ret.setComandante(dto.comandante);
        ret.setCombatLevel(combatLevel(dto.combatLevel));
        ret.setTargetNacao(dto.target == null ? null : nacao(nacoes, dto.target));
        ret.setBonusAttack(dto.attackBonus);
        ret.setBonusDefense(dto.defenseBonus);
        for (Platoon one : dto.platoons) {
            final Pelotao pelotao = new Pelotao();
            pelotao.setTipoTropa(tipoTropa(cenario, one.tipoTropa));
            pelotao.setQtd(one.qtd);
            pelotao.setTreino(one.treino);
            pelotao.setModAtaque(one.modAtaque);
            pelotao.setModDefesa(one.modDefesa);
            ret.getPelotoes().put(pelotao.getCodigo(), pelotao);
        }
        return ret;
    }

    private static Cidade city(City dto, SortedMap<String, Nacao> nacoes) throws TransferException {
        final Cidade ret = new Cidade();
        ret.setCodigo(dto.codigo);
        ret.setNome(dto.nome);
        ret.setTamanho(dto.tamanho);
        ret.setFortificacao(dto.fortificacao);
        ret.setLealdade(dto.lealdade);
        ret.setDocas(dto.docas);
        if (dto.nacao != null) {
            ret.setNacao(nacao(nacoes, dto.nacao));
        }
        return ret;
    }

    // ------------------------------------------------------------------ lookups, each of which can refuse

    private static Nacao nacao(SortedMap<String, Nacao> nacoes, String codigo)
            throws TransferException {
        if (codigo == null) {
            return null;
        }
        final Nacao ret = nacoes == null ? null : nacoes.get(codigo);
        if (ret == null) {
            throw new TransferException("BATTLESIM.TRANSFER.NACAO", codigo);
        }
        return ret;
    }

    private static Terreno terreno(Cenario cenario, String codigo) throws TransferException {
        final Terreno ret = cenario == null || cenario.getTerrenos() == null
                ? null : cenario.getTerrenos().get(codigo);
        if (ret == null) {
            throw new TransferException("BATTLESIM.TRANSFER.TERRENO", codigo);
        }
        return ret;
    }

    private static TipoTropa tipoTropa(Cenario cenario, String codigo) throws TransferException {
        if (codigo == null) {
            throw new TransferException("BATTLESIM.TRANSFER.TROOP", "");
        }
        for (TipoTropa tipo : new CenarioFacade().getTipoTropas(cenario)) {
            if (codigo.equalsIgnoreCase(tipo.getCodigo())) {
                return tipo;
            }
        }
        throw new TransferException("BATTLESIM.TRANSFER.TROOP", codigo);
    }

    private static CombatLevel combatLevel(String name) {
        for (CombatLevel one : CombatLevel.values()) {
            if (one.name().equals(name)) {
                return one;
            }
        }
        // Not a decline: intent is not scenario data, so a name this build does not know is an
        // older or newer file rather than a foreign world. The window's own default is the honest
        // fallback and the player can see it in the combo.
        return null;
    }

    /**
     * Which tactic indices the RECEIVING scenario actually fills. See T-822.
     *
     * {@code CenarioFacade.bonusTatica} is {@code int[10][10]}, all zeros, filled by one of two
     * loaders: the traditional set fills 0-5, and the {@code ;ST2;} set fills 0, 1, 3, 6, 7, 8, 9.
     * The two overlap and neither contains the other.
     */
    private static boolean isTacticValid(Cenario cenario, int tactic) {
        final boolean alternate = cenario != null && cenario.hasHabilidade(";ST2;");
        if (alternate) {
            return tactic == 0 || tactic == 1 || tactic == 3
                    || (tactic >= 6 && tactic <= 9);
        }
        return tactic >= 0 && tactic <= 5;
    }

    // ------------------------------------------------------------------ xml

    private static Transfer parse(String xml) throws TransferException {
        try {
            final Object ret = xstream().fromXML(xml);
            if (!(ret instanceof Transfer)) {
                throw new TransferException("BATTLESIM.TRANSFER.FORMAT", "");
            }
            return (Transfer) ret;
        } catch (TransferException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            // Anything XStream throws means this is not one of our files - a truncated clipboard,
            // an EGF pasted by mistake, someone's shopping list. One answer for all of them.
            throw new TransferException("BATTLESIM.TRANSFER.FORMAT", "");
        }
    }

    private static XStream xstream() {
        final XStream ret = new XStream();
        // ONLY these four. A wildcard over model.** is what an EGF reader needs and would let this
        // one deserialise arbitrary world classes from a file a stranger emailed.
        ret.allowTypes(new Class[]{Transfer.class, Army.class, Platoon.class, City.class,
            Rel.class});
        ret.alias(ROOT, Transfer.class);
        ret.alias("army", Army.class);
        ret.alias("platoon", Platoon.class);
        ret.alias("city", City.class);
        ret.alias("rel", Rel.class);
        return ret;
    }

    // ------------------------------------------------------------------ the wire shape

    /** Plain fields, no behaviour: the file's shape is this class and nothing else. */
    static class Transfer {

        int version;
        String hex;
        String terreno;
        boolean cityParticipates;
        City city;
        List<Army> armies = new ArrayList<>();
        List<Rel> relationships = new ArrayList<>();
    }

    static class Army {

        String codigo;
        String nome;
        String nacao;
        int tactic;
        int moral;
        int comandante;
        String combatLevel;
        String target;
        int attackBonus;
        int defenseBonus;
        List<Platoon> platoons = new ArrayList<>();
    }

    static class Platoon {

        String tipoTropa;
        int qtd;
        int treino;
        int modAtaque;
        int modDefesa;
    }

    static class City {

        String codigo;
        String nome;
        int tamanho;
        int fortificacao;
        int lealdade;
        int docas;
        String nacao;
    }

    static class Rel {

        String from;
        String to;
        int valor;
    }
}
