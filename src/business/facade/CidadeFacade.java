/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import baseLib.SysApoio;
import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import model.*;
import msgs.Msgs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

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
        return Msgs.cidadeDocas[cidade.getDocas()];
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
        return Msgs.cidadeFortificacao[cidade.getFortificacao()];
    }

    public String getFortificacaoNome(int fortificacao) {
        return Msgs.cidadeFortificacao[fortificacao];
    }

    public int getTamanho(Cidade cidade) {
        return cidade.getTamanho();
    }

    public String getTamanhoNome(Cidade cidade) {
        return Msgs.cidadeTamanho[cidade.getTamanho()];
    }

    public String getTamanhoNome(int tamanho) {
        return Msgs.cidadeTamanho[tamanho];
    }

    public String getTamanhoFortificacao(Cidade cidade) {
        String ret = Msgs.cidadeTamanho[cidade.getTamanho()];
        if (cidade.isFortificado()) {
            ret += "/" + Msgs.cidadeFortificacao[cidade.getFortificacao()];
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
            Iterator lista = cidade.getLocal().getPersonagens().values().iterator();
            while (lista.hasNext()) {
                Personagem personagem = (Personagem) lista.next();
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

    public Color getNacaoColor(Cidade cidade) {
        int[][] cor = {
            {180, 180, 180},
            {255, 0, 0}, //red A
            {0, 0, 255}, //azul B
            {255, 255, 0}, //amarelo A
            {0, 128, 0}, //verde B
            {0, 255, 255}, //azul claro B
            {255, 110, 110}, //pink A
            {255, 128, 0}, //laranja
            {128, 255, 0}, //verde amarelado
            {185, 185, 0}, //verde/amarelo A
            {0, 255, 128}, //verde claro B
            {255, 255, 255}, //branco
            {128, 0, 255}, //roxo
            {0, 128, 255}, //azul medio
            {0, 0, 0} //preto
        };
        final Color[] color = {
            new Color(180, 180, 180), //0
            new Color(255, 0, 0), //1    red A
            new Color(0, 0, 255), //2    azul B
            new Color(255, 255, 0), //3  amarelo A
            new Color(0, 128, 0), //4    verde B
            new Color(0, 255, 255), //5  azul claro B
            new Color(255, 110, 110), //6    pink A
            new Color(255, 128, 0), //7  laranja
            new Color(128, 255, 0), //8  verde amarelado
            new Color(128, 128, 0), //9  olive
            new Color(0, 255, 128), //10 verde claro B
            new Color(255, 255, 255), //11   branco
            new Color(128, 0, 255), //12 roxo
            new Color(0, 128, 255), //13 azul medio
            new Color(0, 0, 0), //14 preto
            new Color(25, 25, 25), //15  dark gray
            new Color(255, 0, 255), //16 Fuchsia
            new Color(0, 128, 128), //17 teal
            new Color(255, 215, 32), //18 Gold
            new Color(218, 165, 32), //19 
            new Color(180, 0, 128), //20
            new Color(0, 180, 128), //21
            new Color(175, 238, 238), //22 paleturquoise
            new Color(255, 255, 255) //23
        };
        try {
            return color[SysApoio.parseInt(cidade.getNacao().getCodigo())];
        } catch (Exception ex) {
            return color[0];
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
            if (!produto.isMoney() || producao >= 250) {
                return producao;
            } else if (cidade.getNacao().getHabilidadesNacao().containsKey("0039") && cidade.getNacao().getRaca() == cidade.getRaca()) {
                //se mesma cultura e com habilidade, entao garante minimo de 250
                return 250;
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
}
