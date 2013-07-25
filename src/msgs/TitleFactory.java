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
import model.Habilidade;
import model.Ordem;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author jmoura
 */
public class TitleFactory implements Serializable {

    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final PersonagemFacade personagemFacade = new PersonagemFacade();
    private static final ExercitoFacade exercitoFacade = new ExercitoFacade();
    private static final AcaoFacade acaoFacade = new AcaoFacade();
    private static final String[] tipoPersonagem = {labels.getString("QUALQUER"), labels.getString("COMANDANTE"),
        labels.getString("AGENTE"), labels.getString("EMISSARIO"), labels.getString("MAGO"),
        labels.getString("MILESTONE"), labels.getString("CIDADE")};

    public static String[] getTipoPersonagem() {
        return tipoPersonagem;
    }

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
        ret += String.format("%s: %s\n", labels.getString("WHO.CAN"), getTipoPersonagem(ordem));
        ret += String.format("%s: %s%s\n", labels.getString("TIPO"), getTipoOrdem(ordem), getAjudaTipoOrdem(ordem));
        ret += String.format("%s: %s\n", labels.getString("DIFICULDADE"), getDificuldade(ordem));
        ret += String.format("%s: %s\n", labels.getString("IMPROVE"), acaoFacade.getImproveRank(ordem));
        if (acaoFacade.getCusto(ordem) > 0) {
            ret += String.format("%s: %s\n", labels.getString("CUSTO"), acaoFacade.getCusto(ordem));
        }
        if (!acaoFacade.getHabilidades(ordem).isEmpty()) {
            for (Habilidade habilidade : acaoFacade.getHabilidades(ordem)) {
                ret += String.format("%s: %s\n", labels.getString("ORDEM.FEATURE"), habilidade.getNome());
            }
        }
        ret += String.format("%s: %s\n", labels.getString("REQUISITO"), getAjudaRequisito(ordem));
        ret += String.format("%s:\n%s\n", labels.getString("AJUDA"), ordem.getAjuda());
        return ret;
    }

    public static String getTipoOrdem(Ordem ordem) {
        if (ordem.getTipo().equals("Misc")) {
            return labels.getString("LIVRE");
        } else if (ordem.getTipo().equals("Per")) {
            return labels.getString("PRINCIPAL");
        } else if (ordem.getTipo().equals("Mov")) {
            return labels.getString("MOVIMENTACAO");
        } else if (ordem.getTipo().equals("Cid")) {
            return labels.getString("CIDADE");
        } else if (ordem.getTipo().equals("Milestone")) {
            return labels.getString("MILESTONE");
        } else {
            return "-";
        }
    }

    public static String getTipoPersonagem(Ordem ordem) {
        if (ordem.getTipoPersonagem().equals("X")) {
            return tipoPersonagem[0];
        } else if (ordem.getTipoPersonagem().equals("C")) {
            return tipoPersonagem[1];
        } else if (ordem.getTipoPersonagem().equals("A")) {
            return tipoPersonagem[2];
        } else if (ordem.getTipoPersonagem().equals("E")) {
            return tipoPersonagem[3];
        } else if (ordem.getTipoPersonagem().equals("M")) {
            return tipoPersonagem[4];
        } else if (ordem.getTipoPersonagem().equals("Z")) {
            return tipoPersonagem[5];
        } else if (ordem.getTipoPersonagem().equals("F")) {
            return tipoPersonagem[6];
        } else {
            return "-";
        }
    }

    public static String[][] listTipoPersonagem() {
        String[][] ret = new String[tipoPersonagem.length + 1][2];
        int ii = 0;
        ret[ii][0] = labels.getString("TODOS"); //Display
        ret[ii++][1] = "Todos"; //Id
        for (String elem : tipoPersonagem) {
            ret[ii][0] = elem;
            ret[ii++][1] = elem;
        }
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

    public static String getDificuldade(Ordem ordem) {
        if (ordem.getDificuldade().equals("Aut")) {
            return labels.getString("AUTOMATICA");
        } else if (ordem.getDificuldade().equals("Dif")) {
            return labels.getString("DIFICIL");
        } else if (ordem.getDificuldade().equals("Fac")) {
            return labels.getString("FACIL");
        } else if (ordem.getDificuldade().equals("Med")) {
            return labels.getString("MEDIA");
        } else if (ordem.getDificuldade().equals("Var")) {
            return labels.getString("VARIADA");
        } else {
            return "-";
        }
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
            ret += separator + labels.getString("REQUISITO.AJUDA.CPV");
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
