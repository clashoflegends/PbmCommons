/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import business.interfaces.IExercito;
import java.io.Serializable;
import model.Cidade;
import model.Exercito;
import model.ExercitoSim;
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

    public int getDefesa(Cidade cidade) {
        return getDefesa(cidade.getTamanho(), cidade.getFortificacao(), cidade.getLealdade());
    }

    public int getDefesa(int tamanho, int fortificacao, int lealdade) {
        final int[] bonusFortificacaoCumulativo = {0, 2000, 6000, 10000, 16000, 24000};
        final int[] bonusTamanho = {0, 200, 500, 1000, 2500, 5000};
        int ret = 0;
        ret += bonusTamanho[tamanho] + bonusFortificacaoCumulativo[fortificacao];
        if (lealdade == 0) {
            ret += ret;
        } else {
            ret += ret * lealdade / 100;
        }
        return ret;
    }

//    private int getAtaqueExercito(IExercito exercito, int round) {
//        int ret;
//        if (round == 0) {
//            //first strike
//            ret = getAtaqueExercito(exercito, ";TT1;");
//        } else {
//            ret = getAtaqueExercito(exercito);
//        }
//        return ret;
//    }
//
//    private int getAtaqueExercito(IExercito exercito, String habilidade) {
//        int ret = 0;
//        for (Pelotao pelotao : exercito.getPelotoes().values()) {
//            if (pelotao.getTipoTropa().hasHabilidade(habilidade)) {
//                ret += getAtaquePelotao(pelotao, exercito);
//            }
//        }
//        return ret;
//    }
    public float getAtaqueExercito(IExercito exercito, boolean naval) {
        float ret = 0;
        for (Pelotao pelotao : exercito.getPelotoes().values()) {
            if (naval == pelotao.getTipoTropa().isBarcos()) {
                ret += getAtaquePelotao(pelotao, exercito);
            }
        }
        return ret;
    }

    public float getAtaquePelotao(Pelotao pelotao, IExercito exercito) {
        float ret = 0;
        try {
            float forcaTrop = getAtaqueTipoTropa(pelotao.getTipoTropa(), exercito)
                    * (float) pelotao.getQtd()
                    * ((float) pelotao.getTreino() + (float) pelotao.getModAtaque() + 100f)
                    / 300f;
            ret = forcaTrop * getBonusModificador(exercito) / 100f;
        } catch (NullPointerException ex) {
        }
        return ret;
    }

    private float getBonusModificador(IExercito exercito) {
        return ((float) exercito.getPericiaComandante() + (float) exercito.getMoral() + 200f) / 4f;
    }

    private float getAtaqueTipoTropa(TipoTropa tpTropa, IExercito exercito) {
        try {
            float tropasValor = tpTropa.getAtaqueTerreno().get(exercito.getTerreno());
            LocalFacade lf = new LocalFacade();
            if (tpTropa.hasHabilidade(";TTD;") && lf.isCidade(exercito.getLocal())) {
                //D = ataque dobrado se defendendo cidade aliada
                try {
                    Nacao nacaoCidade = exercito.getLocal().getCidade().getNacao();
                    NacaoFacade nf = new NacaoFacade();
                    if (exercito.getNacao().equals(nacaoCidade) || nf.isAliado(exercito.getNacao(), nacaoCidade)) {
                        tropasValor = tropasValor * 2f;
                    }
                } catch (NullPointerException ex) {
                }
            }
            return (tropasValor);
        } catch (NullPointerException ex) {
            //nao tem a tropa, retorna forca 0
            return 0f;
        }
    }

    private float getDefesa(TipoTropa tpTropa, Terreno terreno) {
        try {
            float defesa = tpTropa.getDefesaTerreno().get(terreno);
            if (tpTropa.hasHabilidade(";TTD;")) {
                //D = defesa eh metade do ataque
                defesa = defesa / 2f;
            }
            return defesa;
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    public float getDefesaPelotao(Pelotao pelotao, Terreno terreno) {
        float vlConstituicao = getDefesa(pelotao.getTipoTropa(), terreno);
        float vlArmadura = (float) pelotao.getModDefesa();
        return pelotao.getQtd() * vlConstituicao * (1F + vlArmadura / 100F);
    }

    public int getDefesaExercito(IExercito exercito, boolean naval) {
        //TODO: combate em terra vs. naval
        int ret = 0;
        for (Pelotao pelotao : exercito.getPelotoes().values()) {
            if (naval == pelotao.getTipoTropa().isBarcos()) {
                ret += getDefesaPelotao(pelotao, exercito.getTerreno());
            }
        }
        return ret;
    }

    public ExercitoSim clone(Exercito exercito) {
        return new ExercitoSim(exercito);
    }
}
