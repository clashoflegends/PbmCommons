/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import business.combat.ArmySim;
import business.interfaces.IExercito;
import java.io.Serializable;
import model.Cidade;
import model.Exercito;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The BattleSim's view of combat, and the one place its ownership boundary is defined.
 *
 * <h3>What the simulator owns, and what it borrows</h3>
 *
 * The player can retype anything in the BattleSim, so the rule is that every object he can edit must
 * belong to the simulator and nothing he edits may reach the world loaded from the EGF.
 *
 * <table>
 *   <tr><th>Type</th><th>Owned or shared</th><th>Why</th></tr>
 *   <tr><td>{@link ArmySim}</td><td>OWNED</td>
 *       <td>Created per simulation. Never a live {@link Exercito}.</td></tr>
 *   <tr><td>{@link Pelotao}</td><td>OWNED</td>
 *       <td>Holds quantity, training, weapon and armour, all editable. Cloned by
 *           {@code ArmySim.doClonePelotoes}.</td></tr>
 *   <tr><td>{@link TipoTropa}</td><td>shared, read only</td>
 *       <td>The scenario's troop catalogue. The simulator repoints a platoon at a different entry but
 *           never edits an entry, and copying it would break identity comparisons.</td></tr>
 *   <tr><td>{@link Cidade}</td><td>OWNED</td>
 *       <td>Holds loyalty, size and fortification, all editable and all three inputs to
 *           {@link #getCityDefenseCombat}. Cloned by {@code CombatScenario.setLocal}, shallowly, so
 *           its own {@code Nacao} / {@code Local} / {@code Terreno} stay shared. Moved here from
 *           "shared" when the city became a participant rather than scenery.</td></tr>
 *   <tr><td>{@link Nacao}, {@link Terreno}, {@link Local}</td><td>shared, read only</td>
 *       <td>Context, not content. Selected, never mutated.</td></tr>
 * </table>
 *
 * <h3>Moving the line</h3>
 *
 * When the catalogue itself becomes editable (magic items, NPCs and powers as what-if data),
 * {@code TipoTropa} moves to OWNED and gets cloned here like {@code Pelotao} is today. That is the
 * only change needed: the boundary is stated once, in this table, and enforced by
 * {@code ArmySimOwnershipTest} and {@code ArmySimWorldUnchangedTest}.
 *
 * <h3>Why this is written down</h3>
 *
 * Both {@code ArmySim} copy constructors used to do {@code platoons.putAll(source)}, which copies the
 * map and shares the platoons. Editing a platoon in the simulator therefore edited the real army for
 * the rest of the session, silently, on the one screen where a player expects to be able to try
 * anything. The fix is small; not noticing it for years was the expensive part.
 *
 * @author jmoura
 */
public class BattleSimFacade implements Serializable {

    private static final Log log = LogFactory.getLog(BattleSimFacade.class);
    private static final int[] bonusFortificacaoCumulativo = {0, 2000, 6000, 10000, 16000, 24000};
    private static final int[] bonusFortificacao = {0, 2000, 4000, 4000, 6000, 8000};
    private static final int[] bonusTamanho = {0, 200, 500, 1000, 2500, 5000};
    private final LocalFacade lf = new LocalFacade();
    private final NacaoFacade nf = new NacaoFacade();
    private final ExercitoFacade ef = new ExercitoFacade();

    public ArmySim clone(Exercito army) {
        return new ArmySim(army);
    }

    public ArmySim clone(ArmySim army) {
        return new ArmySim(army);
    }

    public Pelotao clone(Pelotao platoon) {
        return platoon.clone();
    }

    //Army methods start here
    public int getArmyAttackBase(IExercito army, String habilidade, Local local) {
        int ret = 0;
        for (Pelotao pelotao : army.getPelotoes().values()) {
            if (pelotao.getTipoTropa().hasHabilidade(habilidade)) {
                ret += this.getPlatoonAttack(pelotao, army, local);
            }
        }
        return ret;
    }

    public int getArmyAttackBaseNot(IExercito army, String habilidadeNot, Local local) {
        int ret = 0;
        for (Pelotao pelotao : army.getPelotoes().values()) {
            if (!pelotao.getTipoTropa().hasHabilidade(habilidadeNot)) {
                ret += this.getPlatoonAttack(pelotao, army, local);
            }
        }
        return ret;
    }

    public int getArmyAttackBaseLand(IExercito army, Local local) {
        return getArmyAttackBaseNot(army, ";TTN;", local);
    }

    public float getArmyAttack(IExercito exercito, boolean naval) {
        float ret = 0;
        for (Pelotao pelotao : exercito.getPelotoes().values()) {
            if (naval == pelotao.getTipoTropa().isBarcos()) {
                ret += getPlatoonAttack(pelotao, exercito, exercito.getLocal(), exercito.getTerreno());
            }
        }
        return ret;
    }

    public int getArmyAttackBonus(IExercito army) {
        return army.getAttackBonus();
    }

    private float getArmyBonusModifier(IExercito exercito) {
        return ((float) exercito.getComandantePericia() + (float) exercito.getMoral() + 200f) / 4f;
    }

    public int getArmyDefense(IExercito exercito, boolean naval) {
        //TODO: combate em terra vs. naval
        int ret = 0;
        for (Pelotao pelotao : exercito.getPelotoes().values()) {
            if (naval == pelotao.getTipoTropa().isBarcos()) {
                ret += getPlatoonDefense(exercito, pelotao);
            }
        }
        return ret;
    }

    //replaced getConstituicaoTotalLand
    public int getArmyDefenseTotalLand(IExercito army) {
        int ret = 0;
        float total = 0F;
        for (Pelotao pelotao : army.getPelotoes().values()) {
            if (!pelotao.getTipoTropa().isBarcos()) {
                total += getPlatoonDefense(army, pelotao);
            }
        }
        if (total > 0F) {
            ret += (int) total + army.getArmyDefenseBonus();
        }
        return ret;
    }

    public int getArmyDefenseBonus(IExercito army) {
        return army.getArmyDefenseBonus();
    }

    //Platoon methods start here
    public float getPlatoonAttack(Pelotao pelotao, IExercito exercito, Local local) {
        /*
        * the local can change depending on the usage, but keeps terraina nd local in sync. 
        * NPC AI trying to decide where to attack.
         */
        return getPlatoonAttack(pelotao, exercito, local, exercito.getTerreno());
    }

    public float getPlatoonAttack(Pelotao pelotao, IExercito exercito) {
        //for use in BattleSim and Judge.Combats
        return getPlatoonAttack(pelotao, exercito, exercito.getLocal(), exercito.getTerreno());
    }

    private float getPlatoonAttack(Pelotao pelotao, IExercito exercito, final Local local, final Terreno terreno) {
        float ret = 0;
        try {
            float forcaTrop = getTroopAttack(pelotao.getTipoTropa(), exercito, local, terreno)
                    * (float) pelotao.getQtd()
                    * ((float) pelotao.getTreino() + (float) pelotao.getModAtaque() + 100f)
                    / 300f;
            ret = forcaTrop * getArmyBonusModifier(exercito) / 100f;
        } catch (NullPointerException ex) {
        }
        return ret;
    }

    private float getTroopAttack(TipoTropa tpTropa, IExercito exercito, final Local local, final Terreno terreno) {
        try {
            float tropasValor = tpTropa.getAtaqueTerreno().get(terreno);
            if (tpTropa.isDoubleAttackOnAlliedCities() && lf.isCidade(local)) {
                //D = ataque dobrado se defendendo cidade aliada
                try {
                    Nacao nacaoCidade = local.getCidade().getNacao();
                    if (exercito.getNacao().equals(nacaoCidade) || nf.isAliado(exercito.getNacao(), nacaoCidade)) {
                        tropasValor = tropasValor * 2f;
                    }
                } catch (NullPointerException ex) {
                }
            }
            // Nation guard, for the same reason as getPlatoonDefense's below - but here the NPE
            // was already being caught, and that is worse rather than better. The outer catch
            // answers 0f, so an army with no visible owner reported an attack of ZERO: a real
            // number, silently wrong, indistinguishable from a genuinely harmless army. The catch
            // exists for a missing terrain entry ("nao tem a tropa, retorna forca 0") and is right
            // for that; it was never meant to absorb a nationless army.
            //
            // Scoped so it cannot change a number the Judge computes: where nacao is non-null,
            // which is every army the Judge owns, these conditions evaluate exactly as before.
            final Nacao nacao = exercito.getNacao();
            if (nacao != null && nacao.hasHabilidade(";PAB;") && !tpTropa.isBarcos()
                    && lf.getDistanciaToCapital(nacao, local)
                    <= nacao.getHabilidadeValor(";PAB;")) {
                tropasValor += tropasValor * 0.15f;
            }
            if (nacao != null && nacao.hasHabilidade(";PABN;") && tpTropa.isBarcos()
                    && lf.getDistanciaToCapital(nacao, local) <= nacao.getHabilidadeValor(";PABN;")) {
                tropasValor += tropasValor * 0.15f;
            }
            return (tropasValor);
        } catch (NullPointerException ex) {
            //nao tem a tropa, retorna forca 0
            return 0f;
        }
    }

    private float getTroopDefense(TipoTropa tpTropa, Terreno terreno) {
        try {
            return tpTropa.getDefesaTerreno().get(terreno);
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    public float getPlatoonDefense(IExercito army, Pelotao pelotao) {
        final TipoTropa tpTropa = pelotao.getTipoTropa();
        float vlConstituicao = getTroopDefense(tpTropa, army.getTerreno());
        //D = defesa eh metade do ataque outside allied or owned cities
        if (tpTropa.isHalfDefenseOutAlliedCities()) {
            try {
                final Nacao nacaoCidade = army.getLocal().getCidade().getNacao();
                if (!army.getNacao().equals(nacaoCidade) && !nf.isAliado(army.getNacao(), nacaoCidade)) {
                    vlConstituicao = vlConstituicao / 2f;
                }
            } catch (NullPointerException e) {
                vlConstituicao = vlConstituicao / 2f;
            }
        }
        //heroes leading army have bonuses
        if (ef.isHero(army) && tpTropa.hasHabilidade(";TAH;")) {
            vlConstituicao += vlConstituicao * tpTropa.getHabilidadeValor(";TAH;") / 100;
        }
        // An army with NO NATION reaches here, and these two blocks used to throw on it.
        //
        // The same guard the two getCity*Combat methods below already carry, and the same reason:
        // an ownerless actor is a real state, not a broken one. In the Counselor an army whose
        // owner is not visible arrives with a null nacao - business.combat.HostilityDeriver handles
        // exactly that case by name - and a blank army the player adds in the simulator starts
        // without one. Nothing computed a platoon's defence for such an army until the BattleSim
        // grew attack and defence columns (T-427), so nothing ever hit it.
        //
        // SHARED CODE, and the Judge calls this. Scoped so that it cannot change a single number
        // the Judge computes: where nacao is non-null - which is every army the Judge owns - the
        // two conditions below are evaluated exactly as before. The only behaviour that changes is
        // the one that used to be a NullPointerException.
        //
        // Deliberately NOT an outer try/catch like getTroopAttack's. That form swallows every NPE
        // in the method, including a genuinely missing terrain entry, and answers 0 - a plausible
        // number that hides the fault. Skipping a bonus a nationless army cannot qualify for is a
        // different thing: it is the correct answer, not a fallback.
        final Nacao nacao = army.getNacao();
        if (nacao != null && nacao.hasHabilidade(";PDB;") && !tpTropa.isBarcos()
                && lf.getDistanciaToCapital(nacao, army.getLocal()) <= nacao.getHabilidadeValor(";PDB;")) {
            vlConstituicao += vlConstituicao * 0.2f;
        }
        if (nacao != null && nacao.hasHabilidade(";PDBN;") && tpTropa.isBarcos()
                && lf.getDistanciaToCapital(nacao, army.getLocal()) <= nacao.getHabilidadeValor(";PDBN;")) {
            vlConstituicao += vlConstituicao * 0.2f;
        }

        float vlArmadura;
        if (army.isGameHasResourceManagement()) {
            vlArmadura = (float) pelotao.getModDefesa(); //GOT
        } else {
            vlArmadura = pelotao.getTreino(); //WDO
        }
        return pelotao.getQtd() * vlConstituicao * (1F + vlArmadura / 100F);
    }

    //City methods start here
    public int getCityFortficationDefense(Cidade city) {
        return bonusFortificacaoCumulativo[city.getFortificacao()];
    }

    /**
     * Determine o valor do Centro Populacional, pelo tamanho, e adicione o
     * restante dos pontos de Fortificação. A Defesa do Centro Populacional é o
     * resultado da soma, modificado pela lealdade.
     */
    public int getCityDefenseBase(Cidade cidade) {
        return getCityDefense(cidade.getTamanho(), cidade.getFortificacao(), cidade.getLealdade());
    }

    public int getCityDefense(int tamanho, int fortificacao, int lealdade) {
        int ret = 0;
        ret += bonusTamanho[tamanho] + bonusFortificacaoCumulativo[fortificacao];
        if (lealdade == 0) {
            ret += ret;
        } else {
            ret += ret * lealdade / 100;
        }
        return ret;
    }

    public int getCityDefenseCombat(Cidade city) {
        int ret = this.getCityDefenseBase(city) + city.getDefenseBonus();
        if (city.getNacao() == null) {
            if (city.getTamanho() > 0) {
                log.error(String.format("City without nation, why? %s  %s", city.getCoordenadas(), city.toString()));
            }
            return ret;
        }
        if (city.getNacao().hasHabilidade(";PFD;") && city.isFortificado()) {
            ret += this.getCityFortficationDefense(city) * city.getNacao().getHabilidadeValor(";PFD;") / 100;
        }
        if (city.getNacao().hasHabilidade(";PCD;") && city.getLocal().getTerreno().isMontanha()) {
            ret += ret * city.getNacao().getHabilidadeValor(";PCD;") / 100;
        }
        if (city.getNacao().hasHabilidade(";NWD;") && city.getLocal().getTerreno().isFloresta()) {
            ret += ret * city.getNacao().getHabilidadeValor(";NWD;") / 100;
        }
        if (city.getNacao().hasHabilidade(";NWS;") && city.getLocal().getTerreno().isPantano()) {
            ret += ret * city.getNacao().getHabilidadeValor(";NWS;") / 100;
        }
        return ret;
    }

    public int getCityAttackCombat(Cidade city) {
        int ret = this.getCityDefenseBase(city);
        if (city.getNacao() == null) {
            return ret;
        }
        if (city.getNacao().hasHabilidade(";NCM;") && city.getLocal().getTerreno().isMontanha()) {
            ret += ret * city.getNacao().getHabilidadeValor(";NCM;") / 100;
        }
        if (city.getNacao().hasHabilidade(";PAW;") && city.getLocal().getTerreno().isFloresta()) {
            ret += ret * city.getNacao().getHabilidadeValor(";PAW;") / 100;
        }
        return ret;
    }

    /**
     * calcula ataque das maquinas de guerra contra a fortificacao da cidade
     */
    public int getCitySiegeCombatFactor(Cidade city, int vlSiege) {
        int fator = 0;
        int forcaAtaque = vlSiege;
        for (int ii = city.getFortificacao(); ii > 0; ii--) {
            if (forcaAtaque >= bonusFortificacao[ii]) {
                fator++;
                forcaAtaque -= bonusFortificacao[ii];
            } else {
                break;
            }
        }
        return fator;
    }
}
