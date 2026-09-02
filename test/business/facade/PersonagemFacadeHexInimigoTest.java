package business.facade;

import model.Exercito;
import model.Local;
import model.Nacao;
import model.Personagem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code isInHexInimigo} backs the "exi" requisito, which is the only thing keeping "Attack Enemy"
 * out of the order combo. It must ask whether the BASE nation considers the other one an enemy, and
 * never the reverse.
 *
 * The reverse is not merely a style question, it is unanswerable on the client: the EGF carries a
 * foreign nation's relationship only when it is POSITIVE and aimed at the player, so a HOSTILE
 * nation arrives with an EMPTY relacionamentos map. {@code Nacao.getRelacionamento} then swallows
 * the null unboxing and answers 0 = neutral, so the reversed form is silently always false and hid
 * the order from every player in every game off ;SPD; (v2.1.924, game 901 turn 6).
 *
 * Every enemy here is therefore built the way the EGF actually delivers one - relationship set on
 * MY nation only, nothing on theirs. A test that set both sides would pass on the broken code.
 */
class PersonagemFacadeHexInimigoTest {

    private final PersonagemFacade facade = new PersonagemFacade();

    private static Nacao nacao(String codigo) {
        final Nacao ret = new Nacao();
        ret.setNome(codigo);
        ret.setCodigo(codigo);
        return ret;
    }

    /** An army of {@code dono} standing in {@code local}, keyed the way the EGF keys them. */
    private static void army(Local local, Nacao dono, String id) {
        final Exercito ret = new Exercito();
        ret.setCodigo(id);
        ret.setNome(id);
        ret.setNacao(dono);
        ret.setLocal(local);
        local.getExercitos().put(id, ret);
    }

    private static Personagem personagem(Nacao minha, Local local) {
        final Personagem ret = new Personagem();
        ret.setCodigo("lenwood");
        ret.setNome("Lenwood Tawney");
        ret.setNacao(minha);
        ret.setLocal(local);
        return ret;
    }

    private static Local hex() {
        final Local ret = new Local();
        ret.setCoordenadas("0647");
        return ret;
    }

    /**
     * The live shape: Greyjoy holds -2 toward Lannister, Lannister holds NOTHING. This is the
     * regression - it fails on the reversed check and passes on the correct one.
     */
    @Test
    void enemyArmyIsFoundWhenOnlyMyNationCarriesTheRelationship() {
        final Nacao greyjoy = nacao("House Greyjoy");
        final Nacao lannister = nacao("House Lannister");
        greyjoy.addRelacionamento(lannister, -2);

        final Local local = hex();
        army(local, lannister, "3205173");

        assertTrue(facade.isInHexInimigo(personagem(greyjoy, local)));
    }

    /** An ally in the hex is not something to attack. */
    @Test
    void alliedArmyIsNotAnEnemy() {
        final Nacao greyjoy = nacao("House Greyjoy");
        final Nacao martell = nacao("House Martell");
        greyjoy.addRelacionamento(martell, 2);

        final Local local = hex();
        army(local, martell, "3205319");

        assertFalse(facade.isInHexInimigo(personagem(greyjoy, local)));
    }

    /**
     * Neutral is not an enemy: isInimigo is strictly negative, which is the same line the Judge
     * fights on. A nation absent from the map also reads 0, so this covers both.
     */
    @Test
    void neutralArmyIsNotAnEnemy() {
        final Nacao greyjoy = nacao("House Greyjoy");
        final Nacao watch = nacao("Night's Watch");

        final Local local = hex();
        army(local, watch, "3205324");

        assertFalse(facade.isInHexInimigo(personagem(greyjoy, local)));
    }

    /** My own army does not make the hex hostile. getRelacionamento(self) is 0 by contract. */
    @Test
    void ownArmyIsNotAnEnemy() {
        final Nacao greyjoy = nacao("House Greyjoy");

        final Local local = hex();
        army(local, greyjoy, "3205167");

        assertFalse(facade.isInHexInimigo(personagem(greyjoy, local)));
    }

    /** One enemy among friendlies is enough - the hex in game 901 held four armies. */
    @Test
    void oneEnemyAmongFriendliesIsEnough() {
        final Nacao greyjoy = nacao("House Greyjoy");
        final Nacao martell = nacao("House Martell");
        final Nacao lannister = nacao("House Lannister");
        greyjoy.addRelacionamento(martell, 2);
        greyjoy.addRelacionamento(lannister, -2);

        final Local local = hex();
        army(local, greyjoy, "3205167");
        army(local, martell, "3205319");
        army(local, lannister, "3205173");

        assertTrue(facade.isInHexInimigo(personagem(greyjoy, local)));
    }

    /** An empty hex has nothing to attack. */
    @Test
    void emptyHexHasNoEnemy() {
        assertFalse(facade.isInHexInimigo(personagem(nacao("House Greyjoy"), hex())));
    }

    /** A garrison with no nation must not NPE the render of the combo. */
    @Test
    void armyWithoutNacaoIsIgnored() {
        final Nacao greyjoy = nacao("House Greyjoy");
        final Local local = hex();
        army(local, null, "3205167");

        assertFalse(facade.isInHexInimigo(personagem(greyjoy, local)));
    }
}
