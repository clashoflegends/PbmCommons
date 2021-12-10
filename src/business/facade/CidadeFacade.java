/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import business.converter.ColorFactory;
import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import model.Cenario;
import model.Cidade;
import model.Jogador;
import model.Local;
import model.Mercado;
import model.Nacao;
import model.Personagem;
import model.Produto;
import model.Raca;
import msgs.BaseMsgs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;
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
    private static final CenarioFacade cenarioFacade = new CenarioFacade();
    private static final BattleSimFacade combatSimFacade = new BattleSimFacade();
    public static final int[] ForneceComida = {0, 100, 200, 1000, 2500, 5000};

    public int getArrecadacaoImpostos(Cidade city) {
        try {
            int base = 2500;
            if (city.getNacao().hasHabilidade(";NSP;") && city.getLocal().getTerreno().isPlanicie()) {
                base = base * (100 + city.getNacao().getHabilidadeValor(";NSP;")) / 100;
            }
            if (city.getNacao().hasHabilidade(";NST;")) {
                base = base * (100 + city.getNacao().getHabilidadeValor(";NST;")) / 100;
            }
            if (city.getNacao().hasHabilidade(";NTL;")) {
                base = base * (100 + city.getNacao().getHabilidadeValor(";NTL;")) / 100;
            }
            if (city.getNacao().hasHabilidade(";NTH;") && isHeroPresent(city)) {
                base = base * (100 + city.getNacao().getHabilidadeValor(";NTH;")) / 100;
            }
            return (city.getNacao().getImpostos() * base * (Math.max(city.getTamanho() - 1, 0))) / 100;
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    public boolean isHeroPresent(Cidade city) {
        Nacao nationCity = city.getNacao();
        for (Personagem pc : city.getLocal().getPersonagens().values()) {
            if (pc.getNacao() != nationCity) {
                //not same nation
                continue;
            }
            if (!pc.isHero()) {
                //not hero
                continue;
            }
            return true;
        }
        return false;
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

    public int getTamanho(Local local) {
        try {
            return local.getCidade().getTamanho();
        } catch (NullPointerException e) {
            return 0;
        }
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

    public javafx.scene.paint.Color getNacaoColorFillFx(Cidade cidade) {
        //FIXME: move para nacaoFacade
        try {
            final Color fillColor = cidade.getNacao().getFillColor();
            return javafx.scene.paint.Color.rgb(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue());
        } catch (Exception ex) {
            return javafx.scene.paint.Color.DARKGRAY;
        }
    }

    public javafx.scene.paint.Color getNacaoColorBorderFx(Cidade cidade) {
        //FIXME: move para nacaoFacade
        try {
            final Color borderColor = cidade.getNacao().getBorderColor();
            return javafx.scene.paint.Color.rgb(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue());
        } catch (Exception ex) {
            return javafx.scene.paint.Color.DARKGRAY;
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

    public int getResourceSell(Cidade cidade, Produto produto, Mercado mercado, Cenario cenario, int turno) {
        return (this.getEstoque(cidade, produto) + this.getProducao(cidade, produto, cenario, turno)) * mercado.getProdutoVlVenda(produto);
    }

    public int getResourceBestSell(Cidade cidade, Mercado mercado, Cenario cenario, int turno) {
        int ret = 0;
        for (Produto produto : mercado.getProdutos()) {
            int sellTotal = getResourceSell(cidade, produto, mercado, cenario, turno);
            if (sellTotal > ret) {
                ret = sellTotal;
            }
        }
        return ret;
    }

    public int getProducao(Cidade cidade, Produto produto, Cenario cenario, int turno) {
        return getProducao(cidade, produto) * cenarioFacade.getResourcesWinterReduction(cenario, turno) / 100;
    }

    public int getProducao(Cidade city, Produto produto) {
        int producao = city.getProducao(produto);
        try {
            if (city.getLocal().getCoordenadas().equals("0961")) {
                log.debug("AKI!");
            }

            if (city.getNacao().hasHabilidade(";NWP;") && produto.isWood()) {
                producao += producao * city.getNacao().getHabilidadeValor(";NWP;") / 100;
            }
            if (city.getNacao().hasHabilidade(";NSW;") && city.getLocal().getClima() >= 5) {
                //Summer Production: 50% production bonus in warm or better climate
                producao += producao * city.getNacao().getHabilidadeValor(";NSW;") / 100;
            }
            if (city.getNacao().hasHabilidade(";NTR;") && isHeroPresent(city)) {
                //Epic hero presence boosts resource production by 3x
                producao += producao * city.getNacao().getHabilidadeValor(";NTR;") / 100;
            }
            if (produto.isMoney() && producao <= city.getNacao().getHabilidadeNacaoValor("0039")
                    && city.getNacao().getHabilidadesNacao().containsKey("0039")
                    && city.getNacao().getRaca() == city.getRaca()) {
                //se mesma cultura e com habilidade, entao garante minimo de 250
                producao = city.getNacao().getHabilidadeNacaoValor("0039");
            }
            if (produto.isMoney() && city.getNacao().hasHabilidade(";PGH;")
                    && localFacade.isTerrenoMontanhaColina(city.getLocal().getTerreno())) {
                //se em montanha/colina e com habilidade, entao garante minimo de 500
                producao = Math.max(producao, city.getNacao().getHabilidadeValor(";PGH;"));
            }
            if (produto.isMoney() && city.getNacao().hasHabilidade(";PGM;")
                    && city.getNacao().getRaca() == city.getRaca()) {
                //se mesma cultura e com habilidade, entao garante minimo de 250
                producao = Math.max(producao, city.getNacao().getHabilidadeValor(";PGM;"));
            }
            return producao;
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
        return combatSimFacade.getCityDefenseBase(cidade);
    }

    public int getDefesa(int tamanho, int fortificacao, int lealdade) {
        return combatSimFacade.getCityDefense(tamanho, fortificacao, lealdade);
    }

    public boolean isAtivo(Cidade cidade) {
        return cidade.getTamanho() > 0;
    }

    public Raca getNacaoRaca(Cidade cidade) {
        return getNacao(cidade).getRaca();
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
        if (cidade == null) {
            return false;
        }
        return cidade.getTamanho() >= 4;
    }

    public int getUpkeepMoney(Cidade cidade) {
        return (cidade.getDocas() * 250 + cidade.getFortificacao() * 500);
    }

    public int getFoodGiven(Cidade cidade) {
        return CidadeFacade.ForneceComida[cidade.getTamanho()];
    }

    public int getCustoAmpliacao(Cidade city, int baseCost) {
        int custo = 2000;
        if (city.getNacao().hasHabilidade(";PPB;")) {
            custo = custo / city.getNacao().getHabilidadeValor(";PPB;");
        }
        return (city.getTamanho() * custo + baseCost);
    }

    public int subEstoque(Cidade city, Produto produto, int qtd) {
        city.setEstoque(produto, city.getEstoque(produto) - qtd);
        return city.getEstoque(produto);
    }

    public int sumEstoque(Cidade city, Produto produto, int qtd) {
        city.setEstoque(produto, city.getEstoque(produto) + qtd);
        return city.getEstoque(produto);
    }

    public String getNomeHex(Cidade city) {
        return (city.getNome() + " (" + city.getLocal().getCoordenadas() + ")");
    }

    public void subFortification(Cidade city, int fator) {
        city.setFortificacao(city.getFortificacao() - fator);
    }

    public int getPointsDomination(Cidade city) {
        if (!isPointsDomination(city)) {
            return 0;
        } else if (city.hasHabilidade(";LCP5;")) {
            return city.getHabilidadeValor(";LCP5;");
        } else if (city.hasHabilidade(";LCP2;")) {
            return city.getHabilidadeValor(";LCP2;");
        } else if (city.hasHabilidade(";LCP1;")) {
            return city.getHabilidadeValor(";LCP1;");
        } else {
            return 0;
        }
    }

    public boolean isPointsDomination(Cidade city) {
        return city.hasHabilidade(";LCP;");
    }

}
