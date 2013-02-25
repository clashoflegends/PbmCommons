/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package msgs;

import business.facade.AcaoFacade;
import business.facade.ExercitoFacade;
import business.facade.FeiticoFacade;
import business.facade.PersonagemFacade;
import java.io.Serializable;
import model.Exercito;
import model.Feitico;
import model.Ordem;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author jmoura
 */
public class TitleFactory implements Serializable {

    private static BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final PersonagemFacade personagemFacade = new PersonagemFacade();
    private static final ExercitoFacade exercitoFacade = new ExercitoFacade();
    private static final AcaoFacade acaoFacade = new AcaoFacade();

    public static String displayExercitotitulo(Exercito exercito) {
        String ret;
        try {
            if (exercitoFacade.isGuarnicao(exercito)) {
                ret = String.format(labels.getString("GUARNICAO.AVISTADO"),
                        exercitoFacade.getDescricaoTamanho(exercito),
                        exercitoFacade.getNacaoNome(exercito));
            } else if (exercitoFacade.isEsquadra(exercito) || exercitoFacade.getTamanhoEsquadra(exercito) > 0) {
                ret = String.format(labels.getString("ESQUADRA.AVISTADO"),
                        exercitoFacade.getDescricaoTamanho(exercito),
                        exercitoFacade.getNacaoNome(exercito),
                        personagemFacade.getTituloComandante(exercito));
            } else {
                ret = String.format(labels.getString("EXERCITO.AVISTADO"),
                        exercitoFacade.getDescricaoTamanho(exercito),
                        exercitoFacade.getNacaoNome(exercito),
                        personagemFacade.getTituloComandante(exercito));
            }
        } catch (NullPointerException e) {
            ret = labels.getString("EXERCITO.DESCONHECIDO");
        }
        return ret;
    }

    public static String getFeiticoDisplay(Feitico feitico) {
        FeiticoFacade feiticoFacade = new FeiticoFacade();
        String ret = "";
        ret += String.format("%s: %s\n", labels.getString("NOME"), feiticoFacade.getNome(feitico));
        ret += String.format("%s: %s\n", labels.getString("TOMO"), feiticoFacade.getLivroFeitico(feitico));
        ret += String.format("%s: %s\n", labels.getString("DIFICULDADE"), feiticoFacade.getDificuldadeDisplay(feitico));
        ret += String.format("%s: %s\n", labels.getString("PROIBIDO"), feiticoFacade.getProibidoDisplay(feitico));
        ret += String.format("%s: %s\n", labels.getString("ACAO"), feiticoFacade.getOrdemNome(feitico));
        ret += String.format("%s:\n%s\n", labels.getString("AJUDA"), feitico.getAjuda());
        return ret;
    }

    public static String getOrdemDisplay(Ordem ordem) {
        String ret = ordem.getDescricao() + "\n";
        ret += String.format("%s: %s\n", labels.getString("PARAMETROS"), ordem.getParametros());
        ret += String.format("%s: %s\n", labels.getString("WHO.CAN"), acaoFacade.getTipoPersonagem(ordem));
        ret += String.format("%s: %s%s\n", labels.getString("TIPO"), acaoFacade.getTipoOrdem(ordem), getAjudaTipoOrdem(ordem));
        ret += String.format("%s: %s\n", labels.getString("DIFICULDADE"), acaoFacade.getDificuldade(ordem));
        ret += String.format("%s: %s\n", labels.getString("IMPROVE"), acaoFacade.getImproveRank(ordem));
        if (acaoFacade.getCusto(ordem) > 0) {
            ret += String.format("%s: %s\n", labels.getString("CUSTO"), acaoFacade.getCusto(ordem));
        }
        ret += String.format("%s: %s\n", labels.getString("REQUISITO"), getAjudaRequisito(ordem));
        ret += String.format("%s:\n%s\n", labels.getString("AJUDA"), ordem.getAjuda());
        return ret;
    }

    private static String getAjudaTipoOrdem(Ordem ordem) {
        String ret = " - ";
        if (ordem.getTipo().equals("Misc")) {
            ret += labels.getString("LIVRE.AJUDA");
        } else if (ordem.getTipo().equals("Per")) {
            ret += labels.getString("PRINCIPAL.AJUDA");
        } else if (ordem.getTipo().equals("Mov")) {
            ret += labels.getString("MOVIMENTACAO.AJUDA");
        } else if (ordem.getTipo().equals("Cid")) {
            ret += labels.getString("CIDADE.AJUDA");
        } else if (ordem.getTipo().equals("Milestone")) {
            ret += labels.getString("MILESTONE.AJUDA");
        } else {
            ret = "";
        }
        return ret;
    }

    private static String getAjudaRequisito(Ordem ordem) {
        final String separator = "\n\t-";
        String ret = "";
        String requisitos = ordem.getRequisito().toLowerCase();
        //ajuda por tipo de chave
        if (requisitos.contains("none")) {
            //none nao devia estar com mais nenhum parametro.
            return labels.getString("NENHUM");
        }
        if (requisitos.contains("capital")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.CAPITAL");
        }
        if (requisitos.contains("terra")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.TERRA");
        }
        if (requisitos.contains("2ex")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.2EX");
        }
        if (requisitos.contains("comex")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.COMEX");
        }
        if (requisitos.contains("comesq")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.COMESQ");
        }
        if (requisitos.contains("comgrp")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.COMGRP");
        }
        if (requisitos.contains("ancoravel")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.ANCORAVEL");
        }
        if (requisitos.contains("cpp")) {
            //nao pode estar sitiado
            ret += separator + labels.getString("REQUISITO.AJUDA.CPP");
        }
        if (requisitos.contains("cpv")) {
            //nao pode estar sitiado
            ret += separator + labels.getString("REQUISITO.AJUDA.CPP");
        }
        if (requisitos.contains("cppr")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.CPPR");
        }
        if (requisitos.contains("cps")) {
            //pode estar sitiado
            ret += separator + labels.getString("REQUISITO.AJUDA.CPS");
        }
        if (requisitos.contains("cpx")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.CPX");
        }
        if (requisitos.contains("cpany")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.CPANY");
        }
        if (requisitos.contains("cpn")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.CPN");
        }
        if (requisitos.contains("cpally")) {
            //CIDADE considera PERSONAGEM amigo !this.criticaCpAmigavel(true)
            ret += separator + labels.getString("REQUISITO.AJUDA.CPALLY");
        }
        if (requisitos.contains("cpb")) {
            //PERSONAGEM considera CIDADE amigo !this.criticaCpInimiga(false)
            ret += separator + labels.getString("REQUISITO.AJUDA.CPB");
        }
        if (requisitos.contains("cpi")) {
            //ALVO considera PERSONAGEM amigo !this.criticaCpInimiga(false)
            ret += separator + labels.getString("REQUISITO.AJUDA.CPI");
        }
        if (requisitos.contains("Artefato")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.ARTEFATO");
        }
        if (requisitos.contains("ArtScry")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.ARTSCRY");
        }
        if (requisitos.contains("ArtSumm")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.ARTSUMM");
        }
        if (requisitos.contains("ArtDragonEgg")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.ARTDRAGONEGG");
        }
        return ret;
    }
}
