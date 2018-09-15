/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package msgs;

import business.facade.AcaoFacade;
import business.facade.CenarioFacade;
import business.facade.ExercitoFacade;
import business.facade.FeiticoFacade;
import business.facade.PersonagemFacade;
import business.interfaces.IExercito;
import java.io.Serializable;
import model.Exercito;
import model.Feitico;
import model.Habilidade;
import model.Ordem;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

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
        labels.getString("MILESTONE"), labels.getString("CIDADE"),
        labels.getString("STARTUP"), labels.getString("PERSONAGEM.NPC")};
    private static final String[] tipoSkill = {"COMANDANTE",
        "AGENTE", "EMISSARIO", "MAGO"
    };
    private static final String[] combateTaticaGrito = {
        "@COMBATE.TATICA.GRITO.CARGA#",
        "@COMBATE.TATICA.GRITO.FLANCO#",
        "@COMBATE.TATICA.GRITO.PADRAO#",
        "@COMBATE.TATICA.GRITO.CERCO#",
        "@COMBATE.TATICA.GRITO.GUERRILHA#",
        "@COMBATE.TATICA.GRITO.EMBOSCADA#",
        "@COMBATE.TATICA.GRITO.BARRAGEM#",
        "@COMBATE.TATICA.GRITO.SHIELDWALL#",
        "@COMBATE.TATICA.GRITO.STANDFIRM#",
        "@COMBATE.TATICA.GRITO.SWARM#"};

    public static String[] getTipoPersonagem() {
        return tipoPersonagem;
    }

    public static String[] getTipoSkill() {
        return tipoSkill;
    }

    public static String displayExercitoTitle(Exercito exercito) {
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
                        exercitoFacade.getTituloComandante(exercito));
            } else {
                ret = String.format(labels.getString("EXERCITO.AVISTADO"),
                        exercitoFacade.getDescricaoTamanho(exercito),
                        exercitoFacade.getNacaoNome(exercito),
                        exercitoFacade.getTituloComandante(exercito));
            }
        } catch (NullPointerException e) {
            ret = labels.getString("EXERCITO.DESCONHECIDO");
        }
        return ret;
    }

    public String displayComanderTitle(IExercito exercito) {
        String ret;
        try {
            if (exercitoFacade.isGuarnicao(exercito)) {
                ret = String.format(labels.getString("GUARNICAO.TITLE"),
                        exercitoFacade.getNacaoNome(exercito));
            } else {
                ret = String.format(labels.getString("EXERCITO.COMMANDER"),
                        exercitoFacade.getTituloComandante(exercito),
                        exercitoFacade.getNacaoNome(exercito));
            }
        } catch (NullPointerException e) {
            ret = labels.getString("EXERCITO.INESTIMADO");
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
        if (acaoFacade.isSetup(ordem)) {
            ret += String.format("%s: %s\n", labels.getString("TIPO"), labels.getString("STARTUP"));
        } else {
            ret += String.format("%s: %s%s\n", labels.getString("TIPO"), getTipoOrdem(ordem), getAjudaTipoOrdem(ordem));
        }
        ret += String.format("%s: %s\n", labels.getString("DIFICULDADE"), getDificuldade(ordem));
        ret += String.format("%s: %s\n", labels.getString("IMPROVE"), acaoFacade.getImproveRank(ordem));
        if (acaoFacade.getCusto(ordem) > 0) {
            ret += String.format("%s: %s\n", labels.getString("CUSTO"), acaoFacade.getCusto(ordem));
        }
        if (acaoFacade.getPointsSetup(ordem) > 0) {
            ret += String.format("%s: %s\n", labels.getString("STARTUP.POINTS"), acaoFacade.getPointsSetup(ordem));
        }
        if (!acaoFacade.getHabilidades(ordem).isEmpty()) {
            for (Habilidade habilidade : acaoFacade.getHabilidades(ordem)) {
                if (habilidade.isPackage()) {
                    for (Habilidade power : habilidade.getHabilidades().values()) {
                        ret += String.format("%s: %s\n", labels.getString("ORDEM.FEATURE"), power.getNome());
                    }
                } else {
                    ret += String.format("%s: %s\n", labels.getString("ORDEM.FEATURE"), habilidade.getNome());
                }
            }
        }
        ret += String.format("%s: %s\n", labels.getString("REQUISITO"), getAjudaRequisito(ordem));
        ret += String.format("%s:\n%s\n", labels.getString("AJUDA"), ordem.getAjuda());
//        if (acaoFacade.isSetup(ordem)) {
//            ret += String.format("%s:\n%s\n", labels.getString("AJUDA"), acaoFacade.getSetupDescription(ordem));
//        } else {
//            ret += String.format("%s:\n%s\n", labels.getString("AJUDA"), ordem.getAjuda());
//        }
        return ret;
    }

    public static String getTipoOrdem(Ordem ordem) {
        if (ordem == null) {
            return "-";
        } else if (ordem.getTipo().equals("Misc")) {
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
        } else if (ordem.getTipoPersonagem().equals("N")) {
            return tipoPersonagem[7];
        } else if (ordem.getTipoPersonagem().equals("G")) {
            return tipoPersonagem[8];
        } else {
            return "-";
        }
    }

    public static String getTipoPersonagem(Integer skillClass) {
        switch (skillClass) {
            case CenarioFacade.COMANDANTE:
                return tipoPersonagem[1];
            case CenarioFacade.ROGUE:
                return tipoPersonagem[2];
            case CenarioFacade.DIPLOMAT:
                return tipoPersonagem[3];
            case CenarioFacade.WIZARD:
                return tipoPersonagem[4];
            default:
                return labels.getString("DESCONHECIDA");
        }
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
        final String requisitos = ordem.getRequisito().toLowerCase();
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
        if (requisitos.contains("nogrp")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.NOGRP");
        }
        if (requisitos.contains("noex")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.NOEX");
        }
        if (requisitos.contains("ancoravel")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.ANCORAVEL");
        }
        if (requisitos.contains("cpp")) {
            //nao pode estar sitiado
            ret += separator + labels.getString("REQUISITO.AJUDA.CPP");
        }
        if (requisitos.contains("ccn")) {
            //city has same culture as nation
            ret += separator + labels.getString("REQUISITO.AJUDA.CCN");
        }
        if (requisitos.contains("clm")) {
            //city has loyalty above minimum
            ret += separator + labels.getString("REQUISITO.AJUDA.CLM");
        }
        if (requisitos.contains("cpv")) {
            //nao pode estar sitiado
            ret += separator + labels.getString("REQUISITO.AJUDA.CPV");
        }
        if (requisitos.contains("multi2")) {
            //requires multiple
            ret += separator + labels.getString("REQUISITO.AJUDA.MULTI2");
        }
        if (requisitos.contains("multi3")) {
            //requires multiple
            ret += separator + labels.getString("REQUISITO.AJUDA.MULTI3");
        }
        if (requisitos.contains("multi4")) {
            //requires multiple
            ret += separator + labels.getString("REQUISITO.AJUDA.MULTI4");
        }
        if (requisitos.contains("multi5")) {
            //requires multiple
            ret += separator + labels.getString("REQUISITO.AJUDA.MULTI5");
        }
        if (requisitos.contains("bigcity")) {
            //nao pode estar sitiado
            ret += separator + labels.getString("REQUISITO.AJUDA.BIGCITY");
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
        if (requisitos.contains("cs1")) {
            //Size (tamanho = 1)
            ret += separator + labels.getString("REQUISITO.AJUDA.CS1");
        }
        if (requisitos.contains("cs2")) {
            //Size (tamanho = 2)
            ret += separator + labels.getString("REQUISITO.AJUDA.CS2");
        }
        if (requisitos.contains("cs3")) {
            //Size (tamanho = 3)
            ret += separator + labels.getString("REQUISITO.AJUDA.CS3");
        }
        if (requisitos.contains("cs4")) {
            //Size (tamanho = 4)
            ret += separator + labels.getString("REQUISITO.AJUDA.CS4");
        }
        if (requisitos.contains("cs5")) {
            //Size (tamanho = 5)
            ret += separator + labels.getString("REQUISITO.AJUDA.CS5");
        }
        if (requisitos.contains("csn5")) {
            //Size (tamanho = 5)
            ret += separator + labels.getString("REQUISITO.AJUDA.CSN5");
        }
        if (requisitos.contains("cf0")) {
            //fortifications (fortificacao == 1)
            ret += separator + labels.getString("REQUISITO.AJUDA.CF0");
        }
        if (requisitos.contains("cf1")) {
            //fortifications (fortificacao == 1)
            ret += separator + labels.getString("REQUISITO.AJUDA.CF1");
        }
        if (requisitos.contains("cf2")) {
            //fortifications (fortificacao == 2)
            ret += separator + labels.getString("REQUISITO.AJUDA.CF2");
        }
        if (requisitos.contains("cf3")) {
            //fortifications (fortificacao == 3)
            ret += separator + labels.getString("REQUISITO.AJUDA.CF3");
        }
        if (requisitos.contains("cf4")) {
            //fortifications (fortificacao == 4)
            ret += separator + labels.getString("REQUISITO.AJUDA.CF4");
        }
        if (requisitos.contains("cf5")) {
            //fortifications (fortificacao == 5)
            ret += separator + labels.getString("REQUISITO.AJUDA.CF5");
        }
        if (requisitos.contains("cfn5")) {
            //fortifications (fortificacao == 5)
            ret += separator + labels.getString("REQUISITO.AJUDA.CFN5");
        }
        if (requisitos.contains("artefato")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.ARTEFATO");
        }
        if (requisitos.contains("artscry")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.ARTSCRY");
        }
        if (requisitos.contains("artsumm")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.ARTSUMM");
        }
        if (requisitos.contains("artdragonegg")) {
            ret += separator + labels.getString("REQUISITO.AJUDA.ARTDRAGONEGG");
        }
        if (requisitos.contains("lhf")) {
            //terrain landmark
            ret += separator + labels.getString("REQUISITO.AJUDA.LHF");
        }
        if (requisitos.contains("lhn")) {
            //terrain landmark
            ret += separator + labels.getString("REQUISITO.AJUDA.LHN");
        }
        if (requisitos.contains("lhs")) {
            //terrain landmark spire
            ret += separator + labels.getString("REQUISITO.AJUDA.LHS");
        }
        return ret;
    }

    public static String getTaticasGrito(int tatica) {
        try {
            return combateTaticaGrito[tatica];
        } catch (IndexOutOfBoundsException e) {
            return combateTaticaGrito[0];
        }
    }

    public static String getTaticaNome(int tactic) {
        try {
            return BaseMsgs.taticasLabel[tactic];
        } catch (ArrayIndexOutOfBoundsException ex) {
            return BaseMsgs.taticasLabel[2];
        }
    }
}
