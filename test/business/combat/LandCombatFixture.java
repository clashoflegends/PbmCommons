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

/**
 * The armies, troops and ground that the land-combat suites are built from.
 *
 * Extracted so two suites share ONE set of fixtures rather than drifting apart. That matters more
 * than usual here: several of these tests have already been wrong because the FIXTURE could not have
 * failed - a loser that is wiped out in every variant proves nothing about the variable being
 * tested, and halving attack and defence together leaves the casualty ratio identical by
 * construction. A shared fixture is one place to get that right.
 */
public class LandCombatFixture {


    protected static final Terreno PLAIN = plain();

    protected static Terreno plain() {
        final Terreno ret = new Terreno();
        ret.setCodigo("P");
        ret.setNome("Plain");
        ret.setAncoravel(true);
        return ret;
    }

    protected static final Terreno FOREST = forest();

    protected static Terreno forest() {
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
    protected static TipoTropa twoTerrainTroop(String codigo, int ataque, int defesa) {
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

    protected static SortedMap<Terreno, Integer> byTerrain(int valor) {
        final SortedMap<Terreno, Integer> ret = new TreeMap<>();
        ret.put(PLAIN, valor);
        return ret;
    }

    /** @param firstStrike gives the troop {@code ;TT1;}, the only thing that acts in round 0 */
    protected static TipoTropa troopType(String codigo, int ataque, int defesa, boolean firstStrike) {
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

    protected static Pelotao platoon(TipoTropa tipo, int qtd) {
        final Pelotao ret = new Pelotao();
        ret.setTipoTropa(tipo);
        ret.setQtd(qtd);
        ret.setTreino(50);
        return ret;
    }

    protected static Local hex() {
        final Local ret = new Local();
        ret.setCodigo("1428");
        ret.setCoordenadas("1428");
        ret.setTerreno(PLAIN);
        return ret;
    }

    protected static Nacao nacao(String codigo) {
        final Nacao ret = new Nacao();
        ret.setCodigo(codigo);
        ret.setNome(codigo);
        return ret;
    }

    protected static ArmySim army(String nome, Nacao nacao, Pelotao... pelotoes) {
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

    /** A naval troop type: {@code ;TTN;} is what makes {@code isBarcos()} true. */
    protected static TipoTropa shipType(String codigo) {
        final TipoTropa ret = troopType(codigo, 40, 40, false);
        final Habilidade naval = new Habilidade();
        naval.setCodigo(";TTN;");
        naval.setNome(";TTN;");
        ret.addHabilidade(naval);
        return ret;
    }

    protected static Cenario cenario() {
        final Cenario ret = new Cenario();
        ret.setCodigo("c1");
        ret.setNome("Scenario");
        return ret;
    }

    /** Two hostile land armies, both able to strike from round 0. */
    protected static CombatScenario twoArmiesAtWar(Pelotao mineTroops, Pelotao theirTroops) {
        final Nacao mine = nacao("m"), theirs = nacao("t");
        final Local hex = hex();
        final CombatScenario ret = new CombatScenario(null, hex);
        ret.addArmy(army("mine", mine, mineTroops), CombatScenario.Provenance.EXACT);
        ret.addArmy(army("theirs", theirs, theirTroops), CombatScenario.Provenance.ESTIMATED);
        ret.setRelacionamento(mine, theirs, RelationshipMatrix.SWORN_ENEMY);
        ret.setRelacionamento(theirs, mine, RelationshipMatrix.SWORN_ENEMY);
        return ret;
    }
}
