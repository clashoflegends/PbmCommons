/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package msgs;

import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author jmoura
 */
public final class BaseMsgs {

    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    //utilizada em Feitico
    public static final String[] dificuldade = {labels.getString("AUTOMATICA"), labels.getString("FACIL"), labels.getString("MEDIA"), labels.getString("DIFICIL")};
    //utilizada em Cenario
    public static final String[][] taticasGb = {{labels.getString("CARGA"), "ca"}, {labels.getString("FLANCO"), "fl"}, {labels.getString("PADRAO"), "pa"}, {labels.getString("CERCO"), "ce"}, {labels.getString("GUERRILHA"), "gu"}, {labels.getString("EMBOSCADA"), "em"}};
    public static final String[][] taticasTk = {{labels.getString("TACTIC.BARRAGEM"), "ba"}, {labels.getString("TACTIC.CARGA"), "ca"}, {labels.getString("TACTIC.FLANCO"), "fl"}, {labels.getString("TACTIC.SHIELDWALL"), "sh"}, {labels.getString("TACTIC.STANDFIRM"), "sf"}, {labels.getString("TACTIC.CERCO"), "ce"}, {labels.getString("TACTIC.SWARM"), "sm"}};
    public static final String[] tituloPericiaComandante = {labels.getString("PERICIA.NIVELNONE"), labels.getString("PERICIA.NIVEL00"), labels.getString("PERICIA.COMANDANTE.NIVEL01"), labels.getString("PERICIA.COMANDANTE.NIVEL02"), labels.getString("PERICIA.COMANDANTE.NIVEL03"), labels.getString("PERICIA.COMANDANTE.NIVEL04"), labels.getString("PERICIA.COMANDANTE.NIVEL05"), labels.getString("PERICIA.COMANDANTE.NIVEL06"), labels.getString("PERICIA.COMANDANTE.NIVEL07"), labels.getString("PERICIA.COMANDANTE.NIVEL08"), labels.getString("PERICIA.COMANDANTE.NIVEL09"), labels.getString("PERICIA.COMANDANTE.NIVEL10")};
    public static final String[] tituloPericiaAgente = {labels.getString("PERICIA.NIVELNONE"), labels.getString("PERICIA.NIVEL00"), labels.getString("PERICIA.AGENTE.NIVEL01"), labels.getString("PERICIA.AGENTE.NIVEL02"), labels.getString("PERICIA.AGENTE.NIVEL03"), labels.getString("PERICIA.AGENTE.NIVEL04"), labels.getString("PERICIA.AGENTE.NIVEL05"), labels.getString("PERICIA.AGENTE.NIVEL06"), labels.getString("PERICIA.AGENTE.NIVEL07"), labels.getString("PERICIA.AGENTE.NIVEL08"), labels.getString("PERICIA.AGENTE.NIVEL09"), labels.getString("PERICIA.AGENTE.NIVEL10")};
    public static final String[] tituloPericiaEmissario = {labels.getString("PERICIA.NIVELNONE"), labels.getString("PERICIA.NIVEL00"), labels.getString("PERICIA.EMISSARIO.NIVEL01"), labels.getString("PERICIA.EMISSARIO.NIVEL02"), labels.getString("PERICIA.EMISSARIO.NIVEL03"), labels.getString("PERICIA.EMISSARIO.NIVEL04"), labels.getString("PERICIA.EMISSARIO.NIVEL05"), labels.getString("PERICIA.EMISSARIO.NIVEL06"), labels.getString("PERICIA.EMISSARIO.NIVEL07"), labels.getString("PERICIA.EMISSARIO.NIVEL08"), labels.getString("PERICIA.EMISSARIO.NIVEL09"), labels.getString("PERICIA.EMISSARIO.NIVEL10")};
    public static final String[] tituloPericiaMago = {labels.getString("PERICIA.NIVELNONE"), labels.getString("PERICIA.NIVEL00"), labels.getString("PERICIA.MAGO.NIVEL01"), labels.getString("PERICIA.MAGO.NIVEL02"), labels.getString("PERICIA.MAGO.NIVEL03"), labels.getString("PERICIA.MAGO.NIVEL04"), labels.getString("PERICIA.MAGO.NIVEL05"), labels.getString("PERICIA.MAGO.NIVEL06"), labels.getString("PERICIA.MAGO.NIVEL07"), labels.getString("PERICIA.MAGO.NIVEL08"), labels.getString("PERICIA.MAGO.NIVEL09"), labels.getString("PERICIA.MAGO.NIVEL10")};
    //utilizada em ExercitoConverter
    public static final String[] navioDescricao = {labels.getString("BARCO.GUERRA"), labels.getString("TRANSPORTES")};
    public static final String[] tipoMovimentacao = {labels.getString("NORMAL"), labels.getString("EVASIVO")};
    //utilizados em Facades e server
    public static final String[] cidadeDocas = {labels.getString("NAO"), labels.getString("CIDADE.DOCAS"), labels.getString("CIDADE.PORTO")};
    public static final String[] cidadeFortificacao = {labels.getString("CIDADE.FORTIFICACAO0"), labels.getString("CIDADE.FORTIFICACAO1"), labels.getString("CIDADE.FORTIFICACAO2"), labels.getString("CIDADE.FORTIFICACAO3"), labels.getString("CIDADE.FORTIFICACAO4"), labels.getString("CIDADE.FORTIFICACAO5")};
    public static final String[] cidadeTamanho = {labels.getString("CIDADE.TAMANHO0"), labels.getString("CIDADE.TAMANHO1"), labels.getString("CIDADE.TAMANHO2"), labels.getString("CIDADE.TAMANHO3"), labels.getString("CIDADE.TAMANHO4"), labels.getString("CIDADE.TAMANHO5")};
    public static final String[] guarnicaoTamanho = {labels.getString("GUARNICAO.INESTIMADA"), labels.getString("PEQUENA.GUARNICAO"), labels.getString("MEDIA.GUARNICAO"), labels.getString("GRANDE.GUARNICAO"), labels.getString("VASTA.GUARNICAO"), labels.getString("IMENSA.GUARNICAO")};
    public static final String[] esquadraTamanho = {labels.getString("ESQUADRA.INESTIMADA"), labels.getString("PEQUENA.ESQUADRA"), labels.getString("MEDIA.ESQUADRA"), labels.getString("GRANDE.ESQUADRA"), labels.getString("VASTA.ESQUADRA"), labels.getString("IMENSA.ESQUADRA")};
    public static final String[] exercitoTamanho = {labels.getString("EXERCITO.INESTIMADO"), labels.getString("PEQUENO.EXERCITO"), labels.getString("MEDIO.EXERCITO"), labels.getString("GRANDE.EXERCITO"), labels.getString("VASTO.EXERCITO"), labels.getString("IMENSO.EXERCITO")};
    public static final String[] localClima = {"-", labels.getString("POLAR"), labels.getString("SEVERO"), labels.getString("FRIO"), labels.getString("TEMPERADO"), labels.getString("SUBTROPICAL"), labels.getString("TROPICAL"), labels.getString("EQUATORIAL")};
    public static final String[] nacaoRelacionamento = {labels.getString("RELACIONAMENTO.ODIADO"), labels.getString("RELACIONAMENTO.ANTIPATIZANTE"), labels.getString("RELACIONAMENTO.NEUTRO"), labels.getString("RELACIONAMENTO.TOLERANTE"), labels.getString("RELACIONAMENTO.AMIGAVEL"), labels.getString("RELACIONAMENTO.VASALO"), labels.getString("RELACIONAMENTO.LORDE")};
    public static final String[] tituloAtributoVida = {labels.getString("FERIMENTOS.DESCONHECIDO"), labels.getString("FERIMENTOS.MORTAIS"), labels.getString("FERIMENTOS.GRAVES"), labels.getString("FERIMENTOS.CRITICOS"), labels.getString("FERIMENTOS.SERIOS"), labels.getString("FERIMENTOS.SERIOS"), labels.getString("MUITOS.FERIMENTOS"), labels.getString("FERIMENTOS.LEVES"), labels.getString("FERIMENTOS.LEVES"), labels.getString("ARRANHOES"), labels.getString("SEM.FERIMENTOS")};
    public static final String[][] direcoes = {
        {labels.getString("NORTE-OESTE"), "no"},
        {labels.getString("NORTE-ESTE"), "ne"},
        {labels.getString("LESTE"), "l"},
        {labels.getString("SUL-ESTE"), "se"},
        {labels.getString("SUL-OESTE"), "so"},
        {labels.getString("OESTE"), "o"}
    };
}
