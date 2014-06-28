/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import baseLib.SysApoio;
import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import model.*;
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

    public List<String> getInfo(Cidade cidade) {
        StringRet ret = new StringRet();
        if (cidade == null) {
            return ret.getList();
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

        return ret.getList();
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

    boolean isBigCity(Cidade cidade) {
        return cidade.getTamanho() >= 4;
    }
}
