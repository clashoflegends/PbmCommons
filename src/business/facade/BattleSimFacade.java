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
            if (exercito.getNacao().hasHabilidade(";PAB;") && !tpTropa.isBarcos()
                    && lf.getDistanciaToCapital(exercito.getNacao(), local)
                    <= exercito.getNacao().getHabilidadeValor(";PAB;")) {
                tropasValor += tropasValor * 0.15f;
            }
            if (exercito.getNacao().hasHabilidade(";PABN;") && tpTropa.isBarcos()
                    && lf.getDistanciaToCapital(exercito.getNacao(), local) <= exercito.getNacao().getHabilidadeValor(";PABN;")) {
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
        if (army.getNacao().hasHabilidade(";PDB;") && !tpTropa.isBarcos()
                && lf.getDistanciaToCapital(army.getNacao(), army.getLocal()) <= army.getNacao().getHabilidadeValor(";PDB;")) {
            vlConstituicao += vlConstituicao * 0.2f;
        }
        if (army.getNacao().hasHabilidade(";PDBN;") && tpTropa.isBarcos()
                && lf.getDistanciaToCapital(army.getNacao(), army.getLocal()) <= army.getNacao().getHabilidadeValor(";PDBN;")) {
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
     * Determine o valor do Centro Populacional, pelo tamanho, e adicione o restante dos pontos de Fortificação. A Defesa do Centro Populacional é o
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
