/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import baseLib.SysApoio;
import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import model.Cidade;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Personagem;
import model.Produto;
import model.Raca;
import msgs.BaseMsgs;
import msgs.ColorFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;
import utils.StringRet;

/**
 *
 * @author gurgel
 */
public class CidadeFacade implements Serializable {

    private static final Log log = LogFactory.getLog(CidadeFacade.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final NacaoFacade nacaoFacade = new NacaoFacade();
    private static final LocalFacade localFacade = new LocalFacade();
    private static final BattleSimFacade combatSimFacade = new BattleSimFacade();
    public static final int[] ForneceComida = {0, 100, 200, 1000, 2500, 5000};

    public int getArrecadacaoImpostos(Cidade cidade) {
        return cidade.getArrecadacaoImpostos();
    }

    public String getCoordenadas(Cidade cidade) {
        return localFacade.getCoordenadas(cidade.getLocal());
    }

    public int getDocas(Cidade cidade) {
        return cidade.getDocas();
    }

    public String getDocasNome(Cidade cidade) {
        return BaseMsgs.cidadeDocas[cidade.getDocas()];
    }

    public Jogador getJogador(Cidade cidade) {
        Jogador jogador = null;
        try {
            jogador = cidade.getNacao().getOwner();
        } catch (NullPointerException ex) {
            //faz nada, so retorna null
        }
        return jogador;
    }

    public int getLealdade(Cidade cidade) {
        return cidade.getLealdade();
    }

    public int getLealdadeDelta(Cidade cidade) {
        return (cidade.getLealdade() - cidade.getLealdadeAnterior());
    }

    public Local getLocal(Cidade cidade) {
        return cidade.getLocal();
    }

    public String getNacaoNome(Cidade cidade) {
        return nacaoFacade.getNome(cidade.getNacao());
    }

    public String getNome(Cidade cidade) {
        return cidade.getNome();
    }

    public int getFortificacao(Cidade cidade) {
        return cidade.getFortificacao();
    }

    public String getFortificacaoNome(Cidade cidade) {
        return BaseMsgs.cidadeFortificacao[cidade.getFortificacao()];
    }

    public String getFortificacaoNome(int fortificacao) {
        return BaseMsgs.cidadeFortificacao[fortificacao];
    }

    public int getTamanho(Cidade cidade) {
        return cidade.getTamanho();
    }

    public String getTamanhoNome(Cidade cidade) {
        return BaseMsgs.cidadeTamanho[cidade.getTamanho()];
    }

    public String getTamanhoNome(int tamanho) {
        return BaseMsgs.cidadeTamanho[tamanho];
    }

    public String getTamanhoFortificacao(Cidade cidade) {
        String ret = BaseMsgs.cidadeTamanho[cidade.getTamanho()];
        if (cidade.isFortificado()) {
            ret += "/" + BaseMsgs.cidadeFortificacao[cidade.getFortificacao()];
        }
        return ret;
    }

    public boolean isCapital(Cidade cidade) {
        return cidade.isCapital();
    }

    public boolean isFortificacao(Cidade cidade) {
        return cidade != null && cidade.getFortificacao() > 0;
    }

    public boolean isOculto(Cidade cidade) {
        return cidade.isOculto();
    }

    public boolean isSitiado(Cidade cidade) {
        return cidade.isSitiado();
    }

    public List listaPresencas(Cidade cidade) {
        PersonagemFacade personagemFacade = new PersonagemFacade();
        List listaPresencas = new ArrayList();
        //lista personagems
        try {
            for (Personagem personagem : cidade.getLocal().getPersonagens().values()) {
                if (personagemFacade.isComandaExercito(personagem)) {
                    listaPresencas.add(personagem.getExercito());
                } else {
                    listaPresencas.add(personagem);
                }
            }
        } catch (NullPointerException ex) {
        }
        //FIXME: lista exercitos
        //FIXME: lista artefatos
        return listaPresencas;
    }

    public Color getNacaoColorFill(Cidade cidade) {
        //FIXME: move para nacaoFacade
        try {
            if (cidade.getNacao().getFillColor() == null) {
                return ColorFactory.colorFill[SysApoio.parseInt(cidade.getNacao().getCodigo())];
            } else {
                return cidade.getNacao().getFillColor();
            }
        } catch (Exception ex) {
            return ColorFactory.colorFill[0];
        }
    }

    public Color getNacaoColorBorder(Cidade cidade) {
        //FIXME: move para nacaoFacade
        try {
            if (cidade.getNacao().getBorderColor() == null) {
                return ColorFactory.colorBorder[SysApoio.parseInt(cidade.getNacao().getCodigo())];
            } else {
                return cidade.getNacao().getBorderColor();
            }
        } catch (Exception ex) {
            return ColorFactory.colorBorder[0];
        }
    }

    public String getOculto(Cidade cidade) {
        return SysApoio.iif(cidade.isOculto(), labels.getString("SIM"), labels.getString("NAO"));
    }

    public String getCapital(Cidade cidade) {
        return SysApoio.iif(cidade.isCapital(), labels.getString("SIM"), labels.getString("NAO"));
    }

    public String getSitiado(Cidade cidade) {
        return SysApoio.iif(cidade.isSitiado(), labels.getString("SIM"), labels.getString("NAO"));
    }

    public Nacao getNacao(Cidade cidade) {
        return cidade.getNacao();
    }

    public String getNomeCoordenada(Cidade cidade) {
        return cidade.getComboDisplay();
    }

    public String getRacaNome(Cidade cidade) {
        try {
            return cidade.getRaca().getNome();
        } catch (NullPointerException e) {
            try {
                return cidade.getNacao().getRaca().getNome();
            } catch (NullPointerException ex) {
                return labels.getString("NENHUM");
            }
        }
    }

    public Raca getRaca(Cidade cidade) {
        try {
            return cidade.getRaca();
        } catch (NullPointerException e) {
            try {
                return cidade.getNacao().getRaca();
            } catch (NullPointerException ex) {
                return null;
            }
        }
    }

    public int getProducao(Cidade cidade, Produto produto) {
        final int producao = cidade.getProducao(produto);
        try {
            final int minGold = cidade.getNacao().getHabilidadeNacaoValor("0039");
            if (!produto.isMoney()) {
                return producao;
            } else if (cidade.getNacao().hasHabilidade(";PGH;")
                    && localFacade.isTerrenoMontanhaColina(cidade.getLocal().getTerreno())) {
                //se em montanha/colina e com habilidade, entao garante minimo de 500
                return Math.max(producao, cidade.getNacao().getHabilidadeValor(";PGH;"));
            } else if (cidade.getNacao().hasHabilidade(";PGM;")
                    && cidade.getNacao().getRaca() == cidade.getRaca()) {
                //se mesma cultura e com habilidade, entao garante minimo de 250
                return Math.max(producao, cidade.getNacao().getHabilidadeValor(";PGM;"));
            } else if (producao <= minGold
                    && cidade.getNacao().getHabilidadesNacao().containsKey("0039")
                    && cidade.getNacao().getRaca() == cidade.getRaca()) {
                //se mesma cultura e com habilidade, entao garante minimo de 250
                return minGold;
            } else {
                return producao;
            }
        } catch (NullPointerException ex) {
            return producao;
        }
    }

    public SortedMap<Produto, Integer> getEstoques(Cidade cidade) {
        return cidade.getEstoques();
    }

    public int getEstoque(Cidade cidade, Produto produto) {
        return cidade.getEstoque(produto);
    }

    public int getDefesa(Cidade cidade) {
        return combatSimFacade.getDefesa(cidade);
    }

    public int getDefesa(int tamanho, int fortificacao, int lealdade) {
        return combatSimFacade.getDefesa(tamanho, fortificacao, lealdade);
    }

    public boolean isAtivo(Cidade cidade) {
        return cidade.getTamanho() > 0;
    }

    public Raca getNacaoRaca(Cidade cidade) {
        return getNacao(cidade).getRaca();
    }

    public List<String> getInfo(Cidade cidade, Collection<Nacao> nations) {
        StringRet ret = getInfo(cidade);
        for (Nacao nation : nations) {
            final Nacao targetNation = getNacao(cidade);
            if (targetNation == null || targetNation == nation) {
                continue;
            }
            //print diplomacy
            ret.addTab(String.format("%s: %s", nation.getNome(), nacaoFacade.getRelacionamento(nation, targetNation)));
        }
        return ret.getList();
    }

    public StringRet getInfo(Cidade cidade) {
        StringRet ret = new StringRet();
        if (cidade == null) {
            return ret;
        }
        ret.add(String.format(labels.getString("CIDADE.CAPITAL.DA.NACAO"),
                cidade.getComboDisplay(),
                SysApoio.iif(cidade.isCapital(), labels.getString("(CAPITAL)"), ""),
                nacaoFacade.getNome(cidade.getNacao())));
        ret.addTab(String.format("%s: %s", labels.getString("TAMANHO"), BaseMsgs.cidadeTamanho[cidade.getTamanho()]));
        ret.addTab(String.format("%s: %s", labels.getString("FORTIFICACOES"), BaseMsgs.cidadeFortificacao[cidade.getFortificacao()]));
        if (cidade.getLealdade() > 0) {
            ret.addTab(String.format("%s: %s (%s)",
                    labels.getString("LEALDADE"),
                    cidade.getLealdade(),
                    cidade.getLealdade() - cidade.getLealdadeAnterior()));
        } else {
            ret.addTab(String.format("%s: %s", labels.getString("LEALDADE"), "?"));
        }
        ret.addTab(String.format("%s: %s", labels.getString("CIDADE.DOCAS"), BaseMsgs.cidadeDocas[cidade.getDocas()]));
        ret.addTab(String.format("%s: %s", labels.getString("OCULTO"), getOculto(cidade)));
        ret.addTab(String.format("%s: %s", labels.getString("SITIADO"), getSitiado(cidade)));

        return ret;
    }

    public List<String> getInfoTitle(Cidade cidade) {
        StringRet ret = new StringRet();
        if (cidade == null) {
            return ret.getList();
        }
        ret.add(String.format(labels.getString("CIDADE.CAPITAL.DA.NACAO"),
                cidade.getComboDisplay(),
                SysApoio.iif(cidade.isCapital(), labels.getString("(CAPITAL)"), ""),
                nacaoFacade.getNome(cidade.getNacao())));
        ret.addTab(String.format("%s: %s", labels.getString("TAMANHO"), BaseMsgs.cidadeTamanho[cidade.getTamanho()]));
        return ret.getList();
    }

    public boolean isBigCity(Cidade cidade) {
        return cidade.getTamanho() >= 4;
    }

    public int getUpkeepMoney(Cidade cidade) {
////        if (cidade.getNacao().hasHabilidadeNacaoOld("0042") && cidade.getFortificacao() == 5) {
////            //The Wall: Fortress' upkeep is free
////            return (cidade.getDocas() * 250);
////        } else if (cidade.getNacao().hasHabilidadeNacaoOld("0037")) {
////            //Pays half upkeep cost on fortifications
////            return (cidade.getDocas() * 250 + cidade.getFortificacao() * 500 / 2);
////        } else {
        return (cidade.getDocas() * 250 + cidade.getFortificacao() * 500);
//        }
    }

    public int getFoodGiven(Cidade cidade) {
        return CidadeFacade.ForneceComida[cidade.getTamanho()];
    }
}
